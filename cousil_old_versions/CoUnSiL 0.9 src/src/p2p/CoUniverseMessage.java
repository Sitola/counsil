/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.Serializable;

/**
 *
 * @author maara
 */
public class CoUniverseMessage implements Serializable {
    
    public static final String SENDER_IDENTIFIER_STRING = "CoUniverse: Message sender";
    public static final String RECEIVER_IDENTIFIER_STRING = "CoUniverse: Message receiver";
    
    public MessageType type;
    public Serializable[] content;
    
    public ConnectorID sender, receiver;

    /**
     * @param type Type of the new message; cannot be null
     * @param content Content of the message, array size must match the number of fields specified by type
     * @param sender ID of the sender, cannot be null
     * @param receiver ID of the receiver, can be null if not known
     */
    public CoUniverseMessage(MessageType type, Serializable[] content, ConnectorID sender, ConnectorID receiver) {
        if (type == null || content == null || sender == null) throw new NullPointerException("Type, content and sender arguments cannot be null for CoUniverse message"); 

        if (type.getElementIdentifiers().length != content.length) throw new IllegalArgumentException("Number of content elements does not match the expected number for " + type.getMsgTypeId() + ": Found " + content.length + ", expected " + type.getElementIdentifiers().length);
        
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
    }

    public Object getElement(String id) {
        return content[type.getElementPos(id)];
    }
    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("CoUniverseMessage " + type.getMsgTypeId() + " [");
        
        for (int i = 0; i < type.getElementIdentifiers().length; i++) {
            ret.append(type.getElementIdentifiers()[i]).append(" = ").append(content[i]).append(", ");
        }
        
        ret.append("]");
        return ret.toString();
    }
}
