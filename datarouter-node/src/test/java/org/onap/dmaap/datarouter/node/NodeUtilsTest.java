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
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeUtils")
public class NodeUtilsTest {

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
}
