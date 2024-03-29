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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvFeed;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvFeedSubnet;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvFeedUser;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvForceEgress;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvForceIngress;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvHop;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvNode;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvParam;
import org.onap.dmaap.datarouter.node.config.NodeConfig.ProvSubscription;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.onap.dmaap.datarouter.node.utils.NodeUtils;

/**
 * Parser for provisioning data from the provisioning server.
 *
 * <p>The ProvData class uses a Reader for the text configuration from the provisioning server to construct arrays of
 * raw configuration entries.
 */
public class ProvData {

    private static final String FEED_ID = "feedid";

    private static final EELFLogger eelfLogger = EELFManager.getInstance().getLogger(ProvData.class);
    private final NodeConfig.ProvNode[] provNodes;
    private final NodeConfig.ProvParam[] provParams;
    private final NodeConfig.ProvFeed[] provFeeds;
    private final NodeConfig.ProvFeedUser[] provFeedUsers;
    private final NodeConfig.ProvFeedSubnet[] provFeedSubnets;
    private final NodeConfig.ProvSubscription[] provSubscriptions;
    private final NodeConfig.ProvForceIngress[] provForceIngresses;
    private final NodeConfig.ProvForceEgress[] provForceEgresses;
    private final NodeConfig.ProvHop[] provHops;

    /**
     * Construct raw provisioing data entries from the text (JSON) provisioning document received from the provisioning
     * server.
     *
     * @param reader The reader for the JSON text.
     */
    public ProvData(Reader reader) throws IOException {
        ArrayList<ProvNode> provNodeArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvParam> provParamArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeed> provFeedArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeedUser> provFeedUserArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvFeedSubnet> provFeedSubnetArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvSubscription> provSubscriptionArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvForceIngress> provForceIngressArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvForceEgress> provForceEgressArrayList = new ArrayList<>();
        ArrayList<NodeConfig.ProvHop> provHopArrayList = new ArrayList<>();
        try {
            JSONTokener jsonTokener = new JSONTokener(reader);
            JSONObject jsonObject = new JSONObject(jsonTokener);
            char nextCleanChar = jsonTokener.nextClean();
            if (nextCleanChar != '\0') {
                throw new JSONException("Spurious characters following configuration");
            }
            reader.close();
            addJSONFeeds(provFeedArrayList, provFeedUserArrayList, provFeedSubnetArrayList, jsonObject);
            addJSONSubs(provSubscriptionArrayList, jsonObject);
            addJSONParams(provNodeArrayList, provParamArrayList, jsonObject);
            addJSONRoutingInformation(provForceIngressArrayList, provForceEgressArrayList, provHopArrayList, jsonObject);
        } catch (JSONException jse) {
            NodeUtils.setIpAndFqdnForEelf("ProvData");
            eelfLogger.error(EelfMsgs.MESSAGE_PARSING_ERROR, jse.toString());
            eelfLogger
                    .error("NODE0201 Error parsing configuration data from provisioning server " + jse.toString(), jse);
            throw new IOException(jse.toString(), jse);
        }
        provNodes = provNodeArrayList.toArray(new ProvNode[0]);
        provParams = provParamArrayList.toArray(new ProvParam[0]);
        provFeeds = provFeedArrayList.toArray(new ProvFeed[0]);
        provFeedUsers = provFeedUserArrayList.toArray(new ProvFeedUser[0]);
        provFeedSubnets = provFeedSubnetArrayList.toArray(new ProvFeedSubnet[0]);
        provSubscriptions = provSubscriptionArrayList.toArray(new ProvSubscription[0]);
        provForceIngresses = provForceIngressArrayList.toArray(new ProvForceIngress[0]);
        provForceEgresses = provForceEgressArrayList.toArray(new ProvForceEgress[0]);
        provHops = provHopArrayList.toArray(new ProvHop[0]);
    }

    private static String[] gvasa(JSONObject object, String key) {
        return (gvasa(object.opt(key)));
    }

