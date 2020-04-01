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

import static com.att.eelf.configuration.Configuration.MDC_KEY_REQUEST_ID;
import static com.att.eelf.configuration.Configuration.MDC_SERVER_FQDN;
import static com.att.eelf.configuration.Configuration.MDC_SERVER_IP_ADDRESS;
import static com.att.eelf.configuration.Configuration.MDC_SERVICE_NAME;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

/**
 * Utility functions for the data router node.
 */
public class NodeUtils {

    private static EELFLogger eelfLogger = EELFManager.getInstance()
            .getLogger(NodeUtils.class);

    private NodeUtils() {
    }

    /**
     * Base64 encode a byte array.
     *
     * @param raw The bytes to be encoded
     * @return The encoded string
     */
    public static String base64Encode(byte[] raw) {
        return (Base64.encodeBase64String(raw));
    }

    /**
     * Given a user and password, generate the credentials.
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
     * Given a node name, generate the credentials.
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
            eelfLogger
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
            if (loadKeyStore(ksfile, kspass, ks)) {
                return (null);
            }
        } catch (Exception e) {
            setIpAndFqdnForEelf("getCanonicalName");
            eelfLogger.error(EelfMsgs.MESSAGE_KEYSTORE_LOAD_ERROR, e, ksfile);
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
                String name = getNameFromSubject(ks, aliases);
                if (name != null) {
                    return name;
                }
            }
        } catch (Exception e) {
            eelfLogger.error("NODE0402 Error extracting my name from my keystore file " + e.toString(), e);
        }
        return (null);
    }

    /**
     * Given a string representation of an IP address, get the corresponding byte array.
     *
     * @param ip The IP address as a string
     * @return The IP address as a byte array or null if the address is invalid
     */
    public static byte[] getInetAddress(String ip) {
        try {
            return (InetAddress.getByName(ip).getAddress());
        } catch (Exception exception) {
            eelfLogger
                    .error("Exception in generating byte array for given IP address := " + exception.toString(),
                            exception);
        }
        return (null);
    }

    /**
     * Given a uri with parameters, split out the feed ID and file ID.
     */
    public static String[] getFeedAndFileID(String uriandparams) {
        int end = uriandparams.length();
        int index = uriandparams.indexOf('#');
        if (index != -1 && index < end) {
            end = index;
        }
        index = uriandparams.indexOf('?');
        if (index != -1 && index < end) {
            end = index;
        }
        end = uriandparams.lastIndexOf('/', end);
        if (end < 2) {
            return (null);
        }
        index = uriandparams.lastIndexOf('/', end - 1);
        if (index == -1) {
            return (null);
        }
        return (new String[]{uriandparams.substring(index + 1, end), uriandparams.substring(end + 1)});
    }

    /**
     * Escape fields that might contain vertical bar, backslash, or newline by replacing them with backslash p,
     * backslash e and backslash n.
     */
    public static String loge(String string) {
        if (string == null) {
            return (string);
        }
        return (string.replaceAll("\\\\", "\\\\e").replaceAll("\\|", "\\\\p").replaceAll("\n", "\\\\n"));
    }

    /**
     * Undo what loge does.
     */
    public static String unloge(String string) {
        if (string == null) {
            return (string);
        }
        return (string.replaceAll("\\\\p", "\\|").replaceAll("\\\\n", "\n").replaceAll("\\\\e", "\\\\"));
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

    /** Method prints method name, server FQDN and IP Address of the machine in EELF logs.
     *
     * @param method Prints method name in EELF log.
     */
    public static void setIpAndFqdnForEelf(String method) {
        MDC.clear();
        MDC.put(MDC_SERVICE_NAME, method);
        try {
            MDC.put(MDC_SERVER_FQDN, InetAddress.getLocalHost().getHostName());
            MDC.put(MDC_SERVER_IP_ADDRESS, InetAddress.getLocalHost().getHostAddress());
        } catch (Exception exception) {
            eelfLogger
                    .error("Exception in generating byte array for given IP address := " + exception.toString(),
                            exception);
        }

    }

    /** Method sets RequestIs and InvocationId for se in EELF logs.
     *
     * @param req Request used to get RequestId and InvocationId.
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

    /**
     * Sends error as response with error code input.
     */
    public static void sendResponseError(HttpServletResponse response, int errorCode, EELFLogger intlogger) {
        try {
            response.sendError(errorCode);
        } catch (IOException ioe) {
            intlogger.error("IOException", ioe);
        }
    }

    /**
     * Method to check to see if file is of type gzip.
     *
     * @param file The name of the file to be checked
     * @return True if the file is of type gzip
     */
    public static boolean isFiletypeGzip(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
                GZIPInputStream gzip = new GZIPInputStream(fileInputStream)) {

            return true;
        } catch (IOException e) {
            eelfLogger.error("NODE0403 " + file.toString() + " Not in gzip(gz) format: " + e.toString() + e);
            return false;
        }
    }


    private static boolean loadKeyStore(String ksfile, String kspass, KeyStore ks)
            throws NoSuchAlgorithmException, CertificateException {
        try (FileInputStream fileInputStream = new FileInputStream(ksfile)) {
            ks.load(fileInputStream, kspass.toCharArray());
        } catch (IOException ioException) {
            eelfLogger.error("IOException occurred while opening FileInputStream: " + ioException.getMessage(),
                    ioException);
            return true;
        }
        return false;
    }


    private static String getNameFromSubject(KeyStore ks, Enumeration<String> aliases) throws KeyStoreException {
        String alias = aliases.nextElement();
        if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            if (cert != null) {
                String subject = cert.getSubjectX500Principal().getName();
                try {
                    LdapName ln = new LdapName(subject);
                    for (Rdn rdn : ln.getRdns()) {
                        if (rdn.getType().equalsIgnoreCase("CN")) {
                            return rdn.getValue().toString();
                        }
                    }
                } catch (InvalidNameException e) {
                    eelfLogger.error("No valid CN not found for dr-node cert", e);
                }
            }
        }
        return null;
    }
}
