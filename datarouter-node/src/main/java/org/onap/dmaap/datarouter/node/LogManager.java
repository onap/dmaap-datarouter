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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Cleanup of old log files.
 *
 * <p>Periodically scan the log directory for log files that are older than the log file retention interval, and delete
 * them.  In a future release, This class will also be responsible for uploading events logs to the log server to
 * support the log query APIs.
 */

public class LogManager extends TimerTask {

    private static final String EXCEPTION = "Exception";
    private EELFLogger logger = EELFManager.getInstance().getLogger(LogManager.class);
    private NodeConfigManager config;
    private Matcher isnodelog;
    private Matcher iseventlog;
    private Uploader worker;
    private String uploaddir;
    private String logdir;

    /**
     * Construct a log manager
     *
     * <p>The log manager will check for expired log files every 5 minutes at 20 seconds after the 5 minute boundary.
     * (Actually, the interval is the event log rollover interval, which defaults to 5 minutes).
     */
    public LogManager(NodeConfigManager config) {
        this.config = config;
        try {
            isnodelog = Pattern.compile("node\\.log\\.\\d{8}").matcher("");
            iseventlog = Pattern.compile("events-\\d{12}\\.log").matcher("");
        } catch (Exception e) {
            logger.error(EXCEPTION, e);
        }
        logdir = config.getLogDir();
        uploaddir = logdir + "/.spool";
        (new File(uploaddir)).mkdirs();
        long now = System.currentTimeMillis();
        long intvl = StatusLog.parseInterval(config.getEventLogInterval(), 30000);
        long when = now - now % intvl + intvl + 20000L;
        config.getTimer().scheduleAtFixedRate(this, when - now, intvl);
        worker = new Uploader();
    }

    /**
     * Trigger check for expired log files and log files to upload.
     */
    public void run() {
        worker.poke();
    }

    private class Uploader extends Thread implements DeliveryQueueHelper {

        private static final String META = "/.meta";
        private EELFLogger logger = EELFManager.getInstance().getLogger(Uploader.class);
        private DeliveryQueue dq;

        Uploader() {
            dq = new DeliveryQueue(this,
                    new DestInfoBuilder().setName("LogUpload").setSpool(uploaddir).setSubid(null).setLogdata(null)
                            .setUrl(null).setAuthuser(config.getMyName()).setAuthentication(config.getMyAuth())
                            .setMetaonly(false).setUse100(false).setPrivilegedSubscriber(false)
                            .setFollowRedirects(false)
                            .setDecompress(false).createDestInfo());
            setDaemon(true);
            setName("Log Uploader");
            start();
        }

        public long getInitFailureTimer() {
            return (10000L);
        }

        public long getWaitForFileProcessFailureTimer() {
            return (600000L);
        }

        public double getFailureBackoff() {
            return (2.0);
        }

        public long getMaxFailureTimer() {
            return (150000L);
        }

        public long getExpirationTimer() {
            return (604800000L);
        }

        public int getFairFileLimit() {
            return (10000);
        }

        public long getFairTimeLimit() {
            return (86400000);
        }

        public String getDestURL(DestInfo destinationInfo, String fileid) {
            return (config.getEventLogUrl());
        }

        public void handleUnreachable(DestInfo destinationInfo) {
            throw new UnsupportedOperationException();
        }

        public boolean handleRedirection(DestInfo destinationInfo, String location, String fileid) {
            return (false);
        }

        public boolean isFollowRedirects() {
            return (false);
        }

        public String getFeedId(String subid) {
            return (null);
        }

        private synchronized void snooze() {
            try {
                wait(10000);
            } catch (Exception e) {
                logger.error(EXCEPTION, e);
            }
        }

        private synchronized void poke() {
            notify();
        }

        @Override
        public void run() {
            while (true) {
                scan();
                dq.run();
                snooze();
            }
        }

        private void scan() {
            long threshold = System.currentTimeMillis() - config.getLogRetention();
            File dir = new File(logdir);
            String[] fns = dir.list();
            Arrays.sort(fns);
            String lastqueued = "events-000000000000.log";
            String curlog = StatusLog.getCurLogFile();
            curlog = curlog.substring(curlog.lastIndexOf('/') + 1);
            try {
                Writer writer = new FileWriter(uploaddir + META);
                writer.write("POST\tlogdata\nContent-Type\ttext/plain\n");
                writer.close();
                BufferedReader br = new BufferedReader(new FileReader(uploaddir + "/.lastqueued"));
                lastqueued = br.readLine();
                br.close();
            } catch (Exception e) {
                logger.error(EXCEPTION, e);
            }
            for (String fn : fns) {
                if (!isnodelog.reset(fn).matches()) {
                    if (!iseventlog.reset(fn).matches()) {
                        continue;
                    }
                    lastqueued = setLastQueued(lastqueued, curlog, fn);
                }
                File file = new File(dir, fn);
                if (file.lastModified() < threshold) {
                    file.delete();
                }
            }
            try (Writer w = new FileWriter(uploaddir + "/.lastqueued")) {
                (new File(uploaddir + META)).delete();
                w.write(lastqueued + "\n");
            } catch (Exception e) {
                logger.error(EXCEPTION, e);
            }
        }

        @NotNull
        private String setLastQueued(String lastqueued, String curlog, String fn) {
            if (lastqueued.compareTo(fn) < 0 && curlog.compareTo(fn) > 0) {
                lastqueued = fn;
                try {
                    String pid = config.getPublishId();
                    Files.createLink(Paths.get(uploaddir + "/" + pid), Paths.get(logdir + "/" + fn));
                    Files.createLink(Paths.get(uploaddir + "/" + pid + ".M"), Paths.get(uploaddir + META));
                } catch (Exception e) {
                    logger.error(EXCEPTION, e);
                }
            }
            return lastqueued;
        }
    }
}
