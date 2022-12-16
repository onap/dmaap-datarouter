/*
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
package org.onap.dmaap.datarouter.node;

import java.io.IOException;
import java.util.Properties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.node.utils.NodeTlsManager;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"java.net.ssl", "javax.security.auth.x500.X500Principal", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class NodeTlsManagerTest {

    private static NodeTlsManager nodeTlsManager;

    @BeforeClass
    public static void setUpClass() throws IOException {
        Properties nodeProps = new Properties();
        nodeProps.load(NodeTlsManagerTest.class.getClassLoader().getResourceAsStream("node_test.properties"));
        nodeTlsManager = new NodeTlsManager(nodeProps);
    }

    @Test
    public void Given_Get_CanonicalName_Called_Valid_CN_Returned_From_JKS() {
        String canonicalName = nodeTlsManager.getMyNameFromCertificate();
        Assert.assertEquals("dmaap-dr-node", canonicalName);
    }

}
