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


package org.onap.dmaap.datarouter.provisioning.utils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;
import org.onap.dmaap.datarouter.provisioning.beans.DeliveryExtraRecord;
import org.onap.dmaap.datarouter.provisioning.beans.DeliveryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.ExpiryRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Loadable;
import org.onap.dmaap.datarouter.provisioning.beans.LogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.onap.dmaap.datarouter.provisioning.beans.PubFailRecord;
import org.onap.dmaap.datarouter.provisioning.beans.PublishRecord;

/**
 * This class provides methods that run in a separate thread, in order to process logfiles uploaded into the spooldir.
 * These logfiles are loaded into the MariaDB LOG_RECORDS table. In a running provisioning server, there should only be
 * two places where records can be loaded into this table; here, and in the method DB.retroFit4() which may be run at
 * startup to load the old (1.0) style log tables into LOG_RECORDS;
 * <p>This method maintains an {@link RLEBitSet} which can be used to easily see what records are presently in the
 * database.
 * This bit set is used to synchronize between provisioning servers.</p>
 *
 * @author Robert Eby
 * @version $Id: LogfileLoader.java,v 1.22 2014/03/12 19:45:41 eby Exp $
 */
public class LogfileLoader extends Thread {
    /**
     * NOT USED: Percentage of free space required before old records are removed.
     */
    public static final int REQUIRED_FREE_PCT = 20;

    /**
     * This is a singleton -- there is only one LogfileLoader object in the server.
     */
    private static LogfileLoader logfileLoader;

    /**
     * Each server can assign this many IDs.
     */
    private static final long SET_SIZE = (1L << 56);

    private final EELFLogger logger;
    private final String spooldir;
    private final long setStart;
    private final long setEnd;
    private RLEBitSet seqSet;
    private long nextId;
    private boolean idle;

    private LogfileLoader() {
        this.logger = EELFManager.getInstance().getLogger("InternalLog");
        this.spooldir = ProvRunner.getProvProperties().getProperty("org.onap.dmaap.datarouter.provserver.spooldir");
        this.setStart = getIdRange();
        this.setEnd = setStart + SET_SIZE - 1;
        this.seqSet = new RLEBitSet();
        this.nextId = 0;
        this.idle = false;
        this.setDaemon(true);
        this.setName("LogfileLoader");
    }

    /**
     * Get the singleton LogfileLoader object, and start it if it is not running.
     *
     * @return the LogfileLoader
     */
    public static synchronized LogfileLoader getLoader() {
        if (logfileLoader == null) {
            logfileLoader = new LogfileLoader();
        }
        if (!logfileLoader.isAlive()) {
            logfileLoader.start();
        }
        return logfileLoader;
    }

    private long getIdRange() {
        long size;
        if (BaseServlet.isInitialActivePOD()) {
            size = 0;
        } else if (BaseServlet.isInitialStandbyPOD()) {
            size = SET_SIZE;
        } else {
            size = SET_SIZE * 2;
        }
        String range = String.format("[%X .. %X]", size, size + SET_SIZE - 1);
        logger.debug("This server shall assign RECORD_IDs in the range " + range);
        return size;
    }

    /**
     * Return the bit set representing the record ID's that are loaded in this database.
     *
     * @return the bit set
     */
    public RLEBitSet getBitSet() {
        return seqSet;
    }

    /**
     * True if the LogfileLoader is currently waiting for work.
     *
     * @return true if idle
     */
    public boolean isIdle() {
        return idle;
    }

    /**
     * Run continuously to look for new logfiles in the spool directory and import them into the DB.
     * The spool is checked once per second.  If free space on the MariaDB filesystem falls below
     * REQUIRED_FREE_PCT (normally 20%) then the oldest logfile entries are removed and the LOG_RECORDS
     * table is compacted until free space rises above the threshold.
     */
    @Override
    public void run() {
        initializeNextid();
        while (true) {
            try {
                File dirfile = new File(spooldir);
                while (true) {
                    runLogFileLoad(dirfile);
                }
            } catch (Exception e) {
                logger.warn("PROV0020: Caught exception in LogfileLoader: " + e);
            }
        }
    }

    private void runLogFileLoad(File filesDir) {
        File[] inFiles = filesDir.listFiles((dir, name) -> name.startsWith("IN."));
        if (inFiles != null) {
            if (inFiles.length == 0) {
                idle = true;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                idle = false;
            } else {
                // Remove old rows
                if (pruneRecords()) {
                    // Removed at least some entries, recompute the bit map
                    initializeNextid();
                }
                for (File file : inFiles) {
                    processFile(file);
                }
            }
        }
    }

