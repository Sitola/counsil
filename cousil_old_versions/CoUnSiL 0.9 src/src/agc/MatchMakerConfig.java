package agc;

import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Externalized configuration for MatchMaker and its different versions
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 12.10.2007
 * Time: 8:33:22
 */
public class MatchMakerConfig {

    protected int timeout = 0;  // hard timeout for the search 
    protected HashMap<String, Object> properties = new HashMap<>();

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setProperty(String property, Object set) {
        properties.put(property, set);
    }

    public Object getProperty(String property) {
        if (!properties.containsKey(property)) {
            Logger.getLogger(this.getClass()).trace("MatchMakerConfig unset property requested: " + property);
            return null;
        }
        
        return properties.get(property);
    }
    
    public Boolean getBooleanProperty(String property) {
        return (this.getProperty(property) != null) && ((Boolean) this.getProperty(property));
    }
}
