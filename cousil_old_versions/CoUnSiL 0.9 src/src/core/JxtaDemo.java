/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import myJXTA.MyJXTAConnector;
import myJXTA.MyJXTAConnectorID;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.LocalNodeKnowledge;
import p2p.CoUniverseMessage;
import p2p.MessageType;

/**
 *
 * @author maara
 */
@Deprecated
// TODO: Move to some testutils package, or remove completely
public class JxtaDemo {
    
    
    
    static ArrayList<URI> getRendezvousURIs() {
        
        ArrayList<URI> ret = new ArrayList<>();

        System.out.println("Reading rendezvous:");
        try(BufferedReader br = new BufferedReader(new FileReader("rendezvousList.txt"))) {
            String line = br.readLine();

            System.out.println("File opened. Processing individual lines:");
            
            while (line != null) {
                System.out.println("  " + line);
                URI uri = URI.create("tcp://" + line + ":9701");
                ret.add(uri);
                line = br.readLine();
            }
        } catch (IOException ex) {
        }
        
        System.out.println("Rendezvous list: " + ret);
        
        return ret;
    }
    
    public static void main(String args[]) throws UnknownHostException, InterruptedException {
        
        if (args.length < 2) {
            System.out.println("Must specify two input arguments (1 for true): allowAgc allowRendezvous");
            System.out.println("Setting default: 1 1");
            args = new String[]{"1", "1"};
        }
        
        
        boolean allowAGC = args[0].equals("1");
        boolean allowRendezvous = args[1].equals("1");
        
        MyJXTAConnectorID localNodeID = new MyJXTAConnectorID(myJXTA.MyJXTAUtils.getLocalHostName());
        System.out.println("My ConnectorID = " + localNodeID);
        
        MyJXTAConnector connector = new MyJXTAConnector(localNodeID);
        
        connector.setRendezvousUris(getRendezvousURIs());
        
        connector.setRendezvousEnabled(allowRendezvous);
        
        connector.joinUniverse(allowAGC);
        
        LocalNodeKnowledge localKnowledge = new LocalNodeKnowledge();
        EndpointNetworkNode node = new EndpointNetworkNode();
        node.setNodeName(myJXTA.MyJXTAUtils.getLocalHostName());
        localKnowledge.setLocalnode(node);
        
        while (true) {
            try {
                CoUniverseMessage message = new CoUniverseMessage(
                        MessageType.NEW_ENDPOINT_NODE_MESSAGE,
                        new Serializable[]{node},
                        localNodeID,
                        null
                );
                System.out.println("Sending message to AGC: " + message.toString());
                connector.sendMessageToAgc(message);
            } catch (IOException ex) { }
            
            Thread.sleep(5000);
        }
    }
}
