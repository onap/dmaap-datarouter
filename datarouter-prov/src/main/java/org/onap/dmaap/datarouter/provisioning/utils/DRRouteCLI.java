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

package org.onap.dmaap.datarouter.provisioning.utils;

import static java.lang.System.exit;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;

/**
 * This class provides a Command Line Interface for the routing tables in the DR Release 2.0 DB.
 * A full description of this command is <a href="http://wiki.proto.research.att.com/doku.php?id=datarouter-route-cli">here</a>.
 *
 * @author Robert Eby
 * @version $Id: DRRouteCLI.java,v 1.2 2013/11/05 15:54:16 eby Exp $
 */
public class DRRouteCLI {
    /**
     * Invoke the CLI.  The CLI can be run with a single command (given as command line arguments),
     * or in an interactive mode where the user types a sequence of commands to the program.  The CLI is invoked via:
     * <pre>
     * java org.onap.dmaap.datarouter.provisioning.utils.DRRouteCLI [ -s <i>server</i> ] [ <i>command</i> ]
     * </pre>
     * A full description of the arguments to this command are
     * <a href="http://wiki.proto.research.att.com/doku.php?id=datarouter-route-cli">here</a>.
     *
     * @param args command line arguments
     * @throws Exception for any unrecoverable problem
     */
    public static void main(String[] args) throws Exception {
        String server = System.getenv(ENV_VAR);
        if (args.length >= 2 && args[0].equals("-s")) {
            server = args[1];
            String[] str = new String[args.length - 2];
            if (str.length > 0) {
                System.arraycopy(args, 2, str, 0, str.length);
            }
            args = str;
        }
        if (server == null || server.equals("")) {
            System.err.println("dr-route: you need to specify a server, either via $PROVSRVR or the '-s' option.");
            System.exit(1);
        }
        DRRouteCLI cli = new DRRouteCLI(server);
        if (args.length > 0) {
            boolean bool = cli.runCommand(args);
            System.exit(bool ? 0 : 1);
        } else {
            cli.interactive();
            System.exit(0);
        }
    }

    private static final String ENV_VAR = "PROVSRVR";
    private static final String PROMPT = "dr-route> ";
    private static final String DEFAULT_TRUSTSTORE_PATH = /* $JAVA_HOME + */ "/jre/lib/security/cacerts";
    private static final EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");

    private final String server;
    private int width = 120;        // screen width (for list)
    private AbstractHttpClient httpclient;

