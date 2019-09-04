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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DataSource;

/**
 * The representation of one route in the Ingress Route Table.
 *
 * @author Robert P. Eby
 * @version $Id: IngressRoute.java,v 1.3 2013/12/16 20:30:23 eby Exp $
 */

public class IngressRoute extends NodeClass implements Comparable<IngressRoute> {

    private static final String NODESET = "NODESET";
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static final String SQLEXCEPTION = "SQLException: ";
    private final int seq;
    private final int feedid;
    private final String userid;
    private final String subnet;
    private int nodelist;
    private SortedSet<String> nodes;

    /**
     * Ingress route constructor.
     * @param seq squence number
     * @param feedid id for feed
     * @param user user name
     * @param subnet subnet string
     * @param nodes collection of nodes
     */
    public IngressRoute(int seq, int feedid, String user, String subnet, Collection<String> nodes) {
        this(seq, feedid, user, subnet);
        this.nodelist = -1;
        this.nodes = new TreeSet<>(nodes);
    }

    private IngressRoute(int seq, int feedid, String user, String subnet, int nodeset) {
        this(seq, feedid, user, subnet);
        this.nodelist = nodeset;
        this.nodes = new TreeSet<>(readNodes());
    }

    private IngressRoute(int seq, int feedid, String user, String subnet) {
        this.seq = seq;
        this.feedid = feedid;
        this.userid = (user == null) ? "-" : user;
        this.subnet = (subnet == null) ? "-" : subnet;
        this.nodelist = -1;
        this.nodes = null;
        if (Feed.getFeedById(feedid) == null) {
            throw new IllegalArgumentException("No such feed: " + feedid);
        }
        if (!"-".equals(this.subnet)) {
            SubnetMatcher sm = new SubnetMatcher(subnet);
            if (!sm.isValid()) {
                throw new IllegalArgumentException("Invalid subnet: " + subnet);
            }
        }
    }

    /**
     * Ingress route constructor.
     * @param jo JSONObject
     */
    public IngressRoute(JSONObject jo) {
        this.seq = jo.optInt("seq");
        this.feedid = jo.optInt("feedid");
        String user = jo.optString("user");
        this.userid = "".equals(user) ? "-" : user;
        user = jo.optString("subnet");
        this.subnet = "".equals(user) ? "-" : user;
        this.nodelist = -1;
        this.nodes = new TreeSet<>();
        JSONArray ja = jo.getJSONArray("node");
        for (int i = 0; i < ja.length(); i++) {
            this.nodes.add(ja.getString(i));
        }
    }


    /**
     * Get all IngressRoutes in the database, sorted in order according to their sequence field.
     *
     * @return a sorted set of IngressRoutes
     */
    public static SortedSet<IngressRoute> getAllIngressRoutes() {
        return getAllIngressRoutesForSQL("select SEQUENCE, FEEDID, USERID, SUBNET, NODESET from INGRESS_ROUTES");
    }

    /**
     * Get all IngressRoutes in the database with a particular sequence number.
     *
     * @param seq the sequence number
     * @return a set of IngressRoutes
     */
    public static Set<IngressRoute> getIngressRoutesForSeq(int seq) {
        return getAllIngressRoutesForSQL(
                "select SEQUENCE, FEEDID, USERID, SUBNET, NODESET from INGRESS_ROUTES where SEQUENCE = " + seq);
    }

    private static SortedSet<IngressRoute> getAllIngressRoutesForSQL(String sql) {
        SortedSet<IngressRoute> set = new TreeSet<>();
        try {
            Connection conn = DataSource.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    addIngressRouteToSet(set, rs);
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException | ClassNotFoundException e) {
            intlogger.error("PROV0001 getAllIngressRoutesForSQL: " + e.getMessage(), e);
        }
        return set;
    }

    private static void addIngressRouteToSet(SortedSet<IngressRoute> set, ResultSet rs) throws SQLException {
        while (rs.next()) {
            int seq = rs.getInt("SEQUENCE");
            int feedid = rs.getInt("FEEDID");
            String user = rs.getString("USERID");
            String subnet = rs.getString("SUBNET");
            int nodeset = rs.getInt(NODESET);
            set.add(new IngressRoute(seq, feedid, user, subnet, nodeset));
        }
    }

    /**
     * Get the maximum node set ID in use in the DB.
     *
     * @return the integer value of the maximum
     */
    private static int getMaxNodeSetID() {
        return getMax("select max(SETID) as MAX from NODESETS");
    }

