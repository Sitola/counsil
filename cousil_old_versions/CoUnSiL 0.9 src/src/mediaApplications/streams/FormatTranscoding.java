package mediaApplications.streams;

import core.Reportable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import mediaApplications.streams.WellKnownFormats.Format;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Representation of a transcoding from one stream format to another.
 * Transcoding may occur on some distributors. For simplicity, input and output
 * formats may be the same. In such case, instance of the class represents data
 * forwarding without any decoding or encoding.
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class FormatTranscoding implements Serializable, Reportable {

    public static enum WellKnownTranscoding implements Serializable {// "" Nechat jenom identitu mezi formatem s order 1

        UNCOMPRESSED_FULL_HD_FORWARD(1, WellKnownFormats.Format.UNCOMPRESSED_FULL_HD, WellKnownFormats.Format.UNCOMPRESSED_FULL_HD); 
        
        public final int order;
        public final FormatTranscoding transcoding;
        public final int orderIn;
        public final int orderOut;

        private WellKnownTranscoding(int order, Format formatIn, Format formatOut) {
            this.order = order;
            this.transcoding = new FormatTranscoding(formatIn.format, formatOut.format);
            this.orderIn = formatIn.order;
            this.orderOut = formatOut.order;
        }

        public static ArrayList<FormatTranscoding> getList() {
            ArrayList<FormatTranscoding> list = new ArrayList<>();
            for (WellKnownTranscoding t : WellKnownTranscoding.values()) {
                list.add(t.transcoding);
            }
            return list;
        }

        public static ArrayList<FormatTranscoding> getList(int formatOrderLimit) {
            ArrayList<FormatTranscoding> list = new ArrayList<>();
            for (WellKnownTranscoding t : WellKnownTranscoding.values()) {
                if (t.orderIn <= formatOrderLimit && t.orderOut <= formatOrderLimit) {
                    for (FormatTranscoding ft : list) {
                        if (ft.formatIn.equals(t.transcoding.formatIn)
                                && ft.formatOut.equals(t.transcoding.formatOut)) {
                            System.out.println("Repeated WKT: " + t.orderIn + " " + t.orderOut);
                        }
                    }
                    list.add(t.transcoding);
                }
            }
            return list;
        }
    }
    /**
     * Amount of CPU performance needed to forward 1 Mbps of data
     */
    private static final int BANDWIDTH_CPU_COEF = 1;
    /**
     * Input format
     */
    public final StreamFormat formatIn;
    /**
     * Output format
     */
    public final StreamFormat formatOut;
    /**
     * Latency of the transcoding in milliseconds
     */
    public final int latency;
    /**
     * Resources required by the transcoding
     */
    public final EnumMap<TranscodingResource, Double> resources;
    public int variableIndex;

    public FormatTranscoding(StreamFormat formatIn, StreamFormat formatOut) {
        this.formatIn = formatIn;
        this.formatOut = formatOut;
        this.resources = new EnumMap<>(TranscodingResource.class);

        if (formatIn.equals(formatOut)) {
            /* A "special" (actually quite common) case, in which the data are 
             * only forwarded and no computation intensive de/encoding takes place.
             */
            this.latency = 0;
            double cpu = formatIn.bandwidthMax / 1e6 * BANDWIDTH_CPU_COEF;
            this.resources.put(TranscodingResource.CPU_PERFORMANCE, cpu);
        } else {
            this.latency = formatIn.latencyIn + formatOut.latencyOut;
            for (TranscodingResource res : TranscodingResource.values()) {
                double amount = formatIn.getDecodingResource(res) + formatOut.getEncodingResource(res);
                this.resources.put(res, amount);
            }
        }
    }

    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        
        status.put("formatIn", formatIn.name);
        status.put("formatOut", formatOut.name);
        status.put("latency", latency);
        status.put("resources", StreamFormat.reportResources(resources));
        
        return status;
    }
    
    
}
