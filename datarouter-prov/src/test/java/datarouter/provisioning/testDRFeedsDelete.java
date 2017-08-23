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

package datarouter.provisioning;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class testDRFeedsDelete extends testBase {
	@Test
	public void testNotAllowed() {
		String url = props.getProperty("test.host") + "/";
		HttpDelete del = new HttpDelete(url);
		try {
			del.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");

			HttpResponse response = httpclient.execute(del);
		    ckResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);

			HttpEntity entity = response.getEntity();
			EntityUtils.consume(entity);
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			del.releaseConnection();
		}
	}
}
