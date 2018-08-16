==========================
Data Router (DR) API Guide
==========================
Introduction
------------

The DataRouter(DR)provisioning API is an HTTPS-based,REST-like API for creating and managing DR feeds and subscriptions. The Data Routing System project is intended to provide a common framework by which data producers can make data available to data consumers and a way for potential consumers to find feeds with the data they require.


HTTP Service APIs
-----------------

DMaaP Data Router utilizes an HTTP REST API to service all transactions. HTTP and REST standards are followed so
clients as varied as CURL, Java applications and even Web Browsers will
work to interact with the Data Router.

General HTTP Requirements
=========================

A DMaaP Data Router transactions consists of 4 distinct segments,
HTTP URL, HTTP Header, HTTP Body (POST/PUT) and HTTP Response. The general
considerations for each segment are as follows and are required for each
of the specific transactions described in this section.

HTTP URL
========

http[s]://serverBaseURL/{resourcePath}
"*" The serverBaseURL points to DMaaP Data Router host/port that will service the request.
"*" The resourcePath specifies the specific service that the client is attempting to reach.


HTTP Header
===========

Specifies HTTP Headers, such as Content-Type, that define the parameters
of the HTTP Transaction

HTTP Body
=========

The HTTP Body contains the topic content when creating or subscribing
to a feed.The Body may contain topic messages in several formats (like
below) but it must be noted, that, except in very specific circumstances,
messages are not inspected for content.

Create a Feed
-------------

**Description**:Creates a feed that can then be subscribed and published to.

Sample Request
==============

curl -v -X POST -H "Content-Type : application/vnd.att-dr.feed" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addFeed3.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443


Subscribe to Feed
-----------------

**Description**:subscribes to a created feed to receive files published to that feed.

Request URL
===========

https://{HOST:PORT}/subscribe/{feedId}

Sample Request
==============

curl -v -X POST -H "Content-Type: application/vnd.att-dr.subscription" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addSubscriber.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443/subscribe/1

Publish to feed
---------------

**Description**:publish a file to the feed

Request URL
===========

https://{HOST}:{PORT}/publish/{feedId}/[fileId]

* {feedId} is the id of the feed you are publishing to.
* {fileId} is the id of the file you are publishing onto the feed.


Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  data type |   MaxLen     |  Required   |  Format             |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+=====================+======================================+
| feedId                 | Id of the feed you are          |     Path         |   String   |              |     Y       |                     |                                      |
|                        | publishing to                   |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| fileId                 | Id of the file you are          |     Path         |   String   |              |     Y       |                     |                                      |
|                        | publishing onto the feed        |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |     20       |     N       |                     | application/octet-stream             |
|                        | format                          |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+

Response/Error Codes
====================

+------------------------+---------------------------------+
| Response statusCode    | response description            |
+========================+=================================+
| 204                    | successful PUT or DELETE        |
+------------------------+---------------------------------+
| 400                    | failure- malformed request      |
+------------------------+---------------------------------+
| 401                    | failure-request was missing     |
|                        | authorization header, or        |
|                        | credentials were not accepted   |
+------------------------+---------------------------------+
| 403                    | failure- user could not be      |
|                        | authenticated, or was not       |
|                        | authorized to make the request  |
+------------------------+---------------------------------+
| 404                    | failure- path in the request    |
|                        | URL did not point to a valid    |
|                        | feed publishing URL             |
+------------------------+---------------------------------+
| 500                    | failure- DR experienced an      |
|                        | internal problem                |
+------------------------+---------------------------------+
| 503                    | failure- DR is not currently    |
|                        | available                       |
+------------------------+---------------------------------+

Sample Request
==============

curl -v -X PUT --user rs873m:rs873m -H "Content-Type: application/octet-stream" --data-binary @/opt/app/datartr/sampleFile.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443/publish/1/test1

Feed logging
------------

**Description**:view logs on feeds

Request URL
===========


https://{HOST}:{PORT}/feedlog/{feedId}?{queryParameter}

