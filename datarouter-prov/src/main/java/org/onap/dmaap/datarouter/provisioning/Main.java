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

import java.security.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;
import org.onap.dmaap.datarouter.provisioning.utils.PurgeLogDirTask;
import org.onap.dmaap.datarouter.provisioning.utils.ThrottleFilter;

import javax.servlet.DispatcherType;

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
     * The truststore to use if none is specified
     */
    public static final String DEFAULT_TRUSTSTORE = "/opt/java/jdk/jdk180/jre/lib/security/cacerts";
    public static final String KEYSTORE_TYPE_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.type";
    public static final String KEYSTORE_PATH_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.path";
    public static final String KEYSTORE_PASS_PROPERTY = "org.onap.dmaap.datarouter.provserver.keystore.password";
    public static final String TRUSTSTORE_PATH_PROPERTY = "org.onap.dmaap.datarouter.provserver.truststore.path";
    public static final String TRUSTSTORE_PASS_PROPERTY = "org.onap.dmaap.datarouter.provserver.truststore.password";

    /**
     * The one and only {@link Server} instance in this JVM
     */
    private static Server server;

    /**
     * Starts the Data Router Provisioning server.
     *
     * @param args not used
     * @throws Exception if Jetty has a problem starting
     */
    public static void main(String[] args) throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "4");
        Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.provisioning.internal");

        // Check DB is accessible and contains the expected tables
        if (!checkDatabase()) {
            System.exit(1);
        }

        logger.info("PROV0000 **** AT&T Data Router Provisioning Server starting....");

        // Get properties
        Properties p = (new DB()).getProperties();
        int httpPort = Integer.parseInt(p.getProperty("org.onap.dmaap.datarouter.provserver.http.port", "8080"));
        int httpsPort = Integer.parseInt(p.getProperty("org.onap.dmaap.datarouter.provserver.https.port", "8443"));

        // HTTP configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.setSecurePort(httpsPort);
        httpConfiguration.setOutputBufferSize(32768);
        httpConfiguration.setRequestHeaderSize(2048);
        httpConfiguration.setSendServerVersion(true);
        httpConfiguration.setSendDateHeader(false);

        // Server's thread pool
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setMinThreads(10);
        queuedThreadPool.setMaxThreads(200);
        queuedThreadPool.setDetailedDump(false);

        // The server itself
        server = new Server(queuedThreadPool);

        // HTTP connector
        HandlerCollection hc;
        try (ServerConnector httpServerConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfiguration))) {
            httpServerConnector.setPort(httpPort);
            httpServerConnector.setAcceptQueueSize(2);
            httpServerConnector.setIdleTimeout(300000);

            // HTTPS configuration
            HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.setRequestHeaderSize(8192);

            // HTTPS connector
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(p.getProperty(KEYSTORE_PATH_PROPERTY));
            sslContextFactory.setKeyStorePassword(p.getProperty(KEYSTORE_PASS_PROPERTY));
            sslContextFactory
                    .setKeyManagerPassword(p.getProperty("org.onap.dmaap.datarouter.provserver.keymanager.password"));
            // SSL stuff
            /* Skip SSLv3 Fixes */
            sslContextFactory.addExcludeProtocols("SSLv3");
            logger.info("Excluded protocols prov-" + Arrays.toString(sslContextFactory.getExcludeProtocols()));
            /* End of SSLv3 Fixes */

            try (ServerConnector httpsServerConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfiguration))) {
                httpsServerConnector.setPort(httpsPort);
                httpsServerConnector.setIdleTimeout(30000);
                httpsServerConnector.setAcceptQueueSize(2);

                sslContextFactory.setKeyStoreType(p.getProperty(KEYSTORE_TYPE_PROPERTY, "jks"));
                sslContextFactory.setKeyStorePath(p.getProperty(KEYSTORE_PATH_PROPERTY));
                sslContextFactory.setKeyStorePassword(p.getProperty(KEYSTORE_PASS_PROPERTY));
                sslContextFactory
                        .setKeyManagerPassword(p.getProperty("org.onap.dmaap.datarouter.provserver.keymanager.password"));

                String ts = p.getProperty(TRUSTSTORE_PATH_PROPERTY);
                if (ts != null && ts.length() > 0) {
                    logger.info("@@ TS -> " + ts);
                    sslContextFactory.setTrustStorePath(ts);
                    sslContextFactory.setTrustStorePassword(p.getProperty(TRUSTSTORE_PASS_PROPERTY));
                } else {
                    sslContextFactory.setTrustStorePath(DEFAULT_TRUSTSTORE);
                    sslContextFactory.setTrustStorePassword("changeit");
                }
                sslContextFactory.setWantClientAuth(true);

                // Servlet and Filter configuration
                ServletContextHandler ctxt = new ServletContextHandler(0);
                ctxt.setContextPath("/");
                ctxt.addServlet(new ServletHolder(new FeedServlet()), "/feed/*");
                ctxt.addServlet(new ServletHolder(new FeedLogServlet()), "/feedlog/*");
                ctxt.addServlet(new ServletHolder(new PublishServlet()), "/publish/*");
                ctxt.addServlet(new ServletHolder(new SubscribeServlet()), "/subscribe/*");
                ctxt.addServlet(new ServletHolder(new StatisticsServlet()), "/statistics/*");
                ctxt.addServlet(new ServletHolder(new SubLogServlet()), "/sublog/*");
                ctxt.addServlet(new ServletHolder(new GroupServlet()),
                        "/group/*"); //Provision groups - Rally US708115 -1610
                ctxt.addServlet(new ServletHolder(new SubscriptionServlet()), "/subs/*");
                ctxt.addServlet(new ServletHolder(new InternalServlet()), "/internal/*");
                ctxt.addServlet(new ServletHolder(new RouteServlet()), "/internal/route/*");
                ctxt.addServlet(new ServletHolder(new DRFeedsServlet()), "/");
                ctxt.addFilter(new FilterHolder(new ThrottleFilter()), "/publish/*", EnumSet.of(DispatcherType.REQUEST));

                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.addHandler(ctxt);

                // Request log configuration
                NCSARequestLog nrl = new NCSARequestLog();
                nrl.setFilename(
                        p.getProperty("org.onap.dmaap.datarouter.provserver.accesslog.dir") + "/request.log.yyyy_mm_dd");
                nrl.setFilenameDateFormat("yyyyMMdd");
                nrl.setRetainDays(90);
                nrl.setAppend(true);
                nrl.setExtended(false);
                nrl.setLogCookies(false);
                nrl.setLogTimeZone("GMT");

                RequestLogHandler reqlog = new RequestLogHandler();
                reqlog.setRequestLog(nrl);

                // Server's Handler collection
                hc = new HandlerCollection();
                hc.setHandlers(new Handler[]{contexts, new DefaultHandler()});
                hc.addHandler(reqlog);

                // Daemon to clean up the log directory on a daily basis
                Timer rolex = new Timer();
                rolex.scheduleAtFixedRate(new PurgeLogDirTask(), 0, 86400000L);    // run once per day

                // Start LogfileLoader
                LogfileLoader.getLoader();

                try (ServerConnector serverConnector = new ServerConnector(server,
                        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                        new HttpConnectionFactory(httpsConfiguration))) {
                    serverConnector.setPort(httpsPort);
                    serverConnector.setIdleTimeout(500000);
                }

                server.setConnectors(new Connector[]{httpServerConnector, httpsServerConnector});
            }
        }
        server.setHandler(hc);
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);

        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);

        server.start();
        server.join();
        logger.info("PROV0001 **** AT&T Data Router Provisioning Server halted.");
    }

    private static boolean checkDatabase() {
        DB db = new DB();
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
                // ignore
            }
        });
    }
}
