.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

.. _data_router_api_guide:

.. toctree::
   :maxdepth: 2

Offered APIs
============

**The API Provisioning Model**

The DMaaP Data Router (DR) provisioning API defines two resource types - the feed and the subscription, each with JSON
representations. The API models the provisioning data as a collection of feeds that are known to the DR
(the feeds collection), with each feed containing a collection of the subscriptions to the feed.
The standard HTTP operations (POST, GET, PUT, and DELETE), used in conjunction with these resource
representations, allow an API user to create, get information about, modify, and delete feeds and
subscriptions.

**HTTP Service APIs**

DMaaP Data Router utilizes an HTTPS REST API to service all transactions. HTTPS and REST standards are followed so
clients as varied as CURL, Java applications and even Web Browsers will work to interact with the Data Router.

**General HTTP Requirements**

A DMaaP Data Router transactions consists of 4 distinct segments, HTTP URL, HTTP Header, HTTP Body (POST/PUT)
and HTTP Response. The general considerations for each segment are as follows and are required for each
of the specific transactions described in this section.

**HTTP URL**

http[s]://{serverBaseURL}/{resourcePath}

* The serverBaseURL points to DMaaP Data Router host:port that will service the request.
* The resourcePath specifies the service that the client is attempting to reach.


**HTTP Header**

Specifies HTTP Headers, such as Content-Type, that define the parameters of the HTTP Transaction

**HTTP Body**

The HTTP Body contains the feed content when creating a feed.

**HTTP Authorization**

The user-id:password pair:

* If AAF enabled:  A valid AAF AppId to be authenticated and authorized by the AAF CADI framework.
* If Non AAF    :  When publishing or retracting a file, a valid `EID Object`_ with publish permissions.

Create a Feed
-------------

**Description**: Creates a unique feed URL to service the publisher/subscriber model.

.. code-block:: bash

    POST /

**Request Parameters:**

+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| Field                | Description                    | Param Type | Data Type        | MaxLen | Set By | Updatable? | Required |  Valid/Example Values         |
+======================+================================+============+==================+========+========+============+==========+===============================+
| name                 | Feed name                      | Body       | String           | <=20   | Client | N          | Y        |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| version              | Feed version                   | Body       | String           | <=20   | Client | N          | Y        | v1.0.0                        |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| description          | Feed description               | Body       | String           | <=256  | Client | Y          | N        |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| business description | Business description           | Body       | String           | <=256  | Client | Y          | N        |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| authorization        | Information for authorizing    | Body       |`Auth Object`_    |        | Client | Y          | Y        |                               |
|                      | publishing requests            |            |                  |        |        |            |          |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| suspend              | Set to true if the feed is in  | Body       | Boolean          |        | Client | Y          | N        | * true                        |
|                      | the suspended state            |            |                  |        |        |            |          | * false (default)             |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| group-id             | Auth group for feed management | Body       | Integer          |        | Client | Y          | N        | 0 (default)                   |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| aaf_instance         | The instance passed to aaf     | Body       | String           | <=256  | Client | N          | N        | legacy (default)              |
|                      | during permission checks       |            |                  |        |        |            |          |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| Content-Type         | To specify type of message     | Header     | String           |        | Client | N          | Y        | application/vnd.dmaap-dr.feed |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| publisher            | Publisher identity as passed   | Header     | String           | <=8    | DR     | N          | Y        | username                      |
|                      | in X-DMAAP-DR-ON-BEHALF-OF at  |            |                  |        |        |            |          |                               |
|                      | creation time                  |            |                  |        |        |            |          |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| Authorization        | The user / AppId to be         | Header     | String           |        | Client | N          | Y if AAF | dcae@dcae.onap.org:{password} |
|                      | authorized by the AAF CADI     |            |                  |        |        |            | enabled  |                               |
|                      | framework                      |            |                  |        |        |            |          |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| X-EXCLUDE-AAF        | To determine if the feed to    | Header     | Boolean          |        | Client | N          | Y if AAF | * true (for legacy feed)      |
|                      | create is legacy or AAF        |            |                  |        |        |            | enabled  | * false (for AAF feed)        |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+
| Links                | URLs related to this feed      | Body       |`Feed Links Obj`_ |        | DR     | N          | N        |                               |
+----------------------+--------------------------------+------------+------------------+--------+--------+------------+----------+-------------------------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Consumes**
    application/json

**Produces**
    application/json


**Sample Request**

.. code-block:: bash

    curl -k -X POST -H "Content-Type:application/vnd.dmaap-dr.feed" -H "X-DMAAP-DR-ON-BEHALF-OF:{user}" --data-ascii @createFeed.json https://{host}:{port}

**Sample Body**

.. code-block:: json

    {
      "name": "ONAP Data Feed",
      "version": "v1.0",
      "authorization": {
        "classification": "unclassified",
        "endpoint_addrs": [
        ],
        "endpoint_ids": [
          {
            "id": "dradmin",
            "password": "dradmin"
          }
        ]
      }
    }

