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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;


/**
 * The representation of a Subscription.  Subscriptions can be retrieved from the DB, or stored/updated in the DB.
 *
 * @author Robert Eby
 * @version $Id: Subscription.java,v 1.9 2013/10/28 18:06:53 eby Exp $
 */

public class Subscription extends Syncable {

    private static final String SQLEXCEPTION = "SQLException: ";
    private static final String SUBID_KEY = "subid";
    private static final String SUBID_COL = "SUBID";
    private static final String FEEDID_KEY = "feedid";
    private static final String GROUPID_KEY = "groupid";
    private static final String LAST_MOD_KEY = "last_mod";
    private static final String CREATED_DATE = "created_date";
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static int nextSubid = getMaxSubID() + 1;

    private int subid;
    private int feedid;
    private int groupid; //New field is added - Groups feature Rally:US708115 - 1610
    private SubDelivery delivery;
    private boolean followRedirect;
    private boolean metadataOnly;
    private String subscriber;
    private SubLinks links;
    private boolean suspended;
    private Date lastMod;
    private Date createdDate;
    private boolean privilegedSubscriber;
    private boolean decompress;

    public Subscription() {
        this("", "", "");
    }

    /**
     * Subscription constructor.
     * @param url url string
     * @param user user string
     * @param password password string
     */
    public Subscription(String url, String user, String password) {
        this.subid = -1;
        this.feedid = -1;
        this.groupid = -1; //New field is added - Groups feature Rally:US708115 - 1610
        this.delivery = new SubDelivery(url, user, password, false);
        this.metadataOnly = false;
        this.followRedirect = false;
        this.subscriber = "";
        this.links = new SubLinks();
        this.suspended = false;
        this.lastMod = new Date();
        this.createdDate = new Date();
        this.privilegedSubscriber = false;
        this.decompress = false;
    }

    /**
     * Subscription constructor.
     * @param rs resultset from SQL
     * @throws SQLException in case of SQL error
     */
    public Subscription(ResultSet rs) throws SQLException {
        this.subid = rs.getInt(SUBID_COL);
        this.feedid = rs.getInt("FEEDID");
        this.groupid = rs.getInt("GROUPID"); //New field is added - Groups feature Rally:US708115 - 1610
        this.delivery = new SubDelivery(rs);
        this.metadataOnly = rs.getBoolean("METADATA_ONLY");
        this.followRedirect = rs.getBoolean("FOLLOW_REDIRECTS");
        this.subscriber = rs.getString("SUBSCRIBER");
        this.links = new SubLinks(rs.getString("SELF_LINK"), URLUtilities.generateFeedURL(feedid),
            rs.getString("LOG_LINK"));
        this.suspended = rs.getBoolean("SUSPENDED");
        this.lastMod = rs.getDate("LAST_MOD");
        this.createdDate = rs.getDate("CREATED_DATE");
        this.privilegedSubscriber = rs.getBoolean("PRIVILEGED_SUBSCRIBER");
        this.decompress  = rs.getBoolean("DECOMPRESS");
    }

