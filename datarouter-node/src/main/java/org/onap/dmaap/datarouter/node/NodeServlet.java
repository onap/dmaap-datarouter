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

import static org.onap.dmaap.datarouter.node.NodeUtils.sendResponseError;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

/**
 * Servlet for handling all http and https requests to the data router node
 * <p>
 * Handled requests are:
 * <br>
 * GET http://<i>node</i>/internal/fetchProv - fetch the provisioning data
 * <br>
 * PUT/DELETE https://<i>node</i>/internal/publish/<i>fileid</i> - n2n transfer
 * <br>
 * PUT/DELETE https://<i>node</i>/publish/<i>feedid</i>/<i>fileid</i> - publsh request
 */
public class NodeServlet extends HttpServlet {

    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeServlet");
    private static NodeConfigManager config;
    private static Pattern MetaDataPattern;
    //Adding EELF Logger Rally:US664892
    private static EELFLogger eelflogger = EELFManager.getInstance()
            .getLogger(NodeServlet.class);
    private Delivery delivery;

    static {
        final String ws = "\\s*";
        // assume that \\ and \" have been replaced by X
        final String string = "\"[^\"]*\"";
        //String string = "\"(?:[^\"\\\\]|\\\\.)*\"";
        final String number = "[+-]?(?:\\.\\d+|(?:0|[1-9]\\d*)(?:\\.\\d*)?)(?:[eE][+-]?\\d+)?";
        final String value = "(?:" + string + "|" + number + "|null|true|false)";
        final String item = string + ws + ":" + ws + value + ws;
        final String object = ws + "\\{" + ws + "(?:" + item + "(?:" + "," + ws + item + ")*)?\\}" + ws;
        MetaDataPattern = Pattern.compile(object, Pattern.DOTALL);
    }

    NodeServlet(Delivery delivery) {
        this.delivery = delivery;
    }

    /**
     * Get the NodeConfigurationManager
     */
    public void init() {
        config = NodeConfigManager.getInstance();
        logger.info("NODE0101 Node Servlet Configured");
    }

