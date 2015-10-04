package utils;

import core.Configurable;
import java.io.Serializable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class GeoLocation implements Configurable, Serializable {
    public static final String KeyLatitude = "lat";
    public static final String KeyLongitude = "lng";
    private double latitude;
    private double longtude;

    public GeoLocation() {
        latitude = 0;
        longtude = 0;
    }

    public GeoLocation(double latitude, double longtitude) {
        this.latitude = latitude;
        this.longtude = longtitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longtude;
    }

    public void setLongtude(double longtude) {
        this.longtude = longtude;
    }

    public static GeoLocation fromGeoPosition(GeoPosition g) {
        return new GeoLocation(g.getLatitude(), g.getLongitude());
    }

    public GeoPosition toGeoPosition() {
        return new GeoPosition(getLatitude(), getLongitude());
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException {
        double tmpLatitude = 0;
        double tmpLongtitude = 0;
        try {
            tmpLatitude = configuration.getDouble(KeyLatitude);
            tmpLongtitude = configuration.getDouble(KeyLongitude);
        } catch (JSONException ex) {
            throw new IllegalArgumentException("Given configuration does not contain a valid geo position");
        }
//        tmpLatitude = (tmpLatitude + 90) % 180 + 90;
//        tmpLongtitude = (tmpLongtitude + 180) % 360 + 180;
        boolean updated = false;
        updated |= tmpLatitude != getLatitude();
        updated |= tmpLongtitude != getLongitude();
        setLatitude(tmpLatitude);
        setLongtude(tmpLongtitude);
        return updated;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject loc = new JSONObject();
        try {
            loc.put(KeyLongitude, getLongitude());
            loc.put(KeyLatitude, getLatitude());
        } catch (JSONException ex) {
            Logger.getLogger(ConfigUtils.class.getName()).log(Level.FATAL, null, ex);
            return null;
        }
        return loc;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        return new GeoLocation(49.21162, 16.59827).activeConfig();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoLocation loc = (GeoLocation)o;

        if (Double.compare(loc.getLatitude(), getLatitude()) != 0) return false;
        return Double.compare(loc.getLongitude(), getLongitude()) == 0;
    }

    @Override
    public int hashCode() {
        return new Double(latitude).hashCode() * new Double(longtude).hashCode();
    }
    
}
