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


package org.onap.dmaap.datarouter.node.config;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.onap.dmaap.datarouter.node.DestInfo;
import org.onap.dmaap.datarouter.node.DestInfoBuilder;
import org.onap.dmaap.datarouter.node.IsFrom;
import org.onap.dmaap.datarouter.node.Target;
import org.onap.dmaap.datarouter.node.utils.NodeUtils;

/**
 * Processed configuration for this node.
 *
 * <p>The NodeConfig represents a processed configuration from the Data Router provisioning server.  Each time
 * configuration data is received from the provisioning server, a new NodeConfig is created and the previous one
 * discarded.
 */
public class NodeConfig {

    private static final String PUBLISHER_NOT_PERMITTED = "Publisher not permitted for this feed";
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(NodeConfig.class);
    private final HashMap<String, String> params = new HashMap<>();
    private final HashMap<String, Feed> feeds = new HashMap<>();
    private final HashMap<String, DestInfo> nodeinfo = new HashMap<>();
    private final HashMap<String, DestInfo> subinfo = new HashMap<>();
    private final HashMap<String, IsFrom> nodes = new HashMap<>();
    private final HashMap<String, ProvSubscription> provSubscriptions = new HashMap<>();
    private final String myname;
    private String myauth;
    private final DestInfo[] alldests;
    private int rrcntr;

    /**
     * Process the raw provisioning data to configure this node.
     *
     * @param pd The parsed provisioning data
     * @param myname My name as seen by external systems
     * @param spooldir The directory where temporary files live
     * @param port The port number for URLs
     * @param nodeauthkey The keying string used to generate node authentication credentials
     */
    public NodeConfig(ProvData pd, String myname, String spooldir, int port, String nodeauthkey) {
        this.myname = myname;
        for (ProvParam p : pd.getParams()) {
            params.put(p.getName(), p.getValue());
        }
        ArrayList<DestInfo> destInfos = addDestInfoToNodeConfig(pd, myname, spooldir, port, nodeauthkey);
        PathFinder pf = new PathFinder(myname, nodeinfo.keySet().toArray(new String[0]), pd.getHops());
        HashMap<String, ArrayList<Redirection>> rdtab = addSubRedirInfoToNodeConfig(pd);
        HashMap<String, HashMap<String, String>> pfutab = addFeedUsersToNodeConfig(pd);
        HashMap<String, String> egrtab = addEgressRoutesToNodeConfig(pd, myname);
        HashMap<String, ArrayList<SubnetMatcher>> pfstab = addFeedSubnetToNodeConfig(pd);
        HashSet<String> allfeeds = addFeedsToNodeConfig(pd);
        HashMap<String, StringBuilder> feedTargets = addSubsToNodeConfig(pd, spooldir, destInfos, pf, egrtab, allfeeds);
        alldests = destInfos.toArray(new DestInfo[0]);
        addFeedTargetsToNodeConfig(pd, rdtab, pfutab, pfstab, feedTargets);
    }

    @NotNull
    private ArrayList<DestInfo> addDestInfoToNodeConfig(ProvData pd, String myname, String spooldir, int port,
            String nodeauthkey) {
        ArrayList<DestInfo> destInfos = new ArrayList<>();
        myauth = NodeUtils.getNodeAuthHdr(myname, nodeauthkey);
        for (ProvNode pn : pd.getNodes()) {
            String commonName = pn.getCName();
            if (nodeinfo.get(commonName) != null) {
                continue;
            }
            DestInfo di = new DestInfoBuilder().setName("n:" + commonName).setSpool(spooldir + "/n/" + commonName)
                    .setSubid(null)
                    .setLogdata("n2n-" + commonName).setUrl("https://" + commonName + ":" + port + "/internal/publish")
                    .setAuthuser(commonName).setAuthentication(myauth).setMetaonly(false).setUse100(true)
                    .setPrivilegedSubscriber(false).setFollowRedirects(false).setDecompress(false).createDestInfo();
            (new File(di.getSpool())).mkdirs();
            String auth = NodeUtils.getNodeAuthHdr(commonName, nodeauthkey);
            destInfos.add(di);
            nodeinfo.put(commonName, di);
            nodes.put(auth, new IsFrom(commonName));
        }
        return destInfos;
    }

