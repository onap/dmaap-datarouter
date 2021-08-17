/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.node;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.util.EnumSet;
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
import org.jetbrains.annotations.NotNull;


public class NodeServer {

    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeServer.class);

    private static Server server;
    private static Delivery delivery;

    private NodeServer(){
    }

    static Server getServerInstance() {
        if (server == null) {
            server = createNodeServer(NodeConfigManager.getInstance());
        }
        return server;
    }

    private static Server createNodeServer(NodeConfigManager nodeConfigManager) {
        server = new Server();
        delivery = new Delivery(nodeConfigManager);

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setRequestHeaderSize(2048);

        // HTTP connector
        try (ServerConnector httpServerConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(nodeConfigManager.getHttpPort());
            httpServerConnector.setIdleTimeout(2000);

            SslContextFactory sslContextFactory = getSslContextFactory(nodeConfigManager);

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
                    try {
                        servletContextHandler.addFilter(new FilterHolder(new DRNodeCadiFilter(true,
                                nodeConfigManager.getNodeAafPropsUtils().getPropAccess())), "/*",
                            EnumSet.of(DispatcherType.REQUEST));
                    } catch (ServletException e) {
                        eelfLogger.error("Failed to add CADI Filter: " + e.getMessage(), e);
                    }
                }
                server.setHandler(servletContextHandler);
                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
            }
        }
        return server;
    }

    /**
     * Reset the retry timer for a subscription.
     */
    static void resetQueue(String subid, String ip) {
        delivery.resetQueue(NodeConfigManager.getInstance().getSpoolDir(subid, ip));
    }


    @NotNull
    private static SslContextFactory getSslContextFactory(NodeConfigManager nodeConfigManager) {
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType(nodeConfigManager.getKSType());
        sslContextFactory.setKeyStorePath(nodeConfigManager.getKSFile());
        sslContextFactory.setKeyStorePassword(nodeConfigManager.getKSPass());
        sslContextFactory.setKeyManagerPassword(nodeConfigManager.getKPass());

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
        eelfLogger.info("Unsupported protocols: " + String.join(",", sslContextFactory.getExcludeProtocols()));
        eelfLogger.info("Supported protocols: " + String.join(",", sslContextFactory.getIncludeProtocols()));
        eelfLogger.info("Unsupported ciphers: " + String.join(",", sslContextFactory.getExcludeCipherSuites()));
        eelfLogger.info("Supported ciphers: " + String.join(",", sslContextFactory.getIncludeCipherSuites()));
        return sslContextFactory;
    }
}
