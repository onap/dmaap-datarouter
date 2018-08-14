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

/**
 * The main starting point for the Data Router node
 */
public class NodeMain {
    private NodeMain() {
    }

    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeMain");

    private static class wfconfig implements Runnable {
        private NodeConfigManager ncm;

        wfconfig(NodeConfigManager ncm) {
            this.ncm = ncm;
        }

        public synchronized void run() {
            notify();
        }

        synchronized void waitforconfig() {
            ncm.registerConfigTask(this);
            while (!ncm.isConfigured()) {
                logger.info("NODE0003 Waiting for Node Configuration");
                try {
                    wait();
                } catch (Exception e) {
                    logger.debug("NodeMain: waitforconfig exception");
                }
            }
            ncm.deregisterConfigTask(this);
            logger.info("NODE0004 Node Configuration Data Received");
        }
    }

    private static Delivery d;
    private static NodeConfigManager ncm;

    /**
     * Reset the retry timer for a subscription
     */
    static void resetQueue(String subid, String ip) {
        d.resetQueue(ncm.getSpoolDir(subid, ip));
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
        ncm = NodeConfigManager.getInstance();
        logger.info("NODE0002 I am " + ncm.getMyName());
        (new wfconfig(ncm)).waitforconfig();
        d = new Delivery(ncm);
        LogManager lm = new LogManager(ncm);
        Server server = new Server();

        // HTTP configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setIdleTimeout(2000);
        http_config.setRequestHeaderSize(2048);

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(ncm.getHttpPort());

        // HTTPS configuration
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreType(ncm.getKSType());
        sslContextFactory.setKeyStorePath(ncm.getKSFile());
        sslContextFactory.setKeyStorePassword(ncm.getKSPass());
        sslContextFactory.setKeyManagerPassword(ncm.getKPass());

        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.setRequestHeaderSize(8192);

        ServerConnector https = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(ncm.getHttpsPort());
        https.setIdleTimeout(500000);
        https.setAcceptQueueSize(2);

        /* Skip SSLv3 Fixes */
        sslContextFactory.addExcludeProtocols("SSLv3");
        logger.info("Excluded protocols node-" + sslContextFactory.getExcludeProtocols());
        /* End of SSLv3 Fixes */

        server.setConnectors(new Connector[]{http, https});
        ServletContextHandler ctxt = new ServletContextHandler(0);
        ctxt.setContextPath("/");
        server.setHandler(ctxt);
        ctxt.addServlet(new ServletHolder(new NodeServlet()), "/*");
        logger.info("NODE0005 Data Router Node Activating Service");
        server.start();
        server.join();
    }
}