    @NotNull
    private HashMap<String, ArrayList<Redirection>> addSubRedirInfoToNodeConfig(ProvData pd) {
        HashMap<String, ArrayList<Redirection>> rdtab = new HashMap<>();
        for (ProvForceIngress pfi : pd.getForceIngress()) {
            ArrayList<Redirection> redirections = rdtab.get(pfi.getFeedId());
            if (redirections == null) {
                redirections = new ArrayList<>();
                rdtab.put(pfi.getFeedId(), redirections);
            }
            Redirection redirection = new Redirection();
            if (pfi.getSubnet() != null) {
                redirection.snm = new SubnetMatcher(pfi.getSubnet());
            }
            redirection.user = pfi.getUser();
            redirection.nodes = pfi.getNodes();
            redirections.add(redirection);
        }
        return rdtab;
    }

    @NotNull
    private HashMap<String, HashMap<String, String>> addFeedUsersToNodeConfig(ProvData pd) {
        HashMap<String, HashMap<String, String>> pfutab = new HashMap<>();
        for (ProvFeedUser pfu : pd.getFeedUsers()) {
            HashMap<String, String> userInfo = pfutab.get(pfu.getFeedId());
            if (userInfo == null) {
                userInfo = new HashMap<>();
                pfutab.put(pfu.getFeedId(), userInfo);
            }
            userInfo.put(pfu.getCredentials(), pfu.getUser());
        }
        return pfutab;
    }

    @NotNull
    private HashMap<String, String> addEgressRoutesToNodeConfig(ProvData pd, String myname) {
        HashMap<String, String> egrtab = new HashMap<>();
        for (ProvForceEgress pfe : pd.getForceEgress()) {
            if (pfe.getNode().equals(myname) || nodeinfo.get(pfe.getNode()) == null) {
                continue;
            }
            egrtab.put(pfe.getSubId(), pfe.getNode());
        }
        return egrtab;
    }

    @NotNull
    private HashMap<String, ArrayList<SubnetMatcher>> addFeedSubnetToNodeConfig(ProvData pd) {
        HashMap<String, ArrayList<SubnetMatcher>> pfstab = new HashMap<>();
        for (ProvFeedSubnet pfs : pd.getFeedSubnets()) {
            ArrayList<SubnetMatcher> subnetMatchers = pfstab.get(pfs.getFeedId());
            if (subnetMatchers == null) {
                subnetMatchers = new ArrayList<>();
                pfstab.put(pfs.getFeedId(), subnetMatchers);
            }
            subnetMatchers.add(new SubnetMatcher(pfs.getCidr()));
        }
        return pfstab;
    }

    @NotNull
    private HashSet<String> addFeedsToNodeConfig(ProvData pd) {
        HashSet<String> allfeeds = new HashSet<>();
        for (ProvFeed pfx : pd.getFeeds()) {
            if (pfx.getStatus() == null) {
                allfeeds.add(pfx.getId());
            }
        }
        return allfeeds;
    }

    @NotNull
    private HashMap<String, StringBuilder> addSubsToNodeConfig(ProvData pd, String spooldir,
            ArrayList<DestInfo> destInfos, PathFinder pf, HashMap<String, String> egrtab, HashSet<String> allfeeds) {
        HashMap<String, StringBuilder> feedTargets = new HashMap<>();
        for (ProvSubscription provSubscription : pd.getSubscriptions()) {
            String subId = provSubscription.getSubId();
            String feedId = provSubscription.getFeedId();
            if (isFeedOrSubKnown(allfeeds, subId, feedId)) {
                continue;
            }
            int sididx = 999;
            try {
                sididx = Integer.parseInt(subId);
                sididx -= sididx % 100;
            } catch (Exception e) {
                logger.error("NODE0517 Exception NodeConfig: " + e);
            }
            String subscriptionDirectory = sididx + "/" + subId;
            DestInfo destinationInfo = new DestInfo("s:" + subId,
                    spooldir + "/s/" + subscriptionDirectory, provSubscription);
            (new File(destinationInfo.getSpool())).mkdirs();
            destInfos.add(destinationInfo);
            provSubscriptions.put(subId, provSubscription);
            subinfo.put(subId, destinationInfo);
            String egr = egrtab.get(subId);
            if (egr != null) {
                subId = pf.getPath(egr) + subId;
            }
            StringBuilder sb = feedTargets.get(feedId);
            if (sb == null) {
                sb = new StringBuilder();
                feedTargets.put(feedId, sb);
            }
            sb.append(' ').append(subId);
        }
        return feedTargets;
    }

