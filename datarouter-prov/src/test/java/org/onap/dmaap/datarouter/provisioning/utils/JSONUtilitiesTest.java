/*******************************************************************************
 * ============LICENSE_START==================================================
 * * ONAP:DMAAP
 * * ===========================================================================
 * * Copyright 2018 TechMahindra
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
//import org.mockito.Mock;

public class JSONUtilitiesTest {
	JSONUtilities jutils = null;

	@Before
	public void setUp() throws Exception {
		jutils = new JSONUtilities();
	}

	@Test
	public void testValidIPAddrOrSubnet() {
		String v = "test";
		Boolean flag = JSONUtilities.validIPAddrOrSubnet(v);
		assertTrue(flag);

	}

	@Test
	public void testCreateJSONArray() {
		Collection<String> coll = new ArrayList<String>();
		coll.add("test");
		coll.add("test1");
		JSONUtilities.createJSONArray(coll);

	}

}
