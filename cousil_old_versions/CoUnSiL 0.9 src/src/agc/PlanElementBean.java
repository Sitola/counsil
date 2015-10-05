/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package agc;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import mediaAppFactory.MediaApplicationDistributor;
import mediaAppFactory.MediaApplicationProducer;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.PartiallyKnownNetworkTopology;

/**
 *
 * @author maara
 */
public class PlanElementBean implements Serializable {
    
    public static class Target implements Serializable {

        private final String targetUuid;
        private final String targetIp;
        private final String targetMask;
        private final String format;
        private String targetPort;

        public Target(String targetIp, String targetMask, String format) {
            this.targetIp = targetIp;
            this.targetMask = targetMask;
            this.format = format;
            targetUuid = null;
        }

        public Target(String targetIp, String targetMask, String format, String targetPort) {
            this.targetIp = targetIp;
            this.targetMask = targetMask;
            this.format = format;
            this.targetPort = targetPort;
            this.targetUuid = null;
        }

        public Target(String targetIp, String targetMask, String format, String targetPort, String targetUuid) {
            this.targetIp = targetIp;
            this.targetMask = targetMask;
            this.format = format;
            this.targetPort = targetPort;
            this.targetUuid = targetUuid;
        }

        public void setTargetPort(String targetPort) {
            this.targetPort = targetPort;
        }
        
        public String getTargetIp() {
            return targetIp;
        }

        public String getTargetMask() {
            return targetMask;
        }

        public String getFormat() {
            return format;
        }

        public String getTargetPort() {
            if (targetPort == null) throw new NullPointerException("The target port was not set yet!");
            return targetPort;
        }
        
        @Override
        public String toString() {
            return "" + this.targetIp + "/" + this.targetMask + ":" + targetPort + " @" + this.format;
        }

        public String getTargetUuid() {
            return targetUuid;
        }
        
        
    }

    public static class Plan extends HashMap<String, PlanElementBean> {
        // TODO: Modify the plan for easier tree-search
        
        public Plan() {
        }
        
        public PlanElementBean getPlanElementBean(MediaApplication app) {
            return get(app.getUuid());
        }
        
        public void printPlan(PrintStream out) {
            for (PlanElementBean el : values()) {
                out.println("  App " + el.getAppName() + " receiving at port " + el.getSourcePort() + " will be sending to:");
                for (Target t : el.getTargets()) {
                    out.println("    " + t);
                }
            }
        }
    }
    
    /**
     * A wrapper for Plan, class, creates the distribution trees and allows modifications of the plan.
     * Does not account for blending with multiple predecessors per application!
     */
    public static class ExtendedPlan {
        
        public final Map<MediaApplication, MediaApplication> predecessorMap;
        public final Set<MediaApplication> knownMediaApplications;
        public final Plan plan;
        
        public ExtendedPlan(Plan plan, PartiallyKnownNetworkTopology topo) {
            if (plan == null) {
                plan = new Plan();
            }
            
            HashMap<MediaApplication, MediaApplication> tmpPredecessorMap = new HashMap<>();
            this.plan = plan;
            
            HashSet<MediaApplication> apps = new HashSet<>();
            HashMap<PlanElementBean, MediaApplication> appToPlanElementMap = new HashMap<>();
            
            for (EndpointNetworkNode node : topo.getEndpointNodes()) {
                apps.addAll(node.getNodeApplications());
            }
            
            for (MediaApplication app : apps) {
                PlanElementBean pe = plan.getPlanElementBean(app);
                if (pe != null) {
                    appToPlanElementMap.put(pe, app);
                }
            }

            for (Map.Entry<String, PlanElementBean> predecessorEntry : plan.entrySet()) {
                MediaApplication predecessorApp = null;
                for (MediaApplication app : apps) {
                    if (app.getUuid().equals(predecessorEntry.getKey())) {
                        predecessorApp = app;
                        break;
                    }
                }
                
                if (predecessorApp == null) {
                    continue;
                }
                
                for (int i = 0; i < predecessorEntry.getValue().getTargets().size(); i++) {
                    
                    EndpointNetworkNode targetNode = null;
                    for (EndpointNetworkNode node : topo.getEndpointNodes()) {
                        for (EndpointNodeInterface iface : node.getNodeInterfaces()) {
                            if (iface.getIpAddress().equals(predecessorEntry.getValue().getTargetIPs().get(i))) {
                                targetNode = node;
                                break;
                            }
                        }
                        if (targetNode != null) break;
                    }
                    
                    if (targetNode == null) continue;
                    
                    String targetPort = predecessorEntry.getValue().getTargetPorts().get(i);
                    MediaApplication targetApp = null;
                    
                    for (MediaApplication app : targetNode.getNodeApplications()) {
                        PlanElementBean pe = plan.getPlanElementBean(app);
                        if (pe == null) continue;
                        if (pe.getSourcePort() != null && pe.getSourcePort().equals(targetPort)) {
                            targetApp = app;
                            break;
                        }
                    }
                    
                    if (targetApp == null) continue;
                    
                    tmpPredecessorMap.put(targetApp, predecessorApp);
                }
            }
            
            predecessorMap = Collections.unmodifiableMap(tmpPredecessorMap);
            knownMediaApplications = Collections.unmodifiableSet(apps);
        }
        
        public MediaApplication getAssociatedMediaApplication(PlanElementBean pe) {
            for (MediaApplication ret : knownMediaApplications) {
                if (ret.getUuid().equals(pe.appUuid)) return ret;
            }
            
            return null;
        }
        
