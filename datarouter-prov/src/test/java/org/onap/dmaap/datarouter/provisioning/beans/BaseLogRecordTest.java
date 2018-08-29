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

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

import java.text.ParseException;

public class BaseLogRecordTest {

  private BaseLogRecord baseLogRecord;

  @Test
  public void Validate_Constructor_Creates_Object_With_Get_Methods() throws ParseException {
    String[] args = {"2018-08-29-10-10-10-543.", "DLX", "238465493.fileName",
        "1", "/publish/1/fileName", "285"};
    baseLogRecord = new BaseLogRecord(args);
    Assert.assertEquals("238465493.fileName", baseLogRecord.getPublishId());
    Assert.assertEquals(1, baseLogRecord.getFeedid());
    Assert.assertEquals("", baseLogRecord.getRequestUri());
    Assert.assertEquals("GET", baseLogRecord.getMethod());
    Assert.assertEquals("", baseLogRecord.getContentType());
    Assert.assertEquals(285, baseLogRecord.getContentLength());
  }

  @Test
  public void Validate_AsJsonObject_Correct_Json_Object_After_Set_Methods() throws ParseException {
    String[] args = {"2018-08-29-10-10-10-543.", "DEL", "238465493.fileName",
        "1", "", "/delete/1", "DELETE", "application/octet-stream", "285"};
    baseLogRecord = new BaseLogRecord(args);
    baseLogRecord.setContentLength(265);
    baseLogRecord.setEventTime(1535533810543L);
    baseLogRecord.setPublishId("2345657324.fileName");
    baseLogRecord.setFeedid(2);
    baseLogRecord.setRequestUri("/delete/2");
    baseLogRecord.setMethod("PUT");
    baseLogRecord.setContentType("application/json");
    LOGJSONObject baseLogRecordJson = createBaseLogRecordJson();
    String baseLogRecordString = stripBracketFromJson(baseLogRecordJson);
    String baseLogRecordStringObject = stripBracketFromJson(baseLogRecord.asJSONObject());
    Assert.assertTrue(baseLogRecordStringObject.matches(baseLogRecordString));
  }

  private LOGJSONObject createBaseLogRecordJson() {
    LOGJSONObject baseLogRecordJson = new LOGJSONObject();
    baseLogRecordJson.put("date", "2018-08-29T[0-1][0-9]:10:10.543Z");
    baseLogRecordJson.put("publishId", "2345657324.fileName");
    baseLogRecordJson.put("requestURI", "/delete/2");
    baseLogRecordJson.put("method", "PUT");
    baseLogRecordJson.put("contentType", "application/json");
    baseLogRecordJson.put("contentLength", 265);
    return baseLogRecordJson;
  }

  private String stripBracketFromJson(LOGJSONObject baseLogRecordJson) {
    String baseLogRecordString = baseLogRecordJson.toString();
    baseLogRecordString = baseLogRecordString.substring(1, baseLogRecordString.length() - 1);
    return baseLogRecordString;
  }
}
