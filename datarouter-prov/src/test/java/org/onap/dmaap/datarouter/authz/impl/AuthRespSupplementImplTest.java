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

package org.onap.dmaap.datarouter.authz.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AuthRespSupplementImplTest {

	private Map<String, String> supl = new HashMap<>();
	AuthRespSupplementImpl authRespSupplementImpl = new AuthRespSupplementImpl("hello", supl);

	@Test
	public void getIdTest() {
		String ID = authRespSupplementImpl.getId();
		Assert.assertNotNull(ID);
	}

	@Test
	public void getAttributesTest() {
		Map<String, String> ID = authRespSupplementImpl.getAttributes();
		Assert.assertNotNull(ID);
	}

}
