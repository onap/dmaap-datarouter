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

import static org.onap.dmaap.datarouter.provisioning.BaseServlet.TEXT_CT;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;
import org.onap.dmaap.datarouter.provisioning.beans.EgressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Group;
import org.onap.dmaap.datarouter.provisioning.beans.IngressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.NetworkRoute;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.beans.Syncable;

/**
 * This class handles synchronization between provisioning servers (PODs).  It has three primary functions:
 * <ol>
 * <li>Checking DNS once per minute to see which POD the DNS CNAME points to. The CNAME will point to
 * the active (master) POD.</li>
 * <li>On non-master (standby) PODs, fetches provisioning data and logs in order to keep MariaDB in sync.</li>
 * <li>Providing information to other parts of the system as to the current role (ACTIVE_POD, STANDBY_POD, UNKNOWN_POD)
 * of this POD.</li>
 * </ol>
 * <p>For this to work correctly, the following code needs to be placed at the beginning of main().</p>
 * <code>
 * Security.setProperty("networkaddress.cache.ttl", "10");
 * </code>
 *
 * @author Robert Eby
 * @version $Id: SynchronizerTask.java,v 1.10 2014/03/21 13:50:10 eby Exp $
 */

public class SynchronizerTask extends TimerTask {

    /**
     * This is a singleton -- there is only one SynchronizerTask object in the server.
     */
    private static SynchronizerTask synctask;

    /**
     * This POD is unknown -- not on the list of PODs.
     */
    public static final int UNKNOWN_POD = 0;
    /**
     * This POD is active -- on the list of PODs, and the DNS CNAME points to us.
     */
    public static final int ACTIVE_POD = 1;
    /**
     * This POD is standby -- on the list of PODs, and the DNS CNAME does not point to us.
     */
    public static final int STANDBY_POD = 2;

    private static final String[] stnames = {"UNKNOWN_POD", "ACTIVE_POD", "STANDBY_POD"};
    private static final long ONE_HOUR = 60 * 60 * 1000L;

    private long nextMsg = 0;    // only display the "Current podState" msg every 5 mins.

    private final EELFLogger logger;
    private final Timer rolex;
    private final String spooldir;
    private int podState;
    private boolean doFetch;
    private long nextsynctime;
    private AbstractHttpClient httpclient = null;

    @SuppressWarnings("deprecation")
    private SynchronizerTask() {
        logger = EELFManager.getInstance().getLogger("InternalLog");
        rolex = new Timer();
        spooldir = ProvRunner.getProvProperties().getProperty("org.onap.dmaap.datarouter.provserver.spooldir");
        podState = UNKNOWN_POD;
        doFetch = true;        // start off with a fetch
        nextsynctime = 0;

        logger.info("PROV5000: Sync task starting, server podState is UNKNOWN_POD");
        try {
            // Set up keystore
            String type = AafPropsUtils.KEYSTORE_TYPE_PROPERTY;
            String store = ProvRunner.getAafPropsUtils().getKeystorePathProperty();
            String pass = ProvRunner.getAafPropsUtils().getKeystorePassProperty();
            KeyStore keyStore = KeyStore.getInstance(type);
            try (FileInputStream instream = new FileInputStream(new File(store))) {
                keyStore.load(instream, pass.toCharArray());

            }
            // Set up truststore
            store = ProvRunner.getAafPropsUtils().getTruststorePathProperty();
            pass = ProvRunner.getAafPropsUtils().getTruststorePassProperty();
            KeyStore trustStore = null;
            if (store != null && store.length() > 0) {
                trustStore = KeyStore.getInstance(AafPropsUtils.TRUESTSTORE_TYPE_PROPERTY);
                try (FileInputStream instream = new FileInputStream(new File(store))) {
                    trustStore.load(instream, pass.toCharArray());

                }
            }

            // We are connecting with the node name, but the certificate will have the CNAME
            // So we need to accept a non-matching certificate name
            String keystorepass = ProvRunner.getAafPropsUtils().getKeystorePassProperty();
            try (AbstractHttpClient hc = new DefaultHttpClient()) {
                SSLSocketFactory socketFactory =
                        (trustStore == null)
                                ? new SSLSocketFactory(keyStore, keystorepass)
                                : new SSLSocketFactory(keyStore, keystorepass, trustStore);
                socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Scheme sch = new Scheme("https", 443, socketFactory);
                hc.getConnectionManager().getSchemeRegistry().register(sch);
                httpclient = hc;
            }
            setSynchTimer(ProvRunner.getProvProperties().getProperty(
                "org.onap.dmaap.datarouter.provserver.sync_interval", "5000"));
        } catch (Exception e) {
            logger.warn("PROV5005: Problem starting the synchronizer: " + e);
        }
    }

