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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.After;
import org.junit.Before;

import com.att.research.datarouter.provisioning.FeedServlet;

public class testBase {
	/** The properties file to read the DB properties from */
	public static final String CONFIG_FILE = "tests.properties";

	public Properties props;
	protected AbstractHttpClient httpclient;
	protected String s_33;
	protected String s_257;
	protected static JSONObject db_state;

	@Before
	public void setUp() throws Exception {
		if (props == null) {
			props = new Properties();
			InputStream inStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
			try {
				props.load(inStream);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				inStream.close();
			}
		}

		httpclient = new DefaultHttpClient();
		String s = "0123456789ABCDEF";
		s_33 = s + s + "!";
		s = s + s + s + s;
		s_257 = s + s + s + s + "!";

		// keystore
		String store = props.getProperty("test.keystore");
		String pass  = props.getProperty("test.kspassword");
		KeyStore keyStore  = KeyStore.getInstance(KeyStore.getDefaultType());
	    FileInputStream instream = new FileInputStream(new File(store));
	    try {
	    	keyStore.load(instream, pass.toCharArray());
	    } catch (Exception x) {
	    	System.err.println("READING KEYSTORE: "+x);
	    } finally {
	        try { instream.close(); } catch (Exception ignore) {}
	    }

		store = props.getProperty("test.truststore");
		pass  = props.getProperty("test.tspassword");
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
	    instream = new FileInputStream(new File(store));
	    try {
	        trustStore.load(instream, pass.toCharArray());
	    } catch (Exception x) {
	    	System.err.println("READING TRUSTSTORE: "+x);
	    } finally {
	        try { instream.close(); } catch (Exception ignore) {}
	    }

	    SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore, "changeit", trustStore);
	    Scheme sch = new Scheme("https", 443, socketFactory);
	    httpclient.getConnectionManager().getSchemeRegistry().register(sch);
	}

	public JSONObject getDBstate() {
		// set db_state!
		if (db_state == null) {
			String url   = props.getProperty("test.host") + "/internal/prov";
			HttpGet httpGet = new HttpGet(url);
			try {
				httpGet.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
				HttpResponse response = httpclient.execute(httpGet);
				HttpEntity entity = response.getEntity();
				String ctype = entity.getContentType().getValue().trim();
				// save the response body as db_state
				boolean ok  = ctype.equals(FeedServlet.PROVFULL_CONTENT_TYPE1);
				        ok |= ctype.equals(FeedServlet.PROVFULL_CONTENT_TYPE2);
				if (ok) {
					db_state = null;
					try {
						db_state = new JSONObject(new JSONTokener(entity.getContent()));
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
		return db_state;
	}

	@After
	public void tearDown() throws Exception {
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources
		httpclient.getConnectionManager().shutdown();
	}

	protected void ckResponse(HttpResponse response, int expect) {
		System.out.println(response.getStatusLine());
		StatusLine sl = response.getStatusLine();
		int code = sl.getStatusCode();
		if (code != expect)
			fail("Unexpected response, expect "+expect+" got "+code+" "+sl.getReasonPhrase());
	}
}
