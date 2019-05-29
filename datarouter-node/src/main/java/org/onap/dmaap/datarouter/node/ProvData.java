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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvFeed;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvFeedSubnet;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvFeedUser;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvForceEgress;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvForceIngress;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvHop;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvNode;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvParam;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvSubscription;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;

/**
 * Parser for provisioning data from the provisioning server.
 * <p>
 * The ProvData class uses a Reader for the text configuration from the provisioning server to construct arrays of raw
 * configuration entries.
 */
public class ProvData {

    private static final String FEED_ID = "feedid";
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(ProvData.class);
    private NodeConfig.ProvNode[] provNodes;
    private NodeConfig.ProvParam[] provParams;
    private NodeConfig.ProvFeed[] provFeeds;
    private NodeConfig.ProvFeedUser[] provFeedUsers;
    private NodeConfig.ProvFeedSubnet[] provFeedSubnets;
    private NodeConfig.ProvSubscription[] provSubscriptions;
    private NodeConfig.ProvForceIngress[] provForceIngresses;
    private NodeConfig.ProvForceEgress[] provForceEgresses;
    private NodeConfig.ProvHop[] provHops;

    /**
     * Construct raw provisioing data entries from the text (JSON) provisioning document received from the provisioning
     * server
     *
     * @param r The reader for the JSON text.
     */
    public ProvData(Reader r) throws IOException {
        ArrayList<ProvNode> provNodes1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvParam> provParams1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeed> provFeeds1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeedUser> provFeedUsers1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeedSubnet> provFeedSubnets1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvSubscription> provSubscriptions1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvForceIngress> provForceIngresses1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvForceEgress> provForceEgresses1 = new ArrayList<>();
        ArrayList<NodeConfig.ProvHop> provHops1 = new ArrayList<>();
        try {
            JSONTokener jtx = new JSONTokener(r);
            JSONObject jcfg = new JSONObject(jtx);
            char c = jtx.nextClean();
            if (c != '\0') {
                throw new JSONException("Spurious characters following configuration");
            }
            r.close();
            addJSONFeeds(provFeeds1, provFeedUsers1, provFeedSubnets1, jcfg);
            addJSONSubs(provSubscriptions1, jcfg);
            addJSONParams(provNodes1, provParams1, jcfg);
            addJSONRoutingInformation(provForceIngresses1, provForceEgresses1, provHops1, jcfg);
        } catch (JSONException jse) {
            NodeUtils.setIpAndFqdnForEelf("ProvData");
            eelfLogger.error(EelfMsgs.MESSAGE_PARSING_ERROR, jse.toString());
            eelfLogger
                    .error("NODE0201 Error parsing configuration data from provisioning server " + jse.toString(), jse);
            throw new IOException(jse.toString(), jse);
        }
        provNodes = provNodes1.toArray(new NodeConfig.ProvNode[provNodes1.size()]);
        provParams = provParams1.toArray(new NodeConfig.ProvParam[provParams1.size()]);
        provFeeds = provFeeds1.toArray(new NodeConfig.ProvFeed[provFeeds1.size()]);
        provFeedUsers = provFeedUsers1.toArray(new NodeConfig.ProvFeedUser[provFeedUsers1.size()]);
        provFeedSubnets = provFeedSubnets1.toArray(new NodeConfig.ProvFeedSubnet[provFeedSubnets1.size()]);
        provSubscriptions = provSubscriptions1.toArray(new NodeConfig.ProvSubscription[provSubscriptions1.size()]);
        provForceIngresses = provForceIngresses1.toArray(new NodeConfig.ProvForceIngress[provForceIngresses1.size()]);
        provForceEgresses = provForceEgresses1.toArray(new NodeConfig.ProvForceEgress[provForceEgresses1.size()]);
        provHops = provHops1.toArray(new NodeConfig.ProvHop[provHops1.size()]);
    }

    private static String[] gvasa(JSONObject o, String key) {
        return (gvasa(o.opt(key)));
    }

