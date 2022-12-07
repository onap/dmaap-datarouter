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


package org.onap.dmaap.datarouter.provisioning;

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

import org.onap.dmaap.datarouter.provisioning.beans.Deleteable;
import org.onap.dmaap.datarouter.provisioning.beans.EgressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.IngressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.Insertable;
import org.onap.dmaap.datarouter.provisioning.beans.NetworkRoute;
import org.onap.dmaap.datarouter.provisioning.beans.NodeClass;



/**
 * <p>
 * This servlet handles requests to URLs under /internal/route/ on the provisioning server.
 * This part of the URL tree is used to manipulate the Data Router routing tables.
 * These include:
 * </p>
 * <div class="contentContainer">
 * <table class="packageSummary" border="0" cellpadding="3" cellspacing="0">
 * <caption>
 *     <span>URL Path Summary</span>
 *     <span class="tabEnd">&nbsp;</span>
 * </caption>
 * <tr>
 *   <th class="colFirst" width="35%">URL Path</th>
 *   <th class="colOne">Method</th>
 *   <th class="colLast">Purpose</th>
 * </tr>
 * <tr class="altColor">
 *   <td class="colFirst">/internal/route/</td>
 *   <td class="colOne">GET</td>
 *   <td class="colLast">used to GET a full JSON copy of all three routing tables.</td>
 * </tr>
 * <tr class="rowColor">
 *   <td class="colFirst" rowspan="2">/internal/route/ingress/</td>
 *   <td class="colOne">GET</td>
 *   <td class="colLast">used to GET a full JSON copy of the ingress routing table (IRT).</td>
 * </tr>
 * <tr class="rowColor">
 *   <td class="colOne">POST</td>
 *   <td class="colLast">used to create a new entry in the ingress routing table (IRT).</td></tr>
 * <tr class="altColor">
 *   <td class="colFirst" rowspan="2">/internal/route/egress/</td>
 *   <td class="colOne">GET</td>
 *   <td class="colLast">used to GET a full JSON copy of the egress routing table (ERT).</td>
 * </tr>
 * <tr class="altColor">
 *   <td class="colOne">POST</td>
 *   <td class="colLast">used to create a new entry in the egress routing table (ERT).</td></tr>
 * <tr class="rowColor">
 *   <td class="colFirst" rowspan="2">/internal/route/network/</td>
 *   <td class="colOne">GET</td>
 *   <td class="colLast">used to GET a full JSON copy of the network routing table (NRT).</td>
 * </tr>
 * <tr class="rowColor">
 *   <td class="colOne">POST</td>
 *   <td class="colLast">used to create a new entry in the network routing table (NRT).</td>
 * </tr>
 * <tr class="altColor">
 *   <td class="colFirst">/internal/route/ingress/&lt;feed&gt;/&lt;user&gt;/&lt;subnet&gt;</td>
 *   <td class="colOne">DELETE</td>
 *   <td class="colLast">used to DELETE the ingress route corresponding to <i>feed</i>, <i>user</i> and <i>subnet</i>.
 *   The / in the subnet specified should be replaced with a !, since / cannot be used in a URL.</td>
 * </tr>
 * <tr class="rowColor">
 *   <td class="colFirst">/internal/route/ingress/&lt;seq&gt;</td>
 *   <td class="colOne">DELETE</td>
 *   <td class="colLast">used to DELETE all ingress routes with the matching <i>seq</i> sequence number.</td>
 * </tr>
 * <tr class="altColor">
 *   <td class="colFirst">/internal/route/egress/&lt;sub&gt;</td>
 *   <td class="colOne">DELETE</td>
 *   <td class="colLast">used to DELETE the egress route the matching <i>sub</i> subscriber number.</td>
 * </tr>
 * <tr class="rowColor">
 *   <td class="colFirst">/internal/route/network/&lt;fromnode&gt;/&lt;tonode&gt;</td>
 *   <td class="colOne">DELETE</td>
 *   <td class="colLast">used to DELETE the network route corresponding to <i>fromnode</i>
 *   and <i>tonode</i>.</td>
 * </tr>
 * </table>
 * </div>
 * <p>
 * Authorization to use these URLs is a little different than for other URLs on the provisioning server.
 * For the most part, the IP address that the request comes from should be either:
 * </p>
 * <ol>
 * <li>an IP address of a provisioning server, or</li>
 * <li>the IP address of a node, or</li>
 * <li>an IP address from the "<i>special subnet</i>" which is configured with
 * the PROV_SPECIAL_SUBNET parameter.
 * </ol>
 * <p>
 * All DELETE/GET/POST requests made to this servlet on the standby server are proxied to the
 * active server (using the {@link ProxyServlet}) if it is up and reachable.
 * </p>
 *
 * @author Robert Eby
 * @version $Id$
 */
@SuppressWarnings("serial")

public class RouteServlet extends ProxyServlet {

