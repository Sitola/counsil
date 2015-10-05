package mediaAppFactory;

import java.util.Collection;

/**
 * Interface extending MediaApplication for media producers
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 11:16:26
 */
public abstract class MediaApplicationProducer extends MediaApplication implements MediaCompressor {

    public MediaApplicationProducer(String name, Collection<MediaStream> mediaStreams) {
        super(name, mediaStreams);
    }
            
    @Deprecated
    public abstract void setTargetIP(String targetIP);
    
    @Deprecated
    public abstract String getTargetIP();
}
