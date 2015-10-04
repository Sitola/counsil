package mediaAppFactory;

import core.Configurable;
import core.Reportable;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import networkRepresentation.EndpointNetworkNode;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This is an abstract class wrapping all the media applications that may run on any of the nodes
 * in the collaborative network.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 9:37:28
 */

public abstract class MediaApplication implements Serializable, Configurable, Reportable {
    public static final String ConfigKeyUuid = "uuid";

    private String applicationName;   // application name
    private CopyOnWriteArraySet<MediaStream> mediaStreams;   // thread-safe collection of supported media streams
    private transient String applicationPath = null;   // application path
    private transient String applicationCmdOptions = null;   // command-line options for the application
    protected EndpointNetworkNode parentNode = null;
    private String uuid;
    public transient int variableIndex;
    
    private int localNodeSerialId;
    
    private String preferredReceivingPort;

    /**
     * MediaApplication constructor
     *
     * @param applicationName name of the application
     * @param mediaStreams    supported media streams
     */
    public MediaApplication(String applicationName, Collection<MediaStream> mediaStreams) {
        this.applicationName = applicationName;
        this.mediaStreams = new CopyOnWriteArraySet<MediaStream>();
        assert mediaStreams != null;
        this.mediaStreams.addAll(mediaStreams);
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Returns application name
     *
     * @return application name
     */
    public String getApplicationName() {
        return "#"+localNodeSerialId+": " + this.applicationName;
    }

    /**
     * Returns supported media streams
     *
     * @return thread-safe collection of supported meda streams
     */
    public CopyOnWriteArraySet<MediaStream> getMediaStreams() {
        return mediaStreams;
    }

    /**
     * Sets up application before running it
     *
     * @param path path to the binary/script
     * @param opts command line options
     */
    public void setupApplication(String path, String opts) {
        applicationPath = path;
        applicationCmdOptions = opts;
    }


    /**
     * Gets application path
     *
     * @return application path
     */
    public String getApplicationPath() {
        return (applicationPath == null) ? "" : applicationPath;
    }

    /**
     * Get application command-line options
     *
     * @return
     */
    public String getApplicationCmdOptions() {
        return (applicationCmdOptions == null) ? "" : applicationCmdOptions;
    }

    /**
     * (Re)sets the command-line options for the application
     *
     * @param applicationCmdOptions
     */
    public void setApplicationCmdOptions(String applicationCmdOptions) {
        this.applicationCmdOptions = applicationCmdOptions;
    }

    /**
     * Gets the node the application is residion on.
     * <p/>
     *
     * @return
     */
    public EndpointNetworkNode getParentNode() {
        return parentNode;
    }

    /**
     * Sets the parent node for this application.
     * <p/>
     *
     * @param parentNode
     */
    public void setParentNode(EndpointNetworkNode parentNode) {
        this.parentNode = parentNode;
    }

    // JavaBean enforced methods

    protected MediaApplication() {
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setMediaStreams(CopyOnWriteArraySet<MediaStream> mediaStreams) {
        this.mediaStreams = mediaStreams;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Custom comparator
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaApplication that = (MediaApplication) o;

        if (!uuid.equals(that.uuid)) return false;

        return true;
    }
    
    /**
     * Custom hashCode for keys in HashMaps
     * @return
     */
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * Returns maximum total bandwidth used by this MediaApplication. Iterates through media steams
     * and counts up maximum bandwidths together.
     * <p/>
     * @return maximum bandwidth in bps
     */
    public long getMediaMaxBandwidth() {
        long bw = 0L;
        for (MediaStream mediaStream : this.getMediaStreams()) {
            bw += mediaStream.getBandwidth_max();
        }
        return bw;
    }

    @Override
    public String toString() {
        return "#"+localNodeSerialId+": " + this.applicationName ;
    }
    
    public String getShortDescription() {
        return toString();
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException {
        boolean updated = false;
        
        String tmpName = null;
        String tmpUuid = null;
        String tmpCommand = null;
        String tmpArguments = null;

        try {
            tmpName = configuration.getString("name");
            tmpUuid = configuration.getString(ConfigKeyUuid);
            tmpCommand = configuration.optString("command", "");
            tmpArguments = configuration.optString("arguments", "");
            if (tmpName == null || tmpUuid == null || tmpCommand == null || tmpArguments == null) { throw new IllegalArgumentException(); }
        } catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Given config object does not contain all keys. Compare keys with defaultConfig().");
        }
        
        synchronized (this) {
            updated |= !getApplicationName().equals(tmpName);
            updated |= !getUuid().equals(tmpUuid);
            updated |= !getApplicationPath().equals(tmpCommand);
            updated |= !getApplicationCmdOptions().equals(tmpArguments);

            setApplicationName(tmpName);
            setUuid(tmpUuid);
            setApplicationPath(tmpCommand);
            setApplicationCmdOptions(tmpArguments);
        }
        
        return updated;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject application = new JSONObject();
        application.put("name", getApplicationName());
        application.put(ConfigKeyUuid, getUuid());
        if (applicationPath != null) {
            application.put("command", applicationPath);
        }
        if (applicationCmdOptions != null) {
            application.put("arguments", applicationCmdOptions);
        }
        application.put("mediaMaxBandwidth", getMediaMaxBandwidth());
        
        JSONArray streams = new JSONArray();
        for (MediaStream stream: getMediaStreams()) {
            streams.put(stream.reportStatus());
        }
        application.put("streams", streams);
        
        return application;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject application = new JSONObject();
        application.put("name", getApplicationName());
        application.put("uuid", UUID.randomUUID().toString());
        application.put("command", "/bin/true");
        application.put("arguments", "--argument-ignored");
        
        return application;
    }

    @Override
    public abstract JSONObject reportStatus() throws JSONException;
    
    public void setPreferredReceivingPort(String port) {
        this.preferredReceivingPort = port;
    }
    
    public String getPreferredReceivingPort() {
        return preferredReceivingPort;
    }
    
    public void setLocalNodeSerialId(int id) {
        this.localNodeSerialId = id;
    }
}
