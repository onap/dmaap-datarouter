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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;

public class URLUtilitiesTest {
	URLUtilities uRLUtilities = new URLUtilities();
	int feedid;

	@Test
	public void generateFeedURLTest() {
		String feedurl = uRLUtilities.generateFeedURL(feedid);
		assertNotNull(feedurl);
	}

	@Test
	public void generatePublishURLTest() {
		String puburl = uRLUtilities.generatePublishURL(feedid);
		assertNotNull(puburl);
	}

	@Test
	public void generateSubscriptionURLTest() {
		int subid = 1;
		String suburl = uRLUtilities.generateSubscriptionURL(subid);
		assertNotNull(suburl);
	}
}