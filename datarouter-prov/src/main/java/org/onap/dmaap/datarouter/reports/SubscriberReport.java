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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.onap.dmaap.datarouter.provisioning.utils.DB;

/**
 * Generate a subscribers report.  The report is a .CSV file.  It contains information per-day and per-subscriber,
 * on the status codes returned from each delivery attempt (1XX, 2XX, etc.) as well as a count of 4XX instead of a 100.
 *
 * @author Robert P. Eby
 * @version $Id: SubscriberReport.java,v 1.2 2013/11/06 16:23:55 eby Exp $
 */
public class SubscriberReport extends ReportBase {
    private static final String SELECT_SQL =
            "select date(from_unixtime(EVENT_TIME div 1000)) as DATE, DELIVERY_SUBID, RESULT, COUNT(RESULT) as COUNT" +
                    " from LOG_RECORDS" +
                    " where TYPE = 'del' and EVENT_TIME >= ? and EVENT_TIME <= ?" +
                    " group by DATE, DELIVERY_SUBID, RESULT";
    private static final String SELECT_SQL2 =
            "select date(from_unixtime(EVENT_TIME div 1000)) as DATE, DELIVERY_SUBID, COUNT(CONTENT_LENGTH_2) as COUNT" +
                    " from LOG_RECORDS" +
                    " where TYPE = 'dlx' and CONTENT_LENGTH_2 = -1 and EVENT_TIME >= ? and EVENT_TIME <= ?" +
                    " group by DATE, DELIVERY_SUBID";

    private class Counters {
        private String date;
        private int sub;
        private int c100, c200, c300, c400, c500, cm1, cdlx;

        public Counters(String date, int sub) {
            this.date = date;
            this.sub = sub;
            c100 = c200 = c300 = c400 = c500 = cm1 = cdlx = 0;
        }

        public void addCounts(int status, int n) {
            if (status < 0) {
                cm1 += n;
            } else if (status >= 100 && status <= 199) {
                c100 += n;
            } else if (status >= 200 && status <= 299) {
                c200 += n;
            } else if (status >= 300 && status <= 399) {
                c300 += n;
            } else if (status >= 400 && status <= 499) {
                c400 += n;
            } else if (status >= 500 && status <= 599) {
                c500 += n;
            }
        }

        public void addDlxCount(int n) {
            cdlx += n;
        }

        @Override
        public String toString() {
            return date + "," + sub + "," +
                    c100 + "," + c200 + "," + c300 + "," + c400 + "," + c500 + "," +
                    cm1 + "," + cdlx;
        }
    }

    @Override
    public void run() {
        Map<String, Counters> map = new HashMap<String, Counters>();
        long start = System.currentTimeMillis();

        try {
            DB db = new DB();
            @SuppressWarnings("resource")
            Connection conn = db.getConnection();
            try(PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
                ps.setLong(1, from);
                ps.setLong(2, to);
                try(ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String date = rs.getString("DATE");
                        int sub = rs.getInt("DELIVERY_SUBID");
                        int res = rs.getInt("RESULT");
                        int count = rs.getInt("COUNT");
                        String key = date + "," + sub;
                        Counters c = map.get(key);
                        if (c == null) {
                            c = new Counters(date, sub);
                            map.put(key, c);
                        }
                        c.addCounts(res, count);
                    }
                }
            }

           try( PreparedStatement ps2 = conn.prepareStatement(SELECT_SQL2)) {
               ps2.setLong(1, from);
               ps2.setLong(2, to);
               try(ResultSet rs2 = ps2.executeQuery()) {
                   while (rs2.next()) {
                       String date = rs2.getString("DATE");
                       int sub = rs2.getInt("DELIVERY_SUBID");
                       int count = rs2.getInt("COUNT");
                       String key = date + "," + sub;
                       Counters c = map.get(key);
                       if (c == null) {
                           c = new Counters(date, sub);
                           map.put(key, c);
                       }
                       c.addDlxCount(count);
                   }
                  }
           }

            db.release(conn);
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        }
        logger.debug("Query time: " + (System.currentTimeMillis() - start) + " ms");
        try (PrintWriter os = new PrintWriter(outfile)){
            os.println("date,subid,count100,count200,count300,count400,count500,countminus1,countdlx");
            for (String key : new TreeSet<String>(map.keySet())) {
                Counters c = map.get(key);
                os.println(c.toString());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File cannot be written: " + outfile);
            logger.error("FileNotFoundException: " + e.getMessage());
        }
    }
}
