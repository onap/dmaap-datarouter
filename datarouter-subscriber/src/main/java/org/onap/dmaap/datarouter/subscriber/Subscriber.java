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

package org.onap.dmaap.datarouter.subscriber;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.ssl.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.http.HttpVersion;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Subscriber {

    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.subscriber.Subscriber");

    private static final String CONTEXT_PATH = "/";
    private static final String URL_PATTERN = "/*";

    static Properties props;

    private static void loadProps() {
        if (props == null) {
            props = new Properties();
            try {
                props.load(new FileInputStream(System.getProperty(
                        "org.onap.dmaap.datarouter.subscriber.properties",
                        "/opt/app/subscriber/etc/subscriber.properties")));
            } catch (IOException e) {
                logger.fatal("SubServlet: Exception opening properties: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //Load the properties
        loadProps();

        int httpsPort = Integer.parseInt(props.getProperty("org.onap.dmaap.datarouter.subscriber.https.port", "8443"));
        int httpPort = Integer.parseInt(props.getProperty("org.onap.dmaap.datarouter.subscriber.http.port", "8080"));

        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRequestHeaderSize(8192);

        // HTTP connector
        ServletContextHandler ctxt;
        try (ServerConnector httpServerConnector = new ServerConnector(server,
                new HttpConnectionFactory(httpConfig))) {
            httpServerConnector.setPort(httpPort);
            httpServerConnector.setIdleTimeout(30000);

            // SSL Context Factory
            SslContextFactory sslContextFactory = new SslContextFactory();

            // SSL HTTP Configuration
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            // SSL Connector
            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(httpsPort);
            server.addConnector(sslConnector);

            /*Skip SSLv3 Fixes*/
            sslContextFactory.addExcludeProtocols("SSLv3");
            logger.info("Excluded protocols for Subscriber:" + Arrays.toString(sslContextFactory.getExcludeProtocols()));
            /*End of SSLv3 Fixes*/

            // HTTPS Configuration
            try (ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig))) {
                https.setPort(httpsPort);
                https.setIdleTimeout(30000);
            }
            server.setConnectors(new Connector[]{ httpServerConnector });
        }
        ctxt = new ServletContextHandler(0);
        ctxt.setContextPath(CONTEXT_PATH);
        server.setHandler(ctxt);

        ctxt.addServlet(new ServletHolder(new SubscriberServlet()), URL_PATTERN);
        try {
            server.start();
        } catch ( Exception e ) {
            logger.info("Jetty failed to start. Reporting will be unavailable-"+e);
        }
        server.join();
        logger.info("org.onap.dmaap.datarouter.subscriber.Subscriber started-"+ server.getState());

    }
}