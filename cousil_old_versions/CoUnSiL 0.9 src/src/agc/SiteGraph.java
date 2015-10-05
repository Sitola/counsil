package agc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.NetworkSite;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

/**
 *
 * @author Pavel Troubil
 */
class SiteGraph {

    class IntersiteEdge extends DefaultWeightedEdge {
    }
    private SimpleDirectedWeightedGraph<NetworkSite, IntersiteEdge> g;
    private FloydWarshallShortestPaths<NetworkSite, IntersiteEdge> fw;
    final private boolean cache;
    private double[][] shortestDistances; // indexation relies on external setting of index in NetworkNode object

    public SiteGraph(Collection<EndpointNetworkNode> nodes, Collection<LogicalNetworkLink> links) {
        this(nodes, links, false);
    }

    public SiteGraph(Collection<EndpointNetworkNode> nodes, Collection<LogicalNetworkLink> links, boolean cache) {
        g = new SimpleDirectedWeightedGraph<>(IntersiteEdge.class);
        int maxIndex = -1;
        for (EndpointNetworkNode node : nodes) {
            if (node.getNodeSite() != null) {
                g.addVertex(node.getNodeSite());
            }
            if (node.index > maxIndex) maxIndex = node.index;
        }
        assert maxIndex >= 0;
        maxIndex++;

        for (LogicalNetworkLink link : links) {
            if (link.getFromNode().getNodeSite() != null && link.getToNode().getNodeSite() != null && !link.getFromNode().getNodeSite().equals(link.getToNode().getNodeSite())) {
                IntersiteEdge edge = new SiteGraph.IntersiteEdge();
                g.addEdge(link.getFromNode().getNodeSite(), link.getToNode().getNodeSite(), edge);
                g.setEdgeWeight(edge, link.getLatency());
            }
        }
        fw = new FloydWarshallShortestPaths(g);
        // call lazy calculation
        fw.getShortestPathsCount();
        
        this.cache = cache;
        shortestDistances = new double[maxIndex][maxIndex];
        for (EndpointNetworkNode fromNode : nodes) {
            for (EndpointNetworkNode toNode : nodes) {
                shortestDistances[fromNode.index][toNode.index] = fw.shortestDistance(fromNode.getNodeSite(), toNode.getNodeSite());
            }
        }
    }

    public double getShortestDistance(int from, int to) {
        return shortestDistances[from][to];
    }

    public double getShortestDistance(EndpointNetworkNode from, EndpointNetworkNode to) {
        NetworkSite fromSite = from.getNodeSite();
        NetworkSite toSite = to.getNodeSite();
        
        return fw.shortestDistance(fromSite, toSite);
    }

    public GraphPath<NetworkSite, IntersiteEdge> getShortestPath(EndpointNetworkNode from, EndpointNetworkNode to) {
        return fw.getShortestPath(from.getNodeSite(), to.getNodeSite());
    }

    public double getAverageLatency() {
        double sum = 0.0;
        int count = 0;
        for (NetworkSite s1 : g.vertexSet()) {
            for (NetworkSite s2 : g.vertexSet()) {
                if (s1.equals(s2)) {
                    continue;
                }
                sum += fw.shortestDistance(s1, s2);
                count++;
            }
        }
        if (count > 0) {
            return sum / count;
        }
        return 0.0;
    }

    public double getMedianLatency() {
        ArrayList<Double> latencies = new ArrayList<>();
        for (NetworkSite s1 : g.vertexSet()) {
            for (NetworkSite s2 : g.vertexSet()) {
                if (!s1.equals(s2)) {
                    latencies.add(fw.shortestDistance(s2, s1));
                }
            }
        }
        Collections.sort(latencies);
        return latencies.get(latencies.size() / 2);
    }
    
}
