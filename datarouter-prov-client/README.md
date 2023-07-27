<!---
 ============LICENSE_START===============================================
 org.onap.dmaap
 ========================================================================
 Copyright (c) 2023 J. F. Lucas. All rights reserved.
 ========================================================================
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ============LICENSE_END=================================================
--->
# Data Router provisioning client

## Overview
The Data Router (DR) provisioning client runs as a Kubernetes initContainer for ONAP DCAE applications that use DR feeds to
transfer data between applications.   The logic for the client is contained in a script that makes requests to the DR provisioning
node using the DR provisioning API.  (See the [API documentation](https://docs.onap.org/projects/onap-dmaap-datarouter/en/london/apis/data-router-api.html#dmaap-data-router-api) for details.)

The DR provisioning client (drprov-client) replaces the DMaaP Bus Controller client (dbc-client).  The dbc-client used the DMaaP Bus Controller to
provision data router feeds and subscribers and DMaaP Message Router topics and clients.  The Message Router provisioning functionality
is no longer needed, and Bus Controller will be deprecated and removed from the ONAP tree.

The provisioning logic is in the [drprov-client.sh script](./misc/drprov-client.sh).  This script is set as the entrypoint for the initContainer.

The drprov-client performs two high-level tasks:

- Makes requests to the DR provisioning API to create feeds and subscriptions and captures the API's responses.
- Uses the API's response to update a component's configuration file by replacing placeholders in the file (in the form of environment variable names) with values from the API responses.

The drprov-client script queries the DR provisioning API to determine if a feed already exists (based on the feed name and feed version) and does not attempt to create the feed again.  Instead, it retrieves the feed information for the existing feed and supplies that information to a component.

Similarly, the drprov-client script queries the DR provisioning API to determine if a subscription already exists (based on the username, password, and delivery URL for the subscription).  If one exists, the script does not create a new subscription.

## Usage
DR has been used only by components in the DCAE project.
  The [DCAE common
Helm templates](https://git.onap.org/oom/tree/kubernetes/dcaegen2-services/common/dcaegen2-services-common/templates) create the files need by the drprov-client to make API requests to the DR provisioning server.  They also insert the drprov-client initContainer into the Kubernetes deployment spec for an application that uses DR feeds.  The DCAE common templates rely on the [common DMaaP provisioning template](https://git.onap.org/oom/tree/kubernetes/common/common/templates/_dmaapProvisioning.tpl).

 The developer of a DCAE component that uses DR simply adds a section to the component's `values.yaml` file.  For details on how feeds and subscriptions are defined in a component's `values.yaml` file, see the [inline documentation for the common DMaaP provisioning template](https://git.onap.org/oom/tree/kubernetes/common/common/templates/_dmaapProvisioning.tpl).

## Changes from dbc-client
The DMaaP bus controller provided a unified API that supported provisioning for both DR and the DMaaP Message Router.   The bus controller was
a Java program with its own database, and it shielded its clients from some of the details of the DR provisioning interface.   The drprov-client interacts
directly with the DR provisioning API, and there are some changes in how it behaves and how components that use DR are configured via their values.yaml files.

### What happened to publishers?
DR does not have a concept of a publisher that's distinct from a feed.  Every feed has at least one "endpoint", a username/password pair that an
application will supply via HTTP Basic Authentication when publishing a file.  The bus controller always created an endpoint (with random username
and password) when a client requested creation of a feed.  The bus controller then allowed clients to request creation of a "publisher".  The bus
controller implemented this by using the DR API's updating capability (via an HTTP PUT request) to add an endpoint to the feed.   It would have been
possible to replicate this functionality in a script, but it would have added complexity.  The one current use case employing DR feeds does not require this capability.   Note that it's still possible
to have multiple applications (or multiple instances of the same application) publishing to a DR feed.  They just have to use the same username and
password that was provided when the feed was first created.

This change implies two changes for the values.yaml files of DR clients:
  - A client that publishes to DR creates a feed that includes an endpoint.  It no longer creates a separate publisher.
  - A client that subscribes to a feed does not need to supply a definition for the feed.  The subscription definition
   includes the feed name and feed version, and that's enough information to allow the drprov-client to associate the
   subscription with the feed.

A client that subscribes to a feed needs to include the publisher in its readiness check, so that the feed is set up before the subscriber provisioning runs.  (This is already the case for the one existing DR subscriber in ONAP.)

### Location
The bus controller also had a notion of "location" which has no corresponding notion in DR, so references to "dcaeLocation" in the provisioning data are no longer needed.

### Owner
The bus controller interface included a field called "owner" for a feed.  This may have had some function within the bus controller.  It had a minor effect on the bus controller's invocation of the DR provisioning API (setting the `X-DMAAP-DR-ON-BEHALF-OF` header in the API requests associated with the feed).  It adds no real value.  Maintaining it would add complexity to the drprov-client script and would complicate debugging DR provisioning issues.

Note that the drprov-client.sh script uses "drprovcl" by default as the value in the `X-DMAAP-DR-ON-BEHALF-OF`.  This can be overriden by setting the environment
variable `ONBEHALFHDR` in the [common DMaaP provisioning template](https://git.onap.org/oom/tree/kubernetes/common/common/templates/_dmaapProvisioning.tpl).

`X-DMAAP-DR-ON-BEHALF-OF` forms the basis of a simple authorization mechanism used by the DR provisioning API.  The value passed in this header when creating a
feed or a subscription is stored by the DR provisioning server.  When the DR API receives a request to retrieve the data for a specific feed or a specific subscription, the API checks that the value in the `X-DMAAP-DR-ON-BEHALF-OF` header in the incoming request matches the stored value for the target feed or subscription.   Sometimes during testing or debugging, it is useful to query the DR provisioning API about a specific feed or subscription using a tool like `curl`.  Using `X-DMAAP-DR-ON-BEHALF-OF: drprovcl` will allow access to the feeds and subscription created by the drprov-client.sh script.

### Classification
The bus controller interface included a field called "asprClassification" for a feed.  The field is required by DR and indicates the sensitivity of the information transmitted in a feed.  This has not been used in ONAP, but it is a required field. "aspr" is an internal acronym at the company where DR was originally developed. The field has been renamed "classification".  "unclassified" is a reasonable value to use in ONAP.

### One initContainer instead of two
Using the dbc-client involved running two initContainers.  The first made API requests to the bus controller API and stored the responses in a volume on the pod.  The second used the stored data to update the component's configuration using
information returned by the API calls.  The drprov-client runs in a single initContainer that makes the API calls and updates the component's configuration.

## Limitations

The drprov-client targets a limited range of uses for DR within ONAP.  It is not a general purpose provisioning solution for DR.  The following is a summary of some of the important limitations:

  - A pod running a subscriber to a feed must wait for the feed's publisher to become ready, to ensure that the feed has been created.
  - While multiple applications (or multiple instances of the same application) can publish to the same feed, they must use the same username and password.
  - Feeds and subscriptions are not known to Kubernetes or Helm, so that deleting a publisher or a subscriber via Kubernetes or Helm does not delete the corresponding feeds or subscriptions in DR.  The drprov-client handles the case of a pod being restarted by reusing an existing feed or subscription.

DR was originally designed to move large files to multiple destinations over a wide geographic area.  While it clearly works for large file transfers within a Kubernetes cluster, it isn't optimal.  Currently (as of the Montreal release of ONAP), DR is used by two components ([the DCAE datafile collector](https://git.onap.org/dcaegen2/collectors/datafile/tree/), which publishes performance management data to a DR feed), and [the DCAE pm-mapper](https://git.onap.org/dcaegen2/services/pm-mapper/tree/), which subscribes to the same feed.  If new use cases requiring large file transfer emerge in the future, it would be wise to look at transfer mechanisms designed specifically for the Kubernetes environment.