    private void setSynchTimer(String strInterval) {
        // Run once every 5 seconds to check DNS, etc.
        long interval;
        try {
            interval = Long.parseLong(strInterval);
        } catch (NumberFormatException e) {
            interval = 5000L;
        }
        rolex.scheduleAtFixedRate(this, 0L, interval);
    }

    /**
     * Get the singleton SynchronizerTask object.
     *
     * @return the SynchronizerTask
     */
    public static synchronized SynchronizerTask getSynchronizer() {
        if (synctask == null) {
            synctask = new SynchronizerTask();
        }
        return synctask;
    }

    /**
     * What is the podState of this POD?.
     *
     * @return one of ACTIVE_POD, STANDBY_POD, UNKNOWN_POD
     */
    public int getPodState() {
        return podState;
    }

    /**
     * Is this the active POD?.
     *
     * @return true if we are active (the master), false otherwise
     */
    public boolean isActive() {
        return podState == ACTIVE_POD;
    }

    /**
     * This method is used to signal that another POD (the active POD) has sent us a /fetchProv request, and that we
     * should re-synchronize with the master.
     */
    public void doFetch() {
        doFetch = true;
    }

    /**
     * Runs once a minute in order to <ol>
     * <li>lookup DNS names,</li>
     * <li>determine the podState of this POD,</li>
     * <li>if this is a standby POD, and the fetch flag is set, perform a fetch of podState from the active POD.</li>
     * <li>if this is a standby POD, check if there are any new log records to be replicated.</li>
     * </ol>.
     */
    @Override
    public void run() {
        try {
            podState = lookupState();
            if (podState == STANDBY_POD) {
                // Only copy provisioning data FROM the active server TO the standby
                if (doFetch || (System.currentTimeMillis() >= nextsynctime)) {
                    syncProvisioningData();
                    logger.info("PROV5013: Sync completed.");
                    nextsynctime = System.currentTimeMillis() + ONE_HOUR;
                }
            } else {
                // Don't do fetches on non-standby PODs
                doFetch = false;
            }

            // Fetch DR logs as needed - server to server
            LogfileLoader lfl = LogfileLoader.getLoader();
            if (lfl.isIdle()) {
                // Only fetch new logs if the loader is waiting for them.
                logger.trace("Checking for logs to replicate...");
                RLEBitSet local = lfl.getBitSet();
                RLEBitSet remote = readRemoteLoglist();
                remote.andNot(local);
                if (!remote.isEmpty()) {
                    logger.debug(" Replicating logs: " + remote);
                    replicateDataRouterLogs(remote);
                }
            }
        } catch (Exception e) {
            logger.warn("PROV0020: Caught exception in SynchronizerTask: " + e);
        }
    }

    private void syncProvisioningData() {
        logger.debug("Initiating a sync...");
        JSONObject jo = readProvisioningJson();
        if (jo != null) {
            doFetch = false;
            syncFeeds(jo.getJSONArray("feeds"));
            syncSubs(jo.getJSONArray("subscriptions"));
            syncGroups(jo.getJSONArray("groups")); //Rally:US708115 - 1610
            syncParams(jo.getJSONObject("parameters"));
            // The following will not be present in a version=1.0 provfeed
            JSONArray ja = jo.optJSONArray("ingress");
            if (ja != null) {
                syncIngressRoutes(ja);
            }
            JSONObject j2 = jo.optJSONObject("egress");
            if (j2 != null) {
                syncEgressRoutes(j2);
            }
            ja = jo.optJSONArray("routing");
            if (ja != null) {
                syncNetworkRoutes(ja);
            }
        }
    }

