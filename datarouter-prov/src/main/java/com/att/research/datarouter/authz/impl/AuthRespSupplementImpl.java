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


package com.att.research.datarouter.authz.impl;

import java.util.HashMap;
import java.util.Map;

import com.att.research.datarouter.authz.AuthorizationResponseSupplement;

/** Carries supplementary information--an advice or an obligation--from the authorization response returned
 *  by a XACML Policy Decision Point.   Not used in Data Router R1.
 * @author J. F. Lucas
 *
 */
public class AuthRespSupplementImpl implements AuthorizationResponseSupplement {
	
	private String id = null;
	private Map<String, String> attributes = null;

	/** Constructor, available within the package.
	 * 
	 * @param id  The identifier for the advice or obligation element
	 * @param attributes The attributes (name-value pairs) for the advice or obligation element.
	 */
	AuthRespSupplementImpl (String id, Map<String, String> attributes) {
		this.id = id;
		this.attributes = new HashMap<String,String>(attributes);
	}

	/** Return the identifier for the supplementary information element.
	 * 
	 * @return a <code>String</code> containing the identifier.
	 */
	@Override
	public String getId() {
		return id;
	}

	/** Return the attributes for the supplementary information element, as a <code>Map</code> in which
	 * keys represent attribute identifiers and values represent attribute values.
	 * 
	 * @return attributes for the supplementary information element.
	 */
	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

}
