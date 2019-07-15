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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;


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

    private static final String CHANGE_ME = "changeme";
    private static final String NODE_CONFIG_MANAGER = "NodeConfigManager";
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeConfigManager.class);
    private static NodeConfigManager base = new NodeConfigManager();

    private Timer timer = new Timer("Node Configuration Timer", true);
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
    private String provurl;
    private String provhost;
    private IsFrom provcheck;
    private int gfport;
    private int svcport;
    private int port;
    private String spooldir;
    private String logdir;
    private long logretention;
    private String redirfile;
    private String kstype;
    private String ksfile;
    private String kspass;
    private String kpass;
    private String tstype;
    private String tsfile;
    private String tspass;
    private String myname;
    private RedirManager rdmgr;
    private RateLimitedOperation pfetcher;
    private NodeConfig config;
    private File quiesce;
    private PublishId pid;
    private String nak;
    private TaskList configtasks = new TaskList();
    private String eventlogurl;
    private String eventlogprefix;
    private String eventlogsuffix;
    private String eventloginterval;
    private boolean followredirects;
    private String[] enabledprotocols;
    private String aafType;
    private String aafInstance;
    private String aafAction;
    private String aafURL;
    private boolean cadiEnabled;


    /**
     * Initialize the configuration of a Data Router node.
     */
    private NodeConfigManager() {

        Properties drNodeProperties = new Properties();
        try {
            eelfLogger.debug("NODE0301 Loading local config file node.properties");
            drNodeProperties.load(new FileInputStream(System
                    .getProperty("org.onap.dmaap.datarouter.node.properties", "/opt/app/datartr/etc/node.properties")));
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf(NODE_CONFIG_MANAGER);
            eelfLogger.error(EelfMsgs.MESSAGE_PROPERTIES_LOAD_ERROR, e,
                    System.getProperty("org.onap.dmaap.datarouter.node.properties",
                            "/opt/app/datartr/etc/node.properties"));
        }
        provurl = drNodeProperties.getProperty("ProvisioningURL", "https://dmaap-dr-prov:8443/internal/prov");
        /*
         * START - AAF changes: TDP EPIC US# 307413
         * Pull AAF settings from node.properties
         */
        aafType = drNodeProperties.getProperty("AAFType", "org.onap.dmaap-dr.feed");
        aafInstance = drNodeProperties.getProperty("AAFInstance", "legacy");
        aafAction = drNodeProperties.getProperty("AAFAction", "publish");
        aafURL = drNodeProperties.getProperty("AafUrl", "https://aaf-onap-test.osaaf.org:8095");
        cadiEnabled = Boolean.parseBoolean(drNodeProperties.getProperty("CadiEnabled", "false"));
        /*
         * END - AAF changes: TDP EPIC US# 307413
         * Pull AAF settings from node.properties
         */
        //Disable and enable protocols*/
        enabledprotocols = ((drNodeProperties.getProperty("NodeHttpsProtocols")).trim()).split("\\|");

        try {
            provhost = (new URL(provurl)).getHost();
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf(NODE_CONFIG_MANAGER);
            eelfLogger.error(EelfMsgs.MESSAGE_BAD_PROV_URL, e, provurl);
            System.exit(1);
        }
        eelfLogger.debug("NODE0303 Provisioning server is " + provhost);
        eventlogurl = drNodeProperties.getProperty("LogUploadURL", "https://feeds-drtr.web.att.com/internal/logs");
        provcheck = new IsFrom(provhost);
        gfport = Integer.parseInt(drNodeProperties.getProperty("IntHttpPort", "8080"));
        svcport = Integer.parseInt(drNodeProperties.getProperty("IntHttpsPort", "8443"));
        port = Integer.parseInt(drNodeProperties.getProperty("ExtHttpsPort", "443"));
        spooldir = drNodeProperties.getProperty("SpoolDir", "spool");
        File fdir = new File(spooldir + "/f");
        fdir.mkdirs();
        for (File junk : fdir.listFiles()) {
            if (junk.isFile()) {
                junk.delete();
            }
        }
        logdir = drNodeProperties.getProperty("LogDir", "logs");
        (new File(logdir)).mkdirs();
        logretention = Long.parseLong(drNodeProperties.getProperty("LogRetention", "30")) * 86400000L;
        eventlogprefix = logdir + "/events";
        eventlogsuffix = ".log";
        redirfile = drNodeProperties.getProperty("RedirectionFile", "etc/redirections.dat");
        kstype = drNodeProperties.getProperty("KeyStoreType", "jks");
        ksfile = drNodeProperties.getProperty("KeyStoreFile", "etc/keystore");
        kspass = drNodeProperties.getProperty("KeyStorePassword", CHANGE_ME);
        kpass = drNodeProperties.getProperty("KeyPassword", CHANGE_ME);
        tstype = drNodeProperties.getProperty("TrustStoreType", "jks");
        tsfile = drNodeProperties.getProperty("TrustStoreFile");
        tspass = drNodeProperties.getProperty("TrustStorePassword", CHANGE_ME);
        if (tsfile != null && tsfile.length() > 0) {
            System.setProperty("javax.net.ssl.trustStoreType", tstype);
            System.setProperty("javax.net.ssl.trustStore", tsfile);
            System.setProperty("javax.net.ssl.trustStorePassword", tspass);
        }
        nak = drNodeProperties.getProperty("NodeAuthKey", "Node123!");
        quiesce = new File(drNodeProperties.getProperty("QuiesceFile", "etc/SHUTDOWN"));
        myname = NodeUtils.getCanonicalName(kstype, ksfile, kspass);
        if (myname == null) {
            NodeUtils.setIpAndFqdnForEelf(NODE_CONFIG_MANAGER);
            eelfLogger.error(EelfMsgs.MESSAGE_KEYSTORE_FETCH_ERROR, ksfile);
            eelfLogger.error("NODE0309 Unable to fetch canonical name from keystore file " + ksfile);
            System.exit(1);
        }
        eelfLogger.debug("NODE0304 My certificate says my name is " + myname);
        pid = new PublishId(myname);
        long minrsinterval = Long.parseLong(drNodeProperties.getProperty("MinRedirSaveInterval", "10000"));
        long minpfinterval = Long.parseLong(drNodeProperties.getProperty("MinProvFetchInterval", "10000"));
        rdmgr = new RedirManager(redirfile, minrsinterval, timer);
        pfetcher = new RateLimitedOperation(minpfinterval, timer) {
            public void run() {
                fetchconfig();
            }
        };
        eelfLogger.debug("NODE0305 Attempting to fetch configuration at " + provurl);
        pfetcher.request();
    }

    /**
     * Get the default node configuration manager.
     */
    public static NodeConfigManager getInstance() {
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

    private void fetchconfig() {
        try {
            eelfLogger.debug("NodeConfigMan.fetchConfig: provurl:: " + provurl);
            Reader reader = new InputStreamReader((new URL(provurl)).openStream());
            config = new NodeConfig(new ProvData(reader), myname, spooldir, port, nak);
            localconfig();
            configtasks.startRun();
            runTasks();
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf("fetchconfigs");
            eelfLogger.error(EelfMsgs.MESSAGE_CONF_FAILED, e.toString());
            eelfLogger.error("NODE0306 Configuration failed " + e.toString() + " - try again later", e);
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
    public synchronized void gofetch(String remoteAddr) {
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
        return (config != null);
    }

    /**
     * Am I shut down.
     */
    public boolean isShutdown() {
        return (quiesce.exists());
    }

    /**
     * Given a routing string, get the targets.
     *
     * @param routing Target string
     * @return array of targets
     */
    public Target[] parseRouting(String routing) {
        return (config.parseRouting(routing));
    }

    /**
     * Given a set of credentials and an IP address, is this request from another node.
     *
     * @param credentials Credentials offered by the supposed node
     * @param ip IP address the request came from
     * @return If the credentials and IP address are recognized, true, otherwise false.
     */
    public boolean isAnotherNode(String credentials, String ip) {
        return (config.isAnotherNode(credentials, ip));
    }

    /**
     * Check whether publication is allowed.
     *
     * @param feedid The ID of the feed being requested
     * @param credentials The offered credentials
     * @param ip The requesting IP address
     * @return True if the IP and credentials are valid for the specified feed.
     */
    public String isPublishPermitted(String feedid, String credentials, String ip) {
        return (config.isPublishPermitted(feedid, credentials, ip));
    }

    /**
     * Check whether publication is allowed for AAF Feed.
     *
     * @param feedid The ID of the feed being requested
     * @param ip The requesting IP address
     * @return True if the IP and credentials are valid for the specified feed.
     */
    public String isPublishPermitted(String feedid, String ip) {
        return (config.isPublishPermitted(feedid, ip));
    }

    /**
     * Check whether delete file is allowed.
     *
     * @param subId The ID of the subscription being requested
     * @return True if the delete file is permitted for the subscriber.
     */
    public boolean isDeletePermitted(String subId) {
        return (config.isDeletePermitted(subId));
    }

    /**
     * Check who the user is given the feed ID and the offered credentials.
     *
     * @param feedid The ID of the feed specified
     * @param credentials The offered credentials
     * @return Null if the credentials are invalid or the user if they are valid.
     */
    public String getAuthUser(String feedid, String credentials) {
        return (config.getAuthUser(feedid, credentials));
    }

    /**
     * AAF changes: TDP EPIC US# 307413 Check AAF_instance for feed ID in NodeConfig.
     *
     * @param feedid The ID of the feed specified
     */
    public String getAafInstance(String feedid) {
        return (config.getAafInstance(feedid));
    }

    public String getAafInstance() {
        return aafInstance;
    }

    /**
     * Check if the publish request should be sent to another node based on the feedid, user, and source IP address.
     *
     * @param feedid The ID of the feed specified
     * @param user The publishing user
     * @param ip The IP address of the publish endpoint
     * @return Null if the request should be accepted or the correct hostname if it should be sent to another node.
     */
    public String getIngressNode(String feedid, String user, String ip) {
        return (config.getIngressNode(feedid, user, ip));
    }

    /**
     * Get a provisioned configuration parameter (from the provisioning server configuration).
     *
     * @param name The name of the parameter
     * @return The value of the parameter or null if it is not defined.
     */
    public String getProvParam(String name) {
        return (config.getProvParam(name));
    }

    /**
     * Get a provisioned configuration parameter (from the provisioning server configuration).
     *
     * @param name The name of the parameter
     * @param defaultValue The value to use if the parameter is not defined
     * @return The value of the parameter or deflt if it is not defined.
     */
    public String getProvParam(String name, String defaultValue) {
        name = config.getProvParam(name);
        if (name == null) {
            name = defaultValue;
        }
        return (name);
    }

    /**
     * Generate a publish ID.
     */
    public String getPublishId() {
        return (pid.next());
    }

    /**
     * Get all the outbound spooling destinations. This will include both subscriptions and nodes.
     */
    public DestInfo[] getAllDests() {
        return (config.getAllDests());
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
    public void deregisterConfigTask(Runnable task) {
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
     * Is a destination redirected.
     */
    public boolean isDestRedirected(DestInfo destinfo) {
        return (followredirects && rdmgr.isRedirected(destinfo.getSubId()));
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
     * Set up redirection on receipt of a 3XX from a target URL.
     */
    public boolean handleRedirectionSubLevel(DeliveryTask task, DestInfo destinfo, String redirto, String fileid) {
        fileid = "/" + fileid;
        String subid = destinfo.getSubId();
        String purl = destinfo.getURL();
        if (task.getFollowRedirects() && subid != null && redirto.endsWith(fileid)) {
            redirto = redirto.substring(0, redirto.length() - fileid.length());
            if (!redirto.equals(purl)) {
                rdmgr.redirect(subid, purl, redirto);
                return true;
            }
        }
        return false;
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
        return (initfailuretimer);
    }

    /**
     * Get the timeout before retrying after delivery and wait for file processing.
     */
    public long getWaitForFileProcessFailureTimer() {
        return (waitForFileProcessFailureTimer);
    }

    /**
     * Get the maximum timeout between delivery attempts.
     */
    public long getMaxFailureTimer() {
        return (maxfailuretimer);
    }

    /**
     * Get the ratio between consecutive delivery attempts.
     */
    public double getFailureBackoff() {
        return (failurebackoff);
    }

    /**
     * Get the expiration timer for deliveries.
     */
    public long getExpirationTimer() {
        return (expirationtimer);
    }

    /**
     * Get the maximum number of file delivery attempts before checking if another queue has work to be performed.
     */
    public int getFairFileLimit() {
        return (fairfilelimit);
    }

    /**
     * Get the maximum amount of time spent delivering files before checking if another queue has work to be performed.
     */
    public long getFairTimeLimit() {
        return (fairtimelimit);
    }

    /**
     * Get the targets for a feed.
     *
     * @param feedid The feed ID
     * @return The targets this feed should be delivered to
     */
    public Target[] getTargets(String feedid) {
        return (config.getTargets(feedid));
    }

    /**
     * Get the creation date for a feed.
     *
     * @param feedid The feed ID
     * @return the timestamp of creation date of feed id passed
     */
    public String getCreatedDate(String feedid) {
        return (config.getCreatedDate(feedid));
    }

    /**
     * Get the spool directory for temporary files.
     */
    public String getSpoolDir() {
        return (spooldir + "/f");
    }

    /**
     * Get the spool directory for a subscription.
     */
    public String getSpoolDir(String subid, String remoteaddr) {
        if (provcheck.isFrom(remoteaddr)) {
            String sdir = config.getSpoolDir(subid);
            if (sdir != null) {
                eelfLogger.debug("NODE0310 Received subscription reset request for subscription " + subid
                        + " from provisioning server " + remoteaddr);
            } else {
                eelfLogger.debug("NODE0311 Received subscription reset request for unknown subscription " + subid
                        + " from provisioning server " + remoteaddr);
            }
            return (sdir);
        } else {
            eelfLogger.debug("NODE0312 Received subscription reset request from unexpected server " + remoteaddr);
            return (null);
        }
    }

    /**
     * Get the base directory for spool directories.
     */
    public String getSpoolBase() {
        return (spooldir);
    }

    /**
     * Get the key store type.
     */
    public String getKSType() {
        return (kstype);
    }

    /**
     * Get the key store file.
     */
    public String getKSFile() {
        return (ksfile);
    }

    /**
     * Get the key store password.
     */
    public String getKSPass() {
        return (kspass);
    }

    /**
     * Get the key password.
     */
    public String getKPass() {
        return (kpass);
    }

    /**
     * Get the http port.
     */
    public int getHttpPort() {
        return (gfport);
    }

    /**
     * Get the https port.
     */
    public int getHttpsPort() {
        return (svcport);
    }

    /**
     * Get the externally visible https port.
     */
    public int getExtHttpsPort() {
        return (port);
    }

    /**
     * Get the external name of this machine.
     */
    public String getMyName() {
        return (myname);
    }

    /**
     * Get the number of threads to use for delivery.
     */
    public int getDeliveryThreads() {
        return (deliverythreads);
    }

    /**
     * Get the URL for uploading the event log data.
     */
    public String getEventLogUrl() {
        return (eventlogurl);
    }

    /**
     * Get the prefix for the names of event log files.
     */
    public String getEventLogPrefix() {
        return (eventlogprefix);
    }

    /**
     * Get the suffix for the names of the event log files.
     */
    public String getEventLogSuffix() {
        return (eventlogsuffix);
    }

    /**
     * Get the interval between event log file rollovers.
     */
    public String getEventLogInterval() {
        return (eventloginterval);
    }

    /**
     * Should I follow redirects from subscribers.
     */
    public boolean isFollowRedirects() {
        return (followredirects);
    }

    /**
     * Get the directory where the event and node log files live.
     */
    public String getLogDir() {
        return (logdir);
    }

    /**
     * How long do I keep log files (in milliseconds).
     */
    public long getLogRetention() {
        return (logretention);
    }

    /**
     * Get the timer.
     */
    public Timer getTimer() {
        return (timer);
    }

    /**
     * Get the feed ID for a subscription.
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid) {
        return (config.getFeedId(subid));
    }

    /**
     * Get the authorization string this node uses.
     *
     * @return The Authorization string for this node
     */
    public String getMyAuth() {
        return (config.getMyAuth());
    }

    /**
     * Get the fraction of free spool disk space where we start throwing away undelivered files.  This is
     * FREE_DISK_RED_PERCENT / 100.0.  Default is 0.05.  Limited by 0.01 <= FreeDiskStart <= 0.5.
     */
    public double getFreeDiskStart() {
        return (fdpstart);
    }

    /**
     * Get the fraction of free spool disk space where we stop throwing away undelivered files.  This is
     * FREE_DISK_YELLOW_PERCENT / 100.0.  Default is 0.2.  Limited by FreeDiskStart <= FreeDiskStop <= 0.5.
     */
    public double getFreeDiskStop() {
        return (fdpstop);
    }

    /**
     * Disable and enable protocols.
     */
    public String[] getEnabledprotocols() {
        return enabledprotocols;
    }

    public void setEnabledprotocols(String[] enabledprotocols) {
        this.enabledprotocols = enabledprotocols.clone();
    }

    public String getAafType() {
        return aafType;
    }

    public void setAafType(String aafType) {
        this.aafType = aafType;
    }

    public void setAafInstance(String aafInstance) {
        this.aafInstance = aafInstance;
    }

    public String getAafAction() {
        return aafAction;
    }

    public void setAafAction(String aafAction) {
        this.aafAction = aafAction;
    }

    /*
     * Get aafURL from SWM variable
     * */
    public String getAafURL() {
        return aafURL;
    }

    public void setAafURL(String aafURL) {
        this.aafURL = aafURL;
    }

    public boolean getCadiEnabled() {
        return cadiEnabled;
    }

    public void setCadiEnabled(boolean cadiEnabled) {
        this.cadiEnabled = cadiEnabled;
    }

    /**
     * Builds the permissions string to be verified.
     *
     * @param aafInstance The aaf instance
     * @return The permissions
     */
    protected String getPermission(String aafInstance) {
        try {
            String type = getAafType();
            String action = getAafAction();
            if ("".equals(aafInstance)) {
                aafInstance = getAafInstance();
            }
            return type + "|" + aafInstance + "|" + action;
        } catch (Exception e) {
            eelfLogger.error("NODE0543 NodeConfigManager.getPermission: ", e);
        }
        return null;
    }
}