    /**
     * DELETE route table entries by deleting part of the route table tree.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        EventLogRecord elr = new EventLogRecord(req);
        if (!isAuthorizedForInternal(req)) {
            elr.setMessage(UNAUTHORIZED);
            elr.setResult(HttpServletResponse.SC_FORBIDDEN);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, UNAUTHORIZED, eventlogger);
            return;
        }
        if (isProxyOK(req) && isProxyServer()) {
            super.doDelete(req, resp);
            return;
        }

        String path = req.getPathInfo();
        String[] parts = path.substring(1).split("/");
        Deleteable[] deleteables = null;
        if ("ingress".equals(parts[0])) {
            if (parts.length == 4) {
                // /internal/route/ingress/<feed>/<user>/<subnet>
                try {
                    int feedid = Integer.parseInt(parts[1]);
                    IngressRoute er = IngressRoute.getIngressRoute(feedid, parts[2], parts[3].replaceAll("!", "/"));
                    if (er == null) {
                        sendResponseError(resp,
                            HttpServletResponse.SC_NOT_FOUND, "The specified ingress route does not exist.",
                                eventlogger);
                        return;
                    }
                    deleteables = new Deleteable[] { er };
                } catch (NumberFormatException e) {
                    sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                            "Invalid feed ID in 'delete ingress' command.", eventlogger);
                    return;
                }
            } else if (parts.length == 2) {
                // /internal/route/ingress/<seq>
                try {
                    int seq = Integer.parseInt(parts[1]);
                    Set<IngressRoute> set = IngressRoute.getIngressRoutesForSeq(seq);
                    deleteables = set.toArray(new Deleteable[0]);
                } catch (NumberFormatException e) {
                    sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                            "Invalid sequence number in 'delete ingress' command.", eventlogger);
                    return;
                }
            } else {
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "Invalid number of arguments in 'delete ingress' command.", eventlogger);
                return;
            }
        } else if ("egress".equals(parts[0])) {
            if (parts.length == 2) {
                // /internal/route/egress/<sub>
                try {
                    int subid = Integer.parseInt(parts[1]);
                    EgressRoute er = EgressRoute.getEgressRoute(subid);
                    if (er == null) {
                        sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                                "The specified egress route does not exist.", eventlogger);
                        return;
                    }
                    deleteables = new Deleteable[] { er };
                } catch (NumberFormatException e) {
                    sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                            "Invalid sub ID in 'delete egress' command.", eventlogger);
                    return;
                }
            } else {
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "Invalid number of arguments in 'delete egress' command.", eventlogger);
                return;
            }
        } else if ("network".equals(parts[0])) {
            if (parts.length == 3) {
                // /internal/route/network/<from>/<to>
                try {
                    NetworkRoute nr = new NetworkRoute(
                        NodeClass.normalizeNodename(parts[1]),
                        NodeClass.normalizeNodename(parts[2])
                    );
                    deleteables = new Deleteable[] { nr };
                } catch (IllegalArgumentException e) {
                    String message = "The specified network route does not exist.";
                    eventlogger.error(message, e);
                    sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                    return;
                }
            } else {
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "Invalid number of arguments in 'delete network' command.", eventlogger);
                return;
            }
        }
        if (deleteables == null) {
            sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, BAD_URL, eventlogger);
            return;
        }
        boolean rv = true;
        for (Deleteable dd : deleteables) {
            rv &= doDelete(dd);
        }
        if (rv) {
            elr.setResult(HttpServletResponse.SC_OK);
            eventlogger.info(elr.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
            provisioningDataChanged();
            provisioningParametersChanged();
        } else {
            // Something went wrong with the DELETE
            elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, eventlogger);
        }
    }

    /**
     * GET route table entries from the route table tree specified by the URL path.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        EventLogRecord elr = new EventLogRecord(req);
        if (!isAuthorizedForInternal(req)) {
            elr.setMessage(UNAUTHORIZED);
            elr.setResult(HttpServletResponse.SC_FORBIDDEN);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, UNAUTHORIZED, eventlogger);
            return;
        }
        if (isProxyOK(req) && isProxyServer()) {
            super.doGet(req, resp);
            return;
        }

        String path = req.getPathInfo();
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (!"/".equals(path) && !INGRESS.equals(path) && !EGRESS.equals(path) && !NETWORK.equals(path)) {
            sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, BAD_URL, eventlogger);
            return;
        }

        StringBuilder sb = new StringBuilder("{\n");
        String px2 = "";
        if ("/".equals(path) || INGRESS.equals(path)) {
            String pfx = "\n";
            sb.append("\"ingress\": [");
            for (IngressRoute in : IngressRoute.getAllIngressRoutes()) {
                sb.append(pfx);
                sb.append(in.asJSONObject().toString());
                pfx = ",\n";
            }
            sb.append("\n]");
            px2 = ",\n";
        }

        if ("/".equals(path) || EGRESS.equals(path)) {
            String pfx = "\n";
            sb.append(px2);
            sb.append("\"egress\": {");
            for (EgressRoute eg : EgressRoute.getAllEgressRoutes()) {
                JSONObject jx = eg.asJSONObject();
                for (String key : jx.keySet()) {
                    sb.append(pfx);
                    sb.append("  \"").append(key).append("\": ");
                    try {
                        sb.append("\"").append(jx.getString(key)).append("\"");
                    } catch (JSONException je) {
                        eventlogger.error("PROV0161 RouteServlet.doGet: " + je.getMessage(), je);
                    }
                    pfx = ",\n";
                }
            }
            sb.append("\n}");
            px2 = ",\n";
        }

        if ("/".equals(path) || NETWORK.equals(path)) {
            String pfx = "\n";
            sb.append(px2);
            sb.append("\"routing\": [");
            for (NetworkRoute ne : NetworkRoute.getAllNetworkRoutes()) {
                sb.append(pfx);
                sb.append(ne.asJSONObject().toString());
                pfx = ",\n";
            }
            sb.append("\n]");
        }
        sb.append("}\n");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(sb.toString());
        } catch (IOException ioe) {
            eventlogger.error("PROV0162 RouteServlet.doGet: " + ioe.getMessage(), ioe);
        }
    }

    /**
     * PUT on &lt;/internal/route/*&gt; -- not supported.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        EventLogRecord elr = new EventLogRecord(req);
        if (!isAuthorizedForInternal(req)) {
            elr.setMessage(UNAUTHORIZED);
            elr.setResult(HttpServletResponse.SC_FORBIDDEN);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, UNAUTHORIZED, eventlogger);
            return;
        }
        sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, BAD_URL, eventlogger);
    }

    /**
     * POST - modify existing route table entries in the route table tree specified by the URL path.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        EventLogRecord elr = new EventLogRecord(req);
        if (!isAuthorizedForInternal(req)) {
            elr.setMessage(UNAUTHORIZED);
            elr.setResult(HttpServletResponse.SC_FORBIDDEN);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, UNAUTHORIZED, eventlogger);
            return;
        }
        if (isProxyOK(req) && isProxyServer()) {
            super.doPost(req, resp);
            return;
        }
        String path = req.getPathInfo();
        Insertable[] ins = null;
        if (path.startsWith(INGRESS)) {
            // /internal/route/ingress/?feed=%s&amp;user=%s&amp;subnet=%s&amp;nodepatt=%s
            try {
                // Although it probably doesn't make sense, you can install two identical routes in the IRT
                int feedid = Integer.parseInt(req.getParameter("feed"));
                String user = req.getParameter("user");
                if (user == null) {
                    user = "-";
                }
                String subnet = req.getParameter("subnet");
                if (subnet == null) {
                    subnet = "-";
                }
                String nodepatt = req.getParameter("nodepatt");
                String str = req.getParameter("seq");
                int seq = (str != null) ? Integer.parseInt(str) : (IngressRoute.getMaxSequence() + 100);
                ins = new Insertable[] { new IngressRoute(seq, feedid,
                        user, subnet, NodeClass.lookupNodeNames(nodepatt)) };
            } catch (Exception e) {
                intlogger.info(e.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid arguments in 'add ingress' command.", intlogger);
                return;
            }
        } else if (path.startsWith(EGRESS)) {
            // /internal/route/egress/?sub=%s&amp;node=%s
            try {
                int subid = Integer.parseInt(req.getParameter("sub"));
                EgressRoute er = EgressRoute.getEgressRoute(subid);
                if (er != null) {
                    sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "An egress route already exists for that subscriber.", intlogger);
                    return;
                }
                String node = NodeClass.normalizeNodename(req.getParameter("node"));
                ins = new Insertable[] { new EgressRoute(subid, node) };
            } catch (Exception e) {
                intlogger.info(e.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid arguments in 'add egress' command.", intlogger);
                return;
            }
        } else if (path.startsWith(NETWORK)) {
            // /internal/route/network/?from=%s&amp;to=%s&amp;via=%s
            try {
                String nfrom = req.getParameter("from");
                String nto   = req.getParameter("to");
                String nvia  = req.getParameter("via");
                if (nfrom == null || nto == null || nvia == null) {
                    sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Missing arguments in 'add network' command.", intlogger);
                    return;
                }
                nfrom = NodeClass.normalizeNodename(nfrom);
                nto   = NodeClass.normalizeNodename(nto);
                nvia  = NodeClass.normalizeNodename(nvia);
                NetworkRoute nr = new NetworkRoute(nfrom, nto, nvia);
                for (NetworkRoute route : NetworkRoute.getAllNetworkRoutes()) {
                    if (route.getFromnode() == nr.getFromnode() && route.getTonode() == nr.getTonode()) {
                        sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                "Network route table already contains a route for " + nfrom
                                        + " and " + nto, intlogger);
                        return;
                    }
                }
                ins = new Insertable[] { nr };
            } catch (IllegalArgumentException e) {
                intlogger.info(e.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid arguments in 'add network' command.", intlogger);
                return;
            }
        }
        if (ins == null) {
            sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, BAD_URL, intlogger);
            return;
        }
        boolean rv = true;
        for (Insertable dd : ins) {
            rv &= doInsert(dd);
        }
        if (rv) {
            elr.setResult(HttpServletResponse.SC_OK);
            eventlogger.info(elr.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
            provisioningDataChanged();
            provisioningParametersChanged();
        } else {
            // Something went wrong with the INSERT
            elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, intlogger);
        }
    }
}
