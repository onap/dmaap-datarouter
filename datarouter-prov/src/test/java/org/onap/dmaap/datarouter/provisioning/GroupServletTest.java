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
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Group;
import org.onap.dmaap.datarouter.provisioning.beans.Insertable;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Group")
public class GroupServletTest extends DrServletTestBase {
    private GroupServlet groupServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        groupServlet = new GroupServlet();
        setAuthoriserToReturnRequestIsAuthorized();
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();
        setUpValidAuthorisedRequest();
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        groupServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        groupServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        groupServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        groupServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Group_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setGroupToReturnInvalidGroupIdSupplied();
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_contentType");
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Group_Name_Is_Too_Long_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAnInvalidGroup(true);
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Group_Name_Matches_Group_In_Db_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAValidGroup();
        setGroupToReturnNonNullValueForGetGroupMatching();
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_PUT_Fails_Then_Internal_Server_Error_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAValidGroupWithFail();
        groupServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Succeeds() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAValidGroup();
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        groupServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setBehalfHeader(null);
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated() throws Exception {
        when(request.getContentType()).thenReturn("stub_contentType");
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Contains_Badly_Formed_JSON_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        ServletInputStream inStream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(inStream);
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Group_Description_Is_Too_Long_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAnInvalidGroup(false);
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_POST_Fails_Then_Internal_Server_Error_Response_Is_Generated() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAValidGroupWithFail();
        groupServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_Succeeds() throws Exception {
        when(request.getHeader("Content-Type")).thenReturn("application/vnd.att-dr.group; version=1.0");
        GroupServlet groupServlet = overideGetJSONFromInputToReturnAValidGroup();
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        groupServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_CREATED));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        groupServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
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

    private void setUpValidAuthorisedRequest() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
        setGroupToReturnValidGroupIdSupplied();
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

    private void setGroupToReturnValidGroupIdSupplied() {
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        when(group.asJSONObject()).thenReturn(mock(JSONObject.class));
    }

    private void setGroupToReturnInvalidGroupIdSupplied() {
        PowerMockito.mockStatic(Group.class);
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(null);
    }

    private GroupServlet overideGetJSONFromInputToReturnAnInvalidGroup(Boolean invalidName) {
        GroupServlet groupServlet = new GroupServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject invalidGroup = new JSONObject();
                String invalidEntry = "groupNameThatIsTooLongTooBeValidgroupNameThatIsTooLongTooBeValid";
                invalidEntry = invalidEntry + invalidEntry + invalidEntry + invalidEntry + invalidEntry;
                if (invalidName) {
                    invalidGroup.put("name", invalidEntry);
                    invalidGroup.put("description", "description");
                } else {
                    invalidGroup.put("name", "groupName");
                    invalidGroup.put("description", invalidEntry);
                }
                invalidGroup.put("groupid", 2);
                invalidGroup.put("authid", "User1");
                invalidGroup.put("classification", "class");
                invalidGroup.put("members", "stub_members");
                return invalidGroup;
            }
        };
        return groupServlet;
    }

    private GroupServlet overideGetJSONFromInputToReturnAValidGroupWithFail() {
        GroupServlet groupServlet = new GroupServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject validGroup = new JSONObject();
                validGroup.put("name", "groupName");
                validGroup.put("groupid", 2);
                validGroup.put("description", "Group Description");
                validGroup.put("authid", "User1");
                validGroup.put("classification", "class");
                validGroup.put("members", "stub_members");
                return validGroup;
            }

            protected boolean doUpdate(Updateable bean) {
                return false;
            }

            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };
        return groupServlet;
    }

    private GroupServlet overideGetJSONFromInputToReturnAValidGroup() {
        GroupServlet groupServlet = new GroupServlet() {
            protected JSONObject getJSONfromInput(HttpServletRequest req) {
                JSONObject validGroup = new JSONObject();
                validGroup.put("name", "groupName");
                validGroup.put("groupid", 2);
                validGroup.put("description", "Group Description");
                validGroup.put("authid", "User1");
                validGroup.put("classification", "class");
                validGroup.put("members", "stub_members");
                return validGroup;
            }

            protected boolean doUpdate(Updateable bean) {
                return true;
            }

            protected boolean doInsert(Insertable bean) {
                return true;
            }
        };
        return groupServlet;
    }

    private void setGroupToReturnNonNullValueForGetGroupMatching() {
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        PowerMockito.when(Group.getGroupMatching(Matchers.any(Group.class), anyInt())).thenReturn(group);
    }
}
