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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.onap.dmaap.datarouter.provisioning.beans.Deleteable;
import org.onap.dmaap.datarouter.provisioning.beans.SubDelivery;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.beans.Updateable;
import org.onap.dmaap.datarouter.provisioning.utils.Poker;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class SubscriptionServletTest extends DrServletTestBase {
    private static EntityManagerFactory emf;
    private static EntityManager em;
    private SubscriptionServlet subscriptionServlet;
    private final String URL= "https://172.100.0.5";
    private final String USER = "user1";
    private final String PASSWORD="password1";


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
        listAppender = setTestLogger(SubscriptionServlet.class);
        subscriptionServlet = new SubscriptionServlet();
        setAuthoriserToReturnRequestIsAuthorized();
        setPokerToNotCreateTimersWhenDeleteSubscriptionIsCalled();
        setupValidAuthorisedRequest();
        setUpValidSecurityOnHttpRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_SC_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", true, true);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Fails_An_Internal_Server_Error_Is_Reported() throws Exception {
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet(){
            public boolean doDelete(Deleteable deletable){
                return false;
            }
        };
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        subscriptionServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", true, true);
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("follow_redirect", false);
                jo.put("decompress", true);
                jo.put("sync", true);
                jo.put("changeowner", true);
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_ContentType");
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscription_Object_Is_Invalid_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscriber_Modified_By_Different_Creator_Then_Bad_Request_Is_Generated() throws Exception {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn(null);
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("privilegedSubscriber", true);
                jo.put("decompress", true);
                jo.put("delivery", JSObject);
                jo.put("follow_redirect", false);
                jo.put("subscriber", "differentSubscriber");
                jo.put("sync", true);
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Update_Fails() throws Exception {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("privilegedSubscriber", true);
                jo.put("delivery", JSObject);
                jo.put("decompress", true);
                jo.put("follow_redirect", false);
                jo.put("sync", true);
                return jo;
            }

            @Override
            protected boolean doUpdate(Updateable bean) {
                return false;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Update_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("privilegedSubscriber", true);
                jo.put("decompress", true);
                jo.put("delivery", JSObject);
                jo.put("follow_redirect", false);
                jo.put("sync", true);
                jo.put("changeowner", true);
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        //changeSubscriptionBackToNormal();
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/123");
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_ContentType");
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getHeader(anyString())).thenReturn("application/vnd.dmaap-dr.subscription-control");
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription-control; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Post_Fails() throws Exception {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription-control; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                return jo;
            }
        };
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Post_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.subscription-control; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            public JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("privilegedSubscriber", false);
                jo.put("follow_redirect", false);
                jo.put("decompress", false);
                jo.put("failed", false);
                return jo;
            }
        };
        subscriptionServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_ACCEPTED));
        verifyEnteringExitCalled(listAppender);
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
        Set<String> authAddressesAndNetworks = new HashSet<String>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks, true);
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

    private void setPokerToNotCreateTimersWhenDeleteSubscriptionIsCalled() throws Exception {
        Poker poker = mock(Poker.class);
        FieldUtils.writeDeclaredStaticField(Poker.class, "poker", poker, true);
    }

    private void setupValidAuthorisedRequest() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
    }

    private void changeSubscriptionBackToNormal() throws SQLException {
        Subscription subscription = new Subscription("https://172.100.0.5", "user1", "password1");
        subscription.setSubid(1);
        subscription.setSubscriber("user1");
        subscription.setFeedid(1);
        SubDelivery subDelivery = new SubDelivery(URL, USER, PASSWORD, true);
        subscription.setDelivery(subDelivery);
        subscription.setGroupid(1);
        subscription.setMetadataOnly(false);
        subscription.setSuspended(false);
        subscription.setPrivilegedSubscriber(false);
        subscription.setDecompress(false);
        subscription.changeOwnerShip();
        try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
            subscription.doUpdate(conn);
        }
    }

    private void resetAafSubscriptionInDB() throws SQLException {
        Subscription subscription = new Subscription("https://172.100.0.5:8080", "user2", "password2");
        subscription.setSubid(2);
        subscription.setSubscriber("user2");
        subscription.setFeedid(1);
        SubDelivery subDelivery = new SubDelivery(URL, USER, PASSWORD, true);
        subscription.setDelivery(subDelivery);
        subscription.setGroupid(1);
        subscription.setMetadataOnly(false);
        subscription.setSuspended(false);
        subscription.setDecompress(false);
        subscription.setPrivilegedSubscriber(false);
        try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
            subscription.doUpdate(conn);
        }
    }

    private void addNewSubscriptionInDB() throws SQLException {
        Subscription subscription = new Subscription("https://172.100.0.6:8080", "user3", "password3");
        subscription.setSubid(3);
        subscription.setSubscriber("user3");
        subscription.setFeedid(1);
        SubDelivery subDelivery = new SubDelivery(URL, USER, PASSWORD, true);
        subscription.setDelivery(subDelivery);
        subscription.setGroupid(1);
        subscription.setMetadataOnly(false);
        subscription.setSuspended(false);
        subscription.setDecompress(false);
        try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
            subscription.doInsert(conn);
        }
    }
}