    private void addFeedTargetsToNodeConfig(ProvData pd, HashMap<String, ArrayList<Redirection>> rdtab,
            HashMap<String, HashMap<String, String>> pfutab, HashMap<String, ArrayList<SubnetMatcher>> pfstab,
            HashMap<String, StringBuilder> feedTargets) {
        for (ProvFeed pfx : pd.getFeeds()) {
            String fid = pfx.getId();
            Feed feed = feeds.get(fid);
            if (feed != null) {
                continue;
            }
            feed = new Feed();
            feeds.put(fid, feed);
            feed.createdDate = pfx.getCreatedDate();
            feed.loginfo = pfx.getLogData();
            feed.status = pfx.getStatus();
            ArrayList<SubnetMatcher> v1 = pfstab.get(fid);
            if (v1 == null) {
                feed.subnets = new SubnetMatcher[0];
            } else {
                feed.subnets = v1.toArray(new SubnetMatcher[0]);
            }
            HashMap<String, String> h1 = pfutab.get(fid);
            if (h1 == null) {
                h1 = new HashMap();
            }
            feed.authusers = h1;
            ArrayList<Redirection> v2 = rdtab.get(fid);
            if (v2 == null) {
                feed.redirections = new Redirection[0];
            } else {
                feed.redirections = v2.toArray(new Redirection[0]);
            }
            StringBuilder sb = feedTargets.get(fid);
            if (sb == null) {
                feed.targets = new Target[0];
            } else {
                feed.targets = parseRouting(sb.toString());
            }
        }
    }

    /**
     * Parse a target string into an array of targets.
     *
     * @param routing Target string
     * @return Array of targets.
     */
    public Target[] parseRouting(String routing) {
        routing = routing.trim();
        if ("".equals(routing)) {
            return (new Target[0]);
        }
        String[] routingTable = routing.split("\\s+");
        HashMap<String, Target> tmap = new HashMap<>();
        HashSet<String> subset = new HashSet<>();
        ArrayList<Target> targets = new ArrayList<>();
        for (int i = 0; i < routingTable.length; i++) {
            String target = routingTable[i];
            int index = target.indexOf('/');
            if (index == -1) {
                addTarget(subset, targets, target);
            } else {
                addTargetWithRouting(tmap, targets, target, index);
            }
        }
        return (targets.toArray(new Target[0]));
    }

    /**
     * Check whether this is a valid node-to-node transfer.
     *
     * @param credentials Credentials offered by the supposed node
     * @param ip IP address the request came from
     */
    public boolean isAnotherNode(String credentials, String ip) {
        IsFrom node = nodes.get(credentials);
        return (node != null && node.isFrom(ip));
    }

    /**
     * Check whether publication is allowed.
     *
     * @param feedid The ID of the feed being requested.
     * @param credentials The offered credentials
     * @param ip The requesting IP address
     */
    public String isPublishPermitted(String feedid, String credentials, String ip) {
        Feed feed = feeds.get(feedid);
        String nf = "Feed does not exist";
        if (feed != null) {
            nf = feed.status;
        }
        if (nf != null) {
            return (nf);
        }
        String user = feed.authusers.get(credentials);
        if (user == null) {
            return (PUBLISHER_NOT_PERMITTED);
        }
        if (feed.subnets.length == 0) {
            return (null);
        }
        byte[] addr = NodeUtils.getInetAddress(ip);
        for (SubnetMatcher snm : feed.subnets) {
            if (snm.matches(addr)) {
                return (null);
            }
        }
        return (PUBLISHER_NOT_PERMITTED);
    }

    /**
     * Check whether delete file is allowed.
     *
     * @param subId The ID of the subscription being requested.
     */
    public boolean isDeletePermitted(String subId) {
        ProvSubscription provSubscription = provSubscriptions.get(subId);
        return provSubscription.isPrivilegedSubscriber();
    }

    /**
     * Get authenticated user.
     */
    public String getAuthUser(String feedid, String credentials) {
        return (feeds.get(feedid).authusers.get(credentials));
    }

