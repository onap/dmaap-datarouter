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

package org.onap.dmaap.datarouter.provisioning;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.AafPropsUtils;

public class ProvServerTest {

    private AafPropsUtils aafPropsUtils;

    @Before
    public void setUp() {
        try {
            aafPropsUtils = new AafPropsUtils(new File("src/test/resources/aaf/org.onap.dmaap-dr.props"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void init() {
        System.setProperty(
            "org.onap.dmaap.datarouter.provserver.properties",
            "src/test/resources/h2Database.properties");
    }

    @Test
    public void Verify_Prov_Server_Is_Configured_Correctly() throws IllegalAccessException {
        FieldUtils.writeDeclaredStaticField(ProvRunner.class, "aafPropsUtils", aafPropsUtils, true);
        Assert.assertNotNull(ProvServer.getServerInstance());
    }
}
