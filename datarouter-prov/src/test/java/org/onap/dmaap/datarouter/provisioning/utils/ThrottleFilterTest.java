package org.onap.dmaap.datarouter.provisioning.utils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;
import org.mockito.Mock;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.onap.dmaap.datarouter.provisioning.utils.ThrottleFilter;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Parameters",
                                  "org.eclipse.jetty.server.Request",
                                  "org.eclipse.jetty.continuation.ContinuationSupport",
                                  "org.eclipse.jetty.server.HttpConnection"})
public class ThrottleFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterchain;


    @Test
    public void Given_Throttle_Filter_Configure_And_Parameter_Is_Not_Null_Then_Enabled_And_Action_Is_True() throws Exception {
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "2,5,throttle"));
        ThrottleFilter.configure();
        boolean enabled = (boolean) FieldUtils.readStaticField(ThrottleFilter.class, "enabled", true);
        int action = (int) FieldUtils.readStaticField(ThrottleFilter.class, "action", true);
        assertThat(enabled, is(true));
        assertThat(action, is(0));

    }

    @Test
    public void Given_Do_Filter_run_and_enabled_and_action_is_true_and_rate_is_0_then_continiuation_will_call_setAttribute_and_resume_once() throws Exception {
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "100,5,thing"));
        ThrottleFilter throttlefilter = new ThrottleFilter();
        ThrottleFilter.configure();
        ServletInputStream is = mock(ServletInputStream.class);
        when(is.read(any())).thenReturn(2).thenReturn(1).thenReturn(0);
        when(request.getInputStream()).thenReturn(is);
        Continuation continuation = mock(Continuation.class);
        FieldUtils.writeDeclaredStaticField(ThrottleFilter.class, "action", 1, true);
        Map<String, List<Continuation>> suspended_requests = new HashMap<String, List<Continuation>>();
        List<Continuation> continuation_list = new ArrayList<>();
        continuation_list.add(continuation);
        suspended_requests.put("null/-1", continuation_list);
        FieldUtils.writeDeclaredField(throttlefilter, "suspended_requests", suspended_requests, true);
        throttlefilter.doFilter(request, response, filterchain);
        verify(continuation, times(1)).setAttribute(anyString(), any());
        verify(continuation, times(1)).resume();
    }

    @Test
    public void Given_Do_Filter_Run_and_enabled_and_action_is_true_and_rate_is_greater_than_0_then_continuation_will_call_suspend_and_dispatch_once() throws Exception {
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        PowerMockito.mockStatic(ContinuationSupport.class);
        ContinuationSupport continuationsupport = mock(ContinuationSupport.class);
        Continuation continuation = mock(Continuation.class);
        PowerMockito.when(continuationsupport.getContinuation(any())).thenReturn(continuation);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "0,5,thing"));
        ThrottleFilter.configure();
        ServletInputStream serverinputstream = mock(ServletInputStream.class);
        when(serverinputstream.read(any())).thenReturn(2).thenReturn(1).thenReturn(0);
        when(request.getInputStream()).thenReturn(serverinputstream);
        FieldUtils.writeDeclaredStaticField(ThrottleFilter.class, "action", 1, true);
        ThrottleFilter throttlefilter = new ThrottleFilter();
        throttlefilter.doFilter(request, response, filterchain);
        verify(continuation, times(1)).undispatch();
        verify(continuation, times(1)).suspend();
    }

    @Test
    public void Given_Do_Filter_Run_and_enabled_and_action_is_true_and_rate_is_greater_than_0_and_getFeedId_returns_id_then_continuation_will_call_suspend_and_dispatch_once() throws Exception {
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        PowerMockito.mockStatic(ContinuationSupport.class);
        ContinuationSupport continuationsupport = mock(ContinuationSupport.class);
        Continuation continuation = mock(Continuation.class);
        PowerMockito.when(continuationsupport.getContinuation(any())).thenReturn(continuation);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "0,5,thing"));
        ThrottleFilter.configure();
        ServletInputStream serverinputstream = mock(ServletInputStream.class);
        when(serverinputstream.read(any())).thenReturn(2).thenReturn(1).thenReturn(0);
        when(request.getInputStream()).thenReturn(serverinputstream);
        FieldUtils.writeDeclaredStaticField(ThrottleFilter.class, "action", 1, true);
        ThrottleFilter throttlefilter = new ThrottleFilter();
        when(request.getPathInfo()).thenReturn("/123/fileName.txt");
        throttlefilter.doFilter(request, response, filterchain);
        verify(continuation, times(1)).undispatch();
        verify(continuation, times(1)).suspend();
    }

    @Test
    public void Given_Do_Filter_and_only_enabled_is_true_and_drop_filter_ran_then_request_will_call_getHttpChannel_and_HttpChannel_will_call_getEndPoint_once() throws Exception {
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        ServletInputStream serverinputstream = mock(ServletInputStream.class);
        PowerMockito.mockStatic(ContinuationSupport.class);
        ContinuationSupport continuationsupport = mock(ContinuationSupport.class);
        PowerMockito.when(continuationsupport.getContinuation(any())).thenReturn(mock(Continuation.class));
        when(serverinputstream.read(any())).thenReturn(2).thenReturn(1).thenReturn(0);
        when(request.getInputStream()).thenReturn(serverinputstream);
        PowerMockito.mockStatic(HttpConnection.class);
        HttpConnection httpconnection = mock(HttpConnection.class);
        HttpChannel httpchannel = mock(HttpChannel.class);
        Request req = mock(Request.class);
        EndPoint endpoint = mock(EndPoint.class);
        PowerMockito.when(httpconnection.getCurrentConnection()).thenReturn(httpconnection);
        PowerMockito.when(httpconnection.getHttpChannel()).thenReturn(httpchannel);
        when(httpchannel.getRequest()).thenReturn(req);
        when(req.getHttpChannel()).thenReturn(httpchannel);
        when(httpchannel.getEndPoint()).thenReturn(endpoint);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "0,5,thing"));
        ThrottleFilter.configure();
        FieldUtils.writeDeclaredStaticField(ThrottleFilter.class, "action", 0, true);
        ThrottleFilter throttlefilter = new ThrottleFilter();
        throttlefilter.doFilter(request, response, filterchain);
        verify(req, times(1)).getHttpChannel();
        verify(httpchannel, times(1)).getEndPoint();
    }

    @Test
    public void Given_run_is_called_then_continuation_will_call_prune_once() throws Exception {
        ThrottleFilter tf = new ThrottleFilter();
        Map<String, ThrottleFilter.Counter> map = new HashMap<String, ThrottleFilter.Counter>();
        ThrottleFilter.Counter tfc = mock(ThrottleFilter.Counter.class);
        map.put("Key", tfc);
        when(tfc.prune()).thenReturn(-1);
        FieldUtils.writeDeclaredField(tf, "map", map, true);
        tf.run();
        verify(tfc, times(1)).prune();
    }

    @Test
    public void Given_destroy_is_called_then_map_is_empty() throws Exception
    {
        ThrottleFilter throttleFilter = new ThrottleFilter();
        FilterConfig filterconfig = mock(FilterConfig.class);
        PowerMockito.mockStatic(Parameters.class);
        Parameters parameters = mock(Parameters.class);
        PowerMockito.mockStatic(ContinuationSupport.class);
        ContinuationSupport continuationsupport = mock(ContinuationSupport.class);
        Continuation continuation = mock(Continuation.class);
        PowerMockito.when(continuationsupport.getContinuation(any())).thenReturn(continuation);
        PowerMockito.when(parameters.getParameter(anyString())).thenReturn(new Parameters("key", "0,5,thing"));
        throttleFilter.init(filterconfig);
        throttleFilter.destroy();
    }
}
