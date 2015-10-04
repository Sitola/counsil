/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation.sampleTopologies;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public interface SampleTopology {
    <T extends Enum & SamplePhysicalNode> Class<T> getPhysicalNodes();
    <T extends Enum & SamplePhysicalLink> Class<T> getPhysicalLinks();
    <T extends Enum & SampleIntersiteLink> Class<T> getIntersiteLinks();
    <T extends Enum & SampleEndpointConnection> Class<T> getEndpointConnections();
}
