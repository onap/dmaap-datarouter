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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Updateable;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class FeedServletTest extends DrServletTestBase {

    private static FeedServlet feedServlet;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private static EntityManagerFactory emf;
    private static EntityManager em;

    private ListAppender<ILoggingEvent> listAppender;

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
        listAppender = setTestLogger(FeedServlet.class);
        feedServlet = new FeedServlet();
        setAuthoriserToReturnRequestIsAuthorized();
        setUpValidAuthorisedRequest();
        setUpValidSecurityOnHttpRequest();
        setUpValidContentHeadersAndJSONOnHttpRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
        throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        setBehalfHeader(null);
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_AAF_Feed_Without_Permissions_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        feedServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("AAF disallows access to permission"));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_AAF_Feed_With_Permissions_Then_A_NO_CONTENT_Response_Is_Generated() {
        when(request.getPathInfo()).thenReturn("/3");
        when(request.isUserInRole("org.onap.dmaap-dr.feed|*|delete")).thenReturn(true);
        feedServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Fails_An_Internal_Server_Error_Is_Reported()
        throws Exception {
        FeedServlet feedServlet = new FeedServlet() {
            protected boolean doUpdate(Updateable bean) {
                return false;
            }
        };
        feedServlet.doDelete(request, response);
        verify(response)
            .sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Succeeds_A_NO_CONTENT_Response_Is_Generated() throws Exception {
        feedServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
        reinsertFeedIntoDb();
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
        throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        setBehalfHeader(null);
        feedServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated()
        throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        feedServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated()
        throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        when(request.getPathInfo()).thenReturn("/2");
        feedServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/2");
        feedServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
        throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        setBehalfHeader(null);
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated()
        throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated()
        throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.feed-fail; version=2.0");
        when(request.getContentType()).thenReturn("stub_contentType");
        when(request.getPathInfo()).thenReturn("/2");
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated()
        throws Exception {
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        when(request.getPathInfo()).thenReturn("/2");
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                return null;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("Badly formed JSON"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Contains_Invalid_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                return new JSONObject();
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Feed_Change_Is_Not_Publisher_Who_Requested_Feed_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn(null);
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "1.0");
                jo.put("authorization", JSObject);
                return jo;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("must be modified by the same publisher"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Feed_Name_Change_is_Requested_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "not_stub_name");
                jo.put("version", "1.0");
                jo.put("authorization", JSObject);
                return jo;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("name of the feed may not be updated"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Feed_Version_Change_is_Requested_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.2");
                jo.put("authorization", JSObject);
                return jo;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("version of the feed may not be updated"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.1");
                jo.put("authorization", JSObject);
                return jo;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("Policy Engine disallows access"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_AAF_Feed_Without_Permissions_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.1");
                jo.put("authorization", JSObject);
                jo.put("aaf_instance", "https://aaf-onap-test.osaaf.org:8095");
                return jo;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("AAF disallows access to permission"));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_AAF_Feed_With_Permissions_Then_STATUS_OK__Response_Is_Generated() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/2");
        when(request.isUserInRole("org.onap.dmaap-dr.feed|*|edit")).thenReturn(true);
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.1");
                jo.put("authorization", JSObject);
                jo.put("aaf_instance", "*");
                return jo;
            }
            @Override
            protected boolean doUpdate(Updateable bean) {
                return true;
            }

        };
        feedServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Change_On_Feeds_Fails_An_Internal_Server_Error_Response_Is_Generated() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.1");
                jo.put("authorization", JSObject);
                return jo;
            }

            @Override
            protected boolean doUpdate(Updateable bean) {
                return false;
            }
        };
        feedServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Change_On_Feeds_Suceeds_A_STATUS_OK_Response_Is_Generated() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        FeedServlet feedServlet = new FeedServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "AafFeed");
                jo.put("version", "v0.1");
                jo.put("authorization", JSObject);
                return jo;
            }
            @Override
            protected boolean doUpdate(Updateable bean) {
                return true;
            }

        };
        feedServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_POST_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        feedServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @NotNull
    private JSONObject buildRequestJsonObject() {
        JSONObject JSObject = new JSONObject();
        JSONArray endpointIDs = new JSONArray();
        JSONObject JOEndpointIDs = new JSONObject();
        JOEndpointIDs.put("id", "stub_endpoint_id");
        JOEndpointIDs.put("password", "stub_endpoint_password");
        endpointIDs.put(JOEndpointIDs);

        JSONArray endpointAddresses = new JSONArray();
        endpointAddresses.put("127.0.0.1");

        JSObject.put("classification", "stub_classification");
        JSObject.put("endpoint_ids", endpointIDs);
        JSObject.put("endpoint_addrs", endpointAddresses);
        return JSObject;
    }

    private void setUpValidSecurityOnHttpRequest() throws Exception {
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks,true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", false, true);
    }

    private void setBehalfHeader(String headerValue) {
        when(request.getHeader(BEHALF_HEADER)).thenReturn(headerValue);
    }

    private void setValidPathInfoInHttpHeader() {
        when(request.getPathInfo()).thenReturn("/1");
    }

    private void setAuthoriserToReturnRequestNotAuthorized() throws IllegalAccessException {
        AuthorizationResponse authResponse = mock(AuthorizationResponse.class);
        Authorizer authorizer = mock(Authorizer.class);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authz", authorizer, true);
        when(authorizer.decide(request)).thenReturn(authResponse);
        when(authResponse.isAuthorized()).thenReturn(false);
    }

    private void setAuthoriserToReturnRequestIsAuthorized() throws IllegalAccessException {
        AuthorizationResponse authResponse = mock(AuthorizationResponse.class);
        Authorizer authorizer = mock(Authorizer.class);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authz", authorizer, true);
        when(authorizer.decide(request)).thenReturn(authResponse);
        when(authResponse.isAuthorized()).thenReturn(true);
    }

    private void setUpValidAuthorisedRequest() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
    }

    private void setUpValidContentHeadersAndJSONOnHttpRequest() {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.feed; version=1.0");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
    }

    private void reinsertFeedIntoDb() throws SQLException {
        Feed feed = new Feed("Feed1","v0.1", "First Feed for testing", "First Feed for testing");
        feed.setFeedid(1);
        feed.setGroupid(1);
        feed.setDeleted(false);
        try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
            feed.doUpdate(conn);
        }
    }
}