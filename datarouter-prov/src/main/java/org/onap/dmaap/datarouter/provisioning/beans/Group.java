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
import java.io.InvalidObjectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DataSource;

/**
 * The representation of a Subscription.  Subscriptions can be retrieved from the DB, or stored/updated in the DB.
 *
 * @author vikram
 * @version $Id: Group.java,v 1.0 2016/07/19
 */

public class Group extends Syncable {

    private static final String GROUP_ID_CONST = "groupid";
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static int nextGroupid = getMaxGroupID() + 1;
    private static final String SQLEXCEPTION = "SQLException: ";

    private int groupid;
    private String authid;
    private String name;
    private String description;
    private String classification;
    private String members;
    private Date lastMod;

    public Group() {
        this("", "", "");
    }

    /**
     * Group constructor.
     * @param name group name
     * @param desc group description
     * @param members group members
     */
    public Group(String name, String desc, String members) {
        this.groupid = -1;
        this.authid = "";
        this.name = name;
        this.description = desc;
        this.members = members;
        this.classification = "";
        this.lastMod = new Date();
    }


    /**
     * Group constructor from ResultSet.
     * @param rs ResultSet
     * @throws SQLException in case of SQL statement error
     */
    public Group(ResultSet rs) throws SQLException {
        this.groupid = rs.getInt("GROUPID");
        this.authid = rs.getString("AUTHID");
        this.name = rs.getString("NAME");
        this.description = rs.getString("DESCRIPTION");
        this.classification = rs.getString("CLASSIFICATION");
        this.members = rs.getString("MEMBERS");
        this.lastMod = rs.getDate("LAST_MOD");
    }

    /**
     * Group constructor for JSONObject.
     * @param jo JSONObject
     * @throws InvalidObjectException in case of JSON error
     */
    public Group(JSONObject jo) throws InvalidObjectException {
        this("", "", "");
        try {
            // The JSONObject is assumed to contain a vnd.dmaap-dr.group representation
            this.groupid = jo.optInt(GROUP_ID_CONST, -1);
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
            intlogger.warn("Invalid JSON: " + e.getMessage(), e);
            throw new InvalidObjectException("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Get a group frpm DB.
     * @param gup group object
     * @return Group object
     */
    public static Group getGroupMatching(Group gup) {
        String sql = String.format(
                "select * from GROUPS where NAME='%s'",
                gup.getName()
        );
        List<Group> list = getGroupsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * Get a group from DB using name and groupid.
     * @param gup group object
     * @param groupid id of group
     * @return group object
     */
    public static Group getGroupMatching(Group gup, int groupid) {
        String sql = String.format(
                "select * from GROUPS where  NAME = '%s' and GROUPID != %d ", gup.getName(), gup.getGroupid());
        List<Group> list = getGroupsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * Get group from DB using groupid only.
     * @param id id of group
     * @return group object
     */
    public static Group getGroupById(int id) {
        String sql = "select * from GROUPS where GROUPID = " + id;
        List<Group> list = getGroupsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * Get group from DB using AUTHID.
     * @param id AUTHID
     * @return group object
     */
    static Group getGroupByAuthId(String id) {
        String sql = "select * from GROUPS where AUTHID = '" + id + "'";
        List<Group> list = getGroupsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    public static Collection<Group> getAllgroups() {
        return getGroupsForSQL("select * from GROUPS");
    }

    private static List<Group> getGroupsForSQL(String sql) {
        List<Group> list = new ArrayList<>();
        try {
            Connection conn = DataSource.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Group group = new Group(rs);
                        list.add(group);
                    }
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException e) {
            intlogger.error("PROV0009 getGroupsForSQL: " + e.getMessage(), e);
        }
        return list;
    }

    private static int getMaxGroupID() {
        int max = 0;
        try {
            Connection conn = DataSource.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select MAX(groupid) from GROUPS")) {
                    if (rs.next()) {
                        max = rs.getInt(1);
                    }
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException e) {
            intlogger.info("PROV0001 getMaxSubID: " + e.getMessage(), e);
        }
        return max;
    }

    public int getGroupid() {
        return groupid;
    }

    public static EELFLogger getIntlogger() {
        return intlogger;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public static void setIntlogger(EELFLogger intlogger) {
        Group.intlogger = intlogger;
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

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put(GROUP_ID_CONST, groupid);
        jo.put("authid", authid);
        jo.put("name", name);
        jo.put("description", description);
        jo.put("classification", classification);
        jo.put("members", members);
        jo.put("last_mod", lastMod.getTime());
        return jo;
    }

    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            if (groupid == -1) {
                // No feed ID assigned yet, so assign the next available one
                setGroupid(nextGroupid++);
            }
            // In case we insert a gropup from synchronization
            if (groupid > nextGroupid) {
                nextGroupid = groupid + 1;
            }

            // Create the GROUPS row
            String sql = "insert into GROUPS (GROUPID, AUTHID, NAME, DESCRIPTION, CLASSIFICATION, MEMBERS) "
                                 + "values (?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"GROUPID"});
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
    public boolean doUpdate(Connection conn) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "update GROUPS set AUTHID = ?, NAME = ?, DESCRIPTION = ?, CLASSIFICATION = ? ,  MEMBERS = ? "
                                 + "where GROUPID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, authid);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, classification);
            ps.setString(5, members);
            ps.setInt(6, groupid);
            ps.executeUpdate();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(), e);
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
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "delete from GROUPS where GROUPID = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, groupid);
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
        return Objects.hash(groupid, authid, name, description, classification, members, lastMod);
    }
}