    private static String[] gvasa(Object object) {
        if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            ArrayList<String> array = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String string = gvas(jsonArray, i);
                if (string != null) {
                    array.add(string);
                }
            }
            return (array.toArray(new String[array.size()]));
        } else {
            String string = gvas(object);
            if (string == null) {
                return (new String[0]);
            } else {
                return (new String[]{string});
            }
        }
    }

    private static String gvas(JSONArray array, int index) {
        return (gvas(array.get(index)));
    }

    private static String gvas(JSONObject object, String key) {
        return (gvas(object.opt(key)));
    }

    private static String gvas(Object object) {
        if (object instanceof Boolean || object instanceof Number || object instanceof String) {
            return (object.toString());
        }
        return (null);
    }

    /**
     * Get the raw node configuration entries.
     */
    public NodeConfig.ProvNode[] getNodes() {
        return (provNodes);
    }

    /**
     * Get the raw parameter configuration entries.
     */
    public NodeConfig.ProvParam[] getParams() {
        return (provParams);
    }

    /**
     * Ge the raw feed configuration entries.
     */
    public NodeConfig.ProvFeed[] getFeeds() {
        return (provFeeds);
    }

    /**
     * Get the raw feed user configuration entries.
     */
    public NodeConfig.ProvFeedUser[] getFeedUsers() {
        return (provFeedUsers);
    }

    /**
     * Get the raw feed subnet configuration entries.
     */
    public NodeConfig.ProvFeedSubnet[] getFeedSubnets() {
        return (provFeedSubnets);
    }

    /**
     * Get the raw subscription entries.
     */
    public NodeConfig.ProvSubscription[] getSubscriptions() {
        return (provSubscriptions);
    }

    /**
     * Get the raw forced ingress entries.
     */
    public NodeConfig.ProvForceIngress[] getForceIngress() {
        return (provForceIngresses);
    }

    /**
     * Get the raw forced egress entries.
     */
    public NodeConfig.ProvForceEgress[] getForceEgress() {
        return (provForceEgresses);
    }

    /**
     * Get the raw next hop entries.
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

    private void addJSONFeeds(ArrayList<ProvFeed> pfv, ArrayList<ProvFeedUser> pfuv, ArrayList<ProvFeedSubnet> pfsnv,
            JSONObject jcfg) {
        JSONArray jfeeds = jcfg.optJSONArray("feeds");
        if (jfeeds != null) {
            for (int fx = 0; fx < jfeeds.length(); fx++) {
                addJSONFeed(pfv, pfuv, pfsnv, jfeeds, fx);
            }
        }
    }

    private void addJSONFeed(ArrayList<ProvFeed> pfv, ArrayList<ProvFeedUser> pfuv, ArrayList<ProvFeedSubnet> pfsnv,
            JSONArray jfeeds, int fx) {
        JSONObject jfeed = jfeeds.getJSONObject(fx);
        String stat = getFeedStatus(jfeed);
        String fid = gvas(jfeed, FEED_ID);
        String fname = gvas(jfeed, "name");
        String fver = gvas(jfeed, "version");
        String createdDate = gvas(jfeed, "created_date");
        pfv.add(new ProvFeed(fid, fname + "//" + fver, stat, createdDate));
        addJSONFeedAuthArrays(pfuv, pfsnv, jfeed, fid);
    }

    private void addJSONFeedAuthArrays(ArrayList<ProvFeedUser> pfuv, ArrayList<ProvFeedSubnet> pfsnv, JSONObject jfeed,
            String fid) {
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
                pfuv.add(new ProvFeedUser(fid, login, NodeUtils.getAuthHdr(login, password)));
            }
        }
        JSONArray jeips = jauth.optJSONArray("endpoint_addrs");
        if (jeips != null) {
            for (int ix = 0; ix < jeips.length(); ix++) {
                String sn = gvas(jeips, ix);
                pfsnv.add(new ProvFeedSubnet(fid, sn));
            }
        }
    }

    private void addJSONSubs(ArrayList<ProvSubscription> psv, JSONObject jcfg) {
        JSONArray jsubs = jcfg.optJSONArray("subscriptions");
        if (jsubs != null) {
            for (int sx = 0; sx < jsubs.length(); sx++) {
                addJSONSub(psv, jsubs, sx);
            }
        }
    }

    private void addJSONSub(ArrayList<ProvSubscription> psv, JSONArray jsubs, int sx) {
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
        psv.add(new ProvSubscription(sid, fid, delurl, id, NodeUtils.getAuthHdr(id, password), monly, use100,
                privilegedSubscriber, followRedirect, decompress));
    }

    private void addJSONParams(ArrayList<ProvNode> pnv, ArrayList<ProvParam> ppv, JSONObject jcfg) {
        JSONObject jparams = jcfg.optJSONObject("parameters");
        if (jparams != null) {
            for (String pname : JSONObject.getNames(jparams)) {
                addJSONParam(ppv, jparams, pname);
            }
            addJSONNodesToParams(pnv, jparams);
        }
    }

    private void addJSONParam(ArrayList<ProvParam> ppv, JSONObject jparams, String pname) {
        String pvalue = gvas(jparams, pname);
        if (pvalue != null) {
            ppv.add(new ProvParam(pname, pvalue));
        }
    }

    private void addJSONNodesToParams(ArrayList<ProvNode> pnv, JSONObject jparams) {
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
                pnv.add(new ProvNode(nn));
            }
        }
    }

    private void addJSONRoutingInformation(ArrayList<ProvForceIngress> pfiv, ArrayList<ProvForceEgress> pfev,
            ArrayList<ProvHop> phv, JSONObject jcfg) {
        JSONArray jingresses = jcfg.optJSONArray("ingress");
        if (jingresses != null) {
            for (int fx = 0; fx < jingresses.length(); fx++) {
                addJSONIngressRoute(pfiv, jingresses, fx);
            }
        }
        JSONObject jegresses = jcfg.optJSONObject("egress");
        if (jegresses != null && JSONObject.getNames(jegresses) != null) {
            for (String esid : JSONObject.getNames(jegresses)) {
                addJSONEgressRoute(pfev, jegresses, esid);
            }
        }
        JSONArray jhops = jcfg.optJSONArray("routing");
        if (jhops != null) {
            for (int fx = 0; fx < jhops.length(); fx++) {
                addJSONRoutes(phv, jhops, fx);
            }
        }
    }

    private void addJSONIngressRoute(ArrayList<ProvForceIngress> pfiv, JSONArray jingresses, int fx) {
        JSONObject jingress = jingresses.getJSONObject(fx);
        String fid = gvas(jingress, FEED_ID);
        String subnet = gvas(jingress, "subnet");
        String user = gvas(jingress, "user");
        if (fid == null || "".equals(fid)) {
            return;
        }
        if ("".equals(subnet)) {
            subnet = null;
        }
        if ("".equals(user)) {
            user = null;
        }
        String[] nodes = gvasa(jingress, "node");
        pfiv.add(new ProvForceIngress(fid, subnet, user, nodes));
    }

    private void addJSONEgressRoute(ArrayList<ProvForceEgress> pfev, JSONObject jegresses, String esid) {
        String enode = gvas(jegresses, esid);
        if (esid != null && enode != null && !"".equals(esid) && !"".equals(enode)) {
            pfev.add(new ProvForceEgress(esid, enode));
        }
    }

    private void addJSONRoutes(ArrayList<ProvHop> phv, JSONArray jhops, int fx) {
        JSONObject jhop = jhops.getJSONObject(fx);
        String from = gvas(jhop, "from");
        String to = gvas(jhop, "to");
        String via = gvas(jhop, "via");
        if (from == null || to == null || via == null || "".equals(from) || "".equals(to) || "".equals(via)) {
            return;
        }
        phv.add(new ProvHop(from, to, via));
    }
}
