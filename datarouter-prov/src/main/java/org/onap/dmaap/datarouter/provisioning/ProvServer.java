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
import java.util.EnumSet;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;
import org.onap.dmaap.datarouter.provisioning.utils.AafPropsUtils;
import org.onap.dmaap.datarouter.provisioning.utils.DRProvCadiFilter;
import org.onap.dmaap.datarouter.provisioning.utils.ThrottleFilter;


public class ProvServer {

    public static final EELFLogger intlogger = EELFManager.getInstance()
        .getLogger("InternalLog");

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
        final int httpsPort = Integer.parseInt(
            provProps.getProperty("org.onap.dmaap.datarouter.provserver.https.port", "8443"));

        Security.setProperty("networkaddress.cache.ttl", "4");
        QueuedThreadPool queuedThreadPool = getQueuedThreadPool();

        server = new Server(queuedThreadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);

        NCSARequestLog ncsaRequestLog = getRequestLog(provProps);
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(ncsaRequestLog);

        server.setRequestLog(ncsaRequestLog);

        HttpConfiguration httpConfiguration = getHttpConfiguration(httpsPort);

        //HTTP Connector
        try (ServerConnector httpServerConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(Integer.parseInt(provProps.getProperty(
                "org.onap.dmaap.datarouter.provserver.http.port", "8080")));
            httpServerConnector.setAcceptQueueSize(2);
            httpServerConnector.setIdleTimeout(30000);

            SslContextFactory sslContextFactory = getSslContextFactory(provProps);

            // HTTPS configuration
            HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.setRequestHeaderSize(8192);

            // HTTPS connector
            try (ServerConnector httpsServerConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration))) {
                httpsServerConnector.setPort(httpsPort);
                httpsServerConnector.setIdleTimeout(30000);
                httpsServerConnector.setAcceptQueueSize(2);

                ServletContextHandler servletContextHandler = getServletContextHandler(provProps);
                ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
                contextHandlerCollection.addHandler(servletContextHandler);

                // Server's Handler collection
                HandlerCollection handlerCollection = new HandlerCollection();
                handlerCollection.setHandlers(new Handler[]{contextHandlerCollection, new DefaultHandler()});
                handlerCollection.addHandler(requestLogHandler);

                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
                server.setHandler(handlerCollection);
            }
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
    private static SslContextFactory getSslContextFactory(Properties provProps) {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreType(AafPropsUtils.KEYSTORE_TYPE_PROPERTY);
        sslContextFactory.setKeyStorePath(ProvRunner.getAafPropsUtils().getKeystorePathProperty());
        sslContextFactory.setKeyStorePassword(ProvRunner.getAafPropsUtils().getKeystorePassProperty());
        sslContextFactory.setKeyManagerPassword(ProvRunner.getAafPropsUtils().getKeystorePassProperty());

        sslContextFactory.setTrustStoreType(AafPropsUtils.TRUESTSTORE_TYPE_PROPERTY);
        sslContextFactory.setTrustStorePath(ProvRunner.getAafPropsUtils().getTruststorePathProperty());
        sslContextFactory.setTrustStorePassword(ProvRunner.getAafPropsUtils().getTruststorePassProperty());

        sslContextFactory.setWantClientAuth(true);
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
        sslContextFactory.setIncludeProtocols(provProps.getProperty(
            "org.onap.dmaap.datarouter.provserver.https.include.protocols",
            "TLSv1.1|TLSv1.2").trim().split("\\|"));

        intlogger.info("Unsupported protocols: " + String.join(",", sslContextFactory.getExcludeProtocols()));
        intlogger.info("Supported protocols: " + String.join(",", sslContextFactory.getIncludeProtocols()));
        intlogger.info("Unsupported ciphers: " + String.join(",", sslContextFactory.getExcludeCipherSuites()));
        intlogger.info("Supported ciphers: " + String.join(",", sslContextFactory.getIncludeCipherSuites()));

        return sslContextFactory;
    }

    @NotNull
    private static NCSARequestLog getRequestLog(Properties provProps) {
        NCSARequestLog ncsaRequestLog = new NCSARequestLog();
        ncsaRequestLog.setFilename(provProps.getProperty(
            "org.onap.dmaap.datarouter.provserver.accesslog.dir") + "/request.log.yyyy_mm_dd");
        ncsaRequestLog.setFilenameDateFormat("yyyyMMdd");
        ncsaRequestLog.setRetainDays(90);
        ncsaRequestLog.setAppend(true);
        ncsaRequestLog.setExtended(false);
        ncsaRequestLog.setLogCookies(false);
        ncsaRequestLog.setLogTimeZone("GMT");
        return ncsaRequestLog;
    }

    @NotNull
    private static HttpConfiguration getHttpConfiguration(int httpsPort) {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.setSecurePort(httpsPort);
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
        servletContextHandler.addFilter(new FilterHolder(new ThrottleFilter()),
            "/publish/*", EnumSet.of(DispatcherType.REQUEST));
        setCadiFilter(servletContextHandler, provProps);
        return servletContextHandler;
    }

    private static void setCadiFilter(ServletContextHandler servletContextHandler, Properties provProps) {
        if (Boolean.parseBoolean(provProps.getProperty(
            "org.onap.dmaap.datarouter.provserver.cadi.enabled", "false"))) {
            try {
                servletContextHandler.addFilter(new FilterHolder(new DRProvCadiFilter(
                    true, ProvRunner.getAafPropsUtils().getPropAccess())), "/*", EnumSet.of(DispatcherType.REQUEST));
                intlogger.info("PROV0001 AAF CADI filter enabled");
            } catch (ServletException e) {
                intlogger.error("PROV0001 Failed to add CADI filter to server");
            }

        }
    }
}
