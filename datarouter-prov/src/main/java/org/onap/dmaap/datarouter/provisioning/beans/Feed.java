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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.JSONUtilities;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;



/**
 * The representation of a Feed.  Feeds can be retrieved from the DB, or stored/updated in the DB.
 *
 * @author Robert Eby
 * @version $Id: Feed.java,v 1.13 2013/10/28 18:06:52 eby Exp $
 */
public class Feed extends Syncable {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static int nextFeedID = getMaxFeedID() + 1;
    private static final String SQLEXCEPTION = "SQLException: ";
    private static final String FEEDID = "FEEDID";
    private static final String FEED_ID_STR = "feedid";
    private static final String DELETED_STR = "deleted";
    private static final String LAST_MOD_STR = "last_mod";
    private static final String CREATED_DATE_STR ="created_date";

    private int feedIdInt;
    private int groupid; //New field is added - Groups feature Rally:US708115 - 1610
    private String name;
    private String version;
    private String description;
    private String businessDescription; // New field is added - Groups feature Rally:US708102 - 1610
    private FeedAuthorization authorization;
    private String publisher;
    private FeedLinks links;
    private boolean deleted;
    private boolean suspended;
    private Date lastMod;
    private Date createdDate;
    private String aafInstance;

    public Feed() {
        this("", "", "", "");
    }

    /**
     * Feed constructor.
     * @param name feed name
     * @param version feed version
     * @param desc feed description
     * @param businessDescription feed business description
     */
    public Feed(String name, String version, String desc, String businessDescription) {
        this.feedIdInt = -1;
        this.groupid = -1; //New field is added - Groups feature Rally:US708115 - 1610
        this.name = name;
        this.version = version;
        this.description = desc;
        this.businessDescription = businessDescription; // New field is added - Groups feature Rally:US708102 - 1610
        this.authorization = new FeedAuthorization();
        this.publisher = "";
        this.links = new FeedLinks();
        this.deleted = false;
        this.suspended = false;
        this.lastMod = new Date();
        this.createdDate = new Date();
        this.aafInstance = "";
    }

    /**
     * Feed Constructor from ResultSet.
     * @param rs ResultSet
     * @throws SQLException in case of SQL statement error
     */
    public Feed(ResultSet rs) throws SQLException {
        this.feedIdInt = rs.getInt(FEEDID);
        //New field is added - Groups feature Rally:US708115 - 1610
        this.groupid = rs.getInt("GROUPID");
        this.name = rs.getString("NAME");
        this.version = rs.getString("VERSION");
        this.description = rs.getString("DESCRIPTION");
        // New field is added - Groups feature Rally:US708102 - 1610
        this.businessDescription = rs.getString("BUSINESS_DESCRIPTION");
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
        this.lastMod = rs.getDate("LAST_MOD");
        this.createdDate = rs.getTimestamp("CREATED_DATE");
        this.aafInstance = rs.getString("AAF_INSTANCE");
    }

