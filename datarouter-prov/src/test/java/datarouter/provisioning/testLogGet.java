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
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class testLogGet extends testBase {
	private JSONArray returnedlist;
	private int feedid = 4;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// need to seed the DB here
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// need to "unseed" the DB
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		getDBstate();
//		JSONArray ja = db_state.getJSONArray("feeds");
//		for (int i = 0; i < ja.length(); i++) {
//			JSONObject jo = ja.getJSONObject(i);
//			if (!jo.getBoolean("deleted"))
//				feedid = jo.getInt("feedid");
//		}
	}

	@Test
	public void testNormal() {
		testCommon(HttpServletResponse.SC_OK);
	}
	@Test
	public void testNormalPubOnly() {
		testCommon(HttpServletResponse.SC_OK, "?type=pub");
	}
	@Test
	public void testNormalDelOnly() {
		testCommon(HttpServletResponse.SC_OK, "?type=del");
	}
	@Test
	public void testNormalExpOnly() {
		testCommon(HttpServletResponse.SC_OK, "?type=exp");
	}
	@Test
	public void testNormalXXXOnly() {
		testCommon(HttpServletResponse.SC_BAD_REQUEST, "?type=xxx");
	}
	@Test
	public void testNormalStatusSuccess() {
		testCommon(HttpServletResponse.SC_OK, "?statusCode=success");
	}
	@Test
	public void testNormalStatusRedirect() {
		testCommon(HttpServletResponse.SC_OK, "?statusCode=redirect");
	}
	@Test
	public void testNormalStatusFailure() {
		testCommon(HttpServletResponse.SC_OK, "?statusCode=failure");
	}
	@Test
	public void testNormalStatus200() {
		testCommon(HttpServletResponse.SC_OK, "?statusCode=200");
	}
	@Test
	public void testNormalStatusXXX() {
		testCommon(HttpServletResponse.SC_BAD_REQUEST, "?statusCode=xxx");
	}
	@Test
	public void testNormalExpiryNotRetryable() {
		testCommon(HttpServletResponse.SC_OK, "?expiryReason=notRetryable");
	}
	@Test
	public void testNormalExpiryRetriesExhausted() {
		testCommon(HttpServletResponse.SC_OK, "?expiryReason=retriesExhausted");
	}
	@Test
	public void testNormalExpiryXXX() {
		testCommon(HttpServletResponse.SC_BAD_REQUEST, "?expiryReason=xxx");
	}
	@Test
	public void testNormalPublishId() {
		testCommon(HttpServletResponse.SC_OK, "?publishId=1366985877801.mtdvnj00-drtr.proto.research.att.com");
	}
	@Test
	public void testNormalStart() {
		long n = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L);	// 5 days
		testCommon(HttpServletResponse.SC_OK, String.format("?start=%s", sdf.format(n)));
	}
	@Test
	public void testBadStart() {
		testCommon(HttpServletResponse.SC_BAD_REQUEST, "?start=xxx");
	}
	@Test
	public void testLongEnd() {
		testCommon(HttpServletResponse.SC_OK, "?end=1364837896220");
	}
	@Test
	public void testBadEnd() {
		testCommon(HttpServletResponse.SC_BAD_REQUEST, "?end=2013-04-25T11:01:25Q");
	}
	private void testCommon(int expect) {
		testCommon(expect, "");
	}
	private void testCommon(int expect, String query) {
		String url = props.getProperty("test.host") + "/feedlog/" + feedid + query;
		HttpGet httpGet = new HttpGet(url);
		try {
			HttpResponse response = httpclient.execute(httpGet);
		    ckResponse(response, expect);

			HttpEntity entity = response.getEntity();
			String ctype = entity.getContentType().getValue().trim();
			if (expect == HttpServletResponse.SC_OK) {
				if (!ctype.equals(FeedServlet.LOGLIST_CONTENT_TYPE))
					fail("Got wrong content type: "+ctype);
			}

			// do something useful with the response body and ensure it is fully consumed
			if (ctype.equals(FeedServlet.LOGLIST_CONTENT_TYPE)) {
				try {
					returnedlist = new JSONArray(new JSONTokener(entity.getContent()));
					int n = returnedlist.length();
					if (n != 0)
						System.err.println(n + " items");
				} catch (Exception e) {
					fail("Bad JSON: "+e.getMessage());
				}
			} else {
				EntityUtils.consume(entity);
			}
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			httpGet.releaseConnection();
		}
	}
}
