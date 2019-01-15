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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrServletTestBase {

    static File file = new File("logs/EELF/application.log");


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

    public ListAppender<ILoggingEvent> set_Test_Logger(Class c) {
        Logger fooLogger = (Logger) LoggerFactory.getLogger(c);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);
        return listAppender;
    }

    public void verify_Entering_Exit_Called(ListAppender<ILoggingEvent> listAppender) {
        assertEquals("EELF0004I  Entering", listAppender.list.get(0).getMessage());
        assertEquals("EELF0005I  Exiting", listAppender.list.get(2).getMessage());
        assertEquals(3, listAppender.list.size());
    }

    @After
    public void tearDown() throws Exception {
    }

    @AfterClass
    public static void clearFile() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(file);
        pw.close();
    }
}
