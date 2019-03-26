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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.apache.log4j.Logger;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

import static com.att.eelf.configuration.Configuration.*;
import static org.onap.dmaap.datarouter.node.NodeUtils.isFiletypeGzip;

/**
 * A file to be delivered to a destination.
 * <p>
 * A Delivery task represents a work item for the data router - a file that
 * needs to be delivered and provides mechanisms to get information about
 * the file and its delivery data as well as to attempt delivery.
 */
public class DeliveryTask implements Runnable, Comparable<DeliveryTask> {
    private static Logger loggerDeliveryTask = Logger.getLogger("org.onap.dmaap.datarouter.node.DeliveryTask");
    private static EELFLogger eelflogger = EELFManager.getInstance()
            .getLogger(DeliveryTask.class);
    private DeliveryTaskHelper deliveryTaskHelper;
    private String pubid;
    private DestInfo destInfo;
    private String spool;
    private File datafile;
    private File metafile;
    private long length;
    private long date;
    private String method;
    private String fileid;
    private String ctype;
    private String url;
    private String feedid;
    private String subid;
    private int attempts;
    private boolean followRedirects;
    private String[][] hdrs;
    private String newInvocationId;


    /**
     * Create a delivery task for a given delivery queue and pub ID
     *
     * @param deliveryTaskHelper The delivery task helper for the queue this task is in.
     * @param pubid              The publish ID for this file.  This is used as
     *                           the base for the file name in the spool directory and is of
     *                           the form <milliseconds since 1970>.<fqdn of initial data router node>
     */
    public DeliveryTask(DeliveryTaskHelper deliveryTaskHelper, String pubid) {
        this.deliveryTaskHelper = deliveryTaskHelper;
        this.pubid = pubid;
        destInfo = deliveryTaskHelper.getDestinationInfo();
        subid = destInfo.getSubId();
        this.followRedirects = destInfo.isFollowRedirects();
        feedid = destInfo.getLogData();
        spool = destInfo.getSpool();
        String dfn = spool + "/" + pubid;
        String mfn = dfn + ".M";
        datafile = new File(spool + "/" + pubid);
        metafile = new File(mfn);
        boolean monly = destInfo.isMetaDataOnly();
        date = Long.parseLong(pubid.substring(0, pubid.indexOf('.')));
        Vector<String[]> hdrv = new Vector<>();

        try (BufferedReader br = new BufferedReader(new FileReader(metafile))) {
            String s = br.readLine();
            int i = s.indexOf('\t');
            method = s.substring(0, i);
            NodeUtils.setIpAndFqdnForEelf(method);
            if (!"DELETE".equals(method) && !monly) {
                length = datafile.length();
            }
            fileid = s.substring(i + 1);
            while ((s = br.readLine()) != null) {
                i = s.indexOf('\t');
                String h = s.substring(0, i);
                String v = s.substring(i + 1);
                if ("x-dmaap-dr-routing".equalsIgnoreCase(h)) {
                    subid = v.replaceAll("[^ ]*/", "");
                    feedid = deliveryTaskHelper.getFeedId(subid.replaceAll(" .*", ""));
                }
                if (length == 0 && h.toLowerCase().startsWith("content-")) {
                    continue;
                }
                if (h.equalsIgnoreCase("content-type")) {
                    ctype = v;
                }
                if (h.equalsIgnoreCase("x-onap-requestid")) {
                    MDC.put(MDC_KEY_REQUEST_ID, v);
                }
                if (h.equalsIgnoreCase("x-invocationid")) {
                    MDC.put("InvocationId", v);
                    v = UUID.randomUUID().toString();
                    newInvocationId = v;
                }
                hdrv.add(new String[]{h, v});
            }
        } catch (Exception e) {
            loggerDeliveryTask.error("Exception "+ Arrays.toString(e.getStackTrace()), e);
        }
        hdrs = hdrv.toArray(new String[hdrv.size()][]);
        url = deliveryTaskHelper.getDestURL(fileid);
    }

