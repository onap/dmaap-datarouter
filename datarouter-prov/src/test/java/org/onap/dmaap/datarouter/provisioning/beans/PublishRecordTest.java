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
import org.junit.Before;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

import java.text.ParseException;
import java.util.LinkedHashMap;

public class PublishRecordTest {

  private PublishRecord publishRecord;

  @Before
  public void setUp() throws ParseException {
    String[] args = {"2018-08-29-10-10-10-543.", "PUB", "238465493.fileName",
        "1", "/publish/1/fileName", "PUT", "application/octet-stream", "285",
        "172.100.0.3", "user1", "301"};
    publishRecord = new PublishRecord(args);
  }

  @Test
  public void Validate_Contructor_Creates_Object_With_Get_Methods() {
    Assert.assertEquals("fileName", publishRecord.getFeedFileid());
    Assert.assertEquals("172.100.0.3", publishRecord.getRemoteAddr());
    Assert.assertEquals("user1", publishRecord.getUser());
    Assert.assertEquals(301, publishRecord.getStatus());
  }

  @Test
  public void Validate_AsJsonObject_Correct_Json_Object_After_Set_Methods() {
    publishRecord.setFeedFileid("fileName2");
    publishRecord.setRemoteAddr("172.100.0.4");
    publishRecord.setStatus(201);
    publishRecord.setUser("user2");
    publishRecord.setFileName("fileName");
    LOGJSONObject publishRecordJson = createPublishRecordJson();
    Assert.assertEquals(publishRecordJson.toString(), publishRecord.asJSONObject().toString());
  }

  private LOGJSONObject createPublishRecordJson() {
    LinkedHashMap<String, Object> publishRecordMap = new LinkedHashMap<>();
    publishRecordMap.put("statusCode", 201);
    publishRecordMap.put("publishId", "238465493.fileName");
    publishRecordMap.put("requestURI", "/publish/1/fileName");
    publishRecordMap.put("sourceIP", "172.100.0.4");
    publishRecordMap.put("method", "PUT");
    publishRecordMap.put("contentType", "application/octet-stream");
    publishRecordMap.put("endpointId", "user2");
    publishRecordMap.put("type", "pub");
    publishRecordMap.put("date", "2018-08-29T10:10:10.543Z");
    publishRecordMap.put("contentLength", 285);
    publishRecordMap.put("fileName", "fileName");
    return  new LOGJSONObject(publishRecordMap);
  }
}
