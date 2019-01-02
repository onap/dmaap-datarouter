# DMAAP_DATAROUTER

## OVERVIEW

The Data Routing System project is intended to provide a common framework by which data producers can make data available to data consumers and a way for potential consumers to find feeds with the data they require.
The delivery of data from these kinds of production systems is the domain of the Data Routing System. Its primary goal is to make it easier to move data from existing applications that may not have been designed from the ground up to share data.
The Data Routing System is different from many existing platforms for distributing messages from producers to consumers which focus on real-time delivery of small messages (on the order of a few kilobytes or so) for more

   Provisioning is implemented as a Java servlet running under Jetty in one JVM

   Provisioning data is stored in a MariaDB database

   The backup provisioning server and each node is informed any time provisioning data changes

   The backup provisioning server and each node may request the complete set of provisioning data at any time

   A Node is implemented as a Java servlet running under Jetty in one JVM

Assumptions
    For 95% of all feeds (there will be some exceptions):

    Number of Publishing Endpoints per Feed: 1 – 10

    Number of Subscribers per Feed: 2 – 10

    File Size: 105 – 1010 bytes

    with a distribution towards the high end

    Frequency of Publishing: 1/day – 10/minute

    Lifetime of a Feed: months to years

    Lifetime of a Subscription: months to years


Data Router and Sensitive Data Handling

    A publisher of a Data Router feed of sensitive (e.g., PCI, SPI, etc.) data needs to encrypt that data prior to delivering it to the Data Router

    The Data Router will distribute that data to all of the subscribers of that feed.

    Data Router does not examine the Feed content or enforce any restrictions or Validations on the Feed Content in any way

    It is the responsibility of the subscribers to work with the publisher to determine how to decrypt that data





What the Data Router is NOT:

    Does not support streaming data

    Does not tightly couple to any specific publish endpoint or subscriber

    Agnostic as to source and sink of data residing in an RDBMS, NoSQL DB, Other DBMS, Flat Files, etc.

    Does not transform any published data

    Does not “examine” any published data

    Does not verify the integrity of a published file

    Does not perform any data “cleansing”

    Does not store feeds (not a repository or archive)

    There is no long-term storage – assumes subscribers are responsive most of the time

    Does not encrypt data when queued on a node

    Does not provide guaranteed order of delivery

    Per-file metadata can be used for ordering


## BUILD

Datarouter can be cloned and images can be built using Maven
In the repository run

    mvn clean install -P docker docker:build

Project Build will be Successful


## RUN

Datarouter is a Unix based service

Pre-requisites to run the service

MariaDB Version 10.2.14

Java JDK 1.8

Install MariaDB and load needed table into the database

Sample sql_init_01.sql is provided in the datarouter-prov/src/main/resources/misc

Go to datarouter-prov module and run the service using main.java

Go to datarouter-node module and run the service using nodemain.java

Curl Commands to test:

create a feed:

curl -v -X POST -H "Content-Type : application/vnd.att-dr.feed" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addFeed3.txt --post301 --location-trusted  -k https://dmaap-dr-prov:8443

Subscribe to feed:

curl -v -X POST -H "Content-Type: application/vnd.att-dr.subscription" -H "X-ATT-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addSubscriber.txt --post301 --location-trusted -k https://dmaap-dr-prov:8443/subscribe/1

Publish to feed:

curl -v -X PUT --user rs873m:rs873m -H "Content-Type: application/octet-stream" --data-binary @/opt/app/datartr/addFeed3.txt  --post301 --location-trusted -k https://dmaap-dr-prov:8443/publish/1/test1




 ## CONFIGURATION

Recommended

Environment - Unix based

Java - 1.8

Maven - 3.2.5

MariaDB - 10.2.14

Self Signed SSL certificates


