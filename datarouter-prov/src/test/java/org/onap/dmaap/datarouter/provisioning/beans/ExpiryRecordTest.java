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

public class ExpiryRecordTest {


    private ExpiryRecord expiryRecord;

    @Test
    public void Validate_Constructor_Creates_Object_With_Get_Methods() throws ParseException {
        String[] args = {"2018-08-29-10-10-10-543.", "EXP", "238465493.fileName", "1","285", "123/file.txt","GET","","2000","example","100"};
        expiryRecord = new ExpiryRecord(args);
        Assert.assertEquals("238465493.fileName", expiryRecord.getPublishId());
        Assert.assertEquals(1, expiryRecord.getFeedid());
        Assert.assertEquals("123/file.txt", expiryRecord.getRequestUri());
        Assert.assertEquals("GET", expiryRecord.getMethod());
        Assert.assertEquals("", expiryRecord.getContentType());
        Assert.assertEquals(2000, expiryRecord.getContentLength());
        Assert.assertEquals(285, expiryRecord.getSubid());
        Assert.assertEquals("file.txt", expiryRecord.getFileid());
        Assert.assertEquals(100, expiryRecord.getAttempts());
        Assert.assertEquals("other", expiryRecord.getReason());
    }

    @Test
    public void Validate_AsJsonObject_Correct_Json_Object_After_Set_Methods() throws ParseException {
        String[] args = {"2018-08-29-10-10-10-543.", "EXP", "238465493.fileName", "1","285", "123/file.txt","GET","","2000","example","100"};
        expiryRecord = new ExpiryRecord(args);
        expiryRecord.setContentLength(265);
        expiryRecord.setEventTime(1535533810543L);
        expiryRecord.setPublishId("2345657324.fileName");
        expiryRecord.setFeedid(2);
        expiryRecord.setRequestUri("/delete/2");
        expiryRecord.setContentType("application/json");
        expiryRecord.setMethod("PUT");
        expiryRecord.setSubid(322);
        expiryRecord.setFileid("file.txt");
        expiryRecord.setAttempts(125);
        expiryRecord.setReason("Out of memory");

        LOGJSONObject expiryRecordJson = createBaseLogRecordJson();
        String expiryRecordString = stripBracketFromJson(expiryRecordJson);
        String expiryRecordObject = stripBracketFromJson(expiryRecord.asJSONObject());
        Assert.assertTrue(expiryRecordObject.matches(expiryRecordString));
    }

    private LOGJSONObject createBaseLogRecordJson() {
        LOGJSONObject expiryRecordJson = new LOGJSONObject();
        expiryRecordJson.put("expiryReason", "Out of memory");
        expiryRecordJson.put("publishId", "2345657324.fileName");
        expiryRecordJson.put("attempts", 125);
        expiryRecordJson.put("requestURI", "/delete/2");
        expiryRecordJson.put("method", "PUT");
        expiryRecordJson.put("contentType", "application/json");
        expiryRecordJson.put("type", "exp");
        expiryRecordJson.put("date", "2018-08-29T10:10:10.543Z");
        expiryRecordJson.put("contentLength", 265);
        return expiryRecordJson;
    }

    private String stripBracketFromJson(LOGJSONObject expiryRecordJson) {
        String expiryRecordString = expiryRecordJson.toString();
        expiryRecordString = expiryRecordString.substring(1, expiryRecordString.length() - 1);
        return expiryRecordString;
    }
}

