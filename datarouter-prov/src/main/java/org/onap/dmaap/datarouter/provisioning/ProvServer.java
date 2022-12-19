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

package org.onap.dmaap.datarouter.provisioning;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.security.Security;
import java.util.Properties;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;


public class ProvServer {

    public static final EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");

    private static Server server;

    private ProvServer() {
    }

    static Server getServerInstance() {
        if (server == null) {
            server = createProvServer(ProvRunner.getProvProperties());
        }
        return server;
    }

    private static Server createProvServer(Properties provProps) {
        Security.setProperty("networkaddress.cache.ttl", "4");
        QueuedThreadPool queuedThreadPool = getQueuedThreadPool();

        server = new Server(queuedThreadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);

        HttpConfiguration httpConfiguration = getHttpConfiguration();

        //HTTP Connector
        try (ServerConnector httpServerConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(Integer.parseInt(provProps.getProperty(
                "org.onap.dmaap.datarouter.provserver.http.port", "80")));
            httpServerConnector.setAcceptQueueSize(2);
            httpServerConnector.setIdleTimeout(30000);

            ServletContextHandler servletContextHandler = getServletContextHandler(provProps);
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            contextHandlerCollection.addHandler(servletContextHandler);

            CustomRequestLog customRequestLog = getCustomRequestLog(provProps);
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(customRequestLog);

            server.setRequestLog(customRequestLog);

            // Server's Handler collection
            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.setHandlers(new Handler[]{contextHandlerCollection, new DefaultHandler()});
            handlerCollection.addHandler(requestLogHandler);

            if (Boolean.TRUE.equals(ProvRunner.getTlsEnabled())) {
                // HTTPS configuration
                int httpsPort = Integer.parseInt(
                    provProps.getProperty("org.onap.dmaap.datarouter.provserver.https.port", "443"));
                httpConfiguration.setSecureScheme("https");
                httpConfiguration.setSecurePort(httpsPort);
                HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
                httpsConfiguration.setRequestHeaderSize(8192);
                // HTTPS connector
                try (ServerConnector httpsServerConnector = new ServerConnector(server,
                    new SslConnectionFactory(getSslContextFactory(), HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfiguration))) {
                    httpsServerConnector.setPort(httpsPort);
                    httpsServerConnector.setIdleTimeout(30000);
                    httpsServerConnector.setAcceptQueueSize(2);
                    intlogger.info("ProvServer: TLS enabled. Setting up both HTTP/S connectors.");
                    server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
                }
            } else {
                intlogger.info("ProvServer: TLS disabled. Setting up HTTP connector only.");
                server.setConnectors(new Connector[]{httpServerConnector});
            }
            server.setHandler(handlerCollection);
        }
        return server;
    }

    @NotNull
    private static QueuedThreadPool getQueuedThreadPool() {
        // Server's thread pool
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMinThreads(10);
        queuedThreadPool.setMaxThreads(200);
        queuedThreadPool.setDetailedDump(false);
        return queuedThreadPool;
    }

    @NotNull
    private static SslContextFactory.Server getSslContextFactory() {
        SslContextFactory.Server sslContextFactoryServer = ProvRunner.getProvTlsManager().getSslContextFactoryServer();
        sslContextFactoryServer.setExcludeCipherSuites(
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
        );
        sslContextFactoryServer.addExcludeProtocols("SSLv3");
        intlogger.info("Unsupported protocols: " + String.join(",", sslContextFactoryServer.getExcludeProtocols()));
        intlogger.info("Supported protocols: " + String.join(",", sslContextFactoryServer.getIncludeProtocols()));
        intlogger.info("Unsupported ciphers: " + String.join(",", sslContextFactoryServer.getExcludeCipherSuites()));
        intlogger.info("Supported ciphers: " + String.join(",", sslContextFactoryServer.getIncludeCipherSuites()));
        return sslContextFactoryServer;
    }

    @NotNull
    private static CustomRequestLog getCustomRequestLog(Properties provProps) {
        String filename = provProps.getProperty(
            "org.onap.dmaap.datarouter.provserver.accesslog.dir") + "/request.log.yyyy_mm_dd";
        String format = "yyyyMMdd";
        return new CustomRequestLog(filename, format);
    }

    @NotNull
    private static HttpConfiguration getHttpConfiguration() {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setOutputBufferSize(32768);
        httpConfiguration.setRequestHeaderSize(8192);
        httpConfiguration.setResponseHeaderSize(8192);
        httpConfiguration.setSendServerVersion(true);
        httpConfiguration.setSendDateHeader(false);
        return httpConfiguration;
    }

    @NotNull
    private static ServletContextHandler getServletContextHandler(Properties provProps) {
        ServletContextHandler servletContextHandler = new ServletContextHandler(0);
        servletContextHandler.setContextPath("/");
        servletContextHandler.addServlet(new ServletHolder(new FeedServlet()), "/feed/*");
        servletContextHandler.addServlet(new ServletHolder(new FeedLogServlet()), "/feedlog/*");
        servletContextHandler.addServlet(new ServletHolder(new PublishServlet()), "/publish/*");
        servletContextHandler.addServlet(new ServletHolder(new SubscribeServlet()), "/subscribe/*");
        servletContextHandler.addServlet(new ServletHolder(new StatisticsServlet()), "/statistics/*");
        servletContextHandler.addServlet(new ServletHolder(new SubLogServlet()), "/sublog/*");
        servletContextHandler.addServlet(new ServletHolder(new GroupServlet()), "/group/*");
        servletContextHandler.addServlet(new ServletHolder(new SubscriptionServlet()), "/subs/*");
        servletContextHandler.addServlet(new ServletHolder(new InternalServlet()), "/internal/*");
        servletContextHandler.addServlet(new ServletHolder(new RouteServlet()), "/internal/route/*");
        servletContextHandler.addServlet(new ServletHolder(new DRFeedsServlet()), "/");
        return servletContextHandler;
    }
}
