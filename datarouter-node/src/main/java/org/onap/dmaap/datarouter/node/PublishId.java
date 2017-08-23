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


package org.onap.dmaap.datarouter.node;

/**
 *	Generate publish IDs
 */
public class PublishId	{
	private long	nextuid;
	private String	myname;

	/**
	 *	Generate publish IDs for the specified name
	 *	@param myname	Unique identifier for this publish ID generator (usually fqdn of server)
	 */
	public PublishId(String myname) {
		this.myname = myname;
	}
	/**
	 *	Generate a Data Router Publish ID that uniquely identifies the particular invocation of the Publish API for log correlation purposes.
	 */
	public synchronized String next() {
		long now = System.currentTimeMillis();
		if (now < nextuid) {
			now = nextuid;
		}
		nextuid = now + 1;
		return(now + "." + myname);
	}
}