    /**
     * Is the object a DeliveryTask with the same publication ID?
     */
    public boolean equals(Object o) {
        if (!(o instanceof DeliveryTask)) {
            return (false);
        }
        return (pubid.equals(((DeliveryTask) o).pubid));
    }

    /**
     * Compare the publication IDs.
     */
    public int compareTo(DeliveryTask o) {
        return (pubid.compareTo(o.pubid));
    }

    /**
     * Get the hash code of the publication ID.
     */
    public int hashCode() {
        return (pubid.hashCode());
    }

    /**
     * Return the publication ID.
     */
    public String toString() {
        return (pubid);
    }

    /**
     * Get the publish ID
     */
    public String getPublishId() {
        return (pubid);
    }

    /**
     * Attempt delivery
     */
    public void run() {
        attempts++;
        try {
            destInfo = deliveryTaskHelper.getDestinationInfo();
            boolean expect100 = destInfo.isUsing100();
            boolean monly = destInfo.isMetaDataOnly();
            length = 0;
            if (!"DELETE".equals(method) && !monly) {
                length = datafile.length();
            }
            if (destInfo.isDecompress() && isFiletypeGzip(datafile) && fileid.endsWith(".gz")){
                    fileid = fileid.replace(".gz", "");
            }
            url = deliveryTaskHelper.getDestURL(fileid);
            URL u = new URL(url);
            HttpURLConnection uc = (HttpURLConnection) u.openConnection();
            uc.setConnectTimeout(60000);
            uc.setReadTimeout(60000);
            uc.setInstanceFollowRedirects(false);
            uc.setRequestMethod(method);
            uc.setRequestProperty("Content-Length", Long.toString(length));
            uc.setRequestProperty("Authorization", destInfo.getAuth());
            uc.setRequestProperty("X-DMAAP-DR-PUBLISH-ID", pubid);
            for (String[] nv : hdrs) {
                uc.addRequestProperty(nv[0], nv[1]);
            }
            if (length > 0) {
                if (expect100) {
                    uc.setRequestProperty("Expect", "100-continue");
                }
                uc.setDoOutput(true);
                if (destInfo.isDecompress()) {
                    if (isFiletypeGzip(datafile)) {
                        sendDecompressedFile(uc);
                    } else {
                        uc.setRequestProperty("Decompression_Status", "UNSUPPORTED_FORMAT");
                        sendFile(uc);
                    }
                } else {
                    sendFile(uc);
                }
            }
            int rc = uc.getResponseCode();
            String rmsg = uc.getResponseMessage();
            if (rmsg == null) {
                String h0 = uc.getHeaderField(0);
                if (h0 != null) {
                    int i = h0.indexOf(' ');
                    int j = h0.indexOf(' ', i + 1);
                    if (i != -1 && j != -1) {
                        rmsg = h0.substring(j + 1);
                    }
                }
            }
            String xpubid = null;
            InputStream is;
            if (rc >= 200 && rc <= 299) {
                is = uc.getInputStream();
                xpubid = uc.getHeaderField("X-DMAAP-DR-PUBLISH-ID");
            } else {
                if (rc >= 300 && rc <= 399) {
                    rmsg = uc.getHeaderField("Location");
                }
                is = uc.getErrorStream();
            }
            byte[] buf = new byte[4096];
            if (is != null) {
                while (is.read(buf) > 0) {
                }
                is.close();
            }
            deliveryTaskHelper.reportStatus(this, rc, xpubid, rmsg);
        } catch (Exception e) {
            loggerDeliveryTask.error("Exception " + Arrays.toString(e.getStackTrace()), e);
            deliveryTaskHelper.reportException(this, e);
        }
    }