**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "groupid": 0,
      "description": "",
      "version": "v1.0",
      "authorization": {
        "endpoint_addrs": [
        ],
        "classification": "unclassified",
        "endpoint_ids": [
          {
            "password": "dradmin",
            "id": "dradmin"
          },
          {
            "password": "demo123456!",
            "id": "onap"
          }
        ]
      },
      "name": "ONAP Data Feed",
      "business_description": "",
      "aaf_instance": "legacy",
      "publisher": "dradmin",
      "links": {
        "subscribe": "https://dmaap-dr-prov/subscribe/1",
        "log": "https://dmaap-dr-prov/feedlog/1",
        "publish": "https://dmaap-dr-prov/publish/1",
        "self": "https://dmaap-dr-prov/feed/1"
      }
    }



Update a Feed
-------------

**Description**: Update a feed with new parameters.

.. code-block:: bash

    PUT /feed/{feedId}


**Request Parameters:**

+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Field                  | Description                     |  Param Type |  Data Type    |  MaxLen    |  Required   |
+========================+=================================+=============+===============+============+=============+
| name                   | Feed name                       |     Body    |   String      |   <=20     |     Y       |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| description            | Feed description                |     Body    |   String      |   <=256    |     N       |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| business description   | Business description            |     Body    |   String      |   <=256    |     N       |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| authorization          | Information for authorizing     |     Body    |`Auth Object`_ |            |     Y       |
|                        | publishing requests             |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| suspend                | Set to true if the feed is in   |     Body    |   Boolean     |            |     N       |
|                        | the suspended state             |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| group-id               | Auth group for feed management  |     Body    |   Integer     |            |     N       |
|                        |                                 |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Content-type           | To specify type of message      |    Header   |   String      |            |     Y       |
|                        | (feed,subscriber,publisher)     |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| publisher              | Publisher identity as passed    |    Header   |   String      |   <=8      |     Y       |
|                        | in X-DMAAP-DR-ON-BEHALF-OF at   |             |               |            |             |
|                        | creation time                   |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Authorization          | The user / AppId to be          |    Header   |   String      |            | Y if AAF    |
|                        | authorized by the AAF CADI      |             |               |            | enabled     |
|                        | framework                       |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Consumes**
    application/json

**Produces**
    application/json


**Sample Request**

.. code-block:: bash

    curl -k -X PUT -H "Content-Type: application/vnd.dmaap-dr.feed" -H "X-DMAAP-DR-ON-BEHALF-OF: {user}" --data-ascii @updateFeed.json --location-trusted https://{host}:{port}/feed/{feedId}

**Sample Body**

.. code-block:: json

    {
      "name": "ONAP Data Feed",
      "business_description": "Updated ONAP Feed",
      "groupid": 33,
      "description": "Updated ONAP Feed",
      "authorization": {
        "endpoint_addrs": [
          "10.1.2.3"
        ],
        "classification": "unclassified",
        "endpoint_ids": [
          {
            "password": "dradmin",
            "id": "dradmin"
          },
          {
            "password": "demo123456!",
            "id": "onap"
          }
        ]
      }
    }

**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "groupid": 33,
      "description": "Updated ONAP Feed",
      "authorization": {
        "endpoint_addrs": [
          "10.1.2.3"
        ],
        "classification": "unclassified",
        "endpoint_ids": [
          {
            "password": "dradmin",
            "id": "dradmin"
          },
          {
            "password": "demo123456!",
            "id": "onap"
          }
        ]
      },
      "name": "ONAP Data Feed1",
      "business_description": "Updated ONAP Feed",
      "aaf_instance": "legacy",
      "publisher": "dradmin",
      "links": {
        "subscribe": "https://dmaap-dr-prov/subscribe/1",
        "log": "https://dmaap-dr-prov/feedlog/1",
        "publish": "https://dmaap-dr-prov/publish/1",
        "self": "https://dmaap-dr-prov/feed/1"
      }
    }



Get a Feed
----------

**Description**: Retrieves a representation of the specified feed.

.. code-block:: bash

    GET /feed/{feedId}


**Request Parameters:**

+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Field                  | Description                     |  Param Type |  Data Type    |  MaxLen    |  Required   |
+========================+=================================+=============+===============+============+=============+
| publisher              | Publisher identity as passed    |    Header   |   String      |   <=8      |     Y       |
|                        | in X-DMAAP-DR-ON-BEHALF-OF at   |             |               |            |             |
|                        | creation time                   |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Authorization          | The user / AppId to be          |    Header   |   String      |            | Y if AAF    |
|                        | authorized by the AAF CADI      |             |               |            | enabled     |
|                        | framework                       |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Produces**
    application/json

**Sample Request**

.. code-block:: bash

    curl -k -H "X-DMAAP-DR-ON-BEHALF-OF: {user}" https://{host}:{port}/feed/{feedId}

**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "groupid": 33,
      "description": "Updated ONAP Feed",
      "version": "v1.0",
      "authorization": {
        "endpoint_addrs": [
          "10.1.2.3",
          "173.2.33.4"
        ],
        "classification": "unclassified",
        "endpoint_ids": [
          {
            "password": "dradmin",
            "id": "dradmin"
          },
          {
            "password": "demo123456!",
            "id": "onap"
          }
        ]
      },
      "name": "ONAP Data Feed",
      "business_description": "Updated ONAP Feed",
      "aaf_instance": "legacy",
      "publisher": "dradmin",
      "links": {
        "subscribe": "https://dmaap-dr-prov/subscribe/1",
        "log": "https://dmaap-dr-prov/feedlog/1",
        "publish": "https://dmaap-dr-prov/publish/1",
        "self": "https://dmaap-dr-prov/feed/1"
      }
    }


Delete a Feed
-------------

