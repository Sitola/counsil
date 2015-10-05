/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package core;

import java.net.URI;
import java.util.List;
import myJXTA.MyJXTAConnector;
import networkRepresentation.EndpointNetworkNode;
import p2p.AGCMonitorThread;
import p2p.NetworkConnector;
import p2p.NodeGroupIdentifier;

/**
 *
 * @author maara
 */
public class Peer {

    private NetworkConnector connector;
    private EndpointNetworkNode localNode;
    
    public Peer() {
        
    }
    
    public void updateLocalNode(EndpointNetworkNode localNode) {
        // TODO: any further processing required?
        this.localNode = localNode;
    }
    
    
    /**
     * Connect to the JXTA universe. At least one uri must be specified
     * @param rendezvousSeedingUris List of uris, where the currently running rendezvous uris can be downloaded from
     * @param rendezvousUris List of uris where actual rendezvous nodes can be located
     */
    public void connectToJxtaUniverse(List<URI> rendezvousSeedingUris, List<URI> rendezvousUris, boolean enableRendezvous, boolean enableAgc) {
        
        // TODO: The following call requires ConnectorID of the local node!
        //       Create new MyJXTAConnectorID with arbitrary unique (UUID would be nice) string argument
        MyJXTAConnector jxtaConnector = new MyJXTAConnector(null);
        
        jxtaConnector.setRendezvousSeedingUris(rendezvousSeedingUris);
        jxtaConnector.setRendezvousUris(rendezvousUris);
        
        jxtaConnector.setRendezvousEnabled(enableRendezvous);
        
        connector.joinUniverse(enableAgc);
        connector.startReceiving();
        connector.startReceivingFromGroup(NodeGroupIdentifier.ALL_NODES);
        
    }
    
    
    public NetworkConnector getConnector() {
        return connector;
    }

    public void setConnector(NetworkConnector connector) {
        this.connector = connector;
    }
    
    
    
    
}
