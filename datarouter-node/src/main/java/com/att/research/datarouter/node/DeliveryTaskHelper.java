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


package com.att.research.datarouter.node;

/**
 *	Interface to allow independent testing of the DeliveryTask code.
 *	<p>
 *	This interface represents all the configuraiton information and
 *	feedback mechanisms that a delivery task needs.
 */

public interface DeliveryTaskHelper	{
	/**
	 *	Report that a delivery attempt failed due to an exception (like can't connect to remote host)
	 *	@param task	The task that failed
	 *	@param exception	The exception that occurred
	 */
	public void reportException(DeliveryTask task, Exception exception);
	/**
	 *	Report that a delivery attempt completed (successfully or unsuccessfully)
	 *	@param task	The task that failed
	 *	@param status	The HTTP status
	 *	@param xpubid	The publish ID from the far end (if any)
	 *	@param location	The redirection location for a 3XX response
	 */
	public void reportStatus(DeliveryTask task, int status, String xpubid, String location);
	/**
	 *	Report that a delivery attempt either failed while sending data or that an error was returned instead of a 100 Continue.
	 *	@param task	The task that failed
	 *	@param sent	The number of bytes sent or -1 if an error was returned instead of 100 Continue.
	 */
	public void reportDeliveryExtra(DeliveryTask task, long sent);
	/**
	 *	Get the destination information for the delivery queue
	 *	@return	The destination information
	 */
	public DestInfo getDestInfo();
	/**
	 *	Given a file ID, get the URL to deliver to
	 *	@param fileid	The file id
	 *	@return	The URL to deliver to
	 */
	public String	getDestURL(String fileid);
	/**
	 *	Get the feed ID for a subscription
	 *	@param subid	The subscription ID
	 *	@return	The feed iD
	 */
	public String	getFeedId(String subid);
}
