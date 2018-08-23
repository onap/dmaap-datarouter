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
import org.mockito.Mock;
import org.onap.dmaap.datarouter.provisioning.beans.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.SortedSet;
import java.util.TreeSet;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.IngressRoute",
        "org.onap.dmaap.datarouter.provisioning.beans.EgressRoute",
        "org.onap.dmaap.datarouter.provisioning.beans.NodeClass",
        "org.onap.dmaap.datarouter.provisioning.beans.NetworkRoute"})
public class RouteServletTest extends DrServletTestBase
{
    private RouteServlet routeServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();
        setRouteToReturnValid();
        routeServlet = new RouteServlet();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Is_Not_Authorized() throws Exception {
        routeServlet.doDelete(request, response);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Route_Does_Not_Exist_In_Path() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/3/internal/route/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_FeedID() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/feedID/internal/route/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_Sequence_Number() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/feedID/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ingress_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Route_Does_Not_Exist_In_Path() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/3");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Path_Contains_Invalid_SubID() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/subID");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Egress_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Network_Path_Contains_Invalid_Number_Of_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Deletable_Is_Null() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }

            @Override
            protected boolean doDelete(Deleteable bean) {
                return true;
            }
        };
        routeServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Fails() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/subID/route");
        PowerMockito.mockStatic(NodeClass.class);
        PowerMockito.when(NodeClass.normalizeNodename(anyString())).thenReturn("stub_val");
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
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Is_Not_Authorized() throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        routeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Does_Not_Start_With_Valid_Route() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doGet(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Equals_Ingress_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Path_Equals_Egress_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Ingress_Path_Equals_Network_And_Get_Succeeds() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        ServletOutputStream outStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outStream);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_OK));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Is_Not_Authorized() throws Exception {
        routeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Contains_Bad_URL() throws Exception {
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPut(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_POST_And_Is_Not_Authorized() throws Exception {
        routeServlet.doPost(request, response);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "isAddressAuthEnabled", "true", true);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Ingress_Path_Starts_With_Ingress_And_Contains_Invalid_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/ingress/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("feed")).thenReturn("3");
        when(request.getParameter("user")).thenReturn(null);
        when(request.getParameter("subnet")).thenReturn(null);
        when(request.getParameter("nodepatt")).thenReturn(null);
        when(request.getParameter("seq")).thenReturn(null);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Egress_And_EgressRoute_Already_Exists() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("sub")).thenReturn("3");
        EgressRoute e = mock(EgressRoute.class);
        PowerMockito.when(EgressRoute.getEgressRoute(anyInt())).thenReturn(e);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Egress_And_Contains_Invalid_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/egress/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("sub")).thenReturn("3");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Network_And_Is_Missing_Arguments() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_Starts_With_Network_And_Route_Already_Exists() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("from")).thenReturn("stub_from");
        when(request.getParameter("to")).thenReturn("stub_to");
        when(request.getParameter("via")).thenReturn("stub_via");
        PowerMockito.mockStatic(NodeClass.class);
        PowerMockito.when(NodeClass.normalizeNodename(anyString())).thenReturn("stub_val");
        SortedSet<NetworkRoute> networkSet = new TreeSet();
        networkSet.add(mock(NetworkRoute.class));
        PowerMockito.when(NetworkRoute.getAllNetworkRoutes()).thenReturn(networkSet);
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Path_URL_Is_Null() throws Exception {
        when(request.getPathInfo()).thenReturn("/route/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("from")).thenReturn("stub_from");
        when(request.getParameter("to")).thenReturn("stub_to");
        when(request.getParameter("via")).thenReturn("stub_via");
        PowerMockito.mockStatic(NodeClass.class);
        PowerMockito.when(NodeClass.normalizeNodename(anyString())).thenReturn("stub_val");
        RouteServlet routeServlet = new RouteServlet() {
            protected boolean isAuthorizedForInternal(HttpServletRequest req) {
                return true;
            }
        };
        routeServlet.doPost(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Fails() throws Exception {
        when(request.getPathInfo()).thenReturn("/network/");
        when(request.getRemoteAddr()).thenReturn("stub_addr");
        when(request.getParameter("from")).thenReturn("stub_from");
        when(request.getParameter("to")).thenReturn("stub_to");
        when(request.getParameter("via")).thenReturn("stub_via");
        PowerMockito.mockStatic(NodeClass.class);
        PowerMockito.when(NodeClass.normalizeNodename(anyString())).thenReturn("stub_val");
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
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), argThat(notNullValue(String.class)));
    }

    private void setRouteToReturnValid() throws IllegalAccessException {
        PowerMockito.mockStatic(IngressRoute.class);
        PowerMockito.when(IngressRoute.getIngressRoute(anyInt(), anyString(), anyString())).thenReturn(null);
        SortedSet<IngressRoute> ingressSet = new TreeSet();
        IngressRoute ingressRoute = mock(IngressRoute.class);
        JSONObject joIngress = mock(JSONObject.class);
        when(joIngress.toString()).thenReturn("{}");
        when(ingressRoute.asJSONObject()).thenReturn(joIngress);
        ingressSet.add(ingressRoute);
        PowerMockito.when(IngressRoute.getAllIngressRoutes()).thenReturn(ingressSet);

        PowerMockito.mockStatic(EgressRoute.class);
        PowerMockito.when(EgressRoute.getEgressRoute(anyInt())).thenReturn(null);
        SortedSet<EgressRoute> egressSet = new TreeSet();
        EgressRoute egressRoute = mock(EgressRoute.class);
        JSONObject joEgress = mock(JSONObject.class);
        when(joEgress.toString()).thenReturn("{}");
        when(egressRoute.asJSONObject()).thenReturn(joEgress);
        egressSet.add(egressRoute);
        PowerMockito.when(EgressRoute.getAllEgressRoutes()).thenReturn(egressSet);

        PowerMockito.mockStatic(NetworkRoute.class);
        SortedSet<NetworkRoute> networkSet = new TreeSet();
        PowerMockito.when(NetworkRoute.getAllNetworkRoutes()).thenReturn(networkSet);

    }

    private void setPokerToNotCreateTimersWhenDeleteFeedIsCalled() throws Exception {
        Poker poker = mock(Poker.class);
        FieldUtils.writeDeclaredStaticField(Poker.class, "poker", poker, true);
    }
}
