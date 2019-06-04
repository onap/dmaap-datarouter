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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

public class DeliveryTaskTest {

    @Mock
    private DeliveryQueue deliveryQueue;

    @Test
    public void Validate_Delivery_Task_Equals() {
        DestInfo destInfo = getDestInfo();
        deliveryQueue = mockDelvieryQueue(destInfo);
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123456789.test-dr-datafile");
        DeliveryTask task2 = new DeliveryTask(deliveryQueue, "123456789.test-dr-datafile");
        Assert.assertEquals(task, task2);
        Assert.assertEquals(task.hashCode(), task2.hashCode());
        Assert.assertEquals(task.toString(), task2.toString());
        Assert.assertEquals(0, task.compareTo(task2));
    }

    @Test
    public void Validate_Delivery_Tasks_Not_Equal() {
        DestInfo destInfo = getDestInfo();
        deliveryQueue = mockDelvieryQueue(destInfo);
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123456789.test-dr-node");
        DeliveryTask task2 = new DeliveryTask(deliveryQueue, "123456789.test-dr-datafile");
        Assert.assertNotEquals(task, task2);
        Assert.assertNotEquals(0, task.compareTo(task2));
    }

    private DestInfo getDestInfo() {
        return new DestInfoBuilder().setName("n:" + "dmaap-dr-node")
                .setSpool(System.getProperty("user.dir") + "/src/test/resources")
                .setSubid("1").setLogdata("n2n-dmaap-dr-node").setUrl("https://dmaap-dr-node:8443/internal/publish")
                .setAuthuser("dmaap-dr-node").setAuthentication("Auth").setMetaonly(false).setUse100(true)
                .setPrivilegedSubscriber(false).setFollowRedirects(false).setDecompress(false).createDestInfo();
    }

    private DeliveryQueue mockDelvieryQueue(DestInfo destInfo) {
        DeliveryQueue mockedDeliveryQueue = mock(DeliveryQueue.class);
        when(mockedDeliveryQueue.getDestinationInfo()).thenReturn(destInfo);
        return mockedDeliveryQueue;
    }

}
