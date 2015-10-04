/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

/**
 *
 * @author maara
 */
public enum NodeGroupIdentifier {
    
    LOCAL_NODE("LOCAL_NODE"),
    ALL_NODES("ALL_NODES"), 
    AGC_CAPABLE_NODES("AGC_CAPABLE_NODES"),
    DEBUG_LISTENER_NODES("DEBUG_LISTENER_NODES");    
    public final String identifier;
    
    NodeGroupIdentifier(String identifier) {
        this.identifier = identifier;
    }
}