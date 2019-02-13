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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
public class DeliveryTest {

  @Mock
  private DeliveryQueue deliveryQueue;

  private File nDir = new File("tmp/n");
  private File sDir = new File("tmp/s");

  @Before
  public void setUp() throws IOException {
    nDir.mkdirs();
    sDir.mkdirs();
    File newNDir = new File("tmp/n/0");
    newNDir.mkdirs();
    File newNFile = new File("tmp/n/0/testN.txt");
    newNFile.createNewFile();
    File newSDir = new File("tmp/s/0/1");
    newSDir.mkdirs();
    File newSpoolFile = new File("tmp/s/0/1/testSpool.txt");
    newSpoolFile.createNewFile();
  }

  @Test
  public void Validate_Reset_Queue_Calls_Reset_Queue_On_Delivery_Queue_Object() throws IllegalAccessException {
    NodeConfigManager config = mockNodeConfigManager();
    Delivery delivery = new Delivery(config);
    Hashtable<String, DeliveryQueue> dqs = new Hashtable<>();
    dqs.put("spool/s/0/1", deliveryQueue);
    FieldUtils.writeDeclaredField(delivery, "dqs", dqs, true);
    delivery.resetQueue("spool/s/0/1");
    verify(deliveryQueue, times(1)).resetQueue();
  }

  @After
  public void tearDown() {
    nDir.delete();
    sDir.delete();
    File tmpDir = new File("tmp");
    tmpDir.delete();
  }

  private NodeConfigManager mockNodeConfigManager() {
    PowerMockito.mockStatic(NodeConfigManager.class);
    NodeConfigManager config = mock(NodeConfigManager.class);
    PowerMockito.when(config.isConfigured()).thenReturn(true);
    PowerMockito.when(config.getAllDests()).thenReturn(createDestInfoObjects());
    PowerMockito.when(config.getFreeDiskStart()).thenReturn(0.49);
    PowerMockito.when(config.getFreeDiskStop()).thenReturn(0.5);
    PowerMockito.when(config.getDeliveryThreads()).thenReturn(0);
    PowerMockito.when(config.getSpoolBase()).thenReturn("tmp");
    return config;
  }

  private DestInfo[] createDestInfoObjects() {
    DestInfo[] destInfos = new DestInfo[1];
    DestInfo destInfo = new DestInfo("node.datarouternew.com", "spool/s/0/1", "1", "logs/", "/subs/1", "user1", "Basic dXNlcjE6cGFzc3dvcmQx", false, true, false);
    destInfos[0] = destInfo;
    return destInfos;
  }
}
