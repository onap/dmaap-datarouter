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

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

public class DBTest {
	private DB db=null;

	@Before
	public void setUp() throws Exception {
		db=new DB();
	}

	@Test
	public void testGetProperties() {
		db.getProperties();
		
	}

	/*@Test
	public void testGetConnection() {
		Connection  con = null;
		try {
			con = db.getConnection();
			
		} catch (SQLException e) {
		}
		assertNotNull(con);
	}

	@Test
	public void testRelease() {
		Connection  con = null;
		try {
			con = db.getConnection();
			db.release(con);
			
		} catch (SQLException e) {
		}
		assertNull(con);
	}*/

	@Test
	public void testRunRetroFits() {
		boolean flag = db.runRetroFits();
		assertEquals(Boolean.FALSE, flag);
	}

}
