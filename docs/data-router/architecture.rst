.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Architecture
============


Capabilities
------------
Data Router is a RESTful web service used for the transfer of data across networks any larger than a Message Router message (> 2Mb).

Usage Scenarios
---------------
    Data Router endpoints are used to create/view/delete Feeds, Subscribers and Published files. Clients can use the Data Router endpoints
    to publish a file to a feed and subscribe to this feed to receive the file.

Interactions
------------
Data Router REST service uses the Data Router API to allow users to publish to and subscribe to a feed, in order to send and receive files.



   |image0|

   .. |image0| image:: dr_arch.png
de