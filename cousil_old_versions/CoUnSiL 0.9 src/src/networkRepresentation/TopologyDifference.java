/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkRepresentation;

import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class TopologyDifference {
    private final HashSet<LogicalNetworkLink> newLogicalLinks;
    private final HashSet<PhysicalNetworkLink> newPhysicalLinks;
    private final HashSet<EndpointNetworkNode> newAppNodes;
    private final HashSet<LogicalNetworkLink> modifiedLogicalLinks;
    private final HashSet<PhysicalNetworkLink> modifiedPhysicalLinks;
    private final HashSet<EndpointNetworkNode> modifiedAppNodes;
    private final HashSet<LogicalNetworkLink> removedLogicalLinks;
    private final HashSet<PhysicalNetworkLink> removedPhysicalLinks;
    private final HashSet<EndpointNetworkNode> removedAppNodes;

    public TopologyDifference() {
        this.newLogicalLinks = new HashSet<>();
        this.newPhysicalLinks = new HashSet<>();
        this.newAppNodes = new HashSet<>();
        this.modifiedLogicalLinks = new HashSet<>();
        this.modifiedPhysicalLinks = new HashSet<>();
        this.modifiedAppNodes = new HashSet<>();
        this.removedLogicalLinks = new HashSet<>();
        this.removedPhysicalLinks = new HashSet<>();
        this.removedAppNodes = new HashSet<>();
    }

    public TopologyDifference(TopologyDifference src) {
        this.newLogicalLinks = (HashSet<LogicalNetworkLink>) src.newLogicalLinks.clone();
        this.newPhysicalLinks = (HashSet<PhysicalNetworkLink>) src.modifiedPhysicalLinks.clone();
        this.newAppNodes = (HashSet<EndpointNetworkNode>) src.newAppNodes.clone();
        this.modifiedLogicalLinks = (HashSet<LogicalNetworkLink>) src.modifiedLogicalLinks.clone();
        this.modifiedPhysicalLinks = (HashSet<PhysicalNetworkLink>) src.modifiedPhysicalLinks.clone();
        this.modifiedAppNodes = (HashSet<EndpointNetworkNode>) src.modifiedAppNodes.clone();
        this.removedLogicalLinks = (HashSet<LogicalNetworkLink>) src.removedLogicalLinks.clone();
        this.removedPhysicalLinks = (HashSet<PhysicalNetworkLink>) src.removedPhysicalLinks.clone();
        this.removedAppNodes = (HashSet<EndpointNetworkNode>) src.removedAppNodes.clone();
    }

    public boolean add(LogicalNetworkLink link) {
        return newLogicalLinks.add(link);
    }

    public boolean addMultipleLogLinks(Collection<? extends LogicalNetworkLink> links) {
        return newLogicalLinks.addAll(links);
    }

    public boolean modify(LogicalNetworkLink link) {
        return modifiedLogicalLinks.add(link);
    }

    public boolean remove(LogicalNetworkLink link) {
        return removedLogicalLinks.add(link);
    }

    public boolean add(PhysicalNetworkLink link) {
        return newPhysicalLinks.add(link);
    }

    public boolean modify(PhysicalNetworkLink link) {
        return modifiedPhysicalLinks.add(link);
    }

    public boolean remove(PhysicalNetworkLink link) {
        return removedPhysicalLinks.add(link);
    }

    public boolean add(EndpointNetworkNode node) {
        return newAppNodes.add(node);
    }

    public boolean addMultipleAppNodes(Collection<? extends EndpointNetworkNode> nodes) {
        return newAppNodes.addAll(nodes);
    }

    public boolean modify(EndpointNetworkNode node) {
        return modifiedAppNodes.add(node);
    }

    public boolean remove(EndpointNetworkNode node) {
        return removedAppNodes.add(node);
    }

    public HashSet<LogicalNetworkLink> getNewLogicalLinks() {
        return newLogicalLinks;
    }

    public HashSet<PhysicalNetworkLink> getNewPhysicalLinks() {
        return newPhysicalLinks;
    }

    public HashSet<EndpointNetworkNode> getNewAppNodes() {
        return newAppNodes;
    }

    public HashSet<LogicalNetworkLink> getModifiedLogicalLinks() {
        return modifiedLogicalLinks;
    }

    public HashSet<PhysicalNetworkLink> getModifiedPhysicalLinks() {
        return modifiedPhysicalLinks;
    }

    public HashSet<EndpointNetworkNode> getModifiedAppNodes() {
        return modifiedAppNodes;
    }

    public HashSet<LogicalNetworkLink> getRemovedLogicalLinks() {
        return removedLogicalLinks;
    }

    public HashSet<PhysicalNetworkLink> getRemovedPhysicalLinks() {
        return removedPhysicalLinks;
    }

    public HashSet<EndpointNetworkNode> getRemovedAppNodes() {
        return removedAppNodes;
    }

}
