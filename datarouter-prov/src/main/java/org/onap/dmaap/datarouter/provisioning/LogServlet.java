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


package org.onap.dmaap.datarouter.provisioning;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onap.dmaap.datarouter.provisioning.beans.DeliveryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.ExpiryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.LOGJSONable;
import org.onap.dmaap.datarouter.provisioning.beans.PublishRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.eelf.EelfMsgs;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

/**
 * This servlet handles requests to the &lt;feedLogURL&gt; and  &lt;subLogURL&gt;,
 * which are generated by the provisioning server to handle the log query API.
 *
 * @author Robert Eby
 * @version $Id: LogServlet.java,v 1.11 2014/03/28 17:27:02 eby Exp $
 */
@SuppressWarnings("serial")
public class LogServlet extends BaseServlet {
    //Adding EELF Logger Rally:US664892
    private static EELFLogger eelflogger = EELFManager.getInstance().getLogger("org.onap.dmaap.datarouter.provisioning.LogServlet");
    private static final long TWENTYFOUR_HOURS = (24 * 60 * 60 * 1000L);
    private static final String FMT_1 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String FMT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static  boolean isfeedlog;

    public abstract class RowHandler {
        private final ServletOutputStream out;
        private final String[] fields;
        private boolean firstrow;

        public RowHandler(ServletOutputStream out, String fieldparam, boolean b) {
            this.out = out;
            this.firstrow = b;
            this.fields = (fieldparam != null) ? fieldparam.split(":") : null;
        }
        public void handleRow(ResultSet rs) {
            try {
                LOGJSONable js = buildJSONable(rs);
                LOGJSONObject jo = js.asJSONObject();
                if (fields != null) {
                    // filter out unwanted fields
                    LOGJSONObject j2 = new LOGJSONObject();
                    for (String key : fields) {
                        Object v = jo.opt(key);
                        if (v != null)
                            j2.put(key, v);
                    }
                    jo = j2;
                }
                String t = firstrow ? "\n" : ",\n";
                t += jo.toString();
                out.print(t);
                firstrow = false;
            } catch (Exception exception) {
                intlogger.info("Failed to handle row. Exception = " + exception.getMessage(),exception);
            }
        }
        public abstract LOGJSONable buildJSONable(ResultSet rs) throws SQLException;
    }
    public class PublishRecordRowHandler extends RowHandler {
        public PublishRecordRowHandler(ServletOutputStream out, String fields, boolean b) {
            super(out, fields, b);
        }
        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new PublishRecord(rs);
        }
    }
    public class DeliveryRecordRowHandler extends RowHandler {
        public DeliveryRecordRowHandler(ServletOutputStream out, String fields, boolean b) {
            super(out, fields, b);
        }
        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new DeliveryRecord(rs);
        }
    }
    public class ExpiryRecordRowHandler extends RowHandler {
        public ExpiryRecordRowHandler(ServletOutputStream out, String fields, boolean b) {
            super(out, fields, b);
        }
        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new ExpiryRecord(rs);
        }
    }

    /**
     * This class must be created from either a {@link FeedLogServlet} or a {@link SubLogServlet}.
     * @param isFeedLog boolean to handle those places where a feedlog request is different from
     * a sublog request
     */
    protected LogServlet(boolean isFeedLog) {
        this.isfeedlog = isFeedLog;
    }

    /**
     * DELETE a logging URL -- not supported.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setIpAndFqdnForEelf("doDelete");
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
        String message = "DELETE not allowed for the logURL.";
        EventLogRecord elr = new EventLogRecord(req);
        elr.setMessage(message);
        elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        eventlogger.info(elr);
        sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
    }
    /**
     * GET a logging URL -- retrieve logging data for a feed or subscription.
     * See the <b>Logging API</b> document for details on how this method should be invoked.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setIpAndFqdnForEelf("doGet");
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
        int id = getIdFromPath(req);
        if (id < 0) {
            sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing or bad feed/subscription number.", eventlogger);
            return;
        }
        Map<String, String> map = buildMapFromRequest(req);
        if (map.get("err") != null) {
            sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid arguments: "+map.get("err"), eventlogger);
            return;
        }
        // check Accept: header??

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(LOGLIST_CONTENT_TYPE);

        try (ServletOutputStream out = resp.getOutputStream()) {
            final String fields = req.getParameter("fields");

            out.print("[");
            if (isfeedlog) {
                // Handle /feedlog/feedid request
                boolean firstrow = true;

                // 1. Collect publish records for this feed
                RowHandler rh = new PublishRecordRowHandler(out, fields, firstrow);
                getPublishRecordsForFeed(id, rh, map);
                firstrow = rh.firstrow;

                // 2. Collect delivery records for subscriptions to this feed
                rh = new DeliveryRecordRowHandler(out, fields, firstrow);
                getDeliveryRecordsForFeed(id, rh, map);
                firstrow = rh.firstrow;

                // 3. Collect expiry records for subscriptions to this feed
                rh = new ExpiryRecordRowHandler(out, fields, firstrow);
                getExpiryRecordsForFeed(id, rh, map);
            } else {
                // Handle /sublog/subid request
                Subscription sub = Subscription.getSubscriptionById(id);
                if (sub != null) {
                    // 1. Collect publish records for the feed this subscription feeds
                    RowHandler rh = new PublishRecordRowHandler(out, fields, true);
                    getPublishRecordsForFeed(sub.getFeedid(), rh, map);

                    // 2. Collect delivery records for this subscription
                    rh = new DeliveryRecordRowHandler(out, fields, rh.firstrow);
                    getDeliveryRecordsForSubscription(id, rh, map);

                    // 3. Collect expiry records for this subscription
                    rh = new ExpiryRecordRowHandler(out, fields, rh.firstrow);
                    getExpiryRecordsForSubscription(id, rh, map);
                }
            }
            out.print("]");
        } catch (IOException ioe) {
            eventlogger.error("IOException: " + ioe.getMessage());
        }
    }
    /**
     * PUT a logging URL -- not supported.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        setIpAndFqdnForEelf("doPut");
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
        String message = "PUT not allowed for the logURL.";
        EventLogRecord elr = new EventLogRecord(req);
        elr.setMessage(message);
        elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        eventlogger.info(elr);
        sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
    }
    /**
     * POST a logging URL -- not supported.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        setIpAndFqdnForEelf("doPost");
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF, req.getHeader(BEHALF_HEADER));
        String message = "POST not allowed for the logURL.";
        EventLogRecord elr = new EventLogRecord(req);
        elr.setMessage(message);
        elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        eventlogger.info(elr);
        sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
    }

    private Map<String, String> buildMapFromRequest(HttpServletRequest req) {
        Map<String, String> map = new HashMap<>();
        String s = req.getParameter("type");
        if (s != null) {
            if (s.equals("pub") || s.equals("del") || s.equals("exp")) {
                map.put("type", s);
            } else {
                map.put("err", "bad type");
                return map;
            }
        } else
            map.put("type", "all");
        map.put("publishSQL", "");
        map.put("statusSQL", "");
        map.put("resultSQL", "");
        map.put("reasonSQL", "");

        s = req.getParameter("publishId");
        if (s != null) {
            if (s.indexOf("'") >= 0) {
                map.put("err", "bad publishId");
                return map;
            }
            map.put("publishSQL", " AND PUBLISH_ID = '"+s+"'");
        }

        s = req.getParameter("statusCode");
        if (s != null) {
            String sql = null;
            if (s.equals("success")) {
                sql = " AND STATUS >= 200 AND STATUS < 300";
            } else if (s.equals("redirect")) {
                sql = " AND STATUS >= 300 AND STATUS < 400";
            } else if (s.equals("failure")) {
                sql = " AND STATUS >= 400";
            } else {
                try {
                    Integer n = Integer.parseInt(s);
                    if ((n >= 100 && n < 600) || (n == -1))
                        sql = " AND STATUS = " + n;
                } catch (NumberFormatException e) {
                }
            }
            if (sql == null) {
                map.put("err", "bad statusCode");
                return map;
            }
            map.put("statusSQL", sql);
            map.put("resultSQL", sql.replaceAll("STATUS", "RESULT"));
        }

        s = req.getParameter("expiryReason");
        if (s != null) {
            map.put("type", "exp");
            if (s.equals("notRetryable")) {
                map.put("reasonSQL", " AND REASON = 'notRetryable'");
            } else if (s.equals("retriesExhausted")) {
                map.put("reasonSQL", " AND REASON = 'retriesExhausted'");
            } else if (s.equals("diskFull")) {
                map.put("reasonSQL", " AND REASON = 'diskFull'");
            } else if (s.equals("other")) {
                map.put("reasonSQL", " AND REASON = 'other'");
            } else {
                map.put("err", "bad expiryReason");
                return map;
            }
        }

        long stime = getTimeFromParam(req.getParameter("start"));
        if (stime < 0) {
            map.put("err", "bad start");
            return map;
        }
        long etime = getTimeFromParam(req.getParameter("end"));
        if (etime < 0) {
            map.put("err", "bad end");
            return map;
        }
        if (stime == 0 && etime == 0) {
            etime = System.currentTimeMillis();
            stime = etime - TWENTYFOUR_HOURS;
        } else if (stime == 0) {
            stime = etime - TWENTYFOUR_HOURS;
        } else if (etime == 0) {
            etime = stime + TWENTYFOUR_HOURS;
        }
        map.put("timeSQL", String.format(" AND EVENT_TIME >= %d AND EVENT_TIME <= %d", stime, etime));
        return map;
    }
    private long getTimeFromParam(final String s) {
        if (s == null)
            return 0;
        try {
            // First, look for an RFC 3339 date
            String fmt = (s.indexOf('.') > 0) ? FMT_2 : FMT_1;
            SimpleDateFormat sdf = new SimpleDateFormat(fmt);
            Date d = sdf.parse(s);
            return d.getTime();
        } catch (ParseException parseException) {
            intlogger.error("Exception in getting Time :- "+parseException.getMessage(),parseException);
        }
        try {
            // Also allow a long (in ms); useful for testing
            long n = Long.parseLong(s);
            return n;
        } catch (NumberFormatException numberFormatException) {
            intlogger.error("Exception in getting Time :- "+numberFormatException.getMessage(),numberFormatException);
        }
        intlogger.info("Error parsing time="+s);
        return -1;
    }

    private void getPublishRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if (type.equals("all") || type.equals("pub")) {
            String sql = "select * from LOG_RECORDS where FEEDID = "+feedid
                + " AND TYPE = 'pub'"
                + map.get("timeSQL") + map.get("publishSQL") + map.get("statusSQL");
            getRecordsForSQL(sql, rh);
        }
    }
    private void getDeliveryRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if (type.equals("all") || type.equals("del")) {
            String sql = "select * from LOG_RECORDS where FEEDID = "+feedid
                + " AND TYPE = 'del'"
                + map.get("timeSQL") + map.get("publishSQL") + map.get("resultSQL");
            getRecordsForSQL(sql, rh);
        }
    }
    private void getDeliveryRecordsForSubscription(int subid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if (type.equals("all") || type.equals("del")) {
            String sql = "select * from LOG_RECORDS where DELIVERY_SUBID = "+subid
                + " AND TYPE = 'del'"
                + map.get("timeSQL") + map.get("publishSQL") + map.get("resultSQL");
            getRecordsForSQL(sql, rh);
        }
    }
    private void getExpiryRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if (type.equals("all") || type.equals("exp")) {
            String st = map.get("statusSQL");
            if (st == null || st.length() == 0) {
                String sql = "select * from LOG_RECORDS where FEEDID = "+feedid
                    + " AND TYPE = 'exp'"
                    + map.get("timeSQL") + map.get("publishSQL") + map.get("reasonSQL");
                getRecordsForSQL(sql, rh);
            }
        }
    }
    private void getExpiryRecordsForSubscription(int subid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if (type.equals("all") || type.equals("exp")) {
            String st = map.get("statusSQL");
            if (st == null || st.length() == 0) {
                String sql = "select * from LOG_RECORDS where DELIVERY_SUBID = "+subid
                    + " AND TYPE = 'exp'"
                    + map.get("timeSQL") + map.get("publishSQL") + map.get("reasonSQL");
                getRecordsForSQL(sql, rh);
            }
        }
    }
    private void getRecordsForSQL(String sql, RowHandler rh) {
        intlogger.debug(sql);
        long start = System.currentTimeMillis();
        DB db = new DB();
        Connection conn = null;
        try {
            conn = db.getConnection();
           try( Statement  stmt = conn.createStatement()){
             try(ResultSet rs = stmt.executeQuery(sql)){
                 while (rs.next()) {
                     rh.handleRow(rs);
                 }
             }
           }
        } catch (SQLException sqlException) {
            intlogger.info("Failed to get Records. Exception = " +sqlException.getMessage(),sqlException);
        } finally {
            if (conn != null)
                db.release(conn);
        }
        intlogger.debug("Time: " + (System.currentTimeMillis()-start) + " ms");
    }
}
