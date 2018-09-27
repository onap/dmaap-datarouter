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

import java.io.InvalidObjectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;

/**
 * The representation of a Subscription.  Subscriptions can be retrieved from the DB, or stored/updated in the DB.
 *
 * @author vikram
 * @version $Id: Group.java,v 1.0 2016/07/19
 */
public class Group extends Syncable {

    private static Logger intlogger = Logger.getLogger("org.onap.dmaap.datarouter.provisioning.internal");
    private static int next_groupid = getMaxGroupID() + 1;

    private int groupid;
    private String authid;
    private String name;
    private String description;
    private String classification;
    private String members;
    private Date last_mod;


    public static Group getGroupMatching(Group gup) {
        String sql = String.format(
                "select * from GROUPS where NAME='%s'",
                gup.getName()
        );
        List<Group> list = getGroupsForSQL(sql);
        return list.size() > 0 ? list.get(0) : null;
    }

    public static Group getGroupMatching(Group gup, int groupid) {
        String sql = String.format(
                "select * from GROUPS where  NAME = '%s' and GROUPID != %d ",
                gup.getName(),
                gup.getGroupid()
        );
        List<Group> list = getGroupsForSQL(sql);
        return list.size() > 0 ? list.get(0) : null;
    }

    public static Group getGroupById(int id) {
        String sql = "select * from GROUPS where GROUPID = " + id;
        List<Group> list = getGroupsForSQL(sql);
        return list.size() > 0 ? list.get(0) : null;
    }

    public static Group getGroupByAuthId(String id) {
        String sql = "select * from GROUPS where AUTHID = '" + id + "'";
        List<Group> list = getGroupsForSQL(sql);
        return list.size() > 0 ? list.get(0) : null;
    }

    public static Collection<Group> getAllgroups() {
        return getGroupsForSQL("select * from GROUPS");
    }

    private static List<Group> getGroupsForSQL(String sql) {
        List<Group> list = new ArrayList<Group>();
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Group group = new Group(rs);
                        list.add(group);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("SQLException " + e.getMessage());
        }
        return list;
    }

    public static int getMaxGroupID() {
        int max = 0;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select MAX(groupid) from GROUPS")) {
                    if (rs.next()) {
                        max = rs.getInt(1);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.info("getMaxSubID: " + e.getMessage());
        }
        return max;
    }

    public static Collection<String> getGroupsByClassfication(String classfication) {
        List<String> list = new ArrayList<>();
        String sql = "select * from GROUPS where classification = ?";
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, classfication);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int groupid = rs.getInt("groupid");

                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.error("SQLException " + e.getMessage());
        }
        return list;
    }

    /**
     * Return a count of the number of active subscriptions in the DB.
     *
     * @return the count
     */
    public static int countActiveSubscriptions() {
        int count = 0;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select count(*) from SUBSCRIPTIONS")) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0008 countActiveSubscriptions: " + e.getMessage());
        }
        return count;
    }

    public Group() {
        this("", "", "");
    }

    public Group(String name, String desc, String members) {
        this.groupid = -1;
        this.authid = "";
        this.name = name;
        this.description = desc;
        this.members = members;
        this.classification = "";
        this.last_mod = new Date();
    }


    public Group(ResultSet rs) throws SQLException {
        this.groupid = rs.getInt("GROUPID");
        this.authid = rs.getString("AUTHID");
        this.name = rs.getString("NAME");
        this.description = rs.getString("DESCRIPTION");
        this.classification = rs.getString("CLASSIFICATION");
        this.members = rs.getString("MEMBERS");
        this.last_mod = rs.getDate("LAST_MOD");
    }


    public Group(JSONObject jo) throws InvalidObjectException {
        this("", "", "");
        try {
            // The JSONObject is assumed to contain a vnd.att-dr.group representation
            this.groupid = jo.optInt("groupid", -1);
            String gname = jo.getString("name");
            String gdescription = jo.getString("description");

            this.authid = jo.getString("authid");
            this.name = gname;
            this.description = gdescription;
            this.classification = jo.getString("classification");
            this.members = jo.getString("members");

            if (gname.length() > 50) {
                throw new InvalidObjectException("Group name is too long");
            }
            if (gdescription.length() > 256) {
                throw new InvalidObjectException("Group Description is too long");
            }
        } catch (InvalidObjectException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidObjectException("invalid JSON: " + e.getMessage());
        }
    }

    public int getGroupid() {
        return groupid;
    }

    public static Logger getIntlogger() {
        return intlogger;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public static void setIntlogger(Logger intlogger) {
        Group.intlogger = intlogger;
    }

    public static int getNext_groupid() {
        return next_groupid;
    }

    public static void setNext_groupid(int next_groupid) {
        Group.next_groupid = next_groupid;
    }

    public String getAuthid() {
        return authid;
    }

    public void setAuthid(String authid) {
        this.authid = authid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getMembers() {
        return members;
    }

    public void setMembers(String members) {
        this.members = members;
    }

    public Date getLast_mod() {
        return last_mod;
    }

    public void setLast_mod(Date last_mod) {
        this.last_mod = last_mod;
    }


    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("groupid", groupid);
        jo.put("authid", authid);
        jo.put("name", name);
        jo.put("description", description);
        jo.put("classification", classification);
        jo.put("members", members);
        jo.put("last_mod", last_mod.getTime());
        return jo;
    }

    @Override
    public boolean doInsert(Connection c) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            if (groupid == -1) {
                // No feed ID assigned yet, so assign the next available one
                setGroupid(next_groupid++);
            }
            // In case we insert a gropup from synchronization
            if (groupid > next_groupid) {
                next_groupid = groupid + 1;
            }

            // Create the GROUPS row
            String sql = "insert into GROUPS (GROUPID, AUTHID, NAME, DESCRIPTION, CLASSIFICATION, MEMBERS) values (?, ?, ?, ?, ?, ?)";
            ps = c.prepareStatement(sql, new String[]{"GROUPID"});
            ps.setInt(1, groupid);
            ps.setString(2, authid);
            ps.setString(3, name);
            ps.setString(4, description);
            ps.setString(5, classification);
            ps.setString(6, members);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0005 doInsert: " + e.getMessage());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error("SQLException " + e.getMessage());
            }
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection c) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "update GROUPS set AUTHID = ?, NAME = ?, DESCRIPTION = ?, CLASSIFICATION = ? ,  MEMBERS = ? where GROUPID = ?";
            ps = c.prepareStatement(sql);
            ps.setString(1, authid);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, classification);
            ps.setString(5, members);
            ps.setInt(6, groupid);
            ps.executeUpdate();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error("SQLException " + e.getMessage());
            }
        }
        return rv;
    }

    @Override
    public boolean doDelete(Connection c) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "delete from GROUPS where GROUPID = ?";
            ps = c.prepareStatement(sql);
            ps.setInt(1, groupid);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error("SQLException " + e.getMessage());
            }
        }
        return rv;
    }

    @Override
    public String getKey() {
        return "" + getGroupid();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Group)) {
            return false;
        }
        Group os = (Group) obj;
        if (groupid != os.groupid) {
            return false;
        }
        if (authid != os.authid) {
            return false;
        }
        if (!name.equals(os.name)) {
            return false;
        }
        if (description != os.description) {
            return false;
        }
        if (!classification.equals(os.classification)) {
            return false;
        }
        if (!members.equals(os.members)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "GROUP: groupid=" + groupid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupid, authid, name, description, classification, members, last_mod);
    }
}
