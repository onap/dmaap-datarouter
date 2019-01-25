.. _data_router_api_guide:

==========================
Data Router (DR) API Guide
==========================
Introduction
------------

The DataRouter(DR) provisioning API is an HTTPS-based, REST-like API for creating and managing DR feeds
and subscriptions. The DMaaP Data Router System project is intended to provide a common framework by which
data producers can make data available to data consumers and a way for potential consumers to find feeds
with the data they require.


HTTP Service APIs
-----------------

DMaaP Data Router utilizes an HTTP REST API to service all transactions. HTTP and REST standards are followed so
clients as varied as CURL, Java applications and even Web Browsers will work to interact with the Data Router.

General HTTP Requirements
=========================

A DMaaP Data Router transactions consists of 4 distinct segments, HTTP URL, HTTP Header, HTTP Body (POST/PUT)
and HTTP Response. The general considerations for each segment are as follows and are required for each
of the specific transactions described in this section.

HTTP URL
========

http[s]://{serverBaseURL}/{resourcePath}

* The serverBaseURL points to DMaaP Data Router host:port that will service the request.
* The resourcePath specifies the service that the client is attempting to reach.


HTTP Header
===========

Specifies HTTP Headers, such as Content-Type, that define the parameters of the HTTP Transaction

HTTP Body
=========

The HTTP Body contains the feed content when creating a feed.

Create a Feed
-------------

**Description**: Creates a unique feed URL to service the publisher/subscriber model.

Sample Request
==============

``curl -k -X POST -H "Content-Type:application/vnd.dr.feed" -H "X-DR-ON-BEHALF-OF:{user}" --data-ascii @createFeed.json https://{host}:{port}``

Request Parameters:
===================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| name                   | Feed name                       |     Body         |   String   |    <=20      |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| version                | Feed version                    |     Body         |   String   |    <=20      |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| description            | Feed description                |     Body         |   String   |    <=256     |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| business description   | Business description            |     Body         |   String   |    <=256     |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Authorization          | Information for authorizing     |     Body         |   Object   |              |     Y       |                                      |
|                        | publishing requests             |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| suspend                | Set to true if the feed is in   |     Body         |   Boolean  |              |     N       | * true                               |
|                        | the suspended state             |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| group-id               |                                 |     Body         |   Integer  |              |     Y       |                                      |
|                        |                                 |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/vnd.dr.feed              |
|                        | (feed,subscriber,publisher)     |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| X-DR-ON-BEHALF-OF      | User id of owner of feed        |     Header       |   String   |     <=8      |     Y       |  username                            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 201                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * JSON object in request body does not    |
|                        |   conform to the spec.                    |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the requests Content-Type header is not   |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Sample Body
===========
.. code-block:: json

 {
     "name": "Jettydemo",
     "version": "v1.0.0",
     "description": "Jettydemo",
     "business_description": "Jettydemo",
     "suspend": false,
     "changeowner": true,
     "authorization": {
          "classification": "unclassified",
          "endpoint_addrs": ["172.18.0.3","192.167.3.42"],
          "endpoint_ids": [
               {
                    "password": "password",
                    "id": "user"
               }
          ]
     }
 }


Updating a Feed
---------------

**Description**: Update a feed with new parameters.

Sample Request
==============

``curl -k -X PUT -H "Content-Type: application/vnd.dr.feed" -H "X-DR-ON-BEHALF-OF: {user}" --data-ascii @updateFeed.json --location-trusted https://{host}:{port}/feed/{feedId}``

Request Parameters:
===================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| description            | Feed description                |     Body         |   String   |     <=256    |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| business description   | Business description            |     Body         |   String   |     <=256    |     Y       |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Authorization          | Information for authorizing     |     Body         |   Object   |              |     Y       |                                      |
|                        | publishing requests             |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| suspend                | Set to true if the feed is in   |     Body         |   Boolean  |              |     N       | * true                               |
|                        | the suspended state             |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| group-id               |                                 |     Body         |   Integer  |              |     Y       |                                      |
|                        |                                 |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/vnd.dr.feed              |
|                        | (feed,subscriber,publisher)     |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| X-DR-ON-BEHALF-OF      | User id of owner of feed        |     Header       |   String   |     <=8      |     Y       |  username                            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * JSON object in request body does not    |
|                        |   conform to the spec.                    |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Sample Body
===========
.. code-block:: json

 {
     "name": "Jettydemo",
     "version": "v1.0.0",
     "description": "Updated decription",
     "business_description": "Updated business description",
     "suspend": false,
     "changeowner": true,
     "authorization": {
          "classification": "unclassified",
          "endpoint_addrs": ["172.18.0.3","192.167.3.42"],
          "endpoint_ids": [
               {
                    "password": "password",
                    "id": "user"
               }
          ]
     }
 }