**Description**: Deletes a specified feed

.. code-block:: bash

    DELETE /feed/{feedId}


**Request Parameters:**

+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Field                  | Description                     |  Param Type |  Data Type    |  MaxLen    |  Required   |
+========================+=================================+=============+===============+============+=============+
| publisher              | Publisher identity as passed    |    Header   |   String      |   <=8      |     Y       |
|                        | in X-DMAAP-DR-ON-BEHALF-OF at   |             |               |            |             |
|                        | creation time                   |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+
| Authorization          | The user / AppId to be          |    Header   |   String      |            | Y if AAF    |
|                        | authorized by the AAF CADI      |             |               |            | enabled     |
|                        | framework                       |             |               |            |             |
+------------------------+---------------------------------+-------------+---------------+------------+-------------+

**Response Codes**

* Success:
    204

* Error:
    See `Response Codes`_

**Sample Request**
.. code-block:: bash

    curl -k -X DELETE -H "X-DMAAP-DR-ON-BEHALF-OF: {user}" https://{host}:{port}/feed/{feedId}


Subscribe to Feed
-----------------

**Description**: Subscribes to a created feed to receive files published to that feed.

.. code-block:: bash

    POST /subscribe/{feedId}


**Request Parameters:**

+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| Field           | Description                     |  Param Type   |  Data Type      | MaxLen | Set By | Updatable? | Required |  Valid/Example Values                |
+=================+=================================+===============+=================+========+========+============+==========+======================================+
| feedId          | ID for the feed you are         |     Path      |   String        |        | Client |     N      |     Y    | 1                                    |
|                 | subscribing to                  |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| delivery        | Address and credentials for     |     Body      | `Del Object`_   |        | Client |     Y      |     Y    |                                      |
|                 | delivery                        |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| follow_redirect | Set to true if feed redirection |     Body      |   Boolean       |        | Client |     Y      |     N    | * true                               |
|                 | is expected                     |               |                 |        |        |            |          | * false (default)                    |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| metadata_only   | Set to true if subscription is  |     Body      |   Boolean       |        | Client |     Y      |     Y    | * true                               |
|                 | to receive per-file metadata    |               |                 |        |        |            |          | * false                              |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| suspend         | Set to true if the subscription |     Body      |   Boolean       |        | Client |     Y      |     N    | * true                               |
|                 | is in the suspended state       |               |                 |        |        |            |          | * false (default)                    |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| decompress      | Set to true if the data is to   |     Body      |   Boolean       |        | Client |     Y      |     N    | * true                               |
|                 | be decompressed for subscriber  |               |                 |        |        |            |          | * false (default)                    |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| group-id        | Auth group for sub management   |     Body      |   Integer       |        | Client |     Y      |     Y    | 22                                   |
|                 |                                 |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| aaf_instance    | The instance passed to aaf      |     Body      |   String        | <=256  | Client |     N      |     N    | * legacy (default)                   |
|                 | during permission checks        |               |                 |        |        |            |          |                                      |
|                 |                                 |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| Content-type    | To specify type of message      |     Header    |   String        |        | Client |     N      |     Y    | application/vnd.dmaap-dr.subscription|
|                 | (feed,subscriber,publisher)     |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| subscriber      | Subscriber identity as passed   |     Header    |   String        |   <=8  |  DR    |     N      |     Y    | username                             |
|                 | in X-DMAAP-DR-ON-BEHALF-OF at   |               |                 |        |        |            |          |                                      |
|                 | creation time                   |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| Authorization   | The user / AppId to be          |    Header     |   String        |        | Client |     N      | Y if AAF | dcae@dcae.onap.org:{password}        |
|                 | authorized by the AAF CADI      |               |                 |        |        |            | enabled  |                                      |
|                 | framework                       |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| X-EXCLUDE-AAF   | To determine if the feed to     |    Header     |   Boolean       |        | Client |     N      | Y if AAF | * true (for legacy feed)             |
|                 | create is legacy or AAF         |               |                 |        |        |            | enabled  | * false (for AAF feed)               |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+
| Links           | URLs related to this            |     Body      |`Sub Links Obj`_ |        |  DR    |     N      |     N    |                                      |
|                 | subscription                    |               |                 |        |        |            |          |                                      |
+-----------------+---------------------------------+---------------+-----------------+--------+--------+------------+----------+--------------------------------------+

**Response Codes**

* Success:
    201

* Error:
    See `Response Codes`_

**Consumes**
    application/json

**Produces**
    application/json


**Sample Request**

.. code-block:: bash

    curl -k -X POST -H "Content-Type:application/vnd.dmaap-dr.subscription" -H "X-DMAAP-DR-ON-BEHALF-OF:{user}" --data-ascii @addSubscriber.json https://{host}:{port}/subscribe/{feedId}

**Sample Body**

.. code-block:: json

    {
      "delivery": {
        "url": "http://dmaap-dr-subscriber:7070/",
        "user": "LOGIN",
        "password": "PASSWORD",
        "use100": true
      },
      "metadataOnly": false,
      "groupid": 22,
      "follow_redirect": true,
      "suspend": false,
      "decompress": true
    }

