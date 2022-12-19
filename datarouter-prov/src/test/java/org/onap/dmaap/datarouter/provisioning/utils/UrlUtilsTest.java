/*-
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

package org.onap.dmaap.datarouter.provisioning.utils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class UrlUtilsTest {

    @BeforeClass
    public static void init() {
        System.setProperty(
            "org.onap.dmaap.datarouter.provserver.properties",
            "src/test/resources/h2Database.properties");
    }

    @Before
    public void setUp() throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "initialActivePod", "mypod1", true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "initialStandbyPod", "mypod2", true);
        FieldUtils.writeDeclaredStaticField(ProvRunner.class, "tlsEnabled", false, true);
    }

    @Test
    public void Verify_UrlUtils_generatePeerProvURL_Returns_Valid_Http_Url() {
        Assert.assertEquals(URLUtilities.generatePeerProvURL(), "http://mypod2:8080/internal/prov");
    }

    @Test
    public void Verify_UrlUtils_generatePeerLogsURL_Returns_Valid_Http_Url() {
        Assert.assertEquals(URLUtilities.generatePeerLogsURL(), "http://mypod2:8080/internal/drlogs/");
    }

}