    /**
     * Feed constructor from JSONObject.
     * @param jo JSONObject
     * @throws InvalidObjectException in case of JSON error
     */
    public Feed(JSONObject jo) throws InvalidObjectException {
        this("", "", "", "");
        try {
            // The JSONObject is assumed to contain a vnd.dmaap-dr.feed representation
            this.feedIdInt = jo.optInt(FEED_ID_STR, -1);
            this.groupid = jo.optInt("groupid");
            this.name = jo.getString("name");
            this.aafInstance = jo.optString("aaf_instance", "legacy");
            if (!(aafInstance.equalsIgnoreCase("legacy")) && aafInstance.length() > 255) {
                throw new InvalidObjectException("aaf_instance field is too long");
            }
            if (name.length() > 255) {
                throw new InvalidObjectException("name field is too long");
            }
            try {
                this.version = jo.getString("version");
            } catch (JSONException e) {
                intlogger.warn("PROV0023 Feed.Feed: " + e.getMessage(), e);
                this.version = null;
            }
            if (version != null && version.length() > 20) {
                throw new InvalidObjectException("version field is too long");
            }
            this.description = jo.optString("description");
            this.businessDescription = jo.optString("business_description");
            if (description.length() > 1000) {
                throw new InvalidObjectException("technical description field is too long");
            }
            if (businessDescription.length() > 1000) {
                throw new InvalidObjectException("business description field is too long");
            }
            this.authorization = new FeedAuthorization();
            JSONObject jauth = jo.getJSONObject("authorization");
            this.authorization.setClassification(jauth.getString("classification"));
            if (this.authorization.getClassification().length() > 32) {
                throw new InvalidObjectException("classification field is too long");
            }
            JSONArray endPointIds = jauth.getJSONArray("endpoint_ids");
            for (int i = 0; i < endPointIds.length(); i++) {
                JSONObject id = endPointIds.getJSONObject(i);
                FeedEndpointID fid = new FeedEndpointID(id.getString("id"), id.getString("password"));
                if (fid.getId().length() > 60) {
                    throw new InvalidObjectException("id field is too long (" + fid.getId() + ")");
                }
                if (fid.getPassword().length() > 32) {
                    //Fortify scan fixes - Privacy Violation
                    throw new InvalidObjectException("password field is too long (" + fid.getPassword() + ")");
                }
                this.authorization.getEndpointIDS().add(fid);
            }
            if (this.authorization.getEndpointIDS().isEmpty()) {
                throw new InvalidObjectException("need to specify at least one endpoint_id");
            }
            endPointIds = jauth.getJSONArray("endpoint_addrs");
            for (int i = 0; i < endPointIds.length(); i++) {
                String addr = endPointIds.getString(i);
                if (!JSONUtilities.validIPAddrOrSubnet(addr)) {
                    throw new InvalidObjectException("bad IP addr or subnet mask: " + addr);
                }
                this.authorization.getEndpointAddrs().add(addr);
            }

            this.publisher = jo.optString("publisher", "");
            this.deleted = jo.optBoolean(DELETED_STR, false);
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
            try (PreparedStatement stmt = conn.prepareStatement("select COUNT(*) from FEEDS where FEEDID = ?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
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
            @SuppressWarnings("resource") Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
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

    /**
     * Method to get max feed id.
     * @return int max feed id
     */
    public static int getMaxFeedID() {
        int max = 0;
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
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

    /**
     * Gets all feeds.
     * @return Collection of feeds
     */
    public static Collection<Feed> getAllFeeds() {
        Map<Integer, Feed> map = new HashMap<>();
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select * from FEEDS")) {
                    while (rs.next()) {
                        Feed feed = new Feed(rs);
                        map.put(feed.getFeedIdInt(), feed);
                    }
                }

                String sql = "select * from FEED_ENDPOINT_IDS";
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int id = rs.getInt(FEEDID);
                        Feed feed = map.get(id);
                        if (feed != null) {
                            FeedEndpointID epi = new FeedEndpointID(rs);
                            Collection<FeedEndpointID> ecoll = feed.getAuthorization().getEndpointIDS();
                            ecoll.add(epi);
                        }
                    }
                }

                sql = "select * from FEED_ENDPOINT_ADDRS";
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int id = rs.getInt(FEEDID);
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

    /**
     * Get Feed URL list.
     * @param name of Feed
     * @param val of feed
     * @return List of feed names
     */
    public static List<String> getFilteredFeedUrlList(final String name, final String val) {
        List<String> list = new ArrayList<>();
        String sql = "select SELF_LINK from FEEDS where DELETED = 0";
        if (name.equals("name")) {
            sql += " and NAME = ?";
        } else if (name.equals("publ")) {
            sql += " and PUBLISHER = ?";
        } else if (name.equals("subs")) {
            sql = "select distinct FEEDS.SELF_LINK from FEEDS, SUBSCRIPTIONS "
                          + "where DELETED = 0 "
                          + "and FEEDS.FEEDID = SUBSCRIPTIONS.FEEDID "
                          + "and SUBSCRIPTIONS.SUBSCRIBER = ?";
        }
        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (sql.indexOf('?') >= 0) {
                    ps.setString(1, val);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String str = rs.getString(1);
                        list.add(str.trim());
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
                    sql = "select * from FEED_ENDPOINT_IDS where FEEDID = " + feed.feedIdInt;
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        Collection<FeedEndpointID> ecoll = feed.getAuthorization().getEndpointIDS();
                        while (rs.next()) {
                            FeedEndpointID epi = new FeedEndpointID(rs);
                            ecoll.add(epi);
                        }
                    }
                    sql = "select * from FEED_ENDPOINT_ADDRS where FEEDID = " + feed.feedIdInt;
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



    public int getFeedIdInt() {
        return feedIdInt;
    }

    /**
     *  Set feedid with FeedLinks.
      * @param feedIdInt Feedid to set to
     */
    public void setFeedIdInt(int feedIdInt) {
        this.feedIdInt = feedIdInt;

        // Create link URLs
        FeedLinks fl = getLinks();
        fl.setSelf(URLUtilities.generateFeedURL(feedIdInt));
        fl.setPublish(URLUtilities.generatePublishURL(feedIdInt));
        fl.setSubscribe(URLUtilities.generateSubscribeURL(feedIdInt));
        fl.setLog(URLUtilities.generateFeedLogURL(feedIdInt));
    }

    public String getAafInstance() {
        return aafInstance;
    }

    public void setAafInstance(String aafInstance) {
        this.aafInstance = aafInstance;
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
    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
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

    /**
     * Set publisher.
     * @param publisher Publisher name
     */
    public void setPublisher(String publisher) {
        if (publisher != null) {
            if (publisher.length() > 8) {
                publisher = publisher.substring(0, 8);
            }
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
        jo.put(FEED_ID_STR, feedIdInt);
        //New field is added - Groups feature Rally:US708115 - 1610
        jo.put("groupid", groupid);
        jo.put("name", name);
        jo.put("version", version);
        jo.put("description", description);
        // New field is added - Groups feature Rally:US708102 - 1610
        jo.put("business_description", businessDescription);
        jo.put("authorization", authorization.asJSONObject());
        jo.put("publisher", publisher);
        jo.put("links", links.asJSONObject());
        jo.put(DELETED_STR, deleted);
        jo.put("suspend", suspended);
        jo.put(LAST_MOD_STR, lastMod.getTime());
        jo.put(CREATED_DATE_STR, createdDate.getTime());
        jo.put("aaf_instance", aafInstance);
        return jo;
    }

    /**
     * Method to hide some attributes.
     * @param hidepasswords true/false
     * @return JSONObject
     */
    public JSONObject asJSONObject(boolean hidepasswords) {
        JSONObject jo = asJSONObject();
        if (hidepasswords) {
            jo.remove(FEED_ID_STR);    // we no longer hide passwords, however we do hide these
            jo.remove(DELETED_STR);
            jo.remove(LAST_MOD_STR);
            jo.remove(CREATED_DATE_STR);
        }
        return jo;
    }

    /**
     * Method to limit JSONObject.
     * @return JSONObject
     */
    public JSONObject asLimitedJSONObject() {
        JSONObject jo = asJSONObject();
        jo.remove(DELETED_STR);
        jo.remove(FEED_ID_STR);
        jo.remove(LAST_MOD_STR);
        jo.remove(CREATED_DATE_STR);
        return jo;
    }



    @Override
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            String sql = "delete from FEEDS where FEEDID = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, feedIdInt);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.error("PROV0007 doDelete: " + e.getMessage(), e);
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
    public synchronized boolean doInsert(Connection conn) {
        boolean rv = true;
        try {
            if (feedIdInt == -1) {
                setFeedIdInt(nextFeedID++);
            }
            // In case we insert a feed from synchronization
            if (feedIdInt > nextFeedID) {
                nextFeedID = feedIdInt + 1;
            }

            // Create FEED_ENDPOINT_IDS rows
            FeedAuthorization auth = getAuthorization();
            String sql = "insert into FEED_ENDPOINT_IDS values (?, ?, ?)";
            try (PreparedStatement ps2 = conn.prepareStatement(sql)) {
                for (FeedEndpointID fid : auth.getEndpointIDS()) {
                    ps2.setInt(1, feedIdInt);
                    ps2.setString(2, fid.getId());
                    ps2.setString(3, fid.getPassword());
                    ps2.executeUpdate();
                }
            }

            // Create FEED_ENDPOINT_ADDRS rows
            sql = "insert into FEED_ENDPOINT_ADDRS values (?, ?)";
            try (PreparedStatement ps2 = conn.prepareStatement(sql)) {
                for (String t : auth.getEndpointAddrs()) {
                    ps2.setInt(1, feedIdInt);
                    ps2.setString(2, t);
                    ps2.executeUpdate();
                }
            }

            // Finally, create the FEEDS row
            sql = "insert into FEEDS (FEEDID, NAME, VERSION, DESCRIPTION, AUTH_CLASS, PUBLISHER, SELF_LINK, "
                          + "PUBLISH_LINK, SUBSCRIBE_LINK, LOG_LINK, DELETED, SUSPENDED,"
                          + "BUSINESS_DESCRIPTION, GROUPID, AAF_INSTANCE) "
                          + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps2 = conn.prepareStatement(sql)) {
                ps2.setInt(1, feedIdInt);
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
                ps2.setString(13, getBusinessDescription());
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
    public boolean doUpdate(Connection conn) {
        boolean rv = true;
        Feed oldobj = getFeedById(feedIdInt);
        PreparedStatement ps = null;
        try {
            Set<FeedEndpointID> newset = getAuthorization().getEndpointIDS();
            Set<FeedEndpointID> oldset = oldobj.getAuthorization().getEndpointIDS();

            // Insert new FEED_ENDPOINT_IDS rows
            String sql = "insert into FEED_ENDPOINT_IDS values (?, ?, ?)";
            ps = conn.prepareStatement(sql);
            for (FeedEndpointID fid : newset) {
                if (!oldset.contains(fid)) {
                    ps.setInt(1, feedIdInt);
                    ps.setString(2, fid.getId());
                    ps.setString(3, fid.getPassword());
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Delete old FEED_ENDPOINT_IDS rows
            sql = "delete from FEED_ENDPOINT_IDS where FEEDID = ? AND USERID = ? AND PASSWORD = ?";
            ps = conn.prepareStatement(sql);
            for (FeedEndpointID fid : oldset) {
                if (!newset.contains(fid)) {
                    ps.setInt(1, feedIdInt);
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
            ps = conn.prepareStatement(sql);
            for (String t : newset2) {
                if (!oldset2.contains(t)) {
                    ps.setInt(1, feedIdInt);
                    ps.setString(2, t);
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Delete old FEED_ENDPOINT_ADDRS rows
            sql = "delete from FEED_ENDPOINT_ADDRS where FEEDID = ? AND ADDR = ?";
            ps = conn.prepareStatement(sql);
            for (String t : oldset2) {
                if (!newset2.contains(t)) {
                    ps.setInt(1, feedIdInt);
                    ps.setString(2, t);
                    ps.executeUpdate();
                }
            }
            ps.close();

            // Finally, update the FEEDS row
            sql = "update FEEDS set DESCRIPTION = ?, AUTH_CLASS = ?, DELETED = ?, SUSPENDED = ?, "
                          + "BUSINESS_DESCRIPTION=?, GROUPID=? where FEEDID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, getDescription());
            ps.setString(2, getAuthorization().getClassification());
            ps.setInt(3, deleted ? 1 : 0);
            ps.setInt(4, suspended ? 1 : 0);
            ps.setString(5, getBusinessDescription());
            ps.setInt(6, groupid);
            ps.setInt(7, feedIdInt);
            ps.executeUpdate();
            ps.close();
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

    /**
     * Rally US708115.
     * Change Ownership of FEED - 1610
     */
    public boolean changeOwnerShip() {
        boolean rv = true;
        PreparedStatement ps = null;
        try {

            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            String sql = "update FEEDS set PUBLISHER = ? where FEEDID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, this.publisher);
            ps.setInt(2, feedIdInt);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0008 changeOwnerShip: " + e.getMessage(), e);
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
        return "" + getFeedIdInt();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Feed)) {
            return false;
        }
        Feed of = (Feed) obj;
        if (feedIdInt != of.feedIdInt) {
            return false;
        }
        if (groupid != of.groupid) {
            //New field is added - Groups feature Rally:US708115 - 1610
            return false;
        }
        if (!name.equals(of.name)) {
            return false;
        }
        if (!version.equals(of.version)) {
            return false;
        }
        if (!description.equals(of.description)) {
            return false;
        }
        if (!businessDescription.equals(of.businessDescription)) {
            // New field is added - Groups feature Rally:US708102 - 1610
            return false;
        }
        if (!publisher.equals(of.publisher)) {
            return false;
        }
        if (!authorization.equals(of.authorization)) {
            return false;
        }
        if (!links.equals(of.links)) {
            return false;
        }
        if (deleted != of.deleted) {
            return false;
        }
        if (suspended != of.suspended) {
            return false;
        }
        if (!aafInstance.equals(of.aafInstance)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FEED: feedid=" + feedIdInt + ", name=" + name + ", version=" + version;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}