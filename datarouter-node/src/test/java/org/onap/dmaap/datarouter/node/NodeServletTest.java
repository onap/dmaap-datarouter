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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.node.delivery.Delivery;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
@PrepareForTest(NodeServer.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class NodeServletTest {

    private NodeServlet nodeServlet;
    private Delivery delivery;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ListAppender<ILoggingEvent> listAppender;

    private NodeConfigManager config = mock(NodeConfigManager.class);

    @Before
    public void setUp() throws Exception {
        listAppender = setTestLogger();
        setBehalfHeader("Stub_Value");
        when(request.getPathInfo()).thenReturn("2");
        when(request.isSecure()).thenReturn(true);
        createFilesAndDirectories();
        setUpConfig();
        setUpNodeMainDelivery();
        delivery = mock(Delivery.class);
        when(delivery.markTaskSuccess("spool/s/0/1", "dmaap-dr-node.1234567")).thenReturn(true);
        PowerMockito.mockStatic(NodeServer.class);
        nodeServlet = new NodeServlet(delivery, config);
        when(request.getHeader("Authorization")).thenReturn("User1");
        when(request.getHeader("X-DMAAP-DR-PUBLISH-ID")).thenReturn("User1");
    }

    @AfterClass
    public static void tearDown() {
        deleteCreatedDirectories();
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Config_Is_Down_Then_Service_Unavailable_Response_Is_Generated() throws Exception {
        setNodeConfigManagerIsConfiguredToReturnFalse();
        nodeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Endpoint_Is_Internal_FetchProv_Then_No_Content_Response_Is_Generated() {
        when(request.getPathInfo()).thenReturn("/internal/fetchProv");
        nodeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Endpoint_Is_ResetSubscription_Then_No_Content_Response_Is_Generated() {
        when(request.getPathInfo()).thenReturn("/internal/resetSubscription/1");
        nodeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_GET_To_Invalid_Endpoint_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/incorrect");
        nodeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Config_Is_Down_Then_Service_Unavailable_Response_Is_Generated() throws Exception {
        setNodeConfigManagerIsConfiguredToReturnFalse();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Endpoint_Is_Incorrect_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/incorrect/");
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_Is_Not_Secure_And_TLS_Enabled_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        when(config.isTlsEnabled()).thenReturn(true);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_File_Id_Is_Null_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Authorization_Is_Null_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Publish_Does_Not_Include_File_Id_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/");
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Publish_Not_Permitted_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        setNodeConfigManagerIsPublishPermittedToReturnAReason();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Internal_Publish_On_Same_Node_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/publish/1/fileName");
        setNodeConfigManagerIsPublishPermittedToReturnAReason();
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Internal_Publish_But_Invalid_File_Id_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/internal/publish/1/blah");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(true);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_And_Ingress_Node_Is_Provided_Then_Request_Is_Redirected() throws Exception {
        setNodeConfigManagerToAllowRedirectOnIngressNode();
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        nodeServlet.doPut(request, response);
        verify(response).sendRedirect(anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_With_Meta_Data_Too_Long_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(true);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_On_Publish_With_Meta_Data_Malformed_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(false);
        nodeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }


    @Test
    public void Given_Request_Is_HTTP_DELETE_On_Publish_With_Meta_Data_Malformed_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/publish/1/fileName");
        setHeadersForValidRequest(false);
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_With_Invalid_Endpoint_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/delete/1");
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_And_Is_Not_Privileged_Subscription_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/delete/1/dmaap-dr-node.1234567");
        setUpConfigToReturnUnprivilegedSubscriber();
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_And_Subscription_Does_Not_Exist_Then_Not_Found_Response_Is_Generated() throws Exception {
        when(request.getPathInfo()).thenReturn("/delete/1/dmaap-dr-node.1234567");
        setUpConfigToReturnNullOnIsDeletePermitted();
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_Then_Request_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/delete/1/dmaap-dr-node.1234567");
        createFilesAndDirectories();
        nodeServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_And_Request_Is_Not_Secure_But_TLS_Disabled_Then_Request_Succeeds() throws Exception {
        when(request.isSecure()).thenReturn(false);
        when(config.isTlsEnabled()).thenReturn(false);
        when(request.getPathInfo()).thenReturn("/delete/1/dmaap-dr-node.1234567");
        createFilesAndDirectories();
        nodeServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
        verifyEnteringExitCalled(listAppender);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_File_And_File_Does_Not_Exist_Then_Not_Found_Response_Is_Generated() throws IOException {
        when(request.getPathInfo()).thenReturn("/delete/1/nonExistingFile");
        nodeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyEnteringExitCalled(listAppender);
    }

    private void setBehalfHeader(String headerValue) {
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn(headerValue);
    }

    private ListAppender<ILoggingEvent> setTestLogger() {
        Logger Logger = (Logger) LoggerFactory.getLogger(NodeServlet.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        Logger.addAppender(listAppender);
        return listAppender;
    }

    private void verifyEnteringExitCalled(ListAppender<ILoggingEvent> listAppender) {
        assertEquals("EELF0004I  Entering data router node component with RequestId and InvocationId", listAppender.list.get(0).getMessage());
        assertEquals("EELF0005I  Exiting data router node component with RequestId and InvocationId", listAppender.list.get(listAppender.list.size() -1).getMessage());
    }

    private void setUpConfig() throws IllegalAccessException {
        PowerMockito.mockStatic(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/f");
        when(config.getSpoolBase()).thenReturn("spool");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.getPublishId()).thenReturn("User1");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(false);
        when(config.getEventLogInterval()).thenReturn("40");
        when(config.isDeletePermitted("1")).thenReturn(true);
        when(config.getAllDests()).thenReturn(new DestInfo[0]);
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

    private void setUpConfigToReturnUnprivilegedSubscriber() throws IllegalAccessException {
        PowerMockito.mockStatic(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.isDeletePermitted("1")).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

    private void setUpConfigToReturnNullOnIsDeletePermitted() throws IllegalAccessException {
        PowerMockito.mockStatic(NodeConfigManager.class);
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.isDeletePermitted("1")).thenThrow(new NullPointerException());
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

    private void setUpNodeMainDelivery() throws IllegalAccessException{
        Delivery delivery = mock(Delivery.class);
        doNothing().when(delivery).resetQueue(anyObject());
        FieldUtils.writeDeclaredStaticField(NodeServer.class, "delivery", delivery, true);
    }

    private void setNodeConfigManagerIsConfiguredToReturnFalse() throws IllegalAccessException {
        when(config.isConfigured()).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

    private void setNodeConfigManagerIsPublishPermittedToReturnAReason() throws IllegalAccessException{
        when(config.isShutdown()).thenReturn(false);
        when(config.getMyName()).thenReturn("dmaap-dr-node");
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/dir");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.isPublishPermitted(anyString(), anyString(), anyString())).thenReturn("Publisher not permitted for this feed");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(false);
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

    private void setNodeConfigManagerToAllowRedirectOnIngressNode() {
        when(config.isShutdown()).thenReturn(false);
        when(config.isConfigured()).thenReturn(true);
        when(config.getSpoolDir()).thenReturn("spool/dir");
        when(config.getLogDir()).thenReturn("log/dir");
        when(config.getPublishId()).thenReturn("User1");
        when(config.isAnotherNode(anyString(), anyString())).thenReturn(true);
        when(config.getAuthUser(anyString(), anyString())).thenReturn("User1");
        when(config.getIngressNode(anyString(), anyString(), anyString())).thenReturn("NewNode");
        when(config.getExtHttpsPort()).thenReturn(8080);
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
        headers.add("X-DMAAP-DR-ON-BEHALF-OF");
        headers.add("X-DMAAP-DR-META");
        Enumeration<String> headerNames = Collections.enumeration(headers);
        when(request.getHeaderNames()).thenReturn(headerNames);
        Enumeration<String> contentTypeHeader = Collections.enumeration(Arrays.asList("text/plain"));
        Enumeration<String> behalfHeader = Collections.enumeration(Arrays.asList("User1"));
        Enumeration<String> metaDataHeader = Collections.enumeration(Arrays.asList(metaDataString));
        when(request.getHeaders("Content-Type")).thenReturn(contentTypeHeader);
        when(request.getHeaders("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn(behalfHeader);
        when(request.getHeaders("X-DMAAP-DR-META")).thenReturn(metaDataHeader);
    }

    private void createFilesAndDirectories() throws IOException {
        File nodeDir = new File("spool/n/172.0.0.1");
        File spoolDir = new File("spool/s/0/1");
        File dataFile = new File("spool/s/0/1/dmaap-dr-node.1234567");
        File metaDataFile = new File("spool/s/0/1/dmaap-dr-node.1234567.M");
        nodeDir.mkdirs();
        spoolDir.mkdirs();
        dataFile.createNewFile();
        metaDataFile.createNewFile();
    }

    private static void deleteCreatedDirectories() {
        File spoolDir = new File("spool");
        delete(spoolDir);
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File f: file.listFiles()) {
                delete(f);
            }
        }
        if (!file.delete()) {
            System.out.println("Failed to delete: " + file);
        }
    }
}
