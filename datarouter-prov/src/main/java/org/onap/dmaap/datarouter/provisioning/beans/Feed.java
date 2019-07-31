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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.JSONUtilities;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;

import java.io.InvalidObjectException;
import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * The representation of a Feed.  Feeds can be retrieved from the DB, or stored/updated in the DB.
 *
 * @author Robert Eby
 * @version $Id: Feed.java,v 1.13 2013/10/28 18:06:52 eby Exp $
 */
public class Feed extends Syncable {
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static int next_feedid = getMaxFeedID() + 1;
    private static final String SQLEXCEPTION = "SQLException: ";

    private int feedid;
    private int groupid; //New field is added - Groups feature Rally:US708115 - 1610
    private String name;
    private String version;
    private String description;
    private String business_description; // New field is added - Groups feature Rally:US708102 - 1610
    private FeedAuthorization authorization;
    private String publisher;
    private FeedLinks links;
    private boolean deleted;
    private boolean suspended;
    private Date last_mod;
    private Date created_date;
    private String aaf_instance;

    /**
     * Check if a feed ID is valid.
     *
     * @param id the Feed ID
     * @return true if it is valid
     */
    @SuppressWarnings("resource")
    public static boolean isFeedValid(int id) {
        int count = 0;
        try {
            DB db = new DB();
            Connection conn = db.getConnection();
            try(PreparedStatement stmt = conn.prepareStatement("select COUNT(*) from FEEDS where FEEDID = ?")) {
                stmt.setInt(1, id);
                try(ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0024 Feed.isFeedValid: " + e.getMessage(), e);
        }
        return count != 0;
    }

    /**
     * Get a specific feed from the DB, based upon its ID.
     *
     * @param id the Feed ID
     * @return the Feed object, or null if it does not exist
     */
    public static Feed getFeedById(int id) {
        String sql = "select * from FEEDS where FEEDID = " + id;
        return getFeedBySQL(sql);
    }

    /**
     * Get a specific feed from the DB, based upon its name and version.
     *
     * @param name    the name of the Feed
     * @param version the version of the Feed
     * @return the Feed object, or null if it does not exist
     */
    public static Feed getFeedByNameVersion(String name, String version) {
        name = name.replaceAll("'", "''");
        version = version.replaceAll("'", "''");
        String sql = "select * from FEEDS where NAME = '" + name + "' and VERSION ='" + version + "'";
        return getFeedBySQL(sql);
    }

    /**
     * Return a count of the number of active feeds in the DB.
     *
     * @return the count
     */
    public static int countActiveFeeds() {
        int count = 0;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try(Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select count(*) from FEEDS where DELETED = 0")) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0025 Feed.countActiveFeeds: " + e.getMessage(), e);
        }
        return count;
    }

    public static int getMaxFeedID() {
        int max = 0;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try(Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select MAX(feedid) from FEEDS")) {
                    if (rs.next()) {
                        max = rs.getInt(1);
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0026 Feed.getMaxFeedID: " + e.getMessage(), e);
        }
        return max;
    }

    public static Collection<Feed> getAllFeeds() {
        Map<Integer, Feed> map = new HashMap<>();
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try(Statement stmt = conn.createStatement()) {
                try(ResultSet rs = stmt.executeQuery("select * from FEEDS")) {
                    while (rs.next()) {
                        Feed feed = new Feed(rs);
                        map.put(feed.getFeedid(), feed);
                    }
                }

                String sql = "select * from FEED_ENDPOINT_IDS";
                try(ResultSet rs = stmt.executeQuery(sql)){
                    while (rs.next()) {
                        int id = rs.getInt("FEEDID");
                        Feed feed = map.get(id);
                        if (feed != null) {
                            FeedEndpointID epi = new FeedEndpointID(rs);
                            Collection<FeedEndpointID> ecoll = feed.getAuthorization().getEndpointIDS();
                            ecoll.add(epi);
                        }
                    }
                }

                sql = "select * from FEED_ENDPOINT_ADDRS";
                try(ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int id = rs.getInt("FEEDID");
                        Feed feed = map.get(id);
                        if (feed != null) {
                            Collection<String> acoll = feed.getAuthorization().getEndpointAddrs();
                            acoll.add(rs.getString("ADDR"));
                        }
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0027 Feed.getAllFeeds: " + e.getMessage(), e);
        }
        return map.values();
    }

    public static List<String> getFilteredFeedUrlList(final String name, final String val) {
        List<String> list = new ArrayList<>();
        String sql = "select SELF_LINK from FEEDS where DELETED = 0";
        if (name.equals("name")) {
            sql += " and NAME = ?";
        } else if (name.equals("publ")) {
            sql += " and PUBLISHER = ?";
        } else if (name.equals("subs")) {
            sql = "select distinct FEEDS.SELF_LINK from FEEDS, SUBSCRIPTIONS " +
                    "where DELETED = 0 " +
                    "and FEEDS.FEEDID = SUBSCRIPTIONS.FEEDID " +
                    "and SUBSCRIPTIONS.SUBSCRIBER = ?";
        }
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                if (sql.indexOf('?') >= 0)
                    ps.setString(1, val);
                try(ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String t = rs.getString(1);
                        list.add(t.trim());
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0028 Feed.getFilteredFeedUrlList: " + e.getMessage(), e);
        }
        return list;
    }

    @SuppressWarnings("resource")
    private static Feed getFeedBySQL(String sql) {
        Feed feed = null;
        try {
            DB db = new DB();
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        feed = new Feed(rs);
                    }
                }
                if (feed != null) {
                    sql = "select * from FEED_ENDPOINT_IDS where FEEDID = " + feed.feedid;
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        Collection<FeedEndpointID> ecoll = feed.getAuthorization().getEndpointIDS();
                        while (rs.next()) {
                            FeedEndpointID epi = new FeedEndpointID(rs);
                            ecoll.add(epi);
                        }
                    }
                    sql = "select * from FEED_ENDPOINT_ADDRS where FEEDID = " + feed.feedid;
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        Collection<String> acoll = feed.getAuthorization().getEndpointAddrs();
                        while (rs.next()) {
                            acoll.add(rs.getString("ADDR"));
                        }
                    }
                }
            }
            db.release(conn);
        } catch (SQLException e) {
            intlogger.warn("PROV0029 Feed.getFeedBySQL: " + e.getMessage(), e);
        }
        return feed;
    }

    public Feed() {
        this("", "", "", "");
    }

    public Feed(String name, String version, String desc, String business_description) {
        this.feedid = -1;
        this.groupid = -1; //New field is added - Groups feature Rally:US708115 - 1610
        this.name = name;
        this.version = version;
        this.description = desc;
        this.business_description = business_description; // New field is added - Groups feature Rally:US708102 - 1610
        this.authorization = new FeedAuthorization();
        this.publisher = "";
        this.links = new FeedLinks();
        this.deleted = false;
        this.suspended = false;
        this.last_mod = new Date();
        this.created_date = new Date();
        this.aaf_instance = "";
    }

    public Feed(ResultSet rs) throws SQLException {
        this.feedid = rs.getInt("FEEDID");
        this.groupid = rs.getInt("GROUPID"); //New field is added - Groups feature Rally:US708115 - 1610
        this.name = rs.getString("NAME");
        this.version = rs.getString("VERSION");
        this.description = rs.getString("DESCRIPTION");
        this.business_description = rs.getString("BUSINESS_DESCRIPTION"); // New field is added - Groups feature Rally:US708102 - 1610
        this.authorization = new FeedAuthorization();
        this.authorization.setClassification(rs.getString("AUTH_CLASS"));
        this.publisher = rs.getString("PUBLISHER");
        this.links = new FeedLinks();
        this.links.setSelf(rs.getString("SELF_LINK"));
        this.links.setPublish(rs.getString("PUBLISH_LINK"));
        this.links.setSubscribe(rs.getString("SUBSCRIBE_LINK"));
        this.links.setLog(rs.getString("LOG_LINK"));
        this.deleted = rs.getBoolean("DELETED");
        this.suspended = rs.getBoolean("SUSPENDED");
        this.last_mod = rs.getDate("LAST_MOD");
        this.created_date = rs.getTimestamp("CREATED_DATE");
        this.aaf_instance = rs.getString("AAF_INSTANCE");
    }

    public Feed(JSONObject jo) throws InvalidObjectException {
        this("", "", "", "");
        try {
            // The JSONObject is assumed to contain a vnd.dmaap-dr.feed representation
            this.feedid = jo.optInt("feedid", -1);
            this.groupid = jo.optInt("groupid");
            this.name = jo.getString("name");
            this.aaf_instance = jo.optString("aaf_instance", "legacy");
            if(!(aaf_instance.equalsIgnoreCase("legacy")) && aaf_instance.length() > 255){
                    throw new InvalidObjectException("aaf_instance field is too long");
            }
            if (name.length() > 255)
                throw new InvalidObjectException("name field is too long");
            try {
                this.version = jo.getString("version");
            } catch (JSONException e) {
                intlogger.warn("PROV0023 Feed.Feed: " + e.getMessage(), e);
                this.version = null;
            }
            if(version != null && version.length() > 20)
                throw new InvalidObjectException("version field is too long");
            this.description = jo.optString("description");
            this.business_description = jo.optString("business_description");
            if (description.length() > 1000)
                throw new InvalidObjectException("technical description field is too long");
            if (business_description.length() > 1000)
                throw new InvalidObjectException("business description field is too long");
            this.authorization = new FeedAuthorization();
            JSONObject jauth = jo.getJSONObject("authorization");
            this.authorization.setClassification(jauth.getString("classification"));
            if (this.authorization.getClassification().length() > 32)
                throw new InvalidObjectException("classification field is too long");
            JSONArray endPointIds = jauth.getJSONArray("endpoint_ids");
            for (int i = 0; i < endPointIds.length(); i++) {
                JSONObject id = endPointIds.getJSONObject(i);
                FeedEndpointID fid = new FeedEndpointID(id.getString("id"), id.getString("password"));
                if (fid.getId().length() > 60)
                    throw new InvalidObjectException("id field is too long (" + fid.getId() + ")");
                if (fid.getPassword().length() > 32)
                    throw new InvalidObjectException("password field is too long ("+ fid.getPassword()+")");  //Fortify scan fixes - Privacy Violation
                this.authorization.getEndpointIDS().add(fid);
            }
            if (this.authorization.getEndpointIDS().isEmpty())
                throw new InvalidObjectException("need to specify at least one endpoint_id");
            endPointIds = jauth.getJSONArray("endpoint_addrs");
            for (int i = 0; i < endPointIds.length(); i++) {
                String addr = endPointIds.getString(i);
                if (!JSONUtilities.validIPAddrOrSubnet(addr))
                    throw new InvalidObjectException("bad IP addr or subnet mask: " + addr);
                this.authorization.getEndpointAddrs().add(addr);
            }

            this.publisher = jo.optString("publisher", "");
            this.deleted = jo.optBoolean("deleted", false);
            this.suspended = jo.optBoolean("suspend", false);
            JSONObject jol = jo.optJSONObject("links");
            this.links = (jol == null) ? (new FeedLinks()) : (new FeedLinks(jol));
        } catch (InvalidObjectException e) {
            throw e;
        } catch (Exception e) {
            intlogger.warn("Invalid JSON: " + e.getMessage(), e);
            throw new InvalidObjectException("Invalid JSON: " + e.getMessage());
        }
    }

    public int getFeedid() {
        return feedid;
    }

    public void setFeedid(int feedid) {
        this.feedid = feedid;

        // Create link URLs
        FeedLinks fl = getLinks();
        fl.setSelf(URLUtilities.generateFeedURL(feedid));
        fl.setPublish(URLUtilities.generatePublishURL(feedid));
        fl.setSubscribe(URLUtilities.generateSubscribeURL(feedid));
        fl.setLog(URLUtilities.generateFeedLogURL(feedid));
    }

    public String getAafInstance() {
        return aaf_instance;
    }

    public void setAaf_instance(String aaf_instance) {
        this.aaf_instance = aaf_instance;
    }

    //new getter setters for groups- Rally:US708115 - 1610
    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // New field is added - Groups feature Rally:US708102 - 1610
    public String getBusiness_description() {
        return business_description;
    }

    public void setBusiness_description(String business_description) {
        this.business_description = business_description;
    }

    public FeedAuthorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(FeedAuthorization authorization) {
        this.authorization = authorization;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        if (publisher != null) {
            if (publisher.length() > 8)
                publisher = publisher.substring(0, 8);
            this.publisher = publisher;
        }
    }

    public FeedLinks getLinks() {
        return links;
    }

    public void setLinks(FeedLinks links) {
        this.links = links;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("feedid", feedid);
        jo.put("groupid", groupid); //New field is added - Groups feature Rally:US708115 - 1610
        jo.put("name", name);
        jo.put("version", version);
        jo.put("description", description);
        jo.put("business_description", business_description); // New field is added - Groups feature Rally:US708102 - 1610
        jo.put("authorization", authorization.asJSONObject());
        jo.put("publisher", publisher);
        jo.put("links", links.asJSONObject());
        jo.put("deleted", deleted);
        jo.put("suspend", suspended);
        jo.put("last_mod", last_mod.getTime());
        jo.put("created_date", created_date.getTime());
        jo.put("aaf_instance", aaf_instance);
        return jo;
    }

    public JSONObject asLimitedJSONObject() {
        JSONObject jo = asJSONObject();
        jo.remove("deleted");
        jo.remove("feedid");
        jo.remove("last_mod");
        jo.remove("created_date");
        return jo;
    }

    public JSONObject asJSONObject(boolean hidepasswords) {
        JSONObject jo = asJSONObject();
        if (hidepasswords) {
            jo.remove("feedid");    // we no longer hide passwords, however we do hide these
            jo.remove("deleted");
            jo.remove("last_mod");
            jo.remove("created_date");
        }
        return jo;
    }

    @Override
    public boolean doDelete(Connection c) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "delete from FEEDS where FEEDID = ?";
            ps = c.prepareStatement(sql);
            ps.setInt(1, feedid);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.error("PROV0007 doDelete: " + e.getMessage(), e);
        } finally {
            try {
                if(ps!=null) {
                    ps.close();
                }
            } catch (SQLException e) {
                intlogger.error(SQLEXCEPTION + e.getMessage(), e);
            }
        }
        return rv;
    }

    @Override
    public synchronized boolean doInsert(Connection c) {
        boolean rv = true;
        try {
            if (feedid == -1) {
                setFeedid(next_feedid++);
            }
            // In case we insert a feed from synchronization
            if (feedid > next_feedid)
                next_feedid = feedid + 1;

            // Create FEED_ENDPOINT_IDS rows
            FeedAuthorization auth = getAuthorization();
            String sql = "insert into FEED_ENDPOINT_IDS values (?, ?, ?)";
            try(PreparedStatement ps2 = c.prepareStatement(sql)) {
                for (FeedEndpointID fid : auth.getEndpointIDS()) {
                    ps2.setInt(1, feedid);
                    ps2.setString(2, fid.getId());
                    ps2.setString(3, fid.getPassword());
                    ps2.executeUpdate();
                }
            }

            // Create FEED_ENDPOINT_ADDRS rows
            sql = "insert into FEED_ENDPOINT_ADDRS values (?, ?)";
            try(PreparedStatement ps2 = c.prepareStatement(sql)) {
                for (String t : auth.getEndpointAddrs()) {
                    ps2.setInt(1, feedid);
                    ps2.setString(2, t);
                    ps2.executeUpdate();
                }
            }

            // Finally, create the FEEDS row
            sql = "insert into FEEDS (FEEDID, NAME, VERSION, DESCRIPTION, AUTH_CLASS, PUBLISHER, SELF_LINK, PUBLISH_LINK, SUBSCRIBE_LINK, LOG_LINK, DELETED, SUSPENDED,BUSINESS_DESCRIPTION, GROUPID, AAF_INSTANCE) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try(PreparedStatement ps2 = c.prepareStatement(sql)) {
                ps2.setInt(1, feedid);
                ps2.setString(2, getName());
                ps2.setString(3, getVersion());
                ps2.setString(4, getDescription());
                ps2.setString(5, getAuthorization().getClassification());
                ps2.setString(6, getPublisher());
                ps2.setString(7, getLinks().getSelf());
                ps2.setString(8, getLinks().getPublish());
                ps2.setString(9, getLinks().getSubscribe());
                ps2.setString(10, getLinks().getLog());
                ps2.setBoolean(11, isDeleted());
                ps2.setBoolean(12, isSuspended());
                ps2.setString(13, getBusiness_description());
                ps2.setInt(14, groupid);
                ps2.setString(15, getAafInstance());
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            rv = false;
            intlogger.error("PROV0005 doInsert: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection c) {
        boolean rv = true;
        Feed oldobj = getFeedById(feedid);
        PreparedStatement ps = null;
        try {
            Set<FeedEndpointID> newset = getAuthorization().getEndpointIDS();
            Set<FeedEndpointID> oldset = oldobj.getAuthorization().getEndpointIDS();

            // Insert new FEED_ENDPOINT_IDS rows
            String sql = "insert into FEED_ENDPOINT_IDS values (?, ?, ?)";
            ps = c.prepareStatement(sql);
            for (FeedEndpointID fid : newset) {
                if (!oldset.contains(fid)) {
                    ps.setInt(1, feedid);
                    ps.setString(2, fid.getId());
                    ps.setString(3, fid.getPassword());
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Delete old FEED_ENDPOINT_IDS rows
            sql = "delete from FEED_ENDPOINT_IDS where FEEDID = ? AND USERID = ? AND PASSWORD = ?";
            ps = c.prepareStatement(sql);
            for (FeedEndpointID fid : oldset) {
                if (!newset.contains(fid)) {
                    ps.setInt(1, feedid);
                    ps.setString(2, fid.getId());
                    ps.setString(3, fid.getPassword());
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Insert new FEED_ENDPOINT_ADDRS rows
            Set<String> newset2 = getAuthorization().getEndpointAddrs();
            Set<String> oldset2 = oldobj.getAuthorization().getEndpointAddrs();
            sql = "insert into FEED_ENDPOINT_ADDRS values (?, ?)";
            ps = c.prepareStatement(sql);
            for (String t : newset2) {
                if (!oldset2.contains(t)) {
                    ps.setInt(1, feedid);
                    ps.setString(2, t);
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Delete old FEED_ENDPOINT_ADDRS rows
            sql = "delete from FEED_ENDPOINT_ADDRS where FEEDID = ? AND ADDR = ?";
            ps = c.prepareStatement(sql);
            for (String t : oldset2) {
                if (!newset2.contains(t)) {
                    ps.setInt(1, feedid);
                    ps.setString(2, t);
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Finally, update the FEEDS row
            sql = "update FEEDS set DESCRIPTION = ?, AUTH_CLASS = ?, DELETED = ?, SUSPENDED = ?, BUSINESS_DESCRIPTION=?, GROUPID=? where FEEDID = ?";
            ps = c.prepareStatement(sql);
            ps.setString(1, getDescription());
            ps.setString(2, getAuthorization().getClassification());
            ps.setInt(3, deleted ? 1 : 0);
            ps.setInt(4, suspended ? 1 : 0);
            ps.setString(5, getBusiness_description());
            ps.setInt(6, groupid);
            ps.setInt(7, feedid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(), e);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                intlogger.error(SQLEXCEPTION + e.getMessage(), e);
            }
        }
        return rv;
    }

    /**
     * Rally US708115
     * Change Ownership of FEED - 1610
     */
    public boolean changeOwnerShip() {
        boolean rv = true;
        PreparedStatement ps = null;
        try {

            DB db = new DB();
            @SuppressWarnings("resource")
            Connection c = db.getConnection();
            String sql = "update FEEDS set PUBLISHER = ? where FEEDID = ?";
            ps = c.prepareStatement(sql);
            ps.setString(1, this.publisher);
            ps.setInt(2, feedid);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0008 changeOwnerShip: " + e.getMessage(), e);
        } finally {
            try {
                if(ps!=null) {
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
        return "" + getFeedid();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Feed))
            return false;
        Feed of = (Feed) obj;
        if (feedid != of.feedid)
            return false;
        if (groupid != of.groupid) //New field is added - Groups feature Rally:US708115 - 1610
            return false;
        if (!name.equals(of.name))
            return false;
        if (!version.equals(of.version))
            return false;
        if (!description.equals(of.description))
            return false;
        if (!business_description.equals(of.business_description)) // New field is added - Groups feature Rally:US708102 - 1610
            return false;
        if (!publisher.equals(of.publisher))
            return false;
        if (!authorization.equals(of.authorization))
            return false;
        if (!links.equals(of.links))
            return false;
        if (deleted != of.deleted)
            return false;
        if (suspended != of.suspended)
            return false;
        if (!aaf_instance.equals(of.aaf_instance))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FEED: feedid=" + feedid + ", name=" + name + ", version=" + version;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}