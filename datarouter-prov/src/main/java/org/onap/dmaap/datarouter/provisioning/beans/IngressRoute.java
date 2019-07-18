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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DB;

/**
 * The representation of one route in the Ingress Route Table.
 *
 * @author Robert P. Eby
 * @version $Id: IngressRoute.java,v 1.3 2013/12/16 20:30:23 eby Exp $
 */
public class IngressRoute extends NodeClass implements Comparable<IngressRoute> {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static final String SQLEXCEPTION = "SQLException: ";
    private final int seq;
    private final int feedid;
    private final String userid;
    private final String subnet;
    private int nodelist;
    private SortedSet<String> nodes;

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
        SortedSet<IngressRoute> set = new TreeSet<IngressRoute>();
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int seq = rs.getInt("SEQUENCE");
                        int feedid = rs.getInt("FEEDID");
                        String user = rs.getString("USERID");
                        String subnet = rs.getString("SUBNET");
                        int nodeset = rs.getInt("NODESET");
                        set.add(new IngressRoute(seq, feedid, user, subnet, nodeset));
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("PROV0001 getAllIngressRoutesForSQL: " + e.getMessage(), e);
        }
        return set;
    }

    /**
     * Get the maximum node set ID in use in the DB.
     *
     * @return the integer value of the maximum
     */
    public static int getMaxNodeSetID() {
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
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        rv = rs.getInt("MAX");
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("PROV0002 getMax: " + e.getMessage(), e);
        }
        return rv;
    }

    /**
     * Get an Ingress Route for a particular feed ID, user, and subnet
     *
     * @param feedid the Feed ID to look for
     * @param user the user name to look for
     * @param subnet the subnet to look for
     * @return the Ingress Route, or null of there is none
     */
    public static IngressRoute getIngressRoute(int feedid, String user, String subnet) {
        IngressRoute v = null;
        PreparedStatement ps = null;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            String sql = "select SEQUENCE, NODESET from INGRESS_ROUTES where FEEDID = ? AND USERID = ? and SUBNET = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, feedid);
            ps.setString(2, user);
            ps.setString(3, subnet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int seq = rs.getInt("SEQUENCE");
                    int nodeset = rs.getInt("NODESET");
                    v = new IngressRoute(seq, feedid, user, subnet, nodeset);
                }
            }
            ps.close();
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("PROV0003 getIngressRoute: " + e.getMessage(), e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error(SQLEXCEPTION + e.getMessage(), e);
            }
        }
        return v;
    }

    /**
     * Get a collection of all Ingress Routes with a particular sequence number.
     *
     * @param seq the sequence number to look for
     * @return the collection (may be empty).
     */
    public static Collection<IngressRoute> getIngressRoute(int seq) {
        Collection<IngressRoute> rv = new ArrayList<IngressRoute>();
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            String sql = "select FEEDID, USERID, SUBNET, NODESET from INGRESS_ROUTES where SEQUENCE = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, seq);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int feedid = rs.getInt("FEEDID");
                        String user = rs.getString("USERID");
                        String subnet = rs.getString("SUBNET");
                        int nodeset = rs.getInt("NODESET");
                        rv.add(new IngressRoute(seq, feedid, user, subnet, nodeset));
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("PROV0004 getIngressRoute: " + e.getMessage(), e);
        }
        return rv;
    }

    public IngressRoute(int seq, int feedid, String user, String subnet, Collection<String> nodes)
            throws IllegalArgumentException {
        this(seq, feedid, user, subnet);
        this.nodelist = -1;
        this.nodes = new TreeSet<String>(nodes);
    }

    public IngressRoute(int seq, int feedid, String user, String subnet, int nodeset)
            throws IllegalArgumentException {
        this(seq, feedid, user, subnet);
        this.nodelist = nodeset;
        this.nodes = new TreeSet<String>(readNodes());
    }

    private IngressRoute(int seq, int feedid, String user, String subnet)
            throws IllegalArgumentException {
        this.seq = seq;
        this.feedid = feedid;
        this.userid = (user == null) ? "-" : user;
        this.subnet = (subnet == null) ? "-" : subnet;
        this.nodelist = -1;
        this.nodes = null;
        if (Feed.getFeedById(feedid) == null) {
            throw new IllegalArgumentException("No such feed: " + feedid);
        }
        if (!this.subnet.equals("-")) {
            SubnetMatcher sm = new SubnetMatcher(subnet);
            if (!sm.isValid()) {
                throw new IllegalArgumentException("Invalid subnet: " + subnet);
            }
        }
    }

    public IngressRoute(JSONObject jo) {
        this.seq = jo.optInt("seq");
        this.feedid = jo.optInt("feedid");
        String t = jo.optString("user");
        this.userid = t.equals("") ? "-" : t;
        t = jo.optString("subnet");
        this.subnet = t.equals("") ? "-" : t;
        this.nodelist = -1;
        this.nodes = new TreeSet<String>();
        JSONArray ja = jo.getJSONArray("node");
        for (int i = 0; i < ja.length(); i++) {
            this.nodes.add(ja.getString(i));
        }
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
        if (userid.length() > 0 && !userid.equals("-")) {
            String credentials = req.getHeader("Authorization");
            if (credentials == null || !credentials.startsWith("Basic ")) {
                return false;
            }
            String t = new String(Base64.decodeBase64(credentials.substring(6)));
            int ix = t.indexOf(':');
            if (ix >= 0) {
                t = t.substring(0, ix);
            }
            if (!t.equals(this.userid)) {
                return false;
            }
        }

        // If this route has a subnet, match it against the requester's IP addr
        if (subnet.length() > 0 && !subnet.equals("-")) {
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
         * Construct a subnet matcher given a CIDR
         *
         * @param subnet The CIDR to match
         */
        public SubnetMatcher(String subnet) {
            int i = subnet.lastIndexOf('/');
            if (i == -1) {
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
                int n = Integer.parseInt(subnet.substring(i + 1));
                try {
                    sn = InetAddress.getByName(subnet.substring(0, i)).getAddress();
                    valid = true;
                } catch (UnknownHostException e) {
                    intlogger.error("PROV0008 SubnetMatcher: " + e.getMessage(), e);
                    valid = false;
                }
                len = n / 8;
                mask = ((0xff00) >> (n % 8)) & 0xff;
            }
        }

        public boolean isValid() {
            return valid;
        }

        /**
         * Is the IP address in the CIDR?
         *
         * @param addr the IP address as bytes in network byte order
         * @return true if the IP address matches.
         */
        public boolean matches(byte[] addr) {
            if (!valid || addr.length != sn.length) {
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
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            String sql = "select NODEID from NODESETS where SETID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, nodelist);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("NODEID");
                        set.add(lookupNodeID(id));
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
        }
        return set;
    }

    /**
     * Delete the IRT route having this IngressRoutes feed ID, user ID, and subnet from the database.
     *
     * @return true if the delete succeeded
     */
    @Override
    public boolean doDelete(Connection c) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("delete from INGRESS_ROUTES where FEEDID = ? and USERID = ? and SUBNET = ?");
            ps.setInt(1, feedid);
            ps.setString(2, userid);
            ps.setString(3, subnet);
            ps.execute();
            ps.close();

            ps = c.prepareStatement("delete from NODESETS where SETID = ?");
            ps.setInt(1, nodelist);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage(), e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error(SQLEXCEPTION + e.getMessage(), e);
            }
        }
        return rv;
    }

    @SuppressWarnings("resource")
    @Override
    public boolean doInsert(Connection c) {
        boolean rv = false;
        PreparedStatement ps = null;
        try {
            // Create the NODESETS rows & set nodelist
            int set = getMaxNodeSetID() + 1;
            this.nodelist = set;
            for (String node : nodes) {
                int id = lookupNodeName(node);
                ps = c.prepareStatement("insert into NODESETS (SETID, NODEID) values (?,?)");
                ps.setInt(1, this.nodelist);
                ps.setInt(2, id);
                ps.execute();
                ps.close();
            }

            // Create the INGRESS_ROUTES row
            ps = c.prepareStatement(
                    "insert into INGRESS_ROUTES (SEQUENCE, FEEDID, USERID, SUBNET, NODESET) values (?, ?, ?, ?, ?)");
            ps.setInt(1, this.seq);
            ps.setInt(2, this.feedid);
            ps.setString(3, this.userid);
            ps.setString(4, this.subnet);
            ps.setInt(5, this.nodelist);
            ps.execute();
            ps.close();
            rv = true;
        } catch (SQLException e) {
            intlogger.warn("PROV0005 doInsert: " + e.getMessage(), e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error(SQLEXCEPTION + e.getMessage(), e);
            }
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection c) {
        return doDelete(c) && doInsert(c);
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("feedid", feedid);
        // Note: for user and subnet, null, "", and "-" are equivalent
        if (userid != null && !userid.equals("-") && !userid.equals("")) {
            jo.put("user", userid);
        }
        if (subnet != null && !subnet.equals("-") && !subnet.equals("")) {
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
        int n = this.feedid - in.feedid;
        if (n != 0) {
            return n;
        }
        n = this.seq - in.seq;
        if (n != 0) {
            return n;
        }
        n = this.userid.compareTo(in.userid);
        if (n != 0) {
            return n;
        }
        n = this.subnet.compareTo(in.subnet);
        if (n != 0) {
            return n;
        }
        return this.nodes.equals(in.nodes) ? 0 : 1;
    }

    @Override
    public String toString() {
        return String.format("INGRESS: feed=%d, userid=%s, subnet=%s, seq=%d", feedid, (userid == null) ? "" : userid,
                (subnet == null) ? "" : subnet, seq);
    }
}