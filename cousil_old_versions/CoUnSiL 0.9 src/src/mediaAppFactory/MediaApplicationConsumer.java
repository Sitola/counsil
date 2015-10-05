package mediaAppFactory;

import java.util.Collection;
import java.util.HashSet;
import networkRepresentation.NetworkSite;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Interface extending MediaApplication for media consumer applications
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 11:16:36
 */
public abstract class MediaApplicationConsumer extends MediaApplication implements MediaDecompressor {
    
    public static final String ConfigKeySourceSiteName = "sourceSite";

    protected MediaApplication source;
    protected NetworkSite sourceSite;
    
    public MediaApplicationConsumer(String applicationName, Collection<MediaStream> mediaStreams) {
        super(applicationName, mediaStreams);
    }
            
    /**
     * Sets media application producer for this consumer. This is intended to be used
     * by ApplicationGroupController for MatchMaking purposes. This shouldn't be set
     * by NetworkNode locally (simply because NetworkNode doesn't have notion of MediaApplications
     * running on other nodes).
     * <p/>
     * @param source MediaApplicationProducer for this consumer
     */
    public void setSource(NetworkSite site, MediaApplication source) {
        if (this.sourceSite == null || (! this.sourceSite.equals(site))) throw new IllegalArgumentException("The source site ("+site+") is not recognized by consumer ("+this+")");
        this.source = source;
    }

    @Deprecated
    public HashSet<MediaApplication> getSources() {
        HashSet<MediaApplication> ret = new HashSet<MediaApplication>();
        ret.add(source);
        return ret;
    }
    
    /**
     * Checks if given source is among sources for this consumer.
     * @param source source to check
     * @return True if source found, false otherwise.
     */
    public boolean hasSource(MediaApplication source) {
        return this.source == null;
    }

    /**
     * Sets single source site for this consumer. Any other source sites are removed. This is intended to be used by NetworkNode
     * locally.
     * <p/>
     * @param sourceSite source site to set; null for intentionally inactive consumers
     */
    public void setSourceSite(NetworkSite sourceSite) {
        this.sourceSite = sourceSite;
    }

    /**
     * Adds source site for this consumer. This is intended to be used by NetworkNode locally.
     * <p/>
     * @param site  source site to add
     */
    public NetworkSite getSourceSite() {
        return sourceSite;
    }
    
    @Deprecated
    public HashSet<NetworkSite> getSourceSites() {
        HashSet<NetworkSite> ret = new HashSet<NetworkSite>();
        ret.add(this.sourceSite);
        return ret;
    }
    
    public String toString() {
        return getApplicationName() + " receiving from site "+ (getSourceSite() != null ? getSourceSite().getSiteName() : "null");
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject root = super.activeConfig();
        root.put(ConfigKeySourceSiteName, getSourceSite() != null ? getSourceSite().getSiteName() : "");
        return root;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject root = super.activeConfig();
        root.put(ConfigKeySourceSiteName, "");
        return root;
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException {
        String tmpSourceSiteName = configuration.getString(ConfigKeySourceSiteName);
        NetworkSite tmpSourceSite = null;
        
        if (tmpSourceSiteName != null && !tmpSourceSiteName.trim().isEmpty()) {
            tmpSourceSite = new NetworkSite(tmpSourceSiteName);
        }
        
        NetworkSite oldSite = getSourceSite();
        
        boolean updated = (oldSite != null && !oldSite.equals(tmpSourceSite)) || (tmpSourceSite != null);
        updated |= super.loadConfig(configuration);
        setSourceSite(tmpSourceSite);
        
        return updated;
    }


}
