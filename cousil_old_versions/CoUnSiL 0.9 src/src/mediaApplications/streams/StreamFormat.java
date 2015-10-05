package mediaApplications.streams;

import core.Reportable;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Objects;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Format of a media stream. Flat structure storing just basic data, not details
 * of a media format, e.g., image resolution, bitdepth, codec, etc. This simplification
 * was made for planning purposes, namely scalability of the planner.
 * 
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class StreamFormat implements Serializable, Reportable {
    // TODO: All those "public" don't look very well
    /** Name of the format */
    public final String name;
    /** Minimum bandwidth required to transmit the format in bits per second */
    public final long bandwidthMin;
    /** Maximum bandwidth required to transmit the format in bits per second */
    public final long bandwidthMax;
    /** Bandwidth of bursts when stream is bursty in bits per second */
    public final long bandwidthBurstMax;
    /** Subjective quality of the format */
    public final int quality;
    /** Inverse value of subjective quality of the format */
    public final int inverseQuality;
    /** Latency of format decoding in milliseconds */
    public final int latencyIn;
    /** Latency of format encoding in milliseconds */
    public final int latencyOut;
    /** Computational resources required for format decoding */
    EnumMap<TranscodingResource, Double> decodingResources;
    /** Computational resources required for format encoding */
    EnumMap<TranscodingResource, Double> encodingResources;
    /** Maximum allowed quality value */
    private static final int MAX_QUALITY = 100;
    
    // TODO: Get rid of this
    @Deprecated
    public int variableIndex;

    public StreamFormat(String name, long bandwidthMin, long bandwidthMax, long bandwidthBurstMax, int quality, int latencyIn, int latencyOut, EnumMap<TranscodingResource, Double> decodingResources, EnumMap<TranscodingResource, Double> encodingResources) {
        this.name = name;
        this.bandwidthMin = bandwidthMin;
        this.bandwidthMax = bandwidthMax;
        this.bandwidthBurstMax = bandwidthBurstMax;
        this.quality = quality;
        this.inverseQuality = MAX_QUALITY - quality;
        this.latencyIn = latencyIn;
        this.latencyOut = latencyOut;
        this.decodingResources = decodingResources;
        this.encodingResources = encodingResources;
    }

    public StreamFormat(String name, long bandwidthMin, long bandwidthMax, long bandwidthBurstMax, int quality, int latencyIn, int latencyOut) {
        this(name, bandwidthMin, bandwidthMax, bandwidthBurstMax, quality, latencyIn, latencyOut, new EnumMap<TranscodingResource, Double>(TranscodingResource.class), new EnumMap<TranscodingResource, Double>(TranscodingResource.class));
    }

    public StreamFormat(String name, long bandwidth, int quality, int latencyIn, int latencyOut) {
        this(name, bandwidth, bandwidth, bandwidth, quality, latencyIn, latencyOut, new EnumMap<TranscodingResource, Double>(TranscodingResource.class), new EnumMap<TranscodingResource, Double>(TranscodingResource.class));
    }

    public StreamFormat(String name, long bandwidth) {
        this(name, bandwidth, bandwidth, bandwidth, 80, 0, 0, new EnumMap<TranscodingResource, Double>(TranscodingResource.class), new EnumMap<TranscodingResource, Double>(TranscodingResource.class));
    }

    public double getDecodingResource(TranscodingResource res) {
        Double amount = decodingResources.get(res);
        if (amount == null) {
            return 0;
        }
        return amount;
    }

    public double getEncodingResource(TranscodingResource res) {
        Double amount = encodingResources.get(res);
        if (amount == null) {
            return 0;
        }
        return amount;
    }

    @Override
    public String toString() {
        return "StreamFormat{" + "name=" + name + ", bandwidthMax=" + bandwidthMax + ", quality=" + quality + '}';
    }

    public void setDecodingResource(TranscodingResource res, double amount) {
        decodingResources.put(res, amount >= 0.0 ? amount : 0.0);
    }

    public void setEncodingResource(TranscodingResource res, double amount) {
        encodingResources.put(res, amount >= 0.0 ? amount : 0.0);
    }
    
    public static JSONObject reportResources(EnumMap<TranscodingResource, Double> resources) throws JSONException {
        JSONObject result = new JSONObject();
        
        for (EnumMap.Entry<TranscodingResource, Double> r : resources.entrySet()) {
            result.put(r.getKey().toString(), r.getValue());
        }
        
        return result;
    }

    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        
        status.put("name", name);
        status.put("bandwidthMin", bandwidthMin);
        status.put("bandwidthMax", bandwidthMax);
        status.put("bandwidthBurstMax", bandwidthBurstMax);
        status.put("quality", quality);
        status.put("latencyIn", latencyIn);
        status.put("latencyOut", latencyOut);
        
        status.put("decodingResources", reportResources(decodingResources));
        status.put("encodingResources", reportResources(encodingResources));
        
        return status;
    }

    /* TODO: hashCode() and equals() are based on this.name only! UG reports same values
     * on all machines, matchMaker si not ready for smarter approach as well.
    */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StreamFormat other = (StreamFormat) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
}
