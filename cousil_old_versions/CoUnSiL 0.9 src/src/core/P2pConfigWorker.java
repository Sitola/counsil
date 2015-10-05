package core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// TODO: Add javaDoc - ASAP
public class P2pConfigWorker extends SwingWorker<Void, Void> implements Configurable {
        public static final String ConfigKeyRendezvousSeedingURIs = "rendezvousSeedingURIs";
        public static final String ConfigKeyRendezvousURIs = "rendezvousURIs";
        public static final String ConfigKeyStartAGC = "startAGC";
        public static final String ConfigKeyStartRendezvous = "startRendezvous";

        private List<URI> rendezvousSeedingUris = null;
        private List<URI> rendezvousUris = null;
        private boolean enableRendezvous = false;
        private boolean enableAgc = false;
        
        boolean disabled = false;

        private static P2pConfigWorker instance = null;
        public static P2pConfigWorker getInstance() {
            // TODO: Thread safety 4ever!
            if (instance == null) instance = new P2pConfigWorker();
            return instance;
        }
                
        private P2pConfigWorker() { ; }

        @Override
        public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException {
        List<URI> tmpRendezvousSeedingUris = new ArrayList<>();
            List<URI> tmpRendezvousUris = new ArrayList<>();
            boolean tmpEnableRendezvous = true;
            boolean tmpEnableAgc = true;

            try {
                if (configuration.has(ConfigKeyRendezvousSeedingURIs)) {
                                try {
                                        JSONArray seeds = configuration.getJSONArray(ConfigKeyRendezvousSeedingURIs);
                                        for (int i = 0; i < seeds.length(); ++i) {
                                                tmpRendezvousSeedingUris.add(new URI(seeds.getString(i)));
                                        }
                                } catch (JSONException ex) {
                                        Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
                              }
                        }

                        if (configuration.has(ConfigKeyRendezvousURIs)) {
                                try {
                                        JSONArray rendezvous = configuration.getJSONArray(ConfigKeyRendezvousURIs);
                                        for (int i = 0; i < rendezvous.length(); ++i) {
                                                tmpRendezvousUris.add(new URI(rendezvous.getString(i)));
                                        }
                                } catch (JSONException ex) {
                                        Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
                                }
                        }
                } catch (URISyntaxException ex){
                        Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
                        throw new IllegalArgumentException("Configuration contains mallformed URI");
                }

                try {
                        tmpEnableAgc = configuration.getBoolean(ConfigKeyStartAGC);
                        tmpEnableRendezvous = configuration.getBoolean(ConfigKeyStartRendezvous);
                } catch (JSONException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.FATAL, "Configuration has to contain valid boolean flags for " + ConfigKeyStartAGC + " and " + ConfigKeyStartRendezvous, ex);
                        throw new IllegalArgumentException("Configuration contains mallformed URI");
                }
        
                synchronized (Main.getUniversePeer()) {
                    boolean updated = false;
                    updated |= !tmpRendezvousUris.equals(rendezvousUris);
                    updated |= !tmpRendezvousSeedingUris.equals(rendezvousSeedingUris);
                    updated |= tmpEnableAgc != enableAgc;
                    updated |= tmpEnableRendezvous != enableRendezvous;
                        
                        if (!updated) return false;
                        
                        rendezvousSeedingUris = tmpRendezvousSeedingUris;
                        rendezvousUris = tmpRendezvousUris;
                        enableRendezvous = tmpEnableRendezvous;
                        enableAgc = tmpEnableAgc;
                        
                    reconnect();
                    return true;
                }
        }
        
        public boolean isDisabled() { return disabled; }
        public void enable() { disabled = false; }
        public void disable() { disabled = true; }
        
        private void reconnect() {
                if (disabled) return;
                        
                //synchronized (Main.getUniversePeer()) {        
                // todo maara - zajistit noop pri 1. zavolani
                //Main.getUniversePeer().leaveUniverse();
                Main.getUniversePeer().connectToJxtaUniverse(rendezvousSeedingUris, rendezvousUris, enableRendezvous, enableAgc);
                //}
        }
        
        private JSONObject pack(List<URI> seedingUris, List<URI> uris, boolean startRendezvous, boolean startAgc) throws JSONException {
            // broken atomicity doesn't matter as its UI stub problem
            JSONObject root = new JSONObject();
            JSONArray seeds = new JSONArray();

            if (seedingUris != null) { for (URI seed : seedingUris) seeds.put(seed.toString()); }
            root.put(ConfigKeyRendezvousSeedingURIs, seeds);

            JSONArray rdvs = new JSONArray();
            if (uris != null) { for (URI rendezvous : uris) rdvs.put(rendezvous.toString()); }
            root.put(ConfigKeyRendezvousURIs, rdvs);

            root.put(ConfigKeyStartAGC, startAgc);
            root.put(ConfigKeyStartRendezvous, startRendezvous);

            return root;
        }
            
        @Override
        public JSONObject activeConfig() throws JSONException {
            return pack(rendezvousSeedingUris, rendezvousUris, enableRendezvous, enableAgc);
        }

        @Override
        public JSONObject defaultConfig() throws JSONException {
            return pack(new ArrayList<URI>(), new ArrayList<URI>(), true, true);
        }
                        
        @Override
        protected Void doInBackground() throws Exception {
            reconnect();
            return null;
        }
};
