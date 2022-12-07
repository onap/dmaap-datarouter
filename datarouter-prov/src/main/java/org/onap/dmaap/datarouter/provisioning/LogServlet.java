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

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import jakarta.servlet.ServletOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.onap.dmaap.datarouter.provisioning.beans.DeliveryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.ExpiryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.LOGJSONable;
import org.onap.dmaap.datarouter.provisioning.beans.PublishRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.eelf.EelfMsgs;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;


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
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(LogServlet.class);
    private static final long TWENTYFOUR_HOURS = (24 * 60 * 60 * 1000L);
    private static final String FMT_1 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String FMT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String PUBLISHSQL = "publishSQL";
    private static final String STATUSSQL = "statusSQL";
    private static final String RESULTSQL = "resultSQL";
    private static final String FILENAMESQL = "filenameSQL";
    private static final String TIMESQL = "timeSQL";
    private static final String LOG_RECORDSSQL = "select * from LOG_RECORDS where FEEDID = ";

    private final boolean isfeedlog;

    public abstract static class RowHandler {
        private final ServletOutputStream out;
        private final String[] fields;
        private boolean firstrow;

        /**
         * Row setter.
         * @param out ServletOutputStream
         * @param fieldparam String field
         * @param bool boolean
         */
        RowHandler(ServletOutputStream out, String fieldparam, boolean bool) {
            this.out = out;
            this.firstrow = bool;
            this.fields = (fieldparam != null) ? fieldparam.split(":") : null;
        }

        /**
         * Handling row from DB.
         * @param rs DB Resultset
         */
        void handleRow(ResultSet rs) {
            try {
                LOGJSONable js = buildJSONable(rs);
                LOGJSONObject jo = js.asJSONObject();
                if (fields != null) {
                    // filter out unwanted fields
                    LOGJSONObject j2 = new LOGJSONObject();
                    for (String key : fields) {
                        Object val = jo.opt(key);
                        if (val != null) {
                            j2.put(key, val);
                        }
                    }
                    jo = j2;
                }
                String str = firstrow ? "\n" : ",\n";
                str += jo.toString();
                out.print(str);
                firstrow = false;
            } catch (Exception exception) {
                intlogger.info("Failed to handle row. Exception = " + exception.getMessage(),exception);
            }
        }

        public abstract LOGJSONable buildJSONable(ResultSet rs) throws SQLException;
    }

    public static class PublishRecordRowHandler extends RowHandler {
        PublishRecordRowHandler(ServletOutputStream out, String fields, boolean bool) {
            super(out, fields, bool);
        }

        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new PublishRecord(rs);
        }
    }

    public static class DeliveryRecordRowHandler extends RowHandler {
        DeliveryRecordRowHandler(ServletOutputStream out, String fields, boolean bool) {
            super(out, fields, bool);
        }

        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new DeliveryRecord(rs);
        }
    }

    public static class ExpiryRecordRowHandler extends RowHandler {
        ExpiryRecordRowHandler(ServletOutputStream out, String fields, boolean bool) {
            super(out, fields, bool);
        }

        @Override
        public LOGJSONable buildJSONable(ResultSet rs) throws SQLException {
            return new ExpiryRecord(rs);
        }
    }

    /**
     * This class must be created from either a {@link FeedLogServlet} or a {@link SubLogServlet}.
     * @param isFeedLog boolean to handle those places where a feedlog request is different from a sublog request
     */
    LogServlet(boolean isFeedLog) {
        this.isfeedlog = isFeedLog;
    }

    /**
     * DELETE a logging URL -- not supported.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doDelete", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                req.getHeader(BEHALF_HEADER), getIdFromPath(req) + "");
            String message = "DELETE not allowed for the logURL.";
            EventLogRecord elr = new EventLogRecord(req);
            elr.setMessage(message);
            elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * GET a logging URL -- retrieve logging data for a feed or subscription.
     * See the <b>Logging API</b> document for details on how this method should be invoked.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doGet", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                req.getHeader(BEHALF_HEADER), getIdFromPath(req) + "");
            int id = getIdFromPath(req);
            if (id < 0) {
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or bad feed/subscription number.", eventlogger);
                return;
            }
            Map<String, String> map = buildMapFromRequest(req);
            if (map.get("err") != null) {
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid arguments: " + map.get("err"), eventlogger);
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
                eventlogger.error("PROV0141 LogServlet.doGet: " + ioe.getMessage(), ioe);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * PUT a logging URL -- not supported.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doPut", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                req.getHeader(BEHALF_HEADER),getIdFromPath(req) + "");
            String message = "PUT not allowed for the logURL.";
            EventLogRecord elr = new EventLogRecord(req);
            elr.setMessage(message);
            elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * POST a logging URL -- not supported.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doPost", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF, req.getHeader(BEHALF_HEADER));
            String message = "POST not allowed for the logURL.";
            EventLogRecord elr = new EventLogRecord(req);
            elr.setMessage(message);
            elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    private Map<String, String> buildMapFromRequest(HttpServletRequest req) {
        Map<String, String> map = new HashMap<>();
        String str = req.getParameter("type");
        if (str != null) {
            if ("pub".equals(str) || "del".equals(str) || "exp".equals(str)) {
                map.put("type", str);
            } else {
                map.put("err", "bad type");
                return map;
            }
        } else {
            map.put("type", "all");
        }
        map.put(PUBLISHSQL, "");
        map.put(STATUSSQL, "");
        map.put(RESULTSQL, "");
        map.put(REASON_SQL, "");
        map.put(FILENAMESQL, "");

        str = req.getParameter("publishId");
        if (str != null) {
            if (str.indexOf("'") >= 0) {
                map.put("err", "bad publishId");
                return map;
            }
            map.put(PUBLISHSQL, " AND PUBLISH_ID = '" + str + "'");
        }

        str = req.getParameter("filename");
        if (str != null) {
            map.put(FILENAMESQL, " AND FILENAME = '" + str + "'");
        }

        str = req.getParameter("statusCode");
        if (str != null) {
            String sql = null;
            switch (str) {
                case "success":
                    sql = " AND STATUS >= 200 AND STATUS < 300";
                    break;
                case "redirect":
                    sql = " AND STATUS >= 300 AND STATUS < 400";
                    break;
                case "failure":
                    sql = " AND STATUS >= 400";
                    break;
                default:
                    try {
                        int statusCode = Integer.parseInt(str);
                        if ((statusCode >= 100 && statusCode < 600) || (statusCode == -1)) {
                            sql = " AND STATUS = " + statusCode;
                        }
                    } catch (NumberFormatException e) {
                        intlogger.error("Failed to parse input", e);
                    }
                    break;
            }
            if (sql == null) {
                map.put("err", "bad statusCode");
                return map;
            }
            map.put(STATUSSQL, sql);
            map.put(RESULTSQL, sql.replaceAll("STATUS", "RESULT"));
        }

        str = req.getParameter("expiryReason");
        if (str != null) {
            map.put("type", "exp");
            switch (str) {
                case "notRetryable":
                    map.put(REASON_SQL, " AND REASON = 'notRetryable'");
                    break;
                case "retriesExhausted":
                    map.put(REASON_SQL, " AND REASON = 'retriesExhausted'");
                    break;
                case "diskFull":
                    map.put(REASON_SQL, " AND REASON = 'diskFull'");
                    break;
                case "other":
                    map.put(REASON_SQL, " AND REASON = 'other'");
                    break;
                default:
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
        map.put(TIMESQL, String.format(" AND EVENT_TIME >= %d AND EVENT_TIME <= %d", stime, etime));
        return map;
    }

    private long getTimeFromParam(final String str) {
        if (str == null) {
            return 0;
        }
        try {
            // First, look for an RFC 3339 date
            String fmt = (str.indexOf('.') > 0) ? FMT_2 : FMT_1;
            SimpleDateFormat sdf = new SimpleDateFormat(fmt);
            Date date = sdf.parse(str);
            return date.getTime();
        } catch (ParseException parseException) {
            intlogger.error("Exception in getting Time :- " + parseException.getMessage(),parseException);
        }
        try {
            // Also allow a long (in ms); useful for testing
            return Long.parseLong(str);
        } catch (NumberFormatException numberFormatException) {
            intlogger.error("Exception in getting Time :- " + numberFormatException.getMessage(),numberFormatException);
        }
        intlogger.info("Error parsing time=" + str);
        return -1;
    }

    private void getPublishRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if ("all".equals(type) || "pub".equals(type)) {
            String sql = LOG_RECORDSSQL + feedid
                + " AND TYPE = 'pub'"
                + map.get(TIMESQL) + map.get(PUBLISHSQL) + map.get(STATUSSQL) + map.get(FILENAMESQL);
            getRecordsForSQL(sql, rh);
        }
    }

    private void getDeliveryRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if ("all".equals(type) || "del".equals(type)) {
            String sql = LOG_RECORDSSQL + feedid
                + " AND TYPE = 'del'"
                + map.get(TIMESQL) + map.get(PUBLISHSQL) + map.get(RESULTSQL);
            getRecordsForSQL(sql, rh);
        }
    }

    private void getDeliveryRecordsForSubscription(int subid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if ("all".equals(type) || "del".equals(type)) {
            String sql = "select * from LOG_RECORDS where DELIVERY_SUBID = " + subid
                + " AND TYPE = 'del'"
                + map.get(TIMESQL) + map.get(PUBLISHSQL) + map.get(RESULTSQL);
            getRecordsForSQL(sql, rh);
        }
    }

    private void getExpiryRecordsForFeed(int feedid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if ("all".equals(type) || "exp".equals(type)) {
            String st = map.get(STATUSSQL);
            if (st == null || st.length() == 0) {
                String sql = LOG_RECORDSSQL + feedid
                    + " AND TYPE = 'exp'"
                    + map.get(TIMESQL) + map.get(PUBLISHSQL) + map.get(REASON_SQL);
                getRecordsForSQL(sql, rh);
            }
        }
    }

    private void getExpiryRecordsForSubscription(int subid, RowHandler rh, Map<String, String> map) {
        String type = map.get("type");
        if ("all".equals(type) || "exp".equals(type)) {
            String st = map.get(STATUSSQL);
            if (st == null || st.length() == 0) {
                String sql = "select * from LOG_RECORDS where DELIVERY_SUBID = " + subid
                    + " AND TYPE = 'exp'"
                    + map.get(TIMESQL) + map.get(PUBLISHSQL) + map.get(REASON_SQL);
                getRecordsForSQL(sql, rh);
            }
        }
    }

    private void getRecordsForSQL(String sql, RowHandler rh) {
        intlogger.debug(sql);
        long start = System.currentTimeMillis();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rh.handleRow(rs);
            }
        } catch (SQLException sqlException) {
            intlogger.info("Failed to get Records. Exception = " + sqlException.getMessage(),sqlException);
        }
        intlogger.debug("Time: " + (System.currentTimeMillis() - start) + " ms");
    }
}