**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "delivery": {
        "use100": true,
        "password": "PASSWORD",
        "user": "LOGIN",
        "url": "http://dmaap-dr-subscriber:7070/"
      },
      "subscriber": "onap",
      "groupid": 1,
      "metadataOnly": false,
      "follow_redirect": true,
      "decompress": true,
      "aaf_instance": "legacy",
      "links": {
        "feed": "https://dmaap-dr-prov/feed/1",
        "log": "https://dmaap-dr-prov/sublog/1",
        "self": "https://dmaap-dr-prov/subs/1"
      },
      "created_date": 1553707279509
    }



Update subscription
-------------------

**Description**: Update a subscription to a feed.

.. code-block:: bash

    PUT /subs/{subId}


**Request Parameters:**

+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Field           | Description                     |  Param Type  |  Data Type    | MaxLen |  Required   |
+=================+=================================+==============+===============+========+=============+
| subId           | ID for the subscription you are |     Path     |   String      |        |     Y       |
|                 | updating                        |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| delivery        | Address and credentials for     |     Body     | `Del Object`_ |        |     Y       |
|                 | delivery                        |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| follow_redirect | Set to true if feed redirection |     Body     |   Boolean     |        |     N       |
|                 | is expected                     |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| metadata_only   | Set to true if subscription is  |     Body     |   Boolean     |        |     Y       |
|                 | to receive per-file metadata    |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| suspend         | Set to true if the subscription |     Body     |   Boolean     |        |     N       |
|                 | is in the suspended state       |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| decompress      | Set to true if the data is to   |     Body     |   Boolean     |        |     N       |
|                 | be decompressed for subscriber  |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| group-id        | Auth group for sub management   |     Body     |   Integer     |        |     Y       |
|                 |                                 |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Content-type    | To specify type of message      |     Header   |   String      |        |     Y       |
|                 | (feed,subscriber,publisher)     |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| subscriber      | Subscriber identity as passed   |     Header   |   String      |  <=8   |     Y       |
|                 | in X-DMAAP-DR-ON-BEHALF-OF at   |              |               |        |             |
|                 | creation time                   |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Authorization   | The user / AppId to be          |    Header    |   String      |        |  Y if AAF   |
|                 | authorized by the AAF CADI      |              |               |        |  enabled    |
|                 | framework                       |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| X-EXCLUDE-AAF   | To determine if the feed to     |    Header    |   Boolean     |        |  Y if AAF   |
|                 | create is legacy or AAF         |              |               |        |  enabled    |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Consumes**
    application/json

**Produces**
    application/json

**Sample Request**

.. code-block:: bash

    curl -k -X PUT -H "Content-Type:application/vnd.dmaap-dr.subscription" -H "X-DMAAP-DR-ON-BEHALF-OF:{user}" --data-ascii @updateSubscriber.json https://{host}:{port}/subs/{subId}

**Sample Body**

.. code-block:: json

    {
      "delivery": {
        "url": "http://dmaap-dr-subscriber:7070/",
        "user": "NEW_LOGIN",
        "password": "NEW_PASSWORD",
        "use100": false
      },
      "metadataOnly": true,
      "groupid": 67,
      "follow_redirect": false,
      "decompress": false
    }


**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "delivery": {
        "use100": false,
        "password": "NEW_PASSWORD",
        "user": "NEW_LOGIN",
        "url": "http://dmaap-dr-subscriber:7070/"
      },
      "subscriber": "onap",
      "groupid": 67,
      "metadataOnly": true,
      "follow_redirect": false,
      "decompress": false,
      "aaf_instance": "legacy",
      "links": {
        "feed": "https://dmaap-dr-prov/feed/1",
        "log": "https://dmaap-dr-prov/sublog/1",
        "self": "https://dmaap-dr-prov/subs/1"
      },
      "created_date": 1553714446614
    }



Get a Subscription
------------------

**Description**: Retrieves a representation of the specified subscription.

.. code-block:: bash

    GET /subs/{subId}


**Request Parameters:**

+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Field           | Description                     |  Param Type  |  Data Type    | MaxLen |  Required   |
+=================+=================================+==============+===============+========+=============+
| subscriber      | Subscriber identity as passed   |    Header    |   String      |  <=8   |     Y       |
|                 | in X-DMAAP-DR-ON-BEHALF-OF at   |              |               |        |             |
|                 | creation time                   |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Authorization   | The user / AppId to be          |    Header    |   String      |        |  Y if AAF   |
|                 | authorized by the AAF CADI      |              |               |        |  enabled    |
|                 | framework                       |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Produces**
    application/json

**Sample Request**

.. code-block:: bash

    curl -k -H "X-DMAAP-DR-ON-BEHALF-OF:{user}" https://{host}:{port}/subs/{subId}

**Sample Response**

.. code-block:: json

    {
      "suspend": false,
      "delivery": {
        "use100": false,
        "password": "NEW_PASSWORD",
        "user": "NEW_LOGIN",
        "url": "http://dmaap-dr-subscriber:7070/"
      },
      "subscriber": "onap",
      "groupid": 67,
      "metadataOnly": true,
      "privilegedSubscriber": false,
      "follow_redirect": false,
      "decompress": false,
      "aaf_instance": "legacy",
      "links": {
        "feed": "https://dmaap-dr-prov/feed/2",
        "log": "https://dmaap-dr-prov/sublog/6",
        "self": "https://dmaap-dr-prov/subs/6"
      }
    }



Delete a subscription
---------------------

**Description**: Deletes a specified subscription

.. code-block:: bash

    DELETE /subs/{subId}


**Request Parameters:**

