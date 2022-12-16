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

package org.onap.dmaap.datarouter.node.delivery;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import org.onap.dmaap.datarouter.node.DestInfo;
import org.onap.dmaap.datarouter.node.NodeConfigManager;

/**
 * Main control point for delivering files to destinations.
 *
 * <p>The Delivery class manages assignment of delivery threads to delivery queues and creation and destruction of
 * delivery queues as configuration changes. DeliveryQueues are assigned threads based on a modified round-robin
 * approach giving priority to queues with more work as measured by both bytes to deliver and files to deliver and lower
 * priority to queues that already have delivery threads working. A delivery thread continues to work for a delivery
 * queue as long as that queue has more files to deliver.
 */
public class Delivery {

    private static final String TOTAL = " total=";
    private static final String YELLOW = " yellow=";
    private static EELFLogger logger = EELFManager.getInstance().getLogger(Delivery.class);
    private double fdstart;
    private double fdstop;
    private int threads;
    private int curthreads;
    private NodeConfigManager config;
    private HashMap<String, DeliveryQueue> dqs = new HashMap<>();
    private DeliveryQueue[] queues = new DeliveryQueue[0];
    private int qpos = 0;
    private long nextcheck;

    /**
     * Constructs a new Delivery system using the specified configuration manager.
     *
     * @param config The configuration manager for this delivery system.
     */
    public Delivery(NodeConfigManager config) {
        this.config = config;
        Runnable cmon = this::checkconfig;
        config.registerConfigTask(cmon);
    }

    /**
     * Reset the retry timer for a delivery queue.
     */
    public synchronized void resetQueue(String spool) {
        if (spool != null) {
            DeliveryQueue dq = dqs.get(spool);
            if (dq != null) {
                dq.resetQueue();
            }
        }
    }

    /**
     * Mark the task in spool a success.
     */
    public synchronized boolean markTaskSuccess(String spool, String pubId) {
        boolean succeeded = false;
        if (spool != null) {
            DeliveryQueue dq = dqs.get(spool);
            if (dq != null) {
                succeeded = dq.markTaskSuccess(pubId);
            }
        }
        return succeeded;
    }

    private void cleardir(String dir) {
        if (dqs.get(dir) != null) {
            return;
        }
        File fdir = new File(dir);
        try {
            for (File junk : fdir.listFiles()) {
                if (junk.isFile()) {
                    Files.delete(fdir.toPath());
                }
            }
            Files.delete(fdir.toPath());
        } catch (IOException e) {
            logger.error("Failed to delete file: " + fdir.getPath(), e);
        }
    }

    private void freeDiskCheck() {
        File spoolfile = new File(config.getSpoolBase());
        long tspace = spoolfile.getTotalSpace();
        long start = (long) (tspace * fdstart);
        long cur = spoolfile.getUsableSpace();
        if (cur >= start) {
            return;
        }
        ArrayList<DelItem> cv = new ArrayList<>();
        for (String sdir : dqs.keySet()) {
            for (String meta : (new File(sdir)).list()) {
                if (!meta.endsWith(".M") || meta.charAt(0) == '.') {
                    continue;
                }
                cv.add(new DelItem(meta.substring(0, meta.length() - 2), sdir));
            }
        }
        DelItem[] items = cv.toArray(new DelItem[cv.size()]);
        Arrays.sort(items);
        long stop = (long) (tspace * fdstop);
        logger.warn(
            "NODE0501 Free disk space below red threshold.  current=" + cur + " red=" + start + TOTAL + tspace);
        if (determineFreeDiskSpace(spoolfile, tspace, stop, cur, items)) {
            return;
        }
        cur = spoolfile.getUsableSpace();
        if (cur >= stop) {
            logger.warn("NODE0503 Free disk space at or above yellow threshold.  current=" + cur + YELLOW + stop
                + TOTAL + tspace);
            return;
        }
        logger.warn(
            "NODE0504 Unable to recover sufficient disk space to reach green status.  current=" + cur + YELLOW
                + stop + TOTAL + tspace);
    }

