package networkRepresentation.sampleTopologies;

import agc.ApplicationGroupController;
import java.util.ArrayList;
import java.util.HashMap;
import networkRepresentation.GeneralNetworkLink;
import networkRepresentation.GeneralNetworkNode;
import networkRepresentation.PartiallyKnownNetworkTopology;
import networkRepresentation.PhysicalNetworkLink;
import networkRepresentation.PhysicalNetworkNode;
import networkRepresentation.UnknownNetworkNode;
import org.apache.log4j.Logger;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class SampleTopologyBuilder {
    PartiallyKnownNetworkTopology topology;
    static Logger logger = Logger.getLogger(ApplicationGroupController.class);

    public PartiallyKnownNetworkTopology buildTopology(SampleTopology sample) {
        topology = new PartiallyKnownNetworkTopology();
        
        addAllUnderlayNodes(sample.getPhysicalNodes());
        addAllUnderlayLinks(sample.getPhysicalLinks(), sample.getPhysicalNodes());
        buildIntersiteRoutes(sample.getIntersiteLinks());
        buildNodesAtSites(sample.getEndpointConnections());

        return topology;
    }

    private <T extends Enum & SamplePhysicalNode> void addAllUnderlayNodes(Class<T> c) {
        for (SamplePhysicalNode node : c.getEnumConstants()) {
            if (node.isKnown()) {
                topology.addNetworkNode(new PhysicalNetworkNode(node.getName()));
            } else {
                topology.addNetworkNode(new UnknownNetworkNode(node.getName()));
            }
        }
    }
    
    private <L extends Enum & SamplePhysicalLink, N extends Enum & SamplePhysicalNode> void addAllUnderlayLinks(Class<L> links, Class<N> nodes) {
        HashMap<String, GeneralNetworkNode> nameMap = new HashMap<>();
        for (PhysicalNetworkNode node : topology.getPhysicalNodes()) {
            nameMap.put(node.getNodeName(), node);
        }
        for (UnknownNetworkNode node : topology.getUnknownNetworkNodes()) {
            nameMap.put(node.getNodeName(), node);
        }

        for (SamplePhysicalLink link : links.getEnumConstants()) {
            if (!nameMap.containsKey(link.getFromNode().getName()) ||
                !nameMap.containsKey(link.getToNode().getName())) {
                logger.warn("Cannot add sample link: " + link);
                continue;
            }
            PhysicalNetworkLink forthLink = new PhysicalNetworkLink(link.toString(), link.getCapacity(),
                nameMap.get(link.getFromNode().getName()),
                nameMap.get(link.getToNode().getName()), true);
            topology.addPhysicalLink(forthLink);
            PhysicalNetworkLink backLink = new PhysicalNetworkLink(link.toString() + " reverse", link.getCapacity(),
                nameMap.get(link.getToNode().getName()),
                nameMap.get(link.getFromNode().getName()), true);
            topology.addPhysicalLink(backLink);
            forthLink.setBackLink(backLink);
            backLink.setBackLink(forthLink);
        }
    }
    
    private <R extends Enum & SampleIntersiteLink> void buildIntersiteRoutes(Class<R> routes) {
        HashMap<String, GeneralNetworkNode> nameMap = new HashMap<>();
        for (PhysicalNetworkNode node : topology.getPhysicalNodes()) {
            nameMap.put(node.getNodeName(), node);
        }
        for (UnknownNetworkNode node : topology.getUnknownNetworkNodes()) {
            nameMap.put(node.getNodeName(), node);
        }

        for (SampleIntersiteLink route : routes.getEnumConstants()) {
            final ArrayList<PhysicalNetworkLink> links = new ArrayList<>();
            ArrayList<PhysicalNetworkLink> reverseLinks = new ArrayList<>();
            for (SamplePhysicalLink link : route.getPhysicals()) {
                SamplePhysicalNode sampleFrom = link.getFromNode();
                SamplePhysicalNode sampleTo = link.getToNode();
                GeneralNetworkNode from = nameMap.get(sampleFrom.getName());
                GeneralNetworkNode to = nameMap.get(sampleTo.getName());
                if (from == null || to == null) {
                    logger.warn("Unknown link in intersite route: " + link.toString());
                    continue;
                }
                GeneralNetworkLink graphLink = topology.getNetworkTopologyGraph().getEdge(from, to);
                assert (graphLink instanceof PhysicalNetworkLink) : "Very unexpected link: " + graphLink;
                links.add((PhysicalNetworkLink) graphLink);
                if (((PhysicalNetworkLink) graphLink).getBackLink() != null) {
                    reverseLinks.add(((PhysicalNetworkLink) graphLink).getBackLink());
                }
            }
            topology.setIntersiteRoute(route.getSubnet(), route.getFromSite().getNetworkSite(), route.getToSite().getNetworkSite(), links);
            topology.setIntersiteRoute(route.getSubnet(), route.getToSite().getNetworkSite(), route.getFromSite().getNetworkSite(), reverseLinks);
        }
    }
    
    private <N extends Enum & SampleEndpointConnection> void buildNodesAtSites(Class<N> nodes) {
        HashMap<String, GeneralNetworkNode> nameMap = new HashMap<>();
        for (PhysicalNetworkNode node : topology.getPhysicalNodes()) {
            nameMap.put(node.getNodeName(), node);
        }
        for (UnknownNetworkNode node : topology.getUnknownNetworkNodes()) {
            nameMap.put(node.getNodeName(), node);
        }

        for (SampleEndpointConnection ec : nodes.getEnumConstants()) {
            GeneralNetworkNode node = nameMap.get(ec.getNode().getName());
            topology.setPhysicalNodeAtSite(ec.getSubnet(), ec.getSite().getNetworkSite(), node, ec.getCapacity());
        }
    }
}
