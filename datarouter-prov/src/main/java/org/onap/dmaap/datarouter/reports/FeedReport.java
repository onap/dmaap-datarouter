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


package org.onap.dmaap.datarouter.reports;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;

/**
 * Generate a feeds report.  The report is a .CSV file.
 *
 * @author Robert P. Eby
 * @version $Id: FeedReport.java,v 1.2 2013/11/06 16:23:55 eby Exp $
 */
public class FeedReport extends ReportBase {

    @Override
    public void run() {
        boolean alg1 = true;
        JSONObject jo = new JSONObject();
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                // Note to use the time in the publish_id, use date(from_unixtime(substring(publish_id, 1, 10)))
                // To just use month, substring(from_unixtime(event_time div 1000), 1, 7)
                "select date(from_unixtime(event_time div 1000)) as date, type, feedid, delivery_subid, count(*) "
                    + "as count from LOG_RECORDS where type = 'pub' or type = 'del' group by date, type, feedid, delivery_subid")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("date");
                    String type = rs.getString("type");
                    int feedid = rs.getInt("feedid");
                    int subid = type.equals("del") ? rs.getInt("delivery_subid") : 0;
                    int count = rs.getInt("count");
                    sb.append(date + "," + type + "," + feedid + "," + subid + "," + count + "\n");
                }
            }
        } catch (SQLException e) {
            logger.error(e.toString());
        }
        logger.debug("Query time: " + (System.currentTimeMillis() - start) + " ms");
        try (PrintWriter os = new PrintWriter(outfile)) {
            os.print("date,type,feedid,subid,count\n");
            os.print(sb.toString());
        } catch (FileNotFoundException e) {
            System.err.println("File cannot be written: " + outfile);
            logger.error(e.toString());
        }
    }

    public void run2() {
        JSONObject jo = new JSONObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long start = System.currentTimeMillis();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "select PUBLISH_ID, TYPE, FEEDID, DELIVERY_SUBID from LOG_RECORDS "
                    + "where EVENT_TIME >= ? and EVENT_TIME <= ?")) {
            ps.setLong(1, from);
            ps.setLong(2, to);
            ps.setFetchSize(100000);
            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("PUBLISH_ID");
                    String date = sdf.format(new Date(getPstart(id)));
                    JSONObject datemap = jo.optJSONObject(date);
                    if (datemap == null) {
                        datemap = new JSONObject();
                        jo.put(date, datemap);
                    }
                    int feed = rs.getInt("FEEDID");
                    JSONObject feedmap = datemap.optJSONObject("" + feed);
                    if (feedmap == null) {
                        feedmap = new JSONObject();
                        feedmap.put("pubcount", 0);
                        datemap.put("" + feed, feedmap);
                    }
                    String type = rs.getString("TYPE");
                    if (type.equals("pub")) {
                        try {
                            int n = feedmap.getInt("pubcount");
                            feedmap.put("pubcount", n + 1);
                        } catch (JSONException e) {
                            feedmap.put("pubcount", 1);
                            logger.error(e.toString());
                        }
                    } else if (type.equals("del")) {
                        String subid = "" + rs.getInt("DELIVERY_SUBID");
                        try {
                            int n = feedmap.getInt(subid);
                            feedmap.put(subid, n + 1);
                        } catch (JSONException e) {
                            feedmap.put(subid, 1);
                            logger.error(e.toString());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error(e.toString());
        }
        logger.debug("Query time: " + (System.currentTimeMillis() - start) + " ms");
        try {
            PrintWriter os = new PrintWriter(outfile);
            os.println(toHTML(jo));
            os.close();
        } catch (FileNotFoundException e) {
            System.err.println("File cannot be written: " + outfile);
            logger.error(e.toString());
        }
    }

    private long getPstart(String t) {
        if (t.indexOf('.') > 0)
            t = t.substring(0, t.indexOf('.'));
        return Long.parseLong(t);
    }

    @SuppressWarnings("unused")
    private static String toHTMLNested(JSONObject jo) {
        StringBuilder s = new StringBuilder();
        s.append("<table>\n");
        s.append("<tr><th>Date</th><th>Feeds</th></tr>\n");
        String[] dates = JSONObject.getNames(jo);
        Arrays.sort(dates);
        for (int i = dates.length - 1; i >= 0; i--) {
            String date = dates[i];
            JSONObject j2 = jo.getJSONObject(date);
            String[] feeds = JSONObject.getNames(j2);
            Arrays.sort(feeds);
            s.append("<tr><td>" + date + "</td><td>");
            s.append(feeds.length).append(feeds.length > 1 ? " Feeds\n" : " Feed\n");
            s.append("<table>\n");
            s.append("<tr><th>Feed ID</th><th>Publish Count</th><th>Subscriptions</th></tr>\n");
            for (String feed : feeds) {
                JSONObject j3 = j2.getJSONObject(feed);
                String[] subs = JSONObject.getNames(j3);
                Arrays.sort(subs);
                s.append("<tr><td>" + feed + "</td>");
                s.append("<td>" + j3.getInt("pubcount") + "</td>");
                int scnt = j3.length() - 1;
                s.append("<td>").append(scnt).append(" Subcription");
                if (scnt > 1)
                    s.append("s");
                s.append("<table>\n");
                s.append("<tr><th>Sub ID</th><th>Delivery Count</th></tr>\n");
                for (String sub : subs) {
                    if (!sub.equals("pubcount")) {
                        s.append("<tr><td>" + sub + "</td>");
                        s.append("<td>" + j3.getInt(sub) + "</td>");
                        s.append("</td></tr>\n");
                    }
                }
                s.append("</table>\n");

                s.append("</td></tr>\n");
            }
            s.append("</table>\n");
            s.append("</td></tr>\n");
        }
        s.append("</table>\n");
        return s.toString();
    }

    private static String toHTML(JSONObject jo) {
        StringBuilder s = new StringBuilder();
        s.append("<table>\n");
        s.append("<tr><th>Date</th><th>Feeds</th><th>Feed ID</th><th>Publish Count</th><th>Subs</th><th>Sub ID</th><th>Delivery Count</th></tr>\n");
        String[] dates = JSONObject.getNames(jo);
        Arrays.sort(dates);
        for (int i = dates.length - 1; i >= 0; i--) {
            String date = dates[i];
            JSONObject j2 = jo.getJSONObject(date);
            int rc1 = countrows(j2);
            String[] feeds = JSONObject.getNames(j2);
            Arrays.sort(feeds);
            s.append("<tr><td rowspan=\"" + rc1 + "\">")
                .append(date)
                .append("</td>");
            s.append("<td rowspan=\"" + rc1 + "\">")
                .append(feeds.length)
                .append("</td>");
            String px1 = "";
            for (String feed : feeds) {
                JSONObject j3 = j2.getJSONObject(feed);
                int pubcount = j3.getInt("pubcount");
                int subcnt = j3.length() - 1;
                int rc2 = (subcnt < 1) ? 1 : subcnt;
                String[] subs = JSONObject.getNames(j3);
                Arrays.sort(subs);
                s.append(px1)
                    .append("<td rowspan=\"" + rc2 + "\">")
                    .append(feed)
                    .append("</td>");
                s.append("<td rowspan=\"" + rc2 + "\">")
                    .append(pubcount)
                    .append("</td>");
                s.append("<td rowspan=\"" + rc2 + "\">")
                    .append(subcnt)
                    .append("</td>");
                String px2 = "";
                for (String sub : subs) {
                    if (!sub.equals("pubcount")) {
                        s.append(px2);
                        s.append("<td>" + sub + "</td>");
                        s.append("<td>" + j3.getInt(sub) + "</td>");
                        s.append("</tr>\n");
                        px2 = "<tr>";
                    }
                }
                if (px2.equals(""))
                    s.append("<td></td><td></td></tr>\n");
                px1 = "<tr>";
            }
        }
        s.append("</table>\n");
        return s.toString();
    }

    private static int countrows(JSONObject x) {
        int n = 0;
        for (String feed : JSONObject.getNames(x)) {
            JSONObject j3 = x.getJSONObject(feed);
            int subcnt = j3.length() - 1;
            int rc2 = (subcnt < 1) ? 1 : subcnt;
            n += rc2;
        }
        return (n > 0) ? n : 1;
    }

    /**
     * Convert a .CSV file (as generated by the normal FeedReport mechanism) to an HTML table.
     *
     * @param args
     */
    public void main(String[] args) {
        int rtype = 0;    // 0 -> day, 1 -> week, 2 -> month, 3 -> year
        String infile = null;
        String outfile = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-t")) {
                switch (args[++i].charAt(0)) {
                    case 'w':
                        rtype = 1;
                        break;
                    case 'm':
                        rtype = 2;
                        break;
                    case 'y':
                        rtype = 3;
                        break;
                    default:
                        rtype = 0;
                        break;
                }
            } else if (infile == null) {
                infile = args[i];
            } else if (outfile == null) {
                outfile = args[i];
            }
        }
        if (infile == null) {
            System.err.println("usage: FeedReport [ -t <reporttype> ] [ <input .csv> ] [ <output .html> ]");
            System.exit(1);
        }
        try {
            JSONObject jo = new JSONObject();
            try(LineNumberReader lr = new LineNumberReader(new FileReader(infile))) {
                String line = lr.readLine();
                while (line != null) {
                    String[] tt = line.split(",");
                    if (tt[0].startsWith("2")) {
                        String date = tt[0];
                        switch (rtype) {
                            case 1:
                                String[] xx = date.split("-");
                                Calendar cal = new GregorianCalendar(new Integer(xx[0]), new Integer(xx[1]) - 1, new Integer(xx[2]));
                                date = xx[0] + "-W" + cal.get(Calendar.WEEK_OF_YEAR);
                                break;
                            case 2:
                                date = date.substring(0, 7);
                                break;
                            case 3:
                                date = date.substring(0, 4);
                                break;
                        }
                        JSONObject datemap = jo.optJSONObject(date);
                        if (datemap == null) {
                            datemap = new JSONObject();
                            jo.put(date, datemap);
                        }
                        int feed = Integer.parseInt(tt[2]);
                        JSONObject feedmap = datemap.optJSONObject("" + feed);
                        if (feedmap == null) {
                            feedmap = new JSONObject();
                            feedmap.put("pubcount", 0);
                            datemap.put("" + feed, feedmap);
                        }
                        String type = tt[1];
                        int count = Integer.parseInt(tt[4]);
                        if (type.equals("pub")) {
                            try {
                                int n = feedmap.getInt("pubcount");
                                feedmap.put("pubcount", n + count);
                            } catch (JSONException e) {
                                feedmap.put("pubcount", count);
                                logger.error(e.toString());
                            }
                        } else if (type.equals("del")) {
                            String subid = tt[3];
                            try {
                                int n = feedmap.getInt(subid);
                                feedmap.put(subid, n + count);
                            } catch (JSONException e) {
                                feedmap.put(subid, count);
                                logger.error(e.toString());
                            }
                        }
                    }
                    line = lr.readLine();
                }
            }
            String t = toHTML(jo);
            switch (rtype) {
                case 1:
                    t = t.replaceAll("<th>Date</th>", "<th>Week</th>");
                    break;
                case 2:
                    t = t.replaceAll("<th>Date</th>", "<th>Month</th>");
                    break;
                case 3:
                    t = t.replaceAll("<th>Date</th>", "<th>Year</th>");
                    break;
            }
            System.out.println(t);
        } catch (Exception e) {
            System.err.println(e);
            logger.error(e.toString());
        }
    }
}