    /**
     * Subscription constructor.
     * @param jo JSONObject
     * @throws InvalidObjectException in case of object error
     */
    public Subscription(JSONObject jo) throws InvalidObjectException {
        this("", "", "");
        try {
            // The JSONObject is assumed to contain a vnd.dmaap-dr.subscription representation
            this.subid = jo.optInt(SUBID_KEY, -1);
            this.feedid = jo.optInt(FEEDID_KEY, -1);
            this.groupid = jo.optInt(GROUPID_KEY, -1); //New field is added - Groups feature Rally:US708115 - 1610
            JSONObject jdeli = jo.getJSONObject("delivery");
            String url = jdeli.getString("url");
            String user = jdeli.getString("user");
            final String password = jdeli.getString("password");
            final boolean use100 = jdeli.getBoolean("use100");

            //Data Router Subscriber HTTPS Relaxation feature USERSTORYID:US674047.
            Properties prop = ProvRunner.getProvProperties();
            if (!url.startsWith("https://") && isHttpsRelaxationFalseAndHasSyncKey(jo, prop)) {
                throw new InvalidObjectException("delivery URL is not HTTPS");
            }

            if (url.length() > 256) {
                throw new InvalidObjectException("delivery url field is too long");
            }
            if (user.length() > 60) {
                throw new InvalidObjectException("delivery user field is too long");
            }
            if (password.length() > 32) {
                throw new InvalidObjectException("delivery password field is too long");
            }
            this.delivery = new SubDelivery(url, user, password, use100);
            this.metadataOnly = jo.getBoolean("metadataOnly");
            this.followRedirect = jo.optBoolean("follow_redirect", false);
            this.suspended = jo.optBoolean("suspend", false);
            this.privilegedSubscriber = jo.optBoolean("privilegedSubscriber", false);
            this.decompress = jo.optBoolean("decompress", false);
            this.subscriber = jo.optString("subscriber", "");
            JSONObject jol = jo.optJSONObject("links");
            this.links = (jol == null) ? (new SubLinks()) : (new SubLinks(jol));
        } catch (InvalidObjectException e) {
            throw e;
        } catch (Exception e) {
            intlogger.warn("Invalid JSON: " + e.getMessage(), e);
            throw new InvalidObjectException("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Get specific subscription.
     * @param sub subscription object
     * @return subscription
     */
    public static Subscription getSubscriptionMatching(Subscription sub) {
        SubDelivery deli = sub.getDelivery();
        String sql = String.format(
            "select * from SUBSCRIPTIONS where FEEDID = %d and DELIVERY_URL = \"%s\" and DELIVERY_USER = \"%s\" "
                + "and DELIVERY_PASSWORD = \"%s\" and DELIVERY_USE100 = %d and METADATA_ONLY = %d "
                + "and FOLLOW_REDIRECTS = %d",
            sub.getFeedid(),
            deli.getUrl(),
            deli.getUser(),
            deli.getPassword(),
            deli.isUse100() ? 1 : 0,
            sub.isMetadataOnly() ? 1 : 0,
            sub.isFollowRedirect() ? 1 : 0
        );
        List<Subscription> list = getSubscriptionsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * Get subscription by id.
     * @param id subscription id string
     * @return subscription
     */
    public static Subscription getSubscriptionById(int id) {
        String sql = "select * from SUBSCRIPTIONS where SUBID = " + id;
        List<Subscription> list = getSubscriptionsForSQL(sql);
        return !list.isEmpty() ? list.get(0) : null;
    }

    public static Collection<Subscription> getAllSubscriptions() {
        return getSubscriptionsForSQL("select * from SUBSCRIPTIONS");
    }

    /**
     * Get subscriptions from SQL.
     * @param sql SQL statement
     * @return List of subscriptions
     */
    private static List<Subscription> getSubscriptionsForSQL(String sql) {
        List<Subscription> list = new ArrayList<>();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Subscription sub = new Subscription(rs);
                list.add(sub);
            }
        } catch (SQLException e) {
            intlogger.error("PROV0001 getSubscriptionsForSQL: " + e.toString(), e);
        }
        return list;
    }

    /**
     * Get max subid.
     * @return subid int
     */
    public static int getMaxSubID() {
        int max = 0;
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("select MAX(subid) from SUBSCRIPTIONS");
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                max = rs.getInt(1);
            }
        } catch (SQLException e) {
            intlogger.info("getMaxSubID: " + e.getMessage(), e);
        }
        return max;
    }

