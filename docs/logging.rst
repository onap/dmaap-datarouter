.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Logging
=======


Where to Access Information
---------------------------
Data Router uses logback framework to generate logs.

Error / Warning Messages
------------------------
Currently Data Router does not have any unique error codes. However the following are the common HTTP error codes that
could possibly occur in Data Router:

    OK = 200 : The normal response from a successful update or get of a feed or subscription

    CREATED = 201 : the normal response from successfully creating or subscribing to a feed.

    NO_CONTENT = 204 : the normal response from a successful publish attempt and on successfully deleting a feed or subscription

    MOVED_PERMANENTLY = 301 :the normal redirect response from prov to a publisher

    BAD_REQUEST = 400: Usually indicates that either Json object in request body is incorrect in some way, or an Invalid parameter value was included in query string.

    UNAUTHORIZED = 401 : Usually indicated either request was missing Authorization header, or indicates incorrect Username/password credentials

    FORBIDDEN = 403 : Usually indicates the request originated from an unauthorized IP address, or that a client certificate was not a part of authorized list.

    NOT_FOUND = 404 : Usually indicates an incorrect URI

    METHOD_NOT_ALLOWED = 405 : Indicates an HTTP method is not accepted for given URI