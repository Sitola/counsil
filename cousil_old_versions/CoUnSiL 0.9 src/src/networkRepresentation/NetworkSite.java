package networkRepresentation;

import java.io.Serializable;
import mediaAppFactory.MediaStreamSet;

/**
 * Representation of a site. Allows for aggregation of NetworkNodes per site.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 2.8.2007
 * Time: 11:10:35
 */
public class NetworkSite implements Serializable {
    private String siteName;
    private MediaStreamSet streamSet;

    /**
     * Empty site constructor
     */
    public NetworkSite() {
        this("");
    }

    /**
     * Site constructor.
     * <p/>
     *
     * @param siteName String representation of site name
     */
    public NetworkSite(String siteName) {
        assert siteName != null;
        this.siteName = siteName;
        this.streamSet = new MediaStreamSet(siteName);
    }

    /**
     * Returns site name
     * <p/>
     *
     * @return site name
     */
    public String getSiteName() {
        return siteName;
    }

    public MediaStreamSet getStreamSet() {
        return streamSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkSite that = (NetworkSite) o;

        return siteName.equals(that.siteName);

    }

    @Override
    public int hashCode() {
        return siteName.hashCode();
    }

    // JavaBean enforced setters

    public void setSiteName(String siteName) {
        this.siteName = siteName;
        this.streamSet.setName(siteName);
    }

}
