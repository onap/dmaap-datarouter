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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
public class StatisticsServletTest extends DrServletTestBase{
    private StatisticsServlet statisticsServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception{
        super.setUp();
        statisticsServlet = new StatisticsServlet();
        buildRequestParameters();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        statisticsServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        statisticsServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_SC_METHOD_NOT_ALLOWED_Response_Is_Generated() throws Exception {
        statisticsServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_With_Incorrect_Parameters_Then_Bad_Request_Response_Is_Generated() throws Exception {
        when(request.getParameter("type")).thenReturn("get");
        statisticsServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_With_GroupId_But_No_FeedId_Parameters_Then_Request_Succeeds() throws Exception {
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        statisticsServlet = mock(StatisticsServlet.class);
        doCallRealMethod().when(statisticsServlet).doGet(request, response);
        doNothing().when(statisticsServlet).rsToCSV(anyObject(), anyObject());
        when(statisticsServlet.getFeedIdsByGroupId(anyInt())).thenReturn(new StringBuffer("1"));
        statisticsServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_With_GroupId_And_FeedId_Parameters_Then_Request_Succeeds() throws Exception {
        when(request.getParameter("feedid")).thenReturn("1");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        statisticsServlet = mock(StatisticsServlet.class);
        doCallRealMethod().when(statisticsServlet).doGet(request, response);
        doCallRealMethod().when(statisticsServlet).queryGeneretor(anyObject());
        doNothing().when(statisticsServlet).rsToCSV(anyObject(), anyObject());
        when(statisticsServlet.getFeedIdsByGroupId(anyInt())).thenReturn(new StringBuffer("1"));
        statisticsServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    private void buildRequestParameters() {
        when(request.getParameter("type")).thenReturn("exp");
        when(request.getParameter("publishId")).thenReturn("ID");
        when(request.getParameter("statusCode")).thenReturn("success");
        when(request.getParameter("expiryReason")).thenReturn("other");
        when(request.getParameter("start")).thenReturn("0");
        when(request.getParameter("end")).thenReturn("0");
        when(request.getParameter("output_type")).thenReturn("csv");
        when(request.getParameter("start_time")).thenReturn("13");
        when(request.getParameter("end_time")).thenReturn("15");
        when(request.getParameter("time")).thenReturn("10");
        when(request.getParameter("groupid")).thenReturn("1");
        when(request.getParameter("subid")).thenReturn("1");
    }
}
