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


package org.onap.dmaap.datarouter.provisioning.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Some utility functions used when creating/validating JSON.
 *
 * @author Robert Eby
 * @version $Id: JSONUtilities.java,v 1.1 2013/04/26 21:00:26 eby Exp $
 */
public class JSONUtilities {
	/**
	 * Does the String <i>v</i> represent a valid Internet address (with or without a
	 * mask length appended).
	 * @param v the string to check
	 * @return true if valid, false otherwise
	 */
	public static boolean validIPAddrOrSubnet(String v) {
		String[] pp = { v, "" };
		if (v.indexOf('/') > 0)
			pp = v.split("/");
		try {
			InetAddress addr = InetAddress.getByName(pp[0]);
			if (pp[1].length() > 0) {
				// check subnet mask
				int mask = Integer.parseInt(pp[1]);
				if (mask > (addr.getAddress().length * 8))
					return false;
			}
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}
	/**
	 * Build a JSON array from a collection of Strings.
	 * @param coll the collection
	 * @return a String containing a JSON array
	 */
	public static String createJSONArray(Collection<String> coll) {
		StringBuilder sb = new StringBuilder("[");
		String pfx = "\n";
		for (String t : coll) {
			sb.append(pfx).append("  \"").append(t).append("\"");
			pfx = ",\n";
		}
		sb.append("\n]\n");
		return sb.toString();
	}
}
