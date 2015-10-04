package networkRepresentation.sampleTopologies;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public interface SampleEndpointConnection {
    String getSubnet();
    SampleSite getSite();
    SamplePhysicalNode getNode();
    int getCapacity();
}