    private static String[] gvasa(Object o) {
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            ArrayList<String> v = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                String s = gvas(a, i);
                if (s != null) {
                    v.add(s);
                }
            }
            return (v.toArray(new String[v.size()]));
        } else {
            String s = gvas(o);
            if (s == null) {
                return (new String[0]);
            } else {
                return (new String[]{s});
            }
        }
    }

    private static String gvas(JSONArray a, int index) {
        return (gvas(a.get(index)));
    }

    private static String gvas(JSONObject o, String key) {
        return (gvas(o.opt(key)));
    }

    private static String gvas(Object o) {
        if (o instanceof Boolean || o instanceof Number || o instanceof String) {
            return (o.toString());
        }
        return (null);
    }

    /**
     * Get the raw node configuration entries
     */
    public NodeConfig.ProvNode[] getNodes() {
        return (provNodes);
    }

    /**
     * Get the raw parameter configuration entries
     */
    public NodeConfig.ProvParam[] getParams() {
        return (provParams);
    }

    /**
     * Ge the raw feed configuration entries
     */
    public NodeConfig.ProvFeed[] getFeeds() {
        return (provFeeds);
    }

    /**
     * Get the raw feed user configuration entries
     */
    public NodeConfig.ProvFeedUser[] getFeedUsers() {
        return (provFeedUsers);
    }

    /**
     * Get the raw feed subnet configuration entries
     */
    public NodeConfig.ProvFeedSubnet[] getFeedSubnets() {
        return (provFeedSubnets);
    }

    /**
     * Get the raw subscription entries
     */
    public NodeConfig.ProvSubscription[] getSubscriptions() {
        return (provSubscriptions);
    }

    /**
     * Get the raw forced ingress entries
     */
    public NodeConfig.ProvForceIngress[] getForceIngress() {
        return (provForceIngresses);
    }

    /**
     * Get the raw forced egress entries
     */
    public NodeConfig.ProvForceEgress[] getForceEgress() {
        return (provForceEgresses);
    }

    /**
     * Get the raw next hop entries
     */
    public NodeConfig.ProvHop[] getHops() {
        return (provHops);
    }

    @Nullable
    private String getFeedStatus(JSONObject jfeed) {
        String stat = null;
        if (jfeed.optBoolean("suspend", false)) {
            stat = "Feed is suspended";
        }
        if (jfeed.optBoolean("deleted", false)) {
            stat = "Feed is deleted";
        }
        return stat;
    }

    private void addJSONFeeds(ArrayList<ProvFeed> provFeeds1, ArrayList<ProvFeedUser> provFeedUsers1,
            ArrayList<ProvFeedSubnet> provFeedSubnets1,
            JSONObject jsonConfig) {
        JSONArray jfeeds = jsonConfig.optJSONArray("feeds");
        if (jfeeds != null) {
            for (int fx = 0; fx < jfeeds.length(); fx++) {
                addJSONFeed(provFeeds1, provFeedUsers1, provFeedSubnets1, jfeeds, fx);
            }
        }
    }

    private void addJSONFeed(ArrayList<ProvFeed> provFeeds1, ArrayList<ProvFeedUser> provFeedUsers1,
            ArrayList<ProvFeedSubnet> provFeedSubnets1, JSONArray jfeeds, int feedIndex) {
        JSONObject jfeed = jfeeds.getJSONObject(feedIndex);
        String stat = getFeedStatus(jfeed);
        String fid = gvas(jfeed, FEED_ID);
        String fname = gvas(jfeed, "name");
        String fver = gvas(jfeed, "version");
        String createdDate = gvas(jfeed, "created_date");
        /*
         * START - AAF changes
         * TDP EPIC US# 307413
         * Passing aafInstance to ProvFeed from feeds json passed by prov to identify legacy/AAF feeds
         */
        String aafInstance = gvas(jfeed, "aaf_instance");
        provFeeds1.add(new ProvFeed(fid, fname + "//" + fver, stat, createdDate, aafInstance));
        /*
         * END - AAF changes
         */
        addJSONFeedAuthArrays(provFeedUsers1, provFeedSubnets1, jfeed, fid);
    }

    private void addJSONFeedAuthArrays(ArrayList<ProvFeedUser> provFeedUsers1,
            ArrayList<ProvFeedSubnet> provFeedSubnets1, JSONObject jfeed, String fid) {
        JSONObject jauth = jfeed.optJSONObject("authorization");
        if (jauth == null) {
            return;
        }
        JSONArray jeids = jauth.optJSONArray("endpoint_ids");
        if (jeids != null) {
            for (int ux = 0; ux < jeids.length(); ux++) {
                JSONObject ju = jeids.getJSONObject(ux);
                String login = gvas(ju, "id");
                String password = gvas(ju, "password");
                provFeedUsers1.add(new ProvFeedUser(fid, login, NodeUtils.getAuthHdr(login, password)));
            }
        }
        JSONArray jeips = jauth.optJSONArray("endpoint_addrs");
        if (jeips != null) {
            for (int ix = 0; ix < jeips.length(); ix++) {
                String sn = gvas(jeips, ix);
                provFeedSubnets1.add(new ProvFeedSubnet(fid, sn));
            }
        }
    }

    private void addJSONSubs(ArrayList<ProvSubscription> provSubscriptions1, JSONObject jsonConfig) {
        JSONArray jsubs = jsonConfig.optJSONArray("subscriptions");
        if (jsubs != null) {
            for (int sx = 0; sx < jsubs.length(); sx++) {
                addJSONSub(provSubscriptions1, jsubs, sx);
            }
        }
    }

    private void addJSONSub(ArrayList<ProvSubscription> provSubscriptions1, JSONArray jsubs, int sx) {
        JSONObject jsub = jsubs.getJSONObject(sx);
        if (jsub.optBoolean("suspend", false)) {
            return;
        }
        String sid = gvas(jsub, "subid");
        String fid = gvas(jsub, FEED_ID);
        JSONObject jdel = jsub.getJSONObject("delivery");
        String delurl = gvas(jdel, "url");
        String id = gvas(jdel, "user");
        String password = gvas(jdel, "password");
        boolean monly = jsub.getBoolean("metadataOnly");
        boolean use100 = jdel.getBoolean("use100");
        boolean privilegedSubscriber = jsub.getBoolean("privilegedSubscriber");
        boolean decompress = jsub.getBoolean("decompress");
        boolean followRedirect = jsub.getBoolean("follow_redirect");
        provSubscriptions1
                .add(new ProvSubscription(sid, fid, delurl, id, NodeUtils.getAuthHdr(id, password), monly, use100,
                        privilegedSubscriber, followRedirect, decompress));
    }

    private void addJSONParams(ArrayList<ProvNode> provNodes1, ArrayList<ProvParam> provParams1,
            JSONObject jsonconfig) {
        JSONObject jparams = jsonconfig.optJSONObject("parameters");
        if (jparams != null) {
            for (String pname : JSONObject.getNames(jparams)) {
                addJSONParam(provParams1, jparams, pname);
            }
            addJSONNodesToParams(provNodes1, jparams);
        }
    }

    private void addJSONParam(ArrayList<ProvParam> provParams1, JSONObject jparams, String pname) {
        String pvalue = gvas(jparams, pname);
        if (pvalue != null) {
            provParams1.add(new ProvParam(pname, pvalue));
        }
    }

    private void addJSONNodesToParams(ArrayList<ProvNode> provNodes1, JSONObject jparams) {
        String sfx = gvas(jparams, "PROV_DOMAIN");
        JSONArray jnodes = jparams.optJSONArray("NODES");
        if (jnodes != null) {
            for (int nx = 0; nx < jnodes.length(); nx++) {
                String nn = gvas(jnodes, nx);
                if (nn == null) {
                    continue;
                }
                if (nn.indexOf('.') == -1) {
                    nn = nn + "." + sfx;
                }
                provNodes1.add(new ProvNode(nn));
            }
        }
    }

    private void addJSONRoutingInformation(ArrayList<ProvForceIngress> provForceIngresses1,
            ArrayList<ProvForceEgress> provForceEgresses1, ArrayList<ProvHop> provHops1, JSONObject jsonConfig) {
        JSONArray jingresses = jsonConfig.optJSONArray("ingress");
        if (jingresses != null) {
            for (int fx = 0; fx < jingresses.length(); fx++) {
                addJSONIngressRoute(provForceIngresses1, jingresses, fx);
            }
        }
        JSONObject jegresses = jsonConfig.optJSONObject("egress");
        if (jegresses != null && JSONObject.getNames(jegresses) != null) {
            for (String esid : JSONObject.getNames(jegresses)) {
                addJSONEgressRoute(provForceEgresses1, jegresses, esid);
            }
        }
        JSONArray jhops = jsonConfig.optJSONArray("routing");
        if (jhops != null) {
            for (int fx = 0; fx < jhops.length(); fx++) {
                addJSONRoutes(provHops1, jhops, fx);
            }
        }
    }

    private void addJSONIngressRoute(ArrayList<ProvForceIngress> provForceIngresses1, JSONArray jingresses, int fx) {
        JSONObject jingress = jingresses.getJSONObject(fx);
        String fid = gvas(jingress, FEED_ID);
        String subnet = gvas(jingress, "subnet");
        String user = gvas(jingress, "user");
        String[] nodes = gvasa(jingress, "node");
        if (fid == null || "".equals(fid)) {
            return;
        }
        if ("".equals(subnet)) {
            subnet = null;
        }
        if ("".equals(user)) {
            user = null;
        }
        provForceIngresses1.add(new ProvForceIngress(fid, subnet, user, nodes));
    }

    private void addJSONEgressRoute(ArrayList<ProvForceEgress> provForceEgresses1, JSONObject jegresses, String esid) {
        String enode = gvas(jegresses, esid);
        if (esid != null && enode != null && !"".equals(esid) && !"".equals(enode)) {
            provForceEgresses1.add(new ProvForceEgress(esid, enode));
        }
    }

    private void addJSONRoutes(ArrayList<ProvHop> provHops1, JSONArray jhops, int fx) {
        JSONObject jhop = jhops.getJSONObject(fx);
        String from = gvas(jhop, "from");
        String to = gvas(jhop, "to");
        String via = gvas(jhop, "via");
        if (from == null || to == null || via == null || "".equals(from) || "".equals(to) || "".equals(via)) {
            return;
        }
        provHops1.add(new ProvHop(from, to, via));
    }
}
