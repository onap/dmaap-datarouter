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
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;

/**
 * The representation of one route in the Egress Route Table.
 *
 * @author Robert P. Eby
 * @version $Id: EgressRoute.java,v 1.3 2013/12/16 20:30:23 eby Exp $
 */
public class EgressRoute extends NodeClass implements Comparable<EgressRoute> {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private final int subid;
    private final int nodeid;

    /**
     * EgressRoute constructor.
     * @param subid subscription id
     * @param nodeid node id
     */
    public EgressRoute(int subid, int nodeid) {
        this.subid = subid;
        this.nodeid = nodeid;
    }

    public EgressRoute(int subid, String node) {
        this(subid, lookupNodeName(node));
    }

    /**
     * Get a set of all Egress Routes in the DB.  The set is sorted according to the natural sorting order of the routes
     * (based on the subscription ID in each route).
     *
     * @return the sorted set
     */
    public static SortedSet<EgressRoute> getAllEgressRoutes() {
        SortedSet<EgressRoute> set = new TreeSet<>();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select SUBID, NODEID from EGRESS_ROUTES")) {
            addEgressRouteToSet(set, rs);
        } catch (SQLException e) {
            intlogger.error("PROV0008 EgressRoute.getAllEgressRoutes: " + e.getMessage(), e);
        }
        return set;
    }

    private static void addEgressRouteToSet(SortedSet<EgressRoute> set, ResultSet rs) throws SQLException {
        while (rs.next()) {
            int subid = rs.getInt("SUBID");
            int nodeid = rs.getInt("NODEID");
            set.add(new EgressRoute(subid, nodeid));
        }
    }

    /**
     * Get a single Egress Route for the subscription <i>sub</i>.
     *
     * @param sub the subscription to lookup
     * @return an EgressRoute, or null if there is no route for this subscription
     */
    public static EgressRoute getEgressRoute(int sub) {
        EgressRoute er = null;
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("select NODEID from EGRESS_ROUTES where SUBID = ?")) {
            ps.setInt(1, sub);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int node = rs.getInt("NODEID");
                    er = new EgressRoute(sub, node);
                }
            }
        } catch (SQLException e) {
            intlogger.error("PROV0009 EgressRoute.getEgressRoute: " + e.getMessage(), e);
        }
        return er;
    }

    @Override
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        try (PreparedStatement ps = conn.prepareStatement("delete from EGRESS_ROUTES where SUBID = ?")) {
            ps.setInt(1, subid);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.error("PROV0007 doDelete: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = false;
        try (PreparedStatement ps = conn.prepareStatement("insert into EGRESS_ROUTES (SUBID, NODEID) values (?, ?)")) {
            ps.setInt(1, this.subid);
            ps.setInt(2, this.nodeid);
            ps.execute();
            rv = true;
        } catch (SQLException e) {
            intlogger.warn("PROV0005 doInsert: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection conn) {
        boolean rv = true;
        try (PreparedStatement ps = conn.prepareStatement("update EGRESS_ROUTES set NODEID = ? where SUBID = ?")) {
            ps.setInt(1, nodeid);
            ps.setInt(2, subid);
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
        jo.put("" + subid, lookupNodeID(nodeid));
        return jo;
    }

    @Override
    public String getKey() {
        return "" + subid;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EgressRoute)) {
            return false;
        }
        EgressRoute on = (EgressRoute) obj;
        return (subid == on.subid) && (nodeid == on.nodeid);
    }

    @Override
    public int compareTo(EgressRoute er) {
        return this.subid - er.subid;
    }

    @Override
    public String toString() {
        return String.format("EGRESS: sub=%d, node=%d", subid, nodeid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subid, nodeid);
    }
}
