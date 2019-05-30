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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DestInfoTest {

    private DestInfo destInfo;

    @Before
    public void setUp() {
        destInfo = getDestInfo("/src/test/resources");
    }

    @Test
    public void Validate_Getters_And_Setters() {
        assertEquals("n:dmaap-dr-node", destInfo.getName());
        assertEquals("/src/test/resources", destInfo.getSpool());
        assertEquals("1", destInfo.getSubId());
        assertEquals("n2n-dmaap-dr-node", destInfo.getLogData());
        assertEquals("https://dmaap-dr-node:8443/internal/publish", destInfo.getURL());
        assertEquals("dmaap-dr-node", destInfo.getAuthUser());
        assertEquals("Auth", destInfo.getAuth());
        assertFalse(destInfo.isMetaDataOnly());
        assertTrue(destInfo.isUsing100());
        assertFalse(destInfo.isPrivilegedSubscriber());
        assertFalse(destInfo.isFollowRedirects());
        assertFalse(destInfo.isDecompress());
    }

    @Test
    public void Validate_DestInfo_Objects_Are_Equal() {
        DestInfo destInfo2 = getDestInfo("/src/test/resources");
        assertEquals(destInfo, destInfo2);
        assertEquals(destInfo.hashCode(), destInfo2.hashCode());
    }

    @Test
    public void Validate_DestInfo_Objects_Are_Not_Equal() {
        DestInfo destInfo2 = getDestInfo("notEqual");
        assertNotEquals(destInfo, destInfo2);
        assertNotEquals(destInfo.hashCode(), destInfo2.hashCode());
    }

    private DestInfo getDestInfo(String spool) {
        return new DestInfoBuilder().setName("n:" + "dmaap-dr-node").setSpool(spool)
                .setSubid("1").setLogdata("n2n-dmaap-dr-node").setUrl("https://dmaap-dr-node:8443/internal/publish")
                .setAuthuser("dmaap-dr-node").setAuthentication("Auth").setMetaonly(false).setUse100(true)
                .setPrivilegedSubscriber(false).setFollowRedirects(false).setDecompress(false).createDestInfo();
    }

}
