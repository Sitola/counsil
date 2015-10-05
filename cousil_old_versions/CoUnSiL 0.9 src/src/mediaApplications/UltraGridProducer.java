/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mediaApplications;

import core.Main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mediaAppFactory.MediaApplicationProducer;
import mediaAppFactory.MediaStream;
import mediaApplications.streams.StreamFormat;
import mediaApplications.streams.TranscodingResource;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author maara
 */
public class UltraGridProducer extends MediaApplicationProducer {
    
    HashSet<StreamFormat> formats = new HashSet<>();

    public UltraGridProducer() {
        this("UltraGrid video producer", new ArrayList<MediaStream>());
    }

    public UltraGridProducer(String name, Collection<MediaStream> mediaStreams) {
        super(name, mediaStreams);
        if (getApplicationName() != null) formats.addAll(getFormatsFromUv());
    }

    @Override
    public void setTargetIP(String targetIP) {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName() + ": set target ip not supported yet.");
    }

    @Override
    public String getTargetIP() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName() + ": get target ip not supported yet.");
    }

    @Override
    public Set<StreamFormat> getCompressionFormats() {
        return formats;
    }

    private Set<StreamFormat> getFormatsFromUv() {
        return getFormatsFromUv(getApplicationPath() + " --capabilities");
}

    static Set<StreamFormat> getFormatsFromUv(String command) {
        HashSet<StreamFormat> formats = new HashSet<>();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            Logger.getLogger(UltraGridProducer.class).warn("Cannot exec uv to read capabilities: " + ex.getMessage());
            return formats;
        }
        try (Scanner s = new Scanner(p.getInputStream())) {
            while (s.hasNextLine() && !s.nextLine().equals("Compressions:")) {
            }
            Pattern formatPattern = Pattern.compile("\\(([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+)\\)");
            while (s.hasNextLine()) {
                String line = s.nextLine();
                Matcher m = formatPattern.matcher(line);
                if (m.matches()) {
                    long bitrate;
                    int quality;
                    int decodingLatency;
                    int encodingLatency;
                    double decodingCpuCores;
                    double encodingCpuCores;
                    double decodingGpuGflops;
                    double encodingGpuGflops;
                    try {
                        bitrate = Long.parseLong(m.group(3));
                        quality = Integer.parseInt(m.group(2));
                        decodingLatency = Integer.parseInt(m.group(7));
                        encodingLatency = Integer.parseInt(m.group(4));
                        decodingCpuCores = Double.parseDouble(m.group(8));
                        decodingGpuGflops = Double.parseDouble(m.group(9));
                        encodingCpuCores = Double.parseDouble(m.group(5));
                        encodingGpuGflops = Double.parseDouble(m.group(6));
                    } catch (NumberFormatException numberFormatException) {
                        Logger.getLogger(Main.class).warn("UG capabilities, line: " + line + "\n" + numberFormatException.getMessage());
                        continue;
                    }
                    StreamFormat f = new StreamFormat(m.group(1), bitrate, quality, decodingLatency, encodingLatency);
                    f.setDecodingResource(TranscodingResource.CPU_PERFORMANCE, decodingCpuCores);
                    f.setDecodingResource(TranscodingResource.GPU_PERFORMANCE, decodingGpuGflops);
                    f.setEncodingResource(TranscodingResource.CPU_PERFORMANCE, encodingCpuCores);
                    f.setEncodingResource(TranscodingResource.GPU_PERFORMANCE, encodingGpuGflops);
                    formats.add(f);
                } else {
                    if (line.equals("Capturers:")) {
                        break;
                    }
                }
            }
        }
        try {
            p.getInputStream().close();
        } catch (IOException ex) {
            System.out.println("IO exception");
        }

        return formats;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject configuration = super.defaultConfig();
        configuration.put("command", "uv");
        configuration.put("arguments", "-t testcard:640:480:25:UYVY");
        return configuration;
    }

    private JSONArray reportCompressionFormats() throws JSONException {
        JSONArray formatsReported = new JSONArray();
        
        for (StreamFormat format : getCompressionFormats()) {
            formatsReported.put(format.reportStatus());
        }
        
        return formatsReported;
    }
    
    @Override
    public JSONObject reportStatus() throws JSONException {
        JSONObject status = new JSONObject();
        status.put("compressionFormats", reportCompressionFormats());
        return status;
    }
    
    @Override
    public boolean loadConfig(JSONObject config) throws JSONException, IllegalArgumentException {
        boolean retval = super.loadConfig(config);
        if (retval) formats = new HashSet<>(getFormatsFromUv());
        return retval;
    }
    
}
