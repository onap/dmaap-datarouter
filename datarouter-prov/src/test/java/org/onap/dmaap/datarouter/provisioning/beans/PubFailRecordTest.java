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
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.ParseException;

@RunWith(PowerMockRunner.class)
public class PubFailRecordTest {

    private PubFailRecord pubFailRecord;


    @Test
    public void Validate_PubFailRecord_Created_With_Default_Constructor() throws ParseException {
        String[] args = {"2018-08-29-10-10-10-543.", "PBF", "238465493.fileName",
                "1", "/publish/1/fileName", "PUT", "application/octet-stream", "285", "200",
                "172.100.0.3", "user1", "403"};
        pubFailRecord = new PubFailRecord(args);

        Assert.assertEquals("user1", pubFailRecord.getUser());
        Assert.assertEquals("172.100.0.3", pubFailRecord.getSourceIP());
        Assert.assertEquals("403", pubFailRecord.getError());
        Assert.assertEquals(200, pubFailRecord.getContentLengthReceived());
    }
}
