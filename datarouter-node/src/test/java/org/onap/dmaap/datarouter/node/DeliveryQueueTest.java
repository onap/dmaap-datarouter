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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class DeliveryQueueTest {

    private DeliveryQueue deliveryQueue;
    @Mock
    private DestInfo destInfo;
    @Mock
    DeliveryQueueHelper deliveryQueueHelper;

    private String dirPath = "/tmp/dir001/";
    private String FileName1 = "10000000000004.fileName.M";

    @Before
    public void setUp() {
        when(destInfo.getSpool()).thenReturn("tmp");
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
    }

    @Test
    public void Given_New_DeliveryQueue_Directory_Is_Created_As_Defined_By_DestInfo() {
        when(destInfo.getSpool()).thenReturn("tmp");
        File file = new File("tmp");
        assertTrue(file.exists());
        deleteFile("tmp");
    }

    @Test
    public void Given_Delivery_Task_Failed_And_Resume_Time_Not_Reached_Return_Null() throws Exception {
        FieldUtils.writeField(deliveryQueue,"failed",true,true);
        FieldUtils.writeField(deliveryQueue,"resumetime",System.currentTimeMillis()*2,true);
        assertNull(deliveryQueue.peekNext());
    }

    @Test
    public void Given_Delivery_Task_Return_Next_Delivery_Task_Id() throws Exception {
        prepareFiles();
        when(destInfo.getSpool()).thenReturn(dirPath);
        deliveryQueue = new DeliveryQueue(deliveryQueueHelper, destInfo);
        DeliveryTask nt = deliveryQueue.getNext();
        assertEquals("10000000000004.fileName", nt.getPublishId());
        deleteFile(dirPath + FileName1);
        deleteFile(dirPath);
    }

    @Test
    public void Given_Delivery_Task_Cancel_And_FileId_Is_Null_Return_Zero() {
        long rc = deliveryQueue.cancelTask("123.node.datarouternew.com");
        assertEquals(0, rc);
    }

    private void prepareFiles() throws Exception {
        createFolder(dirPath);
        createFile(FileName1, dirPath);
        String[] files = new String[2];
        files[0] = dirPath + FileName1;
    }

    private void createFolder(String dirName) throws Exception {
        String dirPath = dirName;

        File newDirectory = new File(dirPath);
        boolean isCreated = newDirectory.mkdirs();
        if (isCreated) {
            System.out.println("1. Successfully created directories, path: " + newDirectory.getCanonicalPath());
        } else if (newDirectory.exists()) {
            System.out.print("1. Directory path already exist, path: " + newDirectory.getCanonicalPath());
        } else {
            System.out.println("1. Unable to create directory");
        }
    }

    private void createFile(String file, String dir) throws Exception {
        String FileName = file;
        String dirPath = dir;

        File newFile = new File(dirPath + File.separator + FileName);
        boolean isCreated = newFile.createNewFile();
        if (isCreated) {
            System.out.println("\n2. Successfully created new file, path: " + newFile.getCanonicalPath());
        } else { //File may already exist
            System.out.println("\n2. Unable to create new file: " + newFile.getCanonicalPath());
        }
    }

    private void deleteFile(String fileName) {
        File file = new File(fileName);

        if (file.exists()) {
            file.delete();
        }
    }

}