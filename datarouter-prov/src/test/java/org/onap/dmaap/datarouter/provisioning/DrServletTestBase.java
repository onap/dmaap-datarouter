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

package org.onap.dmaap.datarouter.provisioning;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrServletTestBase {

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("org.onap.dmaap.datarouter.provserver.isaddressauthenabled", "false");
        props.setProperty("org.onap.dmaap.datarouter.provserver.accesslog.dir", "unit-test-logs");
        props.setProperty("org.onap.dmaap.datarouter.provserver.spooldir", "unit-test-logs/spool");
        props.setProperty("org.onap.dmaap.datarouter.provserver.https.relaxation", "false");
        FieldUtils.writeDeclaredStaticField(DB.class, "props", props, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "startmsgFlag", false, true);
        SynchronizerTask synchronizerTask = mock(SynchronizerTask.class);
        when(synchronizerTask.getState()).thenReturn(SynchronizerTask.UNKNOWN);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "synctask", synchronizerTask, true);
    }

    @After
    public void tearDown() throws Exception {

    }
}
