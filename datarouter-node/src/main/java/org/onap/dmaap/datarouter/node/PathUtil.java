/**
 * -
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.dmaap.datarouter.node;

/**
 * FORTIFY SCAN FIXES
 * <p>This Utility is used for Fortify fixes. It Validates the path url formed from
 *  the string passed in the request parameters.</p>
 *
 */
class PathUtil {

    /**
     * This method takes String as the parameter and return the filtered path string.
     * @param aString String to clean
     * @return A cleaned String
     */
    static String cleanString(String aString) {
        if (aString == null) return null;
        String cleanString = "";
        for (int i = 0; i < aString.length(); ++i) {
            cleanString += cleanChar(aString.charAt(i));
        }
        return cleanString;
    }

    /**
     * This method filters the valid special characters in path string.
     * @param aChar The char to be cleaned
     * @return The cleaned char
     */
    private static char cleanChar(char aChar) {
        // 0 - 9
        for (int i = 48; i < 58; ++i) {
            if (aChar == i) return (char) i;
        }
        // 'A' - 'Z'
        for (int i = 65; i < 91; ++i) {
            if (aChar == i) return (char) i;
        }
        // 'a' - 'z'
        for (int i = 97; i < 123; ++i) {
            if (aChar == i) return (char) i;
        }
        // other valid characters
        switch (aChar) {
            case '/':
                return '/';
            case '.':
                return '.';
            case '-':
                return '-';
            case ':':
                return ':';
            case '?':
                return '?';
            case '&':
                return '&';
            case '=':
                return '=';
            case '#':
                return '#';
            case '_':
                return '_';
            case ' ':
                return ' ';
        }
        return '%';
    }
}