    private boolean down(HttpServletResponse resp) throws IOException {
        if (config.isShutdown() || !config.isConfigured()) {
            sendResponseError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, logger);
            logger.info("NODE0102 Rejecting request: Service is being quiesced");
            return (true);
        }
        return (false);
    }

    /**
     * Handle a GET for /internal/fetchProv
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doGet");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelflogger.info(EelfMsgs.ENTRY);
        try {
            eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader("X-ATT-DR-ON-BEHALF-OF"),
                    getIdFromPath(req) + "");
            try {
                if (down(resp)) {
                    return;
                }

            } catch (IOException ioe) {
                logger.error("IOException" + ioe.getMessage());
            }
            String path = req.getPathInfo();
            String qs = req.getQueryString();
            String ip = req.getRemoteAddr();
            if (qs != null) {
                path = path + "?" + qs;
            }
            if ("/internal/fetchProv".equals(path)) {
                config.gofetch(ip);
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            } else if (path.startsWith("/internal/resetSubscription/")) {
                String subid = path.substring(28);
                if (subid.length() != 0 && subid.indexOf('/') == -1) {
                    NodeMain.resetQueue(subid, ip);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
            }

            logger.info("NODE0103 Rejecting invalid GET of " + path + " from " + ip);
            sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, logger);
        } finally {
            eelflogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * Handle all PUT requests
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doPut");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelflogger.info(EelfMsgs.ENTRY);
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader("X-ATT-DR-ON-BEHALF-OF"),
                    getIdFromPath(req) + "");
        try {
            common(req, resp, true);
        } catch (IOException ioe) {
            logger.error("IOException" + ioe.getMessage());
            eelflogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * Handle all DELETE requests
     */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doDelete");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelflogger.info(EelfMsgs.ENTRY);
        eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader("X-ATT-DR-ON-BEHALF-OF"),
                getIdFromPath(req) + "");
        try {
            common(req, resp, false);
        } catch (IOException ioe) {
            logger.error("IOException " + ioe.getMessage());
            eelflogger.info(EelfMsgs.EXIT);
        }
    }

    private void common(HttpServletRequest req, HttpServletResponse resp, boolean isput) throws IOException {
        String fileid = getFileId(req, resp);
        if (fileid == null) return;
        String feedid = null;
        String user = null;
        String credentials = req.getHeader("Authorization");
        if (credentials == null) {
            logger.info("NODE0106 Rejecting unauthenticated PUT or DELETE of " + req.getPathInfo() + " from " + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Authorization header required");
            eelflogger.info(EelfMsgs.EXIT);
            return;
        }
        String ip = req.getRemoteAddr();
        String lip = req.getLocalAddr();
        String pubid = null;
        String xpubid = null;
        String rcvd = NodeUtils.logts(System.currentTimeMillis()) + ";from=" + ip + ";by=" + lip;
        Target[] targets = null;
        if (fileid.startsWith("/delete/")) {
            deleteFile(req, resp, fileid, pubid);
            return;
        }
        else if (fileid.startsWith("/publish/")) {
            fileid = fileid.substring(9);
            int i = fileid.indexOf('/');
            if (i == -1 || i == fileid.length() - 1) {
                logger.info("NODE0105 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + " from " + req
                        .getRemoteAddr());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.  Possible missing fileid.");
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            feedid = fileid.substring(0, i);
            fileid = fileid.substring(i + 1);
            pubid = config.getPublishId();
            xpubid = req.getHeader("X-ATT-DR-PUBLISH-ID");
            targets = config.getTargets(feedid);
        } else if (fileid.startsWith("/internal/publish/")) {
            if (!config.isAnotherNode(credentials, ip)) {
                logger.info("NODE0107 Rejecting unauthorized node-to-node transfer attempt from " + ip);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            fileid = fileid.substring(18);
            pubid = req.getHeader("X-ATT-DR-PUBLISH-ID");
            targets = config.parseRouting(req.getHeader("X-ATT-DR-ROUTING"));
        } else {
            logger.info("NODE0105 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + " from " + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.");
            eelflogger.info(EelfMsgs.EXIT);
            return;
        }
        if (fileid.indexOf('/') != -1) {
            logger.info("NODE0105 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + " from " + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.");
            eelflogger.info(EelfMsgs.EXIT);
            return;
        }
        String qs = req.getQueryString();
        if (qs != null) {
            fileid = fileid + "?" + qs;
        }
        String hp = config.getMyName();
        int xp = config.getExtHttpsPort();
        if (xp != 443) {
            hp = hp + ":" + xp;
        }
        String logurl = "https://" + hp + "/internal/publish/" + fileid;
        if (feedid != null) {
            logurl = "https://" + hp + "/publish/" + feedid + "/" + fileid;
            String reason = config.isPublishPermitted(feedid, credentials, ip);
            if (reason != null) {
                logger.info(
                        "NODE0111 Rejecting unauthorized publish attempt to feed " + feedid + " fileid " + fileid
                                + " from "
                                + ip + " reason " + reason);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, reason);
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            user = config.getAuthUser(feedid, credentials);
            String newnode = config.getIngressNode(feedid, user, ip);
            if (newnode != null) {
                String port = "";
                int iport = config.getExtHttpsPort();
                if (iport != 443) {
                    port = ":" + iport;
                }
                String redirto = "https://" + newnode + port + "/publish/" + feedid + "/" + fileid;
                logger.info(
                        "NODE0108 Redirecting publish attempt for feed " + feedid + " user " + user + " ip " + ip
                                + " to "
                                + redirto);
                resp.sendRedirect(redirto);
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            resp.setHeader("X-ATT-DR-PUBLISH-ID", pubid);
        }
        String fbase = config.getSpoolDir() + "/" + pubid;
        File data = new File(fbase);
        File meta = new File(fbase + ".M");
        OutputStream dos = null;
        Writer mw = null;
        InputStream is = null;
        try {
            StringBuffer mx = new StringBuffer();
            mx.append(req.getMethod()).append('\t').append(fileid).append('\n');
            Enumeration hnames = req.getHeaderNames();
            String ctype = null;
            boolean hasRequestIdHeader = false;
            boolean hasInvocationIdHeader = false;
            while (hnames.hasMoreElements()) {
                String hn = (String) hnames.nextElement();
                String hnlc = hn.toLowerCase();
                if ((isput && ("content-type".equals(hnlc) ||
                        "content-language".equals(hnlc) ||
                        "content-md5".equals(hnlc) ||
                        "content-range".equals(hnlc))) ||
                        "x-att-dr-meta".equals(hnlc) ||
                        (feedid == null && "x-att-dr-received".equals(hnlc)) ||
                        (hnlc.startsWith("x-") && !hnlc.startsWith("x-att-dr-"))) {
                    Enumeration hvals = req.getHeaders(hn);
                    while (hvals.hasMoreElements()) {
                        String hv = (String) hvals.nextElement();
                        if ("content-type".equals(hnlc)) {
                            ctype = hv;
                        }
                        if ("x-onap-requestid".equals(hnlc)) {
                            hasRequestIdHeader = true;
                        }
                        if ("x-invocationid".equals(hnlc)) {
                            hasInvocationIdHeader = true;
                        }
                        if ("x-att-dr-meta".equals(hnlc)) {
                            if (hv.length() > 4096) {
                                logger.info(
                                        "NODE0109 Rejecting publish attempt with metadata too long for feed " + feedid
                                                + " user " + user + " ip " + ip);
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Metadata too long");
                                eelflogger.info(EelfMsgs.EXIT);
                                return;
                            }
                            if (!MetaDataPattern.matcher(hv.replaceAll("\\\\.", "X")).matches()) {
                                logger.info(
                                        "NODE0109 Rejecting publish attempt with malformed metadata for feed " + feedid
                                                + " user " + user + " ip " + ip);
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed metadata");
                                eelflogger.info(EelfMsgs.EXIT);
                                return;
                            }
                        }
                        mx.append(hn).append('\t').append(hv).append('\n');
                    }
                }
            }
            if(!hasRequestIdHeader){
                mx.append("X-ONAP-RequestID\t").append(MDC.get("RequestId")).append('\n');
            }
            if(!hasInvocationIdHeader){
                mx.append("X-InvocationID\t").append(MDC.get("InvocationId")).append('\n');
            }
            mx.append("X-ATT-DR-RECEIVED\t").append(rcvd).append('\n');
            String metadata = mx.toString();
            byte[] buf = new byte[1024 * 1024];
            int i;
            try {
                is = req.getInputStream();
                dos = new FileOutputStream(data);
                while ((i = is.read(buf)) > 0) {
                    dos.write(buf, 0, i);
                }
                is.close();
                is = null;
                dos.close();
                dos = null;
            } catch (IOException ioe) {
                long exlen = -1;
                try {
                    exlen = Long.parseLong(req.getHeader("Content-Length"));
                } catch (Exception e) {
                }
                StatusLog.logPubFail(pubid, feedid, logurl, req.getMethod(), ctype, exlen, data.length(), ip, user,
                        ioe.getMessage());
                eelflogger.info(EelfMsgs.EXIT);
                throw ioe;
            }
            Path dpath = Paths.get(fbase);
            for (Target t : targets) {
                DestInfo di = t.getDestInfo();
                if (di == null) {
                    // TODO: unknown destination
                    continue;
                }
                String dbase = di.getSpool() + "/" + pubid;
                Files.createLink(Paths.get(dbase), dpath);
                mw = new FileWriter(meta);
                mw.write(metadata);
                if (di.getSubId() == null) {
                    mw.write("X-ATT-DR-ROUTING\t" + t.getRouting() + "\n");
                }
                mw.close();
                meta.renameTo(new File(dbase + ".M"));
            }
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            resp.getOutputStream().close();
            StatusLog.logPub(pubid, feedid, logurl, req.getMethod(), ctype, data.length(), ip, user,
                    HttpServletResponse.SC_NO_CONTENT);
        } catch (IOException ioe) {
            logger.info(
                    "NODE0110 IO Exception receiving publish attempt for feed " + feedid + " user " + user + " ip " + ip
                            + " " + ioe.toString(), ioe);
            eelflogger.info(EelfMsgs.EXIT);
            throw ioe;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception e) {
                }
            }
            if (mw != null) {
                try {
                    mw.close();
                } catch (Exception e) {
                }
            }
            try {
                data.delete();
            } catch (Exception e) {
            }
            try {
                meta.delete();
            } catch (Exception e) {
            }
        }
    }

    private void deleteFile(HttpServletRequest req, HttpServletResponse resp, String fileid, String pubid) {
        try {
            fileid = fileid.substring(8);
            int i = fileid.indexOf('/');
            if (i == -1 || i == fileid.length() - 1) {
                logger.info("NODE0112 Rejecting bad URI for DELETE of " + req.getPathInfo() + " from " + req
                        .getRemoteAddr());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid request URI. Expecting <subId>/<pubId>.");
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            String subscriptionId = fileid.substring(0, i);
            String subscriptionStatus = config.isDeletePermitted(subscriptionId);
            if (!subscriptionStatus.equals("Permitted")) {
                logger.info("NODE0113 Unable to delete files. Error: " + subscriptionStatus);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, subscriptionStatus);
                eelflogger.info(EelfMsgs.EXIT);
                return;
            }
            int subId = Integer.parseInt(subscriptionId);
            pubid = fileid.substring(i + 1);
            int subIdDir = subId - (subId % 100);
            String filePath = config.getSpoolBase() + "/s/" + subIdDir + "/" + subId +
                    "/" + pubid;
            String metaFilePath = filePath + ".M";
            File fileToDelete = new File(filePath);
            File metaFileToDelete = new File(metaFilePath);
            if (fileToDelete.delete() && metaFileToDelete.delete()) {
                delivery.resetQueue(config.getSpoolBase() + "/s/" + subIdDir + "/" + subId);
                logger.info("NODE0114 Successfully deleted files (" + fileToDelete.getName() + ", " +
                        metaFileToDelete.getName() + ") from DR Node: " + config.getMyName());
                resp.setStatus(HttpServletResponse.SC_OK);
                eelflogger.info(EelfMsgs.EXIT);
            } else {
                logger.error("NODE0115 Unable to delete files (" + fileToDelete.getName() + ", " +
                        metaFileToDelete.getName() + ") from DR Node: " + config.getMyName());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on server.");
                eelflogger.info(EelfMsgs.EXIT);
            }
        } catch (IOException ioe) {
            logger.error("NODE0116 Unable to delete files (" + pubid + ", " + pubid + ".M) from DR Node: "
                    + config.getMyName() + ". Error: " + ioe.getMessage());
            eelflogger.info(EelfMsgs.EXIT);
            return;
        }
    }

    @Nullable
    private String getFileId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (down(resp)) {
            eelflogger.info(EelfMsgs.EXIT);
            return null;
        }
        if (!req.isSecure()) {
            logger.info(
                    "NODE0104 Rejecting insecure PUT or DELETE of " + req.getPathInfo() + " from " + req
                            .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "https required on publish requests");
            eelflogger.info(EelfMsgs.EXIT);
            return null;
        }
        String fileid = req.getPathInfo();
        if (fileid == null) {
            logger.info("NODE0105 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + " from " + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.");
            eelflogger.info(EelfMsgs.EXIT);
            return null;
        }
        return fileid;
    }

    private int getIdFromPath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.length() < 2) {
            return -1;
        }
        try {
            return Integer.parseInt(path.substring(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
