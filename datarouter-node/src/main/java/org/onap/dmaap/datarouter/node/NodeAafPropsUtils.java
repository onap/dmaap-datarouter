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

package org.onap.dmaap.datarouter.node;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.onap.aaf.cadi.PropAccess;

class NodeAafPropsUtils {

    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeAafPropsUtils.class);
    private PropAccess propAccess;

    NodeAafPropsUtils(File propsFile) throws IOException {
        propAccess = new PropAccess();
        try {
            propAccess.load(new FileInputStream(propsFile.getPath()));
        } catch (IOException e) {
            eelfLogger.error("Failed to load props file: " + propsFile + "\n" + e.getMessage(), e);
            throw e;
        }
    }

    String getDecryptedPass(String password) {
        String decryptedPass = "";
        try {
            decryptedPass = getPropAccess().decrypt(getPropAccess().getProperty(password), false);
        } catch (IOException e) {
            eelfLogger.error("Failed to decrypt " + password + " : " + e.getMessage(), e);
        }
        return decryptedPass;
    }

    PropAccess getPropAccess() {
        return propAccess;
    }
}
