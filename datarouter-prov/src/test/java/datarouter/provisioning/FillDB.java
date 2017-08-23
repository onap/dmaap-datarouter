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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class FillDB {
	public static void main(String[] args)
		throws KeyStoreException, FileNotFoundException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException
	{
		AbstractHttpClient httpclient = new DefaultHttpClient();

		String keystore = "/home/eby/dr2/misc/client.keystore";
		String kspass   = "changeit";
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
	    FileInputStream instream = new FileInputStream(new File(keystore));
	    try {
	        trustStore.load(instream, kspass.toCharArray());
	    } catch (Exception x) {
	    	System.err.println("READING KEYSTORE: "+x);
	    } finally {
	        try { instream.close(); } catch (Exception ignore) {}
	    }

	    SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore, "changeit", trustStore);
	    Scheme sch = new Scheme("https", 443, socketFactory);
	    httpclient.getConnectionManager().getSchemeRegistry().register(sch);

	    JSONObject jo = buildFeedRequest();
		for (int i = 0; i < 10000; i++) {
			jo.put("version", ""+System.currentTimeMillis());
			int rv = -1;
			String url   = "https://conwy.proto.research.att.com:6443/";
			HttpPost httpPost = new HttpPost(url);
			try {
				httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
				String t = jo.toString();
				HttpEntity body = new ByteArrayEntity(t.getBytes(), ContentType.create(FeedServlet.FEED_CONTENT_TYPE));
				httpPost.setEntity(body);

				HttpResponse response = httpclient.execute(httpPost);
				rv = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
				EntityUtils.consume(entity);
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				httpPost.releaseConnection();
			}
			System.out.println(i + " " + rv);
		}
	}
	private static JSONObject buildFeedRequest() {
		JSONObject jo = new JSONObject();
		jo.put("name", "feed");
		jo.put("version", ""+System.currentTimeMillis());
		jo.put("description", "Sample feed used by JUnit to test");

			JSONObject jo2 = new JSONObject();
			jo2.put("classification", "unrestricted");

			JSONArray ja = new JSONArray();
				JSONObject jo3 = new JSONObject();
				jo3.put("id", "id001");
				jo3.put("password", "re1kwelj");
				JSONObject jo4 = new JSONObject();
				jo4.put("id", "id002");
				jo4.put("password", "o9eqlmbd");
				ja.put(jo3);
				ja.put(jo4);
			jo2.put("endpoint_ids", ja);

			ja = new JSONArray();
				ja.put("10.0.0.1");
				ja.put("192.168.0.1");
				ja.put("135.207.136.128/25");
			jo2.put("endpoint_addrs", ja);

		jo.put("authorization", jo2);
		return jo;
	}
}
