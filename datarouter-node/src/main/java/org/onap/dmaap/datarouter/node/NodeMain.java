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

import java.util.Arrays;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * The main starting point for the Data Router node
 */
public class NodeMain {

    private NodeMain() {
    }

    private static Logger nodeMainLogger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeMain");

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
                nodeMainLogger.info("NODE0003 Waiting for Node Configuration");
                try {
                    wait();
                } catch (Exception exception) {
                    nodeMainLogger
                        .debug("NodeMain: waitForConfig exception. Exception Message:- " + exception.toString(),
                            exception);
                }
            }
            localNodeConfigManager.deregisterConfigTask(this);
            nodeMainLogger.info("NODE0004 Node Configuration Data Received");
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
     * The location of the node configuration file can be set using the org.onap.dmaap.datarouter.node.ConfigFile system
     * property.  By default, it is "etc/node.properties".
     */
    public static void main(String[] args) throws Exception {
        nodeMainLogger.info("NODE0001 Data Router Node Starting");
        IsFrom.setDNSCache();
        nodeConfigManager = NodeConfigManager.getInstance();
        nodeMainLogger.info("NODE0002 I am " + nodeConfigManager.getMyName());
        (new WaitForConfig(nodeConfigManager)).waitForConfig();
        delivery = new Delivery(nodeConfigManager);
        Server server = new Server();
        // HTTP configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setIdleTimeout(2000);
        httpConfiguration.setRequestHeaderSize(2048);

        // HTTP connector
        ServletContextHandler ctxt;
        try (ServerConnector httpServerConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(nodeConfigManager.getHttpPort());

            // HTTPS configuration
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStoreType(nodeConfigManager.getKSType());
            sslContextFactory.setKeyStorePath(nodeConfigManager.getKSFile());
            sslContextFactory.setKeyStorePassword(nodeConfigManager.getKSPass());
            sslContextFactory.setKeyManagerPassword(nodeConfigManager.getKPass());
            /* Skip SSLv3 Fixes */
            sslContextFactory.addExcludeProtocols("SSLv3");
            nodeMainLogger.info("Excluded protocols node-" + Arrays.toString(sslContextFactory.getExcludeProtocols()));
            /* End of SSLv3 Fixes */

            HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.setRequestHeaderSize(8192);

            SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
            secureRequestCustomizer.setStsMaxAge(2000);
            secureRequestCustomizer.setStsIncludeSubDomains(true);
            httpsConfiguration.addCustomizer(secureRequestCustomizer);

            // HTTPS connector
            try (ServerConnector httpsServerConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration))) {
                httpsServerConnector.setPort(nodeConfigManager.getHttpsPort());
                httpsServerConnector.setIdleTimeout(500000);
                httpsServerConnector.setAcceptQueueSize(2);

                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
            }
        }
        ctxt = new ServletContextHandler(0);
        ctxt.setContextPath("/");
        server.setHandler(ctxt);
        ctxt.addServlet(new ServletHolder(new NodeServlet()), "/*");
        nodeMainLogger.info("NODE0005 Data Router Node Activating Service");
        server.start();
        server.join();
    }
}
