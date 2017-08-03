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
package com.att.research.datarouter.provisioning.eelf;

import com.att.eelf.i18n.EELFResolvableErrorEnum;
import com.att.eelf.i18n.EELFResourceManager;

public enum EelfMsgs implements EELFResolvableErrorEnum {
	
	/**
     * Application message prints user (accepts one argument)
     */
	MESSAGE_WITH_BEHALF,

	/**
     * Application message prints user and FeedID (accepts two arguments)
     */

	MESSAGE_WITH_BEHALF_AND_FEEDID,

	/**
     * Application message prints user and SUBID (accepts two arguments)
     */

	MESSAGE_WITH_BEHALF_AND_SUBID;

		
    
    /**
     * Static initializer to ensure the resource bundles for this class are loaded...
     * Here this application loads messages from three bundles
     */
    static {
        EELFResourceManager.loadMessageBundle("EelfMessages");
    }
}
