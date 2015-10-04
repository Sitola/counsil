package mediaAppFactory;

import mediaApplications.streams.FormatTranscoding;
import java.util.Collection;

/**
 * Interface extending MediaApplication for data distributors (e.g., reflectors)
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 11:16:58
 */
public abstract class MediaApplicationDistributor extends MediaApplication implements MediaCompressor, MediaDecompressor {
    public MediaApplicationDistributor(String applicationName, Collection<MediaStream> mediaStreams) {
        super(applicationName, mediaStreams);
    }
    public abstract Collection<FormatTranscoding> getTranscodings();
    public abstract void addTranscodings(Collection<FormatTranscoding> transcodings);
}
