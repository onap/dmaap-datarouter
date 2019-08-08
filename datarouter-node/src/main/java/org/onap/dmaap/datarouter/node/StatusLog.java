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

package org.onap.dmaap.datarouter.node;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logging for data router delivery events (PUB/DEL/EXP).
 */
public class StatusLog {

    private static final String EXCEPTION = "Exception";
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(StatusLog.class);
    private static StatusLog instance = new StatusLog();
    private SimpleDateFormat filedate = new SimpleDateFormat("-yyyyMMddHHmm");


    private String prefix = "logs/events";
    private String suffix = ".log";
    private String plainfile;
    private String curfile;
    private long nexttime;
    private OutputStream os;
    private long intvl;
    private static NodeConfigManager config = NodeConfigManager.getInstance();

    private StatusLog() {
    }

    /**
     * Parse an interval of the form xxhyymzzs and round it to the nearest whole fraction of 24 hours.If no units are
     * specified, assume seconds.
     */
    public static long parseInterval(String interval, int def) {
        try {
            Matcher matcher = Pattern.compile("(?:(\\d+)[Hh])?(?:(\\d+)[Mm])?(?:(\\d+)[Ss]?)?").matcher(interval);
            if (matcher.matches()) {
                int dur = getDur(matcher);
                int best = 86400;
                int dist = best - dur;
                if (dur > best) {
                    dist = dur - best;
                }
                best = getBest(dur, best, dist);
                def = best * 1000;
            }
        } catch (Exception e) {
            eelfLogger.error(EXCEPTION, e);
        }
        return (def);
    }

    private static int getBest(int dur, int best, int dist) {
        int base = 1;
        for (int i = 0; i < 8; i++) {
            int base2 = base;
            base *= 2;
            for (int j = 0; j < 4; j++) {
                int base3 = base2;
                base2 *= 3;
                for (int k = 0; k < 3; k++) {
                    int cur = base3;
                    base3 *= 5;
                    int ndist = cur - dur;
                    if (dur > cur) {
                        ndist = dur - cur;
                    }
                    if (ndist < dist) {
                        best = cur;
                        dist = ndist;
                    }
                }
            }
        }
        return best;
    }

    private static int getDur(Matcher matcher) {
        int dur = 0;
        String match = matcher.group(1);
        if (match != null) {
            dur += 3600 * Integer.parseInt(match);
        }
        match = matcher.group(2);
        if (match != null) {
            dur += 60 * Integer.parseInt(match);
        }
        match = matcher.group(3);
        if (match != null) {
            dur += Integer.parseInt(match);
        }
        if (dur < 60) {
            dur = 60;
        }
        return dur;
    }

    /**
     * Get the name of the current log file.
     *
     * @return The full path name of the current event log file
     */
    public static synchronized String getCurLogFile() {
        try {
            instance.checkRoll(System.currentTimeMillis());
        } catch (Exception e) {
            eelfLogger.error(EXCEPTION, e);
        }
        return (instance.curfile);
    }

    /**
     * Log a received publication attempt.
     *
     * @param pubid The publish ID assigned by the node
     * @param feedid The feed id given by the publisher
     * @param requrl The URL of the received request
     * @param method The method (DELETE or PUT) in the received request
     * @param ctype The content type (if method is PUT and clen > 0)
     * @param clen The content length (if method is PUT)
     * @param srcip The IP address of the publisher
     * @param user The identity of the publisher
     * @param status The status returned to the publisher
     */
    public static void logPub(String pubid, String feedid, String requrl, String method, String ctype, long clen,
            String srcip, String user, int status) {
        instance.log(
                "PUB|" + pubid + "|" + feedid + "|" + requrl + "|" + method + "|" + ctype + "|" + clen + "|" + srcip
                        + "|" + user + "|" + status);
        eelfLogger.info("PUB|" + pubid + "|" + feedid + "|" + requrl + "|" + method + "|" + ctype + "|"
                                + clen + "|" + srcip + "|" + user + "|" + status);
    }

    /**
     * Log a data transfer error receiving a publication attempt.
     *
     * @param pubid The publish ID assigned by the node
     * @param feedid The feed id given by the publisher
     * @param requrl The URL of the received request
     * @param method The method (DELETE or PUT) in the received request
     * @param ctype The content type (if method is PUT and clen > 0)
     * @param clen The expected content length (if method is PUT)
     * @param rcvd The content length received
     * @param srcip The IP address of the publisher
     * @param user The identity of the publisher
     * @param error The error message from the IO exception
     */
    public static void logPubFail(String pubid, String feedid, String requrl, String method, String ctype, long clen,
            long rcvd, String srcip, String user, String error) {
        instance.log("PBF|" + pubid + "|" + feedid + "|" + requrl + "|" + method + "|" + ctype + "|" + clen + "|" + rcvd
                + "|" + srcip + "|" + user + "|" + error);
        eelfLogger.info("PBF|" + pubid + "|" + feedid + "|" + requrl + "|" + method + "|" + ctype + "|" + clen
                                + "|" + rcvd + "|" + srcip + "|" + user + "|" + error);
    }