Get a Feed
----------

**Description**: Retrieves a representation of the specified feed.

Request URL
===========

http[s]://{host}:{port}/feed/{feedId}

* {feedId}: Id of the feed you want to see a representation of

Sample Request
==============

``curl -k -H "X-DR-ON-BEHALF-OF: {user}" https://{host}:{port}/feed/{feedId}``

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Delete a Feed
-------------

**Description**: Deletes a specified feed

Request URL
===========

http[s]://{host}:{port}/feed/{feedId}

* {feedId}: Id of the feed you want to delete

Sample Request
==============

``curl -k -X DELETE -H "X-DR-ON-BEHALF-OF: {user}" https://{host}:{port}/feed/{feedId}``

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 204                    | Successful query                          |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+


Subscribe to Feed
-----------------

**Description**: Subscribes to a created feed to receive files published to that feed.

Request URL
===========

http[s]://{host}:{port}/subscribe/{feedId}

* {feedId}: Id of the feed to subscribe to

Sample Request
==============

``curl -k -X POST -H "Content-Type:application/vnd.dr.subscription" -H "X-DR-ON-BEHALF-OF:{user}" --data-ascii @addSubscriber.json https://{host}:{port}/subscribe/{feedId}``

Request Parameters:
===================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| feedId                 | ID for the feed you are         |     Path         |   String   |              |     Y       |                                      |
|                        | subscribing to                  |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| delivery               | Address and credentials for     |     Body         |   Object   |              |     Y       |                                      |
|                        | delivery                        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| follow_redirect        | Set to true if feed redirection |     Body         |   Boolean  |              |     Y       | * true                               |
|                        | is expected                     |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| metadata_only          | Set to true if subscription is  |     Body         |   Boolean  |              |     Y       | * true                               |
|                        | to receive per-file metadata    |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| suspend                | Set to true if the subscription |     Body         |   Boolean  |              |     N       | * true                               |
|                        | is in the suspended state       |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| group-id               |                                 |     Body         |   Integer  |              |     Y       |                                      |
|                        |                                 |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/vnd.dr.subscription      |
|                        | (feed,subscriber,publisher)     |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| X-DR-ON-BEHALF-OF      | User id of subscriber           |     Header       |   String   |     <=8      |     Y       |  username                            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 201                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * JSON object in request body does not    |
|                        |   conform to the spec.                    |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the requests Content-Type header is not   |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Sample Body
===========
.. code-block:: json

 {
    "delivery" :{
        "url" : "http://172.18.0.3:7070/",
        "user" : "LOGIN",
        "password" : "PASSWORD",
        "use100" : true
    },
    "metadataOnly" : false,
    "groupid" : 1,
    "subscriber" : "subuser"
 }

Update subscription
-------------------

**Description**: Update a subscription to a feed.

Request URL
===========

http[s]://{host}:{port}/subs/{subId}

* {subId}: Id of the subscription to be updated

Sample Request
==============

``curl -k -X PUT -H "Content-Type:application/vnd.dr.subscription" -H "X-DR-ON-BEHALF-OF:{user}" --data-ascii @updateSubscriber.json https://{host}:{port}/subs/{subId}``

Request Parameters:
===================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| subId                  | ID for the subscription you are |     Path         |   String   |              |     Y       |                                      |
|                        | updating                        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| delivery               | Address and credentials for     |     Body         |   Object   |              |     Y       |                                      |
|                        | delivery                        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| follow_redirect        | Set to true if feed redirection |     Body         |   Boolean  |              |     Y       | * true                               |
|                        | is expected                     |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| metadata_only          | Set to true if subscription is  |     Body         |   Boolean  |              |     Y       | * true                               |
|                        | to receive per-file metadata    |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| suspend                | Set to true if the subscription |     Body         |   Boolean  |              |     N       | * true                               |
|                        | is in the suspended state       |                  |            |              |             | * false                              |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| group-id               |                                 |     Body         |   Integer  |              |     Y       |                                      |
|                        |                                 |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/vnd.dr.subscription      |
|                        | (feed,subscriber,publisher)     |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| X-DR-ON-BEHALF-OF      | User id of subscriber           |     Header       |   String   |     8        |     Y       |  username                            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * JSON object in request body does not    |
|                        |   conform to the spec.                    |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Sample Body
===========
.. code-block:: json

 {
    "delivery" :{
        "url" : "http://192.0.0.1:7070/",
        "user" : "NEW_LOGIN",
        "password" : "NEW_PASSWORD",
        "use100" : true
    },
    "metadataOnly" : false,
    "groupid" : 2,
    "subscriber" : "subuser"
 }


