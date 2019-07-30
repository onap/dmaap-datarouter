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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.onap.aaf.cadi.PropAccess;

/**
 * The main starting point for the Data Router node.
 */
public class NodeMain {

    private static EELFLogger nodeMainLogger = EELFManager.getInstance().getLogger(NodeMain.class);
    private static Delivery delivery;
    private static NodeConfigManager nodeConfigManager;

    private NodeMain() {
    }

    /**
     * Reset the retry timer for a subscription.
     */

    static void resetQueue(String subid, String ip) {
        delivery.resetQueue(nodeConfigManager.getSpoolDir(subid, ip));
    }

    /**
     * Start the data router.
     *
     * <p>The location of the node configuration file can be set using the org.onap.dmaap.datarouter.node.properties
     * system property. By default, it is "/opt/app/datartr/etc/node.properties".
     */
    public static void main(String[] args) throws Exception {
        nodeMainLogger.debug("NODE0001 Data Router Node Starting");
        IsFrom.setDNSCache();
        nodeConfigManager = NodeConfigManager.getInstance();
        nodeMainLogger.debug("NODE0002 I am " + nodeConfigManager.getMyName());
        (new WaitForConfig(nodeConfigManager)).waitForConfig();
        delivery = new Delivery(nodeConfigManager);
        new LogManager(nodeConfigManager);

        Server server = new Server();

        // HTTP configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setRequestHeaderSize(2048);

        // HTTP connector
        try (ServerConnector httpServerConnector = new ServerConnector(server,
                new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(nodeConfigManager.getHttpPort());
            httpServerConnector.setIdleTimeout(2000);

            // HTTPS configuration
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStoreType(nodeConfigManager.getKSType());
            sslContextFactory.setKeyStorePath(nodeConfigManager.getKSFile());
            sslContextFactory.setKeyStorePassword(nodeConfigManager.getKSPass());
            sslContextFactory.setKeyManagerPassword(nodeConfigManager.getKPass());

            //SP-6: Fixes for SDV scan to exclude/remove DES/3DES
            // ciphers are taken care by upgrading jdk in descriptor.xml
            sslContextFactory.setExcludeCipherSuites(
                    "SSL_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                    "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                    "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
            );

            sslContextFactory.addExcludeProtocols("SSLv3");
            sslContextFactory.setIncludeProtocols(nodeConfigManager.getEnabledprotocols());
            nodeMainLogger.debug("NODE00004 Unsupported protocols node server:-"
                    + String.join(",", sslContextFactory.getExcludeProtocols()));
            nodeMainLogger.debug("NODE00004 Supported protocols node server:-"
                    + String.join(",", sslContextFactory.getIncludeProtocols()));
            nodeMainLogger.debug("NODE00004 Unsupported ciphers node server:-"
                    + String.join(",", sslContextFactory.getExcludeCipherSuites()));

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
                httpsServerConnector.setIdleTimeout(3600000);
                httpsServerConnector.setAcceptQueueSize(2);

                //Context Handler
                ServletContextHandler servletContextHandler = new ServletContextHandler(0);
                servletContextHandler.setContextPath("/");
                servletContextHandler.addServlet(new ServletHolder(new NodeServlet(delivery)), "/*");

                //CADI Filter activation check
                if (nodeConfigManager.getCadiEnabled()) {
                    enableCadi(servletContextHandler);
                }

                server.setHandler(servletContextHandler);
                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
            }
        }

        try {
            server.start();
            nodeMainLogger.debug("NODE00006 Node Server started-" + server.getState());
        } catch (Exception e) {
            nodeMainLogger.error("NODE00006 Jetty failed to start. Reporting will we unavailable: "
                                         + e.getMessage(), e);
        }
        server.join();
        nodeMainLogger.debug("NODE00007 Node Server joined - " + server.getState());
    }

    private static void enableCadi(ServletContextHandler servletContextHandler) throws ServletException {
        Properties cadiProperties = new Properties();
        try {
            Inner obj = new NodeMain().new Inner();
            InputStream in = obj.getCadiProps();
            cadiProperties.load(in);
        } catch (IOException e1) {
            nodeMainLogger
                    .error("NODE00005 Exception in NodeMain.Main() loading CADI properties " + e1.getMessage(), e1);
        }
        cadiProperties.setProperty("aaf_locate_url", nodeConfigManager.getAafURL());
        nodeMainLogger.debug("NODE00005  aaf_url set to - " + cadiProperties.getProperty("aaf_url"));

        PropAccess access = new PropAccess(cadiProperties);
        servletContextHandler.addFilter(new FilterHolder(new DRNodeCadiFilter(true, access)), "/*", EnumSet
                .of(DispatcherType.REQUEST));
    }

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
                nodeMainLogger.debug("NODE0003 Waiting for Node Configuration");
                try {
                    wait();
                } catch (Exception exception) {
                    nodeMainLogger
                            .error("NodeMain: waitForConfig exception. Exception Message:- " + exception.toString(),
                                    exception);
                }
            }
            localNodeConfigManager.deregisterConfigTask(this);
            nodeMainLogger.debug("NODE0004 Node Configuration Data Received");
        }
    }

    class Inner {

        InputStream getCadiProps() {
            InputStream in = null;
            try {
                in = getClass().getClassLoader().getResourceAsStream("drNodeCadi.properties");
            } catch (Exception e) {
                nodeMainLogger.error("Exception in Inner.getCadiProps() method ", e);
            }
            return in;
        }
    }
}
