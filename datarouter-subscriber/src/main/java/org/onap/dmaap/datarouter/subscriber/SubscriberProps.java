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

package org.onap.dmaap.datarouter.subscriber;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

class SubscriberProps {

    private static SubscriberProps instance = null;
    private static Logger subLogger = Logger.getLogger("org.onap.dmaap.datarouter.subscriber.internal");
    private Properties properties;

    private SubscriberProps(String propsPath) throws IOException {
        properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(propsPath));

    }

    static SubscriberProps getInstance(String propsPath) {
        if (instance == null) {
            try {
                instance = new SubscriberProps(propsPath);
            } catch (IOException ioe) {
                subLogger.error("IO Exception: " + ioe.getMessage());
                ioe.printStackTrace();
            }
        }
        return instance;
    }

    static SubscriberProps getInstance() {
        return instance;
    }

    String getValue(String key) {
        return properties.getProperty(key);
    }

    String getValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}