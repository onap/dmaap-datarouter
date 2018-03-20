/*******************************************************************************
 * ============LICENSE_START==================================================
 * * ONAP : DMAAP
 * * ===========================================================================
 * *Copyright 2018 TechMahindra
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * * 
 *  *       http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.onap.dmaap.datarouter.authz.AuthorizationResponseSupplement;

public class AuthRespImplTEST {

	private List<AuthorizationResponseSupplement> advice = new ArrayList<AuthorizationResponseSupplement>();
	private List<AuthorizationResponseSupplement> obligations = new ArrayList<AuthorizationResponseSupplement>();
	AuthRespImpl authRespImpl2 = new AuthRespImpl(true, advice, obligations);

	@Test
	public void isAuthorizedTest() {
		AuthRespImpl authRespImpl = new AuthRespImpl(false);
		boolean flag = authRespImpl.isAuthorized();
		Assert.assertNotNull(flag);
	}

	@Test
	public void getAdviceTest() {
		List<AuthorizationResponseSupplement> list;
		list = authRespImpl2.getAdvice();
		Assert.assertNotNull(list);
	}

	@Test
	public void getobligationsTest() {
		List<AuthorizationResponseSupplement> list;
		list = authRespImpl2.getObligations();
		Assert.assertNotNull(list);
	}

}
