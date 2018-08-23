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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Deleteable;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.beans.Updateable;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Subscription")
public class SubscriptionServletTest extends DrServletTestBase {
    private SubscriptionServlet subscriptionServlet;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        super.setUp();
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
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setSubscriptionToReturnInvalidSubscriptionIdSupplied();
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Fails_An_Internal_Server_Error_Is_Reported() throws Exception {
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet(){
            public boolean doDelete(Deleteable deletable){
                return false;
            }
        };
        subscriptionServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Succeeds_A_NO_CONTENT_Response_Is_Generated() throws Exception {
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet(){
            public boolean doDelete(Deleteable deletable){
                return true;
            }
        };
        subscriptionServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setSubscriptionToReturnInvalidSubscriptionIdSupplied();
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds() throws Exception {
        JSONObject JSObject = buildRequestJsonObject();
        JSONObject jo = new JSONObject();
        jo.put("name", "stub_name");
        jo.put("version", "2.0");
        jo.put("metadataOnly", true);
        jo.put("suspend", true);
        jo.put("delivery", JSObject);
        jo.put("sync", true);
        Subscription sub = new Subscription(jo);
        PowerMockito.mockStatic(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(sub);
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        subscriptionServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setSubscriptionToReturnInvalidSubscriptionIdSupplied();
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_ContentType");
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscription_Object_Is_Invalid_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription; version=1.0");
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Subscriber_Modified_By_Different_Creator() throws Exception {
        when(request.getHeader("X-ATT-DR-ON-BEHALF-OF-GROUP")).thenReturn(null);
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("sync", true);
                return jo;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Update_Fails() throws Exception {
        when(request.getHeader("X-ATT-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("sync", true);
                return jo;
            }

            @Override
            protected boolean doUpdate(Updateable bean) {
                return false;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Update_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getHeader("X-ATT-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("sync", true);
                return jo;
            }

            @Override
            protected boolean doUpdate(Updateable bean) {
                return true;
            }
        };
        subscriptionServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Subscription_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setSubscriptionToReturnInvalidSubscriptionIdSupplied();
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_ContentType");
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getHeader(anyString())).thenReturn("application/vnd.att-dr.subscription-control");
        setAuthoriserToReturnRequestNotAuthorized();
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription-control; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        subscriptionServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Post_Fails() throws Exception {
        when(request.getHeader("X-ATT-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription-control; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
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
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Post_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getHeader("X-ATT-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.subscription-control; version=1.0");
        JSONObject JSObject = buildRequestJsonObject();
        SubscriptionServlet subscriptionServlet = new SubscriptionServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("metadataOnly", true);
                jo.put("suspend", true);
                jo.put("delivery", JSObject);
                jo.put("failed", false);
                return jo;
            }
        };
        subscriptionServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_ACCEPTED));
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
        when(request.getPathInfo()).thenReturn("/123");
    }

    private void setSubscriptionToReturnInvalidSubscriptionIdSupplied() {
        PowerMockito.mockStatic(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(null);
    }

    private void setSubscriptionToReturnValidSubscriptionForSuppliedId() {
        PowerMockito.mockStatic(Subscription.class);
        Subscription subscription = mock(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(subscription);
        when(subscription.getSubscriber()).thenReturn("Stub_Value");
        when(subscription.asJSONObject()).thenReturn(mock(JSONObject.class));
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
        setSubscriptionToReturnValidSubscriptionForSuppliedId();
    }
}