+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Field           | Description                     |  Param Type  |  Data Type    | MaxLen |  Required   |
+=================+=================================+==============+===============+========+=============+
| subscriber      | Subscriber identity as passed   |     Header   |   String      |  <=8   |     Y       |
|                 | in X-DMAAP-DR-ON-BEHALF-OF at   |              |               |        |             |
|                 | creation time                   |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+
| Authorization   | The user / AppId to be          |    Header    |   String      |        |  Y if AAF   |
|                 | authorized by the AAF CADI      |              |               |        |  enabled    |
|                 | framework                       |              |               |        |             |
+-----------------+---------------------------------+--------------+---------------+--------+-------------+

**Response Codes**

* Success:
    204

* Error:
    See `Response Codes`_

**Sample Request**

.. code-block:: bash

    curl -k -X DELETE -H "X-DMAAP-DR-ON-BEHALF-OF:{user}" https://{host}:{port}/subs/{subId}


Publish to Feed
---------------

**Description**: Publish data to a given feed

.. code-block:: bash

    PUT /publish/{feedId}/{fileId}


**Request parameters**

+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |   MaxLen     |  Required   |  Valid/Example Values                     |
+========================+=================================+==================+============+==============+=============+===========================================+
| feedId                 | ID of the feed you are          |     Path         |   String   |              |     Y       |                                           |
|                        | publishing to                   |                  |            |              |             |                                           |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+
| fileId                 | Name of the file when it is     |     Path         |   String   |              |     Y       |                                           |
|                        | published to subscribers        |                  |            |              |             |                                           |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+
| Content-type           | To specify type of message      |     Header       |   String   |              |     Y       | application/octet-stream                  |
|                        | format                          |                  |            |              |             |                                           |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+
| X-DMAAP-DR-META        | Metadata for the file. Accepts  |     Header       |   String   |   <=4096     |     N       | '{“compressionType”: ”gzip”,              |
|                        | only non nested json objects    |                  |            |              |             |   ”id”: 1234,                             |
|                        | of the following type:          |                  |            |              |             |   “transferred”: true,                    |
|                        |                                 |                  |            |              |             |   “size”: null }’                         |
|                        | * Numbers                       |                  |            |              |             |                                           |
|                        | * Strings                       |                  |            |              |             |                                           |
|                        | * Lowercase boolean             |                  |            |              |             |                                           |
|                        | * null                          |                  |            |              |             |                                           |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+
| Authorization          | An `EID Object`_ with publish   |     Header       |  String    |              |     Y       | * (legacy Feed) dradmin:dradmin           |
|                        | permissions.                    |                  |            |              |             | * (AAF Feed) dcae@dcae.onap.org:{password}|
|                        | If AAF CADI is enabled, use a   |                  |            |              |             |                                           |
|                        | valid AAF user/AppId instead.   |                  |            |              |             |                                           |
+------------------------+---------------------------------+------------------+------------+--------------+-------------+-------------------------------------------+

**Response Codes**

* Success:
    204

* Error:
    See `Response Codes`_

**Sample Request**

.. code-block:: bash

    curl -k -X PUT --user {user}:{password} -H "Content-Type:application/octet-stream"  -H "X-DMAAP-DR-META:{\"filetype\":\"txt\"}" --data-binary @sampleFile.txt --post301 --location-trusted https://{host}:{port}/publish/{feedId}/{fileId}



Delete/Retract a Published file
-------------------------------

**Description**: Deletes/retracts a specified published file

.. code-block:: bash

    DELETE /publish/{feedId}/{fileId}


**Request Parameters:**

+-----------------+---------------------------------+--------------+---------------+------------+-------------------------------------------+
| Field           | Description                     |  Param Type  |  Data Type    | Required   |  Valid/Example Values                     |
+=================+=================================+==============+===============+============+===========================================+
| Authorization   | An `EID Object`_ with publish   |   Header     |    String     |    Y       | * (legacy Feed) dradmin:dradmin           |
|                 | permissions.                    |              |               |            | * (AAF Feed) dcae@dcae.onap.org:{password}|
|                 | If AAF CADI is enabled, use a   |              |               |            |                                           |
|                 | valid AAF user/AppId instead.   |              |               |            |                                           |
+-----------------+---------------------------------+--------------+---------------+------------+-------------------------------------------+
| feedId          | ID of the feed that was         |     Path     |    String     |    Y       |                                           |
|                 | publishing to                   |              |               |            |                                           |
+-----------------+---------------------------------+--------------+---------------+------------+-------------------------------------------+
| fileId          | Name of the file when it was    |     Path     |    String     |    Y       |                                           |
|                 | published to subscribers        |              |               |            |                                           |
+-----------------+---------------------------------+--------------+---------------+------------+-------------------------------------------+

**Response Codes**

* Success:
    204

* Error:
    See `Response Codes`_


**Sample Request**

.. code-block:: bash

    curl -k -X DELETE --user {user}:{password} --location-trusted https://{host}:{port}/publish/{feedId}/{fileId}



Feed logging
------------

**Description**: View logging information for specified feeds, which can be narrowed down with further parameters

.. code-block:: bash

    GET /feedlog/{feedId}?{queryParam}


**Request parameters**

