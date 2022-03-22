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
package org.onap.dmaap.datarouter.provisioning.utils;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AafPropsUtilsTest {

    private static AafPropsUtils aafPropsUtils;

    @BeforeClass
    public static void init() throws Exception {
        aafPropsUtils = new AafPropsUtils(new File("src/test/resources/aaf/org.onap.dmaap-dr.props"));
    }

    @Test
    public void Assert_AaafPropsUtils_Decrypt_KeyStorePass() {
        Assert.assertEquals("b.Qo{*!a(a;jmeOfKbA&vHx#", aafPropsUtils.getKeystorePassProperty());
    }

    @Test
    public void Assert_AaafPropsUtils_Decrypt_TruststorePass() {
        Assert.assertEquals("YeXhcTw6%G[od1a43+P!yhoN", aafPropsUtils.getTruststorePassProperty());
    }

    @Test
    public void Assert_AaafPropsUtils_Get_KeyStorePathProp() {
        Assert.assertEquals("src/test/resources/aaf/org.onap.dmaap-dr.p12", aafPropsUtils.getKeystorePathProperty());
    }

    @Test
    public void Assert_AaafPropsUtils_Get_TrustStorePathProp() {
        Assert.assertEquals("src/test/resources/aaf/org.onap.dmaap-dr.trust.jks", aafPropsUtils.getTruststorePathProperty());
    }

    @Test
    public void Assert_AaafPropsUtils_Get_PropAccessObj() {
        Assert.assertNotNull(aafPropsUtils.getPropAccess());
    }

}
