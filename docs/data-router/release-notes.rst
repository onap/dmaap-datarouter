.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Release-notes
==============

Version: 1.0.1
--------------

:Release Date: 2018-09-20


New Features:

 - Implements a RESTful HTTPS API for creating feeds to publish files to and subscribe to,
   as well as perform CRUD operations on these feeds.
 - Implements a RESTful HTTPS API for subscribing to feeds and perform CRUD operations on these subscriptions.
 - Implements a RESTful HTTPS API for publishing to feeds and perform CRUD operations on these published files.



Bug Fixes
NA

Known Issues
NA

Other
NA.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Release-notes
==============

Version: 1.0.1
--------------

:Release Date: 2018-09-20


New Features:

 - Implements a RESTful HTTPS API for creating feeds to publish files to and subscribe to,
   as well as perform CRUD operations on these feeds.
 - Implements a RESTful HTTPS API for subscribing to feeds and perform CRUD operations on these subscriptions.
 - Implements a RESTful HTTPS API for publishing to feeds and perform CRUD operations on these published files.



Bug Fixes
N/A

Known Issues
N/A

Security Issues
N/A

Upgrade Notes
N/A

Deprecation Notes
N/A

Other
N/A

Version: 1.0.7 (Casablanca)
---------------------------

:Release Date: 2018-11-22

New Features:

+--------------+------------------------------------------------------------------+
| JIRA ID      | Description                                                      |
+==============+==================================================================+
| DMAAP-20     | REST api for publishing data to DR                               |
+--------------+------------------------------------------------------------------+
| DMAAP-21     | REST api for subscribing to data in DR                           |
+--------------+------------------------------------------------------------------+
| DMAAP-524    | Kafka upgrade and AAF integration changes in MR                  |
+--------------+------------------------------------------------------------------+
| DMAAP-525    | Create the Kafka AAF Plugin and create the custom Kafka image    |
+--------------+------------------------------------------------------------------+
| DMAAP-530    | AAF Integration for DMaaP Buscontroller                          |
+--------------+------------------------------------------------------------------+
| DMAAP-542    | MM Agent provisioning enhancements                               |
+--------------+------------------------------------------------------------------+
| DMAAP-548    | Set-up static topics - PNF PnP use-case                          |
+--------------+------------------------------------------------------------------+

Bug Fixes:

+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| JIRA ID        | Description                                                                                                                     |
+================+=================================================================================================================================+
| DMAAP-882      | Topic Subscription not working in MR after rebooting the containers                                                             |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-877      | DR Logging API not storing Feed/Sub data                                                                                        |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-874      | AAF Artifact not found                                                                                                          |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-872      | DMaaP MR crash loopback - missing mail-1.4                                                                                      |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-868      | DMaaP MR  cache service unavailable : cant read events on fresh install (sporadic)                                              |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-855      | Buscontroller Jenkins daily jobs failing                                                                                        |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-850      | Second subscriber not receiving the published file                                                                              |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-846      | DBCAPI support for ONAP topic namespace                                                                                         |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-844      | DMAAP service fails from time to time and can not be restored by helm deploy after failure                                      |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-840      | DMaaP server certificate does not have a DNS resolvable name in the SAN section for Heat installs                               |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-827      | Buscontroller CSIT failing                                                                                                      |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-826      | useAAF flag not being initialized                                                                                               |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-783      | helm install error: "Invalid value: 30269: provided port is already allocated" to dmaap and contrib                             |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-689      | Password username authKey AutDate required although HTTPNOAUTH set                                                              |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-688      | Exception thrown that password is needed although it is provided                                                                |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-685      | Kafka container doesnt start                                                                                                    |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-624      | Publish to Feed CSIT Test Case Failing                                                                                          |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-596      | DR - datarouter-prov container fails to come up successfully                                                                    |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-595      | kafka_container and zookeeper_container IPs are mixed in standalone installation guide                                          |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-565      | Incorrect nexusUrl parameter in datarouter pom files                                                                            |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-547      | DMaaP https access through port 3905 fails because expired certificate                                                          |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-543      | error "ConsumerTimeoutException in Kafka consumer" in dmaap                                                                     |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-541      | DMaaP client does not failover to next host when consuming                                                                      |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-539      | Fix blocker issues reported by sonarQube for dmaap project                                                                      |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-504      | Access denied for User iPIxkpAMI8qTcQj8 does not own topic SDC-DISTR-NOTIF-TOPIC-AUTO                                           |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-502      | Failed to collate messages by topic, partition due to: Failed to fetch topic metadata for topic: msgrtr.apinode.metrics.dmaap   |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-202      | dmaap/datarouter/datarouter-prov build failing on LOGJSONObject import                                                          |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-201      | Missing jenkins jobs for dmaap/datarouter                                                                                       |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+
| DMAAP-157      | SDC service models distribution fails                                                                                           |
+----------------+---------------------------------------------------------------------------------------------------------------------------------+

Known Issues
N/A

Security Issues
N/A

Upgrade Notes
N/A

Deprecation Notes
N/A

Other
N/A