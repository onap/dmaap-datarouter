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
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

import static com.att.eelf.configuration.Configuration.*;

/**
 * Utility functions for the data router node
 */
public class NodeUtils {

    private static EELFLogger eelfLogger = EELFManager.getInstance()
            .getLogger(NodeUtils.class);
    private static Logger nodeUtilsLogger = Logger.getLogger("org.onap.dmaap.datarouter.node.NodeUtils");

    private NodeUtils() {
    }

    /**
     * Base64 encode a byte array
     *
     * @param  raw The bytes to be encoded
     * @return The encoded string
     */
    public static String base64Encode(byte[] raw) {
        return (Base64.encodeBase64String(raw));
    }

    /**
     * Given a user and password, generate the credentials
     *
     * @param user User name
     * @param password User password
     * @return Authorization header value
     */
    public static String getAuthHdr(String user, String password) {
        if (user == null || password == null) {
            return (null);
        }
        return ("Basic " + base64Encode((user + ":" + password).getBytes()));
    }

    /**
     * Given a node name, generate the credentials
     *
     * @param node Node name
     */
    public static String getNodeAuthHdr(String node, String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(key.getBytes());
            md.update(node.getBytes());
            md.update(key.getBytes());
            return (getAuthHdr(node, base64Encode(md.digest())));
        } catch (Exception exception) {
            nodeUtilsLogger
                    .error("Exception in generating Credentials for given node name:= " + exception.toString(),
                            exception);
            return (null);
        }
    }

    /**
     * Given a keystore file and its password, return the value of the CN of the first private key entry with a
     * certificate.
     *
     * @param kstype The type of keystore
     * @param ksfile The file name of the keystore
     * @param kspass The password of the keystore
     * @return CN of the certificate subject or null
     */
    public static String getCanonicalName(String kstype, String ksfile, String kspass) {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(kstype);
            try (FileInputStream fileInputStream = new FileInputStream(ksfile)) {
                ks.load(fileInputStream, kspass.toCharArray());
            } catch (IOException ioException) {
                nodeUtilsLogger.error("IOException occurred while opening FileInputStream: " + ioException.getMessage(),
                        ioException);
                return (null);
            }
        } catch (Exception e) {
            setIpAndFqdnForEelf("getCanonicalName");
            eelfLogger.error(EelfMsgs.MESSAGE_KEYSTORE_LOAD_ERROR, ksfile, e.toString());
            nodeUtilsLogger.error("NODE0401 Error loading my keystore file + " + ksfile + " " + e.toString(), e);
            return (null);
        }
        return (getCanonicalName(ks));
    }

    /**
     * Given a keystore, return the value of the CN of the first private key entry with a certificate.
     *
     * @param ks The KeyStore
     * @return CN of the certificate subject or null
     */
    public static String getCanonicalName(KeyStore ks) {
        try {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String s = aliases.nextElement();
                if (ks.entryInstanceOf(s, KeyStore.PrivateKeyEntry.class)) {
                    X509Certificate c = (X509Certificate) ks.getCertificate(s);
                    if (c != null) {
                        String subject = c.getSubjectX500Principal().getName();
                        String[] parts = subject.split(",");
                        if (parts.length < 1) {
                            return (null);
                        }
                        subject = parts[5].trim();
                        if (!subject.startsWith("CN=")) {
                            return (null);

                        }
                        return (subject.substring(3));
                    }
                }
            }
        } catch (Exception e) {
            nodeUtilsLogger.error("NODE0402 Error extracting my name from my keystore file " + e.toString(), e);
        }
        return (null);
    }

    /**
     * Given a string representation of an IP address, get the corresponding byte array
     *
     * @param ip The IP address as a string
     * @return The IP address as a byte array or null if the address is invalid
     */
    public static byte[] getInetAddress(String ip) {
        try {
            return (InetAddress.getByName(ip).getAddress());
        } catch (Exception exception) {
            nodeUtilsLogger
                    .error("Exception in generating byte array for given IP address := " + exception.toString(),
                            exception);
        }
        return (null);
    }

    /**
     * Given a uri with parameters, split out the feed ID and file ID
     */
    public static String[] getFeedAndFileID(String uriandparams) {
        int end = uriandparams.length();
        int i = uriandparams.indexOf('#');
        if (i != -1 && i < end) {
            end = i;
        }
        i = uriandparams.indexOf('?');
        if (i != -1 && i < end) {
            end = i;
        }
        end = uriandparams.lastIndexOf('/', end);
        if (end < 2) {
            return (null);
        }
        i = uriandparams.lastIndexOf('/', end - 1);
        if (i == -1) {
            return (null);
        }
        return (new String[]{uriandparams.substring(i + 1, end), uriandparams.substring(end + 1)});
    }

    /**
     * Escape fields that might contain vertical bar, backslash, or newline by replacing them with backslash p,
     * backslash e and backslash n.
     */
    public static String loge(String s) {
        if (s == null) {
            return (s);
        }
        return (s.replaceAll("\\\\", "\\\\e").replaceAll("\\|", "\\\\p").replaceAll("\n", "\\\\n"));
    }

    /**
     * Undo what loge does.
     */
    public static String unloge(String s) {
        if (s == null) {
            return (s);
        }
        return (s.replaceAll("\\\\p", "\\|").replaceAll("\\\\n", "\n").replaceAll("\\\\e", "\\\\"));
    }

    /**
     * Format a logging timestamp as yyyy-mm-ddThh:mm:ss.mmmZ
     */
    public static String logts(long when) {
        return (logts(new Date(when)));
    }

    /**
     * Format a logging timestamp as yyyy-mm-ddThh:mm:ss.mmmZ
     */
    public static synchronized String logts(Date when) {
        SimpleDateFormat logDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        logDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (logDate.format(when));
    }

    /* Method prints method name, server FQDN and IP Address of the machine in EELF logs
     * @Method - setIpAndFqdnForEelf - Rally:US664892
     * @Params - method, prints method name in EELF log.
     */
    public static void setIpAndFqdnForEelf(String method) {
        MDC.clear();
        MDC.put(MDC_SERVICE_NAME, method);
        try {
            MDC.put(MDC_SERVER_FQDN, InetAddress.getLocalHost().getHostName());
            MDC.put(MDC_SERVER_IP_ADDRESS, InetAddress.getLocalHost().getHostAddress());
        } catch (Exception exception) {
            nodeUtilsLogger
                    .error("Exception in generating byte array for given IP address := " + exception.toString(),
                            exception);
        }

    }

    /* Method sets RequestIs and InvocationId for se in EELF logs
     * @Method - setIpAndFqdnForEelf
     * @Params - Req, Request used to get RequestId and InvocationId
     */
    public static void setRequestIdAndInvocationId(HttpServletRequest req) {
        String reqId = req.getHeader("X-ONAP-RequestID");
        if (StringUtils.isBlank(reqId)) {
            reqId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY_REQUEST_ID, reqId);
        String invId = req.getHeader("X-InvocationID");
        if (StringUtils.isBlank(invId)) {
            invId = UUID.randomUUID().toString();
        }
        MDC.put("InvocationId", invId);
    }

    public static void sendResponseError(HttpServletResponse response, int errorCode, Logger intlogger) {
        try {
            response.sendError(errorCode);
        } catch (IOException ioe) {
            intlogger.error("IOException" + ioe.getMessage());
        }
    }

    /**
     * Method to check to see if file is of type gzip
     *
     * @param   file The name of the file to be checked
     * @return          True if the file is of type gzip
     */
    public static boolean isFiletypeGzip(File file){
        try(FileInputStream fileInputStream = new FileInputStream(file);
            GZIPInputStream gzip = new GZIPInputStream(fileInputStream)) {

            return true;
        }catch (IOException e){
            nodeUtilsLogger.error("NODE0403 " + file.toString() + " Not in gzip(gz) format: " + e.toString() + e);
            return false;
        }
    }


}