    /**
     * Get the maximum node sequence number in use in the DB.
     *
     * @return the integer value of the maximum
     */
    public static int getMaxSequence() {
        return getMax("select max(SEQUENCE) as MAX from INGRESS_ROUTES");
    }

    private static int getMax(String sql) {
        int rv = 0;
        try (Connection conn = DataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    rv = rs.getInt("MAX");
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException | ClassNotFoundException e) {
            intlogger.error("PROV0002 getMax: " + e.getMessage(), e);
        }
        return rv;
    }

    /**
     * Get an Ingress Route for a particular feed ID, user, and subnet.
     *
     * @param feedid the Feed ID to look for
     * @param user the user name to look for
     * @param subnet the subnet to look for
     * @return the Ingress Route, or null of there is none
     */
    public static IngressRoute getIngressRoute(int feedid, String user, String subnet) {
        IngressRoute ir = null;
        String sql = "select SEQUENCE, NODESET from INGRESS_ROUTES where FEEDID = ? AND USERID = ? and SUBNET = ?";
        try (Connection conn = DataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, feedid);
            ps.setString(2, user);
            ps.setString(3, subnet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int seq = rs.getInt("SEQUENCE");
                    int nodeset = rs.getInt(NODESET);
                    ir = new IngressRoute(seq, feedid, user, subnet, nodeset);
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException | ClassNotFoundException e) {
            intlogger.error("PROV0003 getIngressRoute: " + e.getMessage(), e);
        }
        return ir;
    }

