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
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Insertable;
import org.onap.dmaap.datarouter.provisioning.utils.Poker;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class SubscribeServletTest extends DrServletTestBase {
    private static SubscribeServlet subscribeServlet;
    private static EntityManagerFactory emf;
    private static EntityManager em;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

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
        ProvDbUtils.getInstance().initProvDB();
        listAppender = setTestLogger(SubscribeServlet.class);
        subscribeServlet = new SubscribeServlet();
        setAuthoriserToReturnRequestIsAuthorized();
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();
        setupValidAuthorisedRequest();
        setUpValidSecurityOnHttpRequest();
        setUpValidContentHeadersAndJSONOnHttpRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        subscribeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        subscribeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscribeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscribeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscribeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/1");
        subscribeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }


    @Test
    public void Given_Request_Is_HTTP_PUT_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        subscribeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), anyString());
        verifyEnteringExitCalled(listAppender);
    }
    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        when(request.getPathInfo()).thenReturn("/1");
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
             public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("sync", false);
                return jo;
            }
            @Override
            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_AAF_Subscriber_Added_To_Legacy_Feed_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/1");
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("aaf_instance", "*");
                jo.put("follow_redirect", false);
                jo.put("sync", false);
                return jo;
            }
            @Override
            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("AAF Subscriber can not be added to legacy Feed"));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Legacy_Subscriber_Added_To_AAF_Feed_And_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("aaf_instance", "legacy");
                jo.put("follow_redirect", false);
                jo.put("sync", false);
                return jo;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("Policy Engine disallows access."));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_AAF_Subscriber_Added_To_AAF_Feed_Without_Permissions_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("aaf_instance", "*");
                jo.put("follow_redirect", false);
                jo.put("sync", false);
                return jo;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), contains("AAF disallows access to permission"));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_AAF_Subscriber_Added_To_AAF_Feed_With_Permissions_Then_OK_Response_Is_Generated() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getPathInfo()).thenReturn("/2");
        when(request.isUserInRole("org.onap.dmaap-dr.feed|*|approveSub")).thenReturn(true);
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("aaf_instance", "*");
                jo.put("follow_redirect", false);
                jo.put("sync", false);
                return jo;
            }

            @Override
            protected boolean doInsert(Insertable bean) {
                return true;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_CREATED));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.feed; version=1.1");
        when(request.getContentType()).thenReturn("stub_contentType");
        when(request.getPathInfo()).thenReturn("/1");
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/1");
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Active_Feeds_Equals_Max_Feeds_Then_Bad_Request_Response_Is_Generated() throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "maxSubs", 0, true);
        when(request.getPathInfo()).thenReturn("/1");
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                return new JSONObject();
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_CONFLICT), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_POST_Fails_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/2");
        JSONObject JSObject = buildRequestJsonObject();
        SubscribeServlet subscribeServlet = new SubscribeServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("aaf_instance", "legacy");
                jo.put("follow_redirect", false);
                jo.put("sync", false);
                return jo;
            }

            @Override
            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };
        subscribeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @NotNull
    private JSONObject buildRequestJsonObject() {
        JSONObject JSObject = new JSONObject();
        JSObject.put("url", "https://stub_address");
        JSObject.put("use100", "true");
        JSObject.put("password", "stub_password");
        JSObject.put("user", "stub_user");
        return JSObject;
    }

    private void setUpValidSecurityOnHttpRequest() throws Exception {
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", false, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "maxSubs", 100, true);
    }

    private void setBehalfHeader(String headerValue) {
        when(request.getHeader(BEHALF_HEADER)).thenReturn(headerValue);
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

    private void setPokerToNotCreateTimersWhenDeleteFeedIsCalled() throws Exception {
        Poker poker = mock(Poker.class);
        FieldUtils.writeDeclaredStaticField(Poker.class, "poker", poker, true);
    }

    private void setupValidAuthorisedRequest() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
    }

    private void setUpValidContentHeadersAndJSONOnHttpRequest() {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");

    }
}
