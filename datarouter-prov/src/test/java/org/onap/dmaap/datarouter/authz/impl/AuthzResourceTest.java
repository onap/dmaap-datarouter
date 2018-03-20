/*******************************************************************************
 * ============LICENSE_START==================================================
 * * ONAP:DMAAP
 * * ===========================================================================
 * *  Copyright 2018 TechMahindra
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

import org.junit.Test;
import org.onap.dmaap.datarouter.authz.impl.AuthzResource;
import org.onap.dmaap.datarouter.authz.impl.AuthzResource.ResourceType;

import junit.framework.Assert;

public class AuthzResourceTest {

	AuthzResource authzResource = new AuthzResource("test");

	@Test
	public void getTypeTest() {
		ResourceType resourceType = authzResource.getType();
		Assert.assertNull(resourceType);
	}

	@Test
	public void getIdTest() {
		String ID = authzResource.getId();
		Assert.assertNotNull(ID);
	}

}