Get a Subscription
------------------

**Description**: Retrieves a representation of the specified subscription.

Request URL
===========

http[s]://{host}:{port}/subs/{subId}

* {subId}: Id of the subscription you want to see a representation of

Sample Request
==============

``curl -k -H "X-DR-ON-BEHALF-OF:{user}" https://{host}:{port}/subs/{subId}``

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Delete a subscription
---------------------

**Description**: Deletes a specified subscription

Request URL
===========

http[s]://{host}:{port}/subs/{subId}

* {subId}: Id of the subscription you want to delete

Sample Request
==============

``curl -k -X DELETE -H "X-DR-ON-BEHALF-OF:{user}" https://{host}:{port}/subs/{subId}``

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 204                    | Successful query                          |
+------------------------+-------------------------------------------+
| 401                    | Indicates that the request was missing the|
|                        | Authorization header or, if the header    |
|                        | was presented, the credentials were not   |
|                        | acceptable                                |
+------------------------+-------------------------------------------+
| 403                    | The request failed authorization.         |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DR-ON-BEHALF-OF identity is not       |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 415                    | Unsupported Media Type - The media type in|
|                        | the request’s Content-Type header is not  |
|                        | appropriate for the request.              |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request.                 |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
| -1                     | Failed Delivery                           |
+------------------------+-------------------------------------------+

Publish to Feed
---------------

**Description**: Publish data to a given feed

Request URL
===========

http[s]://{host}:{port}/publish/{feedId}/{fileName}

* {feedId} The id of the feed you are publishing to.
* {fileId} The name of the file you are publishing to the feed.

Sample Request
==============

``curl -k -X PUT --user {user}:{password} -H "Content-Type:application/octet-stream"  -H "X-DR-META:{\"filetype\":\"txt\"}" --data-binary @sampleFile.txt --post301 --location-trusted https://{host}:{port}/publish/{feedId}/sampleFile``

Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                                                    |
+========================+=================================+==================+============+==============+=============+==========================================================================+
| feedId                 | ID of the feed you are          |     Path         |   String   |              |     Y       |                                                                          |
|                        | publishing to                   |                  |            |              |             |                                                                          |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------------------------------------------+
| fileId                 | Name of the file when it  is    |     Path         |   String   |              |     Y       |                                                                          |
|                        | published to subscribers        |                  |            |              |             |                                                                          |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------------------------------------------+
| content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/octet-stream                                                 |
|                        | format                          |                  |            |              |             |                                                                          |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------------------------------------------+
| X-DR-META              | Metadata for the file. Accepts  |     Header       |   String   |     4096     |     N       | '{"compressionType":"gzip","id": 1234, "transferred":true, "size":null}' |
|                        | only non nested json objects    |                  |            |              |             |                                                                          |
|                        | of the following type :         |                  |            |              |             |                                                                          |
|                        | -Numbers                        |                  |            |              |             |                                                                          |
|                        | -Strings                        |                  |            |              |             |                                                                          |
|                        | -Lowercase boolean              |                  |            |              |             |                                                                          |
|                        | -null                           |                  |            |              |             |                                                                          |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------------------------------------------+

Response/Error Codes
====================

+------------------------+---------------------------------+
| Response statusCode    | Response Description            |
+========================+=================================+
| 204                    | Successful PUT or DELETE        |
+------------------------+---------------------------------+
| 400                    | Failure - Malformed request     |
+------------------------+---------------------------------+
| 401                    | Failure - Request was missing   |
|                        | authorization header, or        |
|                        | credentials were not accepted   |
+------------------------+---------------------------------+
| 403                    | Failure - User could not be     |
|                        | authenticated, or was not       |
|                        | authorized to make the request  |
+------------------------+---------------------------------+
| 404                    | Failure - Path in the request   |
|                        | URL did not point to a valid    |
|                        | feed publishing URL             |
+------------------------+---------------------------------+
| 500                    | Failure - DR experienced an     |
|                        | internal problem                |
+------------------------+---------------------------------+
| 503                    | Failure - DR is not currently   |
|                        | available                       |
+------------------------+---------------------------------+


Delete a Published file
-----------------------

**Description**: Deletes a specified published file

Request URL
===========

http[s]://{host}:{port}/publish/{feedId}/{fileId}

* {feedId}: Id of the feed you want to delete a published file from
* {fileId}: Id of the published file you want to delete

Sample Request
==============

