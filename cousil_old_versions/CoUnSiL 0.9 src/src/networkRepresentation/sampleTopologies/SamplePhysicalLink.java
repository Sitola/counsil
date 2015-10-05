package networkRepresentation.sampleTopologies;

import java.util.ArrayList;
import networkRepresentation.PhysicalNetworkLink;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public interface SamplePhysicalLink {

    SamplePhysicalNode getFromNode();

    SamplePhysicalNode getToNode();

    int getCapacity();
}
