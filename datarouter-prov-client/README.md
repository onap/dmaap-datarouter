# Data Router provisioning client

## Overview
The Data Router (DR) provisioning client runs as a Kubernetes initContainer for ONAP DCAE applications that use DR feeds to
transfer data between applications.   The logic for the client is contained in a script that makes requests to the DR provisioning
node using the DR provisioning API.

The DR provisioning client replaces the DMaaP Bus Controller client (dbc-client).  The dbc-client used the DMaaP Bus Controller to
provision data router feeds and subscribers and DMaaP Message Router topics and clients.  The Message Router provisioning functionality
is no longer needed, and Bus Controller will be deprecated and removed from the ONAP tree.

The provisioning logic is in the [drprov-client.sh script]<./misc/drprov-client.sh>.  This script is set as the entrypoint for the initContainer.

## What happened to publishers?  (And other changes from dbc-client)
DR does not have a concept of a publisher that's distinct from a feed.  Every feed has at least one "endpoint", a username/password pair that an
application will supply via HTTP Basic Authentication when publishing a file.  The bus controller always created an endpoint (with random username
and password) when a client requested creation of a feed.  The bus controller then allowed clients to request creation of a "publisher".  The bus
controller implement this by using the DR API's updating capability (via an HTTP PUT request) to add an endpoint to the feed.   It would have been
possible to replicate this functionality in a script, but it would have added complexity.  (The bus controller was already complex, and it was written
in java, not shell, so it made sense there.)  The one current use case involve DR feeds does not require this capability.   Note that it's still possible
to have multiple applications (or multiple instances of the same application) publishing to a DR feed.  They just have to use the same username and
password that was provided when the feed was first created.

The bus controller also had a notion of "location" which has no analog in DR, so references to "dcaeLocation" in the provisioning data are no longer
needed.

## Operation

## Dependencies and coupling

## Limitations
