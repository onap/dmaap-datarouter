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

package org.onap.dmaap.datarouter.node;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.onap.dmaap.datarouter.node.eelf.EelfMsgs.MESSAGE_WITH_BEHALF;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import java.util.List;
import org.junit.Test;
import org.onap.dmaap.datarouter.node.eelf.AuditFilter;
import org.onap.dmaap.datarouter.node.eelf.DebugFilter;
import org.onap.dmaap.datarouter.node.eelf.ErrorFilter;
import org.onap.dmaap.datarouter.node.eelf.JettyFilter;
import org.onap.dmaap.datarouter.node.eelf.MetricsFilter;

public class LogbackFilterTest {

    @Test
    public void Given_Event_with_valid_status_then_audit_Filter_ACCEPT()  {
        final List<String> validStatus = asList("DEL|", "PUB|", "PBF|", "EXP|", "DLX|");
        final AuditFilter filter = new AuditFilter();
        filter.start();

        for (final String status : validStatus) {
            final ILoggingEvent event= new LoggingEvent();
            ((LoggingEvent) event).setMessage("Test " + status);
            assertEquals(FilterReply.ACCEPT, filter.decide(event));
        }
    }

    @Test
    public void Given_Event_with_invalid_status_then_audit_Filter_DENY()  {
        final AuditFilter filter = new AuditFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setMessage("Invalid status");
        assertEquals(FilterReply.DENY, filter.decide(event));
    }

    @Test
    public void Given_Event_with_valid_jetty_string_then_jetty_Filter_ACCEPT()  {
        final JettyFilter filter = new JettyFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));
    }

    @Test
    public void Given_Event_with_invalid_jetty_string_then_jetty_Filter_DENY()  {
        final JettyFilter filter = new JettyFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));
    }

    @Test
    public void Given_Event_with_level_info_and_valid_jetty_string_then_metrics_filter_DENY()  {
        final MetricsFilter filter = new MetricsFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_info_and_valid_jetty_string_then_metrics_filter_DENY()  {
        final MetricsFilter filter = new MetricsFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.DEBUG);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_info_and_invalid_jetty_string_then_metrics_filter_DENY() {
        final MetricsFilter filter = new MetricsFilter();
        filter.start();
        final ILoggingEvent event = new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.DEBUG);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));
    }

    @Test
    public void Given_Event_with_level_info_and_invalid_jetty_string_and_status_in_status_list_then_metrics_filter_DENY() {
        final List<String> validStatus = asList("DEL|", "PUB|", "PBF|", "EXP|", "DLX|");
        final MetricsFilter filter = new MetricsFilter();
        filter.start();

        for (final String status : validStatus) {
            final ILoggingEvent event= new LoggingEvent();
            ((LoggingEvent) event).setLevel(Level.INFO);
            ((LoggingEvent) event).setMessage(status);
            assertEquals(FilterReply.DENY, filter.decide(event));
        }
    }

    @Test
    public void Given_Event_with_level_info_and_invalid_jetty_string_and_status_not_in_status_list_then_metrics_filter_ACCEPT() {
        final MetricsFilter filter = new MetricsFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("Invalid status");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));
    }

    @Test
    public void Given_Event_with_level_debug_and_valid_jetty_string_then_debug_filter_DENY()  {
        final DebugFilter filter = new DebugFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.DEBUG);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_debug_and_valid_jetty_string_then_debug_filter_DENY()  {
        final DebugFilter filter = new DebugFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_debug_and_not_valid_jetty_string_then_debug_filter_DENY()  {
        final DebugFilter filter = new DebugFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_debug_and_not_valid_jetty_string_then_debug_filter_ACCEPT()  {
        final DebugFilter filter = new DebugFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.DEBUG);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_error_and_valid_jetty_string_then_error_filter_DENY()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.ERROR);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_warn_and_valid_jetty_string_then_error_filter_DENY()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.WARN);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_warn_or_error_and_valid_jetty_string_then_error_filter_DENY()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("org.eclipse.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_not_warn_or_error_and_invalid_jetty_string_then_error_filter_DENY()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.INFO);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.DENY, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_warn_and_invalid_jetty_string_then_error_filter_ACCEPT()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.WARN);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));

    }

    @Test
    public void Given_Event_with_level_error_and_invalid_jetty_string_then_error_filter_ACCEPT()  {
        final ErrorFilter filter = new ErrorFilter();
        filter.start();
        final ILoggingEvent event= new LoggingEvent();
        ((LoggingEvent) event).setLevel(Level.ERROR);
        ((LoggingEvent) event).setMessage("org.invalid.jetty");
        assertEquals(FilterReply.ACCEPT, filter.decide(event));

    }


    @Test
    public void Given_call_to_EelfMsgs_return_the_correct_enum(){
        assertEquals(MESSAGE_WITH_BEHALF.toString(), "MESSAGE_WITH_BEHALF");
    }

}
