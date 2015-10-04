/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mediaApplications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import mediaAppFactory.MediaApplicationConsumer;
import mediaAppFactory.MediaStream;
import mediaApplications.streams.StreamFormat;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author maara
 */
public class UltraGridConsumer extends MediaApplicationConsumer {
    
    HashSet<StreamFormat> formats = new HashSet<>();

    public UltraGridConsumer() {
        this("UltraGrid video consumer", new ArrayList<MediaStream>());
    }

    public UltraGridConsumer(String name, Collection<MediaStream> mediaStreams) {
        super(name, mediaStreams);
        if (getApplicationName() != null) formats.addAll(getFormatsFromUv());
    }

    private Set<StreamFormat> getFormatsFromUv() {
        // FIXME: Should change once uv output differs between compression and decompression
        return UltraGridProducer.getFormatsFromUv(getApplicationPath() + " --capabilities");
    }

    @Override
    public Set<StreamFormat> getDecompressionFormats() {
        return formats;
    }

    private static JSONArray reportFormats(Collection<StreamFormat> formats) throws JSONException {
        JSONArray formatsReported = new JSONArray();
        
        for (StreamFormat format : formats) {
            formatsReported.put(format.reportStatus());
        }
        
        return formatsReported;
    }
    
    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        status.put("decompressionFormats", reportFormats(getDecompressionFormats()));
        return status;
    }
    
    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject configuration = super.defaultConfig();
        configuration.put("command", "uv");
        configuration.put("arguments", "-d gl");
        return configuration;
    }

    @Override
    public boolean loadConfig(JSONObject config) throws JSONException, IllegalArgumentException {
        boolean retval = super.loadConfig(config);
        if (retval) formats = new HashSet<>(getFormatsFromUv());
        return retval;
    }
    
}
