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

import org.apache.http.HttpException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.ssl.*;
import org.eclipse.jetty.server.*;
import org.apache.log4j.Logger;

import javax.xml.ws.http.HTTPException;

/**
 * The main starting point for the Data Router node
 */
public class NodeMain {
    private NodeMain() {
    }

    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeMain");
    private static ServerConnector httpServerConnector;
    private static ServerConnector httpsServerConnector;
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
                    logger.debug("NodeMain: waitforconfig exception. " + e.getMessage());
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
        Server server = new Server();
        SslContextFactory sslContextFactory = new SslContextFactory();

        // HTTP configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setIdleTimeout(2000);
        httpConfiguration.setRequestHeaderSize(2048);

        // HTTP connector
        try(ServerConnector httpServerConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(ncm.getHttpPort());
            // HTTPS configuration
            sslContextFactory.setKeyStoreType(ncm.getKSType());
            sslContextFactory.setKeyStorePath(ncm.getKSFile());
            sslContextFactory.setKeyStorePassword(ncm.getKSPass());
            sslContextFactory.setKeyManagerPassword(ncm.getKPass());
            /* Skip SSLv3 Fixes */
            sslContextFactory.addExcludeProtocols("SSLv3");
        }
        /* End of SSLv3 Fixes */
        HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
        httpsConfiguration.setRequestHeaderSize(8192);
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setStsMaxAge(2000);
        secureRequestCustomizer.setStsIncludeSubDomains(true);
        httpsConfiguration.addCustomizer(secureRequestCustomizer);

        // HTTPS connector
       try(ServerConnector httpsServerConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration))) {
           httpsServerConnector.setPort(ncm.getHttpsPort());
           httpsServerConnector.setIdleTimeout(500000);
           httpsServerConnector.setAcceptQueueSize(2);
       }
        server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
        ServletContextHandler ctxt = new ServletContextHandler(0);
        ctxt.setContextPath("/");
        server.setHandler(ctxt);
        ctxt.addServlet(new ServletHolder(new NodeServlet()), "/*");
        logger.info("NODE0005 Data Router Node Activating Service");
        server.start();
        server.join();
    }
}
