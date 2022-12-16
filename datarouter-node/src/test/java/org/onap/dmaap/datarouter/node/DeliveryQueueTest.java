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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.node.delivery.DeliveryQueue;
import org.onap.dmaap.datarouter.node.delivery.DeliveryQueueHelper;
import org.onap.dmaap.datarouter.node.delivery.DeliveryTask;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.node.NodeConfigManager")
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class DeliveryQueueTest {

    @Mock
    DeliveryQueueHelper deliveryQueueHelper;
    private DeliveryQueue deliveryQueue;
    @Mock
    private DestInfo destInfo;
    private String dirPath = "/tmp/dir001/";
    private String fileName = "10000000000004.fileName.M";

    @Before
    public void setUp() throws IllegalAccessException {
        when(destInfo.getSpool()).thenReturn(dirPath);
        when(destInfo.isPrivilegedSubscriber()).thenReturn(true);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        NodeConfigManager configManager = mockNodeConfigManager();
        FieldUtils.writeDeclaredStaticField(NodeConfigManager.class, "base", configManager, true);
    }

    @Test
    public void Given_New_DeliveryQueue_Directory_Is_Created_As_Defined_By_DestInfo() {
        File file = new File("/tmp");
        assertTrue(file.exists());
    }

    @Test
    public void Given_Delivery_Task_Failed_And_Resume_Time_Not_Reached_Return_Null() throws Exception {
        FieldUtils.writeField(deliveryQueue, "failed", true, true);
        FieldUtils.writeField(deliveryQueue, "resumetime", System.currentTimeMillis() * 2, true);
        assertNull(deliveryQueue.peekNext());
    }

    @Test
    public void Given_Delivery_Task_Return_Next_Delivery_Task_Id() throws Exception {
        prepareFiles();
        when(destInfo.getSpool()).thenReturn(dirPath);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        DeliveryTask nt = deliveryQueue.getNext();
        assertEquals("10000000000004.fileName", nt.getPublishId());
        deleteFile(dirPath + fileName);
        deleteFile(dirPath);
    }

    @Test
    public void Given_Task_In_Todo_Is_Already_Cleaned_GetNext_Returns_Null() throws Exception {
        when(deliveryQueueHelper.getExpirationTimer()).thenReturn(10000L);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        Vector<DeliveryTask> tasks = new Vector<>();
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123.node.datarouternew.com");
        task.clean();
        tasks.add(task);
        FieldUtils.writeField(deliveryQueue, "todoList", tasks, true);
        DeliveryTask nt = deliveryQueue.getNext();
        assertNull(nt);
    }

    @Test
    public void Given_Task_In_Todo_Has_Resume_Time_In_Future_GetNext_Returns_Null() throws Exception {
        when(destInfo.isPrivilegedSubscriber()).thenReturn(true);
        when(deliveryQueueHelper.getExpirationTimer()).thenReturn(10000L);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        Vector<DeliveryTask> tasks = new Vector<>();
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123.node.datarouternew.com");
        long timeInFuture = 2558366240223L;
        task.setResumeTime(timeInFuture);
        tasks.add(task);
        FieldUtils.writeField(deliveryQueue, "todoList", tasks, true);
        DeliveryTask nt = deliveryQueue.getNext();
        assertNull(nt);
    }

    @Test
    public void Given_Task_In_Todo_Is_Expired_GetNext_Returns_Null() throws Exception {
        when(destInfo.isPrivilegedSubscriber()).thenReturn(true);
        when(deliveryQueueHelper.getExpirationTimer()).thenReturn(10000L);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        Vector<DeliveryTask> tasks = new Vector<>();
        DeliveryTask task = new DeliveryTask(deliveryQueue, "123.node.datarouternew.com");
        long timeInPast = 1058366240223L;
        task.setResumeTime(timeInPast);
        tasks.add(task);
        FieldUtils.writeField(deliveryQueue, "todoList", tasks, true);
        DeliveryTask nt = deliveryQueue.getNext();
        assertNull(nt);
    }

    @Test
    public void Given_Delivery_Task_Cancel_And_FileId_Is_Null_Return_Zero() {
        long rc = deliveryQueue.cancelTask("123.node.datarouternew.com");
        assertEquals(0, rc);
    }

    @Test
    public void Given_Delivery_Task_Is_Working_Cancel_Task_Returns_Zero() throws IllegalAccessException {
        HashMap<String, DeliveryTask> tasks = new HashMap<>();
        tasks.put("123.node.datarouternew.com", new DeliveryTask(deliveryQueue, "123.node.datarouternew.com"));
        FieldUtils.writeField(deliveryQueue, "working", tasks, true);
        long rc = deliveryQueue.cancelTask("123.node.datarouternew.com");
        assertEquals(0, rc);
    }

    @Test
    public void Given_Delivery_Task_In_Todo_Cancel_Task_Returns_Zero() throws IllegalAccessException {
        List<DeliveryTask> tasks = new ArrayList<>();
        tasks.add(new DeliveryTask(deliveryQueue, "123.node.datarouternew.com"));
        FieldUtils.writeField(deliveryQueue, "todoList", tasks, true);
        long rc = deliveryQueue.cancelTask("123.node.datarouternew.com");
        assertEquals(0, rc);
    }

    @Test
    public void Given_Ok_Status_And_Privileged_Subscriber_Then_Set_Resume_Time_Is_Called_On_DeliveryTask() {
        DeliveryTask deliveryTask = mockDeliveryTask();
        deliveryQueue.reportStatus(deliveryTask, 200, "123456789.dmaap-dr-node", "delivery");
        verify(deliveryTask, times(1)).setResumeTime(anyLong());
        cleanUpLogging();
    }

    @Test
    public void Given_Ok_Status_And_Not_Privileged_Subscriber_Then_Clean_Is_Called_On_DeliveryTask() {
        DeliveryTask deliveryTask = mockDeliveryTask();
        when(destInfo.isPrivilegedSubscriber()).thenReturn(false);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        deliveryQueue.reportStatus(deliveryTask, 200, "123456789.dmaap-dr-node", "delivery");
        verify(deliveryTask, times(1)).clean();
        cleanUpLogging();
    }

    @Test
    public void Given_Not_Ok_Status_Then_Clean_Is_Called_On_DeliveryTask() {
        DeliveryTask deliveryTask = mockDeliveryTask();
        deliveryQueue.reportStatus(deliveryTask, 400, "123456789.dmaap-dr-node", "delivery");
        verify(deliveryTask, times(1)).clean();
        cleanUpLogging();
    }

    @Test
    public void Given_Task_In_Working_MarkTaskSuccess_Returns_True() throws IllegalAccessException {
        HashMap<String, DeliveryTask> tasks = new HashMap<>();
        tasks.put("123.node.datarouternew.com", new DeliveryTask(deliveryQueue, "123.node.datarouternew.com"));
        FieldUtils.writeField(deliveryQueue, "working", tasks, true);
        assertTrue(deliveryQueue.markTaskSuccess("123.node.datarouternew.com"));
    }

    @Test
    public void Given_Task_In_Retry_MarkTaskSuccess_Returns_True() throws IllegalAccessException {
        HashMap<String, DeliveryTask> tasks = new HashMap<>();
        tasks.put("123.node.datarouternew.com", new DeliveryTask(deliveryQueue, "123.node.datarouternew.com"));
        FieldUtils.writeField(deliveryQueue, "retry", tasks, true);
        assertTrue(deliveryQueue.markTaskSuccess("123.node.datarouternew.com"));
    }

    @Test
    public void Given_Task_Does_Not_Exist_MarkTaskSuccess_Returns_False() {
        assertFalse(deliveryQueue.markTaskSuccess("false.pubId.com"));
    }

    private void cleanUpLogging() {
        final File currentDir = new File(System.getProperty("user.dir"));
        final File[] files = currentDir.listFiles((file, name) -> name.matches("null.*"));
        if (files != null) {
            for (final File file : files) {
                file.delete();
            }
        }
    }

    @NotNull
    private DeliveryTask mockDeliveryTask() {
        DeliveryTask deliveryTask = mock(DeliveryTask.class);
        when(deliveryTask.getPublishId()).thenReturn("123456789.dmaap-dr-node");
        when(deliveryTask.getFeedId()).thenReturn("1");
        when(deliveryTask.getSubId()).thenReturn("1");
        when(deliveryTask.getURL()).thenReturn("http://subcriber.com:7070/delivery");
        when(deliveryTask.getCType()).thenReturn("application/json");
        when(deliveryTask.getLength()).thenReturn(486L);
        return deliveryTask;
    }

    private NodeConfigManager mockNodeConfigManager() {
        NodeConfigManager config = mock(NodeConfigManager.class);
        PowerMockito.when(config.getEventLogInterval()).thenReturn("30000");
        return config;
    }

    private void prepareFiles() throws Exception {
        createFolder(dirPath);
        createFile(fileName, dirPath);
    }

    private void createFolder(String dirName) {
        File newDirectory = new File(dirName);
        newDirectory.mkdirs();
    }

    private void createFile(String file, String dir) throws Exception {
        File newFile = new File(dir + File.separator + file);
        newFile.createNewFile();
    }

    private void deleteFile(String fileName) {
        File file = new File(fileName);

        if (file.exists()) {
            file.delete();
        }
    }

}