* {feedId} is the id of the feed you wish to get logs from
* {queryParameter} a parameter passed through to narrow the returned logs. multiple parameters can be passed

Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  data type |   MaxLen     |  Required   |  Format             |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+=====================+======================================+
| feedId                 | Id of the feed you want         |     Path         |   String   |              |     N       |                     | 1                                    |
|                        | logs from                       |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| type                   | select records of the           |     Path         |   String   |              |     N       |                     | pub: publish attempt                 |
|                        | specified type                  |                  |            |              |             |                     | del: delivery attempt                |
|                        |                                 |                  |            |              |             |                     | exp: delivery expiry                 |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| publishId              | select records with specified   |     Path         |   String   |              |     N       |                     |                                      |
|                        | publish id, carried in the      |                  |            |              |             |                     |                                      |
|                        | X-ATT-DR-PUBLISH-ID header from |                  |            |              |             |                     |                                      |
|                        | original publish request        |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| start                  | select records created at or    |     Path         |   String   |              |     N       |                     | a date-time expressed in the format  |
|                        | after specified date            |                  |            |              |             |                     | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| end                    | select records created at or    |     Path         |   String   |              |     N       |                     | a date-time expressed in the format  |
|                        | before specified date           |                  |            |              |             |                     | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| statusCode             | select records with the         |     Path         |   String   |              |     N       |                     | an Http Integer status code or one   |
|                        | specified statusCode field      |                  |            |              |             |                     | of the following special values:     |
|                        |                                 |                  |            |              |             |                     | - success: any code between 200-299  |
|                        |                                 |                  |            |              |             |                     | - redirect: any code between 300-399 |
|                        |                                 |                  |            |              |             |                     | - failure: any code > 399            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| expiryReason           | select records with the         |     Path         |   String   |              |     N       |                     |                                      |
|                        | specified expiry reason         |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+

Response Parameters
===================

+------------------------+-------------------------------------------+
| Name                   | Description                               |
+========================+===========================================+
| type                   | Record type:                              |
|                        | pub: publication attempt                  |
|                        | del: delivery attempt                     |
|                        | exp: delivery expiry                      |
+------------------------+-------------------------------------------+
| date                   | The UTC date and time at which the record |
|                        | was generated, with millisecond resolution|
|                        | in the format specified by RFC 3339       |
+------------------------+-------------------------------------------+
| publishId              | The unique identifier assigned by the DR  |
|                        | at the time of the initial publication    |
|                        | request (carried in the X-ATT-DRPUBLISH-ID|
|                        | header in the response to the original    |
|                        | publish request).                         |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| requestURI             | The Request-URI associated with the       |
|                        | request                                   |
+------------------------+-------------------------------------------+
| method                 | The HTTP method (PUT or DELETE) for the   |
|                        | request                                   |
+------------------------+-------------------------------------------+
| contentType            | The media type of the payload of the      |
|                        | request                                   |
+------------------------+-------------------------------------------+
| contentLength          | The size (in bytes) of the payload of     |
|                        | the request                               |
+------------------------+-------------------------------------------+
| sourceIp               | The IP address from which the request     |
|                        | originated                                |
+------------------------+-------------------------------------------+
| endpointId             | The identity used to submit a publish     |
|                        | request to the DR                         |
+------------------------+-------------------------------------------+
| deliveryId             | The identity used to submit a delivery    |
|                        | request to a subscriber endpoint          |
+------------------------+-------------------------------------------+
| statusCode             | The HTTP status code in the response to   |
|                        | the request. A value of -1 indicates that |
|                        | the DR was not able to obtain an HTTP     |
|                        | status code                               |
+------------------------+-------------------------------------------+
| expiryReason           | The reason that delivery attempts were    |
|                        | discontinued:                             |
|                        | - notRetryable: The last delivery attempt |
|                        | encountered an error condition for which  |
|                        | the DR does not make retries.             |
|                        | - retriesExhausted: The DR reached its    |
|                        | limit for making further retry attempts   |
+------------------------+-------------------------------------------+
| attempts               | Total number of attempts made before      |
|                        | delivery attempts were discontinued       |
+------------------------+-------------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | response description                      |
+========================+===========================================+
| 200                    | successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request- the request is defective in  |
|                        | some way.Possible causes:                 |
|                        |                                           |
|                        | - unrecognized parameter name in query    |
|                        |   string                                  |
|                        | - invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 404                    | Not Found- the request was not directed   |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| 405                    | Method not allowed- The HTTP method in    |
|                        | the request was something other than GET  |
+------------------------+-------------------------------------------+
| 406                    | Not Acceptable- The request has an Accept |
|                        | header indicating that the requester will |
|                        | not accept a response with                |
|                        | application/vnd.att-dr.log-list content.  |
+------------------------+-------------------------------------------+
| 500                    | Internal server error- The Dr API server  |
|                        | encountered an internal error and could   |
|                        | could not complete the request            |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable- The Dr API service   |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+

Sample Request
==============

curl -v -k https://172.100.0.3:8443/feedlog/1?statusCode=204

Subscriber logging
------------------

**Description**:view logs on subscriptions

request URL
===========


https://{HOST}:{PORT}/sublog/{subId}?{queryParameter}

