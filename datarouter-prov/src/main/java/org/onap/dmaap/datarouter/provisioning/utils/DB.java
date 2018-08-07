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


package org.onap.dmaap.datarouter.provisioning.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Load the DB JDBC driver, and manage a simple pool of connections to the DB.
 *
 * @author Robert Eby
 * @version $Id$
 */
public class DB {
	/** The name of the properties file (in CLASSPATH) */
	private static final String CONFIG_FILE = "provserver.properties";

	private static String DB_URL;
	private static String DB_LOGIN;
	private static String DB_PASSWORD;
	private static Properties props;
	private static Logger intlogger = Logger.getLogger("org.onap.dmaap.datarouter.provisioning.internal");
	private static final Queue<Connection> queue = new LinkedList<>();

	public static String HTTPS_PORT;
	public static String HTTP_PORT;

	/**
	 * Construct a DB object.  If this is the very first creation of this object, it will load a copy
	 * of the properties for the server, and attempt to load the JDBC driver for the database.  If a fatal
	 * error occurs (e.g. either the properties file or the DB driver is missing), the JVM will exit.
	 */
	public DB() {
		if (props == null) {
			props = new Properties();
			try (InputStream inStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
				props.load(inStream);
				String DB_DRIVER = (String) props.get("org.onap.dmaap.datarouter.db.driver");
				DB_URL = (String) props.get("org.onap.dmaap.datarouter.db.url");
				DB_LOGIN = (String) props.get("org.onap.dmaap.datarouter.db.login");
				DB_PASSWORD = (String) props.get("org.onap.dmaap.datarouter.db.password");
				HTTPS_PORT = (String) props.get("org.onap.dmaap.datarouter.provserver.https.port");
				HTTP_PORT = (String) props.get("org.onap.dmaap.datarouter.provserver.http.port");
				Class.forName(DB_DRIVER);
			} catch (IOException e) {
				intlogger.fatal("PROV9003 Opening properties: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			} catch (ClassNotFoundException e) {
				intlogger.fatal("PROV9004 cannot find the DB driver: " + e);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	/**
	 * Get the provisioning server properties (loaded from provserver.properties).
	 * @return the Properties object
	 */
	public Properties getProperties() {
		return props;
	}
	/**
	 * Get a JDBC connection to the DB from the pool.  Creates a new one if none are available.
	 * @return the Connection
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	public Connection getConnection() throws SQLException {
		Connection connection = null;
		while (connection == null) {
			synchronized (queue) {
				try {
					connection = queue.remove();
				} catch (NoSuchElementException nseEx) {
					int n = 0;
					do {
						// Try up to 3 times to get a connection
						try {
							connection = DriverManager.getConnection(DB_URL, DB_LOGIN, DB_PASSWORD);
						} catch (SQLException sqlEx) {
							if (++n >= 3)
								throw sqlEx;
						}
					} while (connection == null);
				}
			}
			if (connection != null && !connection.isValid(1)) {
				connection.close();
				connection = null;
			}
		}
		return connection;
	}
	/**
	 * Returns a JDBC connection to the pool.
	 * @param connection the Connection to return
	 */
	public void release(Connection connection) {
		if (connection != null) {
			synchronized (queue) {
				if (!queue.contains(connection))
					queue.add(connection);
			}
		}
	}

	/**
	 * Run all necessary retrofits required to bring the database up to the level required for this version
	 * of the provisioning server.  This should be run before the server itself is started.
	 * @return true if all retrofits worked, false otherwise
	 */
	public boolean runRetroFits() {
		return retroFit1();
	}

	/**
	 * Retrofit 1 - Make sure the expected tables are in DB and are initialized.
	 * Uses sql_init_0000.sql to setup the DB.
	 * @return true if the retrofit worked, false otherwise
	 */
	private boolean retroFit1() {
		final String[] expectedTables = {
				"FEEDS", "FEED_ENDPOINT_ADDRS", "FEED_ENDPOINT_IDS", "PARAMETERS",
				"SUBSCRIPTIONS", "LOG_RECORDS", "INGRESS_ROUTES", "EGRESS_ROUTES",
				"NETWORK_ROUTES", "NODESETS", "NODES", "GROUPS"
		};
		Connection connection = null;
		try {
			connection = getConnection();
			Set<String> actualTables = getTableSet(connection);
			boolean initialize = false;
			for (String table : expectedTables) {
				initialize |= !actualTables.contains(table);
			}
			if (initialize) {
				intlogger.info("PROV9001: First time startup; The database is being initialized.");
				runInitScript(connection, 0);
			}
		} catch (SQLException e) {
			intlogger.fatal("PROV9000: The database credentials are not working: "+e.getMessage());
			return false;
		} finally {
			if (connection != null)
				release(connection);
		}
		return true;
	}

	/**
	 * Get a set of all table names in the DB.
	 * @param connection a DB connection
	 * @return the set of table names
	 */
	private Set<String> getTableSet(Connection connection) {
		Set<String> tables = new HashSet<String>();
		try {
			DatabaseMetaData md = connection.getMetaData();
			ResultSet rs = md.getTables("datarouter", "", "", null);
			if (rs != null) {
				while (rs.next()) {
					tables.add(rs.getString("TABLE_NAME"));
				}
				rs.close();
			}
		} catch (SQLException e) {
		}
		return tables;
	}
	/**
	 * Initialize the tables by running the initialization scripts located in the directory specified
	 * by the property <i>org.onap.dmaap.datarouter.provserver.dbscripts</i>.  Scripts have names of
	 * the form sql_init_NNNN.sql
	 * @param connection a DB connection
	 * @param scriptId the number of the sql_init_NNNN.sql script to run
	 */
	private void runInitScript(Connection connection, int scriptId) {
		String scriptDir = (String) props.get("org.onap.dmaap.datarouter.provserver.dbscripts");
		StringBuilder sb = new StringBuilder();
		try {
			String scriptFile = String.format("%s/sql_init_%04d.sql", scriptDir, scriptId);
			if (!(new File(scriptFile)).exists())
				return;

			LineNumberReader in = new LineNumberReader(new FileReader(scriptFile));
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("--")) {
					line = line.trim();
					sb.append(line);
					if (line.endsWith(";")) {
						// Execute one DDL statement
						String sql = sb.toString();
						sb.setLength(0);
						Statement s = connection.createStatement();
						s.execute(sql);
						s.close();
					}
				}
			}
			in.close();
			sb.setLength(0);
		} catch (Exception e) {
			intlogger.fatal("PROV9002 Error when initializing table: "+e.getMessage());
			System.exit(1);
		}
	}
}