    /**
     * This method is used to lookup the CNAME that points to the active server.
     * It returns 0 (UNKNOWN_POD), 1(ACTIVE_POD), or (STANDBY_POD) to indicate the podState of this server.
     *
     * @return the current podState
     */
    public int lookupState() {
        int newPodState = UNKNOWN_POD;
        try {
            InetAddress myaddr = InetAddress.getLocalHost();
            if (logger.isTraceEnabled()) {
                logger.trace("My address: " + myaddr);
            }
            String thisPod = myaddr.getHostName();
            Set<String> pods = new TreeSet<>(Arrays.asList(BaseServlet.getPods()));
            if (pods.contains(thisPod)) {
                InetAddress pserver = InetAddress.getByName(BaseServlet.getActiveProvName());
                newPodState = myaddr.equals(pserver) ? ACTIVE_POD : STANDBY_POD;
                if (logger.isDebugEnabled() && System.currentTimeMillis() >= nextMsg) {
                    logger.debug("Active POD = " + pserver + ", Current podState is " + stnames[newPodState]);
                    nextMsg = System.currentTimeMillis() + (5 * 60 * 1000L);
                }
            } else {
                logger.warn("PROV5003: My name (" + thisPod + ") is missing from the list of provisioning servers.");
            }
        } catch (UnknownHostException e) {
            logger.warn("PROV5002: Cannot determine the name of this provisioning server.", e);
        }

        if (newPodState != podState) {
            logger.info(String.format("PROV5001: Server podState changed from %s to %s",
                    stnames[podState], stnames[newPodState]));
        }
        return newPodState;
    }

    /**
     * Synchronize the Feeds in the JSONArray, with the Feeds in the DB.
     */
    private void syncFeeds(JSONArray ja) {
        Collection<Syncable> coll = new ArrayList<>();
        for (int n = 0; n < ja.length(); n++) {
            try {
                Feed feed = new Feed(ja.getJSONObject(n));
                coll.add(feed);
            } catch (Exception e) {
                logger.warn("PROV5004: Invalid object in feed: " + ja.optJSONObject(n), e);
            }
        }
        if (sync(coll, Feed.getAllFeeds())) {
            BaseServlet.provisioningDataChanged();
        }
    }

    /**
     * Synchronize the Subscriptions in the JSONArray, with the Subscriptions in the DB.
     */
    private void syncSubs(JSONArray ja) {
        Collection<Syncable> coll = new ArrayList<>();
        for (int n = 0; n < ja.length(); n++) {
            try {
                //Data Router Subscriber HTTPS Relaxation feature USERSTORYID:US674047.
                JSONObject jsonObject = ja.getJSONObject(n);
                jsonObject.put("sync", "true");
                Subscription sub = new Subscription(jsonObject);
                coll.add(sub);
            } catch (Exception e) {
                logger.warn("PROV5004: Invalid object in subscription: " + ja.optJSONObject(n), e);
            }
        }
        if (sync(coll, Subscription.getAllSubscriptions())) {
            BaseServlet.provisioningDataChanged();
        }
    }

    /**
     * Rally:US708115  - Synchronize the Groups in the JSONArray, with the Groups in the DB.
     */
    private void syncGroups(JSONArray ja) {
        Collection<Syncable> coll = new ArrayList<>();
        for (int n = 0; n < ja.length(); n++) {
            try {
                Group group = new Group(ja.getJSONObject(n));
                coll.add(group);
            } catch (Exception e) {
                logger.warn("PROV5004: Invalid object in group: " + ja.optJSONObject(n), e);
            }
        }
        if (sync(coll, Group.getAllgroups())) {
            BaseServlet.provisioningDataChanged();
        }
    }


    /**
     * Synchronize the Parameters in the JSONObject, with the Parameters in the DB.
     */
    private void syncParams(JSONObject jo) {
        Collection<Syncable> coll = new ArrayList<>();
        for (String k : jo.keySet()) {
            String val = "";
            try {
                val = jo.getString(k);
            } catch (JSONException e) {
                logger.warn("PROV5004: Invalid object in parameters: " + jo.optJSONObject(k), e);
                try {
                    val = "" + jo.getInt(k);
                } catch (JSONException e1) {
                    logger.warn("PROV5004: Invalid object in parameters: " + jo.optInt(k), e1);
                    JSONArray ja = jo.getJSONArray(k);
                    for (int i = 0; i < ja.length(); i++) {
                        if (i > 0) {
                            val += "|";
                        }
                        val += ja.getString(i);
                    }
                }
            }
            coll.add(new Parameters(k, val));
        }
        if (sync(coll, Parameters.getParameterCollection())) {
            BaseServlet.provisioningDataChanged();
            BaseServlet.provisioningParametersChanged();
        }
    }

