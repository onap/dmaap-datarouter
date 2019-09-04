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


package org.onap.dmaap.datarouter.provisioning;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Timer;
import javax.servlet.DispatcherType;
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
import org.onap.aaf.cadi.PropAccess;

import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.DRProvCadiFilter;
import org.onap.dmaap.datarouter.provisioning.utils.DbConnectionPool;
import org.onap.dmaap.datarouter.provisioning.utils.DbItialization;
import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;
import org.onap.dmaap.datarouter.provisioning.utils.PurgeLogDirTask;
import org.onap.dmaap.datarouter.provisioning.utils.ThrottleFilter;

/**
 * <p>
 * A main class which may be used to start the provisioning server with an "embedded" Jetty server. Configuration is
 * done via the properties file <i>provserver.properties</i>, which should be in the CLASSPATH. The provisioning server
 * may also be packaged with a web.xml and started as a traditional webapp.
 * </p>
 * <p>
 * Most of the work of the provisioning server is carried out within the eight servlets (configured below) that are used
 * to handle each of the eight types of requests the server may receive. In addition, there are background threads
 * started to perform other tasks:
 * </p>
 * <ul>
 * <li>One background Thread runs the {@link LogfileLoader} in order to process incoming logfiles.
 * This Thread is created as a side effect of the first successful POST to the /internal/logs/ servlet.</li>
 * <li>One background Thread runs the {@link SynchronizerTask} which is used to periodically
 * synchronize the database between active and standby servers.</li>
 * <li>One background Thread runs the {@link Poker} which is used to notify the nodes whenever
 * provisioning data changes.</li>
 * <li>One task is run once a day to run {@link PurgeLogDirTask} which purges older logs from the
 * /opt/app/datartr/logs directory.</li>
 * </ul>
 * <p>
 * The provisioning server is stopped by issuing a GET to the URL http://127.0.0.1/internal/halt using <i>curl</i> or
 * some other such tool.
 * </p>
 *
 * @author Robert Eby
 * @version $Id: Main.java,v 1.12 2014/03/12 19:45:41 eby Exp $
 */
public class Main {

    /**
     * The truststore to use if none is specified.
     */
    static final String DEFAULT_TRUSTSTORE = "/opt/java/jdk/jdk180/jre/lib/security/cacerts";
    static final String KEYSTORE_TYPE_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.type";
    static final String KEYSTORE_PATH_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.path";
    static final String KEYSTORE_PASS_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.password";
    static final String TRUSTSTORE_PATH_PROPERTY = "org.onap.dmaap.datarouter.provserver.truststore.path";
    static final String TRUSTSTORE_PASS_PROPERTY = "org.onap.dmaap.datarouter.provserver.truststore.password";
    public static final EELFLogger intlogger = EELFManager.getInstance()
                                                       .getLogger("org.onap.dmaap.datarouter.provisioning.internal");

    /**
     * The one and only {@link Server} instance in this JVM.
     */
    private static Server server;

    class Inner {
        InputStream getCadiProps() {
            InputStream in = null;
            try {
                in = getClass().getClassLoader().getResourceAsStream("drProvCadi.properties");
            } catch (Exception e) {
                intlogger.error("Exception in Main.getCadiProps(): " + e.getMessage(), e);
            }
            return in;
        }
    }

