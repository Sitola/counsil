package mediaApplications.streams;

import java.io.Serializable;

/**
 * Enumeration of resources, which might be used for encoding and decoding
 * of media streams.
 * 
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public enum TranscodingResource implements Serializable {
    CPU_PERFORMANCE, GPU_PERFORMANCE, CPU_MEMORY, GPU_MEMORY
}
