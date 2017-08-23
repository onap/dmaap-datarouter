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

import java.util.List;

/**
 * The <code>AuthorizationResponse</code> interface gives the caller access to information about an authorization
 * decision.  This information includes the permit/deny decision itself, along with supplementary information in the form of
 * advice and obligations.  (The advice and obligations will not be used in Data Router R1.)
 * 
 * @author J. F. Lucas
 *
 */
public interface AuthorizationResponse {
	/**
	 * Indicates whether the request is authorized or not.
	 * 
	 * @return a boolean flag that is <code>true</code> if the request is permitted, and <code>false</code> otherwise.
	 */
	public boolean isAuthorized();
	
	/**
	 * Returns any advice elements that were included in the authorization response.
	 * 
	 * @return A list of objects implementing the <code>AuthorizationResponseSupplement</code> interface, with each object representing an
	 * advice element from the authorization response.
	 */
	public List<AuthorizationResponseSupplement> getAdvice();
	
	/**
	 * Returns any obligation elements that were included in the authorization response.
	 * 
	 * @return A list of objects implementing the <code>AuthorizationResponseSupplement</code> interface, with each object representing an
	 * obligation element from the authorization response.
	 */
	public List<AuthorizationResponseSupplement> getObligations();
}
