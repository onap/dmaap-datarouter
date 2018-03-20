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

package org.onap.dmaap.datarouter.provisioning.beans;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SubDeliveryTest {
	SubDelivery subDelivery = new SubDelivery();

	@Test
	public void getUrl() {
		String url = subDelivery.getUrl();
		assertNotNull(url);
	}

	@Test
	public void getpasswordTest() {
		String pwd = subDelivery.getPassword();
		assertNotNull(pwd);
	}

	@Test
	public void getUserTest() {
		String user = subDelivery.getUser();
		assertNotNull(user);
	}

}
