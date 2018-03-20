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

package org.onap.dmaap.datarouter.provisioning;

import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FeedServletTest {

	@Mock
	FeedServlet feedservlet;

	@Mock
	HttpServletRequest req;
	HttpServletResponse resp;

	@Before
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDoDeleteHttpServletRequestHttpServletResponse() {
		String s = null;
		try {
			feedservlet.doDelete(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			s = e.toString();
		}
		assertNull(s);
	}

	@Test
	public void testDoGetHttpServletRequestHttpServletResponse() {
		String st = null;
		try {
			feedservlet.doGet(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			st = e.toString();
		}
		assertNull(st);
	}

	@Test
	public void testDoPutHttpServletRequestHttpServletResponse() throws IOException {
		feedservlet.doPut(req, resp);

	}

	@Test
	public void testDoPostHttpServletRequestHttpServletResponse() throws IOException {
		feedservlet.doPost(req, resp);

	}

}
