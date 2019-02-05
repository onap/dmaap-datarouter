.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Release-notes
==============
Version: 1.0.7 (Casablanca)
---------------------------

:Release Date: 2018-11-30

The DataRouter(DR) provisioning API is a HTTPS-based, REST-like API for creating and managing DR feeds and
subscriptions.

New Features:

+--------------+------------------------------------------------------------------+
| JIRA ID      | Description                                                      |
+==============+==================================================================+
| DMAAP-20     | REST api for publishing data to DR                               |
+--------------+------------------------------------------------------------------+
| DMAAP-21     | REST api for subscribing to data in DR                           |
+--------------+------------------------------------------------------------------+

Bug Fixes:

+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                                                     |
+================+=================================================================================================================================+
| DMAAP-877      | DR Logging API not storing Feed/Sub data                                                                                        |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-850      | Second subscriber not receiving the published file                                                                              |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-596      | DR - datarouter-prov container fails to come up successfully                                                                    |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-565      | Incorrect nexusUrl parameter in datarouter pom files                                                                            |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+

Known Issues
N/A

Security Issues
DMAAP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been
addressed, items that remain open have been assessed for risk and determined to be false positive. The DMAAP open
Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_.

- `DMAAP Project Page <https://wiki.onap.org/display/DW/DMaaP+Planning>`_
- `Passing Badge information for DMAAP DataRouter <https://bestpractices.coreinfrastructure.org/en/projects/2192>`_
- `Project Vulnerability Review Table for DMAAP <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_

Upgrade Notes
N/A

Deprecation Notes
N/A

Other
N/A