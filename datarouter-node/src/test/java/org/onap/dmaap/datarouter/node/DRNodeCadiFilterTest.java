/**
 * - ============LICENSE_START======================================================= Copyright (C) 2019 Nordix
 * Foundation. ================================================================================ Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.node;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.filter.CadiFilter;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
@PrepareForTest({CadiFilter.class})
@RunWith(PowerMockRunner.class)
public class DRNodeCadiFilterTest {

    @Mock
    private PropAccess access;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private DRNodeCadiFilter cadiFilter;


    @Before
    public void setUp() throws ServletException {
        cadiFilter = new DRNodeCadiFilter(false, access);
    }

    @Test
    public void Given_doFilter_Called_And_Method_Is_GET_And_AAF_DB_Instance_Is_NULL_Then_Chain_doFilter_Called()
            throws Exception {
        PowerMockito.mockStatic(NodeConfigManager.class);
        NodeConfigManager config = mock(NodeConfigManager.class);

        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
        PowerMockito.when(config.getAafInstance("/other/5")).thenReturn("legacy");
        when(request.getPathInfo()).thenReturn("/publish/5");
        when(request.getMethod()).thenReturn("GET");
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_Called_And_Method_Is_GET_And_Path_Includes_Internal_Then_Chain_doFilter_Called()
            throws Exception {
        PowerMockito.mockStatic(NodeConfigManager.class);
        NodeConfigManager config = mock(NodeConfigManager.class);

        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
        PowerMockito.when(config.getAafInstance("/other/5")).thenReturn("legacy");
        when(request.getPathInfo()).thenReturn("/internal/5");
        when(request.getMethod()).thenReturn("GET");
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    public void Given_doFilter_Called_And_Method_Is_GET_And_AAF_DB_Is_Not_Null_Then_Super_doFilter_Called()
            throws Exception {
        PowerMockito.mockStatic(NodeConfigManager.class);
        NodeConfigManager config = mock(NodeConfigManager.class);

        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
        PowerMockito.when(config.getAafInstance("5")).thenReturn("EXISTS");
        when(request.getPathInfo()).thenReturn("/publish/5/fileId");
        when(request.getMethod()).thenReturn("GET");
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(CadiFilter.class));
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(0)).doFilter(request, response);
    }

    @Test
    public void Given_getFileid_Called_And_SendError_Fails_Then_Throw_IOException_And_Call_chain_doFilter()
            throws Exception {
        PowerMockito.mockStatic(NodeConfigManager.class);
        NodeConfigManager config = mock(NodeConfigManager.class);

        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
        when(request.getPathInfo()).thenReturn("/publish/5");
        when(request.getMethod()).thenReturn("DELETE");
        doThrow(new IOException()).when(response).sendError(HttpServletResponse.SC_NOT_FOUND,
                "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.  Possible missing fileid.");
        cadiFilter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }
}
