/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation;

import java.io.Serializable;

/**
 *
 * @author maara
 */
public class LocalNodeKnowledge implements Serializable {
    
    private EndpointNetworkNode localnode;

    public EndpointNetworkNode getLocalnode() {
        return localnode;
    }

    public void setLocalnode(EndpointNetworkNode localnode) {
        this.localnode = localnode;
    }
    
    @Override
    public String toString() {
        return "Local node information from " + localnode.getNodeName();
    }
}
