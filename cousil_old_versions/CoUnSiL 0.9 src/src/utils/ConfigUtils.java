package utils;

import core.Configurable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import networkRepresentation.EndpointNetworkNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class ConfigUtils {
    public static final String ConfigFileName = "nodeConfig.json";
    /**
     * @todo implement as function, relevant to XDG config
     */
    public static final String ConfigDir = ".";

    public static class ConfigManager {
        private static Map<String, Configurable> queryManager = new HashMap<>();

        public static void registerManager(String path, Configurable manager) {
            queryManager.put(path, manager);
        }
        private static void fetchAndMerge(JSONObject fullConfig, String path) throws JSONException {
            try {
                ConfigUtils.mergeConfig(fullConfig, queryManager.get(path).activeConfig());
            } catch (java.lang.NullPointerException ex) {
                Logger.getLogger(ConfigUtils.class.getName()).log(Level.ERROR, "BUG: Fetching config subtree for root node has requested configurable for path " + path + " before such was registered.");
            }
        }
        private static JSONObject getConfigForPath(String path) throws JSONException {
            if (path.equals(".")) {
                JSONObject fullConfig = new JSONObject();
                fetchAndMerge(fullConfig, ".localNode");
                fetchAndMerge(fullConfig, ".p2p");
                return fullConfig;
            } else {
                Configurable m = queryManager.get(path);
                if (m == null) return null;
                return m.activeConfig();
            }
        }
        public static Map.Entry<String, JSONObject> getClosestRootConfig(String path) throws JSONException {
            Map.Entry<String, Configurable> root = getClosestRootManager(path);
            if (root == null) return null;
            return new AbstractMap.SimpleEntry<String, JSONObject>(root.getKey(), root.getValue().activeConfig());
        }
        public static Map.Entry<String, Configurable> getClosestRootManager(String path) {
            if (path.equals(".")) { return null; }

            Map.Entry<String, Configurable> bestMatch = null;

            for (Map.Entry<String, Configurable> entry : queryManager.entrySet()) {
                if (path.startsWith(entry.getKey()) && (bestMatch == null || path.length() > bestMatch.getKey().length())) {
                        bestMatch = entry;
                }
            }

            return bestMatch;
        }
        public static JSONObject getConfig(String path) throws JSONException {
            if ((queryManager == null) && (path.equals("."))) {
                return initialConfig();
            } else if (path.equals(".")) {
                JSONObject overall = new JSONObject();
                for (Map.Entry<String, Configurable> entry : queryManager.entrySet()) {
                    mergeConfig(overall, entry.getValue().activeConfig());
                }
                return overall;
            } else if (queryManager == null) {
                throw new NoSuchElementException("No handle for config path " + " is currently registered");
            } else {
                Map.Entry<String, JSONObject> closest = getClosestRootConfig(path);
                // todo subsearch
                if (!closest.getKey().equals(path)) return null;
                return closest.getValue();
            }
        }
        public static Set<String> getRegisteredPaths() {
            return queryManager.keySet();
        }
    }

    public static String getConfigPath() {
        return ConfigDir + File.separator + ConfigFileName;
    }

    private static JSONObject initialConfig() throws JSONException {
        try {
            return loadConfig(getConfigPath());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
        }

        // ok, so no config file... time to load defaults
        return new EndpointNetworkNode().defaultConfig();
    }

    public static boolean dumpConfig(OutputStream out, JSONObject config) {
        PrintWriter writer = new PrintWriter(out);
        try {
            writer.println(config.toString(4));
            writer.close();
        } catch (JSONException ex) {
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
            return false;
        }
        return true;
    }
    public static boolean saveConfig(String filename, JSONObject config) {
        FileOutputStream out = null;
        boolean ok = false;
        try {
            out = new FileOutputStream(filename);
            ok = dumpConfig(out, config);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
            return false;
        }
        return ok;
    }

    public static JSONObject readConfig(InputStream in) {

        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();

        try {
            try {
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (IOException ex){
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
        }

        JSONObject retval = null;
        try {
            retval = new JSONObject(sb.toString());
        } catch (JSONException ex) {
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
        }
        return retval;
    }
    public static JSONObject loadConfig(String filename) throws FileNotFoundException {
        InputStream in = null;
        try {
            URL configURL = new URL(filename);
            in = configURL.openStream();
        } catch (IOException ex) {
            in = new FileInputStream(filename);
        }

        assert in != null;

        return readConfig(in);
    }

    public static void mergeConfig(JSONObject inout, JSONObject in) throws IllegalArgumentException {
        Iterator<String> key = in.keys();

        while (key.hasNext()) {
            String k = key.next();
            try {
                inout.put(k, in.get(k));
            } catch (JSONException ex) {
                throw new IllegalArgumentException("Unable to merge config objects. Reason: " + ex.toString());
            }
        }
    }
}
