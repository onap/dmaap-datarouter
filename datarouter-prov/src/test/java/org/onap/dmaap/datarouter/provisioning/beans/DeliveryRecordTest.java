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

package org.onap.dmaap.datarouter.provisioning.beans;

import org.junit.Assert;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

import java.text.ParseException;

public class DeliveryRecordTest {

    private DeliveryRecord deliveryRecord;

    @Test
    public void Validate_Constructor_Creates_Object_With_Get_Methods() throws ParseException {
        String[] args = {"2018-08-29-10-10-10-543.", "del", "238465493.fileName", "1", "285", "123/file.txt", "GET", "application/json", "2000", "example", "100"};
        deliveryRecord = new DeliveryRecord(args);
        Assert.assertEquals("238465493.fileName", deliveryRecord.getPublishId());
        Assert.assertEquals(1, deliveryRecord.getFeedid());
        Assert.assertEquals(285, deliveryRecord.getSubid());
        Assert.assertEquals("123/file.txt", deliveryRecord.getRequestUri());
        Assert.assertEquals("GET", deliveryRecord.getMethod());
        Assert.assertEquals("file.txt", deliveryRecord.getFileid());
        Assert.assertEquals("application/json", deliveryRecord.getContentType());
        Assert.assertEquals(2000, deliveryRecord.getContentLength());
        Assert.assertEquals("example", deliveryRecord.getUser());
        Assert.assertEquals(100, deliveryRecord.getResult());
    }

    @Test
    public void Validate_AsJsonObject_Correct_Json_Object_After_Set_Methods() throws ParseException {
        String[] args = {"2018-08-29-10-10-10-543.", "del", "238465493.fileName", "1", "285", "123/file.txt", "GET", "application/json", "2000", "example", "100"};
        deliveryRecord = new DeliveryRecord(args);
        deliveryRecord.setContentLength(265);
        deliveryRecord.setEventTime(1535533810543L);
        deliveryRecord.setPublishId("2345657324.fileName");
        deliveryRecord.setSubid(287);
        deliveryRecord.setFeedid(2);
        deliveryRecord.setRequestUri("/delete/2");
        deliveryRecord.setMethod("PUT");
        deliveryRecord.setContentType("application/json");
        deliveryRecord.setFileid("file2.txt");
        deliveryRecord.setUser("example2");
        deliveryRecord.setResult(300);
        LOGJSONObject deliveryRecordJson = createBaseLogRecordJson();
        String deliveryRecordString = stripBracketFromJson(deliveryRecordJson);
        String deliveryRecordStringObject = stripBracketFromJson(deliveryRecord.asJSONObject());
        Assert.assertTrue(deliveryRecordStringObject.matches(deliveryRecordString));
    }

    private LOGJSONObject createBaseLogRecordJson() {
        LOGJSONObject deliveryRecordJson = new LOGJSONObject();
        deliveryRecordJson.put("statusCode", 300);
        deliveryRecordJson.put("deliveryId", "example2");
        deliveryRecordJson.put("publishId", "2345657324.fileName");
        deliveryRecordJson.put("requestURI", "/delete/2");
        deliveryRecordJson.put("method", "PUT");
        deliveryRecordJson.put("contentType", "application/json");
        deliveryRecordJson.put("type", "del");
        deliveryRecordJson.put("date", "2018-08-29T[0-1][0-9]:10:10.543Z");
        deliveryRecordJson.put("contentLength", 265);

        return deliveryRecordJson;
    }

    private String stripBracketFromJson(LOGJSONObject deliveryRecordJson) {
        String deliveryRecordString = deliveryRecordJson.toString();
        deliveryRecordString = deliveryRecordString.substring(1, deliveryRecordString.length() - 1);
        return deliveryRecordString;
    }
}
