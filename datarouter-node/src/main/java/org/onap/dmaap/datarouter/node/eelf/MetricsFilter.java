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

package org.onap.dmaap.datarouter.node.eelf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;


public class MetricsFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().equals(Level.INFO) && !event.getMessage().contains("org.eclipse.jetty")
                    && !event.getLoggerName().contains("org.eclipse.jetty")) {
            if (!event.getMessage().contains("DEL|") && !event.getMessage().contains("PUB|")
                        && !event.getMessage().contains(
                    "PBF|") && !event.getMessage().contains("EXP|") && !event.getMessage().contains("DLX|")) {
                return FilterReply.ACCEPT;
            }
        }
        return FilterReply.DENY;
    }
}