    /**
     * To send decompressed gzip to the subscribers
     *
     * @param httpURLConnection connection used to make request
     * @throws IOException
     */
    private void sendDecompressedFile(HttpURLConnection httpURLConnection) throws IOException {
        byte[] buffer = new byte[8164];
        httpURLConnection.setRequestProperty("Decompression_Status", "SUCCESS");
        OutputStream outputStream = getOutputStream(httpURLConnection);
        if (outputStream != null) {
            int bytesRead = 0;
            try (InputStream gzipInputStream = new GZIPInputStream(new FileInputStream(datafile))) {
                int bufferLength = buffer.length;
                while ((bytesRead = gzipInputStream.read(buffer, 0, bufferLength)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
            } catch (IOException e) {
                httpURLConnection.setRequestProperty("Decompression_Status", "FAILURE");
                loggerDeliveryTask.info("Could not decompress file");
                sendFile(httpURLConnection);
            }

        }
    }

    /**
     * To send any file to the subscriber.
     *
     * @param httpURLConnection connection used to make request
     * @throws IOException
     */
    private void sendFile(HttpURLConnection httpURLConnection) throws IOException {
        OutputStream os = getOutputStream(httpURLConnection);
        if (os != null) {
            long sofar = 0;
            try (InputStream is = new FileInputStream(datafile)) {
                byte[] buf = new byte[1024 * 1024];
                while (sofar < length) {
                    int i = buf.length;
                    if (sofar + i > length) {
                        i = (int) (length - sofar);
                    }
                    i = is.read(buf, 0, i);
                    if (i <= 0) {
                        throw new IOException("Unexpected problem reading data file " + datafile);
                    }
                    sofar += i;
                    os.write(buf, 0, i);
                }
                os.close();
            } catch (IOException ioe) {
                deliveryTaskHelper.reportDeliveryExtra(this, sofar);
                throw ioe;
            }
        }
    }

    /**
     * Get the outputstream that will be used to send data
     *
     * @param httpURLConnection connection used to make request
     * @return AN Outpustream that can be used to send your data.
     * @throws IOException
     */
    private OutputStream getOutputStream(HttpURLConnection httpURLConnection) throws IOException {
        OutputStream outputStream = null;

        try {
            outputStream = httpURLConnection.getOutputStream();
        } catch (ProtocolException pe) {
            deliveryTaskHelper.reportDeliveryExtra(this, -1L);
            // Rcvd error instead of 100-continue
            loggerDeliveryTask.error("Exception " + Arrays.toString(pe.getStackTrace()), pe);
        }
        return outputStream;
    }

    /**
     * Remove meta and data files
     */
    public void clean() {
        datafile.delete();
        metafile.delete();
        eelflogger.info(EelfMsgs.INVOKE, newInvocationId);
        eelflogger.info(EelfMsgs.EXIT);
        hdrs = null;
    }

    /**
     * Has this delivery task been cleaned?
     */
    public boolean isCleaned() {
        return (hdrs == null);
    }

    /**
     * Get length of body
     */
    public long getLength() {
        return (length);
    }

    /**
     * Get creation date as encoded in the publish ID.
     */
    public long getDate() {
        return (date);
    }

    /**
     * Get the most recent delivery attempt URL
     */
    public String getURL() {
        return (url);
    }

    /**
     * Get the content type
     */
    public String getCType() {
        return (ctype);
    }

    /**
     * Get the method
     */
    public String getMethod() {
        return (method);
    }

    /**
     * Get the file ID
     */
    public String getFileId() {
        return (fileid);
    }

    /**
     * Get the number of delivery attempts
     */
    public int getAttempts() {
        return (attempts);
    }

    /**
     * Get the (space delimited list of) subscription ID for this delivery task
     */
    public String getSubId() {
        return (subid);
    }

    /**
     * Get the feed ID for this delivery task
     */
    public String getFeedId() {
        return (feedid);
    }

    /**
     * Get the followRedirects for this delivery task
     */
    public boolean getFollowRedirects() {
        return(followRedirects);
    }
}
