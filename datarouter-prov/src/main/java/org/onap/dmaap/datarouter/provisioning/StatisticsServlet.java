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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

/**
 * This Servlet handles requests to the &lt;Statistics API&gt; and  &lt;Statistics consilidated
 * resultset&gt;,
 *
 * @author Manish Singh
 * @version $Id: StatisticsServlet.java,v 1.11 2016/08/10 17:27:02 Manish Exp $
 */
@SuppressWarnings("serial")

public class StatisticsServlet extends BaseServlet {

  private static final long TWENTYFOUR_HOURS = (24 * 60 * 60 * 1000L);
  private static final String FMT1 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final String FMT2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";



  /**
   * DELETE a logging URL -- not supported.
   */
  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
    String message = "DELETE not allowed for the logURL.";
    EventLogRecord elr = new EventLogRecord(req);
    elr.setMessage(message);
    elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    eventlogger.error(elr.toString());
    sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
  }

  /**
   * GET a Statistics URL -- retrieve Statistics data for a feed or subscription. See the
   * <b>Statistics API</b> document for details on how this     method should be invoked.
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) {

    Map<String, String> map = buildMapFromRequest(req);
    if (map.get("err") != null) {
      sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid arguments: " + map.get("err"), eventlogger);
      return;
    }
    // check Accept: header??

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType(LOGLIST_CONTENT_TYPE);

    String outputType = "json";

    if (req.getParameter(FEEDID) == null && req.getParameter(GROUPID) == null) {
      try {
        resp.getOutputStream().print("Invalid request, Feedid or Group ID is required.");
      } catch (IOException ioe) {
        eventlogger.error("PROV0171 StatisticsServlet.doGet: " + ioe.getMessage(), ioe);
      }
    }

    if (req.getParameter(FEEDID) != null && req.getParameter(GROUPID) == null) {
      map.put(FEEDIDS, req.getParameter(FEEDID).replace("|", ","));
    }

    if (req.getParameter(GROUPID) != null && req.getParameter(FEEDID) == null) {
      StringBuffer groupid1 = new StringBuffer();

      try {
        groupid1 = this.getFeedIdsByGroupId(Integer.parseInt(req.getParameter(GROUPID)));
        map.put(FEEDIDS, groupid1.toString());
      } catch (NumberFormatException | SQLException e) {
        eventlogger.error("PROV0172 StatisticsServlet.doGet: " + e.getMessage(), e);
      }
    }
    if (req.getParameter(GROUPID) != null && req.getParameter(FEEDID) != null) {
      StringBuffer groupid1 = new StringBuffer();

      try {
        groupid1 = this.getFeedIdsByGroupId(Integer.parseInt(req.getParameter(GROUPID)));
        groupid1.append(",");
        groupid1.append(req.getParameter(FEEDID).replace("|", ","));
        map.put(FEEDIDS, groupid1.toString());
      } catch (NumberFormatException | SQLException e) {
        eventlogger.error("PROV0173 StatisticsServlet.doGet: " + e.getMessage(), e);
      }
    }

    if (req.getParameter(SUBID) != null && req.getParameter(FEEDID) != null) {
      StringBuffer subidstr = new StringBuffer();
      subidstr.append("and e.DELIVERY_SUBID in(");

      subidstr.append(req.getParameter(SUBID).replace("|", ","));
      subidstr.append(")");
      map.put(SUBID, subidstr.toString());
    }
    if (req.getParameter(SUBID) != null && req.getParameter(GROUPID) != null) {
      StringBuffer subidstr = new StringBuffer();
      subidstr.append("and e.DELIVERY_SUBID in(");

      subidstr.append(req.getParameter(SUBID).replace("|", ","));
      subidstr.append(")");
      map.put(SUBID, subidstr.toString());
    }
    if (req.getParameter("type") != null) {
      map.put(EVENT_TYPE, req.getParameter("type").replace("|", ","));
    }
    if (req.getParameter(OUTPUT_TYPE) != null) {
      map.put(OUTPUT_TYPE, req.getParameter(OUTPUT_TYPE));
    }
    if (req.getParameter(START_TIME) != null) {
      map.put(START_TIME, req.getParameter(START_TIME));
    }
    if (req.getParameter(END_TIME) != null) {
      map.put(END_TIME, req.getParameter(END_TIME));
    }

    if (req.getParameter("time") != null) {
      map.put(START_TIME, req.getParameter("time"));
      map.put(END_TIME, null);
    }

    if (req.getParameter(OUTPUT_TYPE) != null) {
      outputType = req.getParameter(OUTPUT_TYPE);
    }
    try {
      this.getRecordsForSQL(map, outputType, resp.getOutputStream(), resp);
    } catch (IOException ioe) {
      eventlogger.error("PROV0174 StatisticsServlet.doGet: " +  ioe.getMessage(), ioe);
    }

  }


  /**
   * rsToJson - Converting RS to JSON object
   *
   * @param out ServletOutputStream, rs as ResultSet
   * @throws IOException, SQLException
   */
  public void rsToCSV(ResultSet rs, ServletOutputStream out) throws IOException, SQLException {
    String header = "FEEDNAME,FEEDID,FILES_PUBLISHED,PUBLISH_LENGTH, FILES_DELIVERED, DELIVERED_LENGTH, SUBSCRIBER_URL, SUBID, PUBLISH_TIME,DELIVERY_TIME, AverageDelay\n";

    out.write(header.getBytes());

    while (rs.next()) {
      StringBuffer line = new StringBuffer();
      line.append(rs.getString("FEEDNAME"));
      line.append(",");
      line.append(rs.getString("FEEDID"));
      line.append(",");
      line.append(rs.getString("FILES_PUBLISHED"));
      line.append(",");
      line.append(rs.getString("PUBLISH_LENGTH"));
      line.append(",");
      line.append(rs.getString("FILES_DELIVERED"));
      line.append(",");
      line.append(rs.getString("DELIVERED_LENGTH"));
      line.append(",");
      line.append(rs.getString("SUBSCRIBER_URL"));
      line.append(",");
      line.append(rs.getString("SUBID"));
      line.append(",");
      line.append(rs.getString("PUBLISH_TIME"));
      line.append(",");
      line.append(rs.getString("DELIVERY_TIME"));
      line.append(",");
      line.append(rs.getString("AverageDelay"));
      line.append(",");

      line.append("\n");
      out.write(line.toString().getBytes());
      out.flush();
    }
  }

  /**
   * rsToJson - Converting RS to JSON object
   *
   * @param out ServletOutputStream, rs as ResultSet
   * @throws IOException, SQLException
   */
  public void rsToJson(ResultSet rs, ServletOutputStream out) throws IOException, SQLException {

    String[] fields = {"FEEDNAME", "FEEDID", "FILES_PUBLISHED", "PUBLISH_LENGTH", "FILES_DELIVERED",
        "DELIVERED_LENGTH", "SUBSCRIBER_URL", "SUBID", "PUBLISH_TIME", "DELIVERY_TIME",
        "AverageDelay"};
    StringBuffer line = new StringBuffer();

    line.append("[\n");

    while (rs.next()) {
      LOGJSONObject j2 = new LOGJSONObject();
      for (String key : fields) {
        Object v = rs.getString(key);
        if (v != null) {
          j2.put(key.toLowerCase(), v);
        } else {
          j2.put(key.toLowerCase(), "");
        }
      }
      line = line.append(j2.toString());
      line.append(",\n");
    }
    line.append("]");
    out.print(line.toString());
  }

  /**
   * getFeedIdsByGroupId - Getting FEEDID's by GROUP ID.
   *
   * @throws SQLException Query SQLException.
   */
  public StringBuffer getFeedIdsByGroupId(int groupIds) throws SQLException {

    DB db = null;
    Connection conn = null;
    ResultSet resultSet = null;
    String sqlGoupid = null;
    StringBuffer feedIds = new StringBuffer();

    try {
      db = new DB();
      conn = db.getConnection();
      sqlGoupid = " SELECT FEEDID from FEEDS  WHERE GROUPID = ?";
      try(PreparedStatement prepareStatement = conn.prepareStatement(sqlGoupid)) {
          prepareStatement.setInt(1, groupIds);
          resultSet = prepareStatement.executeQuery();
          while (resultSet.next()) {
              feedIds.append(resultSet.getInt("FEEDID"));
              feedIds.append(",");
          }
          feedIds.deleteCharAt(feedIds.length() - 1);
          System.out.println("feedIds" + feedIds.toString());
      }
    } catch (SQLException e) {
      eventlogger.error("PROV0175 StatisticsServlet.getFeedIdsByGroupId: " + e.getMessage(), e);
    } finally {
      try {
        if (resultSet != null) {
          resultSet.close();
          resultSet = null;
        }
          if (conn != null) {
          db.release(conn);
        }
      } catch (Exception e) {
        eventlogger.error("PROV0176 StatisticsServlet.getFeedIdsByGroupId: " + e.getMessage(), e);
      }
    }
    return feedIds;
  }


  /**
   * queryGeneretor - Generating sql query
   *
   * @param map as key value pare of all user input fields
   */
  public String queryGeneretor(Map<String, String> map) throws ParseException {

    String sql = null;
    String eventType = null;
    String feedids = null;
    String start_time = null;
    String end_time = null;
    String subid = " ";
    if (map.get(EVENT_TYPE) != null) {
      eventType =  map.get(EVENT_TYPE);
    }
    if (map.get(FEEDIDS) != null) {
      feedids = map.get(FEEDIDS);
    }
    if (map.get(START_TIME) != null) {
      start_time = map.get(START_TIME);
    }
    if (map.get(END_TIME) != null) {
      end_time =  map.get(END_TIME);
    }
    if ("all".equalsIgnoreCase(eventType)) {
      eventType = "PUB','DEL, EXP, PBF";
    }
    if (map.get(SUBID) != null) {
      subid = map.get(SUBID);
    }

    eventlogger.info("Generating sql query to get Statistics resultset. ");

    if (end_time == null && start_time == null) {

      sql = "SELECT (SELECT NAME FROM FEEDS AS f WHERE f.FEEDID in(" + feedids
          + ") and f.FEEDID=e.FEEDID) AS FEEDNAME, e.FEEDID as FEEDID, (SELECT COUNT(*) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
          + feedids
          + ") and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS FILES_PUBLISHED,(SELECT SUM(content_length) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
          + feedids
          + ")  and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS PUBLISH_LENGTH, COUNT(e.EVENT_TIME) as FILES_DELIVERED,  sum(m.content_length) as DELIVERED_LENGTH,SUBSTRING_INDEX(e.REQURI,'/',+3) as SUBSCRIBER_URL, e.DELIVERY_SUBID as SUBID, e.EVENT_TIME AS PUBLISH_TIME, m.EVENT_TIME AS DELIVERY_TIME,  AVG(e.EVENT_TIME - m.EVENT_TIME)/1000 as AverageDelay FROM LOG_RECORDS e JOIN LOG_RECORDS m ON m.PUBLISH_ID = e.PUBLISH_ID AND e.FEEDID IN ("
          + feedids + ") " + subid + " AND m.STATUS=204 AND e.RESULT=204  group by SUBID";

      return sql;
    } else if (start_time != null && end_time == null) {

      long inputTimeInMilli = 60000 * Long.parseLong(start_time);
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      long currentTimeInMilli = cal.getTimeInMillis();
      long compareTime = currentTimeInMilli - inputTimeInMilli;

      sql = "SELECT (SELECT NAME FROM FEEDS AS f WHERE f.FEEDID in(" + feedids
          + ") and f.FEEDID=e.FEEDID) AS FEEDNAME, e.FEEDID as FEEDID, (SELECT COUNT(*) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
          + feedids
          + ") and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS FILES_PUBLISHED,(SELECT SUM(content_length) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
          + feedids
          + ")  and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS PUBLISH_LENGTH, COUNT(e.EVENT_TIME) as FILES_DELIVERED,  sum(m.content_length) as DELIVERED_LENGTH,SUBSTRING_INDEX(e.REQURI,'/',+3) as SUBSCRIBER_URL, e.DELIVERY_SUBID as SUBID, e.EVENT_TIME AS PUBLISH_TIME, m.EVENT_TIME AS DELIVERY_TIME,  AVG(e.EVENT_TIME - m.EVENT_TIME)/1000 as AverageDelay FROM LOG_RECORDS e JOIN LOG_RECORDS m ON m.PUBLISH_ID = e.PUBLISH_ID AND e.FEEDID IN ("
          + feedids + ") " + subid + " AND m.STATUS=204 AND e.RESULT=204 and e.event_time>="
          + compareTime + " group by SUBID";

      return sql;

    } else {
      SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      Date startDate = inFormat.parse(start_time);
      Date endDate = inFormat.parse(end_time);

      long startInMillis = startDate.getTime();
      long endInMillis = endDate.getTime();

      {

        sql = "SELECT (SELECT NAME FROM FEEDS AS f WHERE f.FEEDID in(" + feedids
            + ") and f.FEEDID=e.FEEDID) AS FEEDNAME, e.FEEDID as FEEDID, (SELECT COUNT(*) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
            + feedids
            + ") and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS FILES_PUBLISHED,(SELECT SUM(content_length) FROM LOG_RECORDS AS c WHERE c.FEEDID in("
            + feedids
            + ")  and c.FEEDID=e.FEEDID AND c.TYPE='PUB') AS PUBLISH_LENGTH, COUNT(e.EVENT_TIME) as FILES_DELIVERED,  sum(m.content_length) as DELIVERED_LENGTH,SUBSTRING_INDEX(e.REQURI,'/',+3) as SUBSCRIBER_URL, e.DELIVERY_SUBID as SUBID, e.EVENT_TIME AS PUBLISH_TIME, m.EVENT_TIME AS DELIVERY_TIME,  AVG(e.EVENT_TIME - m.EVENT_TIME)/1000 as AverageDelay FROM LOG_RECORDS e JOIN LOG_RECORDS m ON m.PUBLISH_ID = e.PUBLISH_ID AND e.FEEDID IN ("
            + feedids + ") " + subid
            + " AND m.STATUS=204 AND e.RESULT=204 and e.event_time between " + startInMillis
            + " and " + endInMillis + " group by SUBID";

      }
      return sql;
    }
  }


  /**
   * PUT a Statistics URL -- not supported.
   */
  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse resp) {
    String message = "PUT not allowed for the StatisticsURL.";
    EventLogRecord elr = new EventLogRecord(req);
    elr.setMessage(message);
    elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    eventlogger.error(elr.toString());
    sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
  }

  /**
   * POST a Statistics URL -- not supported.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    String message = "POST not allowed for the StatisticsURL.";
    EventLogRecord elr = new EventLogRecord(req);
    elr.setMessage(message);
    elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    eventlogger.error(elr.toString());
    sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
  }

  private Map<String, String> buildMapFromRequest(HttpServletRequest req) {
    Map<String, String> map = new HashMap<>();
    String s = req.getParameter("type");
    if (s != null) {
      if ("pub".equals(s) || "del".equals(s) || "exp".equals(s)) {
        map.put("type", s);
      } else {
        map.put("err", "bad type");
        return map;
      }
    } else {
      map.put("type", "all");
    }
    map.put("publishSQL", "");
    map.put("statusSQL", "");
    map.put("resultSQL", "");
    map.put(REASON_SQL, "");

    s = req.getParameter("publishId");
    if (s != null) {
      if (s.indexOf("'") >= 0) {
        map.put("err", "bad publishId");
        return map;
      }
      map.put("publishSQL", " AND PUBLISH_ID = '" + s + "'");
    }

    s = req.getParameter("statusCode");
    if (s != null) {
      String sql = null;
      if ("success".equals(s)) {
        sql = " AND STATUS >= 200 AND STATUS < 300";
      } else if ("redirect".equals(s)) {
        sql = " AND STATUS >= 300 AND STATUS < 400";
      } else if ("failure".equals(s)) {
        sql = " AND STATUS >= 400";
      } else {
        try {
          Integer n = Integer.parseInt(s);
          if ((n >= 100 && n < 600) || (n == -1)) {
            sql = " AND STATUS = " + n;
          }
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
      if ("notRetryable".equals(s)) {
        map.put(REASON_SQL, " AND REASON = 'notRetryable'");
      } else if ("retriesExhausted".equals(s)) {
        map.put(REASON_SQL, " AND REASON = 'retriesExhausted'");
      } else if ("diskFull".equals(s)) {
        map.put(REASON_SQL, " AND REASON = 'diskFull'");
      } else if ("other".equals("other")) {
        map.put(REASON_SQL, " AND REASON = 'other'");
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
    if (s == null) {
      return 0;
    }
    try {
      // First, look for an RFC 3339 date
      String fmt = (s.indexOf('.') > 0) ? FMT2 : FMT1;
      SimpleDateFormat sdf = new SimpleDateFormat(fmt);
      Date d = sdf.parse(s);
      return d.getTime();
    } catch (ParseException e) {
    }
    try {
      // Also allow a long (in ms); useful for testing
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
    }
    intlogger.info("Error parsing time=" + s);
    return -1;
  }

  private void getRecordsForSQL(Map<String, String> map, String outputType, ServletOutputStream out, HttpServletResponse resp) {
    try {

      String filterQuery = this.queryGeneretor(map);
      eventlogger.debug("SQL Query for Statistics resultset. " + filterQuery);
      intlogger.debug(filterQuery);
      long start = System.currentTimeMillis();
      DB db = new DB();
      try (Connection conn = db.getConnection()) {
        try (ResultSet rs = conn.prepareStatement(filterQuery).executeQuery()) {
          if ("csv".equals(outputType)) {
            resp.setContentType("application/octet-stream");
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:mm:ss");
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"result:" + dateFormat.format(date) + ".csv\"");
            eventlogger.info("Generating CSV file from Statistics resultset");

            rsToCSV(rs, out);
          } else {
            eventlogger.info("Generating JSON for Statistics resultset");
            this.rsToJson(rs, out);
          }
        }
      } catch (SQLException e) {
        eventlogger.error("SQLException:" + e);
      }
      intlogger.debug("Time: " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      eventlogger.error("IOException - Generating JSON/CSV:" + e);
    } catch (JSONException e) {
      eventlogger.error("JSONException - executing SQL query:" + e);
    } catch (ParseException e) {
      eventlogger.error("ParseException - executing SQL query:" + e);
    }
  }
}
