/*-
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.dmaap.datarouter.provisioning.utils.SynchronizerTask;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({URLUtilities.class, BaseServlet.class, Scheme.class})
@PowerMockIgnore({"javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class ProxyServletTest {
    private static ProxyServlet proxyServlet;
    private static EntityManagerFactory emf;
    private static EntityManager em;

    @Mock
    private DefaultHttpClient httpClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private CloseableHttpResponse response;
    @Mock
    private StatusLine statusLine;

    @BeforeClass
    public static void init() {
        emf = Persistence.createEntityManagerFactory("dr-unit-tests");
        em = emf.createEntityManager();
        System.setProperty(
                "org.onap.dmaap.datarouter.provserver.properties",
                "src/test/resources/h2Database.properties");
    }

    @AfterClass
    public static void tearDownClass() {
        em.clear();
        em.close();
        emf.close();
    }

    @Before
    public void setUp() throws Exception {
        proxyServlet = new ProxyServlet();
        FieldUtils.writeDeclaredField(proxyServlet, "inited", true, true);

        org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = PowerMockito.mock(org.apache.http.conn.ssl.SSLSocketFactory.class);
        Mockito.doNothing().when(sslSocketFactory).setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme scheme = new Scheme("http", 80, sslSocketFactory);
        FieldUtils.writeDeclaredField(proxyServlet, "sch", scheme, true);

        PowerMockito.mockStatic(BaseServlet.class);
        PowerMockito.mockStatic(URLUtilities.class);
        when(URLUtilities.getPeerPodName()).thenReturn("localhost");

        SynchronizerTask synchronizerTask = SynchronizerTask.getSynchronizer();
        FieldUtils.writeDeclaredField(synchronizerTask, "podState", 2, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "synctask", synchronizerTask, true);

        proxyServlet.init(mock(ServletConfig.class));
        setHeadersForValidRequest();
        mockHttpClientForGetRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_POST_Exception_Is_Thrown() {
        proxyServlet.doPost(request, servletResponse);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_Exception_Is_Thrown() {
        proxyServlet.doDelete(request, servletResponse);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_Exception_Is_Thrown() {
        proxyServlet.doGet(request, servletResponse);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_Exception_Is_Thrown() {
        proxyServlet.doPut(request, servletResponse);
    }

    @Test
    public void Given_Request_Is_HTTP_GetWithFallBack_Exception_Is_Thrown() {
        proxyServlet.doGetWithFallback(request, servletResponse);
    }

    @Test
    public void Veirfy_Headers_Are_Copied() {
        HttpResponse proxyResponse = PowerMockito.mock(HttpResponse.class);
        Header[] headersArray = new Header[2];
        headersArray[0] = new BasicHeader("cache-control", "public, max-age=86400");
        headersArray[1] = new BasicHeader("content-type", "text/plain");
        PowerMockito.when(proxyResponse.getAllHeaders()).thenReturn(headersArray);
        proxyServlet.copyResponseHeaders(proxyResponse, servletResponse);
    }

    @Test
    public void Veirfy_EntitiyContent_Copied() throws IOException {
        HttpResponse proxyResponse = PowerMockito.mock(HttpResponse.class);
        HttpEntity entity = PowerMockito.mock(HttpEntity.class);
        PowerMockito.when(proxyResponse.getEntity()).thenReturn(entity);
        PowerMockito.when(entity.getContent()).thenReturn(new ByteArrayInputStream("blah".getBytes()));
        proxyServlet.copyEntityContent(proxyResponse, servletResponse);
    }

    private void setHeadersForValidRequest() {
        List<String> headers = new ArrayList<>();
        headers.add("Content-Type");
        headers.add("X-DMAAP-DR-ON-BEHALF-OF");
        headers.add("X-DMAAP-DR-META");
        Enumeration<String> headerNames = Collections.enumeration(headers);
        Mockito.when(request.getHeaderNames()).thenReturn(headerNames);
        Enumeration<String> contentTypeHeader = Collections.enumeration(Collections.singletonList("text/plain"));
        Enumeration<String> behalfHeader = Collections.enumeration(Collections.singletonList("User1"));
        Enumeration<String> metaDataHeader = Collections.enumeration(Collections.singletonList("?#@><"));
        Mockito.when(request.getHeaders("Content-Type")).thenReturn(contentTypeHeader);
        Mockito.when(request.getHeaders("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn(behalfHeader);
        Mockito.when(request.getHeaders("X-DMAAP-DR-META")).thenReturn(metaDataHeader);
    }

    private void mockHttpClientForGetRequest() throws Exception {
        PowerMockito.when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        Mockito.when(request.getRequestURI()).thenReturn("/api");
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
    }
}
