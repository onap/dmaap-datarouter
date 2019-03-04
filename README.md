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

After Datarouter repository is cloned it can be built using Maven
In the repository

Go to datarouter-prov in the root

	mvn clean install

Go to datarouter-node in the root

	mvn clean install

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

curl -v -X POST -H "Content-Type : application/vnd.dmaap-dr.feed" -H "X-DMAAP-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addFeed3.txt --post301 --location-trusted  -k https://dmaap-dr-prov:8443

Subscribe to feed:

curl -v -X POST -H "Content-Type: application/vnd.dmaap-dr.subscription" -H "X-DMAAP-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addSubscriber.txt --post301 --location-trusted -k https://dmaap-dr-prov:8443/subscribe/1

Publish to feed:

curl -v -X PUT --user rs873m:rs873m -H "Content-Type: application/octet-stream" --data-binary @/opt/app/datartr/addFeed3.txt  --post301 --location-trusted -k https://dmaap-dr-prov:8443/publish/1/test1




 ## CONFIGURATION

Recommended

Environment - Unix based

Java - 1.8

Maven - 3.2.5

MariaDB - 10.2.14

Self Signed SSL certificates

## This section describes how to build and test datarouter containers on a host such as
a laptop or remote server.

- Install git, maven, docker
    - See https://wiki.onap.org/display/DW/Setting+Up+Your+Development+Environment
    - See https://docs.docker.com/install/

### Build
- in terminal 
> git clone https://gerrit.onap.org/r/dmaap/datarouter
> cd datarouter
> mvn clean install -DskipTests -Pdocker
> docker images
REPOSITORY                                               TAG                 IMAGE ID            CREATED             SIZE
nexus3.onap.org:10003/onap/dmaap/datarouter-subscriber   2.0.0-SNAPSHOT      0dfc99a7612c        13 seconds ago      99.2MB
nexus3.onap.org:10003/onap/dmaap/datarouter-subscriber   latest              0dfc99a7612c        13 seconds ago      99.2MB
nexus3.onap.org:10003/onap/dmaap/datarouter-node         2.0.0-SNAPSHOT      6573f4bdc310        27 seconds ago      116MB
nexus3.onap.org:10003/onap/dmaap/datarouter-node         latest              6573f4bdc310        27 seconds ago      116MB
nexus3.onap.org:10003/onap/dmaap/datarouter-prov         2.0.0-SNAPSHOT      9e4148737c18        47 seconds ago      148MB
nexus3.onap.org:10003/onap/dmaap/datarouter-prov         latest              9e4148737c18        47 seconds ago      148MB
openjdk                                                  8-jre-alpine        1b46cc2ba839        3 weeks ago         85MB
nexus3.onap.org:10001/openjdk                            8-jre-alpine        1b46cc2ba839        3 weeks ago         85MB

### Test
> cd datarouter-docker-compose/src/main/resources
- edit docker-compose, change nexus 0001 (remote pull repo) to 0003 (local build)
> docker-compose up

- terminal 2
>  docker container ls -a
CONTAINER ID        IMAGE                                                    COMMAND                  CREATED              STATUS                        PORTS                                                                   NAMES
c193317ec860        nexus3.onap.org:10003/onap/dmaap/datarouter-node         "sh startup.sh"          About a minute ago   Up About a minute             0.0.0.0:9090->8080/tcp, 0.0.0.0:9443->8443/tcp                          datarouter-node
e8dab741550e        nexus3.onap.org:10003/onap/dmaap/datarouter-prov         "sh startup.sh"          About a minute ago   Up About a minute (healthy)   0.0.0.0:8080->8080/tcp, 0.0.0.0:8443->8443/tcp, 0.0.0.0:443->8443/tcp   datarouter-prov
cf0e996f0f31        nexus3.onap.org:10003/onap/dmaap/datarouter-subscriber   "sh startup.sh"          About a minute ago   Up About a minute             8080/tcp, 0.0.0.0:7070->7070/tcp, 8443/tcp                              subscriber-node
73affb6364f9        mariadb:10.2.14                                          "docker-entrypoint.s…"   About a minute ago   Up About a minute (healthy)   0.0.0.0:3306->3306/tcp                                                  mariadb

> docker exec -it datarouter-node /bin/sh
    # curl http://dmaap-dr-prov:8080/internal/prov
> docker exec -it datarouter-prov /bin/sh
    # curl -v -X POST -H "Content-Type : application/vnd.dmaap-dr.feed" -H "X-DMAAP-DR-ON-BEHALF-OF: rs873m" --data-ascii @/opt/app/datartr/addFeed3.txt --post301 --location-trusted  -k https://dmaap-dr-prov:8443