    /**
     * Does this particular IngressRoute match a request, represented by feedid and req? To match, <i>feedid</i> must
     * match the feed ID in the route, the user in the route (if specified) must match the user in the request, and the
     * subnet in the route (if specified) must match the subnet from the request.
     *
     * @param feedid the feedid for this request
     * @param req the remainder of the request
     * @return true if a match, false otherwise
     */
    public boolean matches(int feedid, HttpServletRequest req) {
        // Check feedid
        if (this.feedid != feedid) {
            return false;
        }
        // Get user from request and compare
        // Note: we don't check the password; the node will do that
        if (userid.length() > 0 && !"-".equals(userid)) {
            String credentials = req.getHeader("Authorization");
            if (credentials == null || !credentials.startsWith("Basic ")) {
                return false;
            }
            String cred = new String(Base64.decodeBase64(credentials.substring(6)));
            int ix = cred.indexOf(':');
            if (ix >= 0) {
                cred = cred.substring(0, ix);
            }
            if (!cred.equals(this.userid)) {
                return false;
            }
        }
        // If this route has a subnet, match it against the requester's IP addr
        if (subnet.length() > 0 && !"-".equals(subnet)) {
            try {
                InetAddress inet = InetAddress.getByName(req.getRemoteAddr());
                SubnetMatcher sm = new SubnetMatcher(subnet);
                return sm.matches(inet.getAddress());
            } catch (UnknownHostException e) {
                intlogger.error("PROV0008 matches: " + e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * Compare IP addresses as byte arrays to a subnet specified as a CIDR. Taken from
     * org.onap.dmaap.datarouter.node.SubnetMatcher and modified somewhat.
     */
    public class SubnetMatcher {

        private byte[] sn;
        private int len;
        private int mask;
        private boolean valid;

        /**
         * Construct a subnet matcher given a CIDR.
         *
         * @param subnet The CIDR to match
         */
        public SubnetMatcher(String subnet) {
            int index = subnet.lastIndexOf('/');
            if (index == -1) {
                try {
                    sn = InetAddress.getByName(subnet).getAddress();
                    len = sn.length;
                    valid = true;
                } catch (UnknownHostException e) {
                    intlogger.error("PROV0008 SubnetMatcher: " + e.getMessage(), e);
                    len = 0;
                    valid = false;
                }
                mask = 0;
            } else {
                int num = Integer.parseInt(subnet.substring(index + 1));
                try {
                    sn = InetAddress.getByName(subnet.substring(0, index)).getAddress();
                    valid = true;
                } catch (UnknownHostException e) {
                    intlogger.error("PROV0008 SubnetMatcher: " + e.getMessage(), e);
                    valid = false;
                }
                len = num / 8;
                mask = ((0xff00) >> (num % 8)) & 0xff;
            }
        }

        boolean isValid() {
            return valid;
        }

        /**
         * Is the IP address in the CIDR?.
         *
         * @param addr the IP address as bytes in network byte order
         * @return true if the IP address matches.
         */
        boolean matches(byte[] addr) {
            if (!valid || (addr.length != sn.length)) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                if (addr[i] != sn[i]) {
                    return false;
                }
            }
            if (mask != 0 && ((addr[len] ^ sn[len]) & mask) != 0) {
                return false;
            }
            return true;
        }
    }

    /**
     * Get the list of node names for this route.
     *
     * @return the list
     */
    public SortedSet<String> getNodes() {
        return this.nodes;
    }

    private Collection<String> readNodes() {
        Collection<String> set = new TreeSet<>();
        String sql = "select NODEID from NODESETS where SETID = ?";
        try (Connection conn = DataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, nodelist);
                addNodeToSet(set, ps);
            }
            DataSource.returnConnection(conn);
        } catch (SQLException | ClassNotFoundException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
        }
        return set;
    }

    private void addNodeToSet(Collection<String> set, PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("NODEID");
                set.add(lookupNodeID(id));
            }
        }
    }

    /**
     * Delete the IRT route having this IngressRoutes feed ID, user ID, and subnet from the database.
     *
     * @return true if the delete succeeded
     */
    @Override
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        try (PreparedStatement ps = conn.prepareStatement(
                 "delete from INGRESS_ROUTES where FEEDID = ? and USERID = ? and SUBNET = ?");
                PreparedStatement ps2 = conn.prepareStatement("delete from NODESETS where SETID = ?")) {
            // Delete the Ingress Route
            ps.setInt(1, feedid);
            ps.setString(2, userid);
            ps.setString(3, subnet);
            ps.execute();
            // Delete the NodeSet
            ps2.setInt(1, nodelist);
            ps2.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = false;
        try (PreparedStatement ps = conn.prepareStatement("insert into NODESETS (SETID, NODEID) values (?,?)");
                PreparedStatement ps2 = conn.prepareStatement("insert into INGRESS_ROUTES (SEQUENCE, FEEDID, USERID,"
                        + " SUBNET, NODESET) values (?, ?, ?, ?, ?)")) {
            // Create the NODESETS rows & set nodelist
            this.nodelist = getMaxNodeSetID() + 1;
            for (String node : nodes) {
                int id = lookupNodeName(node);
                ps.setInt(1, this.nodelist);
                ps.setInt(2, id);
                ps.execute();
            }
            // Create the INGRESS_ROUTES row
            ps2.setInt(1, this.seq);
            ps2.setInt(2, this.feedid);
            ps2.setString(3, this.userid);
            ps2.setString(4, this.subnet);
            ps2.setInt(5, this.nodelist);
            ps2.execute();
            rv = true;
        } catch (SQLException e) {
            intlogger.warn("PROV0005 doInsert: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection conn) {
        return doDelete(conn) && doInsert(conn);
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("feedid", feedid);
        // Note: for user and subnet, null, "", and "-" are equivalent
        if (userid != null && !"-".equals(userid) && !"".equals(userid)) {
            jo.put("user", userid);
        }
        if (subnet != null && !"-".equals(subnet) && !"".equals(subnet)) {
            jo.put("subnet", subnet);
        }
        jo.put("seq", seq);
        jo.put("node", nodes);
        return jo;
    }

    @Override
    public String getKey() {
        return String
                .format("%d/%s/%s/%d", feedid, (userid == null) ? "" : userid, (subnet == null) ? "" : subnet, seq);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IngressRoute)) {
            return false;
        }
        return this.compareTo((IngressRoute) obj) == 0;
    }

    @Override
    public int compareTo(IngressRoute in) {
        if (in == null) {
            throw new NullPointerException();
        }
        int num = this.feedid - in.feedid;
        if (num != 0) {
            return num;
        }
        num = this.seq - in.seq;
        if (num != 0) {
            return num;
        }
        num = this.userid.compareTo(in.userid);
        if (num != 0) {
            return num;
        }
        num = this.subnet.compareTo(in.subnet);
        if (num != 0) {
            return num;
        }
        return this.nodes.equals(in.nodes) ? 0 : 1;
    }

    @Override
    public String toString() {
        return String.format("INGRESS: feed=%d, userid=%s, subnet=%s, seq=%d", feedid, (userid == null) ? "" : userid,
                (subnet == null) ? "" : subnet, seq);
    }
}
