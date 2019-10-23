/*-
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.auth.x500.X500Principal", "javax.crypto.*"})
@PrepareForTest({InetAddress.class, URL.class})
public class NodeConfigManagerTest {

    @BeforeClass
    public static void init() {
        System.setProperty("org.onap.dmaap.datarouter.node.properties", "src/test/resources/node_test.properties");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/src/test/resources/spool"));
        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/src/test/resources/logs"));
    }

    @Test
    public void Verify_NodeConfigMan_Getters() {
        NodeConfigManager nodeConfigManager = NodeConfigManager.getInstance();
        Assert.assertEquals("legacy", nodeConfigManager.getAafInstance());
        Assert.assertEquals("src/test/resources/spool/f", nodeConfigManager.getSpoolDir());
        Assert.assertEquals("src/test/resources/spool", nodeConfigManager.getSpoolBase());
        Assert.assertEquals("PKCS12", nodeConfigManager.getKSType());
        Assert.assertEquals(8080, nodeConfigManager.getHttpPort());
        Assert.assertEquals(8443, nodeConfigManager.getHttpsPort());
        Assert.assertEquals(443, nodeConfigManager.getExtHttpsPort());
        Assert.assertEquals("dmaap-dr-node", nodeConfigManager.getMyName());
        Assert.assertEquals("https://dmaap-dr-prov:8443/internal/logs", nodeConfigManager.getEventLogUrl());
        Assert.assertEquals("src/test/resources/logs/events", nodeConfigManager.getEventLogPrefix());
        Assert.assertEquals(".log", nodeConfigManager.getEventLogSuffix());
        Assert.assertEquals("src/test/resources/logs", nodeConfigManager.getLogDir());
        Assert.assertEquals((86400000L * 30), nodeConfigManager.getLogRetention());
        Assert.assertEquals(new String[] {"TLSv1.1", "TLSv1.2"}, nodeConfigManager.getEnabledprotocols());
        Assert.assertEquals("org.onap.dmaap-dr.feed", nodeConfigManager.getAafType());
        Assert.assertEquals("publish", nodeConfigManager.getAafAction());
        Assert.assertFalse(nodeConfigManager.getCadiEnabled());
        Assert.assertFalse(nodeConfigManager.isShutdown());
        Assert.assertFalse(nodeConfigManager.isConfigured());
    }
}