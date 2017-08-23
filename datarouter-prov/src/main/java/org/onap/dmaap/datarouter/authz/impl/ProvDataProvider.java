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
package org.onap.dmaap.datarouter.authz.impl;

/** Interface to access data about subscriptions and feeds.  A software component that 
 * uses the <code>ProvAuthorizer</code> needs to supply an implementation of this interface.
 * @author J. F. Lucas
 *
 */
public interface ProvDataProvider {
	
	/** Get the identity of the owner of a feed.
	 * 
	 * @param feedId the feed ID of the feed whose owner is being looked up.
	 * @return the feed owner's identity
	 */
	public String getFeedOwner(String feedId);
	
	/** Get the security classification of a feed.
	 * 
	 * @param feedId the ID of the feed whose classification is being looked up.
	 * @return the classification of the feed.
	 */
	public String getFeedClassification(String feedId);
	
	/** Get the identity of the owner of a feed
	 * 
	 * @param subId the ID of the subscripition whose owner is being looked up.
	 * @return the subscription owner's identity.
	 */
	public String getSubscriptionOwner(String subId);

	/** Get the identity of the owner of a feed by group id -  Rally : US708115
	 * 
	 * @param feedid, user the ID of the feed whose owner is being looked up.
	 * @return the feed owner's identity by group.
	 */
	public String getGroupByFeedGroupId(String owner, String feedId);
	
	/** Get the identity of the owner of a sub by group id Rally : US708115
	 * 
	 * @param subid, user the ID of the feed whose owner is being looked up.
	 * @return the feed owner's identity by group.
	 */
	public String getGroupBySubGroupId(String owner, String subId);
}
