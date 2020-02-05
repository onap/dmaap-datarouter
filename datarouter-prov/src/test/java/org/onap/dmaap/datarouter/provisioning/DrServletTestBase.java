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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Field;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.onap.dmaap.datarouter.provisioning.utils.SynchronizerTask;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
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
        FieldUtils.writeDeclaredStaticField(ProvRunner.class, "provProperties", props, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "startmsgFlag", false, true);
        SynchronizerTask synchronizerTask = mock(SynchronizerTask.class);
        when(synchronizerTask.getPodState()).thenReturn(SynchronizerTask.UNKNOWN_POD);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "synctask", synchronizerTask, true);
    }

    ListAppender<ILoggingEvent> setTestLogger(Class c) {
        Logger logger = (Logger) LoggerFactory.getLogger(c);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    void verifyEnteringExitCalled(ListAppender<ILoggingEvent> listAppender) {
        assertEquals("EELF0004I  Entering data router provisioning component with RequestId and InvocationId", listAppender.list.get(0).getMessage());
        assertEquals("EELF0005I  Exiting data router provisioning component with RequestId and InvocationId", listAppender.list.get(2).getMessage());
        assertEquals(3, listAppender.list.size());
    }

    @After
    public void tearDown() throws Exception {
    }
}
