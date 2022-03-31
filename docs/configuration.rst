.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

..  _configuration:

Configuration
=============

Most configuration properties for both Data Router Provisioning server and Data Router Node server
should remain as default values.

An exception to this is when a user wants to run over HTTP (non TLS).

For DR Provisioning server config, edit the following props in the provserver.properties file.

.. code-block:: bash

    org.onap.dmaap.datarouter.provserver.tlsenabled  = false

and ensure aaf cadi is disabled also

.. code-block:: bash

    org.onap.dmaap.datarouter.provserver.cadi.enabled = false


For DR Node server config, edit the following props in the node.properties file to target http.

.. code-block:: bash

    #    URL to retrieve dynamic configuration
    ProvisioningURL = http://dmaap-dr-prov:8080/internal/prov
    #
    #    URL to upload PUB/DEL/EXP logs
    LogUploadURL = http://dmaap-dr-prov:8080/internal/logs
    ...
    #
    #    AAF CADI enabled flag
    CadiEnabled = false
    #
    #    Enable to run over http or https (default true|https)
    TlsEnabled = false
