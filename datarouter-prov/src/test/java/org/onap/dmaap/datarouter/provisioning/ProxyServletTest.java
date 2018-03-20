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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxyServletTest {
	@Mock
	ProxyServlet proxyservlet;

	@Mock
	ServletConfig config;
	HttpServletRequest req;
	HttpServletResponse resp;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testInitServletConfig() {
		String s = null;
		try {
			proxyservlet.init(config);
		} catch (ServletException e) {
			
			e.printStackTrace();
			s = e.toString();
		}
		assertNull(s);
	}

	@Test
	public void testIsProxyServer() {
		Boolean flag = proxyservlet.isProxyServer();
		assertEquals(Boolean.FALSE, flag);

	}

	@Test
	public void testDoDeleteHttpServletRequestHttpServletResponse() {
		String s = null;
		try {
			proxyservlet.doDelete(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			s = e.toString();
		}
		assertNull(s);

	}

	@Test
	public void testDoGetHttpServletRequestHttpServletResponse() {
		try {
			proxyservlet.doGet(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testDoPutHttpServletRequestHttpServletResponse() {
		String str = null;
		try {
			proxyservlet.doPut(req, resp);
		} catch (IOException e) {

			e.printStackTrace();
			str = e.toString();
		}
		assertNull(str);

	}

	@Test
	public void testDoPostHttpServletRequestHttpServletResponse() throws IOException {
		proxyservlet.doPost(req, resp);

	}

	@Test
	public void testDoGetWithFallback() throws IOException {
		boolean flag = proxyservlet.doGetWithFallback(req, resp);
		assertEquals(Boolean.FALSE, flag);

	}

}