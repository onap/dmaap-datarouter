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

package org.onap.dmaap.datarouter.provisioning.eelf;

import static org.junit.Assert.assertEquals;
import static org.onap.dmaap.datarouter.provisioning.eelf.EelfMsgs.MESSAGE_WITH_BEHALF;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.Test;



public class LogbackFilterTest {

    @Test
    public void Given_Event_with_invalid_logger_name_and_debug_level_then_debugtracefilter_DENY(){
        final DebugTraceFilter filter = new DebugTraceFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("InvalidLogger");
        event.setLevel(Level.DEBUG);
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_valid_logger_name_and_info_level_then_debugtracefilter_DENY(){
        final DebugTraceFilter filter = new DebugTraceFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("InternalLog");
        event.setLevel(Level.INFO);
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_valid_logger_name_and_debug_level_then_debugtracefilter_ACCEPT(){
        final DebugTraceFilter filter = new DebugTraceFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("InternalLog");
        event.setLevel(Level.DEBUG);
        assertEquals(FilterReply.ACCEPT, filter.decide(event));

    }

    @Test
    public void Given_Event_with_valid_logger_name_and_trace_level_then_debugtracefilter_ACCEPT(){
        final DebugTraceFilter filter = new DebugTraceFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("InternalLog");
        event.setLevel(Level.TRACE);
        assertEquals(FilterReply.ACCEPT, filter.decide(event));

    }

    @Test
    public void Given_Event_with_valid_jetty_string_then_jettyfilter_ACCEPT(){
        final JettyFilter filter = new JettyFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("org.eclipse.jetty");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));
    }

    @Test
    public void Given_Event_with_invalid_jetty_string_then_jettyfilter_DENY(){
        final JettyFilter filter = new JettyFilter();
        filter.start();
        final LoggingEvent event = new LoggingEvent();
        event.setLoggerName("org.invalid.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));
    }

    @Test
    public void Given_Call_to_EelfMsgs_return_the_correct_enum(){
        assertEquals(MESSAGE_WITH_BEHALF.toString(), "MESSAGE_WITH_BEHALF");
    }
}