    /**
     * Get subscription URL list.
     * @param feedid feedid int
     * @return collection of subscription URL
     */
    public static Collection<String> getSubscriptionUrlList(int feedid) {
        List<String> list = new ArrayList<>();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement("select SUBID from SUBSCRIPTIONS where FEEDID = ?")) {
            stmt.setString(1, String.valueOf(feedid));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int subid = rs.getInt(SUBID_COL);
                    list.add(URLUtilities.generateSubscriptionURL(subid));
                }
            }
        } catch (SQLException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
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
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("select count(*) from SUBSCRIPTIONS");
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            intlogger.warn("PROV0008 countActiveSubscriptions: " + e.getMessage(), e);
        }
        return count;
    }

    private boolean isHttpsRelaxationFalseAndHasSyncKey(JSONObject jo, Properties prop) {
        return prop.get("org.onap.dmaap.datarouter.provserver.https.relaxation").toString().equals("false") && !jo
            .has("sync");
    }

    public int getSubid() {
        return subid;
    }

    /**
     * Subid setter.
     * @param subid subid string
     */
    public void setSubid(int subid) {
        this.subid = subid;

        // Create link URLs
        SubLinks sl = getLinks();
        sl.setSelf(URLUtilities.generateSubscriptionURL(subid));
        sl.setLog(URLUtilities.generateSubLogURL(subid));
    }

    public int getFeedid() {
        return feedid;
    }

    /**
     * feedid setter.
     * @param feedid feedid string
     */
    public void setFeedid(int feedid) {
        this.feedid = feedid;

        // Create link URLs
        SubLinks sl = getLinks();
        sl.setFeed(URLUtilities.generateFeedURL(feedid));
    }

    //New getter setters for Groups feature Rally:US708115 - 1610
    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public SubDelivery getDelivery() {
        return delivery;
    }

    public void setDelivery(SubDelivery delivery) {
        this.delivery = delivery;
    }

    public boolean isMetadataOnly() {
        return metadataOnly;
    }

    public void setMetadataOnly(boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
    }

    private boolean isFollowRedirect() {
        return followRedirect;
    }

    public void setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
    }

    boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isPrivilegedSubscriber() {
        return privilegedSubscriber;
    }

    public void setPrivilegedSubscriber(boolean privilegedSubscriber) {
        this.privilegedSubscriber = privilegedSubscriber;
    }

    public String getSubscriber() {
        return subscriber;
    }

    /**
     * Subscriber setter.
     * @param subscriber subscriber string
     */
    public void setSubscriber(String subscriber) {
        if (subscriber != null) {
            if (subscriber.length() > 8) {
                subscriber = subscriber.substring(0, 8);
            }
            this.subscriber = subscriber;
        }
    }

    public SubLinks getLinks() {
        return links;
    }

    void setLinks(SubLinks links) {
        this.links = links;
    }

    public boolean isDecompress() {
        return decompress;
    }

    public void setDecompress(boolean decompress) {
        this.decompress = decompress;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put(SUBID_KEY, subid);
        jo.put(FEEDID_KEY, feedid);
        jo.put(GROUPID_KEY, groupid); //New field is added - Groups feature Rally:US708115 - 1610
        jo.put("delivery", delivery.asJSONObject());
        jo.put("metadataOnly", metadataOnly);
        jo.put("follow_redirect", followRedirect);
        jo.put("subscriber", subscriber);
        jo.put("links", links.asJSONObject());
        jo.put("suspend", suspended);
        jo.put(LAST_MOD_KEY, lastMod.getTime());
        jo.put(CREATED_DATE, createdDate.getTime());
        jo.put("privilegedSubscriber", privilegedSubscriber);
        jo.put("decompress", decompress);
        return jo;
    }

    /**
     * Method to hide attributes.
     * @param hidepasswords true/false
     * @return JSONObject
     */
    public JSONObject asJSONObject(boolean hidepasswords) {
        JSONObject jo = asJSONObject();
        if (hidepasswords) {
            jo.remove(SUBID_KEY);    // we no longer hide passwords, however we do hide these
            jo.remove(FEEDID_KEY);
            jo.remove(LAST_MOD_KEY);
            jo.remove(CREATED_DATE);
        }
        return jo;
    }

    /**
     * Method to remove some attributes from JSON.
     * @ JSONObject
     */
    public JSONObject asLimitedJSONObject() {
        JSONObject jo = asJSONObject();
        jo.remove(SUBID_KEY);
        jo.remove(FEEDID_KEY);
        jo.remove(LAST_MOD_KEY);
        return jo;
    }


    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = true;
        PreparedStatement ps = null;
        try {
            if (subid == -1) {
                // No feed ID assigned yet, so assign the next available one
                setSubid(nextSubid++);
            }
            // In case we insert a feed from synchronization
            if (subid > nextSubid) {
                nextSubid = subid + 1;
            }

            // Create the SUBSCRIPTIONS row
            String sql = "insert into SUBSCRIPTIONS (SUBID, FEEDID, DELIVERY_URL, DELIVERY_USER, DELIVERY_PASSWORD, "
                + "DELIVERY_USE100, METADATA_ONLY, SUBSCRIBER, SUSPENDED, GROUPID, "
                + "PRIVILEGED_SUBSCRIBER, FOLLOW_REDIRECTS, DECOMPRESS) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{SUBID_COL});
            ps.setInt(1, subid);
            ps.setInt(2, feedid);
            ps.setString(3, getDelivery().getUrl());
            ps.setString(4, getDelivery().getUser());
            ps.setString(5, getDelivery().getPassword());
            ps.setInt(6, getDelivery().isUse100() ? 1 : 0);
            ps.setInt(7, isMetadataOnly() ? 1 : 0);
            ps.setString(8, getSubscriber());
            ps.setBoolean(9, isSuspended());
            ps.setInt(10, groupid); //New field is added - Groups feature Rally:US708115 - 1610
            ps.setBoolean(11, isPrivilegedSubscriber());
            ps.setInt(12, isFollowRedirect() ? 1 : 0);
            ps.setBoolean(13, isDecompress());
            ps.execute();
            ps.close();
            // Update the row to set the URLs
            sql = "update SUBSCRIPTIONS set SELF_LINK = ?, LOG_LINK = ? where SUBID = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, getLinks().getSelf());
            ps.setString(2, getLinks().getLog());
            ps.setInt(3, subid);
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
        try (PreparedStatement ps = conn.prepareStatement(
            "update SUBSCRIPTIONS set DELIVERY_URL = ?, DELIVERY_USER = ?, DELIVERY_PASSWORD = ?, "
            + "DELIVERY_USE100 = ?, METADATA_ONLY = ?, SUSPENDED = ?, GROUPID = ?, PRIVILEGED_SUBSCRIBER = ?, "
            + "FOLLOW_REDIRECTS = ?, DECOMPRESS = ? where SUBID = ?")) {
            ps.setString(1, delivery.getUrl());
            ps.setString(2, delivery.getUser());
            ps.setString(3, delivery.getPassword());
            ps.setInt(4, delivery.isUse100() ? 1 : 0);
            ps.setInt(5, isMetadataOnly() ? 1 : 0);
            ps.setInt(6, suspended ? 1 : 0);
            ps.setInt(7, groupid); //New field is added - Groups feature Rally:US708115 - 1610
            ps.setInt(8, privilegedSubscriber ? 1 : 0);
            ps.setInt(9, isFollowRedirect() ? 1 : 0);
            ps.setInt(10, isDecompress() ? 1 : 0);
            ps.setInt(11, subid);
            ps.executeUpdate();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(), e);
        }
        return rv;
    }


    /**
     * Rally US708115 Change Ownership of Subscription - 1610.
     */
    public boolean changeOwnerShip() {
        boolean rv = true;
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "update SUBSCRIPTIONS set SUBSCRIBER = ? where SUBID = ?")) {
            ps.setString(1, this.subscriber);
            ps.setInt(2, subid);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(), e);
        }
        return rv;
    }


    @Override
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        try (PreparedStatement ps = conn.prepareStatement("delete from SUBSCRIPTIONS where SUBID = ?")) {
            ps.setInt(1, subid);
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public String getKey() {
        return "" + getSubid();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Subscription)) {
            return false;
        }
        Subscription os = (Subscription) obj;
        if (subid != os.subid) {
            return false;
        }
        if (feedid != os.feedid) {
            return false;
        }
        if (groupid != os.groupid) {
            //New field is added - Groups feature Rally:US708115 - 1610
            return false;
        }
        if (!delivery.equals(os.delivery)) {
            return false;
        }
        if (metadataOnly != os.metadataOnly) {
            return false;
        }
        if (followRedirect != os.followRedirect) {
            return false;
        }
        if (!subscriber.equals(os.subscriber)) {
            return false;
        }
        if (!links.equals(os.links)) {
            return false;
        }
        if (suspended != os.suspended) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "SUB: subid=" + subid + ", feedid=" + feedid;
    }
}
