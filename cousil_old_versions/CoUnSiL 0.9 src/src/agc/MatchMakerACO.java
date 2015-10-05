package agc;

import agc.MatchMakerACO.GlobalResult;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import mediaAppFactory.MediaApplicationDistributor;
import mediaAppFactory.MediaApplicationProducer;
import mediaAppFactory.MediaCompressor;
import mediaAppFactory.MediaDecompressor;
import mediaAppFactory.MediaStreamSet;
import mediaApplications.streams.FormatTranscoding;
import mediaApplications.streams.StreamFormat;
import mediaApplications.streams.TranscodingResource;
import mediaApplications.streams.WellKnownFormats;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import networkRepresentation.LogicalNetworkLink;
import networkRepresentation.NetworkSite;
import networkRepresentation.PartiallyKnownNetworkTopology;
import networkRepresentation.PhysicalNetworkLink;
import networkRepresentation.TopologyDifference;
import org.apache.log4j.Logger;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.traverse.BreadthFirstIterator;

/**
 *
 * @author Pavel Troubil <pavel@ics.muni.cz>
 */
public class MatchMakerACO extends MatchMaker {

    public static final long PLANNING_DEADLINE = 5000;
    
    private final MatchMakerConfig matchMakerConfig;
    private final PartiallyKnownNetworkTopology networkTopology;
    private final AtomicBoolean matchFound = new AtomicBoolean(false);
    /**
     * Input graph
     */
    private final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> graph = new DirectedWeightedMultigraph<>(LogicalNetworkLink.class);
    /**
     * Input graphs for individual streams.
     */
    private final HashMap<MediaStreamSet, DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>> streamSetGraphs;

    private final ArrayList<MediaStreamSet> streamSets;
    private final HashMap<MediaStreamSet, ArrayList<EndpointNetworkNode>> streamSetProdNodes;
    private final HashMap<MediaStreamSet, HashSet<EndpointNetworkNode>> streamSetConsNodes;
    private final HashMap<MediaApplication, MediaStreamSet> producerStreamSet;
    private final ArrayList<MediaApplication> mediaProducers = new ArrayList<>();
    private final ArrayList<MediaApplication> mediaDistributors = new ArrayList<>();
    private SiteGraph siteGraph;
    private int consumersCount = 0;

    private final ArrayList<EndpointNetworkNode> appNodes;
    private final ArrayList<LogicalNetworkLink> logicalLinks;
    private final ArrayList<PhysicalNetworkLink> physicalLinks;
    private double linkPreferences[];
    private long linkLoad[];
    private long physicalLinkLoad[];
    private long physicalLinkTransfers[];
    private double linkOverload[];
    private double physicalLinkOverload[];
    private final ArrayList<double[]> physicalLinkCapacities;
    private final ArrayList<ThreadedAnt> antThreads = new ArrayList<>();
//    private ExecutorService threadPool;

    private ArrayList<StreamFormat> formats;
    private final ArrayList<StreamFormat> sortedFormats;
    private final long formatBws[];
    private final ArrayList<boolean[][]> transcodingAvailability;
    private int totalNodeLoads[][];
    private double minCapacityMbits = 0;
    private double maxLinkLatency = 0;
    private final EnumMap<ObjectiveFuncComponent, GlobalResult> globalResults = new EnumMap<>(ObjectiveFuncComponent.class);
    private double inflatedQuality = 0.0;

    private int workerThreads;
    private final int sampleSize;
    private final int antAttempts;
    private final boolean forbidNearLoops;
    private final boolean reconnectOnProducerNearLoop;
    private final boolean inflate;
    private int iterationLimit;

    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }
    private final boolean copyOptimum;
    private final PheromoneUpdater phUpdater;

    Objective currentObjective = new Objective();
    private boolean lastImproved = false;
    private double totalProbability = 1.0d;
    private final int runtimeLimitMillis = 1000;
    private boolean changed = false;
    private boolean settledAfterChange = true;
    private int disturbed = 0;
    private int bestDisturbed = 0;
    private double bestProbability = 0.0;

    private final PerStreamEliminator eliminator = new PerStreamEliminator(graph);

    private final double acceptObjectiveThreshold = 0.9;
    private final double initPrefMaxLatency = 0.5;
    private final int defaultSampleSize = 1;
    private final double overloadPush = 1.0;
    private final int defaultWorkerThreads = 1;
    private final int defaultAntAttempts = 5;
    private final boolean defaultForbidNearLoops = false;
    private final boolean defaultReconnectOnProducerNearLoop = true;
    private final int defaultIterationLimit = -1;
    private final boolean defaultCopyOptimum = true;
    private final boolean defaultInflate = false;
    private final String defaultUpdaterName = "EveryIter";

    private PortAssigner portMap;
    private ArrayList<LogicalNetworkLink> usedLinks = null;
    private PlanElementBean.Plan resultingPlan = new PlanElementBean.Plan();
    private volatile AtomicBoolean breakPlanning = new AtomicBoolean(false);

    public MatchMakerACO(PartiallyKnownNetworkTopology networkTopology, MatchMakerConfig matchMakerConfig) {
        PartiallyKnownNetworkTopology tempTopology;
        tempTopology = PartiallyKnownNetworkTopology.getCurrentSnapshot(networkTopology);
        this.networkTopology = tempTopology;
        this.matchMakerConfig = matchMakerConfig;

        if (matchMakerConfig.getProperty("workerThreads") != null) {
            workerThreads = (Integer) matchMakerConfig.getProperty("workerThreads");
        } else {
            workerThreads = defaultWorkerThreads;
        }

        if (matchMakerConfig.getProperty("antAttempts") != null) {
            antAttempts = (Integer) matchMakerConfig.getProperty("antAttempts");
        } else {
            antAttempts = defaultAntAttempts;
        }

        if (matchMakerConfig.getProperty("iterationLimit") != null) {
            iterationLimit = (Integer) matchMakerConfig.getProperty("iterationLimit");
        } else {
            iterationLimit = defaultIterationLimit;
        }

        if (matchMakerConfig.getProperty("copyOptimum") != null) {
            copyOptimum = (Boolean) matchMakerConfig.getProperty("copyOptimum");
        } else {
            copyOptimum = defaultCopyOptimum;
        }

        for (EndpointNetworkNode node : this.networkTopology.getEndpointNodes()) {
            if (!node.getNodeApplications().isEmpty()) {
                graph.addVertex(node);
            }
        }

        for (LogicalNetworkLink link : this.networkTopology.getLogicalLinks()) {
            if (eliminateLink(link)) {
                for (PhysicalNetworkLink physical : link.getPhysicalLinksOnThePath()) {
                    physical.getTraversingLogicalLinks().remove(link);
                }
                continue;
            }
            graph.addEdge(link.getFromNode(), link.getToNode(), link);
        }

        LinkedList<EndpointNetworkNode> nodesToRemove = new LinkedList<>();
        for (EndpointNetworkNode node : graph.vertexSet()) {
            if (graph.edgesOf(node).isEmpty()) {
                nodesToRemove.add(node);
            }
        }
        graph.removeAllVertices(nodesToRemove);
        setWeightsAsLatencies(graph);

        HashSet<MediaStreamSet> tempStreamSets = new HashSet<>();
        for (EndpointNetworkNode appNode : graph.vertexSet()) {
            for (MediaApplication app : appNode.getNodeApplications()) {
                if (app instanceof MediaApplicationProducer) {
                    tempStreamSets.add(app.getParentNode().getNodeSite().getStreamSet());
                }
            }
        }
        streamSets = new ArrayList<>(tempStreamSets);
        for (int z = 0; z < streamSets.size(); z++) {
            streamSets.get(z).index = z;
        }

        streamSetProdNodes = new HashMap<>(streamSets.size(), (float) 1.0);
        streamSetConsNodes = new HashMap<>(streamSets.size(), (float) 1.0);

        for (MediaStreamSet z : streamSets) {
            streamSetProdNodes.put(z, new ArrayList<EndpointNetworkNode>());
            streamSetConsNodes.put(z, new HashSet<EndpointNetworkNode>());
        }

        HashSet<StreamFormat> formatSet = new HashSet<>(20);
        for (EndpointNetworkNode appNode : graph.vertexSet()) {
            for (MediaApplication app : appNode.getNodeApplications()) {
                if (app instanceof MediaApplicationProducer) {
                    mediaProducers.add(app);
                    MediaStreamSet streamSet = appNode.getNodeSite().getStreamSet();
                    if (!streamSetProdNodes.containsKey(streamSet)) {
                        streamSetProdNodes.put(streamSet, new ArrayList<EndpointNetworkNode>());
                    }
                    streamSetProdNodes.get(streamSet).add(appNode);
                    formatSet.addAll(((MediaApplicationProducer) app).getCompressionFormats());
                }
                if (app instanceof MediaApplicationDistributor) {
                    mediaDistributors.add(app);
                    formatSet.addAll(((MediaApplicationDistributor) app).getCompressionFormats());
                }
                if (app instanceof MediaApplicationConsumer) {
                    for (NetworkSite site : ((MediaApplicationConsumer) app).getSourceSites()) {
                        MediaStreamSet streamSet = site.getStreamSet();
                        if (!streamSetConsNodes.containsKey(streamSet)) {
                            streamSetConsNodes.put(streamSet, new HashSet<EndpointNetworkNode>());
                        }
                        streamSetConsNodes.get(streamSet).add(appNode);
                        consumersCount++;
                    }
                    formatSet.addAll(((MediaApplicationConsumer) app).getDecompressionFormats());
                }
            }
        }

        if (!formatSet.isEmpty()) {
            formats = new ArrayList<>(formatSet);
        } else {
            formats = (ArrayList<StreamFormat>) matchMakerConfig.getProperty("formatsList");
        }
        if (formats == null || formats.isEmpty()) {
            final Integer formatsCount = (Integer) matchMakerConfig.getProperty("formatsCount");
            if (formatsCount != null) {
                formats = WellKnownFormats.Format.getFormatList(formatsCount);
            } else {
                formats = WellKnownFormats.Format.getFormatList();
            }
        }
        sortedFormats = new ArrayList<>(formats);
        Collections.sort(sortedFormats, new FormatsBwAscComparator());
        formatBws = new long[formats.size()];
        for (int f = 0; f < formats.size(); f++) {
            formatBws[f] = (long) (sortedFormats.get(f).bandwidthMin / 1e6);
            sortedFormats.get(f).variableIndex = f;
        }

        long minFormatBandwidth = sortedFormats.get(0).bandwidthMin;
        minCapacityMbits = minFormatBandwidth / (double) 1000000;

        producerStreamSet = new HashMap<>(2 * mediaProducers.size());

        assert streamSetProdNodes.size() > 0;
        for (int i = mediaProducers.size() - 1; i >= 0; i--) {
            producerStreamSet.put(mediaProducers.get(i), mediaProducers.get(i).getParentNode().getNodeSite().getStreamSet());
        }

        streamSetGraphs = new HashMap<>(2 * streamSets.size());
        for (MediaStreamSet z : streamSets) {
            streamSetGraphs.put(z, eliminator.getStreamSetSubgraph(z));
        }

        int index = 0;
        appNodes = new ArrayList<>(graph.vertexSet().size());
        for (EndpointNetworkNode node : graph.vertexSet()) {
            appNodes.add(index, node);
            node.index = index++;
        }

        index = 0;
        logicalLinks = new ArrayList<>(graph.edgeSet().size());
        for (LogicalNetworkLink link : graph.edgeSet()) {
            logicalLinks.add(index, link);
            link.variableIndex = index++;
        }

        index = 0;
        physicalLinks = new ArrayList<>(this.networkTopology.getPhysicalLinks().size());
        for (PhysicalNetworkLink l : this.networkTopology.getPhysicalLinks()) {
            physicalLinks.add(index, l);
            l.variableIndex = index++;
        }

        linkPreferences = new double[logicalLinks.size()];
        for (LogicalNetworkLink logicalLink : logicalLinks) {
            if (logicalLink.getLatency() > maxLinkLatency) {
                maxLinkLatency = logicalLink.getLatency();
            }
        }
        setLinkPreferences(logicalLinks);

        linkLoad = new long[logicalLinks.size()];
        linkOverload = new double[logicalLinks.size()];
        Arrays.fill(linkOverload, 1.0d);

        for (int d = 0; d < mediaDistributors.size(); d++) {
            mediaDistributors.get(d).variableIndex = d;
        }

        physicalLinkLoad = new long[physicalLinks.size()];
        physicalLinkOverload = new double[physicalLinks.size()];
        physicalLinkTransfers = new long[physicalLinks.size()];

        Integer tmpSampleSize = (Integer) matchMakerConfig.getProperty("sampleSize");
        if (tmpSampleSize == null) {
            sampleSize = defaultSampleSize;
        } else {
            sampleSize = tmpSampleSize;
        }

        if (matchMakerConfig.getProperty("forbidNearLoops") != null) {
            forbidNearLoops = matchMakerConfig.getBooleanProperty("forbidNearLoops");
        } else {
            forbidNearLoops = defaultForbidNearLoops;
        }

        if (matchMakerConfig.getProperty("reconnectOnProducerNearLoop") != null) {
            reconnectOnProducerNearLoop = matchMakerConfig.getBooleanProperty("reconnectOnProducerNearLoop");
        } else {
            reconnectOnProducerNearLoop = defaultReconnectOnProducerNearLoop;
        }

        if (matchMakerConfig.getProperty("inflate") != null) {
            inflate = matchMakerConfig.getBooleanProperty("inflate");
        } else {
            inflate = defaultInflate;
        }

        physicalLinkCapacities = new ArrayList<>(physicalLinks.size());
        for (int l = 0; l < physicalLinks.size(); l++) {
            physicalLinkCapacities.add(l, new double[]{physicalLinks.get(l).getCapacity()});
        }

        transcodingAvailability = new ArrayList<>(mediaDistributors.size());
        for (int d = 0; d < mediaDistributors.size(); d++) {
            final MediaApplicationDistributor distributor = (MediaApplicationDistributor) mediaDistributors.get(d);
            transcodingAvailability.add(d, new boolean[formats.size()][formats.size()]);
            setTranscodingAvailability(distributor, transcodingAvailability.get(d));
        }
        totalNodeLoads = new int[appNodes.size()][TranscodingResource.values().length];

        for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
            globalResults.put(c, new GlobalResult());
        }

        siteGraph = new SiteGraph(appNodes, logicalLinks, true);

        String updaterName = (String) matchMakerConfig.getProperty("pheromoneUpdater");
        if (updaterName == null) {
            updaterName = defaultUpdaterName;
        }
        switch (updaterName) {
            case "NCA2014":
                phUpdater = new NCA2014Updater();
                break;
            case "EveryIter":
                phUpdater = new EveryIterUpdater();
                break;
            default:
                phUpdater = new EveryIterUpdater();
                break;
        }
        workerThreads = Math.min(workerThreads, Runtime.getRuntime().availableProcessors() - 1);
