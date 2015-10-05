package mediaAppFactory;

import core.Reportable;
import java.io.Serializable;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Object containing information on media stream
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 11:43:36
 */

/* TODO: This class (and everything around needs some deep reevaluation and maybe
 * total extinction. Why do we need it once we have formats? */
public class MediaStream implements Serializable, Reportable {
    String description; // stream description
    long bandwidth_min; // minimum bandwidth in [bps]
    long bandwidth_max; // maximum bandwidth in [bps]
    int latency; // stream latency in given element (not end to end of course) in [ms]
    long bursts_max; // maximum bursts when stream is bursty [bps] - when equal to bandwidth_max, the stream is considered smooth
    float quality; // media quality (0.0 .. 10.0), where 10 is non-achievable ideal; designed for being able to compare different streams when building the environment

    /**
     * Full-fledged MediaStream constructor
     *
     * @param description   description of the stream
     * @param bandwidth_min minimum bandwidth in [bps]
     * @param bandwidth_max maximum bandwidth in [bps]
     * @param latency       stream latency in given element (not end to end of course) in [ms]
     * @param bursts_max    maximum bursts when stream is bursty [bps] - when equal to bandwidth_max, the stream is considered smooth
     * @param quality       edia quality (0.0 .. 10.0), where 10 is non-achievable ideal
     */
    public MediaStream(String description, long bandwidth_min, long bandwidth_max, int latency, long bursts_max, float quality) {
        this.description = description;
        this.bandwidth_min = bandwidth_min;
        this.bandwidth_max = bandwidth_max;
        this.latency = latency;
        this.bursts_max = bursts_max;
        this.quality = quality;
    }

    /**
     * Simpler MediaStream constructor with bandwidth and latecny specification only
     *
     * @param description description description of the stream
     * @param bandwidth   bandwidth in [bps]
     * @param latency     latency stream latency in given element (not end to end of course) in [ms]
     */
    public MediaStream(String description, long bandwidth, int latency) {
        this.description = description;
        this.bandwidth_min = bandwidth;
        this.bandwidth_max = bandwidth;
        this.latency = latency;
        this.bursts_max = bandwidth;
    }

    /**
     * Gets MediaStream description
     *
     * @return MediaStream description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets MediaStream minimum bandwidth
     *
     * @return MediaStream minimum bandwidth
     */
    public long getBandwidth_min() {
        return bandwidth_min;
    }

    /**
     * Gets MediaStream maximum bandwidth
     *
     * @return MediaStream maximum bandwidth
     */
    public long getBandwidth_max() {
        return bandwidth_max;
    }

    /**
     * Gets MediaStream latency
     *
     * @return MediaStream latency
     */
    public int getLatency() {
        return latency;
    }

    /**
     * Gets MediaStream maximum bursts
     *
     * @return MediaStream maximum bursts
     */
    public long getBursts_max() {
        return bursts_max;
    }

    /**
     * Custom matching
     * <p/>
     * @param o object to match
     * @return true if it matches
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaStream that = (MediaStream) o;

        if (bandwidth_max != that.bandwidth_max) return false;
        if (bandwidth_min != that.bandwidth_min) return false;
        if (bursts_max != that.bursts_max) return false;
        if (latency != that.latency) return false;
        if (Float.compare(that.quality, quality) != 0) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    
    // JavaBean enforced methods

    public MediaStream() {
    }

    public float getQuality() {
        return quality;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public void setBandwidth_min(long bandwidth_min) {
        this.bandwidth_min = bandwidth_min;
    }

    public void setBandwidth_max(long bandwidth_max) {
        this.bandwidth_max = bandwidth_max;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

    public void setBursts_max(long bursts_max) {
        this.bursts_max = bursts_max;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject report = new JSONObject();
        
        report.put("description", getDescription());
        report.put("quality", getQuality());
        report.put("maxBandwidth", getBandwidth_max());
        report.put("minBandwidth", getBandwidth_min());
        report.put("maxBurst", getBursts_max());
        report.put("latency", getLatency());
        
        return report;
    }
    
    
}