+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+=============+======================================+
| feedId                 | Id of the feed you want         |     Path         |   String   |     Y       | 1                                    |
|                        | logs for                        |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| type                   | Select records of the           |     Path         |   String   |     N       | * pub: Publish attempt               |
|                        | specified type                  |                  |            |             | * del: Delivery attempt              |
|                        |                                 |                  |            |             | * exp: Delivery expiry               |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| publishId              | Select records with specified   |     Path         |   String   |     N       |                                      |
|                        | publish id, carried in the      |                  |            |             |                                      |
|                        | X-DMAAP-DR-PUBLISH-ID header    |                  |            |             |                                      |
|                        | from original publish request   |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| start                  | Select records created at or    |     Path         |   String   |     N       | A date-time expressed in the format  |
|                        | after specified date            |                  |            |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| end                    | Select records created at or    |     Path         |   String   |     N       | A date-time expressed in the format  |
|                        | before specified date           |                  |            |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| statusCode             | Select records with the         |     Path         |   String   |     N       | An HTTP Integer status code or one   |
|                        | specified statusCode field      |                  |            |             | of the following special values:     |
|                        |                                 |                  |            |             |                                      |
|                        |                                 |                  |            |             | * Success: Any code between 200-299  |
|                        |                                 |                  |            |             | * Redirect: Any code between 300-399 |
|                        |                                 |                  |            |             | * Failure: Any code > 399            |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| expiryReason           | Select records with the         |     Path         |   String   |     N       |                                      |
|                        | specified expiry reason         |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| filename               | Select published records with   |     Path         |   String   |     N       |                                      |
|                        | the specified filename          |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+

**Response Parameters**

+------------------------+----------------------------------------------+
| Name                   | Description                                  |
+========================+==============================================+
| type                   | Record type:                                 |
|                        |                                              |
|                        | * pub: publication attempt                   |
|                        | * del: delivery attempt                      |
|                        | * exp: delivery expiry                       |
+------------------------+----------------------------------------------+
| date                   | The UTC date and time at which the record    |
|                        | was generated, with millisecond resolution   |
|                        | in the format specified by RFC 3339          |
+------------------------+----------------------------------------------+
| publishId              | The unique identifier assigned by the DR     |
|                        | at the time of the initial publication       |
|                        | request (carried in the X-DMAAP-DR-PUBLISH-ID|
|                        | header in the response to the original       |
|                        | publish request)                             |
+------------------------+----------------------------------------------+
| requestURI             | The Request-URI associated with the          |
|                        | request                                      |
+------------------------+----------------------------------------------+
| method                 | The HTTP method (PUT or DELETE) for the      |
|                        | request                                      |
+------------------------+----------------------------------------------+
| contentType            | The media type of the payload of the         |
|                        | request                                      |
+------------------------+----------------------------------------------+
| contentLength          | The size (in bytes) of the payload of        |
|                        | the request                                  |
+------------------------+----------------------------------------------+
| sourceIp               | The IP address from which the request        |
|                        | originated                                   |
+------------------------+----------------------------------------------+
| endpointId             | The identity used to submit a publish        |
|                        | request to the DR                            |
+------------------------+----------------------------------------------+
| deliveryId             | The identity used to submit a delivery       |
|                        | request to a subscriber endpoint             |
+------------------------+----------------------------------------------+
| statusCode             | The HTTP status code in the response to      |
|                        | the request. A value of -1 indicates that    |
|                        | the DR was not able to obtain an HTTP        |
|                        | status code                                  |
+------------------------+----------------------------------------------+
| expiryReason           | The reason that delivery attempts were       |
|                        | discontinued:                                |
|                        |                                              |
|                        | * notRetryable: The last delivery attempt    |
|                        |   encountered an error condition for which   |
|                        |   the DR does not make retries.              |
|                        | * retriesExhausted: The DR reached its       |
|                        |   limit for making further retry attempts    |
+------------------------+----------------------------------------------+
| attempts               | Total number of attempts made before         |
|                        | delivery attempts were discontinued          |
+------------------------+----------------------------------------------+
| filename               | File name associated with a publish record   |
+------------------------+----------------------------------------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Produces**
    application/json


**Sample Request**

.. code-block:: bash

    curl -k https://{host}:{port}/feedlog/{feedId}?statusCode=204``

**Sample Response**

.. code-block:: json

    [
      {
        "statusCode": 204,
        "publishId": "1553715307322.dmaap-dr-node",
        "requestURI": "https://dmaap-dr-node/publish/1/hello",
        "sourceIP": "172.19.0.1",
        "method": "PUT",
        "contentType": "application/octet-stream",
        "endpointId": "dradmin",
        "type": "pub",
        "date": "2019-03-27T19:35:07.324Z",
        "contentLength": 14,
        "fileName": "hello"
      },
      {
        "statusCode": 204,
        "publishId": "1553715312071.dmaap-dr-node",
        "requestURI": "https://dmaap-dr-node/publish/2/hello",
        "sourceIP": "172.19.0.1",
        "method": "PUT",
        "contentType": "application/octet-stream",
        "endpointId": "onap",
        "type": "pub",
        "date": "2019-03-27T19:35:12.072Z",
        "contentLength": 14,
        "fileName": "hello2"
      }
    ]


Subscription logging
--------------------

**Description**: View logging information for specified subscriptions, which can be narrowed down with further parameters

.. code-block:: bash

    GET /sublog/{subId}?{queryParam}


**Request parameters**

