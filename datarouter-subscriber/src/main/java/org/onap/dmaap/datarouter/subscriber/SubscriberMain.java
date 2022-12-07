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

import java.util.Arrays;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SubscriberMain {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberMain.class);

    /**
     * Main class for Subscriber.
     * @param args standard args array
     * @throws Exception generic exception
     */
    public static void main(String[] args) throws Exception {
        SubscriberProps props = SubscriberProps.getInstance(
                System.getProperty("org.onap.dmaap.datarouter.subscriber.properties", "subscriber.properties"));
        int httpsPort = Integer.parseInt(props.getValue("org.onap.dmaap.datarouter.subscriber.https.port", "8443"));
        int httpPort = Integer.parseInt(props.getValue("org.onap.dmaap.datarouter.subscriber.http.port", "8080"));

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
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

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
            logger.debug("Excluded protocols for SubscriberMain:"
                                + Arrays.toString(sslContextFactory.getExcludeProtocols()));
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
        ctxt.setContextPath("/");
        server.setHandler(ctxt);

        ctxt.addServlet(new ServletHolder(new SampleSubscriberServlet()), "/*");
        try {
            server.start();
        } catch ( Exception e ) {
            logger.error("Jetty failed to start. Reporting will be unavailable-" + e);
        }
        server.join();
        logger.debug("org.onap.dmaap.datarouter.subscriber.SubscriberMain started-" + server.getState());

    }
}