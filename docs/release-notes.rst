.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. _release_notes:


..      ===========================
..      * * *    GUILIN       * * *
..      ===========================

DMaaP Data Router Release Notes
===============================

Abstract
--------

This document provides the release notes for the DMaaP DR Guilin release.

Summary
-------

Release Data
------------

+--------------------------------------+--------------------------------------+
| **DMaaP Data Router Project**        |                                      |
+--------------------------------------+--------------------------------------+
| **Docker images**                    | - datarouter-prov 2.1.7              |
|                                      | - datarouter-node 2.1.7              |
+--------------------------------------+--------------------------------------+
| **Release designation**              | 7.0.0 guilin                         |
+--------------------------------------+--------------------------------------+
| **Release date**                     | 2020-10-19 (TBD)                     |
+--------------------------------------+--------------------------------------+

New features
------------

Known Limitations, Issues and Workarounds
-----------------------------------------

System Limitations
------------------

Known Vulnerabilities
---------------------

Workarounds
-----------

Security Notes
--------------


..      ===========================
..      * * *    FRANKFURT    * * *
..      ===========================

DMaaP Data Router Release Notes
-------------------------------

Abstract
--------

This document provides the release notes for the DMaaP DR Frankfurt release.

Summary
-------

Release Data
------------

+--------------------------------------+--------------------------------------+
| **DMaaP Data Router Project**        |                                      |
+--------------------------------------+--------------------------------------+
| **Docker images**                    | - datarouter-prov 2.1.6              |
|                                      | - datarouter-node 2.1.6              |
+--------------------------------------+--------------------------------------+
| **Release designation**              | 6.0.0 frankfurt                      |
+--------------------------------------+--------------------------------------+
| **Release date**                     | 2020-05-14 (TBD)                     |
+--------------------------------------+--------------------------------------+

New features
------------

* Implement boolean flag in OOM to disable HTTP ports
* Dynamic cert distribution from OOM AAF

Known Limitations, Issues and Workarounds
-----------------------------------------

System Limitations
------------------

Known Vulnerabilities
---------------------
* CVE-2019-11358

Workarounds
-----------

Security Notes
--------------

References
----------

For more information on the ONAP Frankfurt release, please see:

#. `ONAP Home Page`_
#. `ONAP Documentation`_
#. `ONAP Release Downloads`_
#. `ONAP Wiki Page`_

.. _`ONAP Home Page`: https://www.onap.org
.. _`ONAP Wiki Page`: https://wiki.onap.org
.. _`ONAP Documentation`: https://docs.onap.org
.. _`ONAP Release Downloads`: https://git.onap.org


Version: 2.1.2 (El Alto)
---------------------------

:Release Date: 2019-09-05

New Features:

+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                                                     |
+================+=================================================================================================================================+
| DMAAP-1227     | Updating logging functionality to log events into correct log files as specified in logging spec                                |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-1228     | Updating Logging pattern to match logging spec                                                                                  |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-1049     | [DR] Update DR logging to match Platform maturity Logging Spec                                                                  |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+

Bug Fixes:

+----------------+--------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                      |
+================+==================================================================================================+
| DMAAP-1421     |  [DR] ElAlto AAF certs expired                                                                   |
+----------------+--------------------------------------------------------------------------------------------------+

Known Issues:
N/A

Security Issues:
N/A

Upgrade Notes:
N/A

Deprecation Notes:
N/A

Other:
N/A


Version: 2.1.0 (Dublin)
---------------------------

:Release Date: 2019-06-06

New Features:

+--------------+-------------------------------------------------------------------------------+
| JIRA ID      | Description                                                                   |
+==============+===============================================================================+
| DMAAP-978    | [DR] Query of publication history (new API) for use by Data File Collector)   |
+--------------+-------------------------------------------------------------------------------+
| DMAAP-980    | [DR] Optional consumer compression feed handling                              |
+--------------+-------------------------------------------------------------------------------+
| DMAAP-1016   | DR provisioning AAF integration                                               |
+--------------+-------------------------------------------------------------------------------+

Bug Fixes:

+----------------+--------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                      |
+================+==================================================================================================+
| DMAAP-964      | [DMAAP] DMAAP deployment failures starting 20190115 on casablanca branch                         |
+----------------+--------------------------------------------------------------------------------------------------+
| DMAAP-1010     | [DR] DMaaP Data Router fails healthcheck                                                         |
+----------------+--------------------------------------------------------------------------------------------------+
| DMAAP-1047     | [DR] Data Router docker version missing explicit version number                                  |
+----------------+--------------------------------------------------------------------------------------------------+
| DMAAP-1048     | [DR] AAF certs expired on dmaap-dr-prov and dmaap-dr-node                                        |
+----------------+--------------------------------------------------------------------------------------------------+
| DMAAP-1161     | [DR] filebeat container on DR-Node and DR-Prov are unable to publish to kibana                   |
+----------------+--------------------------------------------------------------------------------------------------+

Known Issues:
N/A

Security Issues:

*Fixed Security Issues*

*Known Security Issues*

- In default deployment DMAAP (dmaap-dr-prov) exposes HTTP port 30259 outside of cluster. [`OJSI-158 <https://jira.onap.org/browse/OJSI-158>`_]

*Known Vulnerabilities in Used Modules*

DMAAP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been
addressed, items that remain open have been assessed for risk and determined to be false positive. The DMAAP open
Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_.

- `DMAAP Project Page <https://wiki.onap.org/display/DW/DMaaP+Planning>`_
- `Passing Badge information for DMAAP DataRouter <https://bestpractices.coreinfrastructure.org/en/projects/2192>`_
- `Project Vulnerability Review Table for DMAAP <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_

Upgrade Notes:
N/A

Deprecation Notes:
N/A

Other:
N/A


Version: 1.0.8 (Casablanca)
---------------------------

:Release Date: 2019-02-28

New Features:

+--------------+------------------------------------------------------------------+
| JIRA ID      | Description                                                      |
+==============+==================================================================+
+--------------+------------------------------------------------------------------+

Bug Fixes:

+----------------+--------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                      |
+================+==================================================================================================+
| DMAAP-1065     | [DR] Casablanca - AAF certs expired on dmaap-dr-prov and dmaap-dr-node                           |
+----------------+--------------------------------------------------------------------------------------------------+

Known Issues:
N/A

Security Issues:
DMAAP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been
addressed, items that remain open have been assessed for risk and determined to be false positive. The DMAAP open
Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_.

- `DMAAP Project Page <https://wiki.onap.org/display/DW/DMaaP+Planning>`_
- `Passing Badge information for DMAAP DataRouter <https://bestpractices.coreinfrastructure.org/en/projects/2192>`_
- `Project Vulnerability Review Table for DMAAP <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_

Upgrade Notes:
N/A

Deprecation Notes:
N/A

Other:
N/A


Version: 1.0.3 (Casablanca)
---------------------------

:Release Date: 2018-11-30

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

Known Issues:
N/A

Security Issues:
DMAAP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been
addressed, items that remain open have been assessed for risk and determined to be false positive. The DMAAP open
Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_.

- `DMAAP Project Page <https://wiki.onap.org/display/DW/DMaaP+Planning>`_
- `Passing Badge information for DMAAP DataRouter <https://bestpractices.coreinfrastructure.org/en/projects/2192>`_
- `Project Vulnerability Review Table for DMAAP <https://wiki.onap.org/pages/viewpage.action?pageId=42598688>`_

Upgrade Notes:
N/A

Deprecation Notes:
N/A

Other:
N/A
