#!/bin/sh
#
# ============LICENSE_START===============================================
# org.onap.dmaap
# ========================================================================
# Copyright (c) J. F. Lucas. All rights reserved.
# # ========================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=================================================

PROVURL=${PROVURL:-"http://dmaap-dr-prov:8080"}
DRCONFIGDIR=${DRCONFIGDIR:-"/opt/app/config"}
ONBEHALFHDR="X-DMAAP-DR-ON-BEHALF-OF: drprovcl"
FEEDTYPE="Content-Type: application/vnd.dmaap-dr.feed"
SUBTYPE="Content-Type: application/vnd.dmaap-dr.subscription"
APPCONFIGINPUT=${APPCONFIGINPUT:-"/config-input"}
APPCONFIG=${APPCONFIG:-"/config"}

function logit() {
    # Direct log entries to stderr because to
    # allow logging inside functions that use
    # stdout to return values
    echo $(date -u -Ins)\|"$@" >&2
}

function getFeedByNameVer() {
# Get feed info using name and version
#   $1 -- Feed name (arbitrary string without embedded '"')
#   $2 -- Feed version (arbitrary string without embedded '"')
#   Returns feed data and exits with 0 if
#   feed is found.
#   Returns empty string and exits with 1 in
#   any other case.

    # Construct urlencoded query
    local NAME="$(printf %s "$1" | tr -d '"' | jq -R -r @uri)"
    local VER="$(printf %s "$2" | tr -d '"' | jq -R -r @uri)"
    local QUERYURL="${PROVURL}"/?"name=${NAME}"\&version="${VER}"
    local FEEDDATA

    # Make the query
    # Not checking exact cause for error,
    # just looking for success or not.
    local RV=1
    if FEEDDATA=$(curl --fail -s -H "${ONBEHALFHDR}" "${QUERYURL}")
    then
	    echo ${FEEDDATA}
        RV=0
    fi

    return ${RV}
}

function subscriptionExists() {
#  See if there a subscription to the feed
#  that has the specified username, password,
#  and delivery URL.
#      $1 -- subscribe URL for the feed
#      $2 -- username for the subscription
#      $3 -- password for the subscription
#      $4 -- delivery URL for the subscription
# Sets a return value of 0 if a matching
# subscription is found and echoes the
# corresponding subscription URL.
# Others sets a return value of 1 and
# echoes an empty string.

local RV=1
local SUBRESP
local SUBDATA
local SUBLIST

# Query the feed's subscribe URL to get a
# list of the URLs for existing subscriptions
if SUBRESP=$(curl -s --fail -H "${ONBEHALFHDR}" "$1")
then
    # Loop through the list of existing subscriptions
    while read -r SUBURL   # read from $SUBRESP (see redirect on "done")
    do
        # Retrieve subscription data from the subscription's URL
        if SUBDATA=$(curl -s --fail -H "${ONBEHALFHDR}" "${SUBURL}")
        then
            local SUBUSER=$(echo ${SUBDATA} | jq -r .delivery.user)
            local SUBPASS=$(echo ${SUBDATA} | jq -r .delivery.password)
            local SUBDELURL=$(echo ${SUBDATA} | jq -r .delivery.url)
            if [ "$2" = "${SUBUSER}" -a "$3" = "${SUBPASS}" -a "$4" = "${SUBDELURL}" ]
            then
                RV=0  #TRUE
                break
            fi
        else
            # This will happen, for instance, if the name in
            # in the "X-DMAAP-DR-ON-BEHALF-OF" header doesn't
            # match the owner of the feed.  (Not likely in
            # the ONAP use case, but possible.)  Could also be
            # the result of connectivity issues, bad URL,...
            logit "WARNING: Could not retrieve ${SUBURL}"
        fi
    done < <(echo ${SUBRESP} | jq -r .[])
 else
    logit "ERROR: failed to fetch subscription list from $1"
fi

echo ${SUBURL}
return ${RV}
}

function createFeedFromFile() {
# Create a feed using information from a JSON file
# Note that creating a feed also creates the publisher
#   $1 -- Path to JSON file
#   Returns feed data from the DR provisioning node
#   and exits with 0 if the feed is created.
#   Returns empty string and exits with 1 in
#   any other case.

    local FEEDDATA
    local RV=1

    if test -f "$1"
    then
        # Substitute any environment variables in the subscription file
        local FEEDREQUEST=$(envsubst < "$1")
        if FEEDDATA=$(curl --fail -s --data-ascii "${FEEDREQUEST}" -H "${ONBEHALFHDR}" -H "$FEEDTYPE" ${PROVURL}/)
        then
            echo ${FEEDDATA}
            RV=0
        fi
    fi

return ${RV}
}