``curl -k -X DELETE --user {user}:{password} --location-trusted https://{host}:{port}/publish/{feedId}/{fileId}``

Response/Error Codes
====================

+------------------------+---------------------------------+
| Response statusCode    | Response Description            |
+========================+=================================+
| 204                    | Successful PUT or DELETE        |
+------------------------+---------------------------------+
| 400                    | Failure - Malformed request     |
+------------------------+---------------------------------+
| 401                    | Failure - Request was missing   |
|                        | authorization header, or        |
|                        | credentials were not accepted   |
+------------------------+---------------------------------+
| 403                    | Failure - User could not be     |
|                        | authenticated, or was not       |
|                        | authorized to make the request  |
+------------------------+---------------------------------+
| 404                    | Failure - Path in the request   |
|                        | URL did not point to a valid    |
|                        | feed publishing URL             |
+------------------------+---------------------------------+
| 500                    | Failure - DR experienced an     |
|                        | internal problem                |
+------------------------+---------------------------------+
| 503                    | Failure - DR is not currently   |
|                        | available                       |
+------------------------+---------------------------------+

Feed logging
------------

**Description**: View logging information for specified feeds, which can be narrowed down with further parameters

Request URL
===========

http[s]://{host}:{port}/feedlog/{feedId}?{queryParameter}

* {feedId} : The id of the feed you want to get logs for
* {queryParameter}: A parameter passed through to narrow the returned logs. Multiple parameters can be passed.


Sample Request
==============

``curl -k https://{host}:{port}/feedlog/{feedId}?statusCode=204``

Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| feedId                 | Id of the feed you want         |     Path         |   String   |              |     N       | 1                                    |
|                        | logs for                        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| type                   | Select records of the           |     Path         |   String   |              |     N       | * pub: Publish attempt               |
|                        | specified type                  |                  |            |              |             | * del: Delivery attempt              |
|                        |                                 |                  |            |              |             | * exp: Delivery expiry               |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| publishId              | Select records with specified   |     Path         |   String   |              |     N       |                                      |
|                        | publish id, carried in the      |                  |            |              |             |                                      |
|                        | X-DR-PUBLISH-ID header from     |                  |            |              |             |                                      |
|                        | original publish request        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| start                  | Select records created at or    |     Path         |   String   |              |     N       | A date-time expressed in the format  |
|                        | after specified date            |                  |            |              |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| end                    | Select records created at or    |     Path         |   String   |              |     N       | A date-time expressed in the format  |
|                        | before specified date           |                  |            |              |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| statusCode             | Select records with the         |     Path         |   String   |              |     N       | An HTTP Integer status code or one   |
|                        | specified statusCode field      |                  |            |              |             | of the following special values:     |
|                        |                                 |                  |            |              |             |                                      |
|                        |                                 |                  |            |              |             | * Success: Any code between 200-299  |
|                        |                                 |                  |            |              |             | * Redirect: Any code between 300-399 |
|                        |                                 |                  |            |              |             | * Failure: Any code > 399            |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| expiryReason           | Select records with the         |     Path         |   String   |              |     N       |                                      |
|                        | specified expiry reason         |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response Parameters
===================

+------------------------+-------------------------------------------+
| Name                   | Description                               |
+========================+===========================================+
| type                   | Record type:                              |
|                        |                                           |
|                        | * pub: publication attempt                |
|                        | * del: delivery attempt                   |
|                        | * exp: delivery expiry                    |
+------------------------+-------------------------------------------+
| date                   | The UTC date and time at which the record |
|                        | was generated, with millisecond resolution|
|                        | in the format specified by RFC 3339       |
+------------------------+-------------------------------------------+
| publishId              | The unique identifier assigned by the DR  |
|                        | at the time of the initial publication    |
|                        | request (carried in the X-DR-PUBLISH-ID   |
|                        | header in the response to the original    |
|                        | publish request)                          |
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
|                        |                                           |
|                        | * notRetryable: The last delivery attempt |
|                        |   encountered an error condition for which|
|                        |   the DR does not make retries.           |
|                        | * retriesExhausted: The DR reached its    |
|                        |   limit for making further retry attempts |
+------------------------+-------------------------------------------+
| attempts               | Total number of attempts made before      |
|                        | delivery attempts were discontinued       |
+------------------------+-------------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * Unrecognized parameter name in query    |
|                        |   string                                  |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The request was not directed  |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| 405                    | Method not allowed - The HTTP method in   |
|                        | the request was something other than GET  |
+------------------------+-------------------------------------------+
| 406                    | Not Acceptable - The request has an Accept|
|                        | header indicating that the requester will |
|                        | not accept a response with                |
|                        | application/vnd.dr.log-list content.      |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | not complete the request                  |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+


