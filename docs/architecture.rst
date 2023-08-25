.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. _architecture:

Architecture
============

Capabilities
------------
The DMaaP Data Router (DR) provisioning API is an HTTPS-based, REST-like API for creating and managing
DR feeds and subscriptions, which provides a pub/sub architectural model for the transfer of data.

Usage Scenarios
---------------
Typically, DR pub clients request the provisioning of a new DR feed.
Once created, DR sub clients can then subscribe to the feed to receive all data published to that feed.

   .. image:: images/dr_pub_flow.png

Previously, prospective publishers and subscribers would use the DMaaP Bus Controller API, which would call
the DR provisioning API to manage feeds and subscriptions.

However, with the deprecation of Message Router, the DMaaP Bus Controller API has also been deprecated and
DR provisioning has been brought into DR itself. This will be performed through the
Data Router (DR) provisioning client.

The Data Router (DR) provisioning client runs as a Kubernetes initContainer for ONAP DCAE applications
that use DR feeds to transfer data between applications. The logic for the client is contained in a script
that makes requests to the DR provisioning node using the DR provisioning API.
(See the `API documentation <https://docs.onap.org/projects/onap-dmaap-datarouter/en/london/apis/data-router-api.html#dmaap-data-router-api>`_ for details.)

The DR provisioning client (drprov-client) replaces the DMaaP Bus Controller client (dbc-client).
The dbc-client used the DMaaP Bus Controller to provision data router feeds and subscribers and
DMaaP Message Router topics and clients. The Message Router provisioning functionality is no longer needed,
and Bus Controller will be deprecated and removed from the ONAP tree.

The provisioning logic is in a script called drprov-client.sh.  This script is set as the
entrypoint for the initContainer.

The drprov-client performs two high-level tasks:

- Makes requests to the DR provisioning API to create feeds and subscriptions and captures the API's responses.
- Uses the API's response to update a component's configuration file by replacing placeholders in the file
  (in the form of environment variable names) with values from the API responses.

The drprov-client script queries the DR provisioning API to determine if a feed already exists (based on the feed
name and feed version) and does not attempt to create the feed again.  Instead, it retrieves the feed information
for the existing feed and supplies that information to a component.

Similarly, the drprov-client script queries the DR provisioning API to determine if a subscription already exists
(based on the username, password, and delivery URL for the subscription).  If one exists, the script does not create
a new subscription.

Refer to `README file <https://gerrit.onap.org/r/gitweb?p=dmaap/datarouter.git;a=blob;f=datarouter-prov-client/README.md>`_
in drprov-client in the datarouter repo for full details.

High level Architecture
-----------------------
The following diagram shows the high-level relationship between the system components:

   .. image:: images/dr_arch_only.png


DMaaP DR architecture uses the Eclipse Jetty server as an application server to service it's front-end.
   * dmaap-dr-prov services all provisioning requests.
   * dmaap-dr-node services the publishing of data to feed subscribers.

DMaaP DR uses MariaDB as it's storage component for the following:
   * DR Provisioning data. (feeds, subscribers, etc.)
   * Historical logging data related to feed activity. (Publish, Delivery, etc.)