function createSubscriptionFromFile() {
# Create a subscription to a feed from a JSON file
# if a subscription with the same username, password
# and delivery URL doesn't already exist.
# We don't want multiple subscriptions if for some
# reason a subscriber's pod is redeployed.
# $1 -- JSON file defining the subscription
#
    local SUBURL
    local SUBDATA
    local EXISTINGSUB

    local RV=1

    if test -f "$1"
    then
        # Extract feed name and version from the JSON file
        local FEEDNAME=$(jq '.feed.name' "$1")
        local FEEDVER=$(jq '.feed.version' "$1")

        # Extract subscription parameters from the JSON file
        # (needed for checking if there's an existing subscription)
        local SUBUSER=$(jq -r '.delivery.user' "$1")
        local SUBPASS=$(jq -r '.delivery.password' "$1")
        local SUBDELURL=$(jq -r '.delivery.url' "$1")

        # Look up the feed and get the subscribe URL
        if SUBURL=$(getFeedByNameVer "${FEEDNAME}" "${FEEDVER}" | jq -r .links.subscribe)
        then
            # Check whether a matching subscription already exists
            if EXISTINGSUB=$(subscriptionExists ${SUBURL} ${SUBUSER} ${SUBPASS} ${SUBDELURL})
            then
                logit "Using existing subscription: ${EXISTINGSUB}.  No new subscription created."
                RV=0
            else
                # Substitute any environment variables in the subscription file
                local SUBREQUEST=$(envsubst < "$1")
                # Create the subscription
                if SUBDATA=$(curl --fail -s --data-ascii "${SUBREQUEST}" -H "${ONBEHALFHDR}" -H "${SUBTYPE}" ${SUBURL})
                then
                    logit "Created new subscription: $(echo ${SUBDATA} | jq -r '.links.self')"
                    RV=0
                fi
            fi
        fi
    fi
    return ${RV}
}

function createOrGetFeed() {
# Retrieve feed data from the DR provisioning node for
# the feed described in a JSON file.  If the feed
# does not exist, create the file.
#  $1 -- Path to JSON file
#  Returns feed data from the DR provisioning node
#  if the feed exists or if it has been successfully
#  created, and exits with 0.
#  Returns empty string and exits with 1 in
#  any other case.

    local FEEDDATA

    local RV=1

    if test -f "$1"
    then
        # Extract feed name and version from file
        local NAME=$(cat "$1" | jq .name)
        local VER=$(cat "$1" | jq .version)

        # Check whether feed already exists
        # (DR does not allow two feeds with same name and version)
        if FEEDDATA=$(getFeedByNameVer "${NAME}" "${VER}")
        then
            logit "Using existing feed: $(echo ${FEEDDATA} | jq -r '.links.self'). No new feed created."
            RV=0
        else
            # Create feed
            if FEEDDATA=$(createFeedFromFile "$1")
            then
                logit "Created new feed:  $(echo ${FEEDDATA} | jq -r '.links.self')" >&2
                RV=0
            fi
        fi
    fi

    echo ${FEEDDATA}
    return $RV
}

function provisionFeeds() {
# Create a feed for each JSON file in the
# directory specified in $1, unless the
# a feed with the same name and version
# already exists, in which case use the
# information for the existing feed.
# $1 -- Path to directory containing JSON
#       files defining DR feeds

    local FEEDDATA
    if test -d "$1"
    then
        for FEEDFILE in $(ls "$1"/*.json)
        do
            logit "Creating feed from ${FEEDFILE}"
            if FEEDDATA=$(createOrGetFeed ${FEEDFILE})
            then
                # Set environment variables with result data
                # Note that FEEDNUM is taken from the number that's embedded
                # in the file defining the feed.
                FEEDNUM=$(echo "${FEEDFILE}" | sed 's/^.*\/.*-\([0-9]\+\).json/\1/')
                export DR_FEED_PUBURL_${FEEDNUM}="$(echo ${FEEDDATA} | jq '.links.publish')"
                export DR_FEED_LOGURL_${FEEDNUM}="$(echo ${FEEDDATA} | jq '.links.log')"
            fi
        done
    fi
}

function provisionSubscriptions() {
# Create a subscription for each JSON file in the
# directory specified in $1
# $1 -- Path to directory containing JSON
#       files definining DR subscriptions
# Note that when provisioning a subscription to a feed,
# the DR API doesn't return any additional information
# that the subscriber needs.  Hence no information is
# extracted from the DR API response, and no environment
# variables are exported (unlike the provisionFeeds function.)

    if test -d "$1"
    then
        for SUBFILE in $(ls "$1"/*.json)
        do
            logit "Creating subscription from ${SUBFILE}"
            createSubscriptionFromFile ${SUBFILE}
        done
    fi
}

function updateConfigurations() {
# Run envsubst against each file in $1 (the application
# configuration input directory) to create a corresponding
# file in $2 (the application configuration directory) with
# environment variables replaced by the values set in the
# provisioning steps.  The file(s) in $2 will be used by
# the application to get (among other things) the information
# needed to work with DR feeds.
# $1 -- path to application configuration input directory
# $2 -- path to application configuration directory
    cd "$1"
    for CONFFILE in $(ls -1)
    do
        logit "Substituting environment vars in ${CONFFILE}"
        envsubst <${CONFFILE} > "$2"/${CONFFILE}
    done
}
set -ue

provisionFeeds ${DRCONFIGDIR}/feeds
provisionSubscriptions ${DRCONFIGDIR}/dr_subs
updateConfigurations ${APPCONFIGINPUT} ${APPCONFIG}
