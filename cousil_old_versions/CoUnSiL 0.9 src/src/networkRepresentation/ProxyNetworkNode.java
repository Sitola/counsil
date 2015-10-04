/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation;

import java.util.Set;

/**
 *
 * @author maara
 */
public class ProxyNetworkNode extends EndpointNetworkNode {

    @Override
    public boolean removeProxyNode(ProxyNetworkNode proxy) {
        throw new UnsupportedOperationException("Proxy node ("+this+") does not support encapsuled proxy nodes!");
    }

    @Override
    public Set<ProxyNetworkNode> getProxyNodes() {
        throw new UnsupportedOperationException("Proxy node ("+this+") does not support encapsuled proxy nodes!");
    }

    @Override
    public void addProxyNode(ProxyNetworkNode proxy) {
        throw new UnsupportedOperationException("Proxy node ("+this+") does not support encapsuled proxy nodes!");
    }
    
    
}