//        threadPool = Executors.newFixedThreadPool(workerThreads);
        for (int p = 0; p < mediaProducers.size(); p++) {
            antThreads.add(p, new ThreadedAnt(mediaProducers.get(p)));
        }
    }

    public final boolean eliminateLink(LogicalNetworkLink link) {
        boolean eliminate = false;
        if (link.getCapacity() < minCapacityMbits) {
            eliminate = true;
        } else if (link.getFromNode().getNodeSite().equals(link.getToNode().getNodeSite())
            && !link.getFromNode().hasDistributor() && !link.getToNode().hasDistributor()) {
            eliminate = true;
        } else {
            boolean hasSender = false,
                hasReceiver = false;
            for (MediaApplication app : link.getFromNode().getNodeApplications()) {
                if (app instanceof MediaApplicationProducer
                    || app instanceof MediaApplicationDistributor) {
                    hasSender = true;
                }
            }
            for (MediaApplication app : link.getToNode().getNodeApplications()) {
                if (app instanceof MediaApplicationConsumer
                    || app instanceof MediaApplicationDistributor) {
                    hasReceiver = true;
                }
            }
            if (!(hasSender && hasReceiver)) {
                eliminate = true;
            }
        }
        return eliminate;
    }

    private void setTranscodingAvailability(MediaApplicationDistributor mediaDistributor, boolean[][] availability) {
        for (FormatTranscoding tc : mediaDistributor.getTranscodings()) {
            final int inIndex = sortedFormats.indexOf(tc.formatIn);
            final int outIndex = sortedFormats.indexOf(tc.formatOut);
            if (inIndex == -1 || outIndex == -1) {
                continue;
            }
            availability[inIndex][outIndex] = true;
        }
    }

    private void setLinkPreferences(Collection<LogicalNetworkLink> links) {
        for (LogicalNetworkLink link : links) {
            linkPreferences[link.variableIndex] = initPrefMaxLatency + (maxLinkLatency - link.getLatency()) * 0.5;
        }
    }

    @Override
    public PlanElementBean.Plan getPlan() {

        if (true) return resultingPlan;
        
        PlanElementBean.ExtendedPlan extendedPlan = new PlanElementBean.ExtendedPlan(resultingPlan, networkTopology);
        PlanElementBean.Plan ret = new PlanElementBean.Plan();
        
        System.out.println("Plan is:");
        resultingPlan.printPlan(System.out);
        System.out.println("Distribution tree:");
        extendedPlan.printDistributionTree(System.out);
        System.out.println("Predecessor set:");
        extendedPlan.printPredecessors(System.out);
        
        // Create new plan elements
        for (Map.Entry<String, PlanElementBean> entry : resultingPlan.entrySet()) {
            MediaApplication app = extendedPlan.getAssociatedMediaApplication(entry.getValue());
            PlanElementBean newBean = new PlanElementBean(app);
            ret.put(app.getUuid(), newBean);
        }
        
        System.out.println("New plan (without targets):");
        ret.printPlan(System.out);
        
        // Create targets to predecessors
        for (Map.Entry<String, PlanElementBean> entry : resultingPlan.entrySet()) {
            MediaApplication app = extendedPlan.getAssociatedMediaApplication(entry.getValue());
            MediaApplication predecessor = extendedPlan.predecessorMap.get(app);
            
            System.out.println("Application = " + app);
            System.out.println("Predecessor = " + predecessor);
            PlanElementBean newEl = ret.getPlanElementBean(predecessor);
            PlanElementBean oldEl = resultingPlan.getPlanElementBean(predecessor);
            
            PlanElementBean.Target oldTarget = PlanElementBean.ExtendedPlan.getTargetByMediaApplication(oldEl, app);
            PlanElementBean.Target newTarget = new PlanElementBean.Target(oldTarget.getTargetIp(), "", oldTarget.getFormat(), app.getPreferredReceivingPort(), app.getUuid());
            newEl.addTarget(newTarget);
        }   
        
        System.out.println("New plan ((final):");
        ret.printPlan(System.out);
        
        return ret;
    }

    @Override
    public boolean doMatch() {
        boolean timedOut = false;
        breakPlanning.set(false);
        
        new Thread() {
            public void run() {
                long deadline = System.currentTimeMillis() + PLANNING_DEADLINE;
                
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                    }
                    if (System.currentTimeMillis() > deadline) {
                        breakPlanning.set(true);
                        return;
                    }
                }
            }
        }.start();
        
        Executor exec = Executors.newSingleThreadExecutor();
        FutureTask task = new FutureTask(new Runnable() {
            @Override
            public void run() {
                try {
                    doMatchInternal();
                } catch (InterruptedException ex) {
                    matchFound.set(false);
                }
            }
        }, null);
        exec.execute(task);
        try {
            if (this.matchMakerConfig.timeout == 0) {
                task.get();
            } else {
                task.get(this.matchMakerConfig.timeout, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(this.getClass()).error(ex.getMessage());
            this.matchFound.set(false);
        } catch (TimeoutException ex) {
            timedOut = true;
        } finally {
            task.cancel(true);
        }

        if (timedOut) {
            this.matchFound.set(false);
        }

        return this.matchFound.get();
    }

    private void runACO() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting planning at " + startTime);

        int iterations = 0;
        boolean stop = false;

        while (!stop) {
            
            for (int z = 0; z < streamSets.size(); z++) {
                antThreads.get(z).setIteration(iterations);
            }
            /*
             try {
             List<Future<Boolean>> antResults = threadPool.invokeAll(antThreads);
             for (Future<Boolean> result : antResults) {
             if (!result.get()) {
             return;
             }
             }
             } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
             Logger.getLogger(this.getClass()).error(ex.getMessage());
             ex.printStackTrace();
             return;
             }
             */

            for (ThreadedAnt ant : antThreads) {
                ant.sendAnt();
            }

            
            totalProbability = 1.0;
            Arrays.fill(linkLoad, 0);
            Arrays.fill(physicalLinkLoad, 0);
            Arrays.fill(physicalLinkTransfers, 0);
            for (int n = 0; n < appNodes.size(); n++) {
                Arrays.fill(totalNodeLoads[n], 0);
            }

            
            currentObjective.reset();
            disturbed = 0;
            for (ThreadedAnt streamAnt : antThreads) {
                if (streamAnt.outGraph == null) {
                    continue;
                }
                // sum objectives, not considering uncertainty yet
                for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                    currentObjective.add(c, streamAnt.getObjective(c));
                }

            
                for (LogicalNetworkLink link : streamAnt.getOutGraph().edgeSet()) {
                    // calculate load for each link by all streams
                    final long bw = formatBws[streamAnt.getSelectedFormats()[link.variableIndex]];
                    linkLoad[link.variableIndex] += bw;
                    for (PhysicalNetworkLink physical : link.getPhysicalLinksOnThePath()) {
                        physicalLinkTransfers[physical.variableIndex]++;
                        physicalLinkLoad[physical.variableIndex] += bw;
                    }
                }

                
                for (int n = 0; n < appNodes.size(); n++) {
                    for (TranscodingResource r : TranscodingResource.values()) {
                        totalNodeLoads[n][r.ordinal()] += streamAnt.getNodeLoads()[n][r.ordinal()];
                    }
                }
            }
            currentObjective.calculateSum();

            
            for (int l = 0; l < physicalLinks.size(); l++) {
                if (physicalLinkLoad[l] > 0) {
                    // calculate probability that planned traffic fits in the link
                    int probIndex = Arrays.binarySearch(physicalLinkCapacities.get(l), physicalLinkLoad[l]);
                    if (probIndex < 0) {
                        probIndex = -1 - probIndex;
                    }
                    probIndex = Math.min(probIndex, sampleSize);
                    final double linkProbability = (double) (sampleSize - probIndex) / sampleSize;
                    totalProbability *= linkProbability;

                    // physical link overload
                    physicalLinkOverload[l] = (double) 1 / physicalLinkTransfers[l];
                    physicalLinkOverload[l] = Math.min((double) physicalLinkCapacities.get(l)[0/*HACK*/] / physicalLinkLoad[l], physicalLinkOverload[l]);
                } else {
                    physicalLinkOverload[l] = 1.0d;
                }
            }

            // update logical link loads and selection probability
            for (int l = 0; l < logicalLinks.size(); l++) {
                linkOverload[l] = 1.0d;
                for (PhysicalNetworkLink physical : logicalLinks.get(l).getPhysicalLinksOnThePath()) {
                    if (physicalLinkOverload[physical.variableIndex] < linkOverload[l]) {
                        linkOverload[l] = physicalLinkOverload[physical.variableIndex];
                    }
                }
                linkOverload[l] *= overloadPush;
            }

            // calculate objective
            totalProbability = Math.max(totalProbability, 0.0001);
            if (changed) {
            }
            currentObjective.includeProbability(totalProbability);
            if (changed) {
                for (ThreadedAnt ant : antThreads) {
                    disturbed += ant.getDisturbedTransmissions();
                }
            }
            currentObjective.includeDisturbances(disturbed, consumersCount);
            lastImproved = false;
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                if (currentObjective.get(c) < globalResults.get(c).objective.get(c)) {
                    if (totalProbability >= acceptObjectiveThreshold) {
                        settledAfterChange = true;
                    }
                    if (c == ObjectiveFuncComponent.SUM) {
                        lastImproved = true;
                        bestDisturbed = disturbed;
                        bestProbability = totalProbability;
                    } else if (totalProbability < acceptObjectiveThreshold) {
                        break;
                    }
                    globalResults.get(c).update(antThreads, copyOptimum, totalProbability);
                    for (ThreadedAnt ant : antThreads) {
                        ant.setGlobalBest(c);
                    }

                    if (!copyOptimum) {
                        break;
                    }
                }
            }

            iterations++;
            if (iterationLimit <= 0) {
                if (iterations % 50 == 0) {
                    long runtime = System.currentTimeMillis() - startTime;
                    if (runtime > runtimeLimitMillis) {
                        stop = true;
                    }
                }
            } else {
                if (iterations >= iterationLimit) {
                    stop = true;
                }
            }
        }
        
        
        if (inflate && false) {
            inflateQualities(antThreads);
        }

        this.matchFound.set(true);

        usedLinks = new ArrayList<>();
        portMap = new PortAssigner();
        portMap.cleanup(mediaProducers);

        HashMap<MediaApplicationDistributor, LinkedList<MediaApplicationDistributor>> distributorNodeToDistributorMap = new HashMap<>();
        
        for (EndpointNetworkNode node : networkTopology.getEndpointNodes()) {
            for (MediaApplication app : node.getNodeApplications()) {
                if (app instanceof MediaApplicationDistributor) {
                    distributorNodeToDistributorMap.put((MediaApplicationDistributor) app, new LinkedList<MediaApplicationDistributor>());
                    for (MediaApplication that : node.getNodeApplications()) {
                        if (app instanceof MediaApplicationDistributor) {
                            distributorNodeToDistributorMap.get((MediaApplicationDistributor) app).add((MediaApplicationDistributor) that);
                        }
                    }
                }
            }
        }
        
        // Construct plan from this
        for (ThreadedAnt ant : antThreads) {
            final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> graph = ant.globallyBestResults.get(ObjectiveFuncComponent.SUM).outGraph;
            if (graph == null) {
                continue;
            }

            final Set<LogicalNetworkLink> edgeSet = graph.edgeSet();
            HashMap<MediaApplication, LinkedList<LogicalNetworkLink>> targetMap = new HashMap<>();
            for (LogicalNetworkLink link : edgeSet) {
                usedLinks.add(link);
                MediaApplication pred;
                if (link.getFromNode().getNodeApplications().contains(ant.producer)) {
                    pred = ant.producer;
                } else {
                    pred = link.getFromNode().getNodeApplications().iterator().next();
                }
                if (targetMap.get(pred) == null) {
                    targetMap.put(pred, new LinkedList<LogicalNetworkLink>());
                }
                targetMap.get(pred).add(link);

                // Consumers need plan as well
                if (streamSetConsNodes.get(ant.streamSet).contains(link.getToNode())) {
                    for (MediaApplication app : link.getToNode().getNodeApplications()) {
                        if (app instanceof MediaApplicationConsumer) {
                            if (((MediaApplicationConsumer) app).getSourceSites().contains(ant.producer.getParentNode().getNodeSite())) {
                                targetMap.put(app, new LinkedList<LogicalNetworkLink>());
                            }
                        }
                    }
                }
            }

            System.out.println("Creating plan for ant " + ant + ":");
            for (MediaApplication app : targetMap.keySet()) {
                
                MediaApplication substitute = app;
                if (app instanceof MediaApplicationDistributor) {
                    
                    substitute = distributorNodeToDistributorMap.get((MediaApplicationDistributor) app).pop();
                }

                String streamPort = portMap.assignPort((MediaApplicationProducer) ant.producer);

                PlanElementBean el = new PlanElementBean(substitute, streamPort);
                for (LogicalNetworkLink l : targetMap.get(app)) {
                    el.addTarget(l, streamPort, sortedFormats.get(ant.globallyBestResults.get(ObjectiveFuncComponent.SUM).selectedFormats[l.variableIndex]).name);
                }

                StringBuilder sb = new StringBuilder("    " + substitute.getParentNode().getNodeName() + " (" + substitute.getShortDescription() + ") -" + streamPort + "-> [");
                for (LogicalNetworkLink l : targetMap.get(app)) {
                    sb.append(l.toString()).append(", ");
                }
                sb.append("]");
                System.out.println(sb);

                resultingPlan.put(substitute.getUuid(), el);
            }

        }
        shutdown();
    }

    int inflate() {
        return inflateQualities(antThreads);
    }

    public double getInflatedQuality() {
        return inflatedQuality;
    }

    void shutdown() {
//        threadPool.shutdownNow();
    }

    int getConsumersCount() {
        return consumersCount;
    }

    public int getBestDisturbed() {
        return bestDisturbed;
    }

    public double getBestProbability() {
        return bestProbability;
    }

    private void doMatchInternal() throws InterruptedException {
        runACO();
    }

    void signalChange() {
        changed = true;
        settledAfterChange = false;
        for (int p = 0; p < mediaProducers.size(); p++) {
            antThreads.get(p).setOriginalresult();
        }
        for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
            globalResults.get(c).objective.setInitial();
        }
    }

    public void change(TopologyDifference difference) {
        if (difference == null) {
            return;
        }

        changed = true;
        settledAfterChange = false;
        // remove all links from global graph
        if (difference.getRemovedLogicalLinks() != null && !difference.getRemovedLogicalLinks().isEmpty()) {
            for (ThreadedAnt ant : antThreads) {
                if (ant.g.removeAllEdges(difference.getRemovedLogicalLinks())) {
                    ant.requireGraphUpdate();
                    if (ant.g.outgoingEdgesOf(ant.startNode).isEmpty()) {
                        ant.consumersNumber = 0;
                        streamSetConsNodes.get(ant.streamSet).clear();
                    }
                }
            }
        }

        final int oldLogicalLinks = logicalLinks.size();
        final int prevProducersCount = mediaProducers.size();

        for (EndpointNetworkNode node : difference.getNewAppNodes()) {
            final int nodeIndex = appNodes.size();
            appNodes.add(nodeIndex, node);
            node.index = nodeIndex;

            graph.addVertex(node);

            for (MediaApplication app : node.getNodeApplications()) {
                if (app instanceof MediaApplicationProducer) {
                    final MediaStreamSet z = node.getNodeSite().getStreamSet();
                    if (streamSets.contains(z)) {
                        // We already have producer for this stream set. Ignore.
                        continue;
                    }
                    streamSets.add(z);
                    z.index = streamSets.indexOf(z);
                    streamSetProdNodes.put(z, new ArrayList<EndpointNetworkNode>());
                    streamSetProdNodes.get(z).add(node);
                    producerStreamSet.put(app, z);
                    if (!streamSetConsNodes.containsKey(z)) {
                        streamSetConsNodes.put(z, new HashSet<EndpointNetworkNode>());
                    }
                    final int prodIndex = mediaProducers.size();
                    mediaProducers.add(prodIndex, app);
                }
            }
        }

        for (EndpointNetworkNode node : difference.getNewAppNodes()) {
            for (MediaApplication app : node.getNodeApplications()) {
                if (app instanceof MediaApplicationDistributor) {
                    final int index = mediaDistributors.size();
                    mediaDistributors.add(index, app);
                    transcodingAvailability.add(index, new boolean[formats.size()][formats.size()]);
                    setTranscodingAvailability((MediaApplicationDistributor) app, transcodingAvailability.get(index));
                    app.variableIndex = index;
                    for (ThreadedAnt ant : antThreads) {
                        if (ant.g.addVertex(node)) {
                            ant.requireGraphUpdate();
                        }
                    }
                } else if (app instanceof MediaApplicationConsumer) {
                    for (NetworkSite site : ((MediaApplicationConsumer) app).getSourceSites()) {
                        final MediaStreamSet z = site.getStreamSet();
                        if (!streamSetConsNodes.containsKey(z)) {
                            streamSetConsNodes.put(z, new HashSet<EndpointNetworkNode>());
                        }
                        streamSetConsNodes.get(z).add(node);
                        for (ThreadedAnt ant : antThreads) {
                            if (z.equals(ant.streamSet) && ant.g.addVertex(node)) {
                                ant.requireGraphUpdate();
                            }
                        }
                        consumersCount++;
                    }
                }
            }
        }
        int index = this.logicalLinks.size();
        boolean maxLatencyChanged = false;
        for (LogicalNetworkLink link : difference.getNewLogicalLinks()) {
            this.logicalLinks.add(index, link);
            link.variableIndex = index++;
            graph.addEdge(link.getFromNode(), link.getToNode(), link);
            if (link.getLatency() > maxLinkLatency) {
                maxLinkLatency = link.getLatency();
                maxLatencyChanged = true;
            }
        }
        for (ThreadedAnt ant : antThreads) {
            for (LogicalNetworkLink link : difference.getNewLogicalLinks()) {
                if (ant.g.containsVertex(link.getFromNode())
                    && ant.g.containsVertex(link.getToNode())) {
                    ant.g.addEdge(link.getFromNode(), link.getToNode(), link);
                    ant.requireGraphUpdate();
                }
            }
            ant.updateGraph();
        }

        index = this.physicalLinks.size();
        physicalLinkCapacities.ensureCapacity(physicalLinks.size());
        for (PhysicalNetworkLink link : difference.getNewPhysicalLinks()) {
            this.physicalLinks.add(index, link);
            physicalLinkCapacities.add(index, new double[]{link.getCapacity()});
            link.variableIndex = index++;
        }

        // Resize arrays used for temporary values
        if (!difference.getNewLogicalLinks().isEmpty()) {
            linkLoad = new long[logicalLinks.size()];
            linkOverload = Arrays.copyOf(linkOverload, logicalLinks.size());
//            linkOverload = new double[logicalLinks.size()];
            Arrays.fill(linkOverload, oldLogicalLinks, linkOverload.length - 1, 1.0);
            linkPreferences = java.util.Arrays.copyOf(linkPreferences, logicalLinks.size());
            setLinkPreferences(maxLatencyChanged ? this.logicalLinks : difference.getNewLogicalLinks());
        }
        if (!difference.getNewPhysicalLinks().isEmpty()) {
            physicalLinkLoad = new long[physicalLinks.size()];
            physicalLinkOverload = new double[physicalLinks.size()];
            physicalLinkTransfers = new long[physicalLinks.size()];
        }
        if (!difference.getNewAppNodes().isEmpty()) {
            totalNodeLoads = new int[appNodes.size()][TranscodingResource.values().length];
        }

        // Create new ant threads
        for (int p = 0; p < mediaProducers.size(); p++) {
            if (p >= prevProducersCount) {
                antThreads.add(p, new ThreadedAnt(mediaProducers.get(p)));
            } else {
                antThreads.get(p).resize(difference);
                antThreads.get(p).setOriginalresult();
            }
        }
        siteGraph = new SiteGraph(appNodes, logicalLinks, true);
        for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
            globalResults.get(c).objective.setInitial();
        }
    }

    void reset() {
        changed = false;
        settledAfterChange = true;
        Arrays.fill(linkOverload, 1.0);
        for (int p = 0; p < mediaProducers.size(); p++) {
            antThreads.get(p).reset();
        }
        for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
            globalResults.get(c).objective.setInitial();
        }
    }

    HashMap<EndpointNetworkNode, ArrayList<EndpointNetworkNode>> disturbedTransmissions(GlobalResult before, GlobalResult after) {
        HashMap<EndpointNetworkNode, ArrayList<EndpointNetworkNode>> disturbations = new HashMap<>(antThreads.size() * 2);

        for (ThreadedAnt ant : before.graphs.keySet()) {
            final DirectedWeightedMultigraph graphAfter = after.graphs.get(ant);
            if (graphAfter == null) {
                continue;
            }

            final EndpointNetworkNode prodNode = ant.producer.getParentNode();
            BellmanFordShortestPath<EndpointNetworkNode, LogicalNetworkLink> bfBefore = new BellmanFordShortestPath<>(before.graphs.get(ant), prodNode);
            BellmanFordShortestPath<EndpointNetworkNode, LogicalNetworkLink> bfAfter = new BellmanFordShortestPath<>(after.graphs.get(ant), prodNode);
            for (EndpointNetworkNode consNode : streamSetConsNodes.get(ant.streamSet)) {
                if (!before.graphs.get(ant).containsVertex(consNode)
                    || !after.graphs.get(ant).containsVertex(consNode)) {
                    continue;
                }
                List<LogicalNetworkLink> pathBefore = bfBefore.getPathEdgeList(consNode);
                List<LogicalNetworkLink> pathAfter = bfAfter.getPathEdgeList(consNode);
                if (pathBefore == null || pathAfter == null) {
                    continue;
                }
                if (!pathBefore.equals(pathAfter)) {
                    if (!disturbations.containsKey(prodNode)) {
                        disturbations.put(prodNode, new ArrayList<EndpointNetworkNode>());
                    }
                    disturbations.get(prodNode).add(consNode);
                }
            }
        }

        return disturbations;
    }

    private void outputResults(String method) {
        System.out.println();
        System.out.println("Method: " + method + ":");
        for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
            if (c != ObjectiveFuncComponent.SUM) {
                continue;
            }
            System.out.println("Objective component " + c);
            System.out.println("==================================");
            for (ThreadedAnt ant : antThreads) {
                System.out.println("Producer node: " + ant.startNode);
                System.out.println(ant.globallyBestResults.get(c));
                System.out.println();
            }
        }
        System.out.println("SUM best:" + globalResults.get(ObjectiveFuncComponent.SUM).objective);
    }

    int[] packRecvQualities() {
        int[] counts = new int[sortedFormats.size()];
        for (ThreadedAnt streamAnt : antThreads) {
            for (int f = 0; f < formats.size(); f++) {
                counts[f] += streamAnt.countReceivingQualities(streamAnt.globallyBestResults.get(ObjectiveFuncComponent.SUM))[f];
            }
        }
        return counts;
    }

    private void setWeightsAsLatencies(Collection<LogicalNetworkLink> links) {
        for (LogicalNetworkLink l : links) {
            l.setWeight(l.getLatency());
        }
    }

    private void setWeightsAsLatencies(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g) {
        setWeightsAsLatencies(g.edgeSet());
    }

    void modifyCapacity(PhysicalNetworkLink link, double ratio) {
        int index = link.variableIndex;
        for (int i = 0; i < physicalLinkCapacities.get(index).length; i++) {
            physicalLinkCapacities.get(index)[i] *= ratio;
        }
        for (LogicalNetworkLink logical : link.getTraversingLogicalLinks()) {
            logical.setCapacity(logical.getCapacity() * ratio);
        }
    }

    ArrayList<EndpointNetworkNode> getDestinations(PhysicalNetworkLink link) {
        ArrayList<EndpointNetworkNode> destinations = new ArrayList<>();
        for (ThreadedAnt ant : antThreads) {
            destinations.addAll(ant.getDestinations(ant.globallyBestResults.get(ObjectiveFuncComponent.SUM), link));
        }
        return destinations;
    }

    private int inflateQualities(ArrayList<ThreadedAnt> ants) {
        int inflatedConsumers = 0;
        for (ThreadedAnt ant : ants) {
            inflatedConsumers += ant.inflateStreamQuality(ant.globallyBestResults.get(ObjectiveFuncComponent.SUM));
        }
        return inflatedConsumers;
    }

    double getPhysicalLinkLoad(PhysicalNetworkLink link) {
        double load = 0.0;

        for (ThreadedAnt ant : antThreads) {
            for (LogicalNetworkLink logical : link.getTraversingLogicalLinks()) {
                int format = ant.globallyBestResults.get(ObjectiveFuncComponent.SUM).selectedFormats[logical.variableIndex];
                if (format != -1 && ant.globallyBestResults.get(ObjectiveFuncComponent.SUM).outGraph.containsEdge(logical)) {
                    load += sortedFormats.get(format).bandwidthMax;
                }
            }
        }

        return load;
    }

    EnumMap<ObjectiveFuncComponent, GlobalResult> getGlobalResults() {
        return globalResults;
    }

    @Override
    public List<LogicalNetworkLink> getUsedLinks() {
        return usedLinks;
    }

    final class Objective implements Cloneable {

        private final EnumMap<ObjectiveFuncComponent, Double> map;

        private Objective() {
            this(false);
        }

        private Objective(Boolean initial) {
            this.map = new EnumMap<>(ObjectiveFuncComponent.class);
            if (initial) {
                this.setInitial();
            } else {
                this.reset();
            }
        }

        private Objective(Objective objective) {
            this.map = objective.map.clone();
        }

        public void reset() {
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                map.put(c, 0.0);
            }
        }

        public void setInitial() {
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                this.set(c, Double.MAX_VALUE);
            }
        }

        public void set(ObjectiveFuncComponent c, Double v) {
            map.put(c, v);
        }

        public void add(ObjectiveFuncComponent c, Double v) {
            map.put(c, map.get(c) + v);
        }

        public Double get(ObjectiveFuncComponent c) {
            return map.get(c);
        }

        public void calculateSum() {
            double total = 0.0;
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                if (c != ObjectiveFuncComponent.SUM) {
                    total += map.get(c);
                }
            }
            map.put(ObjectiveFuncComponent.SUM, total);
        }

        public void includeProbability(double p) {
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                map.put(c, map.get(c) / Math.pow(p, 0.2));
            }
        }

        public void includeDisturbances(int disturbed, int total) {
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                map.put(c, map.get(c) * (1 + (double) disturbed / total));
            }
        }

        public StringBuilder appendToSB(StringBuilder sb) {
            sb.append("Components:\n");
            for (ObjectiveFuncComponent c : map.keySet()) {
                sb.append('\t').append(c).append(" ").append(map.get(c)).append('\n');
            }
            return sb;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            this.appendToSB(sb);
            return sb.toString();
        }
    }

    final class GlobalResult {

        @Override
        protected Object clone() throws CloneNotSupportedException {
            GlobalResult clone = (GlobalResult) super.clone();
            clone.objective = new Objective(this.objective);
            return clone;
        }
        private Objective objective;
        private HashMap<ThreadedAnt, DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>> graphs = new HashMap<>(antThreads.size() * 2);
        private HashMap<ThreadedAnt, int[]> selectedFormats = new HashMap<>(antThreads.size() * 2);

        public GlobalResult() {
            // Construct objective as initial with maximum values
            objective = new Objective(true);
        }

        public GlobalResult(GlobalResult src) {
            this.objective = new Objective(src.objective);
            this.graphs = new HashMap<>(src.graphs.size() * 2);
            for (ThreadedAnt ant : src.graphs.keySet()) {
                this.graphs.put(ant, (DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>) src.graphs.get(ant).clone());
            }
            this.selectedFormats = new HashMap<>(src.graphs.size() * 2);
            for (ThreadedAnt ant : src.selectedFormats.keySet()) {
                this.selectedFormats.put(ant, Arrays.copyOf(src.selectedFormats.get(ant), src.selectedFormats.get(ant).length));
            }
        }

        public void update(Collection<ThreadedAnt> ants, Boolean copy, double probability) {
            objective.reset();
            for (ThreadedAnt ant : ants) {
                if (ant.outGraph != null) {
                    graphs.put(ant, (DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>) (copy ? ant.outGraph.clone() : ant.outGraph));
                }
                selectedFormats.put(ant, copy ? ant.selectedFormats.clone() : ant.selectedFormats);
                for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                    objective.add(c, ant.result.objective.get(c));
                }
            }
            objective.calculateSum();
            objective.includeProbability(probability);
        }

        public Objective getObjective() {
            return objective;
        }
    }

    GlobalResult getGlobalResultCopy(GlobalResult gr) {
        return new GlobalResult(gr);
    }

    private class ThreadedAnt implements Callable<Boolean> {

        private final MediaApplication producer;
        private final MediaStreamSet streamSet;
        private final int zIndex;
        private final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g;
        private final EndpointNetworkNode startNode;
        private int consumersNumber;
        private static final double MINIMUM_PHEROMONE = 0.1;

        private int iteration;
        private final Random rn = new Random();
        private boolean needsGraphUpdate = false;

        private CandidateEdgeSet candidateEdges;
        private CandidateEdgeSet backupCandidateEdges;
        private DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> outGraph;
        private double pheromone[] = new double[logicalLinks.size()];
        private double selectProbability[] = new double[logicalLinks.size()];
        private int selectedFormats[] = new int[logicalLinks.size()];
        private int incomingFormat[] = new int[appNodes.size()];
        private boolean keptOriginalPath[] = new boolean[appNodes.size()];
        private boolean outgoingFormats[][] = new boolean[appNodes.size()][formats.size()]; // set to false automatically
        private int[][] nodeLoads = new int[appNodes.size()][TranscodingResource.values().length];
        private final int[] receivingQualities = new int[formats.size()];
        private final Objective objective = new Objective();
        private Result result = new Result(outGraph, selectedFormats, objective, 0, keptOriginalPath);
        private final EnumMap<ObjectiveFuncComponent, Result> globallyBestResults = new EnumMap<>(ObjectiveFuncComponent.class);
        private Result originalResult = null;
        private BellmanFordShortestPath<EndpointNetworkNode, LogicalNetworkLink> originalPaths = null;
        private int disturbedTransmissions = 0;

        public ThreadedAnt(MediaApplication producer) {
            this.candidateEdges = new CandidateEdgeSet();
            this.backupCandidateEdges = new CandidateEdgeSet();
            this.producer = producer;
            this.streamSet = producerStreamSet.get(this.producer);
            if (streamSetGraphs.containsKey(streamSet)) {
                this.g = streamSetGraphs.get(streamSet);
            } else {
                this.g = eliminator.getStreamSetSubgraph(streamSet, graph, false);
            }
            this.zIndex = streamSet.index;
            this.startNode = this.producer.getParentNode();
            this.updateConsumersNumber();
            Arrays.fill(this.pheromone, 1.0d);
            Arrays.fill(this.selectedFormats, -1);
            for (ObjectiveFuncComponent c : ObjectiveFuncComponent.values()) {
                globallyBestResults.put(c, new Result());
            }
        }

        public void setIteration(int iteration) {
            this.iteration = iteration;
        }

        @Override
        public Boolean call() throws Exception {
            int attempts = 0;
            while (attempts < antAttempts && !this.sendAnt()) {
                attempts++;
            }
            return (attempts < antAttempts);
        }

        private boolean sendAnt() throws InterruptedException {
            final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> out = new DirectedWeightedMultigraph<>(LogicalNetworkLink.class);

            if (consumersNumber == 0) {
                outGraph = out;
                return true;
            }

            EndpointNetworkNode currentNode = startNode;
            int seenConsumers = 0;
            if (iteration > 0) {
                phUpdater.update(currentObjective, totalProbability, g.edgeSet(), selectedFormats, pheromone, keptOriginalPath, lastImproved);
            }

            disturbedTransmissions = 0;
            keptOriginalPath[startNode.index] = true;
            outGraph = null;
            objective.reset();
            Arrays.fill(receivingQualities, 0);
            out.addVertex(currentNode);
            candidateEdges.clear();
            backupCandidateEdges.clear();
            boolean localForbidNearLoops = forbidNearLoops;
            for (LogicalNetworkLink link : g.outgoingEdgesOf(startNode)) {
                if (!settledAfterChange && originalResult != null
                    && (!originalResult.outGraph.containsEdge(link)) && (originalResult.outGraph.edgeSet().size() > 1 || consumersNumber == 1)) {
                    continue;
                }
                // HACK!!
                if (!(streamSetConsNodes.get(streamSet).contains(link.getToNode()) ^ consumersNumber == 1)) {
                    selectProbability[link.variableIndex] = linkPreferences[link.variableIndex] * pheromone[link.variableIndex];
                    candidateEdges.add(link, selectProbability[link.variableIndex]);
                }
            }
            if (candidateEdges.isEmpty()) {
                return (seenConsumers >= consumersNumber);
            }
            do {
                while (true) {
                    if (breakPlanning.get()) throw new InterruptedException("Could not finish planning");
                    
                    if (candidateEdges.isEmpty() && localForbidNearLoops) {
                        CandidateEdgeSet temp = candidateEdges;
                        candidateEdges = backupCandidateEdges;
                        backupCandidateEdges = temp;
                        localForbidNearLoops = false;
                    }
                    
                    final double random = rn.nextDouble() * candidateEdges.getTotalWeight();
                    LogicalNetworkLink newEdge;
                    newEdge = logicalLinks.get(candidateEdges.getRandomLinkIndex(random));
                    final EndpointNetworkNode newNode = newEdge.getToNode();
                    final long maxCapacity = (long) (newEdge.getCapacity() * linkOverload[newEdge.variableIndex]);
                    
                    if (localForbidNearLoops) {
                        EndpointNetworkNode checkNode = newEdge.getFromNode();
                        double totalDist = newEdge.getLatency();
                        boolean refuseLoop = false;
                        LogicalNetworkLink inLink = null;
                        while (!refuseLoop) {
                            final Set<LogicalNetworkLink> inLinkSet = out.incomingEdgesOf(checkNode);
                            final Iterator<LogicalNetworkLink> it = inLinkSet.iterator();
                            
                            if (!it.hasNext()) {
                                
                                break; // This also stops at startNode
                            }
                            inLink = it.next();
                            System.out.println("  inLink = " + inLink);
                            checkNode = inLink.getFromNode();
                            totalDist += inLink.getLatency();
                            double minDist = siteGraph.getShortestDistance(checkNode.index, newNode.index);
                            if (totalDist / minDist > 2) {
                                refuseLoop = true;
                            }
                        }
                        if (refuseLoop) {
                            if (checkNode != startNode) {
                                candidateEdges.remove(newEdge);
                                backupCandidateEdges.add(newEdge, selectProbability[newEdge.variableIndex]);
                                continue;
                            } else if (reconnectOnProducerNearLoop && g.containsEdge(startNode, newNode) && newNode.hasDistributor() && inLink != null) {
                                // remove current producer outlink
                                out.removeEdge(inLink);
                                final int format = selectedFormats[inLink.variableIndex];
                                selectedFormats[inLink.variableIndex] = -1;
                                // add link from producer to newNode and the node
                                final LogicalNetworkLink fromProducerLink = g.getEdge(startNode, newNode);
                                out.addEdge(startNode, newNode, fromProducerLink);
                                selectedFormats[fromProducerLink.variableIndex] = format;
                                incomingFormat[newNode.index] = format;
                                // add link from newNode to original target of producer
                                final LogicalNetworkLink reverseNewLink = g.getEdge(newNode, inLink.getToNode());
                                out.addEdge(newNode, inLink.getToNode(), reverseNewLink);
                                selectedFormats[reverseNewLink.variableIndex] = format;
                                // update candidate edge set
                                for (LogicalNetworkLink link : g.incomingEdgesOf(newNode)) {
                                    candidateEdges.remove(link);
                                }
                                currentNode = newNode;
                                if (changed && originalResult != null) {
                                    disturbedTransmissions = fixKeptPaths(out);
                                }
                                
                                break;
                            }
                        }
                    }
                    for (int i = 0; i < formatBws.length; i++) {
                    }
                    int formatIndex = Arrays.binarySearch(formatBws, maxCapacity);
                    if (formatIndex < 0) {
                        formatIndex = -formatIndex - 1;
                    }
                    if (formatIndex == 0) {
                        if (formatBws[0] > maxCapacity) {
                            continue;
                        }
                    } else {
                        formatIndex = Math.min(formatIndex, formats.size() - 1);
                        formatIndex -= (formatBws[formatIndex] > maxCapacity ? 1 : 0);
                    }
                    MediaCompressor sendingApp = null;
                    MediaDecompressor receivingApp = null;
                    for (MediaApplication a : newEdge.getFromNode().getNodeApplications()) {
                        if (a instanceof MediaCompressor) {
                            sendingApp = (MediaCompressor) a;
                            
                            break;
                        }
                    }
                    assert sendingApp != null;
                    while (formatIndex >= 0 && !sendingApp.getCompressionFormats().contains(sortedFormats.get(formatIndex))) {
                        formatIndex--;
                    }
                    if (formatIndex < 0) {
                        continue;
                    }
                    for (MediaApplication a : newNode.getNodeApplications()) {
                        if (a instanceof MediaDecompressor) {
                            receivingApp = (MediaDecompressor) a;
                            
                            break;
                        }
                    }
                    assert receivingApp != null;
                    while (formatIndex >= 0 && !receivingApp.getDecompressionFormats().contains(sortedFormats.get(formatIndex))) {
                        formatIndex--;
                    }
                    if (formatIndex < 0) {
                        continue;
                    }
                    if (streamSetConsNodes.get(streamSet).contains(newNode)) {
                        seenConsumers++;
                        objective.add(ObjectiveFuncComponent.QUALITY, (double) sortedFormats.get(formatIndex).inverseQuality);
                        receivingQualities[formatIndex]++;
                    } else if (newNode.hasDistributor()) {
                        MediaApplication d = newNode.getNodeApplications().iterator().next();
                        // TODO: Needs fix!!
                    }
                    incomingFormat[newNode.index] = formatIndex;
                    if (incomingFormat[newEdge.getFromNode().index] >= 0 && outgoingFormats[newEdge.getFromNode().index][formatIndex] != true) {
                        outgoingFormats[newEdge.getFromNode().index][formatIndex] = true;
                        final int inFormat = incomingFormat[newEdge.getFromNode().index];
                        objective.add(ObjectiveFuncComponent.LATENCY, (double) sortedFormats.get(formatIndex).latencyOut);
                        for (int i = 0; i < TranscodingResource.values().length; i++) {
                            nodeLoads[newEdge.getFromNode().index][i] += sortedFormats.get(formatIndex).getEncodingResource(TranscodingResource.values()[i]);
                        }
                    }
                    selectedFormats[newEdge.variableIndex] = formatIndex;

                    if (!out.addVertex(newNode)) {
                        candidateEdges.remove(newEdge);
                    } else {
                        out.addEdge(newEdge.getFromNode(), newNode, newEdge);
                    }
                    if (originalResult != null) {
                        if ((newEdge.variableIndex < originalResult.selectedFormats.length && originalResult.selectedFormats[newEdge.variableIndex] == -1)
                            || (!keptOriginalPath[newEdge.getFromNode().index])) {
                            keptOriginalPath[newNode.index] = false;
                        }
                        if (!keptOriginalPath[newNode.index] && newEdge.variableIndex < originalResult.selectedFormats.length && streamSetConsNodes.get(streamSet).contains(newNode)) {
                            disturbedTransmissions++;
                        }
                    }

                    if (currentNode == startNode) {
                        candidateEdges.clear();
                    } else {
                        for (LogicalNetworkLink link : g.incomingEdgesOf(newNode)) {
                            candidateEdges.remove(link);
                            backupCandidateEdges.remove(link);
                        }
                    }
                    currentNode = newNode;
                    break;
                }
                for (LogicalNetworkLink link : g.outgoingEdgesOf(currentNode)) {
                    if (!out.containsVertex(link.getToNode())) {
                        if (changed && !settledAfterChange && originalResult != null && !originalResult.outGraph.containsEdge(link)) {
                            continue;
                        }
                        selectProbability[link.variableIndex] = linkPreferences[link.variableIndex] * pheromone[link.variableIndex];
                        candidateEdges.add(link, selectProbability[link.variableIndex]);
                    }
                }

                if (changed && !settledAfterChange && candidateEdges.isEmpty()) {
                    for (EndpointNetworkNode node : out.vertexSet()) {
                        if (node == startNode) {
                            continue;
                        }
                        for (LogicalNetworkLink link : g.outgoingEdgesOf(node)) {
                            if (!out.containsVertex(link.getToNode())) {
                                selectProbability[link.variableIndex] = linkPreferences[link.variableIndex] * pheromone[link.variableIndex];
                                candidateEdges.add(link, selectProbability[link.variableIndex]);
                            }
                        }
                    }
                }
            } while (seenConsumers < consumersNumber && !candidateEdges.isEmpty());
            if (seenConsumers < consumersNumber) {
                return false;
            }
            eliminator.traverse(out, streamSetConsNodes.get(streamSet), null, true);
            for (LogicalNetworkLink link : out.edgeSet()) {
                objective.add(ObjectiveFuncComponent.LATENCY, link.getLatency());
                if (link.getFromNode() == startNode) {
                    objective.add(ObjectiveFuncComponent.LATENCY, (double) sortedFormats.get(selectedFormats[link.variableIndex]).latencyOut);
                }
            }
            for (EndpointNetworkNode node : out.vertexSet()) {
                boolean transcoding = false;
                for (int f = 0; f < formats.size(); f++) {
                    if (outgoingFormats[node.index][f] && f != incomingFormat[node.index]) {
                        transcoding = true;
                    }
                }
                if (transcoding) {
                    objective.add(ObjectiveFuncComponent.LATENCY, (double) sortedFormats.get(incomingFormat[node.index]).latencyIn);
                }
            }
            outGraph = out;
            return true;
        }

        private void updateGraph() {
            if (!this.needsGraphUpdate) {
                return;
            }

            eliminator.removeUselessElements(g, streamSet);
            if (this.g.containsVertex(startNode)) {
                eliminator.traverse(this.g, java.util.Collections.singleton(startNode), new TraverseModifier() {

                    @Override
                    public void onRemove(LogicalNetworkLink link) {
                    }

                    @Override
                    public void onRemove(EndpointNetworkNode node) {
                        if (node.hasConsumer()) {
                            streamSetConsNodes.get(streamSet).remove(node);
                        }
                // There cannot be producer of the current stream set, since we started from that
                        // There may be distributor, but may stay reachable for other nodes
                    }
                });
            }
            updateConsumersNumber();
            this.needsGraphUpdate = false;
        }

        ArrayList<EndpointNetworkNode> getDestinations(Result r, PhysicalNetworkLink link) {
            ArrayList<EndpointNetworkNode> destinations = new ArrayList<>();
            for (LogicalNetworkLink logical : link.getTraversingLogicalLinks()) {
                if (r.outGraph.containsEdge(logical)) {
                    destinations.addAll(getDestinations(r, logical));
                }
            }
            return destinations;
        }

        ArrayList<EndpointNetworkNode> getDestinations(Result r, LogicalNetworkLink link) {
            ArrayList<EndpointNetworkNode> destinations = new ArrayList<>();
            if (!r.outGraph.containsEdge(link)) {
                return destinations;
            }
            ArrayDeque<EndpointNetworkNode> q = new ArrayDeque<>();
            q.add(link.getToNode());
            while (!q.isEmpty()) {
                EndpointNetworkNode n = q.removeFirst();
                if (r.outGraph.outgoingEdgesOf(n).isEmpty()) {
                    destinations.add(n);
                } else {
                    for (LogicalNetworkLink l : r.outGraph.outgoingEdgesOf(n)) {
                        q.add(l.getToNode());
                    }
                }
            }
            return destinations;
        }

        public void eliminateGraph() {
            eliminator.getStreamSetSubgraph(streamSet, g, true);
        }

        private void resize(TopologyDifference diff) {
            final int oldLogicalLinks = this.pheromone.length;
            final int newLogicalLinks = diff.getNewLogicalLinks().size();
            if (newLogicalLinks > 0) {
                this.pheromone = Arrays.copyOf(pheromone, oldLogicalLinks + newLogicalLinks);
                Arrays.fill(pheromone, oldLogicalLinks, oldLogicalLinks + newLogicalLinks - 1, MINIMUM_PHEROMONE);
                this.selectProbability = new double[oldLogicalLinks + newLogicalLinks];
                this.selectedFormats = new int[oldLogicalLinks + newLogicalLinks];
            }

            final int oldAppNodes = this.outgoingFormats.length;
            final int newAppNodes = diff.getNewAppNodes().size();
            this.incomingFormat = new int[oldAppNodes + newAppNodes];
            this.keptOriginalPath = new boolean[oldAppNodes + newAppNodes];
            Arrays.fill(keptOriginalPath, true);
            this.outgoingFormats = new boolean[oldAppNodes + newAppNodes][formats.size()];
            this.nodeLoads = new int[oldAppNodes + newAppNodes][TranscodingResource.values().length];
            this.candidateEdges.resize(oldLogicalLinks + newLogicalLinks);
            this.backupCandidateEdges.resize(oldLogicalLinks + newLogicalLinks);

//            for (int i = 0; i < 10; i++) {
//                phUpdater.update(globallyBestResults.get(ObjectiveFuncComponent.SUM).objective, 1.0,
//                        globallyBestResults.get(ObjectiveFuncComponent.SUM).outGraph.edgeSet(),
//                        globallyBestResults.get(ObjectiveFuncComponent.SUM).selectedFormats,
//                        pheromone, keptOriginalPath, true, false);
//            }
            Arrays.fill(selectedFormats, -1);
        }

        private void reset() {
            Arrays.fill(pheromone, MINIMUM_PHEROMONE);
            originalResult = null;
        }

        int countDisturbances() {
            int d = 0;
            if (originalResult == null) {
                return 0;
            }
            BellmanFordShortestPath<EndpointNetworkNode, LogicalNetworkLink> paths = new BellmanFordShortestPath<>(outGraph, startNode);
            for (EndpointNetworkNode consumer : streamSetConsNodes.get(streamSet)) {
                if (originalResult.outGraph.containsVertex(consumer)
                    && outGraph.containsVertex(consumer)
                    && !originalPaths.getPathEdgeList(consumer).equals(paths.getPathEdgeList(consumer))) {
                    d++;
                }
            }

            return d;
        }

        private int fixKeptPaths(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g) {
            Deque<EndpointNetworkNode> q = new ArrayDeque<>(g.vertexSet().size());
            int disturbed = 0;
            q.add(startNode);
            while (!q.isEmpty()) {
                EndpointNetworkNode n = q.removeFirst();
                for (LogicalNetworkLink l : g.outgoingEdgesOf(n)) {
                    keptOriginalPath[l.getToNode().index] = keptOriginalPath[n.index] && (l.variableIndex >= originalResult.selectedFormats.length || originalResult.selectedFormats[l.variableIndex] != -1);
                    if (!keptOriginalPath[l.getToNode().index] && streamSetConsNodes.get(streamSet).contains(l.getToNode())) {
                        disturbed++;
                    }
                    q.add(l.getToNode());
                }
            }
            return disturbed;
        }

        int getDisturbedTransmissions() {
            return disturbedTransmissions;
        }

        public int getzIndex() {
            return zIndex;
        }

        public int[][] getNodeLoads() {
            return nodeLoads;
        }

        public Double getObjective() {
            return objective.get(ObjectiveFuncComponent.SUM);
        }

        public Double getObjective(ObjectiveFuncComponent c) {
            return objective.get(c);
        }

        public void setGlobalBest(ObjectiveFuncComponent c) {
            if (copyOptimum) {
                globallyBestResults.put(c,
                    new Result(outGraph != null ? (DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>) outGraph.clone() : null,
                        selectedFormats.clone(), new Objective(objective), this.disturbedTransmissions, this.keptOriginalPath.clone()));
            } else {
                globallyBestResults.put(c, result);
            }
        }

        public DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> getOutGraph() {
            return outGraph;
        }

        public int[] getSelectedFormats() {
            return selectedFormats;
        }

        void setOriginalresult() {
            originalResult = new Result(this.globallyBestResults.get(ObjectiveFuncComponent.SUM));
//            originalPaths = new BellmanFordShortestPath<>(originalResult.outGraph, startNode);
        }

        public int[] getReceivingQualities() {
            return receivingQualities;
        }

        final void updateConsumersNumber() {
            this.consumersNumber = streamSetConsNodes.get(streamSet).size();
        }

        public void requireGraphUpdate() {
            this.needsGraphUpdate = true;
        }

        private int maxReceivingQuality(Result result) {
            int f = receivingQualities.length - 1;
            for (EndpointNetworkNode node : streamSetConsNodes.get(streamSets.get(zIndex))) {
                for (LogicalNetworkLink link : result.outGraph.incomingEdgesOf(node)) {
                    if (result.selectedFormats[link.variableIndex] < f) {
                        f = result.selectedFormats[link.variableIndex];
                    }
                }
            }
            return f;
        }

        private int inflateStreamQuality(Result result) {
//            DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> tree = this.getOutGraph();
            DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> tree = result.outGraph;
            final ArrayList<EndpointNetworkNode> producerNodes = streamSetProdNodes.get(streamSets.get(zIndex));
            final HashSet<EndpointNetworkNode> consumerNodes = streamSetConsNodes.get(streamSets.get(zIndex));
            final Deque<EndpointNetworkNode> q;
            final HashSet<LogicalNetworkLink> toInflateForward = new HashSet<>();
            final HashSet<LogicalNetworkLink> toInflateBackward = new HashSet<>();
            final HashSet<LogicalNetworkLink> reallyInflate = new HashSet<>();
            final int maxFormat = maxReceivingQuality(result);
            int inflated = 0;
            int inflatedConsumers = 0;
            if (tree == null || !tree.containsVertex(startNode)) {
                return 0;
            }
            q = new ArrayDeque<>();
            for (EndpointNetworkNode node : consumerNodes) {
                for (LogicalNetworkLink link : tree.incomingEdgesOf(node)) {
                    if (result.selectedFormats[link.variableIndex] < formatBws.length - 1) {
                        q.add(node);
                    }
                }
            }
            while (!q.isEmpty()) {
                final EndpointNetworkNode curNode = q.removeFirst();
                for (LogicalNetworkLink link : tree.incomingEdgesOf(curNode)) {
                    final int formatIndex = result.selectedFormats[link.variableIndex];
                    if (formatIndex < maxFormat) {
                        final long currentBw = formatBws[formatIndex];
                        final long newBw = formatBws[formatIndex + 1];
                        final long bwDiff = currentBw - newBw;
                        boolean fits = true;
                        for (PhysicalNetworkLink physical : link.getPhysicalLinksOnThePath()) {
                            if (physicalLinkCapacities.get(physical.variableIndex)[sampleSize / 2] - physicalLinkLoad[physical.variableIndex] < bwDiff) {
                                fits = false;
                                break;
                            }
                        }
                        if (!fits) {
                            continue;
                        }
                    }
                    if (q.contains(link.getFromNode())) {
                        continue;
                    }
                    if (consumerNodes.contains(link.getToNode())) {
                        reallyInflate.add(link);
                    }
                    for (LogicalNetworkLink downLink : tree.outgoingEdgesOf(link.getToNode())) {
                        if (reallyInflate.contains(downLink) && result.selectedFormats[link.variableIndex] <= result.selectedFormats[downLink.variableIndex]) {
                            reallyInflate.add(link);
                        }
                    }

                    toInflateBackward.add(link);
                    q.add(link.getFromNode());
                }
            }
            q.clear();
            q.addAll(producerNodes);
            while (!q.isEmpty()) {
                final EndpointNetworkNode curNode = q.removeFirst();
                for (LogicalNetworkLink outLink : tree.outgoingEdgesOf(curNode)) {
                    if (toInflateBackward.contains(outLink)) {
                        toInflateForward.add(outLink);
                        q.add(outLink.getToNode());
                    }
                }
            }

            for (LogicalNetworkLink link : toInflateForward) {
                if (!reallyInflate.contains(link)) {
                    continue;
                }
                final int originalFormat = result.selectedFormats[link.variableIndex];
                final long bwDiff = formatBws[originalFormat + 1] - formatBws[originalFormat];
                for (PhysicalNetworkLink physical : link.getPhysicalLinksOnThePath()) {
                    physicalLinkLoad[physical.variableIndex] += bwDiff;
                }
                result.selectedFormats[link.variableIndex]++;
                inflated++;
                if (link.getToNode().hasConsumer()) {
                    inflatedConsumers++;
                    final int qdiff = sortedFormats.get(originalFormat + 1).inverseQuality - sortedFormats.get(originalFormat).inverseQuality;
                    result.objective.add(ObjectiveFuncComponent.SUM, (double) qdiff);
                    inflatedQuality += qdiff;
                }
            }
            return inflatedConsumers;
        }

        int[] countReceivingQualities(Result res) {
            if (outGraph == null) {
                return null;
            }

            int[] counts = new int[sortedFormats.size()];
            for (EndpointNetworkNode node : streamSetConsNodes.get(streamSet)) {
                int linkIndex = -1;
                for (LogicalNetworkLink inLink : res.outGraph.incomingEdgesOf(node)) {
                    linkIndex = inLink.variableIndex;
                }
                if (linkIndex != -1) {
                    counts[res.selectedFormats[linkIndex]]++;
                }
            }
            return counts;
        }

        private class CandidateEdgeSet {

            private final LinkedHashSet<LogicalNetworkLink> edgeSet;
            private RouleteSelector selector;

            public CandidateEdgeSet() {
                edgeSet = new LinkedHashSet<>();
                selector = new RouleteSelector(logicalLinks.size());
            }

            public void clear() {
                edgeSet.clear();
                selector.clear();
            }

            public boolean isEmpty() {
                return edgeSet.isEmpty();
            }

            public void add(LogicalNetworkLink link, double probability) {
                if (edgeSet.add(link)) {
                    selector.push(link.variableIndex, probability);
                }
            }

            public void remove(LogicalNetworkLink link) {
                if (edgeSet.remove(link)) {
                    selector.pop(link.variableIndex);
                }
            }

            public double getTotalWeight() {
                return selector.getTotalWeight();
            }

            public int getRandomLinkIndex(double random) {
                if (isEmpty()) {
                    return -1;
                }
                return selector.get(random);
            }

            public void resize(int logicalLinks) {
                selector = new RouleteSelector(logicalLinks);
            }
        }

        private class Result {

            private final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> outGraph;
            private final int selectedFormats[];
            private final Objective objective;
            private final int disturbed;
            private final boolean keptOriginalPath[];

            private Result() {
                this.objective = new Objective();
                this.selectedFormats = new int[logicalLinks.size()];
                this.outGraph = new DirectedWeightedMultigraph<>(LogicalNetworkLink.class);
                this.disturbed = 0;
                this.keptOriginalPath = null;
            }

            public Result(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> outGraph, int[] selectedFormats, Objective objective, int disturbed, boolean keptOriginalPath[]) {
                this.outGraph = outGraph;
                this.selectedFormats = selectedFormats;
                this.objective = objective;
                this.disturbed = disturbed;
                this.keptOriginalPath = keptOriginalPath;
            }

            public Result(Result src) {
                this.outGraph = (DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>) src.outGraph.clone();
                this.selectedFormats = Arrays.copyOf(src.selectedFormats, src.selectedFormats.length);
                this.objective = new Objective(src.objective);
                this.disturbed = src.disturbed;
                if (src.keptOriginalPath != null) {
                    this.keptOriginalPath = Arrays.copyOf(src.keptOriginalPath, src.selectedFormats.length);
                } else {
                    this.keptOriginalPath = null;
                }
            }

            public StringBuilder appendToSB(StringBuilder sb) {

                if (outGraph == null || !outGraph.containsVertex(ThreadedAnt.this.startNode)) {
                    return sb;
                }
                BreadthFirstIterator<EndpointNetworkNode, LogicalNetworkLink> it = new BreadthFirstIterator<>(outGraph, ThreadedAnt.this.startNode);

                while (it.hasNext()) {
                    final EndpointNetworkNode node = it.next();
                    sb.append("Node: ").append(node).append(" index: ").append(node.index).append("\n");
                    for (LogicalNetworkLink link : outGraph.outgoingEdgesOf(node)) {
                        sb.append("\t--> ").append(link.getToNode()).append("; Format ").append(sortedFormats.get(selectedFormats[link.variableIndex]))
                            .append(" Index: ").append(link.variableIndex).append(" Pheromone: ").append(pheromone[link.variableIndex]).append("\n");
                    }
                    if (outGraph.outgoingEdgesOf(node).isEmpty()) {
                        for (LogicalNetworkLink inLink : outGraph.incomingEdgesOf(node)) {
                            sb.append("\tReceiving format ").append(sortedFormats.get(selectedFormats[inLink.variableIndex])).append("\n");
                        }
                    }
                }

                return sb;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();

                appendToSB(sb);
                sb.append("\nObjective:\n");
                objective.calculateSum();
                objective.appendToSB(sb);

                return sb.toString();
            }

        }
    }

    private class RouleteSelector {

        double weights[];
        double subtreeWeights[];
        int indices[];
        int indexPositions[];
        int lastPos = 0;

        public RouleteSelector(int maxItems) {
            assert maxItems > 0;
            final int storageSize = maxItems * 2 + 1;
            weights = new double[storageSize];
            subtreeWeights = new double[storageSize];
            indices = new int[storageSize];
            Arrays.fill(indices, -1);
            indexPositions = new int[storageSize];
        }

        double getTotalWeight() {
            return subtreeWeights[1];
        }

        void clear() {
            Arrays.fill(weights, 0, lastPos, 0.0);
            Arrays.fill(subtreeWeights, 0, lastPos, 0.0);
            Arrays.fill(indices, 0, lastPos, -1);
            Arrays.fill(indexPositions, 0);
            lastPos = 0;
        }

        private void dump() {
            if (MatchMaker.logger.isDebugEnabled()) {
                MatchMaker.logger.debug("RouteSelector " + this.toString());
                MatchMaker.logger.debug("\tweights: " + Arrays.toString(weights));
                MatchMaker.logger.debug("\tweightSubtrees: " + Arrays.toString(subtreeWeights));
                MatchMaker.logger.debug("\tindices: " + Arrays.toString(indices));
                MatchMaker.logger.debug("\tindexPositions: " + Arrays.toString(indexPositions));
                MatchMaker.logger.debug("\tlastPos: " + lastPos);
            }
        }

        void push(int index, double weight) {
            int pos = lastPos + 1;
            while (indices[pos >> 1] == -1 && pos > 1) {
                pos >>= 1;
            }
            lastPos = Math.max(lastPos, pos);
            indices[pos] = index;
            weights[pos] = weight;
            subtreeWeights[pos] = weight;
            indexPositions[index] = pos;
            pos >>= 1;
            while (pos >= 1) {
                subtreeWeights[pos] += weight;
                pos >>= 1;
            }

            dump();
        }

        void pop(int index) {
            final int posToRemove = indexPositions[index];
            final double weightOut = weights[posToRemove];
            final double weightReplace = weights[lastPos];
            final double weightDiff = weightOut - weightReplace;

            indexPositions[indices[lastPos]] = posToRemove;
            indexPositions[index] = 0;
            indices[posToRemove] = indices[lastPos];
            indices[lastPos] = -1;
            weights[posToRemove] = weights[lastPos];
            weights[lastPos] = 0.0;

            int pos = lastPos;
            while (pos > 1) {
                subtreeWeights[pos] -= weightReplace;
                pos >>= 1;
            }

            pos = posToRemove;
            while (pos > 1) {
                subtreeWeights[pos] -= weightDiff;
                pos >>= 1;
            }
            subtreeWeights[1] -= weightOut;
            while (indices[lastPos] == -1 && lastPos > 0) {
                lastPos--;
            }
            dump();
        }

        int get(double prob) {
            int pos = 1;
            while (prob > weights[pos]) {
                prob -= weights[pos];
                pos <<= 1;
                if (prob > subtreeWeights[pos]) {
                    prob -= subtreeWeights[pos];
                    pos++;
                }
            }

            return indices[pos];
        }
    }

    private class PerStreamEliminator {

        private final DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g;

        public PerStreamEliminator(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g) {
            this.g = g;
        }

        public boolean nodeCanSend(EndpointNetworkNode node, MediaStreamSet z) {
            for (MediaApplication a : node.getNodeApplications()) {
                if (a instanceof MediaApplicationDistributor) {
                    return true;
                }
                if (a instanceof MediaApplicationProducer && streamSetProdNodes.get(z).contains(node)) {
                    return true;
                }
            }
            return false;
        }

        public boolean nodeCanReceive(EndpointNetworkNode node, MediaStreamSet z) {
            for (MediaApplication a : node.getNodeApplications()) {
                if (a instanceof MediaApplicationDistributor) {
                    return true;
                }
                if ((a instanceof MediaApplicationConsumer) && streamSetConsNodes.get(z).contains(node)) {
                    return true;
                }
            }
            return false;
        }

        public boolean keepNode(EndpointNetworkNode node, MediaStreamSet z) {
            for (MediaApplication a : node.getNodeApplications()) {
                if (a instanceof MediaApplicationConsumer && streamSetConsNodes.get(z).contains(node)) {
                    return true;
                }
                if (a instanceof MediaApplicationDistributor) {
                    return true;
                }
                if (a instanceof MediaApplicationProducer && streamSetProdNodes.get(z).contains(node)) {
                    return true;
                }
            }

            return false;
        }

        DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> getStreamSetSubgraph(MediaStreamSet z, DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> in, boolean inPlace) {
            DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> out;
            if (inPlace) {
                out = in;
            } else {
                out = (DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink>) in.clone();
            }

            removeUselessElements(out, z);

            ArrayList<EndpointNetworkNode> producerNodes = streamSetProdNodes.get(z);
            HashSet<EndpointNetworkNode> consumerNodes = streamSetConsNodes.get(z);

            traverse(out, producerNodes, null);
            traverse(out, consumerNodes, null, true);

            return out;
        }

        public void removeUselessElements(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> g, MediaStreamSet z) {
            ArrayList<EndpointNetworkNode> nodesToRemove = new ArrayList<>();
            ArrayList<LogicalNetworkLink> linksToRemove = new ArrayList<>();

            for (EndpointNetworkNode node : g.vertexSet()) {
                final boolean canSend = nodeCanSend(node, z);
                final boolean canReceive = nodeCanReceive(node, z);

                if (!canSend && !canReceive) {
                    nodesToRemove.add(node);
                } else {
                    if (!canReceive) {
                        linksToRemove.addAll(g.incomingEdgesOf(node));
                    } else if (!canSend) {
                        linksToRemove.addAll(g.outgoingEdgesOf(node));
                    }
                }
            }
            g.removeAllVertices(nodesToRemove);
            g.removeAllEdges(linksToRemove);
        }

        DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> getStreamSetSubgraph(MediaStreamSet z) {
            return getStreamSetSubgraph(z, g, false);
        }

        private void traverse(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> out, Collection<EndpointNetworkNode> startNodes, TraverseModifier modifier) {
            traverse(out, startNodes, modifier, false);
        }

        void traverse(DirectedWeightedMultigraph<EndpointNetworkNode, LogicalNetworkLink> out, Collection<EndpointNetworkNode> startNodes, TraverseModifier modifier, boolean backwards) {
            HashSet<EndpointNetworkNode> seenNodes = new HashSet<>(startNodes);
            HashSet<LogicalNetworkLink> traversedEdges = new HashSet<>(out.edgeSet().size(), (float) 1.0);
            Deque<EndpointNetworkNode> q = new ArrayDeque<>(startNodes);

            while (!q.isEmpty()) {
                EndpointNetworkNode activeNode = q.removeFirst();

                Set<LogicalNetworkLink> incidentEdges;
                incidentEdges = backwards ? out.incomingEdgesOf(activeNode) : out.outgoingEdgesOf(activeNode);
                for (LogicalNetworkLink edge : incidentEdges) {
                    traversedEdges.add(edge);
                    final EndpointNetworkNode incidentNode = backwards ? edge.getFromNode() : edge.getToNode();
                    if (!seenNodes.contains(incidentNode)) {
                        seenNodes.add(incidentNode);
                        q.add(incidentNode);
                    }
                }
            }

            ArrayList<EndpointNetworkNode> nodesToRemove = new ArrayList<>();
            for (EndpointNetworkNode node : out.vertexSet()) {
                if (!seenNodes.contains(node)) {
                    nodesToRemove.add(node);
                }
            }
            out.removeAllVertices(nodesToRemove);

            ArrayList<LogicalNetworkLink> linksToRemove = new ArrayList<>();
            for (LogicalNetworkLink link : out.edgeSet()) {
                if (!traversedEdges.contains(link)) {
                    linksToRemove.add(link);
                }
            }
            out.removeAllEdges(linksToRemove);

            if (modifier != null) {
                for (EndpointNetworkNode node : nodesToRemove) {
                    modifier.onRemove(node);
                }
                for (LogicalNetworkLink link : linksToRemove) {
                    modifier.onRemove(link);
                }
            }
        }
    }

    private interface TraverseModifier {

        void onRemove(LogicalNetworkLink link);

        void onRemove(EndpointNetworkNode node);
    }

    private class FormatsBwAscComparator implements Comparator<StreamFormat>, Serializable {

        @Override
        public int compare(StreamFormat t, StreamFormat t1) {
            if (t.bandwidthMin < t1.bandwidthMin) {
                return -1;
            } else if (t.bandwidthMin > t1.bandwidthMin) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    enum ObjectiveFuncComponent {

        SUM, LATENCY, QUALITY
    }

    interface PheromoneUpdater {

        void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved);

        void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved, boolean evaporate);

        double getUpdateValue(MatchMakerACO.Objective objective, double probability);
    }

    class NCA2014Updater implements PheromoneUpdater {

        static final double pheromoneEvaporation = 0.97;
        static final double MINIMUM_PHEROMONE = 0.1;

        @Override
        public void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved) {
            update(objective, probability, logicalLinks, selectedFormats, pheromones, keptOriginal, improved, true);
        }

        @Override
        public void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved, boolean evaporate) {
            if (!improved) {
                for (LogicalNetworkLink link : logicalLinks) {
                    final int index = link.variableIndex;
                    if (selectedFormats[index] != -1) {
                        selectedFormats[index] = -1;
                        keptOriginal[link.getToNode().index] = true;
                    }
                    if (evaporate) {
                        pheromones[index] = Math.max(pheromones[index] * pheromoneEvaporation, MINIMUM_PHEROMONE);
                    }
                }
            } else {
                final double f = consumersCount * 250 / objective.get(ObjectiveFuncComponent.SUM) * probability;
                for (LogicalNetworkLink link : logicalLinks) {
                    final int index = link.variableIndex;
                    if (selectedFormats[index] != -1) {
                        selectedFormats[index] = -1;
                        pheromones[index] += f;
                        keptOriginal[link.getToNode().index] = true;
                    }
                    if (evaporate) {
                        pheromones[index] = Math.max(pheromones[index] * pheromoneEvaporation, MINIMUM_PHEROMONE);
                    }
                }
            }
        }

        @Override
        public double getUpdateValue(MatchMakerACO.Objective objective, double probability) {
            return consumersCount * 250 / objective.get(ObjectiveFuncComponent.SUM) * probability;
        }

    }

    class EveryIterUpdater implements PheromoneUpdater {

        static final double pheromoneEvaporation = 0.9;
        static final double MINIMUM_PHEROMONE = 0.1;

        @Override
        public void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved) {
            update(objective, probability, logicalLinks, selectedFormats, pheromones, keptOriginal, improved, true);
        }

        @Override
        public void update(MatchMakerACO.Objective objective, double probability, Collection<LogicalNetworkLink> logicalLinks, int[] selectedFormats, double[] pheromones, boolean keptOriginal[], boolean improved, boolean evaporate) {
            final double f = getUpdateValue(objective, probability);
            for (LogicalNetworkLink link : logicalLinks) {
                final int index = link.variableIndex;
                if (selectedFormats[index] != -1) {
                    selectedFormats[index] = -1;
                    pheromones[index] += f;
                    keptOriginal[link.getToNode().index] = true;
                }
                if (evaporate) {
                    pheromones[index] = Math.max(pheromones[index] * pheromoneEvaporation, MINIMUM_PHEROMONE);
                }
            }
        }

        @Override
        public double getUpdateValue(MatchMakerACO.Objective objective, double probability) {
            return consumersCount * 250 / objective.get(ObjectiveFuncComponent.SUM) * probability;
        }
    }

    private class PortAssigner {

        private static final int FIRST_PORT = 5024;
        private static final int PORT_STEP = 4;

        private final HashMap<MediaApplication, Integer> appToPortMap = new HashMap<>();
        private final LinkedList<Integer> freePorts = new LinkedList<>();

        public PortAssigner() {
            for (int i = 0; i < antThreads.size(); i++) {
                freePorts.add(FIRST_PORT + i * PORT_STEP);
            }
        }

        public String assignPort(MediaApplicationProducer p) {
            if (appToPortMap.containsKey(p)) {
                return appToPortMap.get(p).toString();
            }

            Integer port = freePorts.pop();
            appToPortMap.put(p, port);

            return port.toString();
        }

        public String getPort(MediaApplication p) {
            if (appToPortMap.get(p) == null) {
                return null;
            }
            return appToPortMap.get(p).toString();
        }

        public void unassignPort(MediaApplication p) {
            if (appToPortMap.get(p) != null) {
                freePorts.offer(appToPortMap.get(p));
                appToPortMap.remove(p);
            }
        }

        public void cleanup(Iterable<MediaApplication> activeProducers) {
            for (MediaApplication producer : activeProducers) {
                if (getPort(producer) != null) {
                    appToPortMap.remove(producer);
                }
            }

            for (MediaApplication producer : appToPortMap.keySet()) {
                unassignPort(producer);
            }
        }
    }

}
