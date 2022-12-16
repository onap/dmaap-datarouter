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

import static java.lang.System.exit;
import static java.lang.System.getProperty;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.Timer;
import org.onap.dmaap.datarouter.node.config.NodeConfig;
import org.onap.dmaap.datarouter.node.config.ProvData;
import org.onap.dmaap.datarouter.node.delivery.DeliveryQueueHelper;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.onap.dmaap.datarouter.node.utils.NodeTlsManager;
import org.onap.dmaap.datarouter.node.utils.NodeUtils;


/**
 * Maintain the configuration of a Data Router node
 *
 * <p>The NodeConfigManager is the single point of contact for servlet, delivery, event logging, and log retention
 * subsystems to access configuration information.
 *
 * <p>There are two basic sets of configuration data.  The static local configuration data, stored in a local
 * configuration file (created as part of installation by SWM), and the dynamic global configuration data fetched from
 * the data router provisioning server.
 */
public class NodeConfigManager implements DeliveryQueueHelper {

    private static final String NODE_CONFIG_MANAGER = "NodeConfigManager";
    private static final EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeConfigManager.class);
    private long maxfailuretimer;
    private long initfailuretimer;
    private long waitForFileProcessFailureTimer;
    private long expirationtimer;
    private double failurebackoff;
    private long fairtimelimit;
    private int fairfilelimit;
    private double fdpstart;
    private double fdpstop;
    private int deliverythreads;
    private final String provurl;
    private String provhost;
    private final int intHttpPort;
    private final int intHttpsPort;
    private final int extHttpsPort;
    private final boolean tlsEnabled;
    private String myname;
    private final String nak;
    private final File quiesce;
    private final String spooldir;
    private final String logdir;
    private final long logretention;
    private final String eventlogurl;
    private final String eventlogprefix;
    private final String eventlogsuffix;
    private String eventloginterval;
    private boolean followredirects;
    private final TaskList configtasks = new TaskList();
    private final PublishId publishId;
    private final IsFrom provcheck;
    private final RedirManager rdmgr;
    private final Timer timer = new Timer("Node Configuration Timer", true);
    private final RateLimitedOperation pfetcher;
    private static NodeConfigManager base;
    private static NodeTlsManager nodeTlsManager;
    private NodeConfig nodeConfig;
    private static Properties drNodeProperties;

    public static Properties getDrNodeProperties() {
        if (drNodeProperties == null) {
            try (FileInputStream props = new FileInputStream(getProperty(
                "org.onap.dmaap.datarouter.node.properties",
                "/opt/app/datartr/etc/node.properties"))) {
                drNodeProperties = new Properties();
                drNodeProperties.load(props);
            } catch (IOException e) {
                eelfLogger.error("Failed to load NODE properties: " + e.getMessage(), e);
                exit(1);
            }
        }
        return drNodeProperties;
    }
    /**
     * Initialize the configuration of a Data Router node.
     */
    private NodeConfigManager() {
        provurl = getDrNodeProperties().getProperty("ProvisioningURL", "http://dmaap-dr-prov:8080/internal/prov");
        try {
            provhost = (new URL(provurl)).getHost();
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf(NODE_CONFIG_MANAGER);
            eelfLogger.error(EelfMsgs.MESSAGE_BAD_PROV_URL, e, provurl);
            exit(1);
        }
        eelfLogger.debug("NODE0303 Provisioning server is at: " + provhost);
        provcheck = new IsFrom(provhost);
        tlsEnabled = Boolean.parseBoolean(getDrNodeProperties().getProperty("TlsEnabled", "true"));
        if (isTlsEnabled()) {
            try {
                nodeTlsManager = new NodeTlsManager(getDrNodeProperties());
                myname = nodeTlsManager.getMyNameFromCertificate();
                if (myname == null) {
                    NodeUtils.setIpAndFqdnForEelf(NODE_CONFIG_MANAGER);
                    eelfLogger.error(EelfMsgs.MESSAGE_KEYSTORE_FETCH_ERROR, nodeTlsManager.getKeyStorefile());
                    eelfLogger.error("NODE0309 Unable to fetch canonical name from keystore file {}", nodeTlsManager.getKeyStorefile());
                    exit(1);
                }
                eelfLogger.debug("NODE0304 My certificate says my name is {}", myname);
            } catch (Exception e) {
                eelfLogger.error("NODE0314 Failed to load AAF props. Exiting", e);
                exit(1);
            }
        }
        myname = "dmaap-dr-node";
        eventlogurl = getDrNodeProperties().getProperty("LogUploadURL", "https://feeds-drtr.web.att.com/internal/logs");
        intHttpPort = Integer.parseInt(getDrNodeProperties().getProperty("IntHttpPort", "80"));
        intHttpsPort = Integer.parseInt(getDrNodeProperties().getProperty("IntHttpsPort", "443"));
        extHttpsPort = Integer.parseInt(getDrNodeProperties().getProperty("ExtHttpsPort", "443"));
        spooldir = getDrNodeProperties().getProperty("SpoolDir", "spool");

        File fdir = new File(spooldir + "/f");
        fdir.mkdirs();
        for (File junk : Objects.requireNonNull(fdir.listFiles())) {
            try {
                Files.deleteIfExists(junk.toPath());
            } catch (IOException e) {
                eelfLogger.error("NODE0313 Failed to clear junk files from " + fdir.getPath(), e);
            }
        }
        logdir = getDrNodeProperties().getProperty("LogDir", "logs");
        (new File(logdir)).mkdirs();
        logretention = Long.parseLong(getDrNodeProperties().getProperty("LogRetention", "30")) * 86400000L;
        eventlogprefix = logdir + "/events";
        eventlogsuffix = ".log";
        String redirfile = getDrNodeProperties().getProperty("RedirectionFile", "etc/redirections.dat");
        publishId = new PublishId(myname);
        nak = getDrNodeProperties().getProperty("NodeAuthKey", "Node123!");
        quiesce = new File(getDrNodeProperties().getProperty("QuiesceFile", "etc/SHUTDOWN"));
        rdmgr = new RedirManager(redirfile,
            Long.parseLong(getDrNodeProperties().getProperty("MinRedirSaveInterval", "10000")), timer);
        pfetcher = new RateLimitedOperation(
            Long.parseLong(getDrNodeProperties().getProperty("MinProvFetchInterval", "10000")), timer) {
            public void run() {
                fetchNodeConfigFromProv();
            }
        };
        eelfLogger.debug("NODE0305 Attempting to fetch configuration at " + provurl);
        pfetcher.request();
    }

    /**
     * Get the default node configuration manager.
     */
    public static NodeConfigManager getInstance() {
        if (base == null) {
            base = new NodeConfigManager();
        }
        return base;
    }

    private void localconfig() {
        followredirects = Boolean.parseBoolean(getProvParam("FOLLOW_REDIRECTS", "false"));
        eventloginterval = getProvParam("LOGROLL_INTERVAL", "30s");
        initfailuretimer = 10000;
        waitForFileProcessFailureTimer = 600000;
        maxfailuretimer = 3600000;
        expirationtimer = 86400000;
        failurebackoff = 2.0;
        deliverythreads = 40;
        fairfilelimit = 100;
        fairtimelimit = 60000;
        fdpstart = 0.05;
        fdpstop = 0.2;
        try {
            initfailuretimer = (long) (Double.parseDouble(getProvParam("DELIVERY_INIT_RETRY_INTERVAL")) * 1000);
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_INIT_RETRY_INTERVAL", e);
        }
        try {
            waitForFileProcessFailureTimer = (long) (Double.parseDouble(getProvParam("DELIVERY_FILE_PROCESS_INTERVAL"))
                    * 1000);
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_FILE_PROCESS_INTERVAL", e);
        }
        try {
            maxfailuretimer = (long) (Double.parseDouble(getProvParam("DELIVERY_MAX_RETRY_INTERVAL")) * 1000);
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_MAX_RETRY_INTERVAL", e);
        }
        try {
            expirationtimer = (long) (Double.parseDouble(getProvParam("DELIVERY_MAX_AGE")) * 1000);
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_MAX_AGE", e);
        }
        try {
            failurebackoff = Double.parseDouble(getProvParam("DELIVERY_RETRY_RATIO"));
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_RETRY_RATIO", e);
        }
        try {
            deliverythreads = Integer.parseInt(getProvParam("DELIVERY_THREADS"));
        } catch (Exception e) {
            eelfLogger.trace("Error parsing DELIVERY_THREADS", e);
        }
        try {
            fairfilelimit = Integer.parseInt(getProvParam("FAIR_FILE_LIMIT"));
        } catch (Exception e) {
            eelfLogger.trace("Error parsing FAIR_FILE_LIMIT", e);
        }
        try {
            fairtimelimit = (long) (Double.parseDouble(getProvParam("FAIR_TIME_LIMIT")) * 1000);
        } catch (Exception e) {
            eelfLogger.trace("Error parsing FAIR_TIME_LIMIT", e);
        }
        try {
            fdpstart = Double.parseDouble(getProvParam("FREE_DISK_RED_PERCENT")) / 100.0;
        } catch (Exception e) {
            eelfLogger.trace("Error parsing FREE_DISK_RED_PERCENT", e);
        }
        try {
            fdpstop = Double.parseDouble(getProvParam("FREE_DISK_YELLOW_PERCENT")) / 100.0;
        } catch (Exception e) {
            eelfLogger.trace("Error parsing FREE_DISK_YELLOW_PERCENT", e);
        }
        if (fdpstart < 0.01) {
            fdpstart = 0.01;
        }
        if (fdpstart > 0.5) {
            fdpstart = 0.5;
        }
        if (fdpstop < fdpstart) {
            fdpstop = fdpstart;
        }
        if (fdpstop > 0.5) {
            fdpstop = 0.5;
        }
    }

    private void fetchNodeConfigFromProv() {
        try {
            eelfLogger.debug("NodeConfigMan.fetchNodeConfigFromProv: provurl:: {}", provurl);
            URL url = new URL(provurl);
            Reader reader = new InputStreamReader(url.openStream());
            nodeConfig = new NodeConfig(new ProvData(reader), myname, spooldir, extHttpsPort, nak);
            localconfig();
            configtasks.startRun();
            runTasks();
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf("fetchNodeConfigFromProv");
            eelfLogger.error(EelfMsgs.MESSAGE_CONF_FAILED, e.toString());
            eelfLogger.error("NODE0306 Configuration failed {} - try again later", e);
            pfetcher.request();
        }
    }

    private void runTasks() {
        Runnable rr;
        while ((rr = configtasks.next()) != null) {
            try {
                rr.run();
            } catch (Exception e) {
                eelfLogger.error("NODE0518 Exception fetchconfig: " + e);
            }
        }
    }

    /**
     * Process a gofetch request from a particular IP address.  If the IP address is not an IP address we would go to to
     * fetch the provisioning data, ignore the request.  If the data has been fetched very recently (default 10
     * seconds), wait a while before fetching again.
     */
    synchronized void gofetch(String remoteAddr) {
        if (provcheck.isReachable(remoteAddr)) {
            eelfLogger.debug("NODE0307 Received configuration fetch request from provisioning server " + remoteAddr);
            pfetcher.request();
        } else {
            eelfLogger.debug("NODE0308 Received configuration fetch request from unexpected server " + remoteAddr);
        }
    }

    /**
     * Am I configured.
     */
    public boolean isConfigured() {
        return nodeConfig != null;
    }

    /**
     * Am I shut down.
     */
    boolean isShutdown() {
        return quiesce.exists();
    }

    /**
     * Given a routing string, get the targets.
     *
     * @param routing Target string
     * @return array of targets
     */
    Target[] parseRouting(String routing) {
        return nodeConfig.parseRouting(routing);
    }

    /**
     * Given a set of credentials and an IP address, is this request from another node.
     *
     * @param credentials Credentials offered by the supposed node
     * @param ip IP address the request came from
     * @return If the credentials and IP address are recognized, true, otherwise false.
     */
    boolean isAnotherNode(String credentials, String ip) {
        return nodeConfig.isAnotherNode(credentials, ip);
    }

    /**
     * Check whether publication is allowed.
     *
     * @param feedid The ID of the feed being requested
     * @param credentials The offered credentials
     * @param ip The requesting IP address
     * @return True if the IP and credentials are valid for the specified feed.
     */
    String isPublishPermitted(String feedid, String credentials, String ip) {
        return nodeConfig.isPublishPermitted(feedid, credentials, ip);
    }

    /**
     * Check whether delete file is allowed.
     *
     * @param subId The ID of the subscription being requested
     * @return True if the delete file is permitted for the subscriber.
     */
    boolean isDeletePermitted(String subId) {
        return nodeConfig.isDeletePermitted(subId);
    }

    /**
     * Check who the user is given the feed ID and the offered credentials.
     *
     * @param feedid The ID of the feed specified
     * @param credentials The offered credentials
     * @return Null if the credentials are invalid or the user if they are valid.
     */
    String getAuthUser(String feedid, String credentials) {
        return nodeConfig.getAuthUser(feedid, credentials);
    }

    /**
     * Check if the publish request should be sent to another node based on the feedid, user, and source IP address.
     *
     * @param feedid The ID of the feed specified
     * @param user The publishing user
     * @param ip The IP address of the publish endpoint
     * @return Null if the request should be accepted or the correct hostname if it should be sent to another node.
     */
    String getIngressNode(String feedid, String user, String ip) {
        return nodeConfig.getIngressNode(feedid, user, ip);
    }

    /**
     * Get a provisioned configuration parameter (from the provisioning server configuration).
     *
     * @param name The name of the parameter
     * @return The value of the parameter or null if it is not defined.
     */
    private String getProvParam(String name) {
        return nodeConfig.getProvParam(name);
    }

    /**
     * Get a provisioned configuration parameter (from the provisioning server configuration).
     *
     * @param name The name of the parameter
     * @param defaultValue The value to use if the parameter is not defined
     * @return The value of the parameter or deflt if it is not defined.
     */
    private String getProvParam(String name, String defaultValue) {
        name = nodeConfig.getProvParam(name);
        if (name == null) {
            name = defaultValue;
        }
        return name;
    }

    /**
     * Generate a publish ID.
     */
    public String getPublishId() {
        return publishId.next();
    }

    /**
     * Get all the outbound spooling destinations. This will include both subscriptions and nodes.
     */
    public DestInfo[] getAllDests() {
        return nodeConfig.getAllDests();
    }

    /**
     * Register a task to run whenever the configuration changes.
     */
    public void registerConfigTask(Runnable task) {
        configtasks.addTask(task);
    }

    /**
     * Deregister a task to run whenever the configuration changes.
     */
    void deregisterConfigTask(Runnable task) {
        configtasks.removeTask(task);
    }

    /**
     * Get the URL to deliver a message to.
     *
     * @param destinationInfo The destination information
     * @param fileid The file ID
     * @return The URL to deliver to
     */
    public String getDestURL(DestInfo destinationInfo, String fileid) {
        String subid = destinationInfo.getSubId();
        String purl = destinationInfo.getURL();
        if (followredirects && subid != null) {
            purl = rdmgr.lookup(subid, purl);
        }
        return (purl + "/" + fileid);
    }

    /**
     * Set up redirection on receipt of a 3XX from a target URL.
     */
    public boolean handleRedirection(DestInfo destinationInfo, String redirto, String fileid) {
        fileid = "/" + fileid;
        String subid = destinationInfo.getSubId();
        String purl = destinationInfo.getURL();
        if (followredirects && subid != null && redirto.endsWith(fileid)) {
            redirto = redirto.substring(0, redirto.length() - fileid.length());
            if (!redirto.equals(purl)) {
                rdmgr.redirect(subid, purl, redirto);
                return (true);
            }
        }
        return (false);
    }

    /**
     * Handle unreachable target URL.
     */
    public void handleUnreachable(DestInfo destinationInfo) {
        String subid = destinationInfo.getSubId();
        if (followredirects && subid != null) {
            rdmgr.forget(subid);
        }
    }

    /**
     * Get the timeout before retrying after an initial delivery failure.
     */
    public long getInitFailureTimer() {
        return initfailuretimer;
    }

    /**
     * Get the timeout before retrying after delivery and wait for file processing.
     */
    public long getWaitForFileProcessFailureTimer() {
        return waitForFileProcessFailureTimer;
    }

    /**
     * Get the maximum timeout between delivery attempts.
     */
    public long getMaxFailureTimer() {
        return maxfailuretimer;
    }

    /**
     * Get the ratio between consecutive delivery attempts.
     */
    public double getFailureBackoff() {
        return failurebackoff;
    }

    /**
     * Get the expiration timer for deliveries.
     */
    public long getExpirationTimer() {
        return expirationtimer;
    }

    /**
     * Get the maximum number of file delivery attempts before checking if another queue has work to be performed.
     */
    public int getFairFileLimit() {
        return fairfilelimit;
    }

    /**
     * Get the maximum amount of time spent delivering files before checking if another queue has work to be performed.
     */
    public long getFairTimeLimit() {
        return fairtimelimit;
    }

    /**
     * Get the targets for a feed.
     *
     * @param feedid The feed ID
     * @return The targets this feed should be delivered to
     */
    Target[] getTargets(String feedid) {
        return nodeConfig.getTargets(feedid);
    }

    /**
     * Get the spool directory for temporary files.
     */
    String getSpoolDir() {
        return spooldir + "/f";
    }

    /**
     * Get the spool directory for a subscription.
     */
    String getSpoolDir(String subid, String remoteaddr) {
        if (provcheck.isFrom(remoteaddr)) {
            String sdir = nodeConfig.getSpoolDir(subid);
            if (sdir != null) {
                eelfLogger.debug("NODE0310 Received subscription reset request for subscription " + subid
                        + " from provisioning server " + remoteaddr);
            } else {
                eelfLogger.debug("NODE0311 Received subscription reset request for unknown subscription " + subid
                        + " from provisioning server " + remoteaddr);
            }
            return sdir;
        } else {
            eelfLogger.debug("NODE0312 Received subscription reset request from unexpected server " + remoteaddr);
            return null;
        }
    }

    /**
     * Get the base directory for spool directories.
     */
    public String getSpoolBase() {
        return spooldir;
    }

    /**
     * Get the http port.
     */
    int getHttpPort() {
        return intHttpPort;
    }

    /**
     * Get the https port.
     */
    int getHttpsPort() {
        return intHttpsPort;
    }

    /**
     * Get the externally visible https port.
     */
    int getExtHttpsPort() {
        return extHttpsPort;
    }

    /**
     * Get the external name of this machine.
     */
    public String getMyName() {
        return myname;
    }

    /**
     * Get the number of threads to use for delivery.
     */
    public int getDeliveryThreads() {
        return deliverythreads;
    }

    /**
     * Get the URL for uploading the event log data.
     */
    public String getEventLogUrl() {
        return eventlogurl;
    }

    /**
     * Get the prefix for the names of event log files.
     */
    public String getEventLogPrefix() {
        return eventlogprefix;
    }

    /**
     * Get the suffix for the names of the event log files.
     */
    public String getEventLogSuffix() {
        return eventlogsuffix;
    }

    /**
     * Get the interval between event log file rollovers.
     */
    public String getEventLogInterval() {
        return eventloginterval;
    }

    /**
     * Should I follow redirects from subscribers.
     */
    public boolean isFollowRedirects() {
        return followredirects;
    }

    /**
     * Get the directory where the event and node log files live.
     */
    public String getLogDir() {
        return logdir;
    }

    /**
     * How long do I keep log files (in milliseconds).
     */
    public long getLogRetention() {
        return logretention;
    }

    /**
     * Get the timer.
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * Get the feed ID for a subscription.
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid) {
        return nodeConfig.getFeedId(subid);
    }

    /**
     * Get the authorization string this node uses.
     *
     * @return The Authorization string for this node
     */
    public String getMyAuth() {
        return nodeConfig.getMyAuth();
    }

    /**
     * Get the fraction of free spool disk space where we start throwing away undelivered files.  This is
     * FREE_DISK_RED_PERCENT / 100.0.  Default is 0.05.  Limited by 0.01 <= FreeDiskStart <= 0.5.
     */
    public double getFreeDiskStart() {
        return fdpstart;
    }

    /**
     * Get the fraction of free spool disk space where we stop throwing away undelivered files.  This is
     * FREE_DISK_YELLOW_PERCENT / 100.0.  Default is 0.2.  Limited by FreeDiskStart <= FreeDiskStop <= 0.5.
     */
    public double getFreeDiskStop() {
        return fdpstop;
    }

    protected boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public static NodeTlsManager getNodeTlsManager() {
        return nodeTlsManager;
    }

    /**
     * Generate publish IDs.
     */
    static class PublishId {

        private long nextuid;
        private final String myname;

        /**
         * Generate publish IDs for the specified name.
         *
         * @param myname Unique identifier for this publish ID generator (usually fqdn of server)
         */
        public PublishId(String myname) {
            this.myname = myname;
        }

        /**
         * Generate a Data Router Publish ID that uniquely identifies the particular invocation of the Publish API for log
         * correlation purposes.
         */
        public synchronized String next() {
            long now = System.currentTimeMillis();
            if (now < nextuid) {
                now = nextuid;
            }
            nextuid = now + 1;
            return (now + "." + myname);
        }
    }

    /**
     * Manage a list of tasks to be executed when an event occurs. This makes the following guarantees:
     * <ul>
     * <li>Tasks can be safely added and removed in the middle of a run.</li>
     * <li>No task will be returned more than once during a run.</li>
     * <li>No task will be returned when it is not, at that moment, in the list of tasks.</li>
     * <li>At the moment when next() returns null, all tasks on the list have been returned during the run.</li>
     * <li>Initially and once next() returns null during a run, next() will continue to return null until startRun() is
     * called.
     * </ul>
     */
    static class TaskList {

        private Iterator<Runnable> runlist;
        private final HashSet<Runnable> tasks = new HashSet<>();
        private HashSet<Runnable> togo;
        private HashSet<Runnable> sofar;
        private HashSet<Runnable> added;
        private HashSet<Runnable> removed;

        /**
         * Start executing the sequence of tasks.
         */
        synchronized void startRun() {
            sofar = new HashSet<>();
            added = new HashSet<>();
            removed = new HashSet<>();
            togo = new HashSet<>(tasks);
            runlist = togo.iterator();
        }

        /**
         * Get the next task to execute.
         */
        synchronized Runnable next() {
            while (runlist != null) {
                if (runlist.hasNext()) {
                    Runnable task = runlist.next();
                    if (addTaskToSoFar(task)) {
                        return task;
                    }
                }
                if (!added.isEmpty()) {
                    togo = added;
                    added = new HashSet<>();
                    removed.clear();
                    runlist = togo.iterator();
                    continue;
                }
                togo = null;
                added = null;
                removed = null;
                sofar = null;
                runlist = null;
            }
            return (null);
        }

        /**
         * Add a task to the list of tasks to run whenever the event occurs.
         */
        synchronized void addTask(Runnable task) {
            if (runlist != null) {
                added.add(task);
                removed.remove(task);
            }
            tasks.add(task);
        }

        /**
         * Remove a task from the list of tasks to run whenever the event occurs.
         */
        synchronized void removeTask(Runnable task) {
            if (runlist != null) {
                removed.add(task);
                added.remove(task);
            }
            tasks.remove(task);
        }

        private boolean addTaskToSoFar(Runnable task) {
            if (removed.contains(task)) {
                return false;
            }
            if (sofar.contains(task)) {
                return false;
            }
            sofar.add(task);
            return true;
        }
    }
}
