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


package org.onap.dmaap.datarouter.provisioning.beans;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DataSource;

/**
 * The representation of one route in the Network Route Table.
 *
 * @author Robert P. Eby
 * @version $Id: NetworkRoute.java,v 1.2 2013/12/16 20:30:23 eby Exp $
 */
public class NetworkRoute extends NodeClass implements Comparable<NetworkRoute> {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static final String SQLEXCEPTION = "SQLException: ";
    private final int fromnode;
    private final int tonode;
    private final int vianode;

    /**
     * NetworkRoute Constructor.
     * @param fromnode node source
     * @param tonode node destination
     */
    public NetworkRoute(String fromnode, String tonode) {
        this.fromnode = lookupNodeName(fromnode);
        this.tonode = lookupNodeName(tonode);
        this.vianode = -1;
    }

    /**
     * NetworkRoute Constructor.
     * @param fromnode node source
     * @param tonode node destination
     * @param vianode via node
     */
    public NetworkRoute(String fromnode, String tonode, String vianode) {
        this.fromnode = lookupNodeName(fromnode);
        this.tonode = lookupNodeName(tonode);
        this.vianode = lookupNodeName(vianode);
    }

    /**
     * NetworkRoute Constructor.
     * @param jo JSONObject of attributes
     */
    public NetworkRoute(JSONObject jo) {
        this.fromnode = lookupNodeName(jo.getString("from"));
        this.tonode = lookupNodeName(jo.getString("to"));
        this.vianode = lookupNodeName(jo.getString("via"));
    }

    /**
     * NetworkRoute Constructor.
     * @param fromnode integer source node
     * @param tonode integer destination node
     * @param vianode integer via node
     */
    private NetworkRoute(int fromnode, int tonode, int vianode) {
        this.fromnode = fromnode;
        this.tonode = tonode;
        this.vianode = vianode;
    }

    /**
     * Get a set of all Network Routes in the DB.  The set is sorted according to the natural sorting order of the
     * routes (based on the from and to node names in each route).
     *
     * @return the sorted set
     */
    public static SortedSet<NetworkRoute> getAllNetworkRoutes() {
        SortedSet<NetworkRoute> set = new TreeSet<>();
        try {
            Connection conn = DataSource.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select FROMNODE, TONODE, VIANODE from NETWORK_ROUTES")) {
                    addNetworkRouteToSet(set, rs);
                }
            } finally {
                DataSource.returnConnection(conn);
            }
        } catch (SQLException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
        }
        return set;
    }

    private static void addNetworkRouteToSet(SortedSet<NetworkRoute> set, ResultSet rs) throws SQLException {
        while (rs.next()) {
            int fromnode = rs.getInt("FROMNODE");
            int tonode = rs.getInt("TONODE");
            int vianode = rs.getInt("VIANODE");
            set.add(new NetworkRoute(fromnode, tonode, vianode));
        }
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
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        String sql = "delete from NETWORK_ROUTES where FROMNODE = ? AND TONODE = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromnode);
            ps.setInt(2, tonode);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = false;
        String sql = "insert into NETWORK_ROUTES (FROMNODE, TONODE, VIANODE) values (?, ?, ?)";
        if (this.vianode >= 0) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Create the NETWORK_ROUTES row
                ps.setInt(1, this.fromnode);
                ps.setInt(2, this.tonode);
                ps.setInt(3, this.vianode);
                ps.execute();
                rv = true;
            } catch (SQLException e) {
                intlogger.warn("PROV0005 doInsert: " + e.getMessage(), e);
            }
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection conn) {
        boolean rv = true;
        String sql = "update NETWORK_ROUTES set VIANODE = ? where FROMNODE = ? and TONODE = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vianode);
            ps.setInt(2, fromnode);
            ps.setInt(3, tonode);
            ps.executeUpdate();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("from", lookupNodeID(fromnode));
        jo.put("to", lookupNodeID(tonode));
        jo.put("via", lookupNodeID(vianode));
        return jo;
    }

    @Override
    public String getKey() {
        return lookupNodeID(fromnode) + ":" + lookupNodeID(tonode);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkRoute)) {
            return false;
        }
        NetworkRoute on = (NetworkRoute) obj;
        return (fromnode == on.fromnode) && (tonode == on.tonode) && (vianode == on.vianode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromnode, tonode, vianode);
    }

    @Override
    public int compareTo(NetworkRoute nr) {
        if (this.fromnode == nr.fromnode) {
            if (this.tonode == nr.tonode) {
                return this.vianode - nr.vianode;
            }
            return this.tonode - nr.tonode;
        }
        return this.fromnode - nr.fromnode;
    }

    @Override
    public String toString() {
        return String.format("NETWORK: from=%d, to=%d, via=%d", fromnode, tonode, vianode);
    }
}
