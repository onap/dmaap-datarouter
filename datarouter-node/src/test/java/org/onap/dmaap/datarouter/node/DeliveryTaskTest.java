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

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.dmaap.datarouter.node.delivery.DeliveryQueue;
import org.onap.dmaap.datarouter.node.delivery.DeliveryTask;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class DeliveryTaskTest {

    @Mock
    private DeliveryQueue deliveryQueue;

    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        DestInfo destInfo = getPrivDestInfo();
        deliveryQueue = mockDelvieryQueue(destInfo);

        URL url = PowerMockito.mock(URL.class);
        HttpURLConnection urlConnection = PowerMockito.mock(HttpURLConnection.class);

        PowerMockito.whenNew(URL.class).withParameterTypes(String.class).withArguments(Mockito.anyString()).thenReturn(url);

        PowerMockito.when(urlConnection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        PowerMockito.when(urlConnection.getHeaderField(0)).thenReturn("PUT");
        PowerMockito.when(urlConnection.getResponseCode()).thenReturn(200);
        PowerMockito.when(url.openConnection()).thenReturn(urlConnection);
    }

    @After
    public void tearDown() {
    }


    @Test
    public void Validate_Delivery_Task_Equals() {
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123456789.test-dr-node");
        DeliveryTask task2 = new DeliveryTask(deliveryQueue, "123456789.test-dr-node");
        Assert.assertEquals(task, task2);
        Assert.assertEquals(task.hashCode(), task2.hashCode());
        Assert.assertEquals(task.toString(), task2.toString());
        Assert.assertEquals(task.getPublishId(), task2.getPublishId());
        Assert.assertEquals(task.getSubId(), task2.getSubId());
        Assert.assertEquals(task.getFeedId(), task2.getFeedId());
        Assert.assertEquals(task.getLength(), task2.getLength());
        Assert.assertEquals(task.isCleaned(), task2.isCleaned());
        Assert.assertEquals(task.getDate(), task2.getDate());
        Assert.assertEquals(task.getURL(), task2.getURL());
        Assert.assertEquals(task.getCType(), task2.getCType());
        Assert.assertEquals(task.getMethod(), task2.getMethod());
        Assert.assertEquals(task.getFileId(), task2.getFileId());
        Assert.assertEquals(task.getAttempts(), task2.getAttempts());
        Assert.assertEquals(task.getFollowRedirects(), task2.getFollowRedirects());
        Assert.assertEquals(0, task.compareTo(task2));
    }

    @Test
    public void Validate_Delivery_Tasks_Success_For_Standard_File() throws InterruptedException {
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123456789.test-dr-node");
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(task);

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void Validate_Delivery_Tasks_Success_For_Compressed_File() throws InterruptedException {
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123456789.test-dr-node.gz");
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(task);

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    private DestInfo getPrivDestInfo() {
        return new DestInfoBuilder().setName("n:" + "dmaap-dr-node")
                       .setSpool(System.getProperty("user.dir") + "/src/test/resources/delivery_files")
                       .setSubid("1").setLogdata("n2n-dmaap-dr-node").setUrl("https://dmaap-dr-node:8443/internal/publish")
                       .setAuthuser("dmaap-dr-node").setAuthentication("Auth").setMetaonly(false).setUse100(true)
                       .setPrivilegedSubscriber(true).setFollowRedirects(false).setDecompress(true).createDestInfo();
    }

    private DeliveryQueue mockDelvieryQueue(DestInfo destInfo) {
        DeliveryQueue mockedDeliveryQueue = PowerMockito.mock(DeliveryQueue.class);
        PowerMockito.when(mockedDeliveryQueue.getDestinationInfo()).thenReturn(destInfo);
        PowerMockito.when(mockedDeliveryQueue.getDestURL(Mockito.anyString())).thenReturn("https://dmaap-dr-node:8443/internal/publish");
        return mockedDeliveryQueue;
    }

}
