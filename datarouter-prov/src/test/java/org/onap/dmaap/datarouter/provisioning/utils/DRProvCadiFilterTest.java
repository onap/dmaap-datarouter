/**-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.provisioning.utils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.filter.CadiFilter;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.onap.dmaap.datarouter.provisioning.BaseServlet.BEHALF_HEADER;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CadiFilter.class})
public class DRProvCadiFilterTest {

    @Mock
    private PropAccess access;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private DRProvCadiFilter cadiFilter;


    private static EntityManagerFactory emf;
    private static EntityManager em;


    @BeforeClass
    public static void init() {
        emf = Persistence.createEntityManagerFactory("dr-unit-tests");
        em = emf.createEntityManager();
        System.setProperty(
                "org.onap.dmaap.datarouter.provserver.properties",
                "src/test/resources/h2Database.properties");
    }

    @Before
    public void setUp() throws Exception {
        cadiFilter = new DRProvCadiFilter(false, access);
    }

    @Test
    public void Given_doFilter_Called_And_Path_Contains_subs_And_SubId_Is_Incorrectly_Set_Then_Not_Found_Response_Returned() throws Exception{
        setRequestMocking("PUT", "subs");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_doFilter_called_And_Path_Contains_subs_And_Is_AAF_Subscriber_then_call_Super_doFilter() throws Exception{
        setRequestMocking("PUT", "subs");
        when(request.getPathInfo()).thenReturn("/2");
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(CadiFilter.class));
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(0)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_called_And_Path_Contains_subs_And_Is_Not_AAF_Subscriber_then_call_chain_doFilter() throws Exception{
        setRequestMocking("PUT", "subs");
        when(request.getPathInfo()).thenReturn("/5");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_called_And_FeedId_Is_Incorrectly_Set_Then_Not_Found_Response_Returned () throws Exception{
        setRequestMocking("PUT", "feeds");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_doFilter_called_And_FeedId_Is_Correctly_Set_And_Is_AAF_Feed_Then_Call_Super_doFilter() throws Exception{
        setRequestMocking("PUT", "feeds");
        when(request.getPathInfo()).thenReturn("/2");
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(CadiFilter.class));
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(0)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_called_And_FeedId_Is_Correctly_Set_And_Is_Not_AAF_Feed_then_call_chain_doFilter() throws Exception{
        setRequestMocking("PUT", "feeds");
        when(request.getPathInfo()).thenReturn("/1");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_called_With_Get_Then_call_chain_doFilter() throws Exception{
        setRequestMocking("GET", "feeds");
        when(request.getPathInfo()).thenReturn("/5");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }


    @Test
    public void Given_doFilter_called_With_POST_Then_call_chain_doFilter() throws Exception{
        setRequestMocking("POST", "subscribe");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));

    }

    @Test
    public void Given_doFilter_called_With_POST_And_FeedId_Is_Incorrectly_Set_Then_Not_Found_Response_Returned() throws Exception{
        setRequestMocking("POST", "subscribe");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Exclude_AAF_Is_NULL_Then_Bad_Request_Response_Returned() throws Exception{
        setRequestMocking("POST", "subscribe");
        when(request.getPathInfo()).thenReturn("/2");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Exclude_AAF_Equals_True_Then_Call_Chain_doFilter() throws Exception{
        setRequestMocking("POST", "subscribe");
        when(request.getPathInfo()).thenReturn("/2");
        when(request.getHeader("X-EXCLUDE-AAF")).thenReturn("true");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Exclude_AAF_Equals_False_Then_Call_Super_doFilter() throws Exception{
        setRequestMocking("POST", "subscribe");
        when(request.getPathInfo()).thenReturn("/2");
        when(request.getHeader("X-EXCLUDE-AAF")).thenReturn("false");
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(CadiFilter.class));

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(0)).doFilter(request, response);

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Is_Not_AAF_Exclude_AAF_Equals_Then_Call_Chain_doFilter() throws Exception{
        setRequestMocking("POST", "subscribe");
        when(request.getPathInfo()).thenReturn("/5");
        when(request.getHeader("X-EXCLUDE-AAF")).thenReturn("false");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Path_Not_Includes_subscribe_And_Exclude_AAF_Is_NULL_Then_Bad_Request_Response_Returned() throws Exception{
        setRequestMocking("POST", "other");
        when(request.getPathInfo()).thenReturn("/5");

        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), argThat(notNullValue(String.class)));

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Path_Not_Includes_subscribe_And_Exclude_AAF_Equals_True_Then_Call_Chain_doFilter() throws Exception{
        setRequestMocking("POST", "other");
        when(request.getPathInfo()).thenReturn("/5");
        when(request.getHeader("X-EXCLUDE-AAF")).thenReturn("true");

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);

    }

    @Test
    public void Given_doFilter_called_With_POST_And_Path_Not_Includes_subscribe_And_Exclude_AAF_Equals_False_Then_Call_Super_doFilter() throws Exception{
        setRequestMocking("POST", "other");
        when(request.getPathInfo()).thenReturn("/5");
        when(request.getHeader("X-EXCLUDE-AAF")).thenReturn("false");
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(CadiFilter.class));

        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(0)).doFilter(request, response);

    }

    @Test
    public void Given_doFilter_Called_And_Path_Contains_subs_And_getSubId_Throws_NumberFormatException_then_Not_Found_response_returned() throws Exception{
            setRequestMocking("PUT", "subs");
            when(request.getPathInfo()).thenReturn("5/");
            cadiFilter.doFilter(request, response, chain);
            verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));

    }

    @Test
    public void Given_doFilter_called_And_FeedId_Throws_Set_Then_Not_Found_Response_Returned () throws Exception{
        setRequestMocking("PUT", "feeds");
        when(request.getPathInfo()).thenReturn("//5");
        cadiFilter.doFilter(request, response, chain);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    private void setRequestMocking(String method, String servletPath)
    {
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getHeader(BEHALF_HEADER)).thenReturn(null);
        when(request.getAttribute(BaseServlet.CERT_ATTRIBUTE)).thenReturn(null);
        when(request.getMethod()).thenReturn(method);
        when(request.getServletPath()).thenReturn(servletPath);
    }

    }