    private void processFile(File infile) {
        if (logger.isDebugEnabled()) {
            logger.debug("PROV8001 Starting " + infile + " ...");
        }
        long time = System.currentTimeMillis();
        int[] array = process(infile);
        time = System.currentTimeMillis() - time;
        logger.info(String.format("PROV8000 Processed %s in %d ms; %d of %d records.",
            infile.toString(), time, array[0], array[1]));
        try {
            Files.delete(infile.toPath());
        } catch (IOException e) {
            logger.info("PROV8001 failed to delete file " + infile.getName(), e);
        }
    }

    boolean pruneRecords() {
        boolean did1 = false;
        long count = countRecords();
        Parameters defaultLogRetention = Parameters.getParameter(Parameters.DEFAULT_LOG_RETENTION);
        long threshold = (defaultLogRetention != null) ? Long.parseLong(defaultLogRetention.getValue()) : 1000000L;
        Parameters provLogRetention = Parameters.getParameter(Parameters.PROV_LOG_RETENTION);
        if (provLogRetention != null) {
            try {
                long retention = Long.parseLong(provLogRetention.getValue());
                // This check is to prevent inadvertent errors from wiping the table out
                if (retention > 1000000L) {
                    threshold = retention;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        logger.debug("Pruning LOG_RECORD table: records in DB=" + count + ", threshold=" + threshold);
        if (count > threshold) {
            // we need to remove this many records
            count -= threshold;
            // histogram of records per day
            Map<Long, Long> hist = getHistogram();
            // Determine the cutoff point to remove the needed number of records
            long sum = 0;
            long cutoff = 0;
            for (Long day : new TreeSet<>(hist.keySet())) {
                sum += hist.get(day);
                cutoff = day;
                if (sum >= count) {
                    break;
                }
            }
            cutoff++;
            // convert day to ms
            cutoff *= 86400000L;
            logger.debug("  Pruning records older than=" + (cutoff / 86400000L) + " (" + new Date(cutoff) + ")");

            try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
                // Limit to a million at a time to avoid typing up the DB for too long.
                try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE from LOG_RECORDS where EVENT_TIME < ? limit 1000000")) {
                    ps.setLong(1, cutoff);
                    while (count > 0) {
                        if (!ps.execute()) {
                            int dcount = ps.getUpdateCount();
                            count -= dcount;
                            logger.debug("  " + dcount + " rows deleted.");
                            did1 |= (dcount != 0);
                            if (dcount == 0) {
                                count = 0;    // prevent inf. loops
                            }
                        } else {
                            count = 0;    // shouldn't happen!
                        }
                    }
                }
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("OPTIMIZE TABLE LOG_RECORDS");
                }
            } catch (SQLException e) {
                logger.error("LogfileLoader.pruneRecords: " + e.getMessage(), e);
            }
        }
        return did1;
    }

