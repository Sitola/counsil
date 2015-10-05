package mediaApplications;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import mediaAppFactory.MediaApplicationDistributor;
import mediaAppFactory.MediaStream;
import mediaApplications.streams.FormatTranscoding;
import mediaApplications.streams.StreamFormat;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Optimized version of RUM (UDP Packet Reflector) for uncompressed HD video distribution
 * <p/> 
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 11:35:56
 */
public class RumHD extends MediaApplicationDistributor {
    
    private HashSet<StreamFormat> formats;
    private LinkedHashSet<FormatTranscoding> transcodings;
    
    public RumHD() {
        this("HD UDP Packet Reflector");
    }

    public RumHD(String applicationName) {
        super(applicationName, new HashSet<MediaStream>());
        formats = new HashSet<>();
        if (getApplicationName() != null) formats.addAll(getFormatsFromUv());
        
        // TODO: now we suppose that any transcoding is available once UG claims to have resources
        transcodings = new LinkedHashSet<>();
    }
    
    @Override
    public Collection<FormatTranscoding> getTranscodings() {
        return transcodings;
    }

    @Override
    public void addTranscodings(Collection<FormatTranscoding> transcodings) {
        transcodings.addAll(transcodings);
    }

    @Override
    public Set<StreamFormat> getCompressionFormats() {
        return formats;
    }

    @Override
    public Set<StreamFormat> getDecompressionFormats() {
        // TODO: Should change once uv output differs between compression and decompression
        return formats;
    }

    private Set<StreamFormat> getFormatsFromUv() {
        // FIXME: Should change once uv output differs between compression and decompression
        return UltraGridProducer.getFormatsFromUv(this.getApplicationPath() + " --capabilities");
    }

    private static JSONArray reportFormats(Collection<StreamFormat> formats) throws JSONException {
        JSONArray formatsReported = new JSONArray();
        
        for (StreamFormat format : formats) {
            formatsReported.put(format.reportStatus());
        }
        
        return formatsReported;
    }
    private static JSONArray reportTranscodings(Collection<FormatTranscoding> fmtTranscodings) throws JSONException {
        JSONArray formatsReported = new JSONArray();
        
        for (FormatTranscoding transcoding : fmtTranscodings) {
            formatsReported.put(transcoding.reportStatus());
        }
        
        return formatsReported;
    }
    
    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        status.put("compressionFormats", reportFormats(getCompressionFormats()));
        status.put("decompressionFormats", reportFormats(getDecompressionFormats()));
        status.put("transcodings", reportTranscodings(getTranscodings()));
        return status;
    }
    
    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject configuration = super.defaultConfig();
        configuration.put("command", "rum-hd-transcode");
        return configuration;
    }
    
    @Override
    public boolean loadConfig(JSONObject config) throws JSONException, IllegalArgumentException {
        boolean retval = super.loadConfig(config);
        if (retval) formats = new HashSet<>(getFormatsFromUv());
        return retval;
    }
    
}