    /**
     * Create a DRRouteCLI object connecting to the specified server.
     *
     * @param server the server to send command to
     * @throws Exception generic exception
     */
    public DRRouteCLI(String server) throws Exception {
        this.server = server;
        this.httpclient = new DefaultHttpClient();
        AafPropsUtils aafPropsUtils = null;

        Properties provProperties = ProvRunner.getProvProperties();
        try {
            aafPropsUtils = new AafPropsUtils(new File(provProperties.getProperty(
                "org.onap.dmaap.datarouter.provserver.aafprops.path",
                "/opt/app/osaaf/local/org.onap.dmaap-dr.props")));
        } catch (IOException e) {
            intlogger.error("NODE0314 Failed to load AAF props. Exiting", e);
            exit(1);
        }

        String truststoreFile = aafPropsUtils.getTruststorePathProperty();
        String truststorePw = aafPropsUtils.getTruststorePassProperty();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        if (truststoreFile == null || truststoreFile.equals("")) {
            String jhome = System.getenv("JAVA_HOME");
            if (jhome == null || jhome.equals("")) {
                jhome = "/opt/java/jdk/jdk180";
            }
            truststoreFile = jhome + DEFAULT_TRUSTSTORE_PATH;
        }
        File file = new File(truststoreFile);
        if (file.exists()) {
            FileInputStream instream = new FileInputStream(file);
            try {
                trustStore.load(instream, truststorePw.toCharArray());
            } catch (Exception x) {
                intlogger.error("Problem reading truststore: " + x.getMessage(), x);
                throw x;
            } finally {
                try {
                    instream.close();
                } catch (Exception e) {
                    intlogger.error("Ignore error closing input stream: " + e.getMessage(), e);
                }
            }
        }

        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        Scheme sch = new Scheme("https", 443, socketFactory);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);
    }

    private void interactive() throws IOException {
        LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(PROMPT);
            String line = in.readLine();
            if (line == null) {
                return;
            }
            line = line.trim();
            if (line.equalsIgnoreCase("exit")) {    // "exit" may only be used in interactive mode
                return;
            }
            if (line.equalsIgnoreCase("quit")) {   // "quit" may only be used in interactive mode
                return;
            }
            String[] args = line.split("[ \t]+");
            if (args.length > 0) {
                runCommand(args);
            }
        }
    }

    /**
     * Run the command specified by the arguments.
     *
     * @param args The command line arguments.
     * @return true if the command was valid and succeeded
     */
    boolean runCommand(String[] args) {
        String cmd = args[0].trim().toLowerCase();
        if (cmd.equals("add")) {
            if (args.length > 2) {
                if (args[1].startsWith("in") && args.length >= 6) {
                    return addIngress(args);
                }
                if (args[1].startsWith("eg") && args.length == 4) {
                    return addEgress(args);
                }
                if (args[1].startsWith("ne") && args.length == 5) {
                    return addRoute(args);
                }
            }
            System.err.println("Add command should be one of:");
            System.err.println("  add in[gress] feedid user subnet nodepatt [ seq ]");
            System.err.println("  add eg[ress]  subid node");
            System.err.println("  add ne[twork] fromnode tonode vianode");
        } else if (cmd.startsWith("del")) {
            if (args.length > 2) {
                if (args[1].startsWith("in") && args.length == 5) {
                    return delIngress(args);
                }
                if (args[1].startsWith("in") && args.length == 3) {
                    return delIngress(args);
                }
                if (args[1].startsWith("eg") && args.length == 3) {
                    return delEgress(args);
                }
                if (args[1].startsWith("ne") && args.length == 4) {
                    return delRoute(args);
                }
            }
            System.err.println("Delete command should be one of:");
            System.err.println("  del in[gress] feedid user subnet");
            System.err.println("  del in[gress] seq");
            System.err.println("  del eg[ress]  subid");
            System.err.println("  del ne[twork] fromnode tonode");
        } else if (cmd.startsWith("lis")) {
            return list(args);
        } else if (cmd.startsWith("wid") && args.length > 1) {
            width = Integer.parseInt(args[1]);
            return true;
        } else if (cmd.startsWith("?") || cmd.startsWith("hel") || cmd.startsWith("usa")) {
            usage();
        } else if (cmd.startsWith("#")) {
            // comment -- ignore
        } else {
            System.err.println("Command should be one of add, del, list, exit, quit");
        }
        return false;
    }

    private void usage() {
        System.out.println("Enter one of the following commands:");
        System.out.println("  add in[gress] feedid user subnet nodepatt [ seq ]");
        System.out.println("  add eg[ress]  subid node");
        System.out.println("  add ne[twork] fromnode tonode vianode");
        System.out.println("  del in[gress] feedid user subnet");
        System.out.println("  del in[gress] seq");
        System.out.println("  del eg[ress]  subid");
        System.out.println("  del ne[twork] fromnode tonode");
        System.out.println("  list [ all | ingress | egress | network ]");
        System.out.println("  exit");
        System.out.println("  quit");
    }

    private boolean addIngress(String[] args) {
        String url = String.format("https://%s/internal/route/ingress/?feed=%s&user=%s&subnet=%s&nodepatt=%s", server, args[2], args[3], args[4], args[5]);
        if (args.length > 6) {
            url += "&seq=" + args[6];
        }
        return doPost(url);
    }

    private boolean addEgress(String[] args) {
        String url = String.format("https://%s/internal/route/egress/?sub=%s&node=%s", server, args[2], args[3]);
        return doPost(url);
    }

    private boolean addRoute(String[] args) {
        String url = String.format("https://%s/internal/route/network/?from=%s&to=%s&via=%s", server, args[2], args[3], args[4]);
        return doPost(url);
    }

    private boolean delIngress(String[] args) {
        String url;
        if (args.length == 5) {
            String subnet = args[4].replaceAll("/", "!");    // replace the / with a !
            url = String.format("https://%s/internal/route/ingress/%s/%s/%s", server, args[2], args[3], subnet);
        } else {
            url = String.format("https://%s/internal/route/ingress/%s", server, args[2]);
        }
        return doDelete(url);
    }

    private boolean delEgress(String[] args) {
        String url = String.format("https://%s/internal/route/egress/%s", server, args[2]);
        return doDelete(url);
    }

    private boolean delRoute(String[] args) {
        String url = String.format("https://%s/internal/route/network/%s/%s", server, args[2], args[3]);
        return doDelete(url);
    }

    private boolean list(String[] args) {
        String tbl = (args.length == 1) ? "all" : args[1].toLowerCase();
        JSONObject jo = doGet("https://" + server + "/internal/route/");    // Returns all 3 tables
        StringBuilder sb = new StringBuilder();
        if (tbl.startsWith("al") || tbl.startsWith("in")) {
            // Display the IRT
            JSONArray irt = jo.optJSONArray("ingress");
            int cw1 = 6;
            int cw2 = 6;
            int cw3 = 6;
            int cw4 = 6;        // determine column widths for first 4 cols
            for (int i = 0; irt != null && i < irt.length(); i++) {
                JSONObject jsonObject = irt.getJSONObject(i);
                cw1 = Math.max(cw1, ("" + jsonObject.getInt("seq")).length());
                cw2 = Math.max(cw2, ("" + jsonObject.getInt("feedid")).length());
                String str = jsonObject.optString("user");
                cw3 = Math.max(cw3, (str == null) ? 1 : str.length());
                str = jsonObject.optString("subnet");
                cw4 = Math.max(cw4, (str == null) ? 1 : str.length());
            }

            int nblank = cw1 + cw2 + cw3 + cw4 + 8;
            sb.append("Ingress Routing Table\n");
            sb.append(String.format("%s  %s  %s  %s  Nodes\n", ext("Seq", cw1),
                    ext("FeedID", cw2), ext("User", cw3), ext("Subnet", cw4)));
            for (int i = 0; irt != null && i < irt.length(); i++) {
                JSONObject jsonObject = irt.getJSONObject(i);
                String seq = "" + jsonObject.getInt("seq");
                String feedid = "" + jsonObject.getInt("feedid");
                String user = jsonObject.optString("user");
                String subnet = jsonObject.optString("subnet");
                if (user.equals("")) {
                    user = "-";
                }
                if (subnet.equals("")) {
                    subnet = "-";
                }
                JSONArray nodes = jsonObject.getJSONArray("node");
                int sol = sb.length();
                sb.append(String.format("%s  %s  %s  %s  ", ext(seq, cw1),
                        ext(feedid, cw2), ext(user, cw3), ext(subnet, cw4)));
                for (int j = 0; j < nodes.length(); j++) {
                    String nd = nodes.getString(j);
                    int cursor = sb.length() - sol;
                    if (j > 0 && (cursor + nd.length() > width)) {
                        sb.append("\n");
                        sol = sb.length();
                        sb.append(ext(" ", nblank));
                    }
                    sb.append(nd);
                    if ((j + 1) < nodes.length()) {
                        sb.append(", ");
                    }
                }
                sb.append("\n");
            }
        }
        if (tbl.startsWith("al") || tbl.startsWith("eg")) {
            // Display the ERT
            JSONObject ert = jo.optJSONObject("egress");
            String[] subs = (ert == null) ? new String[0] : JSONObject.getNames(ert);
            if (subs == null) {
                subs = new String[0];
            }
            Arrays.sort(subs);
            int cw1 = 5;
            for (int i = 0; i < subs.length; i++) {
                cw1 = Math.max(cw1, subs[i].length());
            }

            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Egress Routing Table\n");
            sb.append(String.format("%s  Node\n", ext("SubID", cw1)));
            for (int i = 0; i < subs.length; i++) {
                if (ert != null && ert.length() != 0 ) {
                    String node = ert.getString(subs[i]);
                    sb.append(String.format("%s  %s\n", ext(subs[i], cw1), node));
                }

            }
        }
        if (tbl.startsWith("al") || tbl.startsWith("ne")) {
            // Display the NRT
            JSONArray nrt = jo.optJSONArray("routing");
            int cw1 = 4;
            int cw2 = 4;
            for (int i = 0; nrt != null && i < nrt.length(); i++) {
                JSONObject jsonObject = nrt.getJSONObject(i);
                String from = jsonObject.getString("from");
                String to = jsonObject.getString("to");
                cw1 = Math.max(cw1, from.length());
                cw2 = Math.max(cw2, to.length());
            }

            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Network Routing Table\n");
            sb.append(String.format("%s  %s  Via\n", ext("From", cw1), ext("To", cw2)));
            for (int i = 0; nrt != null && i < nrt.length(); i++) {
                JSONObject jsonObject = nrt.getJSONObject(i);
                String from = jsonObject.getString("from");
                String to = jsonObject.getString("to");
                String via = jsonObject.getString("via");
                sb.append(String.format("%s  %s  %s\n", ext(from, cw1), ext(to, cw2), via));
            }
        }
        System.out.print(sb.toString());
        return true;
    }

    private String ext(String str, int num) {
        if (str == null) {
            str = "-";
        }
        while (str.length() < num) {
            str += " ";
        }
        return str;
    }

    private boolean doDelete(String url) {
        boolean rv = false;
        HttpDelete meth = new HttpDelete(url);
        try {
            HttpResponse response = httpclient.execute(meth);
            HttpEntity entity = response.getEntity();
            StatusLine sl = response.getStatusLine();
            rv = (sl.getStatusCode() == HttpServletResponse.SC_OK);
            if (rv) {
                System.out.println("Routing entry deleted.");
                EntityUtils.consume(entity);
            } else {
                printErrorText(entity);
            }
        } catch (Exception e) {
            intlogger.error("PROV0006 doDelete: " + e.getMessage(), e);
        } finally {
            meth.releaseConnection();
        }
        return rv;
    }

    private JSONObject doGet(String url) {
        JSONObject rv = new JSONObject();
        HttpGet meth = new HttpGet(url);
        try {
            HttpResponse response = httpclient.execute(meth);
            HttpEntity entity = response.getEntity();
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpServletResponse.SC_OK) {
                rv = new JSONObject(new JSONTokener(entity.getContent()));
            } else {
                printErrorText(entity);
            }
        } catch (Exception e) {
            intlogger.error("PROV0005 doGet: " + e.getMessage(), e);
        } finally {
            meth.releaseConnection();
        }
        return rv;
    }

    private boolean doPost(String url) {
        boolean rv = false;
        HttpPost meth = new HttpPost(url);
        try {
            HttpResponse response = httpclient.execute(meth);
            HttpEntity entity = response.getEntity();
            StatusLine sl = response.getStatusLine();
            rv = (sl.getStatusCode() == HttpServletResponse.SC_OK);
            if (rv) {
                System.out.println("Routing entry added.");
                EntityUtils.consume(entity);
            } else {
                printErrorText(entity);
            }
        } catch (Exception e) {
            intlogger.error("PROV0009 doPost: " + e.getMessage(), e);
        } finally {
            meth.releaseConnection();
        }
        return rv;
    }

    private void printErrorText(HttpEntity entity) throws IOException {
        // Look for and print only the part of the output between <pre>...</pre>
        InputStream is = entity.getContent();
        StringBuilder sb = new StringBuilder();
        byte[] bite = new byte[512];
        int num;
        while ((num = is.read(bite)) > 0) {
            sb.append(new String(bite, 0, num));
        }
        is.close();
        int ix = sb.indexOf("<pre>");
        if (ix > 0) {
            sb.delete(0, ix + 5);
        }
        ix = sb.indexOf("</pre>");
        if (ix > 0) {
            sb.delete(ix, sb.length());
        }
        System.err.println(sb.toString());
    }
}
