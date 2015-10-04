package p2p;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public enum MessageType {
    
    REMOVE_NODE_MESSAGE("REMOVE_NODE_MESSAGE", "Collab Universe: NetworkNode Removed Message"),
    PLAN_UPDATE_MESSAGE("NEW_PLAN_MESSAGE", "Collab Universe: New Plan"),
    NETWORK_UPDATE_MESSAGE("NETWORK_UPDATE_MESSAGE", 
            "Collab Universe: EndpointNetworkNodes update", 
            "Collab Universe: PhysicalNetworkNodes update", 
            "Collab Universe: UnknownNetworkNodes update", 
            "Collab Universe: LogicalNetworkLinks update",
            "Collab Universe: PhysicalNetworkLinks update"),
    ACTIVE_LINKS_MESSAGE("ACTIVE_LINKS_MESSAGE", "Collab Universe: Active NetworkLinks update"),

    NODE_UNREACHABLE_MESSAGE("NODE_UNREACHABLE_MESSAGE", "Collab Universe: NetworkNode Unreachable via NetworkLink"),
    NODE_REACHABLE_MESSAGE("NODE_REACHABLE_MESSAGE", "Collab Universe: NetworkNode Reachable via NetworkLink"),
    NEW_ENDPOINT_NODE_MESSAGE("NEW_ENDPOINT_NODE_MESSAGE", 
            "Collab Universe: EndpointNetworkNode Information Message"), 
    NEW_GENERAL_NODE_MESSAGE("NEW_GENERAL_NODE_MESSAGE", 
            "Collab Universe: EndpointNetworkNode Information Message"),
    LINK_LATENCY_CHANGED_MESSAGE("LINK_LATENCY_CHANGED_MESSAGE", "CollabUniverse: NetworkLink latency update"),
    
    AGC_PING_MESSAGE("AGC_PING_MESSAGE"), // request response from AGC to check its liveness
    AGC_LOCATION_MESSAGE("AGC_LOCATION_MESSAGE"), 
        // AGC notification to the other nodes; used as PONG response
        //   or as the vote result notification. AGC ConnectorID is contained in the message.sender attribute
    AGC_INIT_VOTE_MESSAGE("AGC_INIT_VOTE_MESSAGE"),
        // Cannot locate AGC, hence request a new AGC vote in order to find a new one
    AGC_VOTE_TICKET_MESSAGE("AGC_VOTE_TICKET_MESSAGE", "CollabUniverse: AGC ticket"),
        // AGC-allowed node response for a vote request. Contains the voting
        //   information about local node for comparison
    
    COUNSIL_WANT_TO_TALK("COUNSIL_WANT_TO_TALK", "Counsil: id"),
    COUNSIL_DO_NOT_WANT_TO_TALK("COUNSIL_DO_NOT_WANT_TO_TALK", "Counsil: id"),
    COUNSIL_CAN_TALK("COUNSIL_CAN_TALK", "Counsil: id"),
    COUNSIL_STOPPED_TALKING("COUNSIL_STOPPED_TALKING", "Counsil: id"),
    COUNSIL_CAN_NOT_TALK("COUNSIL_CAN_NOT_TALK", "Counsil: id"),
    COUNSIL_PRODUCER_STARTED("COUNSIL_PRODUCER_STARTED"), 
    COUNSIL_NETWORK_UPDATE("COUNSIL_NETWORK_UPDATE", "Counsil: nodes"), 
    
    AUXILIARY_MESSAGE("AUXILIARY_MESSAGE", "descriptor", "payload"),
    DEBUG_LOG_MESSAGE("DEBUG_LOG_MESSAGE", "CoUniverse: Source descriptor chain", "CoUniverse: message");
        // Parallel timestamped logging of the whole universe
        //   Source descriptor chain is a list of sub-descriptors used to locate message source;

    
    public static final String MESSAGE_TYPE_ID = "CollabUniverse: Message type id";
    
    // List of critical messages - should be echoed, if the jxta multicast is unreliable
    public static final List<MessageType> SHOUT_AND_ECHO_MESSAGE_TYPES = Collections.unmodifiableList(
            new LinkedList<MessageType>() {
                {
                    add(AGC_PING_MESSAGE);
                    add(AGC_LOCATION_MESSAGE);
                    add(AGC_INIT_VOTE_MESSAGE);
                    add(AGC_VOTE_TICKET_MESSAGE);
                }
            });

    private final String msgTypeId;
    private final String[] elementIdentifiers;

    private MessageType(String msgTypeId, String... elementIdentifiers) {
        if (!(msgTypeId != null && msgTypeId.length() > 0 && elementIdentifiers != null)) {
            throw new IllegalArgumentException();
        }
        this.msgTypeId = msgTypeId;
        this.elementIdentifiers = elementIdentifiers;
    }

    public String getMsgTypeId() {
        return msgTypeId;
    }

    public String[] getElementIdentifiers() {
        return elementIdentifiers;
    }

    public boolean matchMessageType(String typeId, String... elements) {
        if (msgTypeId.equals(typeId) && elements.length == elementIdentifiers.length) {
            boolean matchFound = true;
            for (int i = 0; i < elements.length; i++) {
                if (!elementIdentifiers[i].equals(elements[i])) {
                    matchFound = false;
                }
            }
            if (matchFound) {
                return true;
            }
        }
        return false;
    }

    public int getElementPos(String typeId) {
        for(int i = 0; i < this.elementIdentifiers.length; i++) {
            if(this.elementIdentifiers[i].equals(typeId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Requested bad element " + typeId + " from MessageType " + getMsgTypeId());
    }


    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < elementIdentifiers.length; i++) {
            if(i != 0) {
                s.append(", ");
            }
            s.append(("["+elementIdentifiers[i]+"]"));
        }
        return msgTypeId + ": " + s.toString();
    }

}
