package networkRepresentation;

import com.rits.cloning.Cloner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationProducer;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.jgrapht.graph.DirectedWeightedMultigraph;

/**
 * Representation of partially knowh network topology
 * <p/>
 * User: Pavel Troubil (pavel@ics.muni.cz)
 */
@ThreadSafe
public class PartiallyKnownNetworkTopology implements Cloneable, Serializable {
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    /**
     * This is a synchronized class to work with change status.
     */
    private class ChangedStatus implements Serializable {
        private NetworkTopologyStatus status;

        public ChangedStatus() {
            status = new NetworkTopologyStatus();
            status.setChanged(false);
            status.setChangedStamp(System.currentTimeMillis());
            status.setChangesCount(0);
        }

        synchronized NetworkTopologyStatus getChanged() {
            NetworkTopologyStatus s = new NetworkTopologyStatus();
            s.setChanged(status.isChanged());
            s.setChangedStamp(status.getChangedStamp());
            s.setChangesCount(status.getChangesCount());
            return s;
        }

        synchronized void makeChange() {
            status.setChanged(true);
            status.setChangedStamp(System.currentTimeMillis());
            status.setChangesCount(status.getChangesCount() + 1);
            logger.debug("Status: change made. Currently " );
        }

        synchronized void invalidateChanges() {
            status.setChanged(false);
            status.setChangedStamp(System.currentTimeMillis());
            status.setChangesCount(0);
        }
        
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            
        }
    }

    /**
     * A class holding sets of physical links between pairs of sites
     */
    protected class IntersiteRoutes implements Serializable {
        private final HashMap<String, HashMap<NetworkSite, HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>>>> routes = new HashMap<>();
        
        private void set(String subnet, NetworkSite from, NetworkSite to, ArrayList<PhysicalNetworkLink> links) {
            HashMap<NetworkSite, HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>>> subnetMap;
            if (!routes.containsKey(subnet)) {
                routes.put(subnet, new HashMap<NetworkSite, HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>>>());
            }
            subnetMap = routes.get(subnet);
            HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>> toMap;
            if (!subnetMap.containsKey(from)) {
                subnetMap.put(from, new HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>>());
            }
            toMap = subnetMap.get(from);
            
            toMap.put(to, links);
        }
        
        private ArrayList<PhysicalNetworkLink> get(String subnet, NetworkSite from, NetworkSite to) {
            if (!routes.containsKey(subnet)) {
                return null;
            }
            final HashMap<NetworkSite, HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>>> subnetMap = routes.get(subnet);
            if (!SEARCH_SITE_BY_PREFIX) {
                if (!subnetMap.containsKey(from)) {
                    return null;
                }
                return subnetMap.get(from).get(to);
            }
            
            // Search by prefix name
            NetworkSite realFrom = null;
            for (NetworkSite s : subnetMap.keySet()) {
                if (from.getSiteName().startsWith(s.getSiteName())) {
                    realFrom = s;
                }
            }
            if (realFrom == null) {
                return null;
            }
            final HashMap<NetworkSite, ArrayList<PhysicalNetworkLink>> fromMap = subnetMap.get(realFrom);
            for (NetworkSite s : fromMap.keySet()) {
                if (to.getSiteName().startsWith(s.getSiteName())) {
                    return fromMap.get(s);
                }
            }
            return null;
        }
    }

    protected class PhysicalNodesAtSites implements Serializable {
        HashMap<String, HashMap<NetworkSite, GeneralNetworkNode>> nodes = new HashMap<>();
        HashMap<String, HashMap<NetworkSite, Integer>> capacities = new HashMap<>();
        
        private void set(String subnet, NetworkSite site, GeneralNetworkNode node, int capacity) {
            assert (node instanceof PhysicalNetworkNode || node instanceof UnknownNetworkNode) : "Wrong subclass of GeneralNetworkNode.";
            
            HashMap<NetworkSite, GeneralNetworkNode> subnetMap;
            HashMap<NetworkSite, Integer> capacityMap;
            if (!nodes.containsKey(subnet)) {
                nodes.put(subnet, new HashMap<NetworkSite, GeneralNetworkNode>());
                capacities.put(subnet, new HashMap<NetworkSite, Integer>());
            }
            subnetMap = nodes.get(subnet);
            capacityMap = capacities.get(subnet);
            
            subnetMap.put(site, node);
            capacityMap.put(site, capacity);
        }
        
        private NetworkSite getPrefixSite(String subnet, NetworkSite site) {
            for (NetworkSite s : nodes.get(subnet).keySet()) {
                if (site.getSiteName().startsWith(s.getSiteName())) {
                    return s;
                }
            }
            return null;
        }

        private GeneralNetworkNode getNode(String subnet, NetworkSite site) {
            if (!nodes.containsKey(subnet)) {
                return null;
            }
            if (!SEARCH_SITE_BY_PREFIX) {
                return nodes.get(subnet).get(site);
            }
            return nodes.get(subnet).get(getPrefixSite(subnet, site));
        }

        private Integer getCapacity(String subnet, NetworkSite site) {
            if (!capacities.containsKey(subnet)) {
                return null;
            }
            if (!SEARCH_SITE_BY_PREFIX) {
                return capacities.get(subnet).get(site);
            }
            return capacities.get(subnet).get(getPrefixSite(subnet, site));
        }
    }

    protected DirectedWeightedMultigraph<GeneralNetworkNode, GeneralNetworkLink> networkTopologyGraph;
    protected HashSet<EndpointNetworkNode> endpointNodes;
    protected HashSet<UnknownNetworkNode> unknownNetworkNodes;
    protected HashSet<PhysicalNetworkNode> physicalNodes;
    protected HashSet<PhysicalNetworkLink> physicalLinks;
    protected HashSet<LogicalNetworkLink> logicalLinks;
    private HashMap<String, EndpointSubNetwork> subNets;
    protected final IntersiteRoutes intersiteRoutes = new IntersiteRoutes();
    protected final PhysicalNodesAtSites nodesAtSites = new PhysicalNodesAtSites();
    private ChangedStatus changed;
    private String topologyName;
    private static final double initLinkLatency = 150;
    private static final boolean SEARCH_SITE_BY_PREFIX = true;

    /**
     * This is an empty JavaBean constructor in order to support XMLEncoder and XMLDecoder
     */
    public PartiallyKnownNetworkTopology() {
        this.networkTopologyGraph = new DirectedWeightedMultigraph<>(GeneralNetworkLink.class);
        this.endpointNodes = new HashSet<>();
        this.unknownNetworkNodes = new HashSet<>();
        this.physicalNodes = new HashSet<>();
        this.physicalLinks = new HashSet<>();
        this.logicalLinks = new HashSet<>();
        this.subNets = new HashMap<>();
        changed = new ChangedStatus();
        this.topologyName = new String();
    }

    /*
     * TODO: correspondence between physical and logical links
     */
    
    /**
     * Adds a Network node to the network topology.
     * <p/>
     * Each peer in the network should create respective EndpointNetworkNode instance and add it to the NetworkTopology
     * <p/>
     *
     * @param node Network node to add
     */
    public void addNetworkNode(EndpointNetworkNode node) {
        
        assert this.networkTopologyGraph != null : "Network topology hasn't been properly initialized!";
        assert node != null : "Attempted to add null node to the network";
        
        logger.debug("Adding network node "+node.getNodeName()+":");
        
        if (!this.networkTopologyGraph.containsVertex(node)) {

            logger.debug("    The node is new, adding");

            // Update list of available subnets    
            for (EndpointNodeInterface ni : node.getNodeInterfaces()) {
                if (ni.getParentNode() == null) ni.setParentNode(node);
                if (!subNets.containsKey(ni.getSubnet())) {
                    subNets.put(ni.getSubnet(), new EndpointSubNetwork(ni.getSubnet(), new CopyOnWriteArraySet<EndpointNodeInterface>()));
                }
                EndpointSubNetwork subNetwork = subNets.get(ni.getSubnet());
                subNetwork.addSubNetworkNodeInterface(ni);
            }

            this.networkTopologyGraph.addVertex(node);
            this.endpointNodes.add(node);

            // Create the private subnetwork topology for added node
            for (EndpointNodeInterface localInterface : node.getNodeInterfaces()) {
                GeneralNetworkNode localPhysicalNode = nodesAtSites.getNode(localInterface.getSubnet(), node.getNodeSite());
                PhysicalNetworkLink localPhysicalLinkUp = addLastmilePhysicalLinks(node, localInterface, localPhysicalNode);
                PhysicalNetworkLink localPhysicalLinkDown = localPhysicalLinkUp != null ? localPhysicalLinkUp.getBackLink() : null;
                if (localPhysicalLinkUp != null) addPhysicalLink(localPhysicalLinkUp);
                if (localPhysicalLinkDown != null) addPhysicalLink(localPhysicalLinkDown);
                for (EndpointNodeInterface toIface : subNets.get(localInterface.getSubnet()).getSubNetworkNodeInterfaces()) {
                    EndpointNetworkNode toNode = toIface.getParentNode();
                    LambdaLink lambda = null;
                    if (node == toNode) {
                        continue;
                    }
                    if (toNode == null) {
                        throw new NullPointerException("Parrent node of Iface "+toIface+" is null!");
                    }
                    double capacity = (localInterface.getBandwidth() < toIface.getBandwidth()) ? localInterface.getBandwidth() : toIface.getBandwidth();

                    // Check for potential LambdaLinks
                    if ((localInterface.getLambdaLinkEndpoint() != null) && (toIface.getLambdaLinkEndpoint() != null)) {
                        // TODO: How to handle the capacity for a lambda? This is obviously wrong!!!
                        lambda = new LambdaLink(localInterface.getLambdaLinkEndpoint(), toIface.getLambdaLinkEndpoint(), capacity);
                    }
                    GeneralNetworkNode remotePhysicalNode = nodesAtSites.getNode(localInterface.getSubnet(), toNode.getNodeSite());
                    PhysicalNetworkLink remotePhysicalLinkUp = null;
                    PhysicalNetworkLink remotePhysicalLinkDown = null;
                    if (remotePhysicalNode != null) {
                        for (GeneralNetworkLink l : networkTopologyGraph.getAllEdges(toNode, remotePhysicalNode)) {
                            if (!(l instanceof PhysicalNetworkLink)) {
                                continue;
                            }
                            PhysicalNetworkLink p = (PhysicalNetworkLink) l;
                            if (p.getEndpointNodeInterface().equals(toIface)) {
                                remotePhysicalLinkUp = p;
                                break;
                            }
                        }
                        if (remotePhysicalLinkUp != null) {
                            remotePhysicalLinkDown = remotePhysicalLinkUp.getBackLink();
                        }
                    }
                    LogicalNetworkLink forthLink = new LogicalNetworkLink();
                    forthLink.setParameters(capacity, initLinkLatency, node, localInterface, toNode, toIface);
                    associateNewLogicalLink(localInterface.getSubnet(), forthLink, localPhysicalLinkUp, remotePhysicalLinkDown);
                    logicalLinks.add(forthLink);
                    if (!this.networkTopologyGraph.addEdge(node, toNode, forthLink)) {
                        throw new RuntimeException("Failed to add the edge");
                    }
                    if (lambda != null) {
                        forthLink.associateLambda(lambda);
                        lambda.addNetworkLink(forthLink);
                    }
//                    if (intersiteRoutes.get(localInterface.getSubnet(), node.getNodeSite(), toNode.getNodeSite()))
                    LogicalNetworkLink backLink = new LogicalNetworkLink();
                    backLink.setParameters(capacity, initLinkLatency, toNode, toIface, node, localInterface);
                    associateNewLogicalLink(localInterface.getSubnet(), backLink, localPhysicalLinkDown, remotePhysicalLinkUp);
                    logicalLinks.add(backLink);
                    if (!this.networkTopologyGraph.addEdge(toNode, node, backLink)) {
                        throw new RuntimeException("Failed to add the edge");
                    }
                    if (lambda != null) {
                        backLink.associateLambda(lambda);
                        lambda.addNetworkLink(backLink);
                    }
                }
            }

            changed.makeChange();
        } else { // The node is already present in the topology. Only update its variable  fields

            Logger.getLogger(this.getClass()).log(Priority.INFO, "    The node is already present, updating");

            EndpointNetworkNode oldNode = null;
            for (GeneralNetworkNode tmpNode : this.networkTopologyGraph.vertexSet()) {
                if (node.equals(tmpNode)) {
                    oldNode = (EndpointNetworkNode) tmpNode;
                    break;
                }
            }
            
            if (oldNode == null) throw new NullPointerException("The topology reported a node ("+node+") is present, but found none!");
            
            if (oldNode.update(node)) {
                changed.makeChange();
                Logger.getLogger(this.getClass()).log(Priority.INFO, "    Node updated");
            } else {
                Logger.getLogger(this.getClass()).log(Priority.INFO, "    Unchanged");
            }
        }
    }

    private void associateNewLogicalLink(String subnet, LogicalNetworkLink logicalLink, PhysicalNetworkLink localPhysicalLinkUp, PhysicalNetworkLink remotePhysicalLinkDown) {
        ArrayList<PhysicalNetworkLink> physicals = intersiteRoutes.get(subnet, logicalLink.getFromNode().getNodeSite(), logicalLink.getToNode().getNodeSite());
        if (physicals == null) {
            return;
        }
        for (PhysicalNetworkLink linkToAssoc : physicals) {
            associateLinks(linkToAssoc, logicalLink);
        }
        associateLinks(localPhysicalLinkUp, logicalLink);
        associateLinks(remotePhysicalLinkDown, logicalLink);
    }

    private PhysicalNetworkLink addLastmilePhysicalLinks(EndpointNetworkNode node, EndpointNodeInterface localInterface, GeneralNetworkNode localPhysicalNode) {
        PhysicalNetworkLink localPhysicalLinkUp;
        if (localPhysicalNode == null) {
            logger.warn("Cannot create physical link for iface " + localInterface.toString() + " at " + node.toString());
            return null;
        } else {
            // From endpoint to physical node
            localPhysicalLinkUp = new PhysicalNetworkLink("",
                    nodesAtSites.getCapacity(localInterface.getSubnet(), node.getNodeSite()),
                    node, localPhysicalNode, true, localInterface);
            physicalLinks.add(localPhysicalLinkUp);
            // from physical node to endpoint
            PhysicalNetworkLink localPhysicalLinkDown = new PhysicalNetworkLink("",
                    nodesAtSites.getCapacity(localInterface.getSubnet(), node.getNodeSite()),
                    localPhysicalNode, node, true, localInterface, localPhysicalLinkUp);
            physicalLinks.add(localPhysicalLinkDown);
            localPhysicalLinkUp.setBackLink(localPhysicalLinkDown);
            return localPhysicalLinkUp;
        }
    }

    /**
     * Adds a physical network node to the network topology.
     * <p/>
     * Some peers in the network might be informed about some part of physical network topology. For the known network nodes, (routers, switches), these peers should create respective instances of the PhysicalNetworkNode and add it to the network topology.
     * <p/>
     * @param node Physical network node to add
     */
    public void addNetworkNode(PhysicalNetworkNode node) {
        assert this.networkTopologyGraph != null : "Network topology hasn't been properly initialized!";
        assert node != null : "Attempted to add null node to the network";

        System.out.println("========== ADDING NETWORK NODE: "+node.getClass()+" ("+node+")");
//        if (true) throw new UnsupportedOperationException("");

        if (this.networkTopologyGraph.containsVertex(node)) {
            PartiallyKnownNetworkTopology.logger.warn("Network node " + node.getNodeName() + " is already in the network topology.");
            return;
        }

        this.networkTopologyGraph.addVertex(node);
        this.physicalNodes.add(node);
        changed.makeChange();
    }

    /**
     * Adds a network node representing an unknown topology subnetwork to the network topology.
     * <p/>
     * Peers aware of subnetworks with unknown internal topology should create respective instances of the UnknownNetworkNode and add it to the network topology.
     * <p/>
     * @param node Physical network node to add
     */
    public void addNetworkNode(UnknownNetworkNode node) {
        assert this.networkTopologyGraph != null : "Network topology hasn't been properly initialized!";
        assert node != null : "Attempted to add null node to the network";

        if (this.networkTopologyGraph.containsVertex(node)) {
            PartiallyKnownNetworkTopology.logger.warn("Network node " + node.getNodeName() + " is already in the network topology.");
            return;
        }

        this.networkTopologyGraph.addVertex(node);
        this.unknownNetworkNodes.add(node);
        changed.makeChange();
    }

    /**
     * Removes an endpoint network node from the network topology.
     * <p/>
     *
     * @param node NetworkNode to remove
     */
    public void removeNetworkNode(EndpointNetworkNode node) {
        if (this.networkTopologyGraph != null) {
            if (node != null) {
                if (this.networkTopologyGraph.containsVertex(node)) {
                    // Update list of available subnets
                    for (EndpointNodeInterface ni : node.getNodeInterfaces()) {
                        EndpointSubNetwork subNetwork = subNets.get(ni.getSubnet());
                        subNetwork.removeSubNetworkNodeInterface(ni);
                    }

                    // Do not forget to remove all edges comming from the node
                    // Only logical links are allowed between two endpoints
                    for (EndpointNetworkNode otherNode : this.endpointNodes) {
                        Set<GeneralNetworkLink> removedForthLinks = this.networkTopologyGraph.removeAllEdges(node, otherNode);
                        for (GeneralNetworkLink link : removedForthLinks) {
                            System.out.println("TOPOLOGY: Removing link <"+link.toString()+">");
                            removeLinkAssociations((LogicalNetworkLink) link);
                            this.logicalLinks.remove((LogicalNetworkLink) link);
                        }
                        Set<GeneralNetworkLink> removedBackLinks = this.networkTopologyGraph.removeAllEdges(otherNode, node);
                        for (GeneralNetworkLink link : removedBackLinks) {
                            System.out.println("TOPOLOGY: Removing link <"+link.toString()+">");
                            removeLinkAssociations((LogicalNetworkLink) link);
                            this.logicalLinks.remove((LogicalNetworkLink) link);
                        }
                    }

                    // While only physical links are allowed among any other couple of nodes
                    for (UnknownNetworkNode otherNode : this.unknownNetworkNodes) {
                        GeneralNetworkLink removedForthLink = this.networkTopologyGraph.removeEdge(node, otherNode);
                        if (removedForthLink != null) {
                            removeLinkAssociations((PhysicalNetworkLink) removedForthLink);
                            this.physicalLinks.remove((PhysicalNetworkLink) removedForthLink);
                        }
                        GeneralNetworkLink removedBackLink = this.networkTopologyGraph.removeEdge(node, otherNode);
                        if (removedBackLink != null) {
                            removeLinkAssociations((PhysicalNetworkLink) removedBackLink);
                            this.physicalLinks.remove((PhysicalNetworkLink) removedForthLink);
                        }
                    }
                    for (PhysicalNetworkNode otherNode : this.physicalNodes) {
                        GeneralNetworkLink removedForthLink = this.networkTopologyGraph.removeEdge(node, otherNode);
                        if (removedForthLink != null) {
                            removeLinkAssociations((PhysicalNetworkLink) removedForthLink);
                            this.physicalLinks.remove((PhysicalNetworkLink) removedForthLink);
                        }
                        GeneralNetworkLink removedBackLink = this.networkTopologyGraph.removeEdge(node, otherNode);
                        if (removedBackLink != null) {
                            removeLinkAssociations((PhysicalNetworkLink) removedBackLink);
                            this.physicalLinks.remove((PhysicalNetworkLink) removedForthLink);
                        }
                    }
                    
                    // And finaly remove the node vertex
                    this.networkTopologyGraph.removeVertex(node);
                    this.endpointNodes.remove(node);

                    changed.makeChange();
                } else {
                    PartiallyKnownNetworkTopology.logger.warn("No such network node in network topology.");
                }
            }
        }
    }

    public void removeNetworkNode(PhysicalNetworkNode node) {
        if (this.networkTopologyGraph == null || node == null) {
            return;
        }
        
        if (this.networkTopologyGraph.containsVertex(node)) {
            // Remove all edges to another nodes
            for (PhysicalNetworkLink link : this.physicalLinks) {
                if (link.fromNode.equals(node)) {
                    this.networkTopologyGraph.removeEdge(node, link.toNode);
                    this.physicalLinks.remove(link);
                }
                if (link.toNode.equals(node)) {
                    this.networkTopologyGraph.removeEdge(link.fromNode, node);
                    this.physicalLinks.remove(link);
                            
                }
            }
            
            // Remove the vertex
            this.networkTopologyGraph.removeVertex(node);
            this.physicalNodes.remove(node);
                    
            changed.makeChange();
        } else {
            PartiallyKnownNetworkTopology.logger.warn("No such network node in network topology.");
        }
    }
    
    public void removeNetworkNode(UnknownNetworkNode node) {
        if (this.networkTopologyGraph == null || node == null) {
            return;
        }
        
        if (this.networkTopologyGraph.containsVertex(node)) {
            // Remove all edges to another nodes
            for (PhysicalNetworkLink link : this.physicalLinks) {
                if (link.fromNode.equals(node)) {
                    this.networkTopologyGraph.removeEdge(node, link.toNode);
                    this.physicalLinks.remove(link);
                }
                if (link.toNode.equals(node)) {
                    this.networkTopologyGraph.removeEdge(link.fromNode, node);
                    this.physicalLinks.remove(link);
                            
                }
            }
            
            // Remove the vertex
            this.networkTopologyGraph.removeVertex(node);
            this.unknownNetworkNodes.remove(node);
                    
            changed.makeChange();
        } else {
            PartiallyKnownNetworkTopology.logger.warn("No such network node in network topology.");
        }
    }

    public void addPhysicalLink(PhysicalNetworkLink link) {
        assert this.networkTopologyGraph != null : "Network topology hasn't been properly initialized!";
        assert link != null : "Attempted to add null link to the network!";
        
        if (this.networkTopologyGraph.containsEdge(link)) {
            PartiallyKnownNetworkTopology.logger.warn("Network edge " + link.getLinkName() + " is already in the network topology.");
            return;
        }
        
        this.networkTopologyGraph.addEdge(link.fromNode, link.toNode, link);
        this.physicalLinks.add(link);
        
        changed.makeChange();
    }

    public void removeLink(PhysicalNetworkLink link) {
        if (this.networkTopologyGraph == null || link == null) {
            return;
        }
        
        // remove associations with logical links
        this.removeLinkAssociations(link);
        
        //Remove from the structures
        this.networkTopologyGraph.removeEdge(link);
        this.physicalLinks.remove(link);
        
        if (changed != null) {
            changed.makeChange(); // otherwise, the topology is just a snapshot and no monitoring is required
        }
    }
    
    public void removeLink(LogicalNetworkLink link) {
        if (this.networkTopologyGraph == null || link == null) {
            return;
        }
        
        // remove associations with logical links
        this.removeLinkAssociations(link);
        
        //Remove from the structures
        this.networkTopologyGraph.removeEdge(link);
        this.logicalLinks.remove(link);
        
        if (changed != null) { 
            changed.makeChange(); // otherwise, the topology is just a snapshot and no monitoring is required
        }
    }
    
    public void associateLinks(PhysicalNetworkLink physical, LogicalNetworkLink logical) {
        assert physical != null : "Passed physical link is null!";
        assert logical != null : "Passed logical link is null!";
        
        physical.addTraversingLogicalLink(logical);
        logical.addPhysicalLink(physical);
        logical.setCapacity(Math.min(logical.getCapacity(), physical.getCapacity()));
        
        changed.makeChange();
    }

    public ArrayList<EndpointNetworkNode> getSiteEndpointNodes(NetworkSite site) {
        ArrayList<EndpointNetworkNode> siteNodes = new ArrayList<EndpointNetworkNode>();
        
        for (EndpointNetworkNode node : this.endpointNodes) {
            if (node.getNodeSite().equals(site)) {
                siteNodes.add(node);
            }
        }
        return siteNodes;
    }
    
    /**
     * Gets graph representation of the network topology
     * <p/>
     *
     * @return network topology graph
     */
    public DirectedWeightedMultigraph<GeneralNetworkNode, GeneralNetworkLink> getNetworkTopologyGraph() {
        return networkTopologyGraph;
    }


    /**
     * Returns complete status of network topology, i.e. changed flag plus last changed timestamp.
     * This is worked upon and returned atomically.
     * <p/>
     *
     * @return status of network topology
     */
    public NetworkTopologyStatus getStatus() {
        return changed.getChanged();
    }

    /**
     * Returns snapshot of the network topology graph to work upon. It also resets changed status on
     * current NetworkTopology object.
     * <p/>
     *
     * @param networkTopology to make snapshot of
     * @return snapshot
     * @throws CloneNotSupportedException when networkTopology is not cloneable
     */
    static public PartiallyKnownNetworkTopology getCurrentSnapshot(PartiallyKnownNetworkTopology networkTopology) {
        Object copy = null;
//        try {
            // Write the object out to a byte array
        
//            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            XMLEncoder xmlenc = new XMLEncoder(output);
//            xmlenc.writeObject(networkTopology);
//            xmlenc.close();
//
//            XMLDecoder xmldec = new XMLDecoder(new ByteArrayInputStream(output.toByteArray()));
//            copy = xmldec.readObject();

            Cloner cloner = new Cloner();
            copy = cloner.deepClone(networkTopology);

//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            ObjectOutputStream out = new ObjectOutputStream(bos);
//            out.writeObject(networkTopology);
//            out.flush();
//            out.close();
        
            // Make an input stream from the byte array and read
            // a copy of the object back in.
//            ObjectInputStream in = new ObjectInputStream(
//                new ByteArrayInputStream(
//                    bos.toByteArray()));
//            copy = in.readObject();
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//        }
//        catch(ClassNotFoundException cnfe) {
//            cnfe.printStackTrace();
//        }

        PartiallyKnownNetworkTopology copyTopology = (PartiallyKnownNetworkTopology) copy;
        copyTopology.resetChangedStatus();

        return copyTopology;

        /*
        PartiallyKnownNetworkTopology n = new PartiallyKnownNetworkTopology();

        n.networkTopologyGraph = null;
        n.endpointNodes        = new HashSet<EndpointNetworkNode>(networkTopology.endpointNodes);
        n.physicalNodes        = new HashSet<PhysicalNetworkNode>(networkTopology.physicalNodes);
        n.unknownNetworkNodes  = new HashSet<UnknownNetworkNode> (networkTopology.unknownNetworkNodes);
        n.logicalLinks         = new HashSet<LogicalNetworkLink> (); //networkTopology.logicalLinks);
        n.physicalLinks        = new HashSet<PhysicalNetworkLink>(networkTopology.physicalLinks);

        for (LogicalNetworkLink link : networkTopology.getLogicalLinks()) {
            n.logicalLinks.add((LogicalNetworkLink) link.clone());
        }

        n.subNets              = new HashMap<String, EndpointSubNetwork>(networkTopology.subNets);
        n.topologyName         = networkTopology.topologyName;

        networkTopology.changed.invalidateChanges();
        return n;
        */
    }
    
    public void resetChangedStatus() {
        if (changed == null) {
            changed = new ChangedStatus();
        }
        changed.invalidateChanges();
    }

    public void switchNetworkLinkUp(GeneralNetworkLink linkToBringUp) {
        assert linkToBringUp != null;
        GeneralNetworkLink linkToWorkOn = this.findEdge(linkToBringUp);
        assert linkToWorkOn != null;
        if (this.networkTopologyGraph.containsEdge(linkToWorkOn)) {
            if (!linkToWorkOn.isActive()) {
                linkToWorkOn.setActive(true);
                changed.makeChange();
            } else {
                PartiallyKnownNetworkTopology.logger.warn("" + linkToWorkOn + " was already up.");
            }
        } else {
            PartiallyKnownNetworkTopology.logger.error("Failed to find edge " + linkToWorkOn + " to set up.");
        }
    }

    public void switchNetworkLinkDown(GeneralNetworkLink linkToShutDown) {
        assert linkToShutDown != null;
        GeneralNetworkLink linkToWorkOn = this.findEdge(linkToShutDown);
        assert linkToWorkOn != null;
        if (this.networkTopologyGraph.containsEdge(linkToWorkOn)) {
            if (linkToWorkOn.isActive()) {
                linkToWorkOn.setActive(false);
                changed.makeChange();
            } else {
                PartiallyKnownNetworkTopology.logger.debug("" + linkToWorkOn + " was already down.");
            }
        } else {
            PartiallyKnownNetworkTopology.logger.error("Failed to find edge " + linkToWorkOn + " to set down.");
        }
    }

    public void setNetworkTopologyGraph(DirectedWeightedMultigraph<GeneralNetworkNode, GeneralNetworkLink> networkTopologyGraph) {
        this.networkTopologyGraph = networkTopologyGraph;
    }


    public HashMap<String, EndpointSubNetwork> getSubNets() {
        return subNets;
    }

    public void setSubNets(HashMap<String, EndpointSubNetwork> subNets) {
        this.subNets = subNets;
    }

    public String getName() {
      return topologyName;
    }

    public void setName(String name) {
      this.topologyName = name;
    }

    public HashSet<PhysicalNetworkLink> getPhysicalLinks() {
        return physicalLinks;
    }

    public HashSet<PhysicalNetworkNode> getPhysicalNodes() {
        return physicalNodes;
    }

    public HashSet<UnknownNetworkNode> getUnknownNetworkNodes() {
        return unknownNetworkNodes;
    }

    public HashSet<EndpointNetworkNode> getEndpointNodes() {
        return endpointNodes;
    }

    public HashSet<LogicalNetworkLink> getLogicalLinks() {
        return logicalLinks;
    }

    private GeneralNetworkLink findEdge(GeneralNetworkLink matchLink) {
        for (GeneralNetworkLink graphLink : this.networkTopologyGraph.edgeSet()) {
            if (graphLink.equals(matchLink)) {
                return graphLink;
            }
        }
        return null;
    }

    private GeneralNetworkNode findNode(GeneralNetworkNode matchNode) {
        for (GeneralNetworkNode graphNode : this.networkTopologyGraph.vertexSet()) {
            if (graphNode.equals(matchNode)) {
                return graphNode;
            }
        }
        return null;
    }

    public void removeLinkAssociations(LogicalNetworkLink logical) {
        for (PhysicalNetworkLink physical : logical.getPhysicalLinksOnThePath()) {
            physical.removeTraversingLogicalLink(logical);
        }
        logical.removeAllPhysicalLinks();
    }

    public void removeLinkAssociations(PhysicalNetworkLink physical) {
        for (LogicalNetworkLink logical : physical.getTraversingLogicalLinks()) {
            logical.removePhysicalLink(physical);
        }
        physical.clearTraversingLogicalLinks();
    }

    public void setIntersiteRoute(String subnet, NetworkSite from, NetworkSite to, ArrayList<PhysicalNetworkLink> links) {
        this.intersiteRoutes.set(subnet, from, to, links);
    }

    public void setPhysicalNodeAtSite(String subnet, NetworkSite site, GeneralNetworkNode node, int capacity) {
        this.nodesAtSites.set(subnet, site, node, capacity);
    }

    public void print() {
        System.out.println("==== Network topology ====");
        
        // Print network nodes, with their appliactions
        System.out.println("Nodes:");
        for (GeneralNetworkNode node : endpointNodes) {
            System.out.println("  "+node+", running applications:");
            if (node instanceof EndpointNetworkNode) {
                for (MediaApplication app : ((EndpointNetworkNode) node).getNodeApplications()) {
                    System.out.println("    "+app);
                }
            }
        }
        /**/
        
        // Print individual logical links and their mapping to physical links
        System.out.println("");
        System.out.println("Logical links:");
        for (LogicalNetworkLink link : getLogicalLinks()) {
            System.out.println("  "+link.fromNode.getNodeName()+" -> "+link.toNode.getNodeName()+" @"+link.getCapacity()+" ("+link.getLatency()+" ms):");
            for (PhysicalNetworkLink plink : link.getPhysicalLinksOnThePath()) {
                System.out.println("    "+plink.fromNode.getNodeName()+" -> "+plink.toNode.getNodeName() + " @"+plink.getCapacity() );
            }
        }
        /**/
    }
    
    public boolean deepEquals(PartiallyKnownNetworkTopology that, boolean unique) {
        if (this == that) return (!unique);
        
        if (unique) {
            if (this.getEndpointNodes() == that.getEndpointNodes()) return false;
            if (this.getPhysicalNodes() == that.getPhysicalNodes()) return false;
            if (this.getUnknownNetworkNodes() == that.getUnknownNetworkNodes()) return false;

            if (this.getLogicalLinks() == that.getLogicalLinks()) return false;
            if (this.getPhysicalLinks() == that.getPhysicalLinks()) return false;
            
            if (this.getNetworkTopologyGraph() == that.getNetworkTopologyGraph()) return false;
        }
        
        if (! this.getEndpointNodes().equals(that.getEndpointNodes())) return false;
        if (! this.getPhysicalNodes().equals(that.getPhysicalNodes())) return false;
        if (! this.getUnknownNetworkNodes().equals(that.getUnknownNetworkNodes())) return false;
        
        if (! this.getLogicalLinks().equals(that.getLogicalLinks())) return false;
        if (! this.getPhysicalLinks().equals(that.getPhysicalLinks())) return false;
        
        LinkedList<GeneralNetworkNode> thisNodes = new LinkedList<>(), thatNodes = new LinkedList<>();
        thisNodes.addAll(this.getEndpointNodes());
        thisNodes.addAll(this.getPhysicalNodes());
        thisNodes.addAll(this.getUnknownNetworkNodes());
            
        thatNodes.addAll(that.getEndpointNodes());
        thatNodes.addAll(that.getPhysicalNodes());
        thatNodes.addAll(that.getUnknownNetworkNodes());
        
        for (GeneralNetworkNode thisNode : thisNodes) {
            boolean found = false;
            for (GeneralNetworkNode thatNode : thatNodes) {
                if (thisNode.equals(thatNode)) {
                    if (unique && thisNode == thatNode) return false;
                    if (found) throw new IllegalStateException("Node " + thisNode + " matches multiple nodes in the opposing collection!");
                    if (! thisNode.deepEquals(thatNode, unique)) return false;
                    found = true;
                }
            }
        }
        
        
        LinkedList<GeneralNetworkLink> thisLinks = new LinkedList<>(), thatLinks = new LinkedList<>();
        thisLinks.addAll(this.getLogicalLinks());
        thisLinks.addAll(this.getPhysicalLinks());
        thatLinks.addAll(that.getLogicalLinks());
        thatLinks.addAll(that.getPhysicalLinks());
            
        for (GeneralNetworkLink thisLink : thisLinks) {
            boolean found = false;
            for (GeneralNetworkLink thatLink : thatLinks) {
                if (thisLink.equals(thatLink)) {
                    if (unique && thisLink == thatLink) return false;
                    if (found) throw new IllegalStateException("Link " + thisLink + " matches multiple links in the opposing collection!");
                    if (! thisLink.deepEquals(thatLink, unique)) return false;
                    found = true;
                }
            }
        }
        
        
        for (GeneralNetworkNode thisNode : this.getNetworkTopologyGraph().vertexSet()) {
            boolean found = false;
            for (GeneralNetworkNode thatNode : that.getNetworkTopologyGraph().vertexSet()) {
                if (thisNode.equals(thatNode)) {
                    if (unique && thisNode == thatNode) return false;
                    if (found) throw new IllegalStateException("Node " + thisNode + " matches multiple nodes in the opposing collection!");
                    if (! thisNode.deepEquals(thatNode, unique)) return false;
                    found = true;
                }
            }
        }
        
        
        for (GeneralNetworkLink thisLink : this.getNetworkTopologyGraph().edgeSet()) {
            boolean found = false;
            for (GeneralNetworkLink thatLink : that.getNetworkTopologyGraph().edgeSet()) {
                if (thisLink.equals(thatLink)) {
                    if (unique && thisLink == thatLink) return false;
                    if (found) throw new IllegalStateException("Link " + thisLink + " matches multiple links in the opposing collection!");
                    if (! thisLink.deepEquals(thatLink, unique)) return false;
                    found = true;
                }
            }
        }
        

        return true; 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.endpointNodes);
        hash = 67 * hash + Objects.hashCode(this.unknownNetworkNodes);
        hash = 67 * hash + Objects.hashCode(this.physicalNodes);
        hash = 67 * hash + Objects.hashCode(this.physicalLinks);
        hash = 67 * hash + Objects.hashCode(this.logicalLinks);
        hash = 67 * hash + Objects.hashCode(this.subNets);
        hash = 67 * hash + Objects.hashCode(this.topologyName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PartiallyKnownNetworkTopology other = (PartiallyKnownNetworkTopology) obj;
        
        // NOTE: DirectedWeightedMultigraph.equals() does not work properly!
        if (!Objects.equals(this.endpointNodes, other.endpointNodes)) {
            System.out.println("Endpoints do not equal");
            return false;
        }
        if (!Objects.equals(this.unknownNetworkNodes, other.unknownNetworkNodes)) {
            System.out.println("Unknowns do not equal");
            return false;
        }
        if (!Objects.equals(this.physicalNodes, other.physicalNodes)) {
            System.out.println("Physical nodes do not equal");
            return false;
        }
        if (!Objects.equals(this.physicalLinks, other.physicalLinks)) {
            System.out.println("Physical links do not equal");
            return false;
        }
        if (!Objects.equals(this.logicalLinks, other.logicalLinks)) {
            System.out.println("Logical links do not equal");
            return false;
        }
        if (!Objects.equals(this.subNets, other.subNets)) {
            System.out.println("Subnets do not equal");
            return false;
        }
        if (!Objects.equals(this.topologyName, other.topologyName)) {
            System.out.println("Names do not equal");
            return false;
        }
        return true;
    }
}