    private void syncIngressRoutes(JSONArray ja) {
        Collection<Syncable> coll = new ArrayList<>();
        for (int n = 0; n < ja.length(); n++) {
            try {
                IngressRoute in = new IngressRoute(ja.getJSONObject(n));
                coll.add(in);
            } catch (NumberFormatException e) {
                logger.warn("PROV5004: Invalid object in ingress routes: " + ja.optJSONObject(n));
            }
        }
        if (sync(coll, IngressRoute.getAllIngressRoutes())) {
            BaseServlet.provisioningDataChanged();
        }
    }

    private void syncEgressRoutes(JSONObject jo) {
        Collection<Syncable> coll = new ArrayList<>();
        for (String key : jo.keySet()) {
            try {
                int sub = Integer.parseInt(key);
                String node = jo.getString(key);
                EgressRoute er = new EgressRoute(sub, node);
                coll.add(er);
            } catch (NumberFormatException e) {
                logger.warn("PROV5004: Invalid subid in egress routes: " + key, e);
            } catch (IllegalArgumentException e) {
                logger.warn("PROV5004: Invalid node name in egress routes: " + key, e);
            }
        }
        if (sync(coll, EgressRoute.getAllEgressRoutes())) {
            BaseServlet.provisioningDataChanged();
        }
    }

    private void syncNetworkRoutes(JSONArray ja) {
        Collection<Syncable> coll = new ArrayList<>();
        for (int n = 0; n < ja.length(); n++) {
            try {
                NetworkRoute nr = new NetworkRoute(ja.getJSONObject(n));
                coll.add(nr);
            } catch (JSONException e) {
                logger.warn("PROV5004: Invalid object in network routes: " + ja.optJSONObject(n), e);
            }
        }
        if (sync(coll, NetworkRoute.getAllNetworkRoutes())) {
            BaseServlet.provisioningDataChanged();
        }
    }