Subscription logging
------------------

**Description**: View logging information for specified subscriptions, which can be narrowed down with further parameters

Request URL
===========

http[s]://{host}:{port}/sublog/{subId}?{queryParameter}

* {subId}: The id of the feed you want to get logs from
* {queryParameter}: A parameter passed through to narrow the returned logs. Multiple parameters can be passed.


Sample Request
==============

``curl -k https://{host}:{port}/sublog/{subId}?statusCode=204``

Request parameters
==================

+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+==============+=============+======================================+
| subId                  | Id of the subscription you want |     Path         |   String   |              |     N       | 1                                    |
|                        | logs for                        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| type                   | Select records of the           |     Path         |   String   |              |     N       | * pub: Publish attempt               |
|                        | specified type                  |                  |            |              |             | * del: Delivery attempt              |
|                        |                                 |                  |            |              |             | * exp: Delivery expiry               |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| publishId              | Select records with specified   |     Path         |   String   |              |     N       |                                      |
|                        | publish id, carried in the      |                  |            |              |             |                                      |
|                        | X-DR-PUBLISH-ID header from     |                  |            |              |             |                                      |
|                        | original publish request        |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| start                  | Select records created at or    |     Path         |   String   |              |     N       | A date-time expressed in the format  |
|                        | after specified date            |                  |            |              |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| end                    | Select records created at or    |     Path         |   String   |              |     N       | A date-time expressed in the format  |
|                        | before specified date           |                  |            |              |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| statusCode             | Select records with the         |     Path         |   String   |              |     N       | An Http Integer status code or one   |
|                        | specified statusCode field      |                  |            |              |             | of the following special values:     |
|                        |                                 |                  |            |              |             |                                      |
|                        |                                 |                  |            |              |             | * Success: Any code between 200-299  |
|                        |                                 |                  |            |              |             | * Redirect: Any code between 300-399 |
|                        |                                 |                  |            |              |             | * Failure: Any code > 399            |
|                        |                                 |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+
| expiryReason           | Select records with the         |     Path         |   String   |              |     N       |                                      |
|                        | specified expiry reason         |                  |            |              |             |                                      |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+--------------------------------------+

Response Parameters
===================

+------------------------+-------------------------------------------+
| Name                   | Description                               |
+========================+===========================================+
| type                   | Record type:                              |
|                        |                                           |
|                        | * pub: publication attempt                |
|                        | * del: delivery attempt                   |
|                        | * exp: delivery expiry                    |
+------------------------+-------------------------------------------+
| date                   | The UTC date and time at which the record |
|                        | was generated, with millisecond resolution|
|                        | in the format specified by RFC 3339       |
+------------------------+-------------------------------------------+
| publishId              | The unique identifier assigned by the DR  |
|                        | at the time of the initial publication    |
|                        | request(carried in the X-DR-PUBLISH-ID    |
|                        | header in the response to the original    |
|                        | publish request) to a feed log URL or     |
|                        | subscription log URL known to the system  |
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
|                        |                                           |
|                        | * notRetryable: The last delivery attempt |
|                        |   encountered an error condition for which|
|                        |   the DR does not make retries.           |
|                        | * retriesExhausted: The DR reached its    |
|                        |   limit for making further retry attempts |
+------------------------+-------------------------------------------+
| attempts               | Total number of attempts made before      |
|                        | delivery attempts were discontinued       |
+------------------------+-------------------------------------------+

Response/Error Codes
====================

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200                    | Successful query                          |
+------------------------+-------------------------------------------+
| 400                    | Bad request - The request is defective in |
|                        | some way. Possible causes:                |
|                        |                                           |
|                        | * Unrecognized parameter name in query    |
|                        |   string                                  |
|                        | * Invalid parameter value in query string |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The request was not directed  |
|                        | to a feed log URL or subscription log URL |
|                        | known to the system                       |
+------------------------+-------------------------------------------+
| 405                    | Method not allowed - The HTTP method in   |
|                        | the request was something other than GET  |
+------------------------+-------------------------------------------+
| 406                    | Not Acceptable - The request has an Accept|
|                        | header indicating that the requester will |
|                        | not accept a response with                |
|                        | application/vnd.dr.log-list content.      |
+------------------------+-------------------------------------------+
| 500                    | Internal Server Error - The DR API server |
|                        | encountered an internal error and could   |
|                        | could not complete the request            |
+------------------------+-------------------------------------------+
| 503                    | Service Unavailable - The DR API service  |
|                        | is currently unavailable                  |
+------------------------+-------------------------------------------+
