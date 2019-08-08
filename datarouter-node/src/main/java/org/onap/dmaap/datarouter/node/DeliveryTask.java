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
import static org.onap.dmaap.datarouter.node.NodeUtils.isFiletypeGzip;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.Nullable;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;
import org.slf4j.MDC;

/**
 * A file to be delivered to a destination.
 *
 * <p>A Delivery task represents a work item for the data router - a file that needs to be delivered and provides
 * mechanisms to get information about the file and its delivery data as well as to attempt delivery.
 */
public class DeliveryTask implements Runnable, Comparable<DeliveryTask> {

    private static final String DECOMPRESSION_STATUS = "Decompression_Status";
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(DeliveryTask.class);
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
    private long resumeTime;


    /**
     * Create a delivery task for a given delivery queue and pub ID.
     *
     * @param deliveryTaskHelper The delivery task helper for the queue this task is in.
     * @param pubid The publish ID for this file.  This is used as the base for the file name in the spool directory and
     *      is of the form (milliseconds since 1970).(fqdn of initial data router node)
     */
    DeliveryTask(DeliveryTaskHelper deliveryTaskHelper, String pubid) {
        this.deliveryTaskHelper = deliveryTaskHelper;
        this.pubid = pubid;
        destInfo = deliveryTaskHelper.getDestinationInfo();
        subid = destInfo.getSubId();
        this.followRedirects = destInfo.isFollowRedirects();
        feedid = destInfo.getLogData();
        spool = destInfo.getSpool();
        String dfn = spool + File.separator + pubid;
        String mfn = dfn + ".M";
        datafile = new File(spool + File.separator + pubid);
        metafile = new File(mfn);
        boolean monly = destInfo.isMetaDataOnly();
        date = Long.parseLong(pubid.substring(0, pubid.indexOf('.')));
        resumeTime = System.currentTimeMillis();
        ArrayList<String[]> hdrv = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(metafile))) {
            String line = br.readLine();
            int index = line.indexOf('\t');
            method = line.substring(0, index);
            NodeUtils.setIpAndFqdnForEelf(method);
            if (!"DELETE".equals(method) && !monly) {
                length = datafile.length();
            }
            fileid = line.substring(index + 1);
            while ((line = br.readLine()) != null) {
                index = line.indexOf('\t');
                String header = line.substring(0, index);
                String headerValue = line.substring(index + 1);
                if ("x-dmaap-dr-routing".equalsIgnoreCase(header)) {
                    subid = headerValue.replaceAll("[^ ]*/", "");
                    feedid = deliveryTaskHelper.getFeedId(subid.replaceAll(" .*", ""));
                }
                if (length == 0 && header.toLowerCase().startsWith("content-")) {
                    continue;
                }
                if ("content-type".equalsIgnoreCase(header)) {
                    ctype = headerValue;
                }
                if ("x-onap-requestid".equalsIgnoreCase(header)) {
                    MDC.put(MDC_KEY_REQUEST_ID, headerValue);
                }
                if ("x-invocationid".equalsIgnoreCase(header)) {
                    MDC.put("InvocationId", headerValue);
                    headerValue = UUID.randomUUID().toString();
                    newInvocationId = headerValue;
                }
                hdrv.add(new String[]{header, headerValue});
            }
        } catch (Exception e) {
            eelfLogger.error("Exception", e);
        }
        hdrs = hdrv.toArray(new String[hdrv.size()][]);
        url = deliveryTaskHelper.getDestURL(fileid);
    }

    /**
     * Is the object a DeliveryTask with the same publication ID.
     */
    public boolean equals(Object object) {
        if (!(object instanceof DeliveryTask)) {
            return (false);
        }
        return (pubid.equals(((DeliveryTask) object).pubid));
    }

    /**
     * Compare the publication IDs.
     */
    public int compareTo(DeliveryTask other) {
        return (pubid.compareTo(other.pubid));
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
     * Get the publish ID.
     */
    String getPublishId() {
        return (pubid);
    }

    /**
     * Attempt delivery.
     */
    public void run() {
        attempts++;
        try {
            destInfo = deliveryTaskHelper.getDestinationInfo();
            boolean monly = destInfo.isMetaDataOnly();
            length = 0;
            if (!"DELETE".equals(method) && !monly) {
                length = datafile.length();
            }
            stripSuffixIfIsDecompress();
            url = deliveryTaskHelper.getDestURL(fileid);
            URL urlObj = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setConnectTimeout(60000);
            urlConnection.setReadTimeout(60000);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod(method);
            urlConnection.setRequestProperty("Content-Length", Long.toString(length));
            urlConnection.setRequestProperty("Authorization", destInfo.getAuth());
            urlConnection.setRequestProperty("X-DMAAP-DR-PUBLISH-ID", pubid);
            boolean expect100 = destInfo.isUsing100();
            int rc = deliverFileToSubscriber(expect100, urlConnection);
            String rmsg = urlConnection.getResponseMessage();
            rmsg = getResponseMessage(urlConnection, rmsg);
            String xpubid = null;
            InputStream is;
            if (rc >= 200 && rc <= 299) {
                is = urlConnection.getInputStream();
                xpubid = urlConnection.getHeaderField("X-DMAAP-DR-PUBLISH-ID");
            } else {
                if (rc >= 300 && rc <= 399) {
                    rmsg = urlConnection.getHeaderField("Location");
                }
                is = urlConnection.getErrorStream();
            }
            byte[] buf = new byte[4096];
            if (is != null) {
                while (is.read(buf) > 0) {
                    //flush the buffer
                }
                is.close();
            }
            deliveryTaskHelper.reportStatus(this, rc, xpubid, rmsg);
        } catch (Exception e) {
            eelfLogger.error("Exception " + Arrays.toString(e.getStackTrace()), e);
            deliveryTaskHelper.reportException(this, e);
        }
    }

    /**
     * To send decompressed gzip to the subscribers.
     *
     * @param httpURLConnection connection used to make request
     */
    private void sendDecompressedFile(HttpURLConnection httpURLConnection) throws IOException {
        byte[] buffer = new byte[8164];
        httpURLConnection.setRequestProperty(DECOMPRESSION_STATUS, "SUCCESS");
        OutputStream outputStream = getOutputStream(httpURLConnection);
        if (outputStream != null) {
            int bytesRead;
            try (InputStream gzipInputStream = new GZIPInputStream(new FileInputStream(datafile))) {
                int bufferLength = buffer.length;
                while ((bytesRead = gzipInputStream.read(buffer, 0, bufferLength)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
            } catch (IOException e) {
                httpURLConnection.setRequestProperty(DECOMPRESSION_STATUS, "FAILURE");
                eelfLogger.info("Could not decompress file", e);
                sendFile(httpURLConnection);
            }

        }
    }

    /**
     * To send any file to the subscriber.
     *
     * @param httpURLConnection connection used to make request
     */
    private void sendFile(HttpURLConnection httpURLConnection) throws IOException {
        OutputStream os = getOutputStream(httpURLConnection);
        if (os == null) {
            return;
        }
        long sofar = 0;
        try (InputStream is = new FileInputStream(datafile)) {
            byte[] buf = new byte[1024 * 1024];
            while (sofar < length) {
                int len = buf.length;
                if (sofar + len > length) {
                    len = (int) (length - sofar);
                }
                len = is.read(buf, 0, len);
                if (len <= 0) {
                    throw new IOException("Unexpected problem reading data file " + datafile);
                }
                sofar += len;
                os.write(buf, 0, len);
            }
            os.close();
        } catch (IOException ioe) {
            deliveryTaskHelper.reportDeliveryExtra(this, sofar);
            throw ioe;
        }
    }

    /**
     * Get the outputstream that will be used to send data.
     *
     * @param httpURLConnection connection used to make request
     * @return AN Outpustream that can be used to send your data.
     */
    OutputStream getOutputStream(HttpURLConnection httpURLConnection) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = httpURLConnection.getOutputStream();
        } catch (ProtocolException pe) {
            deliveryTaskHelper.reportDeliveryExtra(this, -1L);
            // Rcvd error instead of 100-continue
            eelfLogger.error("Exception " + Arrays.toString(pe.getStackTrace()), pe);
        }
        return outputStream;
    }

    private void stripSuffixIfIsDecompress() {
        if (destInfo.isDecompress() && isFiletypeGzip(datafile) && fileid.endsWith(".gz")) {
            fileid = fileid.replace(".gz", "");
        }
    }

    private int deliverFileToSubscriber(boolean expect100, HttpURLConnection uc) throws IOException {
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
                    uc.setRequestProperty(DECOMPRESSION_STATUS, "UNSUPPORTED_FORMAT");
                    sendFile(uc);
                }
            } else {
                sendFile(uc);
            }
        }
        return uc.getResponseCode();
    }

    @Nullable
    private String getResponseMessage(HttpURLConnection uc, String rmsg) {
        if (rmsg == null) {
            String h0 = uc.getHeaderField(0);
            if (h0 != null) {
                int indexOfSpace1 = h0.indexOf(' ');
                int indexOfSpace2 = h0.indexOf(' ', indexOfSpace1 + 1);
                if (indexOfSpace1 != -1 && indexOfSpace2 != -1) {
                    rmsg = h0.substring(indexOfSpace2 + 1);
                }
            }
        }
        return rmsg;
    }

    /**
     * Remove meta and data files.
     */
    void clean() {
        deleteWithRetry(datafile);
        deleteWithRetry(metafile);
        eelfLogger.info(EelfMsgs.INVOKE, newInvocationId);
        eelfLogger.info(EelfMsgs.EXIT);
        hdrs = null;
    }

    private void deleteWithRetry(File file) {
        int maxTries = 3;
        int tryCount = 1;
        while (tryCount <= maxTries) {
            try {
                Files.deleteIfExists(file.toPath());
                break;
            } catch (IOException e) {
                eelfLogger.error("IOException : Failed to delete file :"
                                         + file.getName() + " on attempt " + tryCount, e);
            }
            tryCount++;
        }
    }

    /**
     * Get the resume time for a delivery task.
     */
    long getResumeTime() {
        return resumeTime;
    }

    /**
     * Set the resume time for a delivery task.
     */
    void setResumeTime(long resumeTime) {
        this.resumeTime = resumeTime;
    }

    /**
     * Has this delivery task been cleaned.
     */
    boolean isCleaned() {
        return (hdrs == null);
    }

    /**
     * Get length of body.
     */
    public long getLength() {
        return (length);
    }

    /**
     * Get creation date as encoded in the publish ID.
     */
    long getDate() {
        return (date);
    }

    /**
     * Get the most recent delivery attempt URL.
     */
    public String getURL() {
        return (url);
    }

    /**
     * Get the content type.
     */
    String getCType() {
        return (ctype);
    }

    /**
     * Get the method.
     */
    String getMethod() {
        return (method);
    }

    /**
     * Get the file ID.
     */
    String getFileId() {
        return (fileid);
    }

    /**
     * Get the number of delivery attempts.
     */
    int getAttempts() {
        return (attempts);
    }

    /**
     * Get the (space delimited list of) subscription ID for this delivery task.
     */
    String getSubId() {
        return (subid);
    }

    /**
     * Get the feed ID for this delivery task.
     */
    String getFeedId() {
        return (feedid);
    }

    /**
     * Get the followRedirects for this delivery task.
     */
    boolean getFollowRedirects() {
        return (followRedirects);
    }
}