    private boolean sync(Collection<? extends Syncable> newc, Collection<? extends Syncable> oldc) {
        boolean changes = false;
        try {
            Map<String, Syncable> newmap = getMap(newc);
            Map<String, Syncable> oldmap = getMap(oldc);
            Set<String> union = new TreeSet<>(newmap.keySet());
            union.addAll(oldmap.keySet());
            for (String n : union) {
                Syncable newobj = newmap.get(n);
                Syncable oldobj = oldmap.get(n);
                if (oldobj == null) {
                    try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
                        changes = insertRecord(conn, newobj);
                    }
                } else if (newobj == null) {
                    try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
                        changes = deleteRecord(conn, oldobj);
                    }
                } else if (!newobj.equals(oldobj)) {
                    try (Connection conn = ProvDbUtils.getInstance().getConnection()) {
                        changes = updateRecord(conn, newobj, oldobj);
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("PROV5009: problem during sync, exception: " + e);
        }
        return changes;
    }

    private boolean updateRecord(Connection conn, Syncable newobj, Syncable oldobj) {
        if (logger.isDebugEnabled()) {
            logger.debug("  Updating record: " + newobj);
        }
        boolean changes = newobj.doUpdate(conn);
        checkChangeOwner(newobj, oldobj);

        return changes;
    }

    private boolean deleteRecord(Connection conn, Syncable oldobj) {
        if (logger.isDebugEnabled()) {
            logger.debug("  Deleting record: " + oldobj);
        }
        return oldobj.doDelete(conn);
    }

    private boolean insertRecord(Connection conn, Syncable newobj) {
        if (logger.isDebugEnabled()) {
            logger.debug("  Inserting record: " + newobj);
        }
        return newobj.doInsert(conn);
    }

    private Map<String, Syncable> getMap(Collection<? extends Syncable> coll) {
        Map<String, Syncable> map = new HashMap<>();
        for (Syncable v : coll) {
            map.put(v.getKey(), v);
        }
        return map;
    }

    /**
     * Change owner of FEED/SUBSCRIPTION.
     * Rally US708115 Change Ownership of FEED - 1610
     */
    private void checkChangeOwner(Syncable newobj, Syncable oldobj) {
        if (newobj instanceof Feed) {
            Feed oldfeed = (Feed) oldobj;
            Feed newfeed = (Feed) newobj;

            if (!oldfeed.getPublisher().equals(newfeed.getPublisher())) {
                logger.info("PROV5013 -  Previous publisher: "
                                    + oldfeed.getPublisher() + ": New publisher-" + newfeed.getPublisher());
                oldfeed.setPublisher(newfeed.getPublisher());
                oldfeed.changeOwnerShip();
            }
        } else if (newobj instanceof Subscription) {
            Subscription oldsub = (Subscription) oldobj;
            Subscription newsub = (Subscription) newobj;

            if (!oldsub.getSubscriber().equals(newsub.getSubscriber())) {
                logger.info("PROV5013 -  Previous subscriber: "
                                    + oldsub.getSubscriber() + ": New subscriber-" + newsub.getSubscriber());
                oldsub.setSubscriber(newsub.getSubscriber());
                oldsub.changeOwnerShip();
            }
        }

    }

    /**
     * Issue a GET on the peer POD's /internal/prov/ URL to get a copy of its provisioning data.
     *
     * @return the provisioning data (as a JONObject)
     */
    private synchronized JSONObject readProvisioningJson() {
        String url = URLUtilities.generatePeerProvURL();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = httpclient.execute(get);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpServletResponse.SC_OK) {
                logger.warn("PROV5010: readProvisioningJson failed, bad error code: " + code);
                return null;
            }
            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            if (!ctype.equals(BaseServlet.PROVFULL_CONTENT_TYPE1)
                        && !ctype.equals(BaseServlet.PROVFULL_CONTENT_TYPE2)) {
                logger.warn("PROV5011: readProvisioningJson failed, bad content type: " + ctype);
                return null;
            }
            return new JSONObject(new JSONTokener(entity.getContent()));
        } catch (Exception e) {
            logger.warn("PROV5012: readProvisioningJson failed, exception: " + e);
            return null;
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * Issue a GET on the peer POD's /internal/drlogs/ URL to get an RELBitSet representing the log records available in
     * the remote database.
     *
     * @return the bitset
     */
    public RLEBitSet readRemoteLoglist() {
        RLEBitSet bs = new RLEBitSet();
        String url = URLUtilities.generatePeerLogsURL();

        //Fixing if only one Prov is configured, not to give exception to fill logs, return empty bitset.
        if ("".equals(url)) {
            return bs;
        }
        //End of fix.

        HttpGet get = new HttpGet(url);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            HttpResponse response = httpclient.execute(get);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpServletResponse.SC_OK) {
                logger.warn("PROV5010: readRemoteLoglist failed, bad error code: " + code);
                return bs;
            }
            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            if (!TEXT_CT.equals(ctype)) {
                logger.warn("PROV5011: readRemoteLoglist failed, bad content type: " + ctype);
                return bs;
            }
            InputStream is = entity.getContent();
            int ch;
            while ((ch = is.read()) >= 0) {
                bos.write(ch);
            }
            bs.set(bos.toString());
            is.close();
        } catch (Exception e) {
            logger.warn("PROV5012: readRemoteLoglist failed, exception: " + e);
            return bs;
        } finally {
            get.releaseConnection();
        }
        return bs;
    }

    /**
     * Issue a POST on the peer POD's /internal/drlogs/ URL to fetch log records available in the remote database that
     * we wish to copy to the local database.
     *
     * @param bs the bitset (an RELBitSet) of log records to fetch
     */
    public void replicateDataRouterLogs(RLEBitSet bs) {
        String url = URLUtilities.generatePeerLogsURL();
        HttpPost post = new HttpPost(url);
        try {
            String str = bs.toString();
            HttpEntity body = new ByteArrayEntity(str.getBytes(), ContentType.create(TEXT_CT));
            post.setEntity(body);
            if (logger.isDebugEnabled()) {
                logger.debug("Requesting records: " + str);
            }

            HttpResponse response = httpclient.execute(post);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpServletResponse.SC_OK) {
                logger.warn("PROV5010: replicateDataRouterLogs failed, bad error code: " + code);
                return;
            }
            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            if (!TEXT_CT.equals(ctype)) {
                logger.warn("PROV5011: replicateDataRouterLogs failed, bad content type: " + ctype);
                return;
            }

            String spoolname = "" + System.currentTimeMillis();
            Path tmppath = Paths.get(spooldir, spoolname);
            Path donepath = Paths.get(spooldir, "IN." + spoolname);
            Files.copy(entity.getContent(), Paths.get(spooldir, spoolname), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmppath, donepath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Approximately " + bs.cardinality() + " records replicated.");
        } catch (Exception e) {
            logger.warn("PROV5012: replicateDataRouterLogs failed, exception: " + e);
        } finally {
            post.releaseConnection();
        }
    }
}
