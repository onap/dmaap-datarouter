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


package com.att.research.datarouter.authz;

import java.util.Map;

/** An object that meets the <code>AuthorizationResponseSupplement</code> interface carries supplementary
 * information for an authorization response.  In a XACML-based system, a response to an authorization request
 * carries not just the permit/deny decision but, optionally, supplemental information in the form of advice and
 * obligation elements.  The structure of a XACML advice element and a XACML obligation element are similar: each has an identifier and
 * a set of attributes (name-value) pairs.  (The difference between a XACML advice element and a XACML obligation element is in
 * how the recipient of the response--the Policy Enforcement Point, in XACML terminology--handles the element.)
 * 
 * @author J. F. Lucas
 *
 */
public interface AuthorizationResponseSupplement {
	/** Return the identifier for the supplementary information element.
	 * 
	 * @return a <code>String</code> containing the identifier.
	 */
	public String getId();
	
	/** Return the attributes for the supplementary information element, as a <code>Map</code> in which
	 * keys represent attribute identifiers and values represent attribute values.
	 * 
	 * @return attributes for the supplementary information element.
	 */
	public Map<String, String> getAttributes();
}
