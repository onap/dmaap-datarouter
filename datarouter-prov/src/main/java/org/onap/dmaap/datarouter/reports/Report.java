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

import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class provides a CLI to generate any of the reports defined in this package.
 *
 * @author Robert P. Eby
 * @version $Id: Report.java,v 1.2 2013/11/06 16:23:55 eby Exp $
 */
public class Report {
    /**
     * Generate .csv report files from the database.  Usage:
     * <pre>
     * java org.onap.dmaap.datarouter.reports.Report [ -t <i>type</i> ] [ -o <i>outfile</i> ] [ <i>fromdate</i> [ <i>todate</i> ]]
     * </pre>
     * <i>type</i> should be <b>volume</b> for a {@link VolumeReport},
     * <b>feed</b> for a {@link FeedReport},
     * <b>latency</b> for a {@link LatencyReport}, or
     * <b>dailyLatency</b> for a {@link DailyLatencyReport}.
     * If <i>outfile</i> is not specified, the report goes into a file <i>/tmp/nnnnnnnnnnnnn.csv</i>,
     * where nnnnnnnnnnnnn is the current time in milliseconds.
     * If <i>from</i> and <i>to</i> are not specified, then the report is limited to the last weeks worth of data.
     * <i>from</i> can be the keyword <b>ALL</b> to specify all data in the DB, or the keyword <b>yesterday</b>.
     * Otherwise, <i>from</i> and <i>to</i> should match the pattern YYYY-MM-DD.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ReportBase report = new VolumeReport();
        String outfile = "/tmp/" + System.currentTimeMillis() + ".csv";
        String from = null, to = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-?")) {
                System.err.println("usage: java org.onap.dmaap.datarouter.reports.Report [ -t <i>type</i> ] [ -o <i>outfile</i> ] [ <i>fromdate</i> [ <i>todate</i> ]]");
                System.exit(0);
            } else if (args[i].equals("-o")) {
                if (++i < args.length) {
                    outfile = args[i];
                }
            } else if (args[i].equals("-t")) {
                if (++i < args.length) {
                    String base = args[i];
                    base = Character.toUpperCase(base.charAt(0)) + base.substring(1);
                    base = "org.onap.dmaap.datarouter.reports." + base + "Report";
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ReportBase> cl = (Class<? extends ReportBase>) Class.forName(base);
                        Constructor<? extends ReportBase> con = cl.getConstructor();
                        report = con.newInstance();
                    } catch (Exception e) {
                        System.err.println("Unknown report type: " + args[i]);
                        System.exit(1);
                    }
                }
            } else if (from == null) {
                from = args[i];
            } else {
                to = args[i];
            }
        }
        long lfrom = 0, lto = 0;
        if (from == null) {
            // last 7 days
            TimeZone utc = TimeZone.getTimeZone("UTC");
            Calendar cal = new GregorianCalendar(utc);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            lfrom = cal.getTimeInMillis() - (7 * 24 * 60 * 60 * 1000L);    // 1 week
            lto = cal.getTimeInMillis() - 1;
        } else if (to == null) {
            try {
                String[] dates = getDates(from);
                lfrom = Long.parseLong(dates[0]);
                lto = Long.parseLong(dates[1]);
            } catch (Exception e) {
                System.err.println("Invalid date: " + from);
                System.exit(1);
            }
        } else {
            String[] dates;
            try {
                dates = getDates(from);
                lfrom = Long.parseLong(dates[0]);
            } catch (Exception e) {
                System.err.println("Invalid date: " + from);
                System.exit(1);
            }
            try {
                dates = getDates(to);
                lto = Long.parseLong(dates[0]);
            } catch (Exception e) {
                System.err.println("Invalid date: " + to);
                System.exit(1);
            }
        }

        report.setFrom(lfrom);
        report.setTo(lto);
        report.setOutputFile(outfile);
        report.run();
    }

    private static String[] getDates(String d) throws Exception {
        if (d.equals("ALL"))
            return new String[]{"1", "" + System.currentTimeMillis()};

        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = new GregorianCalendar(utc);
        if (d.matches("20\\d\\d-\\d\\d-\\d\\d")) {
            cal.set(Calendar.YEAR, Integer.parseInt(d.substring(0, 4)));
            cal.set(Calendar.MONTH, Integer.parseInt(d.substring(5, 7)) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d.substring(8, 10)));
        } else if (d.equals("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else
            throw new Exception("wa?");
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = start + (24 * 60 * 60 * 1000L) - 1;
        return new String[]{"" + start, "" + end};
    }
}
