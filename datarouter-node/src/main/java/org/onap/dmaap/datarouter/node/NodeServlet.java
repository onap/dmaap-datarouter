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
import org.jetbrains.annotations.Nullable;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

/**
 * Servlet for handling all http and https requests to the data router node.
 *
 * <p>Handled requests are:
 * <br>
 * GET http://<i>node</i>/internal/fetchProv - fetch the provisioning data
 * <br>
 * PUT/DELETE https://<i>node</i>/internal/publish/<i>fileid</i> - n2n transfer
 * <br>
 * PUT/DELETE https://<i>node</i>/publish/<i>feedid</i>/<i>fileid</i> - publsh request
 */
public class NodeServlet extends HttpServlet {

    private static final String FROM = " from ";
    private static final String INVALID_REQUEST_URI = "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.";
    private static final String IO_EXCEPTION = "IOException";
    private static final String ON_BEHALF_OF = "X-DMAAP-DR-ON-BEHALF-OF";
    private static NodeConfigManager config;
    private static Pattern metaDataPattern;
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeServlet.class);

    static {
        final String ws = "\\s*";
        // assume that \\ and \" have been replaced by X
        final String string = "\"[^\"]*\"";
        final String number = "[+-]?(?:\\.\\d+|(?:0|[1-9]\\d*)(?:\\.\\d*)?)(?:[eE][+-]?\\d+)?";
        final String value = "(?:" + string + "|" + number + "|null|true|false)";
        final String item = string + ws + ":" + ws + value + ws;
        final String object = ws + "\\{" + ws + "(?:" + item + "(?:" + "," + ws + item + ")*)?\\}" + ws;
        metaDataPattern = Pattern.compile(object, Pattern.DOTALL);
    }

    private final Delivery delivery;

    NodeServlet(Delivery delivery) {
        this.delivery = delivery;
    }

    /**
     * Get the NodeConfigurationManager.
     */
    @Override
    public void init() {
        config = NodeConfigManager.getInstance();
        eelfLogger.debug("NODE0101 Node Servlet Configured");
    }

    private boolean down(HttpServletResponse resp) {
        if (config.isShutdown() || !config.isConfigured()) {
            sendResponseError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, eelfLogger);
            eelfLogger.error("NODE0102 Rejecting request: Service is being quiesced");
            return true;
        }
        return false;
    }

    /**
     * Handle a GET for /internal/fetchProv.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doGet");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.debug(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(ON_BEHALF_OF),
                    getIdFromPath(req) + "");
            if (down(resp)) {
                return;
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
                    NodeServer.resetQueue(subid, ip);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
            }

            eelfLogger.debug("NODE0103 Rejecting invalid GET of " + path + FROM + ip);
            sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, eelfLogger);
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * Handle all PUT requests.
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doPut");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelfLogger.info(EelfMsgs.ENTRY);
        eelfLogger.debug(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(ON_BEHALF_OF),
                getIdFromPath(req) + "");
        try {
            common(req, resp, true);
        } catch (IOException ioe) {
            eelfLogger.error(IO_EXCEPTION, ioe);
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * Handle all DELETE requests.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        NodeUtils.setIpAndFqdnForEelf("doDelete");
        NodeUtils.setRequestIdAndInvocationId(req);
        eelfLogger.info(EelfMsgs.ENTRY);
        eelfLogger.debug(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID, req.getHeader(ON_BEHALF_OF),
                getIdFromPath(req) + "");
        try {
            common(req, resp, false);
        } catch (IOException ioe) {
            eelfLogger.error(IO_EXCEPTION, ioe);
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    private void common(HttpServletRequest req, HttpServletResponse resp, boolean isput) throws IOException {
        final String PUBLISH = "/publish/";
        final String INTERNAL_PUBLISH = "/internal/publish/";
        final String HTTPS = "https://";
        final String USER = " user ";
        String fileid = getFileId(req, resp);
        if (fileid == null) {
            return;
        }
        String feedid = null;
        String user = null;
        String ip = req.getRemoteAddr();
        String lip = req.getLocalAddr();
        String pubid = null;
        String rcvd = NodeUtils.logts(System.currentTimeMillis()) + ";from=" + ip + ";by=" + lip;
        Target[] targets = null;
        boolean isAAFFeed = false;
        if (fileid.startsWith("/delete/")) {
            deleteFile(req, resp, fileid, pubid);
            return;
        }
        String credentials = req.getHeader("Authorization");
        if (credentials == null) {
            eelfLogger.error("NODE0306 Rejecting unauthenticated PUT or DELETE of " + req.getPathInfo() + FROM + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Authorization header required");
            eelfLogger.info(EelfMsgs.EXIT);
            return;
        }
        if (fileid.startsWith(PUBLISH)) {
            fileid = fileid.substring(9);
            int index = fileid.indexOf('/');
            if (index == -1 || index == fileid.length() - 1) {
                eelfLogger.error("NODE0205 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + FROM + req
                        .getRemoteAddr());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.  Possible missing fileid.");
                eelfLogger.info(EelfMsgs.EXIT);
                return;
            }
            feedid = fileid.substring(0, index);

            if (config.getCadiEnabled()) {
                String path = req.getPathInfo();
                if (!path.startsWith("/internal") && feedid != null) {
                    String aafInstance = config.getAafInstance(feedid);
                    if (!("legacy".equalsIgnoreCase(aafInstance))) {
                        isAAFFeed = true;
                        String permission = config.getPermission(aafInstance);
                        eelfLogger.debug("NodeServlet.common() permission string - " + permission);
                        //Check in CADI Framework API if user has AAF permission or not
                        if (!req.isUserInRole(permission)) {
                            String message = "AAF disallows access to permission string - " + permission;
                            eelfLogger.error("NODE0307 Rejecting unauthenticated PUT or DELETE of " + req.getPathInfo()
                                    + FROM + req.getRemoteAddr());
                            resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
                            eelfLogger.info(EelfMsgs.EXIT);
                            return;
                        }
                    }
                }
            }

            fileid = fileid.substring(index + 1);
            pubid = config.getPublishId();
            targets = config.getTargets(feedid);
        } else if (fileid.startsWith(INTERNAL_PUBLISH)) {
            if (!config.isAnotherNode(credentials, ip)) {
                eelfLogger.error("NODE0107 Rejecting unauthorized node-to-node transfer attempt from " + ip);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                eelfLogger.info(EelfMsgs.EXIT);
                return;
            }
            fileid = fileid.substring(18);
            pubid = generateAndValidatePublishId(req);

            user = "datartr";   // SP6 : Added usr as datartr to avoid null entries for internal routing
            targets = config.parseRouting(req.getHeader("X-DMAAP-DR-ROUTING"));
        } else {
            eelfLogger.error("NODE0204 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + FROM + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    INVALID_REQUEST_URI);
            eelfLogger.info(EelfMsgs.EXIT);
            return;
        }
        if (fileid.indexOf('/') != -1) {
            eelfLogger.error("NODE0202 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + FROM + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    INVALID_REQUEST_URI);
            eelfLogger.info(EelfMsgs.EXIT);
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
        String logurl = HTTPS + hp + INTERNAL_PUBLISH + fileid;
        if (feedid != null) {
            logurl = HTTPS + hp + PUBLISH + feedid + "/" + fileid;
            //Cadi code starts
            if (!isAAFFeed) {
                String reason = config.isPublishPermitted(feedid, credentials, ip);
                if (reason != null) {
                    eelfLogger.error("NODE0111 Rejecting unauthorized publish attempt to feed " + PathUtil
                            .cleanString(feedid) + " fileid " + PathUtil.cleanString(fileid) + FROM + PathUtil
                            .cleanString(ip) + " reason " + PathUtil.cleanString(reason));
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, reason);
                    eelfLogger.info(EelfMsgs.EXIT);
                    return;
                }
                user = config.getAuthUser(feedid, credentials);
            } else {
                String reason = config.isPublishPermitted(feedid, ip);
                if (reason != null) {
                    eelfLogger.error("NODE0111 Rejecting unauthorized publish attempt to feed " + PathUtil
                            .cleanString(feedid) + " fileid " + PathUtil.cleanString(fileid) + FROM + PathUtil
                            .cleanString(ip) + " reason   Invalid AAF user- " + PathUtil.cleanString(reason));
                    String message = "Invalid AAF user- " + PathUtil.cleanString(reason);
                    eelfLogger.debug("NODE0308 Rejecting unauthenticated PUT or DELETE of " + PathUtil
                            .cleanString(req.getPathInfo()) + FROM + PathUtil.cleanString(req.getRemoteAddr()));
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
                    return;
                }
                if ((req.getUserPrincipal() != null) && (req.getUserPrincipal().getName() != null)) {
                    String userName = req.getUserPrincipal().getName();
                    String[] attid = userName.split("@");
                    user = attid[0];
                } else {
                    user = "AAFUser";
                }
            }
            //Cadi code Ends
            String newnode = config.getIngressNode(feedid, user, ip);
            if (newnode != null) {
                String port = "";
                int iport = config.getExtHttpsPort();
                if (iport != 443) {
                    port = ":" + iport;
                }
                String redirto = HTTPS + newnode + port + PUBLISH + feedid + "/" + fileid;
                eelfLogger
                        .debug("NODE0108 Redirecting publish attempt for feed " + PathUtil.cleanString(feedid) + USER
                                + PathUtil.cleanString(user) + " ip " + PathUtil.cleanString(ip) + " to " + PathUtil
                                .cleanString(redirto));  //Fortify scan fixes - log forging
                resp.sendRedirect(PathUtil.cleanString(redirto));         //Fortify scan fixes-open redirect - 2 issues
                eelfLogger.info(EelfMsgs.EXIT);
                return;
            }
            resp.setHeader("X-DMAAP-DR-PUBLISH-ID", pubid);
        }
        if (req.getPathInfo().startsWith(INTERNAL_PUBLISH)) {
            feedid = req.getHeader("X-DMAAP-DR-FEED-ID");
        }
        String fbase = PathUtil.cleanString(config.getSpoolDir() + "/" + pubid);  //Fortify scan fixes-Path manipulation
        File data = new File(fbase);
        File meta = new File(fbase + ".M");
        Writer mw = null;
        try {
            StringBuilder mx = new StringBuilder();
            mx.append(req.getMethod()).append('\t').append(fileid).append('\n');
            Enumeration hnames = req.getHeaderNames();
            String ctype = null;
            boolean hasRequestIdHeader = false;
            boolean hasInvocationIdHeader = false;
            while (hnames.hasMoreElements()) {
                String hn = (String) hnames.nextElement();
                String hnlc = hn.toLowerCase();
                if ((isput && ("content-type".equals(hnlc)
                        || "content-language".equals(hnlc)
                        || "content-md5".equals(hnlc)
                        || "content-range".equals(hnlc)))
                        || "x-dmaap-dr-meta".equals(hnlc)
                        || (feedid == null && "x-dmaap-dr-received".equals(hnlc))
                        || (hnlc.startsWith("x-") && !hnlc.startsWith("x-dmaap-dr-"))) {
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
                        if ("x-dmaap-dr-meta".equals(hnlc)) {
                            if (hv.length() > 4096) {
                                eelfLogger.error("NODE0109 Rejecting publish attempt with metadata too long for feed "
                                        + PathUtil.cleanString(feedid) + USER + PathUtil.cleanString(user) + " ip "
                                        + PathUtil.cleanString(ip));  //Fortify scan fixes - log forging
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Metadata too long");
                                eelfLogger.info(EelfMsgs.EXIT);
                                return;
                            }
                            if (!metaDataPattern.matcher(hv.replaceAll("\\\\.", "X")).matches()) {
                                eelfLogger.error("NODE0109 Rejecting publish attempt with malformed metadata for feed "
                                        + PathUtil.cleanString(feedid) + USER + PathUtil.cleanString(user) + " ip "
                                        + PathUtil.cleanString(ip));  //Fortify scan fixes - log forging
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed metadata");
                                eelfLogger.info(EelfMsgs.EXIT);
                                return;
                            }
                        }
                        mx.append(hn).append('\t').append(hv).append('\n');
                    }
                }
            }
            if (!hasRequestIdHeader) {
                mx.append("X-ONAP-RequestID\t").append(MDC.get("RequestId")).append('\n');
            }
            if (!hasInvocationIdHeader) {
                mx.append("X-InvocationID\t").append(MDC.get("InvocationId")).append('\n');
            }
            mx.append("X-DMAAP-DR-RECEIVED\t").append(rcvd).append('\n');
            String metadata = mx.toString();
            long exlen = getExlen(req);
            String message = writeInputStreamToFile(req, data);
            if (message != null) {
                StatusLog.logPubFail(pubid, feedid, logurl, req.getMethod(), ctype, exlen, data.length(), ip, user,
                        message);
                throw new IOException(message);
            }
            Path dpath = Paths.get(fbase);
            for (Target t : targets) {
                DestInfo di = t.getDestInfo();
                if (di == null) {
                    //Handle this? : unknown destination
                    continue;
                }
                String dbase = PathUtil
                        .cleanString(di.getSpool() + "/" + pubid);  //Fortify scan fixes-Path Manipulation
                Files.createLink(Paths.get(dbase), dpath);
                mw = new FileWriter(meta);
                mw.write(metadata);
                if (di.getSubId() == null) {
                    mw.write("X-DMAAP-DR-ROUTING\t" + t.getRouting() + "\n");
                }
                mw.close();
                if (!meta.renameTo(new File(dbase + ".M"))) {
                    eelfLogger.error("Rename of file " + dbase + " failed.");
                }
            }
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            try {
                resp.getOutputStream().close();
            } catch (IOException ioe) {
                StatusLog.logPubFail(pubid, feedid, logurl, req.getMethod(), ctype, exlen, data.length(), ip, user,
                        ioe.getMessage());
                //Fortify scan fixes - log forging
                eelfLogger.error("NODE0110 IO Exception while closing IO stream " + PathUtil.cleanString(feedid)
                        + USER + PathUtil.cleanString(user) + " ip " + PathUtil.cleanString(ip) + " " + ioe
                        .toString(), ioe);
                throw ioe;
            }

            StatusLog.logPub(pubid, feedid, logurl, req.getMethod(), ctype, data.length(), ip, user,
                    HttpServletResponse.SC_NO_CONTENT);
        } catch (IOException ioe) {
            eelfLogger.error("NODE0110 IO Exception receiving publish attempt for feed " + feedid + USER + user
                    + " ip " + ip + " " + ioe.toString(), ioe);
            eelfLogger.info(EelfMsgs.EXIT);
            throw ioe;
        } finally {
            if (mw != null) {
                try {
                    mw.close();
                } catch (Exception e) {
                    eelfLogger.error("NODE0532 Exception common: " + e);
                }
            }
            try {
                Files.delete(data.toPath());
                Files.delete(meta.toPath());
            } catch (Exception e) {
                eelfLogger.error("NODE0533 Exception common: " + e);
            }
        }
    }

    private String generateAndValidatePublishId(HttpServletRequest req) throws IOException {
        String newPubId = req.getHeader("X-DMAAP-DR-PUBLISH-ID");

        String regex = ".*";

        if(newPubId.matches(regex)){
            return newPubId;
        }
        throw new IOException("Invalid Header X-DMAAP-DR-PUBLISH-ID");
    }

    private String writeInputStreamToFile(HttpServletRequest req, File data) {
        byte[] buf = new byte[1024 * 1024];
        int bytesRead;
        try (OutputStream dos = new FileOutputStream(data);
                InputStream is = req.getInputStream()) {
            while ((bytesRead = is.read(buf)) > 0) {
                dos.write(buf, 0, bytesRead);
            }
        } catch (IOException ioe) {
            eelfLogger.error("NODE0530 Exception common: " + ioe, ioe);
            eelfLogger.info(EelfMsgs.EXIT);
            return ioe.getMessage();
        }
        return null;
    }

    private long getExlen(HttpServletRequest req) {
        long exlen = -1;
        try {
            exlen = Long.parseLong(req.getHeader("Content-Length"));
        } catch (Exception e) {
            eelfLogger.error("NODE0529 Exception common: " + e);
        }
        return exlen;
    }

    private void deleteFile(HttpServletRequest req, HttpServletResponse resp, String fileid, String pubid) {
        final String FROM_DR_MESSAGE = ".M) from DR Node: ";
        try {
            fileid = fileid.substring(8);
            int index = fileid.indexOf('/');
            if (index == -1 || index == fileid.length() - 1) {
                eelfLogger.error("NODE0112 Rejecting bad URI for DELETE of " + req.getPathInfo() + FROM + req
                        .getRemoteAddr());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid request URI. Expecting <subId>/<pubId>.");
                eelfLogger.info(EelfMsgs.EXIT);
                return;
            }
            String subscriptionId = fileid.substring(0, index);
            int subId = Integer.parseInt(subscriptionId);
            pubid = fileid.substring(index + 1);
            String errorMessage = "Unable to delete files (" + pubid + ", " + pubid + FROM_DR_MESSAGE
                    + config.getMyName() + ".";
            int subIdDir = subId - (subId % 100);
            if (!isAuthorizedToDelete(resp, subscriptionId, errorMessage)) {
                return;
            }
            boolean result = delivery.markTaskSuccess(config.getSpoolBase() + "/s/" + subIdDir + "/" + subId, pubid);
            if (result) {
                eelfLogger.debug("NODE0115 Successfully deleted files (" + pubid + ", " + pubid + FROM_DR_MESSAGE
                        + config.getMyName());
                resp.setStatus(HttpServletResponse.SC_OK);
                eelfLogger.info(EelfMsgs.EXIT);
            } else {
                eelfLogger.error("NODE0116 " + errorMessage);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on server.");
                eelfLogger.info(EelfMsgs.EXIT);
            }
        } catch (IOException ioe) {
            eelfLogger.error("NODE0117 Unable to delete files (" + pubid + ", " + pubid + FROM_DR_MESSAGE
                    + config.getMyName(), ioe);
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    @Nullable
    private String getFileId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (down(resp)) {
            eelfLogger.info(EelfMsgs.EXIT);
            return null;
        }
        if (!req.isSecure()) {
            eelfLogger.error(
                    "NODE0104 Rejecting insecure PUT or DELETE of " + req.getPathInfo() + FROM + req
                            .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "https required on publish requests");
            eelfLogger.info(EelfMsgs.EXIT);
            return null;
        }
        String fileid = req.getPathInfo();
        if (fileid == null) {
            eelfLogger.error("NODE0201 Rejecting bad URI for PUT or DELETE of " + req.getPathInfo() + FROM + req
                    .getRemoteAddr());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    INVALID_REQUEST_URI);
            eelfLogger.info(EelfMsgs.EXIT);
            return null;
        }
        return fileid;
    }

    private boolean isAuthorizedToDelete(HttpServletResponse resp, String subscriptionId, String errorMessage)
            throws IOException {
        try {
            boolean deletePermitted = config.isDeletePermitted(subscriptionId);
            if (!deletePermitted) {
                eelfLogger.error("NODE0113 " + errorMessage + " Error: Subscription "
                        + subscriptionId + " is not a privileged subscription");
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                eelfLogger.info(EelfMsgs.EXIT);
                return false;
            }
        } catch (NullPointerException npe) {
            eelfLogger.error("NODE0114 " + errorMessage + " Error: Subscription " + subscriptionId
                    + " does not exist", npe);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            eelfLogger.info(EelfMsgs.EXIT);
            return false;
        }
        return true;
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
