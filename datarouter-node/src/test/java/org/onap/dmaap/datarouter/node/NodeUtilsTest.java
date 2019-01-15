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
package org.onap.dmaap.datarouter.node;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import static com.att.eelf.configuration.Configuration.MDC_SERVER_FQDN;
import static com.att.eelf.configuration.Configuration.MDC_SERVER_IP_ADDRESS;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeUtils")
@PrepareForTest({ UUID.class, InetAddress.class })
public class NodeUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    public void Given_Uri_With_Params_Then_Get_Feed_And_File_Id_Returns_Correct_Values() {
        String uri = "prov.datarouternew.com:8443/feed/12/fileName";
        String[] uriParams = NodeUtils.getFeedAndFileID(uri);
        Assert.assertEquals("12", uriParams[0]);
        Assert.assertEquals("fileName", uriParams[1]);
    }

    @Test
    public void Given_Uri_With_Illegal_Params_Then_Get_Feed_And_File_Id_Returns_Null() {
        String uri = "prov.datarouternew.com:8443/feed";
        String[] uriParams = NodeUtils.getFeedAndFileID(uri);
        Assert.assertNull(uriParams);
    }

    @Test
    public void Given_String_With_Escape_Fields_Then_Loge_Returns_Special_Chars() {
        String s = NodeUtils.loge("\\search|pub|12\n");
        Assert.assertEquals("\\esearch\\ppub\\p12\\n", s);
    }

    @Test
    public void Given_String_With_Special_Chars_Then_Loge_Returns_String_With_Escape_Fields() {
        String s = NodeUtils.unloge("\\esearch\\ppub\\p12\\n");
        Assert.assertEquals("\\search|pub|12\n", s);
    }

    @Test
    public void Given_Request_Has_RequestId_And_InvocationId_Headers_set_MDC_Values() {
        when(request.getHeader("X-ONAP-RequestID")).thenReturn("123");
        when(request.getHeader("X-InvocationID")).thenReturn("456");
        NodeUtils.setRequestIdAndInvocationId(request);
        Assert.assertEquals("123", MDC.get("RequestId"));
        Assert.assertEquals("456", MDC.get("InvocationId"));
    }

    @Test
    public void Given_setIpAndFqdnForEelf_called_set_MDC_Values() throws IOException {
        mockStatic(InetAddress.class);
        when(InetAddress.getLocalHost().getHostName()).thenReturn("testHostName");
        when(InetAddress.getLocalHost().getHostAddress()).thenReturn("testHostAddress");
        NodeUtils.setIpAndFqdnForEelf("doGet");
        Assert.assertEquals("testHostName", MDC.get(MDC_SERVER_FQDN));
        Assert.assertEquals("testHostAddress", MDC.get(MDC_SERVER_IP_ADDRESS));
    }

    @Test
    public void Given_Request_Has_Empty_RequestId_And_InvocationId_Headers_Generate_MDC_Values() {
        when(request.getHeader("X-ONAP-RequestID")).thenReturn("");
        when(request.getHeader("X-InvocationID")).thenReturn("");
        mockStatic(UUID.class);
        when(UUID.randomUUID().toString()).thenReturn("123", "456");
        NodeUtils.setRequestIdAndInvocationId(request);
        Assert.assertEquals("123", MDC.get("RequestId"));
        Assert.assertEquals("456", MDC.get("InvocationId"));
    }
}