    /**
     * Check if the request should be redirected to a different ingress node.
     */
    public String getIngressNode(String feedid, String user, String ip) {
        Feed feed = feeds.get(feedid);
        if (feed.redirections.length == 0) {
            return (null);
        }
        byte[] addr = NodeUtils.getInetAddress(ip);
        for (Redirection r : feed.redirections) {
            if ((r.user != null && !user.equals(r.user)) || (r.snm != null && !r.snm.matches(addr))) {
                continue;
            }
            for (String n : r.nodes) {
                if (myname.equals(n)) {
                    return (null);
                }
            }
            if (r.nodes.length == 0) {
                return (null);
            }
            return (r.nodes[rrcntr++ % r.nodes.length]);
        }
        return (null);
    }

    /**
     * Get a provisioned configuration parameter.
     */
    public String getProvParam(String name) {
        return (params.get(name));
    }

    /**
     * Get all the DestInfos.
     */
    public DestInfo[] getAllDests() {
        return (alldests);
    }

    /**
     * Get the targets for a feed.
     *
     * @param feedid The feed ID
     * @return The targets this feed should be delivered to
     */
    public Target[] getTargets(String feedid) {
        if (feedid == null) {
            return (new Target[0]);
        }
        Feed feed = feeds.get(feedid);
        if (feed == null) {
            return (new Target[0]);
        }
        return (feed.targets);
    }

    /**
     * Get the creation date for a feed.
     *
     * @param feedid The feed ID
     * @return the timestamp of creation date of feed id passed
     */
    public String getCreatedDate(String feedid) {
        Feed feed = feeds.get(feedid);
        return (feed.createdDate);
    }

    /**
     * Get the feed ID for a subscription.
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid) {
        DestInfo di = subinfo.get(subid);
        if (di == null) {
            return (null);
        }
        return (di.getLogData());
    }

    /**
     * Get the spool directory for a subscription.
     *
     * @param subid The subscription ID
     * @return The spool directory
     */
    public String getSpoolDir(String subid) {
        DestInfo di = subinfo.get(subid);
        if (di == null) {
            return (null);
        }
        return (di.getSpool());
    }

    /**
     * Get the Authorization value this node uses.
     *
     * @return The Authorization header value for this node
     */
    public String getMyAuth() {
        return (myauth);
    }

    private boolean isFeedOrSubKnown(HashSet<String> allfeeds, String subId, String feedId) {
        return !allfeeds.contains(feedId) || subinfo.get(subId) != null;
    }

    private void addTargetWithRouting(HashMap<String, Target> tmap, ArrayList<Target> targets, String target,
            int index) {
        String node = target.substring(0, index);
        String rtg = target.substring(index + 1);
        DestInfo di = nodeinfo.get(node);
        if (di == null) {
            targets.add(new Target(null, target));
        } else {
            Target tt = tmap.get(node);
            if (tt == null) {
                tt = new Target(di, rtg);
                tmap.put(node, tt);
                targets.add(tt);
            } else {
                tt.addRouting(rtg);
            }
        }
    }

    private void addTarget(HashSet<String> subset, ArrayList<Target> targets, String target) {
        DestInfo destInfo = subinfo.get(target);
        if (destInfo == null) {
            targets.add(new Target(null, target));
        } else {
            if (!subset.contains(target)) {
                subset.add(target);
                targets.add(new Target(destInfo, null));
            }
        }
    }

    /**
     * Raw configuration entry for a data router node.
     */
    public static class ProvNode {

        private String cname;

        /**
         * Construct a node configuration entry.
         *
         * @param cname The cname of the node.
         */
        public ProvNode(String cname) {
            this.cname = cname;
        }

        /**
         * Get the cname of the node.
         */
        public String getCName() {
            return (cname);
        }
    }

    /**
     * Raw configuration entry for a provisioning parameter.
     */
    public static class ProvParam {

        private String name;
        private String value;

        /**
         * Construct a provisioning parameter configuration entry.
         *
         * @param name The name of the parameter.
         * @param value The value of the parameter.
         */
        public ProvParam(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Get the name of the parameter.
         */
        public String getName() {
            return (name);
        }

        /**
         * Get the value of the parameter.
         */
        public String getValue() {
            return (value);
        }
    }

