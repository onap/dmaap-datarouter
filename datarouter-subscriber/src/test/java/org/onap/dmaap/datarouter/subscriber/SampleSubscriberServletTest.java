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
package org.onap.dmaap.datarouter.subscriber;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
public class SampleSubscriberServletTest {

  private SampleSubscriberServlet sampleSubServlet;
  private SubscriberProps props = SubscriberProps.getInstance();

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  @Before
  public void setUp() {
    props =
        SubscriberProps.getInstance(
            System.getProperty(
                "org.onap.dmaap.datarouter.subscriber.properties", "testsubscriber.properties"));
    sampleSubServlet = new SampleSubscriberServlet();
    sampleSubServlet.init();
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(
        new File(props.getValue("org.onap.dmaap.datarouter.subscriber.delivery.dir")));
  }

  @Test
  public void
      Given_Request_Is_HTTP_PUT_And_Request_Header_Is_Null_Then_Unathorized_Response_Is_Generated()
          throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);
    sampleSubServlet.doPut(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED));
  }

  @Test
  public void
      Given_Request_Is_HTTP_PUT_And_Request_Header_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated()
          throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Invalid Header");
    sampleSubServlet.doPut(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN));
  }

  @Test
  public void Given_Request_Is_HTTP_PUT_Then_Request_Succeeds() throws Exception {
    setUpSuccessfulFlow();
    sampleSubServlet.doPut(request, response);
    verify(response, times(2)).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
  }

  @Test
  public void Given_Request_Is_HTTP_DELETE_Then_Request_Succeeds() throws Exception {
    setUpSuccessfulFlow();
    sampleSubServlet.doDelete(request, response);
    verify(response).setStatus(eq(HttpServletResponse.SC_NO_CONTENT));
  }

  private void setUpSuccessfulFlow() throws IOException {
    when(request.getHeader("Authorization")).thenReturn("Basic TE9HSU46UEFTU1dPUkQ=");
    when(request.getPathInfo()).thenReturn("/publish/1/testfile");
    when(request.getHeader("X-DR-PUBLISH-ID")).thenReturn("1");
    when(request.getHeader("X-DR-META")).thenReturn("{\"Key\":\"Value\"}");
    when(request.getQueryString()).thenReturn(null);
    ServletInputStream inStream = mock(ServletInputStream.class);
    when(request.getInputStream()).thenReturn(inStream);
  }
}
