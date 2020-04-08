.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. _architecture:

Architecture
============

Capabilities
------------
The DMaaP Data Router (DR) provisioning API is an HTTPS-based, REST-like API for creating and managing
DR feeds and subscriptions, which provides a pub/sub architectural model for the transfer of data.

The DR API also supports `AAF CADI authorization <https://docs.onap.org/en/latest/submodules/aaf/authz.git/docs/sections/architecture/cadi.html#authorization>`_.

To use this feature, the API client must provide a valid AAF AppID / MechID with each request.
To enable this feature, see the :ref:`configuration` section.

.. note:: In future releases, AAF CADI auth will be enabled by default.

Usage Scenarios
---------------
Typically, DR pub clients request the provisioning of a new DR feed.
Once created, DR sub clients can then subscribe to the feed to receive all data published to that feed.

   .. image:: images/dr_pub_flow.png


The DR provisioning API is not meant to be used directly by DR end users (publishers and subscribers)
for feed / subscription CRUD (create, read, update, delete) operations.

Instead, prospective publishers and subscribers should use the DMaaP Bus Controller API, which will call
the DR provisioning API to manage feeds and subscriptions.

   .. image:: images/dr_bc_prov.png


See DMaaP Bus Controller API docs for more information:

`Bus Controller Feeds API <https://onap.readthedocs.io/en/latest/submodules/dmaap/dbcapi.git/docs/api.html#feeds>`_

`Bus Controller Subs API <https://onap.readthedocs.io/en/latest/submodules/dmaap/dbcapi.git/docs/api.html#dr-subs>`_


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