    /**
     * Raw configuration entry for a data feed.
     */
    public static class ProvFeed {

        private final String id;
        private final String logdata;
        private final String status;
        private final String createdDate;

        /**
         * Construct a feed configuration entry.
         *
         * @param id The feed ID of the entry.
         * @param logdata String for log entries about the entry.
         * @param status The reason why this feed cannot be used (Feed has been deleted, Feed has been suspended) or
         *      null if it is valid.
         */
        public ProvFeed(String id, String logdata, String status, String createdDate) {
            this.id = id;
            this.logdata = logdata;
            this.status = status;
            this.createdDate = createdDate;
        }

        /**
         * Get the created date of the data feed.
         */
        public String getCreatedDate() {
            return (createdDate);
        }

        /**
         * Get the feed id of the data feed.
         */
        public String getId() {
            return (id);
        }

        /**
         * Get the log data of the data feed.
         */
        public String getLogData() {
            return (logdata);
        }

        /**
         * Get the status of the data feed.
         */
        public String getStatus() {
            return (status);
        }
    }

    /**
     * Raw configuration entry for a feed user.
     */
    public static class ProvFeedUser {

        private final String feedid;
        private final String user;
        private final String credentials;

        /**
         * Construct a feed user configuration entry.
         *
         * @param feedid The feed id.
         * @param user The user that will publish to the feed.
         * @param credentials The Authorization header the user will use to publish.
         */
        public ProvFeedUser(String feedid, String user, String credentials) {
            this.feedid = feedid;
            this.user = user;
            this.credentials = credentials;
        }

        /**
         * Get the feed id of the feed user.
         */
        public String getFeedId() {
            return (feedid);
        }

        /**
         * Get the user for the feed user.
         */
        public String getUser() {
            return (user);
        }

        /**
         * Get the credentials for the feed user.
         */
        public String getCredentials() {
            return (credentials);
        }
    }

    /**
     * Raw configuration entry for a feed subnet.
     */
    public static class ProvFeedSubnet {

        private final String feedid;
        private final String cidr;

        /**
         * Construct a feed subnet configuration entry.
         *
         * @param feedid The feed ID
         * @param cidr The CIDR allowed to publish to the feed.
         */
        public ProvFeedSubnet(String feedid, String cidr) {
            this.feedid = feedid;
            this.cidr = cidr;
        }

        /**
         * Get the feed id of the feed subnet.
         */
        public String getFeedId() {
            return (feedid);
        }

        /**
         * Get the CIDR of the feed subnet.
         */
        public String getCidr() {
            return (cidr);
        }
    }

    /**
     * Raw configuration entry for a subscription.
     */
    public static class ProvSubscription {

        private final String subid;
        private final String feedid;
        private final String url;
        private final String authuser;
        private final String credentials;
        private final boolean metaonly;
        private final boolean use100;
        private final boolean privilegedSubscriber;
        private final boolean followRedirect;
        private final boolean decompress;

        /**
         * Construct a subscription configuration entry.
         *
         * @param subid The subscription ID
         * @param feedid The feed ID
         * @param url The base delivery URL (not including the fileid)
         * @param authuser The user in the credentials used to deliver
         * @param credentials The credentials used to authenticate to the delivery URL exactly as they go in the
         *      Authorization header.
         * @param metaonly Is this a meta data only subscription?
         * @param use100 Should we send Expect: 100-continue?
         * @param privilegedSubscriber Can we wait to receive a delete file call before deleting file
         * @param followRedirect Is follow redirect of destination enabled?
         * @param decompress To see if they want their information compressed or decompressed
         */
        public ProvSubscription(String subid, String feedid, String url, String authuser, String credentials,
                boolean metaonly, boolean use100, boolean privilegedSubscriber, boolean followRedirect,
                boolean decompress) {
            this.subid = subid;
            this.feedid = feedid;
            this.url = url;
            this.authuser = authuser;
            this.credentials = credentials;
            this.metaonly = metaonly;
            this.use100 = use100;
            this.privilegedSubscriber = privilegedSubscriber;
            this.followRedirect = followRedirect;
            this.decompress = decompress;
        }

        /**
         * Get the subscription ID.
         */
        public String getSubId() {
            return (subid);
        }

        /**
         * Get the feed ID.
         */
        public String getFeedId() {
            return (feedid);
        }

