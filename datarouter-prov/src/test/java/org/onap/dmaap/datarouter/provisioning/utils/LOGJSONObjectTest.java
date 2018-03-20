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

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class LOGJSONObjectTest {
	LOGJSONObject logobject = null;

	@Before
	public void setUp() throws Exception {
		logobject = new LOGJSONObject();
	}

	@Test
	public void testAccumulate() throws JSONException {
		Object value;
		String key;
		logobject.accumulate("Key", "value");

	}

	@Test
	public void testAppend() {
		try {
			logobject.append("Key", new JSONArray());
		} catch (JSONException e) {
			// TODO: handle exception
		}
	}

	@Test
	public void testDoubleToString() {
		String val = logobject.doubleToString(100);
		assertEquals("100", val);
	}

	@Test
	public void testGet() {
		try {
			logobject.append("Key", new JSONArray());
			Object obj = logobject.get("Key");
			assertNotNull(obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetBoolean() {
		try {
			boolean flag = logobject.getBoolean("Key");
			assertEquals(Boolean.TRUE, flag);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetDouble() {
		try {
			logobject.append("DoubleKey", 100);
			double val = logobject.getDouble("DoubleKey");
			assertEquals(100, val);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testGetInt() {
		try {
			logobject.append("intKey", 100);
			int val = logobject.getInt("intKey");
			assertEquals(100, val);
		} catch (JSONException e) {

			e.printStackTrace();
		}
	}

	@Test
	public void testGetJSONArray() {
		try {
			logobject.append("jsonArry", new JSONArray());
			Object obj = logobject.getJSONArray("jsonArray");
			assertNotNull("jsonArray");
		} catch (JSONException e) {

			e.printStackTrace();
		}
	}

	@Test
	public void testGetJSONObject() {
		try {
			logobject.append("jsonObject", new JSONObject());
			Object obj = logobject.getJSONObject("jsonObject");
			assertNotNull("jsonObject");
		} catch (JSONException e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testGetLong() {
		try {
			logobject.append("longKey", 1l);
			long val = logobject.getLong("longKey");
			assertEquals(1l, val);
		} catch (JSONException e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testGetString() {
		try {
			logobject.append("stringKey", "100");
			String val = logobject.getString("stringKey");
			assertEquals("100", val);
		} catch (JSONException e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testHas() {
		boolean flag = logobject.has("Key");
		assertEquals(Boolean.FALSE, flag);
	}

	@Test
	public void testIncrement() {
		try {
			logobject.append("intKey", 100);
			logobject.increment("intKey");
			int val = logobject.getInt("intKey");
			assertEquals(101, val);
		} catch (JSONException e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testIsNull() {
		boolean flag = logobject.isNull("test");
		assertEquals(Boolean.TRUE, flag);
	}

	@Test
	public void testKeys() {
		Iterator<String> itr = logobject.keys();
		assertNotNull(itr);

	}

	@Test
	public void testToString() {
		logobject.toString();

	}

}