    /**
     * Log a delivery attempt.
     *
     * @param pubid The publish ID assigned by the node
     * @param feedid The feed ID
     * @param subid The (space delimited list of) subscription ID
     * @param requrl The URL used in the attempt
     * @param method The method (DELETE or PUT) in the attempt
     * @param ctype The content type (if method is PUT, not metaonly, and clen > 0)
     * @param clen The content length (if PUT and not metaonly)
     * @param user The identity given to the subscriber
     * @param status The status returned by the subscriber or -1 if an exeception occured trying to connect
     * @param xpubid The publish ID returned by the subscriber
     */
    public static void logDel(String pubid, String feedid, String subid, String requrl, String method, String ctype,
            long clen, String user, int status, String xpubid) {
        if (feedid == null) {
            return;
        }
        instance.log(
                "DEL|" + pubid + "|" + feedid + "|" + subid + "|" + requrl + "|" + method + "|" + ctype + "|" + clen
                        + "|" + user + "|" + status + "|" + xpubid);
        eelfLogger.info("DEL|" + pubid + "|" + feedid + "|" + subid + "|" + requrl + "|" + method + "|"
                                + ctype + "|" + clen + "|" + user + "|" + status + "|" + xpubid);
    }

    /**
     * Log delivery attempts expired.
     *
     * @param pubid The publish ID assigned by the node
     * @param feedid The feed ID
     * @param subid The (space delimited list of) subscription ID
     * @param requrl The URL that would be delivered to
     * @param method The method (DELETE or PUT) in the request
     * @param ctype The content type (if method is PUT, not metaonly, and clen > 0)
     * @param clen The content length (if PUT and not metaonly)
     * @param reason The reason the attempts were discontinued
     * @param attempts The number of attempts made
     */
    public static void logExp(String pubid, String feedid, String subid, String requrl, String method, String ctype,
            long clen, String reason, int attempts) {
        if (feedid == null) {
            return;
        }
        instance.log(
                "EXP|" + pubid + "|" + feedid + "|" + subid + "|" + requrl + "|" + method + "|" + ctype + "|" + clen
                        + "|" + reason + "|" + attempts);
        eelfLogger.info("EXP|" + pubid + "|" + feedid + "|" + subid + "|" + requrl + "|" + method + "|"
                                + ctype + "|" + clen + "|" + reason + "|" + attempts);
    }

    /**
     * Log extra statistics about unsuccessful delivery attempts.
     *
     * @param pubid The publish ID assigned by the node
     * @param feedid The feed ID
     * @param subid The (space delimited list of) subscription ID
     * @param clen The content length
     * @param sent The # of bytes sent or -1 if subscriber returned an error instead of 100 Continue, otherwise, the
     *      number of bytes sent before an error occurred.
     */
    public static void logDelExtra(String pubid, String feedid, String subid, long clen, long sent) {
        if (feedid == null) {
            return;
        }
        instance.log("DLX|" + pubid + "|" + feedid + "|" + subid + "|" + clen + "|" + sent);
        eelfLogger.info("DLX|" + pubid + "|" + feedid + "|" + subid + "|" + clen + "|" + sent);
    }

    private synchronized void checkRoll(long now) throws IOException {
        if (now >= nexttime) {
            if (os != null) {
                os.close();
                os = null;
            }
            intvl = parseInterval(config.getEventLogInterval(), 300000);
            prefix = config.getEventLogPrefix();
            suffix = config.getEventLogSuffix();
            nexttime = now - now % intvl + intvl;
            curfile = prefix + filedate.format(new Date(nexttime - intvl)) + suffix;
            plainfile = prefix + suffix;
            notifyAll();
        }
    }

    private synchronized void log(String string) {
        try {
            long now = System.currentTimeMillis();
            checkRoll(now);
            if (os == null) {
                os = new FileOutputStream(curfile, true);
                Files.deleteIfExists(new File(plainfile).toPath());
                Files.createLink(Paths.get(plainfile), Paths.get(curfile));
            }
            os.write((NodeUtils.logts(new Date(now)) + '|' + string + '\n').getBytes());
            os.flush();
        } catch (IOException ioe) {
            eelfLogger.error("IOException", ioe);
        }
    }
}
