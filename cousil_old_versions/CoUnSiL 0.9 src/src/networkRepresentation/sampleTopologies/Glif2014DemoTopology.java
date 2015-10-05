package networkRepresentation.sampleTopologies;

import java.util.ArrayList;
import java.util.Arrays;
import networkRepresentation.NetworkSite;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class Glif2014DemoTopology implements SampleTopology {

    private static final String DEFAULT_SUBNET = "glif";

    private static enum Sites implements SampleSite {

        BRNO(new NetworkSite("Brno")),
        PRAGUE(new NetworkSite("Prague")),
        NZ_CONTROL(new NetworkSite("Control")),
        NZ_DISTRIBUTION(new NetworkSite("Distribution"));
        
        private final NetworkSite site;

        private Sites(NetworkSite site) {
            this.site = site;
        }

        @Override
        public NetworkSite getNetworkSite() {
            return site;
        }

    }

    private static enum Node implements SamplePhysicalNode {

        BRNO_HP("Brno HP5412", true),
        BRNO_JUNIPER("Brno EX4500", true),
        PRAGUE_JUNIPER("Prague EX4550", true),
        GLIF_JUNIPER("GLIF MX480", true);
        
        private final String name;
        private final boolean known;

        private Node(String name, boolean known) {
            this.name = name;
            this.known = known;
        }

        @Override
        public boolean isKnown() {
            return known;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    private static enum Link implements SamplePhysicalLink {

        BRNOHP_JUNIPER(Node.BRNO_HP, Node.BRNO_JUNIPER, 10000),
        BRNO_PRAGUE(Node.BRNO_JUNIPER, Node.PRAGUE_JUNIPER, 10000),
        PRAGUE_GLIF(Node.PRAGUE_JUNIPER, Node.GLIF_JUNIPER, 10000);

        private final SamplePhysicalNode fromNode;
        private final SamplePhysicalNode toNode;
        private final int capacity;

        private Link(SamplePhysicalNode fromNode, SamplePhysicalNode toNode, int capacity) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.capacity = capacity;
        }

        @Override
        public SamplePhysicalNode getFromNode() {
            return fromNode;
        }

        @Override
        public SamplePhysicalNode getToNode() {
            return toNode;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

    }

    private static enum IntersiteLink implements SampleIntersiteLink {

        BRNO_PRAGUE(DEFAULT_SUBNET, Sites.BRNO, Sites.PRAGUE, new ArrayList(Arrays.asList(Link.BRNO_PRAGUE))),
        BRNO_CONTROL(DEFAULT_SUBNET, Sites.BRNO, Sites.NZ_CONTROL, new ArrayList(Arrays.asList(Link.BRNO_PRAGUE, Link.PRAGUE_GLIF))),
        BRNO_DISTRIBUTION(DEFAULT_SUBNET, Sites.BRNO, Sites.NZ_DISTRIBUTION, new ArrayList(Arrays.asList(Link.BRNO_PRAGUE, Link.PRAGUE_GLIF))),
        PRAGUE_CONTROL(DEFAULT_SUBNET, Sites.PRAGUE, Sites.NZ_CONTROL, new ArrayList(Arrays.asList(Link.PRAGUE_GLIF))),
        PRAGUE_DISTRIBUTION(DEFAULT_SUBNET, Sites.PRAGUE, Sites.NZ_DISTRIBUTION, new ArrayList(Arrays.asList(Link.PRAGUE_GLIF))),
        CONTROL_DISTRIBUION(DEFAULT_SUBNET, Sites.NZ_CONTROL, Sites.NZ_DISTRIBUTION, new ArrayList(Arrays.asList()));

        private final String subnet;
        private final SampleSite fromSite;
        private final SampleSite toSite;
        private final ArrayList<SamplePhysicalLink> physicals;

        private IntersiteLink(String subnet, SampleSite fromSite, SampleSite toSite, ArrayList<SamplePhysicalLink> physicals) {
            this.subnet = subnet;
            this.fromSite = fromSite;
            this.toSite = toSite;
            this.physicals = physicals;
        }

        @Override
        public String getSubnet() {
            return subnet;
        }

        @Override
        public SampleSite getFromSite() {
            return fromSite;
        }

        @Override
        public SampleSite getToSite() {
            return toSite;
        }

        @Override
        public ArrayList<SamplePhysicalLink> getPhysicals() {
            return physicals;
        }
        
    }

    private static enum EndpointConnections implements SampleEndpointConnection {

        BRNO_JUNIPER(DEFAULT_SUBNET, Sites.BRNO, Node.BRNO_JUNIPER, 10000),
        PRAGUE_JUNIPER(DEFAULT_SUBNET, Sites.PRAGUE, Node.PRAGUE_JUNIPER, 10000),
        CONTROL(DEFAULT_SUBNET, Sites.NZ_CONTROL, Node.GLIF_JUNIPER, 1000),
        DISTRIBUTION(DEFAULT_SUBNET, Sites.NZ_DISTRIBUTION, Node.GLIF_JUNIPER, 10000);

        private final String subnet;
        private final SampleSite site;
        private final SamplePhysicalNode node;
        private final int capacity;

        private EndpointConnections(String subnet, SampleSite site, SamplePhysicalNode node, int capacity) {
            this.subnet = subnet;
            this.site = site;
            this.node = node;
            this.capacity = capacity;
        }

        @Override
        public String getSubnet() {
            return subnet;
        }

        @Override
        public SampleSite getSite() {
            return site;
        }

        @Override
        public SamplePhysicalNode getNode() {
            return node;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }
        
    }

    @Override
    public <T extends Enum & SamplePhysicalNode> Class<T> getPhysicalNodes() {
        return (Class<T>) Node.class;
    }

    @Override
    public <T extends Enum & SamplePhysicalLink> Class<T> getPhysicalLinks() {
        return (Class<T>) Link.class;
    }

    @Override
    public <T extends Enum & SampleIntersiteLink> Class<T> getIntersiteLinks() {
        return (Class<T>) IntersiteLink.class;
    }

    @Override
    public <T extends Enum & SampleEndpointConnection> Class<T> getEndpointConnections() {
        return (Class<T>) EndpointConnections.class;
    }
    
}