* {subId} is the id of the feed you wish to get logs from
* {queryParameter} a parameter passed through to narrow the returned logs. multiple parameters can be passed

Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  data type |   MaxLen     |  Required   |  Format             |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+=====================+======================================+
| subId                  | Id of the feed you want         |     Path         |   String   |              |     N       |                     | 1                                    |
|                        | logs from                       |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| type                   | select records of the           |     Path         |   String   |              |     N       |                     | pub: publish attempt                 |
|                        | specified type                  |                  |            |              |             |                     | del: delivery attempt                |
|                        |                                 |                  |            |              |             |                     | exp: delivery expiry                 |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| publishId              | select records with specified   |     Path         |   String   |              |     N       |                     |                                      |
|                        | publish id, carried in the      |                  |            |              |             |                     |                                      |
|                        | X-ATT-DR-PUBLISH-ID header from |                  |            |              |             |                     |                                      |
|                        | original publish request        |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| start                  | select records created at or    |     Path         |   String   |              |     N       |                     | a date-time expressed in the format  |
|                        | after specified date            |                  |            |              |             |                     | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| end                    | select records created at or    |     Path         |   String   |              |     N       |                     | a date-time expressed in the format  |
|                        | before specified date           |                  |            |              |             |                     | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| statusCode             | select records with the         |     Path         |   String   |              |     N       |                     | an Http Integer status code or one   |
|                        | specified statusCode field      |                  |            |              |             |                     | of the following special values:     |
|                        |                                 |                  |            |              |             |                     |                                      |
|                        |                                 |                  |            |              |             |                     | - success: any code between 200-299  |
|                        |                                 |                  |            |              |             |                     | - redirect: any code between 300-399 |
|                        |                                 |                  |            |              |             |                     | - failure: any code > 399            |
|                        |                                 |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+
| expiryReason           | select records with the         |     Path         |   String   |              |     N       |                     |                                      |
|                        | specified expiry reason         |                  |            |              |             |                     |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+---------------------+--------------------------------------+

Response Parameters
===================

+------------------------+-------------------------------------------+
| Name                   | Description                               |
+========================+===========================================+
| type                   | Record type:                              |
|                        | pub: publication attempt                  |
|                        | del: delivery attempt                     |
|                        | exp: delivery expiry                      |
+------------------------+-------------------------------------------+
| date                   | The UTC date and time at which the record |
|                        | was generated, with millisecond resolution|
|                        | in the format specified by RFC 3339       |
+------------------------+-------------------------------------------+
| publishId              | The unique identifier assigned by the DR  |
|                        | at the time of the initial publication    |
|                        | request (carried in the X-ATT-DRPUBLISH-ID|
|                        | header in the response to the original    |
|                        | publish request).                         |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| requestURI             | The Request-URI associated with the       |
|                        | request                                   |
+------------------------+-------------------------------------------+
| method                 | The HTTP method (PUT or DELETE) for the   |
|                        | request                                   |
+------------------------+-------------------------------------------+
| contentType            | The media type of the payload of the      |
|                        | request                                   |
+------------------------+-------------------------------------------+
| contentLength          | The size (in bytes) of the payload of     |
|                        | the request                               |
+------------------------+-------------------------------------------+
| sourceIp               | The IP address from which the request     |
|                        | originated                                |
+------------------------+-------------------------------------------+
| endpointId             | The identity used to submit a publish     |
|                        | request to the DR                         |
+------------------------+-------------------------------------------+
| deliveryId             | The identity used to submit a delivery    |
|                        | request to a subscriber endpoint          |
+------------------------+-------------------------------------------+
| statusCode             | The HTTP status code in the response to   |
|                        | the request. A value of -1 indicates that |
|                        | the DR was not able to obtain an HTTP     |
|                        | status code                               |
+------------------------+-------------------------------------------+
| expiryReason           | The reason that delivery attempts were    |
|                        | discontinued:                             |
|                        | - notRetryable: The last delivery attempt |
|                        | encountered an error condition for which  |
|                        | the DR does not make retries.             |
|                        | - retriesExhausted: The DR reached its    |
|                        | limit for making further retry attempts   |
+------------------------+-------------------------------------------+
| attempts               | Total number of attempts made before      |
|                        | delivery attempts were discontinued       |
+------------------------+-------------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | response description                      |
+========================+===========================================+
| 200                    | successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request- the request is defective in  |
|                        | some way.Possible causes:                 |
|                        |                                           |
|                        | - unrecognized parameter name in query    |
|                        |   string                                  |
|                        | - invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 404                    | Not Found- the request was not directed   |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| 405                    | Method not allowed- The HTTP method in    |
|                        | the request was something other than GET  |
+------------------------+-------------------------------------------+
| 406                    | Not Acceptable- The request has an Accept |
|                        | header indicating that the requester will |
|                        | not accept a response with                |
|                        | application/vnd.att-dr.log-list content.  |
+------------------------+-------------------------------------------+
| 500                    | Internal server error- The Dr API server  |
|                        | encountered an internal error and could   |
|                        | could not complete the request            |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable- The Dr API service   |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+

Sample Request
==============

curl -v -k https://172.100.0.3:8443/sublog/1?statusCode=204