    private long countRecords() {
        long count = 0;
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) as COUNT from LOG_RECORDS");
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                count = rs.getLong("COUNT");
            }
        } catch (SQLException e) {
            logger.error("LogfileLoader.countRecords: " + e.getMessage(), e);
        }
        return count;
    }

    private Map<Long, Long> getHistogram() {
        Map<Long, Long> map = new HashMap<>();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT FLOOR(EVENT_TIME/86400000) AS DAY, COUNT(*) AS COUNT FROM LOG_RECORDS GROUP BY DAY");
            ResultSet rs = ps.executeQuery()) {
            logger.debug("  LOG_RECORD table histogram...");
            while (rs.next()) {
                long day = rs.getLong("DAY");
                long cnt = rs.getLong("COUNT");
                map.put(day, cnt);
                logger.debug("  " + day + "  " + cnt);
            }
        } catch (SQLException e) {
            logger.error("LogfileLoader.getHistogram: " + e.getMessage(), e);
        }
        return map;
    }

    private void initializeNextid() {
        try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
            RLEBitSet nbs = new RLEBitSet();
            try (Statement stmt = conn.createStatement()) {
                // Build a bitset of all records in the LOG_RECORDS table
                // We need to run this SELECT in stages, because otherwise we run out of memory!
                final long stepsize = 6000000L;
                boolean goAgain = true;
                for (long i = 0; goAgain; i += stepsize) {
                    String sql = String.format("select RECORD_ID from LOG_RECORDS LIMIT %d,%d", i, stepsize);
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        goAgain = false;
                        while (rs.next()) {
                            long recordId = rs.getLong("RECORD_ID");
                            nbs.set(recordId);
                            goAgain = true;
                        }
                    }
                }
            }
            seqSet = nbs;
            // Compare with the range for this server
            // Determine the next ID for this set of record IDs
            RLEBitSet tbs = (RLEBitSet) nbs.clone();
            RLEBitSet idset = new RLEBitSet();
            idset.set(setStart, setStart + SET_SIZE);
            tbs.and(idset);
            long bitLength = tbs.length();
            nextId = (bitLength == 0) ? setStart : (bitLength - 1);
            if (nextId >= setStart + SET_SIZE) {
                // Handle wraparound, when the IDs reach the end of our "range"
                Long[] last = null;
                Iterator<Long[]> li = tbs.getRangeIterator();
                while (li.hasNext()) {
                    last = li.next();
                }
                if (last != null) {
                    tbs.clear(last[0], last[1] + 1);
                    bitLength = tbs.length();
                    nextId = (bitLength == 0) ? setStart : (bitLength - 1);
                }
            }
            logger.debug(String.format("LogfileLoader.initializeNextid, next ID is %d (%x)", nextId, nextId));
        } catch (SQLException e) {
            logger.error("LogfileLoader.initializeNextid: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("resource")
    int[] process(File file) {
        int ok = 0;
        int total = 0;
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "insert into LOG_RECORDS values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            Reader reader = file.getPath().endsWith(".gz")
                ? new InputStreamReader(new GZIPInputStream(new FileInputStream(file)))
                : new FileReader(file);
            try (LineNumberReader in = new LineNumberReader(reader)) {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        for (Loadable rec : buildRecords(line)) {
                            rec.load(ps);
                            if (rec instanceof LogRecord) {
                                LogRecord lr = ((LogRecord) rec);
                                if (!seqSet.get(lr.getRecordId())) {
                                    ps.executeUpdate();
                                    seqSet.set(lr.getRecordId());
                                } else {
                                    logger.debug("Duplicate record ignored: " + lr.getRecordId());
                                }
                            } else {
                                if (++nextId > setEnd) {
                                    nextId = setStart;
                                }
                                ps.setLong(18, nextId);
                                ps.executeUpdate();
                                seqSet.set(nextId);
                            }
                            ps.clearParameters();
                            ok++;
                        }
                    } catch (SQLException e) {
                        logger.warn("PROV8003 Invalid value in record: " + line, e);
                    } catch (NumberFormatException e) {
                        logger.warn("PROV8004 Invalid number in record: " + line, e);
                    } catch (ParseException e) {
                        logger.warn("PROV8005 Invalid date in record: " + line, e);
                    } catch (Exception e) {
                        logger.warn("PROV8006 Invalid pattern in record: " + line, e);
                    }
                    total++;
                }
            }
        } catch (SQLException | IOException e) {
            logger.warn("PROV8007 Exception reading " + file + ": " + e);
        }
        return new int[]{ok, total};
    }

    Loadable[] buildRecords(String line) throws ParseException {
        String[] pp = line.split("\\|");
        if (pp != null && pp.length >= 7) {
            String rtype = pp[1].toUpperCase();
            if ("PUB".equals(rtype) && pp.length == 11) {
                // Fields are: date|PUB|pubid|feedid|requrl|method|ctype|clen|srcip|user|status
                return new Loadable[]{new PublishRecord(pp)};
            }
            if ("DEL".equals(rtype) && pp.length == 12) {
                // Fields are: date|DEL|pubid|feedid|subid|requrl|method|ctype|clen|user|status|xpubid
                String[] subs = pp[4].split("\\s+");
                if (subs != null) {
                    Loadable[] rv = new Loadable[subs.length];
                    for (int i = 0; i < subs.length; i++) {
                        // create a new record for each individual sub
                        pp[4] = subs[i];
                        rv[i] = new DeliveryRecord(pp);
                    }
                    return rv;
                }
            }
            if ("EXP".equals(rtype) && pp.length == 11) {
                // Fields are: date|EXP|pubid|feedid|subid|requrl|method|ctype|clen|reason|attempts
                ExpiryRecord expiryRecord = new ExpiryRecord(pp);
                if ("other".equals(expiryRecord.getReason())) {
                    logger.info("Invalid reason '" + pp[9] + "' changed to 'other' for record: "
                        + expiryRecord.getPublishId());
                }
                return new Loadable[]{expiryRecord};
            }
            if ("PBF".equals(rtype) && pp.length == 12) {
                // Fields are: date|PBF|pubid|feedid|requrl|method|ctype|clen-expected|clen-received|srcip|user|error
                return new Loadable[]{new PubFailRecord(pp)};
            }
            if ("DLX".equals(rtype) && pp.length == 7) {
                // Fields are: date|DLX|pubid|feedid|subid|clen-tosend|clen-sent
                return new Loadable[]{new DeliveryExtraRecord(pp)};
            }
            if ("LOG".equals(rtype) && (pp.length == 19 || pp.length == 20)) {
                // Fields are: date|LOG|pubid|feedid|requrl|method|ctype|clen|type|
                // feedFileid|remoteAddr|user|status|subid|fileid|result|attempts|reason|record_id
                return new Loadable[]{new LogRecord(pp)};
            }
        }
        logger.warn("PROV8002 bad record: " + line);
        return new Loadable[0];
    }

    /**
     * The LogfileLoader can be run stand-alone by invoking the main() method of this class.
     *
     * @param str ignored
     */
    public static void main(String[] str) throws InterruptedException {
        LogfileLoader.getLoader();
        Thread.sleep(200000L);
    }
}
