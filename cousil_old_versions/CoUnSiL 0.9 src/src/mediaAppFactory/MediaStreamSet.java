package mediaAppFactory;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * MediaStreamSet represents a set of streams, which are equivalent by their content.
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class MediaStreamSet implements Serializable {
    String name;
    LinkedHashSet<MediaStream> streams;
    // TODO Get rid of this
    @Deprecated
    transient public int index;

    public MediaStreamSet(String name) {
        this.name = name;
        streams = new LinkedHashSet<>();
    }
    
    public void addStream(MediaStream s) {
        this.streams.add(s);
    }
    
    public boolean removeStream(MediaStream s) {
        return this.streams.remove(s);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || (! (o instanceof MediaStreamSet)) ) return false;
        
        MediaStreamSet that = (MediaStreamSet) o;
        
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.name);
        return hash;
    }
}
