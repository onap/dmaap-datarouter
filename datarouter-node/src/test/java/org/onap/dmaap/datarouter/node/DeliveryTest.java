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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.node.delivery.Delivery;
import org.onap.dmaap.datarouter.node.delivery.Delivery.DelItem;
import org.onap.dmaap.datarouter.node.delivery.DeliveryQueue;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class DeliveryTest {

    @Mock
    private DeliveryQueue deliveryQueue;
    @Mock
    private NodeConfigManager config;
    private File nDir = new File("tmp/n");
    private File newNDir = new File("tmp/n/0");
    private File newNFile = new File("tmp/n/0/testN.txt");
    private File sDir = new File("tmp/s");
    private File newSDir = new File("tmp/s/0/1");
    private File newSpoolFile = new File("tmp/s/0/1/123456789.dmaap-dr-node");
    private File spoolFileMeta = new File("tmp/s/0/1/123456789.dmaap-dr-node.M");

    @Before
    public void setUp() throws IOException {
        nDir.mkdirs();
        sDir.mkdirs();
        newNDir.mkdirs();
        newNFile.createNewFile();
        newSDir.mkdirs();
        newSpoolFile.createNewFile();
        spoolFileMeta.createNewFile();
        config = mockNodeConfigManager();
    }

    @Test
    public void Validate_Reset_Queue_Calls_Reset_Queue_On_Delivery_Queue_Object() throws IllegalAccessException {
        Delivery delivery = new Delivery(config);
        HashMap<String, DeliveryQueue> dqs = new HashMap<>();
        dqs.put("tmp/s/0/1", deliveryQueue);
        FieldUtils.writeDeclaredField(delivery, "dqs", dqs, true);
        delivery.resetQueue("tmp/s/0/1");
        verify(deliveryQueue, times(1)).resetQueue();
    }

    @Test
    public void Validate_Mark_Success_Calls_Mark_Success_On_Delivery_Queue_Object() throws IllegalAccessException {
        Delivery delivery = new Delivery(config);
        HashMap<String, DeliveryQueue> dqs = new HashMap<>();
        dqs.put("tmp/s/0/1", deliveryQueue);
        FieldUtils.writeDeclaredField(delivery, "dqs", dqs, true);
        delivery.markTaskSuccess("tmp/s/0/1", "123456789.dmaap-dr-node");
        verify(deliveryQueue, times(1)).markTaskSuccess("123456789.dmaap-dr-node");
    }

    @Test
    public void Validate_DelItem_With_Equal_Spool_And_PubId_Are_Equal() {
        DelItem delItem1 = new DelItem("123456789.dmaap-dr-node", "tmp/s/0/1");
        DelItem delItem2 = new DelItem("123456789.dmaap-dr-node", "tmp/s/0/1");
        Assert.assertEquals(delItem1, delItem2);
        Assert.assertEquals(0, delItem1.compareTo(delItem2));
    }

    @Test
    public void Validate_DelItem_With_Unequal_Spool_And_PubId_Are_Not_Equal() {
        DelItem delItem1 = new DelItem("123456789.dmaap-dr-node", "tmp/s/0/1");
        DelItem delItem2 = new DelItem("000000000.dmaap-dr-node", "tmp/s/0/2");
        Assert.assertNotEquals(delItem1, delItem2);
        Assert.assertNotEquals(0, delItem1.compareTo(delItem2));
    }

    @After
    public void tearDown() {
        newSpoolFile.delete();
        spoolFileMeta.delete();
        newNFile.delete();
        newNDir.delete();
        newSDir.delete();
        new File("tmp/s/0").delete();
        nDir.delete();
        sDir.delete();
        File tmpDir = new File("tmp");
        tmpDir.delete();
    }

    private NodeConfigManager mockNodeConfigManager() {
        NodeConfigManager config = mock(NodeConfigManager.class);
        PowerMockito.when(config.isConfigured()).thenReturn(true);
        PowerMockito.when(config.getAllDests()).thenReturn(createDestInfoObjects());
        PowerMockito.when(config.getFreeDiskStart()).thenReturn(0.9);
        PowerMockito.when(config.getFreeDiskStop()).thenReturn(0.2);
        PowerMockito.when(config.getDeliveryThreads()).thenReturn(0);
        PowerMockito.when(config.getSpoolBase()).thenReturn("tmp");
        return config;
    }

    private DestInfo[] createDestInfoObjects() {
        DestInfo[] destInfos = new DestInfo[1];
        DestInfo destInfo = new DestInfoBuilder().setName("node.datarouternew.com").setSpool("tmp/s/0/1")
                .setSubid("1")
                .setLogdata("logs/").setUrl("/subs/1").setAuthuser("user1")
                .setAuthentication("Basic dXNlcjE6cGFzc3dvcmQx")
                .setMetaonly(false).setUse100(true).setPrivilegedSubscriber(false).setFollowRedirects(false)
                .setDecompress(false).createDestInfo();
        destInfos[0] = destInfo;
        return destInfos;
    }
}
