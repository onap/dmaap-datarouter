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

package org.onap.dmaap.datarouter.provisioning.utils;

import com.att.eelf.configuration.EELFLogger;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class HttpServletUtils {

    private HttpServletUtils(){

    }

    /**
     * Send response error.
     * @param response HttpServletResponse
     * @param errorCode errorcode int
     * @param message String message
     * @param intlogger Logger
     */
    public static void sendResponseError(HttpServletResponse response,
            int errorCode, String message, EELFLogger intlogger) {
        try {
            response.sendError(errorCode, message);
        } catch (IOException ioe) {
            intlogger.error("IOException" + ioe.getMessage(), ioe);
        }
    }
}
