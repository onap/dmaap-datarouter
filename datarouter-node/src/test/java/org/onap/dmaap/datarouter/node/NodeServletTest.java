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
package org.onap.dmaap.datarouter.node;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
public class NodeServletTest {

    private NodeServlet nodeServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception{
        nodeServlet = new NodeServlet();
        setBehalfHeader("Stub_Value");
        when(request.getPathInfo()).thenReturn("2");
        when(request.isSecure()).thenReturn(true);
        setUpConfig();
        setUpNodeMainDelivery();
        when(request.getHeader("Authorization")).thenReturn("User1");
        when(request.getHeader("X-DR-PUBLISH-ID")).thenReturn("User1");
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Config_Is_Down_Then_Service_Unavailable_Response_Is_Generated() throws Exception {
        setNodeConfigManagerIsConfiguredToReturnFalse();
        nodeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Endpoint_Is_Internal_FetchProv_Then_No_Content_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/fetchProv");
        nodeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Endpoint_Is_ResetSubscription_Then_No_Content_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/resetSubscription/1");
        nodeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_To_Invalid_Endpoint_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/incorrect");
        nodeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Config_Is_Down_Then_Service_Unavailable_Response_Is_Generated() throws Exception {
        setNodeConfigManagerIsConfiguredToReturnFalse();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Endpoint_Is_Incorrect_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/incorrect/");
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Is_Not_Secure_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_File_Id_Is_Null_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Authorization_Is_Null_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Publish_Does_Not_Include_File_Id_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/");
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Publish_Not_Permitted_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setNodeConfigManagerIsPublishPermittedToReturnAReason();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Internal_Publish_On_Same_Node_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/publish/1/fileName");
        setNodeConfigManagerIsPublishPermittedToReturnAReason();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Internal_Publish_But_Invalid_File_Id_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/publish/1/");
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_And_Ingress_Node_Is_Provided_Then_Request_Is_Redirected() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setNodeConfigManagerToAllowRedirectOnIngressNode();
        nodeServlet.doPut(request, response);
        verify(response).sendRedirect(anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_With_Meta_Data_Too_Long_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(true);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_With_Meta_Data_Malformed_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(false);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_On_Publish_With_Meta_Data_Malformed_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(false);
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }


    private void setBehalfHeader(String headerValue) {
        when(request.getHeader("X-DR-ON-BEHALF-OF")).thenReturn(headerValue);
    }

    private void setUpConfig() throws IllegalAccessException{
        NodeConfigManager config = mock(NodeConfigManager.class);
        PowerMockito.mockStatic(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/dir");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.getPublishId()).thenReturn("User1");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(true);
        when(config.getEventLogInterval()).thenReturn("40");
        FieldUtils.writeDeclaredStaticField(NodeServlet.class, "config", config, true);
        FieldUtils.writeDeclaredStaticField(NodeMain.class, "nodeConfigManager", config, true);
        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
    }


    private void setUpNodeMainDelivery() throws IllegalAccessException{
        Delivery delivery = mock(Delivery.class);
        doNothing().when(delivery).resetQueue(anyObject());
        FieldUtils.writeDeclaredStaticField(NodeMain.class, "delivery", delivery, true);
    }

    private void setNodeConfigManagerIsConfiguredToReturnFalse() throws IllegalAccessException{
        NodeConfigManager config = mock(NodeConfigManager.class);
        when(config.isConfigured()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(NodeServlet.class, "config", config, true);
    }

    private void setNodeConfigManagerIsPublishPermittedToReturnAReason() throws IllegalAccessException{
        NodeConfigManager config = mock(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/dir");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.isPublishPermitted(anyString(), anyString(), anyString())).thenReturn("Not Permitted");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(NodeServlet.class, "config", config, true);
    }

    private void setNodeConfigManagerToAllowRedirectOnIngressNode() throws IllegalAccessException{
        NodeConfigManager config = mock(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/dir");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.getPublishId()).thenReturn("User1");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(true);
        when(config.getAuthUser(anyString(), anyString())).thenReturn("User1");
        when(config.getIngressNode(anyString(), anyString(), anyString())).thenReturn("NewNode");
        when(config.getExtHttpsPort()).thenReturn(8080);
        FieldUtils.writeDeclaredStaticField(NodeServlet.class, "config", config, true);
    }

    private String createLargeMetaDataString() {
        StringBuilder myString = new StringBuilder("meta");
        for (int i = 0; i <= 4098; ++i) {
            myString.append('x');
        }
        return myString.toString();
    }

    private void setHeadersForValidRequest(boolean isMetaTooLong) {
        String metaDataString;
        if (isMetaTooLong) {
            metaDataString = createLargeMetaDataString();
        } else {
            metaDataString = "?#@><";
        }
        List<String> headers = new ArrayList<>();
        headers.add("Content-Type");
        headers.add("X-DR-ON-BEHALF-OF");
        headers.add("X-DR-META");
        Enumeration<String> headerNames = Collections.enumeration(headers);
        when(request.getHeaderNames()).thenReturn(headerNames);
        Enumeration<String> contentTypeHeader = Collections.enumeration(Arrays.asList("text/plain"));
        Enumeration<String> behalfHeader = Collections.enumeration(Arrays.asList("User1"));
        Enumeration<String> metaDataHeader = Collections.enumeration(Arrays.asList(metaDataString));
        when(request.getHeaders("Content-Type")).thenReturn(contentTypeHeader);
        when(request.getHeaders("X-DR-ON-BEHALF-OF")).thenReturn(behalfHeader);
        when(request.getHeaders("X-DR-META")).thenReturn(metaDataHeader);
    }
}