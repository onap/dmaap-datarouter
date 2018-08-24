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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.provisioning.beans.Deleteable;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Insertable;
import org.onap.dmaap.datarouter.provisioning.beans.LogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.NodeClass;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.beans.Updateable;
import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LogRecord.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Feed",
    "org.onap.dmaap.datarouter.provisioning.beans.Parameters",
    "org.onap.dmaap.datarouter.provisioning.beans.NodeClass",
    "org.onap.dmaap.datarouter.provisioning.beans.Subscription",
    "org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader"})
public class InternalServletTest extends DrServletTestBase {

  private InternalServlet internalServlet;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    internalServlet = new InternalServlet();
    setAuthoriserToReturnRequestIsAuthorized();
    setUpValidAuthorisedRequest();
  }

  @Test
  public void Given_Request_Is_HTTP_GET_And_Address_Not_Authorized_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
      throws Exception {
    when(request.getRemoteAddr()).thenReturn("127.100.0.3");
    internalServlet.doGet(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Halt_In_Endpoint_But_Not_Sent_From_Localhost_Then_Forbidden_Response_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/halt");
    when(request.isSecure()).thenReturn(false);
    when(request.getRemoteAddr()).thenReturn("127.100.0.3");
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_FORBIDDEN));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Halt_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/halt");
    when(request.isSecure()).thenReturn(false);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_FetchProv_In_Endpoint_Request_Succeeds()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/fetchProv");
    when(request.isSecure()).thenReturn(false);
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Prov_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/prov");
    when(request.getQueryString()).thenReturn(null);
    setPokerToNotCreateTimers();
    ServletOutputStream outStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outStream);
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Logs_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/logs/");
    ServletOutputStream outStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outStream);
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_Starts_With_Logs_In_Endpoint_Request_Succeeds()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/logs/TestFile");
    internalServlet.doGet(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_NO_CONTENT), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Api_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(false);
    ServletOutputStream outStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outStream);
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_With_Drlogs_In_Endpoint_Request_Succeeds()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/drlogs/");
    PowerMockito.mockStatic(LogfileLoader.class);
    LogfileLoader logfileLoader = mock(LogfileLoader.class);
    when(logfileLoader.getBitSet()).thenReturn(new RLEBitSet());
    PowerMockito.when(LogfileLoader.getLoader()).thenReturn(logfileLoader);
    ServletOutputStream outStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outStream);
    internalServlet.doGet(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_GET_Incorrect_Endpoint_Then_No_Content_Response_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/incorrect/");
    internalServlet.doGet(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_PUT_And_Address_Not_Authorized_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
      throws Exception {
    when(request.getRemoteAddr()).thenReturn("127.100.0.3");
    internalServlet.doPut(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_PUT_With_Api_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(false);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerSuccess();
    setPokerToNotCreateTimers();
    mockProvisioningParametersChanged();
    internalServlet.doPut(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_PUT_With_Api_In_Endpoint_And_Update_Fails_Then_Internal_Server_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(false);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerFailure();
    internalServlet.doPut(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
        argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_PUT_With_Incorrect_Endpoint_Then_Not_Found_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/incorrect");
    internalServlet.doPut(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_DELETE_And_Address_Not_Authorized_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
      throws Exception {
    when(request.getRemoteAddr()).thenReturn("127.100.0.3");
    internalServlet.doDelete(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_DELETE_With_Api_In_Endpoint_Request_Succeeds()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(false);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerSuccess();
    setPokerToNotCreateTimers();
    mockProvisioningParametersChanged();
    internalServlet.doDelete(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_DELETE_With_Api_In_Endpoint_And_Delete_Fails_Then_Internal_Server_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(false);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerFailure();
    internalServlet.doDelete(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
        argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_DELETE_With_Incorrect_Endpoint_Then_Not_Found_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/incorrect");
    internalServlet.doDelete(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_And_Address_Not_Authorized_When_HTTPS_Is_Required_Then_Forbidden_Response_Is_Generated()
      throws Exception {
    when(request.getRemoteAddr()).thenReturn("127.100.0.3");
    internalServlet.doPost(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_With_Api_In_Endpoint_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(true);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerSuccess();
    setPokerToNotCreateTimers();
    mockProvisioningParametersChanged();
    internalServlet.doPost(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_With_Api_In_Endpoint_And_Insert_Fails_Then_Internal_Server_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/api/Key");
    setParametersToNotContactDb(true);
    String[] values = {"V", "a", "l", "u", "e", "s"};
    when(request.getParameterValues(anyString())).thenReturn(values);
    internalServlet = internalServerFailure();
    internalServlet.doPost(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
        argThat(notNullValue(String.class)));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_To_Logs_And_Content_Header_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated()
      throws Exception {
    when(request.getHeader("Content-Type")).thenReturn("stub_contentType");
    when(request.getPathInfo()).thenReturn("/logs/");
    internalServlet.doPost(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_To_Logs_And_Content_Encoding_Is_Not_Supported_Type_Then_Unsupported_Media_Type_Response_Is_Generated()
      throws Exception {
    when(request.getHeader("Content-Encoding")).thenReturn("not-supported");
    when(request.getPathInfo()).thenReturn("/logs/");
    internalServlet.doPost(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_To_Drlogs_And_Then_Unsupported_Media_Type_Response_Is_Generated()
      throws Exception {
    when(request.getHeader("Content-Type")).thenReturn("stub_contentType");
    when(request.getPathInfo()).thenReturn("/drlogs/");
    internalServlet.doPost(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_To_Drlogs_And_Request_Succeeds() throws Exception {
    when(request.getPathInfo()).thenReturn("/drlogs/");
    ServletInputStream inStream = mock(ServletInputStream.class);
    when(inStream.read()).thenReturn(1, -1);
    when(request.getInputStream()).thenReturn(inStream);
    PowerMockito.mockStatic(LogRecord.class);
    internalServlet.doPost(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_OK));
  }

  @Test
  public void Given_Request_Is_HTTP_POST_With_Incorrect_Endpoint_Then_Not_Found_Error_Is_Generated()
      throws Exception {
    when(request.getPathInfo()).thenReturn("/incorrect/");
    internalServlet.doPost(request, response);
    verify(response)
        .sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
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
    when(request.getHeader("Content-Type")).thenReturn("text/plain");
    when(request.getHeader("Content-Encoding")).thenReturn("gzip");
  }

  private void setUpValidSecurityOnHttpRequest() throws Exception {
    when(request.isSecure()).thenReturn(true);
    when(request.getRemoteAddr()).thenReturn(InetAddress.getLocalHost().getHostAddress());
    InetAddress[] nodeAddresses = new InetAddress[1];
    nodeAddresses[0] = InetAddress.getLocalHost();
    FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodeAddresses", nodeAddresses, true);
    FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", false, true);
  }

  private void setBehalfHeader(String headerValue) {
    when(request.getHeader(BEHALF_HEADER)).thenReturn(headerValue);
  }

  private void setValidPathInfoInHttpHeader() {
    when(request.getPathInfo()).thenReturn("/123");
  }

  private void setPokerToNotCreateTimers() throws Exception {
    Poker poker = mock(Poker.class);
    FieldUtils.writeDeclaredStaticField(Poker.class, "poker", poker, true);
  }

  private void setParametersToNotContactDb(boolean isPost) {
    PowerMockito.mockStatic(Parameters.class);
    Parameters parameters = mock(Parameters.class);
    if (isPost) {
      PowerMockito.when(Parameters.getParameter(anyString())).thenReturn(null);
    } else {
      PowerMockito.when(Parameters.getParameter(anyString())).thenReturn(parameters);
    }
  }

  private InternalServlet internalServerSuccess() {
    InternalServlet internalServlet = new InternalServlet() {

      protected boolean doUpdate(Updateable bean) {
        return true;
      }

      protected boolean doDelete(Deleteable bean) {
        return true;
      }

      protected boolean doInsert(Insertable bean) {
        return true;
      }
    };
    return internalServlet;
  }

  private InternalServlet internalServerFailure() {
    InternalServlet internalServlet = new InternalServlet() {

      protected boolean doUpdate(Updateable bean) {
        return false;
      }

      protected boolean doDelete(Deleteable bean) {
        return false;
      }

      protected boolean doInsert(Insertable bean) {
        return false;
      }
    };
    return internalServlet;
  }

  private void mockProvisioningParametersChanged() throws IllegalAccessException {
    PowerMockito.mockStatic(Feed.class);
    PowerMockito.mockStatic(Subscription.class);
    PowerMockito.when(Feed.countActiveFeeds()).thenReturn(0);
    PowerMockito.when(Subscription.countActiveSubscriptions()).thenReturn(0);
    Map<String, Integer> map = new HashMap<>();
    FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", map, true);
  }
}
