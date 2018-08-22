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
import org.onap.dmaap.datarouter.provisioning.beans.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

/**
 * Created by ezcoxem on 21/08/2018.
 */

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Feed")
public class PublishServletTest extends DrServletTestBase {
    private PublishServlet publishServlet;

    private static String START_JSON_STRING = "{";
    private static String END_JSON_STRING = "}";
    private static String START_JSON_ARRAY = "[";
    private static String END_JSON_ARRAY = "]";
    private static String COMMA = ",";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        publishServlet = new PublishServlet();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_There_Are_No_Nodes_Then_Service_Unavailable_Error_Is_Returned()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[0], true);
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_is_Null_Then_Not_Found_Error_Is_Returned()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ix_is_null_Then_Not_Found_Error_Is_Returned()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        when(request.getPathInfo()).thenReturn("/123/");
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Feed_is_not_valid_Then_FeedId_is_less_than_zero_And_Then_Not_Found_Error_Is_Returned()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Request_succeeds()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        FieldUtils.writeDeclaredField(publishServlet, "next_node", 0, true);
        FieldUtils.writeDeclaredField(publishServlet, "provstring", "", true);
        FieldUtils.writeDeclaredField(publishServlet, "irt", new ArrayList<IngressRoute>(), true);
        FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", new HashMap<String,String>(), true);
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.isFeedValid(anyInt())).thenReturn(true);
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();
        when(request.getHeader(anyString())).thenReturn("Basic dXNlcg==");
        publishServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_succeeds()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        FieldUtils.writeDeclaredField(publishServlet, "next_node", 0, true);
        FieldUtils.writeDeclaredField(publishServlet, "provstring", "", true);
        FieldUtils.writeDeclaredField(publishServlet, "irt", new ArrayList<IngressRoute>(), true);
        FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", new HashMap<String,String>(), true);
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.isFeedValid(anyInt())).thenReturn(true);
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();

        publishServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_succeeds()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        FieldUtils.writeDeclaredField(publishServlet, "next_node", 0, true);
        FieldUtils.writeDeclaredField(publishServlet, "provstring", "", true);
        FieldUtils.writeDeclaredField(publishServlet, "irt", new ArrayList<IngressRoute>(), true);
        FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", new HashMap<String,String>(), true);
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.isFeedValid(anyInt())).thenReturn(true);
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();

        publishServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_succeeds()
            throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
        FieldUtils.writeDeclaredField(publishServlet, "next_node", 0, true);
        FieldUtils.writeDeclaredField(publishServlet, "provstring", "", true);
        FieldUtils.writeDeclaredField(publishServlet, "irt", new ArrayList<IngressRoute>(), true);
        FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", new HashMap<String,String>(), true);
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.isFeedValid(anyInt())).thenReturn(true);
        setPokerToNotCreateTimersWhenDeleteFeedIsCalled();

        publishServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    private void setPokerToNotCreateTimersWhenDeleteFeedIsCalled() throws Exception {
        Poker poker = mock(Poker.class);
        FieldUtils.writeDeclaredStaticField(Poker.class, "poker", poker, true);
        when(poker.getProvisioningString()).thenReturn(buildProvisioningString());
    }

    private String buildProvisioningString(){
        StringBuffer provisionString = new StringBuffer();
        provisionString.append(START_JSON_STRING);
        provisionString.append("'ingress':");
        provisionString.append(START_JSON_ARRAY);
        provisionString.append(buildIngressRoute());
        provisionString.append(END_JSON_ARRAY);
        provisionString.append(END_JSON_STRING);
        return provisionString.toString();
    }

    private StringBuffer buildIngressRoute(){
        StringBuffer provisionString = new StringBuffer();
        provisionString.append(START_JSON_STRING);
        provisionString.append("'seq':1");
        provisionString.append(COMMA);
        provisionString.append("'feedid':123");
        provisionString.append(COMMA);
        provisionString.append("'user':'user'");
        provisionString.append(COMMA);
        provisionString.append("'subnet':'127.0.0.1'");
        provisionString.append(COMMA);
        provisionString.append("'nodelist':-1");
        provisionString.append(COMMA);
        provisionString.append("'node':['1','2']");
        provisionString.append(END_JSON_STRING);
        return provisionString;
    }

    private void setAuthoriserToReturnRequestIsAuthorized() throws IllegalAccessException {
        AuthorizationResponse authResponse = mock(AuthorizationResponse.class);
        Authorizer authorizer = mock(Authorizer.class);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authz", authorizer, true);
        when(authorizer.decide(request)).thenReturn(authResponse);
        when(authResponse.isAuthorized()).thenReturn(true);
    }
}
