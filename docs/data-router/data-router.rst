============================================
Data Router (DR) API Guide
============================================
Introduction
------------------

The DataRouter(DR)provisioning API is an HTTPS-based,REST-like API for creating and managing DR feeds and subscriptions. The Data Routing System project is intended to provide a common framework by which data producers can make data available to data consumers and a way for potential consumers to find feeds with the data they require.


HTTP Service APIs
------------------

DMaaP Message Router utilizes an HTTP REST API to service all transactions. HTTP and REST standards are followed so
clients as varied as CURL, Java applications and even Web Browsers will
work to interact with the Data Router.

General HTTP Requirements
=========================

A DMaaP Message Router transactions consists of 4 distinct segments,
HTTP URL, HTTP Header, HTTP Body (POST/PUT) and HTTP Response. The general
considerations for each segment are as follows and are required for each
of the specific transactions described in this section.

HTTP URL
========

http[s]://serverBaseURL/{resourcePath}


HTTP Header
===========

Specifies HTTP Headers, such as Content-Type, that define the parameters
of the HTTP Transaction

HTTP Body
=========

The HTTP Body contains the topic content when Publishing or Consuming.
The Body may contain topic messages in several formats (like below) but
it must be noted, that, except in very specific circumstances, messages
are not inspected for content.

Create a Feed
-----------

**Description**:Creates the feed

Sample Request
==============

curl -v -X POST -H "Content-Type : application/vnd.att-dr.feed" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addFeed3.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443

Subscribe to Feed
-----------

curl -v -X POST -H "Content-Type: application/vnd.att-dr.subscription" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addSubscriber.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443/subscribe/1

Sample Request
==============

POST http(s)://{HOST:PORT}/events/{topicname}

Publish to feed
-----------

**Description**:publish  the feed

Sample Request
==============

curl -v -X PUT --user rs873m:rs873m -H "Content-Type: application/octet-stream" --data-binary @/opt/app/datartr/addFeed3.txt --post301 --location-trusted -k https://prov.datarouternew.com:8443/publish/1/test1
