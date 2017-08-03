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


import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.ssl.*;
import org.eclipse.jetty.server.*;
import org.apache.log4j.Logger;

/**
 *	Example stand alone subscriber
 */
public class SSASubscriber {
	private static final int Port = 8447;
	private static final String KeyStoreType = "jks";
	private static final String KeyStoreFile = "/root/sub/subscriber.jks";
	//private static final String KeyStoreFile = "c:/tmp/subscriber.jks";
	private static final String KeyStorePassword = "changeit";
	private static final String KeyPassword = "changeit";
	private static final String ContextPath = "/";
	private static final String URLPattern = "/*";

	public static void main(String[] args) throws Exception {
		//User story # US792630  -Jetty Upgrade to 9.3.11
		//SSASubscriber register Jetty server.
        Server server = new Server();
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(Port);
        http_config.setRequestHeaderSize(8192);
		
        // HTTP connector
        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        http.setPort(7070);
        http.setIdleTimeout(30000);
        
        // SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreType(KeyStoreType);
        sslContextFactory.setKeyStorePath(KeyStoreFile);
        sslContextFactory.setKeyStorePassword(KeyStorePassword);
        sslContextFactory.setKeyManagerPassword(KeyPassword);
        
        // sslContextFactory.setTrustStorePath(ncm.getKSFile());
        // sslContextFactory.setTrustStorePassword("changeit");
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

        // SSL HTTP Configuration
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(https_config));
        sslConnector.setPort(Port);
        server.addConnector(sslConnector);
        
    	/**Skip SSLv3 Fixes*/
        sslContextFactory.addExcludeProtocols("SSLv3");
        System.out.println("Excluded protocols SSASubscriber-"+sslContextFactory.getExcludeProtocols().toString());  
		/**End of SSLv3 Fixes*/
        
        // HTTPS Configuration
        ServerConnector https = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(Port);
        https.setIdleTimeout(30000);
        //server.setConnectors(new Connector[] { http, https });
        server.setConnectors(new Connector[] {  http });
		ServletContextHandler ctxt = new ServletContextHandler(0);
		ctxt.setContextPath(ContextPath);
		server.setHandler(ctxt);
		
		ctxt.addServlet(new ServletHolder(new SubscriberServlet()), "/*");
		
		try { 
		    server.start();
		} catch ( Exception e ) { 
			System.out.println("Jetty failed to start. Reporting will we unavailable-"+e);
		};
        server.join();
        
        System.out.println("Subscriber started-"+ server.getState());  

	}
}