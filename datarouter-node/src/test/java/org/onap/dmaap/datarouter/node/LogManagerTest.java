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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.node.LogManager.Uploader;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.node.NodeConfigManager"})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class LogManagerTest {

    @Mock
    private NodeConfigManager config;

    private LogManager logManager;

    @Before
    public void setUp() throws IllegalAccessException {
        mockNodeConfigManager();
        logManager = new LogManager(config);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        File spoolDir = new File(System.getProperty("user.dir") + "/src/test/resources/.spool");
        FileUtils.deleteDirectory(spoolDir);
    }

    @Test
    public void Verify_LogManager_Attempts_To_Deliver_Log_Files_To_Prov() {
        logManager.run();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
        File file = new File(System.getProperty("user.dir") + "/src/test/resources/.spool/.lastqueued");
        assertTrue(file.isFile());
    }

    @Test
    public void Validate_Uploader_Getters() {
        Uploader worker = logManager.getWorker();
        assertEquals(10000L, worker.getInitFailureTimer());
        assertEquals(600000L, worker.getWaitForFileProcessFailureTimer());
        assertEquals(2.0, worker.getFailureBackoff(), 0.0);
        assertEquals(150000L, worker.getMaxFailureTimer());
        assertEquals(604800000L, worker.getExpirationTimer());
        assertEquals(10000, worker.getFairFileLimit());
        assertEquals(86400000, worker.getFairTimeLimit());
        assertEquals("https://dmaap-dr-prov:8443/internal/logs",
                worker.getDestURL(new DestInfoBuilder().createDestInfo(), "String"));
        assertFalse(worker.handleRedirection(new DestInfoBuilder().createDestInfo(), "", ""));
        assertFalse(worker.isFollowRedirects());
        assertNull(worker.getFeedId(""));
    }

    private void mockNodeConfigManager() throws IllegalAccessException {
        PowerMockito.when(config.getLogDir()).thenReturn(System.getProperty("user.dir") + "/src/test/resources");
        PowerMockito.when(config.getTimer()).thenReturn(new Timer("Node Configuration Timer", true));
        PowerMockito.when(config.getEventLogPrefix())
                .thenReturn(System.getProperty("user.dir") + "/src/test/resources/events");
        PowerMockito.when(config.getEventLogSuffix()).thenReturn(".log");
        PowerMockito.when(config.getLogRetention()).thenReturn(94608000000L);
        PowerMockito.when(config.getEventLogInterval()).thenReturn("30s");
        PowerMockito.when(config.getPublishId()).thenReturn("123456789.dmaap-dr-node");
        PowerMockito.when(config.getEventLogUrl()).thenReturn("https://dmaap-dr-prov:8443/internal/logs");
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", config, true);
    }

}
