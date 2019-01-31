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

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Insertable;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Feed")
public class DRFeedsServletTest extends DrServletTestBase {

    private static DRFeedsServlet drfeedsServlet;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        drfeedsServlet = new DRFeedsServlet();
        setAuthoriserToReturnRequestIsAuthorized();
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();
        setupValidAuthorisedRequest();
        setUpValidSecurityOnHttpRequest();
        setUpValidContentHeadersAndJSONOnHttpRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        drfeedsServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
            throws Exception {
        when(request.isSecure()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        drfeedsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        setBehalfHeader(null);
        drfeedsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_URL_Path_Not_Valid_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        when(request.getRequestURI()).thenReturn("/123");
        drfeedsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated()
            throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        drfeedsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Fails_With_Valid_Name_And_Version() throws Exception {
        when(request.getParameter("name")).thenReturn("stub_name");
        when(request.getParameter("version")).thenReturn("stub_version");
        drfeedsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds_With_Valid_Name_And_Version() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        when(request.getParameter("name")).thenReturn("stub_name");
        when(request.getParameter("version")).thenReturn("stub_version");
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedByNameVersion(anyString(), anyString())).thenReturn(feed);
        when(feed.asJSONObject(true)).thenReturn(mock(JSONObject.class));
        drfeedsServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds_With_Invalid_Name_And_Version() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        drfeedsServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }


    @Test
    public void Given_Request_Is_HTTP_PUT_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        drfeedsServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
            throws Exception {
        when(request.isSecure()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        setBehalfHeader(null);
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_URL_Path_Not_Valid_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        when(request.getRequestURI()).thenReturn("/123");
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated()
            throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.feed; version=1.1");
        when(request.getContentType()).thenReturn("stub_contentType");
        drfeedsServlet.doPost(request, response);
        verify(response)
                .sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated()
            throws Exception {
        setAuthoriserToReturnRequestNotAuthorized();
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Active_Feeds_Equals_Max_Feeds_Then_Bad_Request_Response_Is_Generated()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "maxFeeds", 0, true);
        DRFeedsServlet drfeedsServlet = new DRFeedsServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                return new JSONObject();
            }
        };
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_CONFLICT), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Feed_Is_Not_Valid_Object_Bad_Request_Response_Is_Generated()
            throws Exception {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn(null);
        JSONObject JSObject = buildRequestJsonObject();

        DRFeedsServlet drfeedsServlet = new DRFeedsServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                return jo;
            }
        };

        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Feed_Already_Exists_Bad_Request_Response_Is_Generated()
            throws Exception {
        setFeedToReturnInvalidFeedIdSupplied();
        JSONObject JSObject = buildRequestJsonObject();
        DRFeedsServlet drfeedsServlet = new DRFeedsServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "not_stub_name");
                jo.put("version", "1.0");
                jo.put("authorization", JSObject);
                return jo;
            }
        };
        drfeedsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_POST_Fails_Bad_Request_Response_Is_Generated() throws Exception {
        JSONObject JSObject = buildRequestJsonObject();
        DRFeedsServlet drfeedsServlet = new DRFeedsServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "2.0");
                jo.put("authorization", JSObject);
                return jo;
            }

            @Override
            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };
        drfeedsServlet.doPost(request, response);
        verify(response)
                .sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Change_On_Feeds_Succeeds_A_STATUS_OK_Response_Is_Generated()
            throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        JSONObject JSObject = buildRequestJsonObject();
        DRFeedsServlet drfeedsServlet = new DRFeedsServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject jo = new JSONObject();
                jo.put("name", "stub_name");
                jo.put("version", "1.0");
                jo.put("authorization", JSObject);
                return jo;
            }

            @Override
            protected boolean doInsert(Insertable bean) {
                return true;
            }
        };
        drfeedsServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_CREATED));
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
        Set<String> authAddressesAndNetworks = new HashSet<String>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils
                .writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks,
                        true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", false, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "maxFeeds", 100, true);
    }

    private void setBehalfHeader(String headerValue) {
        when(request.getHeader(BEHALF_HEADER)).thenReturn(headerValue);
    }

    private void setValidPathInfoInHttpHeader() {
        when(request.getPathInfo()).thenReturn("/123");
    }

    private void setFeedToReturnInvalidFeedIdSupplied() {
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
        when(Feed.getFeedByNameVersion(anyString(), anyString())).thenReturn(mock(Feed.class));
    }

    private void setFeedToReturnValidFeedForSuppliedId() {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.isDeleted()).thenReturn(false);
        when(feed.asJSONObject(true)).thenReturn(mock(JSONObject.class));
        when(feed.getPublisher()).thenReturn("Stub_Value");
        when(feed.getName()).thenReturn("stub_name");
        when(feed.getVersion()).thenReturn("1.0");
        when(feed.asLimitedJSONObject()).thenReturn(mock(JSONObject.class));
        PowerMockito.when(feed.getFeedByNameVersion(anyString(), anyString())).thenReturn(null);
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
        setValidPathInfoInHttpHeader();
        setFeedToReturnValidFeedForSuppliedId();
    }

    private void setUpValidContentHeadersAndJSONOnHttpRequest() {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.dmaap-dr.feed; version=1.0");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_subjectGroup");

    }
}