        /**
         * Get the delivery URL.
         */
        public String getURL() {
            return (url);
        }

        /**
         * Get the user.
         */
        public String getAuthUser() {
            return (authuser);
        }

        /**
         * Get the delivery credentials.
         */
        public String getCredentials() {
            return (credentials);
        }

        /**
         * Is this a meta data only subscription.
         */
        public boolean isMetaDataOnly() {
            return (metaonly);
        }

        /**
         * Should we send Expect: 100-continue.
         */
        public boolean isUsing100() {
            return (use100);
        }

        /**
         * Can we wait to receive a delete file call before deleting file.
         */
        public boolean isPrivilegedSubscriber() {
            return (privilegedSubscriber);
        }

        /**
         * Should I decompress the file before sending it on.
         */
        public boolean isDecompress() {
            return (decompress);
        }

        /**
         * New field is added - FOLLOW_REDIRECTS feature iTrack:DATARTR-17 - 1706 Get the followRedirect of this
         * destination.
         */
        public boolean getFollowRedirect() {
            return (followRedirect);
        }
    }

    /**
     * Raw configuration entry for controlled ingress to the data router node.
     */
    public static class ProvForceIngress {

        private final String feedid;
        private final String subnet;
        private final String user;
        private final String[] nodes;

        /**
         * Construct a forced ingress configuration entry.
         *
         * @param feedid The feed ID that this entry applies to
         * @param subnet The CIDR for which publisher IP addresses this entry applies to or "" if it applies to all
         *      publisher IP addresses
         * @param user The publishing user this entry applies to or "" if it applies to all publishing users.
         * @param nodes The array of FQDNs of the data router nodes to redirect publication attempts to.
         */
        public ProvForceIngress(String feedid, String subnet, String user, String[] nodes) {
            this.feedid = feedid;
            this.subnet = subnet;
            this.user = user;
            //Sonar fix
            if (nodes == null) {
                this.nodes = new String[0];
            } else {
                this.nodes = Arrays.copyOf(nodes, nodes.length);
            }
        }

        /**
         * Get the feed ID.
         */
        public String getFeedId() {
            return (feedid);
        }

        /**
         * Get the subnet.
         */
        public String getSubnet() {
            return (subnet);
        }

        /**
         * Get the user.
         */
        public String getUser() {
            return (user);
        }

        /**
         * Get the node.
         */
        public String[] getNodes() {
            return (nodes);
        }
    }

    /**
     * Raw configuration entry for controlled egress from the data router.
     */
    public static class ProvForceEgress {

        private final String subid;
        private final String node;

        /**
         * Construct a forced egress configuration entry.
         *
         * @param subid The subscription ID the subscription with forced egress
         * @param node The node handling deliveries for this subscription
         */
        public ProvForceEgress(String subid, String node) {
            this.subid = subid;
            this.node = node;
        }

        /**
         * Get the subscription ID.
         */
        public String getSubId() {
            return (subid);
        }

        /**
         * Get the node.
         */
        public String getNode() {
            return (node);
        }
    }

    /**
     * Raw configuration entry for routing within the data router network.
     */
    public static class ProvHop {

        private final String from;
        private final String to;
        private final String via;

        /**
         * Construct a hop entry.
         *
         * @param from The FQDN of the node with the data to be delivered
         * @param to The FQDN of the node that will deliver to the subscriber
         * @param via The FQDN of the node where the from node should send the data
         */
        public ProvHop(String from, String to, String via) {
            this.from = from;
            this.to = to;
            this.via = via;
        }

        /**
         * A human readable description of this entry.
         */
        public String toString() {
            return ("Hop " + from + "->" + to + " via " + via);
        }

        /**
         * Get the from node.
         */
        public String getFrom() {
            return (from);
        }

        /**
         * Get the to node.
         */
        public String getTo() {
            return (to);
        }

        /**
         * Get the next intermediate node.
         */
        public String getVia() {
            return (via);
        }
    }

    private static class Redirection {

        SubnetMatcher snm;
        String user;
        String[] nodes;
    }

    private static class Feed {

        String loginfo;
        String status;
        SubnetMatcher[] subnets;
        HashMap<String, String> authusers = new HashMap<>();
        Redirection[] redirections;
        Target[] targets;
        String createdDate;
    }
}
