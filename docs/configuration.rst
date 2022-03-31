.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

..  _configuration:

Configuration
=============

Most configuration properties for both Data Router Provisioning server and Data Router Node server
should remain as default values.

An exception to this is when a user wants to run over HTTP (non TLS).

For DR Provisioning server config, edit the following props in the provserver.properties file.

org.onap.dmaap.datarouter.provserver.tlsenabled   = false
and ensure aaf is disabled also
org.onap.dmaap.datarouter.provserver.cadi.enabled = false


For DR Node server config, edit the following props in the node.properties file.

TlsEnabled = false
and ensure cadi aaf is disabled also
CadiEnabled = false

