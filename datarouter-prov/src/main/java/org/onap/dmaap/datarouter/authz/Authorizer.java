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


package org.onap.dmaap.datarouter.authz;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * A Data Router API that requires authorization of incoming requests creates an instance of a class that implements
 * the <code>Authorizer</code> interface.   The class implements all of the logic necessary to determine if an API
 * request is permitted.  In Data Router R1, the classes that implement the <code>Authorizer</code> interface will have
 * local logic that makes the authorization decision.  After R1, these classes will instead have logic that creates XACML
 * authorization requests, sends these requests to a Policy Decision Point (PDP), and parses the XACML responses.
 * 
 * @author J. F. Lucas
 *
 */
public interface Authorizer {
	/**
	 * Determine if the API request carried in the <code>request</code> parameter is permitted.
	 * 
	 * @param request the HTTP request for which an authorization decision is needed
	 * @return an object implementing the <code>AuthorizationResponse</code> interface.  This object includes the
	 * permit/deny decision for the request and (after R1) supplemental information related to the response in the form
	 * of advice and obligations.
	 */
	public AuthorizationResponse decide(HttpServletRequest request);
	
	/**
	 * Determine if the API request carried in the <code>request</code> parameter, with additional attributes provided in
	 * the <code>additionalAttrs</code> parameter, is permitted.
	 * 
	 * @param request the HTTP request for which an authorization decision is needed
	 * @param additionalAttrs additional attributes that the <code>Authorizer</code> can in making an authorization decision
	 * @return an object implementing the <code>AuthorizationResponse</code> interface.  This object includes the
	 * permit/deny decision for the request and (after R1) supplemental information related to the response in the form
	 * of advice and obligations.
	 */
	public AuthorizationResponse decide(HttpServletRequest request, Map<String,String> additionalAttrs);
}
