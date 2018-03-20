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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseServletTest {
	@Mock
	BaseServlet base;
	@Mock
	ServletConfig config;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

	}

	@Test
	public void testInit() {
		String s = null;
		try {
			base.init(config);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			s = e.toString();
		}

		assertNull(s);
	}

	@Test
	public void testGetNodes() {
		String[] str = base.getNodes();
		assertNotNull(str);
	}

	@Test
	public void testGetNodeAddresses() {
		InetAddress[] nodeaddress = base.getNodeAddresses();
		assertNotNull(nodeaddress);
	}

	@Test
	public void testGetPods() {
		String[] str1 = base.getPods();
		assertNotNull(str1);
	}

	@Test
	public void testGetPodAddresses() {
		InetAddress[] podaddress = BaseServlet.getPodAddresses();
		assertNotNull(podaddress);
	}
}