+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| Name                   | Description                     |  Param Type      |  Data Type |  Required   |  Valid/Example Values                |
+========================+=================================+==================+============+=============+======================================+
| subId                  | Id of the subscription you want |     Path         |   String   |     Y       | 1                                    |
|                        | logs for                        |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| type                   | Select records of the           |     Path         |   String   |     N       | * pub: Publish attempt               |
|                        | specified type                  |                  |            |             | * del: Delivery attempt              |
|                        |                                 |                  |            |             | * exp: Delivery expiry               |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| publishId              | Select records with specified   |     Path         |   String   |     N       |                                      |
|                        | publish id, carried in the      |                  |            |             |                                      |
|                        | X-DMAAP-DR-PUBLISH-ID header    |                  |            |             |                                      |
|                        | from original publish request   |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| start                  | Select records created at or    |     Path         |   String   |     N       | A date-time expressed in the format  |
|                        | after specified date            |                  |            |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| end                    | Select records created at or    |     Path         |   String   |     N       | A date-time expressed in the format  |
|                        | before specified date           |                  |            |             | specified by RFC 3339                |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| statusCode             | Select records with the         |     Path         |   String   |     N       | An Http Integer status code or one   |
|                        | specified statusCode field      |                  |            |             | of the following special values:     |
|                        |                                 |                  |            |             |                                      |
|                        |                                 |                  |            |             | * Success: Any code between 200-299  |
|                        |                                 |                  |            |             | * Redirect: Any code between 300-399 |
|                        |                                 |                  |            |             | * Failure: Any code > 399            |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+
| expiryReason           | Select records with the         |     Path         |   String   |     N       |                                      |
|                        | specified expiry reason         |                  |            |             |                                      |
+------------------------+---------------------------------+------------------+------------+-------------+--------------------------------------+

Response Parameters
-------------------

+------------------------+---------------------------------------------+
| Name                   | Description                                 |
+========================+=============================================+
| type                   | Record type:                                |
|                        |                                             |
|                        | * pub: publication attempt                  |
|                        | * del: delivery attempt                     |
|                        | * exp: delivery expiry                      |
+------------------------+---------------------------------------------+
| date                   | The UTC date and time at which the record   |
|                        | was generated, with millisecond resolution  |
|                        | in the format specified by RFC 3339         |
+------------------------+---------------------------------------------+
| publishId              | The unique identifier assigned by the DR    |
|                        | at the time of the initial publication      |
|                        | request(carried in the X-DMAAP-DR-PUBLISH-ID|
|                        | header in the response to the original      |
|                        | publish request) to a feed log URL or       |
|                        | subscription log URL known to the system    |
+------------------------+---------------------------------------------+
| requestURI             | The Request-URI associated with the         |
|                        | request                                     |
+------------------------+---------------------------------------------+
| method                 | The HTTP method (PUT or DELETE) for the     |
|                        | request                                     |
+------------------------+---------------------------------------------+
| contentType            | The media type of the payload of the        |
|                        | request                                     |
+------------------------+---------------------------------------------+
| contentLength          | The size (in bytes) of the payload of       |
|                        | the request                                 |
+------------------------+---------------------------------------------+
| sourceIp               | The IP address from which the request       |
|                        | originated                                  |
+------------------------+---------------------------------------------+
| endpointId             | The identity used to submit a publish       |
|                        | request to the DR                           |
+------------------------+---------------------------------------------+
| deliveryId             | The identity used to submit a delivery      |
|                        | request to a subscriber endpoint            |
+------------------------+---------------------------------------------+
| statusCode             | The HTTP status code in the response to     |
|                        | the request. A value of -1 indicates that   |
|                        | the DR was not able to obtain an HTTP       |
|                        | status code                                 |
+------------------------+---------------------------------------------+
| expiryReason           | The reason that delivery attempts were      |
|                        | discontinued:                               |
|                        |                                             |
|                        | * notRetryable: The last delivery attempt   |
|                        |   encountered an error condition for which  |
|                        |   the DR does not make retries.             |
|                        | * retriesExhausted: The DR reached its      |
|                        |   limit for making further retry attempts   |
+------------------------+---------------------------------------------+
| attempts               | Total number of attempts made before        |
|                        | delivery attempts were discontinued         |
+------------------------+---------------------------------------------+

**Response Codes**

* Success:
    200

* Error:
    See `Response Codes`_

**Produces**
    application/json

**Sample Request**

.. code-block:: bash

    curl -k https://{host}:{port}/sublog/{subId}?statusCode=204

**Sample Response**

.. code-block:: json

    [
      {
        "statusCode": 204,
        "publishId": "1553715307322.dmaap-dr-node",
        "requestURI": "https://dmaap-dr-node/publish/1/hello",
        "sourceIP": "172.19.0.1",
        "method": "PUT",
        "contentType": "application/octet-stream",
        "endpointId": "dradmin",
        "type": "pub",
        "date": "2019-03-27T19:35:07.324Z",
        "contentLength": 14,
        "fileName": "hello"
      },
      {
        "statusCode": 204,
        "publishId": "1553715312071.dmaap-dr-node",
        "requestURI": "https://dmaap-dr-node/publish/2/hello",
        "sourceIP": "172.19.0.1",
        "method": "PUT",
        "contentType": "application/octet-stream",
        "endpointId": "onap",
        "type": "pub",
        "date": "2019-03-27T19:35:12.072Z",
        "contentLength": 14,
        "fileName": "hello2"
      }
    ]