    private void cleardirs() {
        String basedir = config.getSpoolBase();
        String nbase = basedir + "/n";
        for (String nodedir : (new File(nbase)).list()) {
            if (!nodedir.startsWith(".")) {
                cleardir(nbase + "/" + nodedir);
            }
        }
        String sxbase = basedir + "/s";
        for (String sxdir : (new File(sxbase)).list()) {
            if (sxdir.startsWith(".")) {
                continue;
            }
            File sxf = new File(sxbase + File.separator + sxdir);
            for (String sdir : sxf.list()) {
                if (!sdir.startsWith(".")) {
                    cleardir(sxbase + "/" + sxdir + "/" + sdir);
                }
            }
            try {
                if (sxf.list().length == 0) {
                    Files.delete(sxf.toPath());  // won't if anything still in it
                }
            } catch (IOException e) {
                logger.error("Failed to delete file: " + sxf.getPath(), e);
            }
        }
    }

    private synchronized void checkconfig() {
        if (!config.isConfigured()) {
            return;
        }
        fdstart = config.getFreeDiskStart();
        fdstop = config.getFreeDiskStop();
        threads = config.getDeliveryThreads();
        if (threads < 1) {
            threads = 1;
        }
        DestInfo[] alldis = config.getAllDests();
        DeliveryQueue[] nqs = new DeliveryQueue[alldis.length];
        qpos = 0;
        HashMap<String, DeliveryQueue> ndqs = new HashMap<>();
        for (DestInfo di : alldis) {
            String spl = di.getSpool();
            DeliveryQueue dq = dqs.get(spl);
            if (dq == null) {
                dq = new DeliveryQueue(config, di);
            } else {
                dq.config(di);
            }
            ndqs.put(spl, dq);
            nqs[qpos++] = dq;
        }
        queues = nqs;
        dqs = ndqs;
        cleardirs();
        while (curthreads < threads) {
            curthreads++;
            (new Thread("del-thread-" + curthreads) {
                @Override
                public void run() {
                    dodelivery();
                }
            }).start();
        }
        nextcheck = 0;
        notifyAll();
    }

    private void dodelivery() {
        DeliveryQueue dq;
        while ((dq = getNextQueue()) != null) {
            dq.run();
        }
    }

    private synchronized DeliveryQueue getNextQueue() {
        while (true) {
            if (curthreads > threads) {
                curthreads--;
                return (null);
            }
            if (qpos < queues.length) {
                DeliveryQueue dq = queues[qpos++];
                if (dq.isSkipSet()) {
                    continue;
                }
                nextcheck = 0;
                notifyAll();
                return (dq);
            }
            long now = System.currentTimeMillis();
            if (now < nextcheck) {
                try {
                    wait(nextcheck + 500 - now);
                } catch (Exception e) {
                    logger.error("InterruptedException", e);
                }
                now = System.currentTimeMillis();
            }
            if (now >= nextcheck) {
                nextcheck = now + 5000;
                qpos = 0;
                freeDiskCheck();
            }
        }
    }

    private boolean determineFreeDiskSpace(File spoolfile, long tspace, long stop, long cur, DelItem[] items) {
        for (DelItem item : items) {
            long amount = dqs.get(item.getSpool()).cancelTask(item.getPublishId());
            logger.debug("NODE0502 Attempting to discard " + item.getSpool() + "/" + item.getPublishId()
                + " to free up disk");
            if (amount > 0) {
                cur += amount;
                if (cur >= stop) {
                    cur = spoolfile.getUsableSpace();
                }
                if (cur >= stop) {
                    logger.warn(
                        "NODE0503 Free disk space at or above yellow threshold.  current=" + cur + YELLOW + stop
                            + TOTAL + tspace);
                    return true;
                }
            }
        }
        return false;
    }

    public static class DelItem implements Comparable<DelItem> {

        private String pubid;
        private String spool;

        public DelItem(String pubid, String spool) {
            this.pubid = pubid;
            this.spool = spool;
        }

        public int compareTo(DelItem other) {
            int diff = pubid.compareTo(other.pubid);
            if (diff == 0) {
                diff = spool.compareTo(other.spool);
            }
            return (diff);
        }

        public String getPublishId() {
            return (pubid);
        }

        public String getSpool() {
            return (spool);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            DelItem delItem = (DelItem) object;
            return Objects.equals(pubid, delItem.pubid)
                && Objects.equals(getSpool(), delItem.getSpool());
        }

        @Override
        public int hashCode() {
            return Objects.hash(pubid, getSpool());
        }
    }
}
