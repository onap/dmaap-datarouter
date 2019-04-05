.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

Installation
=============

**Standalone**
Pre-requisites:

* docker 18.09.3 or higher.
* docker-compose 1.17.1 or higher.
* Ensure port 8080 is not already in use.

1. Clone the DMaaP Data Router project from ONAP gerrit:

.. code-block:: bash

    git clone https://gerrit.onap.org/r/dmaap/datarouter

2. Move/cd to the docker-compose directory and execute the following:

.. code-block:: bash

    cd datarouter/datarouter-docker-compose/src/main/resources/

    docker-compose up -d


The following docker containers should be deployed successfully:

.. code-block:: bash

    docker ps --format '{{.Image}}'

    nexus3.onap.org:10001/onap/dmaap/datarouter-node
    nexus3.onap.org:10001/onap/dmaap/datarouter-prov
    nexus3.onap.org:10001/onap/dmaap/datarouter-subscriber
    mariadb:10.2.14


To verify that the provisioning API is active, get the IP of the datarouter-prov container:

.. code-block:: bash

    docker inspect --format '{{ .NetworkSettings.Networks.resources_testing_net.IPAddress }}' datarouter-prov

and execute the following CURL command:

.. code-block:: bash

    curl -k https://{DR_PROV_CONTAINER_IP}:8443/internal/prov