**Feed Authorization Object**

.. _`Auth Object`:

+----------------+-----------------+--------------------------------+------------------------------+
| Field          | Type            | Description                    | Restrictions                 |
+================+=================+================================+==============================+
| classification | string          | An indicator of the feed’s     | Length <=32                  |
|                |                 | data security classification   |                              |
+----------------+-----------------+--------------------------------+------------------------------+
| endpoint_ids   |`EID Object`_ [] | Array of objects defining the  | At least 1 id in the array   |
|                |                 | identities that are allowed    |                              |
|                |                 | to publish to this feed        |                              |
+----------------+-----------------+--------------------------------+------------------------------+
| endpoint_addrs | string[]        | Array of IP addresses or IP    | Each string must be a valid  |
|                |                 | subnetwork addresses that      | textual representation of    |
|                |                 | are allowed to publish to this | IPv4 or IPv6 host address or |
|                |                 | feed; an empty array indicates | subnetwork address.          |
|                |                 | that publish requests are      |                              |
|                |                 | permitted from any IP address  |                              |
+----------------+-----------------+--------------------------------+------------------------------+


**Endpoint Identity Object**

.. _`EID Object`:

+----------+--------+--------------------------+--------------+
| Field    | Type   | Description              | Restrictions |
+==========+========+==========================+==============+
| id       | string | Publishing endpoint      | Length <= 20 |
|          |        | identifier               |              |
+----------+--------+--------------------------+--------------+
| password | string | Password associated with | Length <= 32 |
|          |        | id                       |              |
+----------+--------+--------------------------+--------------+


**Feed Links Object**

.. _`Feed Links Obj`:

+-----------+---------------------------------------------------+----------------+
| Field     | Description                                       | Symbolic Name  |
+===========+===================================================+================+
| self      | URL pointing to this feed, used for updating and  | <feedURL>      |
|           | deleting the feed.                                |                |
+-----------+---------------------------------------------------+----------------+
| publish   | URL for publishing requests for this feed         | <publishURL>   |
+-----------+---------------------------------------------------+----------------+
| subscribe | URL for subscribing to this feed                  | <subscribeURL> |
+-----------+---------------------------------------------------+----------------+
| log       | URL for accessing log information about this feed | <feedLogURL>   |
+-----------+---------------------------------------------------+----------------+


**Delivery Object**

.. _`Del Object`:

+----------+---------+-----------------------------------------------+-------------------------------------+
|  Field   |  Type   | Description                                   | Restrictions                        |
+==========+=========+===============================================+=====================================+
|  url     | string  | URL to which deliveries for this subscription | length <= 256                       |
|          |         |  should be directed Valid HTTPS URL           |                                     |
+----------+---------+-----------------------------------------------+-------------------------------------+
|  user    | string  | User ID to be passed in the Authorization     | Length <= 20                        |
|          |         | header when deliveries are made               |                                     |
+----------+---------+-----------------------------------------------+-------------------------------------+
| password | string  | Password to be passed in the Authorization    | Length <= 32                        |
|          |         | header when deliveries are made               |                                     |
+----------+---------+-----------------------------------------------+-------------------------------------+
| use100   | boolean | Flag indicating whether the DR should use     | Must be: true to use 100-continue   |
|          |         |  the HTTP 100-continue feature                |                                     |
|          |         |                                               | false to disable using 100-continue |
+----------+---------+-----------------------------------------------+-------------------------------------+


**Sub Links Object**

.. _`Sub Links Obj`:

+-----------+---------------------------------------------------+-------------------+
| Field     | Description                                       | Symbolic Name     |
+===========+===================================================+===================+
| self      | URL pointing to this subscription, used for       | <subscriptionURL> |
|           | updating and deleting the subscription.           |                   |
+-----------+---------------------------------------------------+-------------------+
| feed      | URL of the feed to which this subscription        | <feedURL>         |
|           | applies; the same URL as the <feedURL> in the     |                   |
|           | representation of the feed                        |                   |
+-----------+---------------------------------------------------+-------------------+
| log       | URL for accessing log information about this      | <subLogURL>       |
|           | subscription                                      |                   |
+-----------+---------------------------------------------------+-------------------+


**Response/Error Codes**

.. _`Response Codes`:

+------------------------+-------------------------------------------+
| Response statusCode    | Response Description                      |
+========================+===========================================+
| 200 to 299             | Success Response                          |
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
| 403                    | Forbidden - The request failed            |
|                        | authorization.                            |
|                        | Possible causes:                          |
|                        |                                           |
|                        | * Request originated from an unauthorized |
|                        |   IP address                              |
|                        | * Client certificate subject is not on    |
|                        |   the API’s authorized list.              |
|                        | * X-DMAAP-DR-ON-BEHALF-OF identity is not |
|                        |   authorized to perform                   |
+------------------------+-------------------------------------------+
| 404                    | Not Found - The Request-URI does not point|
|                        | to a resource that is known to the API.   |
+------------------------+-------------------------------------------+
| 405                    | Method Not Allowed - The HTTP method in   |
|                        | the request is not supported for the      |
|                        | resource addressed by the Request-URI.    |
+------------------------+-------------------------------------------+
| 406                    | Not Acceptable - The request has an Accept|
|                        | header indicating that the requester will |
|                        | not accept a response with                |
|                        | application/vnd.dmaap-dr.log-list content.|
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