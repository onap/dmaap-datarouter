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

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.onap.dmaap.datarouter.provisioning.utils.AafPropsUtils;
import org.onap.dmaap.datarouter.provisioning.utils.SynchronizerTask;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;

/**
 * This class is the base class for those servlets that need to proxy their requests from the standby to active server.
 * Its methods perform the proxy function to the active server. If the active server is not reachable, a 503
 * (SC_SERVICE_UNAVAILABLE) is returned.  Only DELETE/GET/PUT/POST are supported.
 *
 * @author Robert Eby
 * @version $Id: ProxyServlet.java,v 1.3 2014/03/24 18:47:10 eby Exp $
 */
@SuppressWarnings("serial")

public class ProxyServlet extends BaseServlet {

    private boolean inited = false;
    private Scheme sch;

    /**
     * Initialize this servlet, by setting up SSL.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            // Set up keystore
            String type = AafPropsUtils.KEYSTORE_TYPE_PROPERTY;
            String store = ProvRunner.getAafPropsUtils().getKeystorePathProperty();
            String pass = ProvRunner.getAafPropsUtils().getKeystorePassProperty();
            KeyStore keyStore = readStore(store, pass, type);
            // Set up truststore
            store = ProvRunner.getAafPropsUtils().getTruststorePathProperty();
            pass = ProvRunner.getAafPropsUtils().getTruststorePassProperty();
            KeyStore trustStore = readStore(store, pass, AafPropsUtils.TRUESTSTORE_TYPE_PROPERTY);

            // We are connecting with the node name, but the certificate will have the CNAME
            // So we need to accept a non-matching certificate name
            SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore,
                    ProvRunner.getAafPropsUtils().getKeystorePassProperty(), trustStore);
            socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            sch = new Scheme("https", 443, socketFactory);
            inited = true;
        } catch (Exception e) {
            intlogger.error("ProxyServlet.init: " + e.getMessage(), e);
        }
        intlogger.info("ProxyServlet: inited = " + inited);
    }

    private KeyStore readStore(String store, String pass, String type) throws KeyStoreException {
        KeyStore ks = KeyStore.getInstance(type);
        try (FileInputStream instream = new FileInputStream(new File(store))) {
            ks.load(instream, pass.toCharArray());
        } catch (FileNotFoundException fileNotFoundException) {
            intlogger.error("ProxyServlet.readStore: " + fileNotFoundException.getMessage(), fileNotFoundException);
        } catch (Exception x) {
            intlogger.error("READING TRUSTSTORE: " + x);
        }
        return ks;
    }

    /**
     * Return <i>true</i> if the requester has NOT set the <i>noproxy</i> CGI variable. If they have, this indicates
     * they want to forcibly turn the proxy off.
     *
     * @param req the HTTP request
     * @return true or false
     */
    boolean isProxyOK(final HttpServletRequest req) {
        String str = req.getQueryString();
        if (str != null) {
            str = str.replaceAll("&amp;", "&");
            for (String s : str.split("&")) {
                if ("noproxy".equals(s) || s.startsWith("noproxy=")) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Is this the standby server?  If it is, the proxy functions can be used. If not, the proxy functions should not be
     * called, and will send a response of 500 (Internal Server Error).
     *
     * @return true if this server is the standby (and hence a proxy server).
     */
    boolean isProxyServer() {
        SynchronizerTask st = SynchronizerTask.getSynchronizer();
        return st.getPodState() == SynchronizerTask.STANDBY_POD;
    }

    /**
     * Issue a proxy DELETE to the active provisioning server.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        doProxy(req, resp, "DELETE");
    }

    /**
     * Issue a proxy GET to the active provisioning server.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doProxy(req, resp, "GET");
    }

    /**
     * Issue a proxy PUT to the active provisioning server.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        doProxy(req, resp, "PUT");
    }

    /**
     * Issue a proxy POST to the active provisioning server.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doProxy(req, resp, "POST");
    }

    /**
     * Issue a proxy GET to the active provisioning server.  Unlike doGet() above, this method will allow the caller to
     * fall back to other code if the remote server is unreachable.
     *
     * @return true if the proxy succeeded
     */
    boolean doGetWithFallback(HttpServletRequest req, HttpServletResponse resp) {
        boolean rv = false;
        if (inited) {
            String url = buildUrl(req);
            intlogger.info("ProxyServlet: proxying with fallback GET " + url);
            try (AbstractHttpClient httpclient = new DefaultHttpClient()) {
                HttpRequestBase proxy = new HttpGet(url);
                try {
                    httpclient.getConnectionManager().getSchemeRegistry().register(sch);

                    // Copy request headers and request body
                    copyRequestHeaders(req, proxy);

                    // Execute the request
                    HttpResponse pxyResponse = httpclient.execute(proxy);

                    // Get response headers and body
                    int code = pxyResponse.getStatusLine().getStatusCode();
                    resp.setStatus(code);
                    copyResponseHeaders(pxyResponse, resp);
                    copyEntityContent(pxyResponse, resp);
                    rv = true;

                } catch (IOException e) {
                    intlogger.error("ProxyServlet.doGetWithFallback: " + e.getMessage(), e);
                } finally {
                    proxy.releaseConnection();
                    httpclient.getConnectionManager().shutdown();
                }
            }
        } else {
            intlogger.warn("ProxyServlet: proxy disabled");
        }
        return rv;
    }

    private void doProxy(HttpServletRequest req, HttpServletResponse resp, final String method) {
        if (inited && isProxyServer()) {
            String url = buildUrl(req);
            intlogger.info("ProxyServlet: proxying " + method + " " + url);
            try (AbstractHttpClient httpclient = new DefaultHttpClient()) {
                ProxyHttpRequest proxy = new ProxyHttpRequest(method, url);
                try {
                    httpclient.getConnectionManager().getSchemeRegistry().register(sch);

                    // Copy request headers and request body
                    copyRequestHeaders(req, proxy);

                    handlePutOrPost(req, method, proxy);

                    // Execute the request
                    HttpResponse pxyResponse = httpclient.execute(proxy);

                    // Get response headers and body
                    int code = pxyResponse.getStatusLine().getStatusCode();
                    resp.setStatus(code);
                    copyResponseHeaders(pxyResponse, resp);
                    copyEntityContent(pxyResponse, resp);
                } catch (IOException e) {
                    intlogger.warn("ProxyServlet.doProxy: " + e.getMessage(), e);
                    sendResponseError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "", intlogger);
                } finally {
                    proxy.releaseConnection();
                    httpclient.getConnectionManager().shutdown();
                }
            }
        } else {
            intlogger.warn("ProxyServlet: proxy disabled");
            sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, intlogger);
        }
    }

    private void handlePutOrPost(HttpServletRequest req, String method, ProxyHttpRequest proxy) throws IOException {
        if ("POST".equals(method) || "PUT".equals(method)) {
            BasicHttpEntity body = new BasicHttpEntity();
            body.setContent(req.getInputStream());
            body.setContentLength(-1);    // -1 = unknown
            proxy.setEntity(body);
        }
    }

    private String buildUrl(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder("https://");
        sb.append(URLUtilities.getPeerPodName());
        sb.append(req.getRequestURI());
        String query = req.getQueryString();
        if (query != null) {
            sb.append("?").append(query);
        }
        return sb.toString();
    }

    private void copyRequestHeaders(HttpServletRequest from, HttpRequestBase to) {
        List<String> list = Collections.list(from.getHeaderNames());
        for (String name : list) {
            // Proxy code will add this one
            if (!"Content-Length".equalsIgnoreCase(name)) {
                to.addHeader(name, from.getHeader(name));
            }
        }
    }

    void copyResponseHeaders(HttpResponse from, HttpServletResponse to) {
        for (Header hdr : from.getAllHeaders()) {
            // Don't copy Date: our Jetty will add another Date header
            if (!"Date".equals(hdr.getName())) {
                to.addHeader(hdr.getName(), hdr.getValue());
            }
        }
    }

    void copyEntityContent(HttpResponse pxyResponse, HttpServletResponse resp) {
        HttpEntity entity = pxyResponse.getEntity();
        if (entity != null) {
            try (InputStream in = entity.getContent()) {
                IOUtils.copy(in, resp.getOutputStream());
            } catch (Exception e) {
                intlogger.error("ProxyServlet.copyEntityContent: " + e.getMessage(), e);
            }
        }
    }

    public static class ProxyHttpRequest extends HttpEntityEnclosingRequestBase {

        private final String method;

        ProxyHttpRequest(final String method, final String uri) {
            super();
            this.method = method;
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}
