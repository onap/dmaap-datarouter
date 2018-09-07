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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.provisioning.beans.*;
import org.powermock.modules.junit4.PowerMockRunner;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class RouteServletTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private RouteServlet routeServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

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
        routeServlet = new RouteServlet();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Is_Not_Authorized_Then_Forbidden_Response_Is_Generated()
            throws Exception {
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Route_Does_Not_Exist_In_Path_Then_Route_Does_Not_Exist_Is_Returned()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/3/internal/route/");
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_FeedID_Then_Feed_Not_Found_Is_Returned()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/feedID/internal/route/");
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_Sequence_Number_Then_Invalid_Sequence_Is_Returned()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/feedID/");
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Route_Does_Not_Exist_In_Path() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/3");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Path_Contains_Invalid_SubID() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/subID");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Network_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Deletable_Is_Null_Then_Bad_Url_Is_Returned() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Fails() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/node01/node02");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }

            @Override
            protected boolean doDelete(Deleteable bean) {
                return false;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response)
                .sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Authorized() throws Exception {
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        routeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Does_Not_Start_With_Valid_Route() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Equals_Ingress_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Equals_Egress_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Ingress_Path_Equals_Network_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Authorized() throws Exception {
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        routeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Contains_Bad_URL() throws Exception {
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Authorized() throws Exception {
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Ingress_Path_Starts_With_Ingress_And_Contains_Invalid_Arguments()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        when(request.getParameter("feed")).thenReturn("3");
        when(request.getParameter("user")).thenReturn(null);
        when(request.getParameter("subnet")).thenReturn(null);
        when(request.getParameter("nodepatt")).thenReturn(null);
        when(request.getParameter("seq")).thenReturn(null);
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Egress_And_EgressRoute_Already_Exists()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        when(request.getParameter("sub")).thenReturn("1");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Egress_And_Contains_Invalid_Arguments()
            throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        when(request.getParameter("sub")).thenReturn("3");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Network_And_Is_Missing_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Network_And_Route_Already_Exists() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getParameter("from")).thenReturn("stub_from");
        when(request.getParameter("to")).thenReturn("stub_to");
        when(request.getParameter("via")).thenReturn("stub_via");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_URL_Is_Null() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        when(request.getParameter("from")).thenReturn("stub_from");
        when(request.getParameter("to")).thenReturn("stub_to");
        when(request.getParameter("via")).thenReturn("stub_via");
        RouteServlet routeServlet = new RouteServlet();
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Fails() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getParameter("from")).thenReturn("node01");
        when(request.getParameter("to")).thenReturn("node02");
        when(request.getParameter("via")).thenReturn("node03");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }

            @Override
            protected boolean doInsert(Insertable bean) {
                return false;
            }
        };

        routeServlet.doPost(request, response);
        verify(response)
                .sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }
}