        public static Target getTargetByIp(PlanElementBean pe, String ip) {
            for (Target ret : pe.getTargets()) {
                if (ret.targetIp.equals(ip)) return ret;
            }

            return null;
        }
        
        public static Target getTargetByMediaApplication(PlanElementBean pe, MediaApplication app) {
            if (app == null) return null;
            for (EndpointNodeInterface iface : app.getParentNode().getNodeInterfaces()) {
                Target ret = getTargetByIp(pe, iface.getIpAddress());
                
                if (ret != null) return ret;
            }
            
            return null;
        }
        
        public void setReceivingPort(MediaApplication app, String port) {
            MediaApplication predecessor = predecessorMap.get(app);
            Target target = getTargetByMediaApplication( plan.getPlanElementBean(predecessor), app);
            
            PlanElementBean pe = plan.getPlanElementBean(app);

            if (predecessor == null) throw new NullPointerException("Failed to locate predecessor of application " + app);
            if (target == null) throw new NullPointerException("Failed to locate predecessor of application " + app);
            if (pe == null) throw new NullPointerException("Failed to locate corresponding plan element for application " + app);
            
            target.targetPort = port;
            plan.getPlanElementBean(app).sourcePort = port;
        }

        void printPredecessors(PrintStream out) {
            
            out.println("The predecessors of applications are:");
            for (Map.Entry<MediaApplication, MediaApplication> entry : predecessorMap.entrySet()) {
                out.println("  " + entry.getKey() + "  receiving from " + entry.getValue());
            }
        }
        
        private void printDistributionTreeRecursive(PrintStream out, MediaApplication root, String indent) {
            out.println("  " + indent + root + " - " + root.getParentNode().getNodeInterfaces().iterator().next().getIpAddress() + ":" + root.getPreferredReceivingPort());
            for (Map.Entry<MediaApplication, MediaApplication> entry : predecessorMap.entrySet()) {
                if (entry.getValue().equals(root)) {
                    printDistributionTreeRecursive(out, entry.getKey(), indent + indent);
                }
            }
        }
        
        void printDistributionTree(PrintStream out) {
            for (MediaApplication app : knownMediaApplications) {
                if (app instanceof MediaApplicationProducer) {
                    printDistributionTreeRecursive(out, app, "  ");
                }
            }
        }
    }
    
    private String appName;
    private String appUuid;
    private String sourcePort;
    private ArrayList<Target> targets = new ArrayList<>();

    public PlanElementBean() {
    }

    public PlanElementBean(MediaApplication app) {
        this.appUuid = app.getUuid();
        this.sourcePort = app.getPreferredReceivingPort();
        this.appName = app.getApplicationName();
    }
    
    public PlanElementBean(MediaApplication app, String sourcePort) {
        this.appUuid = app.getUuid();
        this.sourcePort = sourcePort;
        this.appName = app.getApplicationName();
    }

    public PlanElementBean(MediaApplication app, List<LogicalNetworkLink> links, List<String> ports, String sourcePort, String format) {
        this.appUuid = app.getUuid();
        this.sourcePort = sourcePort;
        this.appName = app.getApplicationName();
        
        if (app instanceof MediaApplicationConsumer) {
            this.targets = null;
        }
        if (app instanceof MediaApplicationProducer || app instanceof MediaApplicationDistributor) {

            if (links == null) {
                links = new ArrayList<>();
            }
            if (ports == null || ports.size() != links.size()) {
                ports = new ArrayList<>();
                for (LogicalNetworkLink link : links) {
                    ports.add("");
                }
            }

            targets = new ArrayList<>();
            
            for (int i = 0; i < links.size(); i++) {
                targets.add(new Target(links.get(i).getToInterface().getIpAddress(), links.get(i).getToInterface().getNetMask(), ports.get(i), format));
            }
        }

        if (app instanceof MediaApplicationProducer && targets.size() > 1) {
            throw new IllegalArgumentException("The producer can support at most one outgoing transmission!");
        }
    }

    public void addTarget(LogicalNetworkLink link, String port, String format) {
        addTarget(new Target(link.getToInterface().getIpAddress(), link.getToInterface().getNetMask(), format, port));
    }
    
    public void addTarget(Target target) {
        targets.add(target);
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public boolean hasTargets() {
        return targets != null;
    }

    public ArrayList<Target> getTargets() {
        return targets;
    }

    public ArrayList<String> getTargetIPs() {
        ArrayList<String> output = new ArrayList<>();
        for(Target t : targets) {
            output.add(t.getTargetIp());
        }
        return output;
    }

    public ArrayList<String> getTargetMasks() {
        ArrayList<String> output = new ArrayList<>();
        for(Target t : targets) {
            output.add(t.getTargetMask());
        }
        return output;
    }

    public ArrayList<String> getTargetPorts() {
        ArrayList<String> output = new ArrayList<>();
        for(Target t : targets) {
            output.add(t.getTargetPort());
        }
        return output;
    }

    public ArrayList<String> getFormats() {
        ArrayList<String> output = new ArrayList<>();
        for(Target t : targets) {
            output.add(t.getFormat());
        }
        return output;
    }

    public String getUuid() {
        return appUuid;
    }
    
    public void clearTargets() {
        targets.clear();
    }

    public String getAppName() {
        return appName == null ? appUuid : appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    @Override
    public String toString() {
        return ""+this.getAppName()+": receiving at "+this.sourcePort+", sending to "+this.getTargetIPs() + " formats " + this.getFormats();
    }

    public void setAppUuid(String appUuid) {
        this.appUuid = appUuid;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    public void setTargets(ArrayList<Target> targets) {
        this.targets = targets;
    }
    
    
}
