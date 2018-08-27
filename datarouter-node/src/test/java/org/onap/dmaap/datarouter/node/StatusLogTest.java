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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
@PrepareForTest(StatusLog.class)
public class StatusLogTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(NodeConfigManager.class);
        NodeConfigManager config = mock(NodeConfigManager.class);
        when(config.getEventLogInterval()).thenReturn("5m");
        when(config.getEventLogPrefix()).thenReturn("logFile");
        when(config.getEventLogSuffix()).thenReturn(".log");
        PowerMockito.when(NodeConfigManager.getInstance()).thenReturn(config);
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1535367126000L);
    }

    @Test
    public void Given_Time_Interval_Parse_Interval_Returns_Correct_Value() {
        long parsedTime = StatusLog.parseInterval("2h24m35s", 1);
        Assert.assertEquals(8640000, parsedTime);
    }

    @Test
    public void Given_Time_Interval_In_Seconds_Parse_Interval_Returns_Correct_Value() {
        long parsedTime = StatusLog.parseInterval("56784", 1);
        Assert.assertEquals(43200000, parsedTime);
    }

    @Test
    public void Validate_Get_Cur_Log_File_Returns_Correct_File_Name() {
        String logFile = StatusLog.getCurLogFile();
        Assert.assertTrue(logFile.matches("logFile-201808271[0-1]50.log"));
    }
}
