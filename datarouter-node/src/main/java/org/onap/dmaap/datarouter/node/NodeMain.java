/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/

package org.onap.dmaap.datarouter.node;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.ssl.*;
import org.eclipse.jetty.server.*;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * The main starting point for the Data Router node
 */
public class NodeMain {
    private NodeMain() {
    }

    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeMain");

    private static class WaitForConfig implements Runnable {
        private NodeConfigManager localNodeConfigManager;

        WaitForConfig(NodeConfigManager ncm) {
            this.localNodeConfigManager = ncm;
        }

        public synchronized void run() {
            notify();
        }

        synchronized void waitForConfig() {
            localNodeConfigManager.registerConfigTask(this);
            while (!localNodeConfigManager.isConfigured()) {
                logger.info("NODE0003 Waiting for Node Configuration");
                try {
                    wait();
                } catch (Exception e) {
                    logger.debug("NodeMain: waitForConfig exception");
                }
            }
            localNodeConfigManager.deregisterConfigTask(this);
            logger.info("NODE0004 Node Configuration Data Received");
        }
    }

    private static Delivery delivery;
    private static NodeConfigManager nodeConfigManager;

    /**
     * Reset the retry timer for a subscription
     */
    static void resetQueue(String subid, String ip) {
        delivery.resetQueue(nodeConfigManager.getSpoolDir(subid, ip));
    }

    /**
     * Start the data router.
     * <p>
     * The location of the node configuration file can be set using the
     * org.onap.dmaap.datarouter.node.ConfigFile system property.  By
     * default, it is "etc/node.properties".
     */
    public static void main(String[] args) throws Exception {
        logger.info("NODE0001 Data Router Node Starting");
        IsFrom.setDNSCache();
        nodeConfigManager = NodeConfigManager.getInstance();
        logger.info("NODE0002 I am " + nodeConfigManager.getMyName());
        (new WaitForConfig(nodeConfigManager)).waitForConfig();
        delivery = new Delivery(nodeConfigManager);
        LogManager lm = new LogManager(nodeConfigManager);
        Server server = new Server();

        // HTTP configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setIdleTimeout(2000);
        httpConfig.setRequestHeaderSize(2048);

        ServletContextHandler ctxt;
        try (ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig))) {
            http.setPort(nodeConfigManager.getHttpPort());

            // HTTPS configuration
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStoreType(nodeConfigManager.getKSType());
            sslContextFactory.setKeyStorePath(nodeConfigManager.getKSFile());
            sslContextFactory.setKeyStorePassword(nodeConfigManager.getKSPass());
            sslContextFactory.setKeyManagerPassword(nodeConfigManager.getKPass());

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.setRequestHeaderSize(8192);

            try (ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig))) {
                https.setPort(nodeConfigManager.getHttpsPort());
                https.setIdleTimeout(500000);
                https.setAcceptQueueSize(2);

                /* Skip SSLv3 Fixes */
                sslContextFactory.addExcludeProtocols("SSLv3");
                logger.info("Excluded protocols Data Router Node: " + Arrays.toString(sslContextFactory.getExcludeProtocols()));
                /* End of SSLv3 Fixes */

                server.setConnectors(new Connector[]{http, https});
            }
        }
        ctxt = new ServletContextHandler(0);
        ctxt.setContextPath("/");
        server.setHandler(ctxt);
        ctxt.addServlet(new ServletHolder(new NodeServlet()), "/*");
        logger.info("NODE0005 Data Router Node Activating Service");
        server.start();
        server.join();
    }
}
