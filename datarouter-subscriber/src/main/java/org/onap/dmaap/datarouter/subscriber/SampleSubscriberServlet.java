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

package org.onap.dmaap.datarouter.subscriber;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleSubscriberServlet extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(SampleSubscriberServlet.class);

    private static String outputDirectory;
    private static String basicAuth;

    /**
     * Configure the SampleSubscriberServlet.
     *
     * <ul>
     * <li>Login - The login expected in the Authorization header (default "LOGIN").
     * <li>Password - The password expected in the Authorization header (default "PASSWORD").
     * <li>outputDirectory - The directory where files are placed (default
     * "/opt/app/subscriber/delivery").
     * </ul>
     */
    @Override
    public void init() {
        SubscriberProps props = SubscriberProps.getInstance();
        String login = props.getValue("org.onap.dmaap.datarouter.subscriber.auth.user", "LOGIN");
        String password = props.getValue("org.onap.dmaap.datarouter.subscriber.auth.password", "PASSWORD");
        outputDirectory =
                props.getValue("org.onap.dmaap.datarouter.subscriber.delivery.dir", "/opt/app/subscriber/delivery");
        try {
            Files.createDirectory(Paths.get(outputDirectory));
        } catch (IOException e) {
            logger.error("SubServlet: Failed to create delivery dir: " + e.getMessage(), e);
        }
        basicAuth = "Basic " + Base64.encodeBase64String((login + ":" + password).getBytes());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            common(req, resp, false);
        } catch (IOException e) {
            logger.error("SampleSubServlet: Failed to doPut: " + req.getRemoteAddr() + " : " + req.getPathInfo(), e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            common(req, resp, true);
        } catch (IOException e) {
            logger.error("SampleSubServlet: Failed to doDelete: " + req.getRemoteAddr() + " : " + req.getPathInfo(), e);
        }
    }

    /**
     * Process a PUT or DELETE request.
     *
     * <ol>
     * <li>Verify that the request contains an Authorization header or else UNAUTHORIZED.
     * <li>Verify that the Authorization header matches the configured Login and Password or else
     * FORBIDDEN.
     * <li>If the request is PUT, store the message body as a file in the configured outputDirectory
     * directory protecting against evil characters in the received FileID. The file is created
     * initially with its name prefixed with a ".", and once it is complete, it is renamed to
     * remove the leading "." character.
     * <li>If the request is DELETE, instead delete the file (if it exists) from the configured
     * outputDirectory directory.
     * <li>Respond with NO_CONTENT.
     * </ol>
     */
    private void common(HttpServletRequest req, HttpServletResponse resp, boolean isdelete) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            logger.info("SampleSubServlet: Rejecting request with no Authorization header from " + req.getRemoteAddr()
                                + ": " + req.getPathInfo());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!basicAuth.equals(authHeader)) {
            logger.error("SampleSubServlet: Rejecting request with incorrect Authorization header from "
                                + req.getRemoteAddr() + ": " + req.getPathInfo());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String fileid = req.getPathInfo();
        fileid = fileid.substring(fileid.lastIndexOf('/') + 1);
        String queryString = req.getQueryString();
        if (queryString != null) {
            fileid = fileid + "?" + queryString;
        }
        String publishid = req.getHeader("X-DMAAP-DR-PUBLISH-ID");
        String filename = URLEncoder.encode(fileid, StandardCharsets.UTF_8).replaceAll("^\\.", "%2E").replaceAll("\\*", "%2A");
        String fullPath = outputDirectory + "/" + filename;
        String tmpPath = outputDirectory + "/." + filename;
        String fullMetaDataPath = outputDirectory + "/" + filename + ".M";
        String tmpMetaDataPath = outputDirectory + "/." + filename + ".M";
        try {
            if (isdelete) {
                Files.deleteIfExists(Paths.get(fullPath));
                Files.deleteIfExists(Paths.get(fullMetaDataPath));
                logger.info("SampleSubServlet: Received delete for file id " + fileid + " from " + req.getRemoteAddr()
                                    + " publish id " + publishid + " as " + fullPath);
            } else {
                new File(tmpPath).createNewFile();
                new File(tmpMetaDataPath).createNewFile();
                try (InputStream is = req.getInputStream(); OutputStream os = new FileOutputStream(tmpPath)) {
                    byte[] buf = new byte[65536];
                    int bufferSize;
                    while ((bufferSize = is.read(buf)) > 0) {
                        os.write(buf, 0, bufferSize);
                    }
                }
                Files.move(Paths.get(tmpPath), Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
                try (PrintWriter writer = new PrintWriter(new FileOutputStream(tmpMetaDataPath))) {
                    String metaData = req.getHeader("X-DMAAP-DR-META");
                    writer.print(metaData);
                }
                Files.move(Paths.get(tmpMetaDataPath), Paths.get(fullMetaDataPath),
                        StandardCopyOption.REPLACE_EXISTING);
                logger.info(
                        "SampleSubServlet: Received file id " + fileid + " from " + req.getRemoteAddr() + " publish id "
                                + publishid + " as " + fullPath);
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (IOException ioe) {
            Files.deleteIfExists(Paths.get(tmpPath));
            Files.deleteIfExists(Paths.get(tmpMetaDataPath));
            logger.error("SampleSubServlet: Failed to process file " + fullPath + " from " + req.getRemoteAddr() + ": "
                                + req.getPathInfo());
            throw ioe;
        }
    }
}
