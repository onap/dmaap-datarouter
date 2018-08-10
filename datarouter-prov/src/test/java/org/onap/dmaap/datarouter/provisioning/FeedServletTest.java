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
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;


//@RunWith(MockitoJUnitRunner.class)
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Feed")
public class FeedServletTest {
    private static final Logger LOGGER = Logger.getLogger(FeedServletTest.class);
    private static FeedServlet feedServlet;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;


    private void initialiseBaseServletToBypassRetreiviingInitialisationParametersFromDatabase() throws IllegalAccessException {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "startmsg_flag", false, true);
        SynchronizerTask synchronizerTask = mock(SynchronizerTask.class);
        when(synchronizerTask.getState()).thenReturn(SynchronizerTask.UNKNOWN);
        FieldUtils.writeDeclaredStaticField(SynchronizerTask.class, "synctask", synchronizerTask, true);
    }

    private void setUpValidSecurityOnHttpRequest()throws Exception{
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<String>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "require_cert", false, true);
    }

    private void setBehalfHeader(String headerValue){
        when(request.getHeader(BEHALF_HEADER)).thenReturn(headerValue);
    }

    private void setValidPathInfoInHttpHeader() {
        when(request.getPathInfo()).thenReturn("/123");
    }

    private void setFeedToReturnInvalidFeedIdSupplied() {
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
    }

    private Feed setFeedToReturnValidFeedForSuppliedId(){
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.isDeleted()).thenReturn(false);
        return feed;
    }

    private void setAuthorizerToReturnRequestNotAuthorized() throws IllegalAccessException {
        AuthorizationResponse authResponse = mock(AuthorizationResponse.class);
        Authorizer authorizer = mock(Authorizer.class);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authz", authorizer, true);
        when(authorizer.decide(request)).thenReturn(authResponse);
        when(authResponse.isAuthorized()).thenReturn(false);
    }

    private void setAuthorizerToReturnRequestIsAuthorized() throws IllegalAccessException {
        AuthorizationResponse authResponse = mock(AuthorizationResponse.class);
        Authorizer authorizer = mock(Authorizer.class);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authz", authorizer, true);
        when(authorizer.decide(request)).thenReturn(authResponse);
        when(authResponse.isAuthorized()).thenReturn(true);
    }

    @Before
    public void setUp() throws Exception {
        initialiseBaseServletToBypassRetreiviingInitialisationParametersFromDatabase();
        feedServlet = new FeedServlet();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Is_Not_Secure_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated() throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_DELETE_And_BEHALF_HEADER_Is_Not_Set_In_Request_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader(null);
        feedServlet.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_Header_Is_Not_Set_In_Request_With_Valid_Path_Then_Bad_Request_Response_Is_Generated() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        feedServlet.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Feed_Id_Is_Invalid_Then_Not_Found_Response_Is_Generated() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
        setFeedToReturnInvalidFeedIdSupplied();
        feedServlet.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }



    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Request_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated() throws Exception {
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
        setFeedToReturnValidFeedForSuppliedId();
        setAuthorizerToReturnRequestNotAuthorized();
        feedServlet.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }


 //   @Test
    public void Given_Request_Is_HTTP_DELETE_And_Delete_On_Database_Fails_An_Internal_Server_Error_Is_Reported() throws Exception {
       FeedServlet feedServletSpy = spy(feedServlet);

//        FeedServlet feedServlet = new FeedServlet(){
//            protected boolean doUpdate(Updateable bean) {
//                return false;
//            }
//        };
        setUpValidSecurityOnHttpRequest();
        setBehalfHeader("Stub_Value");
        setValidPathInfoInHttpHeader();
        Feed feed = setFeedToReturnValidFeedForSuppliedId();
        setAuthorizerToReturnRequestIsAuthorized();
        when(feedServletSpy.doUpdate(feed)).thenReturn(false);
        feedServletSpy.doDelete(request, response);
        verify(response).sendError( eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

/*
    @Test
    public void testServletFeedIDDelete() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doDelete(request, response);
    }

    @Test
    public void testServletFeedNotFoundDelete() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doDelete(request, response);
    }

//    @Test
//    public void testServletAuthorizedDelete() throws Exception {
//
//        feedServlet.doDelete(request, response);
//    }

    @Test
    public void testServletDoDelete() throws Exception {

        feedServlet.doDelete(request, response);
    }


    //  doGet  ///////////////////////////////////////////////////////////////
    @Test
    public void testServletIsAuthorisedForProvisioningGet() throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doGet(request, response);
    }

//    @Test
//    public void testServletIsProxyServerGet() throws Exception {
//
//        feedServlet.doGet(request, response);
//    }

    @Test
    public void testServletBehalfHeaderGet() throws Exception {
        when(request.getHeader(anyString())).thenReturn(null);
        feedServlet.doGet(request, response);
    }

    @Test
    public void testServletFeedIDGet() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doGet(request, response);
    }

    @Test
    public void testServletFeedNotFoundGet() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doGet(request, response);
    }

//    @Test
//    public void testServletAuthorizedGet() throws Exception {
//
//        feedServlet.doGet(request, response);
//    }

    @Test
    public void testServletDoGet() throws Exception {

        feedServlet.doGet(request, response);
    }


    //  doPut  ///////////////////////////////////////////////////////////////
    @Test
    public void testServletIsAuthorisedForProvisioningPut() throws Exception {
        when(request.isSecure()).thenReturn(false);
        feedServlet.doGet(request, response);
    }

//    @Test
//    public void testServletIsProxyServerPut() throws Exception {
//
//        feedServlet.doPut(request, response);
//    }

    @Test
    public void testServletBehalfHeaderPut() throws Exception {
        when(request.getHeader(anyString())).thenReturn(null);
        feedServlet.doPut(request, response);
    }

    @Test
    public void testServletFeedIDPut() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doPut(request, response);
    }

    @Test
    public void testServletFeedNotFoundPut() throws Exception {
        when(request.getPathInfo()).thenReturn(null);
        feedServlet.doPut(request, response);
    }

    @Test
    public void testServletContentHeaderPut() throws Exception {
        when(request.getHeader(anyString())).thenReturn(null);
        feedServlet.doPut(request, response);
    }

    @Test
    public void testServletJSONObjectPut() throws Exception {
        when(request.getInputStream()).thenReturn(null);
        feedServlet.doPut(request, response);
    }

    @Test
    public void testServletSubjectGroupPut() throws Exception {
        when(request.getHeader(anyString())).thenReturn(null);
        feedServlet.doPut(request, response);
    }

//    @Test
//    public void testServletAuthorizedPut() throws Exception {
//
//        feedServlet.doPut(request, response);
//    }

    @Test
    public void testServletDoPut() throws Exception {

        feedServlet.doPut(request, response);
    }


    //  doPost  ///////////////////////////////////////////////////////////////
    @Test
    public void testServletDoPost() throws Exception {

        feedServlet.doPost(request, response);
    }
    */
}