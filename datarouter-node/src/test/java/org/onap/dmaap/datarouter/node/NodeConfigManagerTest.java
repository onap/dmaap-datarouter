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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.node.config.NodeConfig;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;

@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.node.NodeConfigManager"})
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.auth.x500.X500Principal", "javax.crypto.*"})
@PrepareForTest({InetAddress.class})
public class NodeConfigManagerTest {

    private static HttpUrlStreamHandler httpUrlStreamHandler;

    @Mock
    InputStream inputStream;

    @Mock
    NodeConfig nodeConfig;

    @BeforeClass
    public static void init() {
        System.setProperty("org.onap.dmaap.datarouter.node.properties", "src/test/resources/node_test.properties");
        // Allows for mocking URL connections
        URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
        URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);
        httpUrlStreamHandler = new HttpUrlStreamHandler();
        given(urlStreamHandlerFactory.createURLStreamHandler("https")).willReturn(httpUrlStreamHandler);
    }

    @Before
    public void reset() throws IOException {
        String href = "https://dmaap-dr-prov:8443/internal/prov";
        URLConnection urlConnection = mock(URLConnection.class);
        httpUrlStreamHandler.addConnection(new URL(href), urlConnection);
        InputStream anyInputStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get("src/test/resources/prov_data.json")));
        when(urlConnection.getInputStream()).thenReturn(anyInputStream);
    }

    @After
    public void resetHandler() {
        httpUrlStreamHandler.resetConnections();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/src/test/resources/spool"));
        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/src/test/resources/logs"));
    }

    @Test
    public void Verify_NodeConfigMan_Getters_Secure() {
        NodeConfigManager nodeConfigManager = NodeConfigManager.getInstance();
        Assert.assertEquals("src/test/resources/spool/f", nodeConfigManager.getSpoolDir());
        Assert.assertEquals("src/test/resources/spool", nodeConfigManager.getSpoolBase());
        Assert.assertEquals(8080, nodeConfigManager.getHttpPort());
        Assert.assertEquals(8443, nodeConfigManager.getHttpsPort());
        Assert.assertEquals(443, nodeConfigManager.getExtHttpsPort());
        Assert.assertEquals("dmaap-dr-node", nodeConfigManager.getMyName());
        Assert.assertEquals("https://dmaap-dr-prov:8443/internal/logs", nodeConfigManager.getEventLogUrl());
        Assert.assertEquals("src/test/resources/logs/events", nodeConfigManager.getEventLogPrefix());
        Assert.assertEquals(".log", nodeConfigManager.getEventLogSuffix());
        Assert.assertEquals("src/test/resources/logs", nodeConfigManager.getLogDir());
        Assert.assertEquals((86400000L * 30), nodeConfigManager.getLogRetention());
        Assert.assertFalse(nodeConfigManager.isShutdown());
        Assert.assertFalse(nodeConfigManager.isTlsEnabled());
        Assert.assertTrue(nodeConfigManager.isConfigured());
        Assert.assertNotNull(nodeConfigManager.getPublishId());
        Assert.assertNotNull(nodeConfigManager.getAllDests());
        Assert.assertEquals(10000, nodeConfigManager.getInitFailureTimer());
        Assert.assertEquals(600000, nodeConfigManager.getWaitForFileProcessFailureTimer());
        Assert.assertEquals(3600000, nodeConfigManager.getMaxFailureTimer());
        Assert.assertEquals(2.0, nodeConfigManager.getFailureBackoff(),0.0);
        Assert.assertEquals(86400000, nodeConfigManager.getExpirationTimer());
        Assert.assertEquals(100, nodeConfigManager.getFairFileLimit());
        Assert.assertEquals(60000, nodeConfigManager.getFairTimeLimit());
        Assert.assertNotNull(nodeConfigManager.getTargets("1"));
        Assert.assertEquals("src/test/resources/spool/f", nodeConfigManager.getSpoolDir());
        Assert.assertEquals(40, nodeConfigManager.getDeliveryThreads());
        Assert.assertEquals("30", nodeConfigManager.getEventLogInterval());
        Assert.assertFalse(nodeConfigManager.isFollowRedirects());
        Assert.assertNotNull(nodeConfigManager.getTimer());
        Assert.assertEquals("1", nodeConfigManager.getFeedId("1"));
        Assert.assertEquals("Basic ZG1hYXAtZHItbm9kZTp2OStFanZpWVBXSURrazVxRlF5ZkoxSC9LdHBuYWo4K0NVTXlNL0lRRUp2UGdjOUxpU2s5ZnpKTjdFazl3SzZkaG11S1E4S3RtcC9kelpBU3BRUGZCdz09", nodeConfigManager.getMyAuth());
        Assert.assertEquals(0.05, nodeConfigManager.getFreeDiskStart(), 0.0);
        Assert.assertEquals(0.2, nodeConfigManager.getFreeDiskStop(), 0.0);
    }

    /**
     * {@link URLStreamHandler} that allows us to control the {@link URLConnection URLConnections} that are returned
     * by {@link URL URLs} in the code under test.
     */
    public static class HttpUrlStreamHandler extends URLStreamHandler {

        private Map<URL, URLConnection> connections = new HashMap();

        @Override
        protected URLConnection openConnection(URL url) {
            return connections.get(url);
        }

        void resetConnections() {
            connections = new HashMap();
        }

        HttpUrlStreamHandler addConnection(URL url, URLConnection urlConnection) {
            connections.put(url, urlConnection);
            return this;
        }
    }
}