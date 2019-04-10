.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Release-notes
=============

Version: 1.0.9 (Casablanca)
---------------------------

:Release Date: 2019-04-01

The DataRouter(DR) provisioning API is a HTTPS-based, REST-like API for creating and managing DR feeds and
subscriptions.

New Features:
N/A

Bug Fixes:
N/A

Known Issues:
N/A

Security Issues:
N/A

Upgrade Notes:
N/A

Deprecation Notes:
N/A

Other:
New AAF Certs were added to the DR images that have a one year duration.


Version: 1.0.8
--------------

:Release Date: 2019-02-28

New Features:
N/A

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

Version: 1.0.1
--------------

:Release Date: 2018-09-20

New Features:

 - Implements a RESTful HTTPS API for creating feeds to publish files to and subscribe to,
   as well as perform CRUD operations on these feeds.
 - Implements a RESTful HTTPS API for subscribing to feeds and perform CRUD operations on these subscriptions.
 - Implements a RESTful HTTPS API for publishing to feeds and perform CRUD operations on these published files.

Bug Fixes:
N/A

Known Issues:
N/A

Other:
N/A
