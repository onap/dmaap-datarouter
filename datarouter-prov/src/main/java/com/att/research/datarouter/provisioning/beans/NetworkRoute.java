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


package com.att.research.datarouter.provisioning.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.att.research.datarouter.provisioning.utils.DB;

/**
 * The representation of one route in the Network Route Table.
 *
 * @author Robert P. Eby
 * @version $Id: NetworkRoute.java,v 1.2 2013/12/16 20:30:23 eby Exp $
 */
public class NetworkRoute extends NodeClass implements Comparable<NetworkRoute> {
	private static Logger intlogger = Logger.getLogger("com.att.research.datarouter.provisioning.internal");
	private final int fromnode;
	private final int tonode;
	private final int vianode;

	/**
	 * Get a set of all Network Routes in the DB.  The set is sorted according to the natural sorting order
	 * of the routes (based on the from and to node names in each route).
	 * @return the sorted set
	 */
	public static SortedSet<NetworkRoute> getAllNetworkRoutes() {
		SortedSet<NetworkRoute> set = new TreeSet<NetworkRoute>();
		try {
			DB db = new DB();
			@SuppressWarnings("resource")
			Connection conn = db.getConnection();
			Statement  stmt = conn.createStatement();
			ResultSet    rs = stmt.executeQuery("select FROMNODE, TONODE, VIANODE from NETWORK_ROUTES");
			while (rs.next()) {
				int fromnode = rs.getInt("FROMNODE");
				int tonode   = rs.getInt("TONODE");
				int vianode  = rs.getInt("VIANODE");
				set.add(new NetworkRoute(fromnode, tonode, vianode));
			}
			rs.close();
			stmt.close();
			db.release(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return set;
	}

	public NetworkRoute(String fromnode, String tonode) throws IllegalArgumentException {
		this.fromnode = lookupNodeName(fromnode);
		this.tonode   = lookupNodeName(tonode);
		this.vianode  = -1;
	}

	public NetworkRoute(String fromnode, String tonode, String vianode) throws IllegalArgumentException {
		this.fromnode = lookupNodeName(fromnode);
		this.tonode   = lookupNodeName(tonode);
		this.vianode  = lookupNodeName(vianode);
	}

	public NetworkRoute(JSONObject jo) throws IllegalArgumentException {
		this.fromnode = lookupNodeName(jo.getString("from"));
		this.tonode   = lookupNodeName(jo.getString("to"));
		this.vianode  = lookupNodeName(jo.getString("via"));
	}

	public NetworkRoute(int fromnode, int tonode, int vianode) throws IllegalArgumentException {
		this.fromnode = fromnode;
		this.tonode   = tonode;
		this.vianode  = vianode;
	}

	public int getFromnode() {
		return fromnode;
	}

	public int getTonode() {
		return tonode;
	}

	public int getVianode() {
		return vianode;
	}

	@Override
	public boolean doDelete(Connection c) {
		boolean rv = true;
		PreparedStatement ps = null;
		try {
			String sql = "delete from NETWORK_ROUTES where FROMNODE = ? AND TONODE = ?";
			ps = c.prepareStatement(sql);
			ps.setInt(1, fromnode);
			ps.setInt(2, tonode);
			ps.execute();
		} catch (SQLException e) {
			rv = false;
			intlogger.warn("PROV0007 doDelete: "+e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return rv;
	}

	@Override
	public boolean doInsert(Connection c) {
		boolean rv = false;
		if (this.vianode >= 0) {
			PreparedStatement ps = null;
			try {
				// Create the NETWORK_ROUTES row
				String sql = "insert into NETWORK_ROUTES (FROMNODE, TONODE, VIANODE) values (?, ?, ?)";
				ps = c.prepareStatement(sql);
				ps.setInt(1, this.fromnode);
				ps.setInt(2, this.tonode);
				ps.setInt(3, this.vianode);
				ps.execute();
				ps.close();
				rv = true;
			} catch (SQLException e) {
				intlogger.warn("PROV0005 doInsert: "+e.getMessage());
				e.printStackTrace();
			} finally {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return rv;
	}

	@Override
	public boolean doUpdate(Connection c) {
		boolean rv = true;
		PreparedStatement ps = null;
		try {
			String sql = "update NETWORK_ROUTES set VIANODE = ? where FROMNODE = ? and TONODE = ?";
			ps = c.prepareStatement(sql);
			ps.setInt(1, vianode);
			ps.setInt(2, fromnode);
			ps.setInt(3, tonode);
			ps.executeUpdate();
		} catch (SQLException e) {
			rv = false;
			intlogger.warn("PROV0006 doUpdate: "+e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return rv;
	}

	@Override
	public JSONObject asJSONObject() {
		JSONObject jo = new JSONObject();
		jo.put("from", lookupNodeID(fromnode));
		jo.put("to",   lookupNodeID(tonode));
		jo.put("via",  lookupNodeID(vianode));
		return jo;
	}

	@Override
	public String getKey() {
		return lookupNodeID(fromnode)+":"+lookupNodeID(tonode);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NetworkRoute))
			return false;
		NetworkRoute on = (NetworkRoute)obj;
		return (fromnode == on.fromnode) && (tonode == on.tonode) && (vianode == on.vianode);
	}

	@Override
	public int compareTo(NetworkRoute o) {
		if (this.fromnode == o.fromnode) {
			if (this.tonode == o.tonode)
				return this.vianode - o.vianode;
			return this.tonode - o.tonode;
		}
		return this.fromnode - o.fromnode;
	}

	@Override
	public String toString() {
		return String.format("NETWORK: from=%d, to=%d, via=%d", fromnode, tonode, vianode);
	}
}