    /**
     * Starts the Data Router Provisioning server.
     *
     * @param args not used
     * @throws Exception if Jetty has a problem starting
     */
    public static void main(String[] args) throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "4");
        // Check DB is accessible and contains the expected tables
        if (!checkDatabase()) {
            System.exit(1);
        }

        intlogger.info("PROV0000 **** AT&T Data Router Provisioning Server starting....");

        Security.setProperty("networkaddress.cache.ttl", "4");
        Properties provProperties = (DbConnectionPool.getProperties());
        int httpPort = Integer.parseInt(provProperties
                                             .getProperty("org.onap.dmaap.datarouter.provserver.http.port", "8080"));
        final int httpsPort = Integer.parseInt(provProperties
                                             .getProperty("org.onap.dmaap.datarouter.provserver.https.port", "8443"));

        // Server's thread pool
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMinThreads(10);
        queuedThreadPool.setMaxThreads(200);
        queuedThreadPool.setDetailedDump(false);

        // The server itself
        server = new Server(queuedThreadPool);
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);

        // Request log configuration
        NCSARequestLog ncsaRequestLog = new NCSARequestLog();
        ncsaRequestLog.setFilename(provProperties
                                           .getProperty("org.onap.dmaap.datarouter.provserver.accesslog.dir")
                                           + "/request.log.yyyy_mm_dd");
        ncsaRequestLog.setFilenameDateFormat("yyyyMMdd");
        ncsaRequestLog.setRetainDays(90);
        ncsaRequestLog.setAppend(true);
        ncsaRequestLog.setExtended(false);
        ncsaRequestLog.setLogCookies(false);
        ncsaRequestLog.setLogTimeZone("GMT");

        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(ncsaRequestLog);
        server.setRequestLog(ncsaRequestLog);

        // HTTP configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.setSecurePort(httpsPort);
        httpConfiguration.setOutputBufferSize(32768);
        httpConfiguration.setRequestHeaderSize(8192);
        httpConfiguration.setResponseHeaderSize(8192);
        httpConfiguration.setSendServerVersion(true);
        httpConfiguration.setSendDateHeader(false);

        //HTTP Connector
        HandlerCollection handlerCollection;
        try (ServerConnector httpServerConnector =
                     new ServerConnector(server, new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(httpPort);
            httpServerConnector.setAcceptQueueSize(2);
            httpServerConnector.setIdleTimeout(300000);

            // SSL Context
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStoreType(provProperties.getProperty(KEYSTORE_TYPE_PROPERTY, "jks"));
            sslContextFactory.setKeyStorePath(provProperties.getProperty(KEYSTORE_PATH_PROPERTY));
            sslContextFactory.setKeyStorePassword(provProperties.getProperty(KEYSTORE_PASS_PROPERTY));
            sslContextFactory.setKeyManagerPassword(provProperties
                                          .getProperty("org.onap.dmaap.datarouter.provserver.keymanager.password"));

            String ts = provProperties.getProperty(TRUSTSTORE_PATH_PROPERTY);
            if (ts != null && ts.length() > 0) {
                intlogger.info("@@ TS -> " + ts);
                sslContextFactory.setTrustStorePath(ts);
                sslContextFactory.setTrustStorePassword(provProperties.getProperty(TRUSTSTORE_PASS_PROPERTY));
            } else {
                sslContextFactory.setTrustStorePath(DEFAULT_TRUSTSTORE);
                sslContextFactory.setTrustStorePassword("changeit");
            }

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
            sslContextFactory.setIncludeProtocols(provProperties.getProperty(
                    "org.onap.dmaap.datarouter.provserver.https.include.protocols",
                    "TLSv1.1|TLSv1.2").trim().split("\\|"));

            intlogger.info("Not supported protocols prov server:-"
                                   + String.join(",", sslContextFactory.getExcludeProtocols()));
            intlogger.info("Supported protocols prov server:-"
                                   + String.join(",", sslContextFactory.getIncludeProtocols()));
            intlogger.info("Not supported ciphers prov server:-"
                                   + String.join(",", sslContextFactory.getExcludeCipherSuites()));
            intlogger.info("Supported ciphers prov server:-"
                                   + String.join(",", sslContextFactory.getIncludeCipherSuites()));

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

                // Servlet and Filter configuration
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

                //CADI Filter activation check
                if (Boolean.parseBoolean(provProperties.getProperty(
                        "org.onap.dmaap.datarouter.provserver.cadi.enabled", "false"))) {
                    //Get cadi properties
                    Properties cadiProperties = null;
                    try {
                        intlogger.info("PROV0001 Prov - Loading CADI properties");
                        cadiProperties = new Properties();
                        Inner obj = new Main().new Inner();
                        InputStream in = obj.getCadiProps();
                        cadiProperties.load(in);
                    } catch (IOException ioe) {
                        intlogger.error("PROV0001 Exception loading CADI properties: " + ioe.getMessage(), ioe);
                    }
                    cadiProperties.setProperty("aaf_locate_url", provProperties.getProperty("org.onap.dmaap.datarouter.provserver.cadi.aaf.url", "https://aaf-onap-test.osaaf.org:8095"));
                    intlogger.info("PROV0001  aaf_url set to - " + cadiProperties.getProperty("aaf_url"));

                    PropAccess access = new PropAccess(cadiProperties);
                    servletContextHandler.addFilter(new FilterHolder(new DRProvCadiFilter(true, access)),
                            "/*", EnumSet.of(DispatcherType.REQUEST));
                }

                ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
                contextHandlerCollection.addHandler(servletContextHandler);

                // Server's Handler collection
                handlerCollection = new HandlerCollection();
                handlerCollection.setHandlers(new Handler[]{contextHandlerCollection, new DefaultHandler()});
                handlerCollection.addHandler(requestLogHandler);

                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
            }
        }
        server.setHandler(handlerCollection);

        // Daemon to clean up the log directory on a daily basis
        Timer rolex = new Timer();
        rolex.scheduleAtFixedRate(new PurgeLogDirTask(), 0, 86400000L);    // run once per day

        // Start LogfileLoader
        LogfileLoader.getLoader();

        try {
            server.start();
            intlogger.info("Prov Server started-" + server.getState());
        } catch (Exception e) {
            intlogger.info("Jetty failed to start. Reporting will we unavailable: " + e.getMessage(), e);
        }
        server.join();
        intlogger.info("PROV0001 **** AT&T Data Router Provisioning Server halted.");
    }

    private static boolean checkDatabase() {
        DbItialization db = new DbItialization();
        return db.runRetroFits();
    }

    /**
     * Stop the Jetty server.
     */
    static void shutdown() {
        new Thread(() -> {
            try {
                server.stop();
                Thread.sleep(5000L);
                System.exit(0);
            } catch (Exception e) {
                intlogger.error("Exception in Main.shutdown(): " + e.getMessage(), e);
            }
        });
    }
}
