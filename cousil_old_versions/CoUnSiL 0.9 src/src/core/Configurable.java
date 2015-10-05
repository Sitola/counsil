package core;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public interface Configurable {
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException, JSONException;
    public JSONObject activeConfig() throws JSONException;
    public JSONObject defaultConfig() throws JSONException;
}
