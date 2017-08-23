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

import java.util.*;
import java.io.*;

/**
 *	Track redirections of subscriptions
 */
public class RedirManager	{
	private Hashtable<String, String> sid2primary = new Hashtable<String, String>();
	private Hashtable<String, String> sid2secondary = new Hashtable<String, String>();
	private String	redirfile;
	RateLimitedOperation	op;
	/**
	 *	Create a mechanism for maintaining subscription redirections.
	 *	@param redirfile	The file to store the redirection information.
	 *	@param mininterval	The minimum number of milliseconds between writes to the redirection information file.
	 *	@param timer	The timer thread used to run delayed file writes.
	 */
	public RedirManager(String redirfile, long mininterval, Timer timer) {
		this.redirfile = redirfile;
		op = new RateLimitedOperation(mininterval, timer) {
			public void run() {
				try {
					StringBuffer sb = new StringBuffer();
					for (String s: sid2primary.keySet()) {
						sb.append(s).append(' ').append(sid2primary.get(s)).append(' ').append(sid2secondary.get(s)).append('\n');
					}
					OutputStream os = new FileOutputStream(RedirManager.this.redirfile);
					os.write(sb.toString().getBytes());
					os.close();
				} catch (Exception e) {
				}
			}
		};
		try {
			String s;
			BufferedReader br = new BufferedReader(new FileReader(redirfile));
			while ((s = br.readLine()) != null) {
				s = s.trim();
				String[] sx = s.split(" ");
				if (s.startsWith("#") || sx.length != 3) {
					continue;
				}
				sid2primary.put(sx[0], sx[1]);
				sid2secondary.put(sx[0], sx[2]);
			}
			br.close();
		} catch (Exception e) {
			// missing file is normal
		}
	}
	/**
	 *	Set up redirection.  If a request is to be sent to subscription ID sid, and that is configured to go to URL primary, instead, go to secondary.
	 *	@param sid	The subscription ID to be redirected
	 *	@param primary	The URL associated with that subscription ID
	 *	@param secondary	The replacement URL to use instead
	 */
	public synchronized void redirect(String sid, String primary, String secondary) {
		sid2primary.put(sid, primary);
		sid2secondary.put(sid, secondary);
		op.request();
	}
	/**
	 *	Cancel redirection.  If a request is to be sent to subscription ID sid, send it to its primary URL.
	 *	@param	sid	The subscription ID to remove from the table.
	 */
	public synchronized void forget(String sid) {
		sid2primary.remove(sid);
		sid2secondary.remove(sid);
		op.request();
	}
	/**
	 *	Look up where to send a subscription.  If the primary has changed or there is no redirection, use the primary.  Otherwise, redirect to the secondary URL.
	 *	@param	sid	The subscription ID to look up.
	 *	@param	primary	The configured primary URL.
	 *	@return	The destination URL to really use.
	 */
	public synchronized String lookup(String sid, String primary) {
		String oprim = sid2primary.get(sid);
		if (primary.equals(oprim)) {
			return(sid2secondary.get(sid));
		} else if (oprim != null) {
			forget(sid);
		}	
		return(primary);
	}
	/**
	 *	Is a subscription redirected?
	 */
	public synchronized boolean isRedirected(String sid) {
		return(sid != null && sid2secondary.get(sid) != null);
	}
}
