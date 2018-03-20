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

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class BaseLogRecordTest {
	BaseLogRecord obj = null;

	@Mock
	PreparedStatement ps;
	@Mock
	ResultSet rs;

	@Before
	public void setUp() throws Exception {

		obj = new BaseLogRecord(rs);

	}

	@Test
	public void testGetEventTime() {
		obj.setEventTime(0);
		assertEquals(obj.getEventTime(), 0);
	}

	@Test
	public void testGetPublishId() {
		obj.setPublishId("1");
		assertEquals(obj.getPublishId(), "1");
	}

	@Test
	public void testGetFeedid() {
		obj.setFeedid(1);
		assertEquals(obj.getFeedid(), 1);
	}

	@Test
	public void testGetRequestUri() {
		obj.setRequestUri("1");
		assertEquals(obj.getRequestUri(), "1");
	}

	@Test
	public void testGetMethod() {
		obj.setMethod("1");
		assertEquals(obj.getMethod(), "1");

	}

	@Test
	public void testGetContentType() {
		obj.setContentType("1");
		assertEquals(obj.getContentType(), "1");
	}

	@Test
	public void testGetContentLength() {
		obj.setContentLength(0);
		assertEquals(obj.getContentLength(), 0);
	}

	@Test
	public void testAsJSONObject() throws JSONException, SQLException {
		obj.setEventTime(System.currentTimeMillis());
		obj.setPublishId("1");
		obj.setRequestUri("TestURI");
		obj.setMethod("PUT");

		PowerMockito.when(rs.getLong("EVENT_TIME")).thenReturn(1l);
		PowerMockito.when(rs.getString("PUBLISH_ID")).thenReturn("1");
		PowerMockito.when(rs.getString("REQURI")).thenReturn("test");
		PowerMockito.when(rs.getString("METHOD")).thenReturn("PUT");

		LOGJSONObject obj2 = obj.asJSONObject();
		assertEquals(obj2.get("publishId"), "1");
		assertEquals(obj2.get("requestURI"), "TestURI");
		assertEquals(obj2.get("method"), "PUT");
	}

}
