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


import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class LogServletTest extends DrServletTestBase {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private static LogServlet logServlet;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

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
        logServlet = new LogServlet(true);
        setUpValidParameterValuesForMap();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Is_Not_Allowed_Then_Forbidden_Response_Is_Generated()
        throws Exception {
        logServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_FeedID_Is_Invalid_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_Type_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getParameter("type")).thenReturn("bad_type");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_PublishID_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        when(request.getParameter("publishId")).thenReturn("bad_PublishID'");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_StatusCode_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        when(request.getParameter("statusCode")).thenReturn("1'");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_ExpiryReason() throws Exception {
        when(request.getParameter("expiryReason")).thenReturn("bad_ExpiryReason");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_Start() throws Exception {
        when(request.getParameter("start")).thenReturn("bad_startTime");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Has_Bad_End() throws Exception {
        when(request.getParameter("end")).thenReturn("bad_endTime");
        logServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_FeedLog_A_STATUS_OK_Response_Is_Generated() {
        logServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Allowed_Then_Forbidden_Response_Is_Generated() throws Exception {
        logServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Allowed_Then_Forbidden_Response_Is_Generated() throws Exception {
        logServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_GetPublishRecordsForFeed_And_Type_Is_Publish_A_STATUS_OK_Response_Is_Generated() {
        when(request.getParameter("type")).thenReturn("pub");
        when(request.getParameter("expiryReason")).thenReturn(null);
        logServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_getDeliveryRecordsForFeed_And_Type_Is_Delivery_A_STATUS_OK_Response_Is_Generated() {
        when(request.getParameter("type")).thenReturn("del");
        when(request.getParameter("expiryReason")).thenReturn(null);
        logServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_getExpiryRecordsForFeed_And_Type_Is_Expire_A_STATUS_OK_Response_Is_Generated()
        throws Exception {
        when(request.getParameter("type")).thenReturn("exp");
        when(request.getParameter("statusCode")).thenReturn(null);
        when(request.getParameter("expiryReason")).thenReturn(null);
        ServletOutputStream s = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(s);
        logServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_getDeliveryRecordsForSubscription_And_Type_Is_Delivery_A_STATUS_OK_Response_Is_Generated() {
        LogServlet logServletNotFeedlog = new LogServlet(false);
        when(request.getParameter("type")).thenReturn("del");
        when(request.getParameter("statusCode")).thenReturn(null);
        when(request.getParameter("expiryReason")).thenReturn(null);
        logServletNotFeedlog.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_getExpiryRecordsForSubscription_And_Type_Is_Expiry_A_STATUS_OK_Response_Is_Generated() {
        LogServlet logServletNotFeedlog = new LogServlet(false);
        when(request.getParameter("type")).thenReturn("exp");
        when(request.getParameter("statusCode")).thenReturn(null);
        when(request.getParameter("expiryReason")).thenReturn(null);
        logServletNotFeedlog.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    private void setUpValidParameterValuesForMap() throws Exception {
        when(request.getPathInfo()).thenReturn("123");
        when(request.getParameter("type")).thenReturn("exp");
        when(request.getParameter("publishId")).thenReturn("bad_PublishID");
        when(request.getParameter("statusCode")).thenReturn("-1");
        when(request.getParameter("expiryReason")).thenReturn("other");
        when(request.getParameter("start")).thenReturn(null);
        when(request.getParameter("end")).thenReturn(null);
        ServletOutputStream s = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(s);
    }
}