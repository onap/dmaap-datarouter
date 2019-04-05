.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

..  _configuration:

Configuration
=============

Configuration properties for both Data Router Provisioning server and Data Router Node server should remain as default values.

The only exception to this, is when enabling the AAF CADI framework to authorize the DR provisioning requests.

.. note:: The AAF CADI filtering feature is disabled by default. When AAF CADI auth is enabled, all DR API calls must provide an AAF AppID to access the relevant API endpoint.

To enable DR AAF CADI authorization, the following steps are required:

DR CADI Prerequisites:
    * AAF deployment

Update the following properties at deployment time.


**DMaaP DR Prov AAF properties**

::

    # AAF config
    org.onap.dmaap.datarouter.provserver.cadi.enabled = true

    # AAF URL to connect to AAF server
    org.onap.dmaap.datarouter.provserver.cadi.aaf.url = https://<RELEVANT_AAF_URL>:8095


**DMaaP DR Node AAF properties**

::

    # AAF URL to connect to AAF server
    AafUrl = https://<RELEVANT_AAF_URL>:8095

    # AAF CADI enabled flag
    CadiEnabled = true

