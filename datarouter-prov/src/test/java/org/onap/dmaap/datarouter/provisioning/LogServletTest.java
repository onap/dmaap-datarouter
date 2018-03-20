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

import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class LogServletTest {
	// LogServlet logservlet=null;

	@Mock
	HttpServletRequest req;
	@Mock
	HttpServletResponse resp;

	@Mock
	LogServlet logservlet;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

	}

	@Test
	public void testDoDeleteHttpServletRequestHttpServletResponse() throws IOException {
		logservlet.doDelete(req, resp);

	}

	@Test
	public void testDoGetHttpServletRequestHttpServletResponse() {
		String s = null;
		try {
			logservlet.doGet(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			s = e.toString();
		}
		assertNull(s);

	}

	@Test
	public void testDoPutHttpServletRequestHttpServletResponse() {
		String st = null;
		try {
			logservlet.doPut(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			st = e.toString();
		}
		assertNull(st);

	}

	@Test
	public void testDoPostHttpServletRequestHttpServletResponse() throws IOException {
		logservlet.doPost(req, resp);

	}

}
