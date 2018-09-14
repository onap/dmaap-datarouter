package org.onap.dmaap.datarouter.provisioning.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.log4j.Logger;

public class HttpServletUtils {
    public static void sendResponseError(HttpServletResponse response, int errorCode, String message, Logger intlogger) {
        try {
            response.sendError(errorCode, message);
        } catch (IOException ioe) {
            intlogger.error("IOException" + ioe.getMessage());
        }
    }
}
