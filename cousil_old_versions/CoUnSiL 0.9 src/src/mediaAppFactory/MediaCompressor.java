package mediaAppFactory;

import java.util.Set;
import mediaApplications.streams.StreamFormat;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public interface MediaCompressor {

    Set<StreamFormat> getCompressionFormats();
}
