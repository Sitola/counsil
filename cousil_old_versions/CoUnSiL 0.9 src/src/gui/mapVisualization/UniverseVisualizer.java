package gui.mapVisualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import mediaAppFactory.MediaApplication;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.mapviewer.WaypointRenderer;
import utils.CircleShimGenerator;
import utils.GeoLocation;
import utils.Shim;
import utils.ShimGenerator;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 22, 2008
 * Time: 4:37:21 PM
 * To change this template use File | Settings | File Templates.
 */

public class UniverseVisualizer extends JXMapViewer {

    public static enum TileProviderEnum {OpenStreetMaps, GoogleMaps, NasaMaps};
    
    public class StreamWaypoint extends Waypoint {

        final String fromNodeUuid, toNodeUuid;
        double shim = 0.5;
        JSONObject streamInfo;
        Color color;

        public StreamWaypoint(String fromNodeUuid, String toNodeUuid, Color color, JSONObject streamInfo) {
            this.fromNodeUuid = fromNodeUuid;
            this.toNodeUuid = toNodeUuid;
            this.streamInfo = streamInfo;
            this.color = color;
        }

        @SuppressWarnings({"DesignForExtension"})
        public void setPosition(GeoPosition geoPosition) {
        }

        public GeoPosition getPosition() {
            return getFromPosition();
        }

        public JSONObject getStreamInfo() {
            return streamInfo;
        }
        
        public GeoPosition getFromPosition() {
            try {
                return getNodeLocation(getNode(fromNodeUuid)).toGeoPosition();
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("Failed to read geo position: " + ex.toString(), ex);
            } catch (NullPointerException ex) {
                Logger.getLogger(this.getClass()).error("BUG: inconsistency between node caches: " + ex.toString(), ex);
            }
            return new GeoPosition(0, 0);
        }

        public GeoPosition getToPosition() {
            try {
                return getNodeLocation(getNode(toNodeUuid)).toGeoPosition();
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("Failed to read geo position: " + ex.toString(), ex);
            } catch (NullPointerException ex) {
                Logger.getLogger(this.getClass()).error("BUG: inconsistency between node caches: " + ex.toString(), ex);
            }
            return new GeoPosition(0, 0);
        }
        
        private Shim getToShim() {
            try {
                return nodeShims.get(toNodeUuid);
            } catch (NullPointerException ex) {
                Logger.getLogger(this.getClass()).error("BUG: inconsistency between node caches: " + ex.toString(), ex);
                return null;
            }
        }

        private Shim getFromShim() {
            try {
                return nodeShims.get(fromNodeUuid);
            } catch (NullPointerException ex) {
                Logger.getLogger(this.getClass()).error("BUG: inconsistency between node caches: " + ex.toString(), ex);
                return null;
            }
        }

        public Color getColor() {
            return color;
        }
    }

    private JSlider zoomSlider;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private NodeWaipointInfoPanel selectedNodeWaypointInfoPanel;

    private GeoPosition startPosition;
    private TileFactory currentFactory = null;
    // private final DirectedWeightedMultigraph<GeneralNetworkNode, GeneralNetworkLink> networkTopologyGraph;

    private UniverseWaypoint lastClickedWaypoint = null;
    
    private final HashMap<String, JSONObject> nodes = new HashMap<>();
    private final HashMap<String, HashSet<String> > interfacesByNode = new HashMap<>();
    private final HashMap<String, String> nodesByInterfaces = new HashMap<>();
    private final HashMap<String, JSONObject> interfaces = new HashMap<>();
    private final HashMap<String, Shim> nodeShims = new HashMap<>();
    private final HashMap<String, HashSet<String>> appsByNode = new HashMap<>();
    private final HashMap<String, String> nodeByApp = new HashMap<>();
    private final HashMap<String, JSONObject> applications = new HashMap<>();
    private final HashMap<String, String> ifaceByAddress = new HashMap<>();
    private final HashMap<String, UniverseWaypoint> nodeWaypointMap = new HashMap<>();
    private final HashMap<String, HashMap<String, List<StreamWaypoint>>> streamWaypointMap = new HashMap<String, HashMap<String, List<StreamWaypoint>>>();
    
    private String localNodeUuid;
    
    private static String getInterfaceUuid(JSONObject iface) throws JSONException {
        return iface.getString(EndpointNodeInterface.ConfigKeyUuid);
    }
    private static String getAppUuid(JSONObject app) throws JSONException {
        return app.getString(MediaApplication.ConfigKeyUuid);
    }
    private static String getNodeUuid(JSONObject node) throws JSONException {
        return node.getString(EndpointNetworkNode.ConfigKeyUUID);
    }
    private static GeoLocation getNodeLocation(JSONObject node) throws JSONException {
        GeoLocation retval = new GeoLocation();
        retval.loadConfig(node.getJSONObject(EndpointNetworkNode.ConfigKeyLocation));
        return retval;
    }
    private static JSONArray getNodeInterfaces(JSONObject node) throws JSONException {
        return node.getJSONArray(EndpointNetworkNode.ConfigKeyNodeInterfaces);
    }
    private static String getNodeName(JSONObject node) {
        try {
            return node.getString(EndpointNetworkNode.ConfigKeyNodeName);
        } catch (JSONException ex) {
            Logger.getLogger(UniverseVisualizer.class).error(ex.toString());
            return "";
        }
    }
    public JSONObject getNode(String nodeUuid) {
        return nodes.get(nodeUuid);
    }
    private JSONObject getInterface(String ifaceUuid) {
        return interfaces.get(ifaceUuid);
    }
    private JSONObject getApp(String appUuid) {
        return applications.get(appUuid);
    }
    private static String getIfaceAddress(JSONObject iface) {
        try {
            return iface.getString(EndpointNodeInterface.ConfigKeyAddress);
        } catch (JSONException ex) {
            Logger.getLogger(UniverseVisualizer.class).error(ex.toString());
            return null;
        }
    }
    public HashMap<String, JSONObject> getNodeApplications(String nodeUuid){
        HashMap<String, JSONObject> retval = new HashMap<>();
        
        for (String appUuid : appsByNode.get(nodeUuid)) {
            retval.put(appUuid, getApp(appUuid));
        }
        
        return retval;
    }
    
    
    private void updateInterface(String nodeUuid, JSONObject iface) throws JSONException {
        if (!interfacesByNode.containsKey(nodeUuid)) {
            interfacesByNode.put(nodeUuid, new HashSet<String>());
        }
        
        Set<String> nodeInterfaces = interfacesByNode.get(nodeUuid);
        String ifaceUuid = getInterfaceUuid(iface);
        nodeInterfaces.add(ifaceUuid);
        
        interfaces.put(ifaceUuid, iface);
        nodesByInterfaces.put(ifaceUuid, nodeUuid);
        String address = getIfaceAddress(iface);
        if (address != null) {
            ifaceByAddress.put(address, ifaceUuid);
        }
    }
    
    private void updateApplication(String nodeUuid, JSONObject app) throws JSONException {
        if (!appsByNode.containsKey(nodeUuid)) {
            appsByNode.put(nodeUuid, new HashSet<String>());
        }
        
        Set<String> nodeApplications = appsByNode.get(nodeUuid);
        String appUuid = getAppUuid(app);
        nodeApplications.add(appUuid);
        
        nodeByApp.put(appUuid, nodeUuid);
        applications.put(appUuid, app);
    }
    
    private void updateNode(JSONObject node) throws JSONException {
        String nodeUuid = node.getString(EndpointNetworkNode.ConfigKeyUUID);
        nodes.put(nodeUuid, node);
        
        Set<String> ifaceUuids = interfacesByNode.get(nodeUuid);
        if (ifaceUuids != null) {
            for (String ifaceUuid : ifaceUuids) {
                nodesByInterfaces.remove(ifaceUuid);
                
                JSONObject iface = getInterface(ifaceUuid);
                String address = getIfaceAddress(iface);
                interfaces.remove(ifaceUuid);
                if (address != null) ifaceByAddress.remove(address);
            }
        }
        interfacesByNode.put(nodeUuid, new HashSet<String>());
        
        // old mappings cleaned up, store new interfaces
        JSONArray newInterfaces = getNodeInterfaces(node);
        for (int ifaceIdx = 0; newInterfaces != null && ifaceIdx < newInterfaces.length(); ++ifaceIdx) {
            JSONObject iface = newInterfaces.getJSONObject(ifaceIdx);
            updateInterface(nodeUuid, iface);
        }
        // networks done
        
        // waypoint update
        if (!nodeWaypointMap.containsKey(nodeUuid)) {
            UniverseWaypoint waypoint = new UniverseWaypoint(nodeUuid, getNodeLocation(node).toGeoPosition());
            checkAndAdjustShims();
            nodeWaypointMap.put(nodeUuid, waypoint);
            painter.getWaypoints().add(waypoint);
        }
        nodeWaypointMap.get(nodeUuid).nodePosition = getNodeLocation(node).toGeoPosition();
        
        // waypoint update
        Set<String> appUuids = appsByNode.get(nodeUuid);
        if (appUuids != null) {
            for (String appUuid : appUuids) {
                nodeByApp.remove(appUuid);
                applications.remove(appUuid);
            }
        }
        appsByNode.put(nodeUuid, new HashSet<String>());
        
        // old mappings cleaned up, store new interfaces
        try {
            JSONArray nodeApplications = node.getJSONArray("applications");
            for (int appIdx = 0; nodeApplications != null && appIdx < nodeApplications.length(); ++appIdx) {
                JSONObject app = nodeApplications.getJSONObject(appIdx);
                updateApplication(nodeUuid, app);
            }
        } catch (JSONException ex) {
            Logger.getLogger(this.getClass()).info("Node " + nodeUuid + " does not hold any applications (the entry is missing)", ex);
        }
        
    }
    
    private void removeNode(String nodeUuid) {
        JSONObject node = getNode(nodeUuid);
        
        Set<String> ifaceUuids = interfacesByNode.get(nodeUuid);
        if (ifaceUuids != null) {
            for (String ifaceUuid : ifaceUuids) {
                nodesByInterfaces.remove(ifaceUuid);
                interfaces.remove(ifaceUuid);
            }
            interfacesByNode.remove(nodeUuid);
        }
        // node->ifaces cleaned, ifaces->node cleaned, ifaces cleaned
        
        Set<String> appUuids = appsByNode.get(nodeUuid);
        if (appUuids != null) {
            for (String appUuid : appUuids) {
                applications.remove(appUuid);
            }
            appsByNode.remove(nodeUuid);
        }
        // node->apps cleaned, apps cleaned

        UniverseWaypoint waypoint = nodeWaypointMap.get(nodeUuid);
        if (waypoint != null) {
            painter.getWaypoints().remove(waypoint);
            nodeWaypointMap.remove(nodeUuid);
        }

        nodes.remove(nodeUuid);
        nodeShims.remove(nodeUuid);
    }    
    
    //for lucid overwiev is better to pack following atribute
    class UniverseNetworkPainter extends WaypointPainter<JXMapViewer> {
        UniverseNetworkPainter(WaypointRenderer renderer) {
            setRenderer(renderer);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
            //figure out which waypoints are within this map viewport
            //so, get the bounds
            Rectangle viewportBounds = map.getViewportBounds();
            int zoom = map.getZoom();
            Dimension sizeInTiles = map.getTileFactory().getMapSize(zoom);
            int tileSize = map.getTileFactory().getTileSize(zoom);
            Dimension sizeInPixels = new Dimension(sizeInTiles.width * tileSize, sizeInTiles.height * tileSize);

            double vpx = viewportBounds.getX();
            // normalize the left edge of the viewport to be positive
            while (vpx < 0) {
                vpx += sizeInPixels.getWidth();
            }
            // normalize the left edge of the viewport to no wrap around the world
            while (vpx > sizeInPixels.getWidth()) {
                vpx -= sizeInPixels.getWidth();
            }

            // create two new viewports next to eachother
            Rectangle2D vp2 = new Rectangle2D.Double(vpx,
                    viewportBounds.getY(), viewportBounds.getWidth(), viewportBounds.getHeight());
            Rectangle2D vp3 = new Rectangle2D.Double(vpx - sizeInPixels.getWidth(),
                    viewportBounds.getY(), viewportBounds.getWidth(), viewportBounds.getHeight());

            //for each waypoint within these bounds
            for (Waypoint w : getWaypoints()) {
                Point2D point = map.getTileFactory().geoToPixel(w.getPosition(), map.getZoom());
                if (w instanceof UniverseWaypoint && vp2.contains(point) ) {
                    paintUniverseWaypoint((UniverseWaypoint)w, g, map, point, vp2);
                } else if (w instanceof StreamWaypoint) {
                    paintStreamWaypoint((StreamWaypoint)w, g, map, point, vp2);
                }
                
                if (vp3.contains(point)) {
                    int x = (int) (point.getX() - vp3.getX());
                    int y = (int) (point.getY() - vp3.getY());
                    g.translate(x, y);
                    paintWaypoint(w, map, g);
                    g.translate(-x, -y);
                }
            }

        }
        
        private void paintUniverseWaypoint(UniverseWaypoint uvpt, Graphics2D g, JXMapViewer map, Point2D point, Rectangle2D vp) {
            int x = (int) (point.getX() - vp.getX());
            int y = (int) (point.getY() - vp.getY());

            Shim shim = nodeShims.get(uvpt.nodeUuid);
            System.out.println("Painting waypoint " + uvpt.nodeUuid + " with shim " + shim);
            int shimX = shim == null ? 0 : shim.getMyShimX();
            int shimY = shim == null ? 0 : shim.getMyShimY();
            
            g.translate(x + shimX, y + shimY);
            uvpt.setMapOriginPosition(x,y);
            paintWaypoint(uvpt, map, g);
            g.translate(-(x + shimX), -(y + shimY));
        }
        private void paintStreamWaypoint(StreamWaypoint uvpt, Graphics2D g, JXMapViewer map, Point2D point, Rectangle2D vp) {
            int x = (int) (point.getX() - vp.getX());
            int y = (int) (point.getY() - vp.getY());
            g.translate(x, y);
            paintWaypoint(uvpt, map, g);
            g.translate(-x, -y);
        }
    }
    private final WaypointRenderer nodeRenderer = new WaypointRenderer() {
        private boolean paintUniverseWaypoint(Graphics2D graphics2D, JXMapViewer jxMapViewer, UniverseWaypoint waypoint) {
            JSONObject node = getNode(waypoint.getNodeUuid());
            if (node == null) return false;
            
            graphics2D = (Graphics2D) graphics2D.create();

            graphics2D.translate(waypoint.getShimX(), waypoint.getShimY());

            // get size of node description
            Font f = graphics2D.getFont();
            FontRenderContext frc = graphics2D.getFontRenderContext();
            Rectangle2D tRect = f.getStringBounds(getNodeName(node), frc);
            LineMetrics lMetrics = f.getLineMetrics(getNodeName(node), frc);
            int boxWidth = (int) Math.round(tRect.getWidth() + 10);
            int boxHeight = (int) Math.round(tRect.getHeight() + 5);
            waypoint.setSize(boxWidth, boxHeight);
            // draw the node
            //graphics2D.setColor(new Color(255, 0, 0, 50));
            graphics2D.setPaint(new Color(0, 0, 0, 150));
            graphics2D.fillRoundRect((int) Math.round(-0.5 * boxWidth), (int) Math.round(-0.5 * boxHeight), boxWidth, boxHeight, 20, 20);
            graphics2D.setColor(Color.white);
            graphics2D.drawString(getNodeName(node), Math.round(-0.5 * tRect.getWidth()), Math.round(-0.5 * tRect.getHeight() + lMetrics.getAscent()));


            graphics2D.setColor(Color.black);
            // draw the anchor to physical location
            if (waypoint.getShimX() != 0 || waypoint.getShimY() != 0) {
                graphics2D.setStroke(new BasicStroke(1));
                graphics2D.drawLine(0, 0, - waypoint.getShimX(), - waypoint.getShimY());
                graphics2D.fillOval(-waypoint.getShimX() - 2, -waypoint.getShimY() - 2, 4, 4);
            }

            // restore the original position
            graphics2D.translate(-waypoint.getShimX(), -waypoint.getShimY());

            return false;
        }

        private boolean paintStreamWaypoint(Graphics2D graphics2D, JXMapViewer jxMapViewer, StreamWaypoint streamWaypoint) {
            Point2D fromPoint = jxMapViewer.getTileFactory().geoToPixel(streamWaypoint.getFromPosition(), jxMapViewer.getZoom());
            Shim fromShim = streamWaypoint.getFromShim();
            fromPoint.setLocation(fromPoint.getX() + (fromShim != null ? fromShim.getMyShimX() : 0), fromPoint.getY() + (fromShim != null ? fromShim.getMyShimY() : 0));

            Point2D toPoint = jxMapViewer.getTileFactory().geoToPixel(streamWaypoint.getToPosition(), jxMapViewer.getZoom());
            Shim toShim = streamWaypoint.getToShim();
            toPoint.setLocation(toPoint.getX() + (toShim != null ? toShim.getMyShimX() : 0), toPoint.getY() + (toShim != null ? toShim.getMyShimY() : 0));
            graphics2D.translate(fromShim.getMyShimX(), fromShim.getMyShimY());

            // draw the line
            graphics2D.setColor(streamWaypoint.getColor());
            graphics2D.setStroke(new BasicStroke(3));

            // graphics2D.drawLine(0, 0, (int) (toPoint.getX() - fromPoint.getX()), (int) (toPoint.getY() - fromPoint.getY()));
            int posShimX = (int) Math.round(((toPoint.getX() - fromPoint.getX()) / 2) + 0.5 * streamWaypoint.shim * toPoint.distance(fromPoint));
            int posShimY = (int) Math.round(((toPoint.getY() - fromPoint.getY()) / 2) - 0.5 * streamWaypoint.shim * toPoint.distance(fromPoint));
            QuadCurve2D link = new QuadCurve2D.Double(0, 0, posShimX, posShimY, (int) (toPoint.getX() - fromPoint.getX()), (int) (toPoint.getY() - fromPoint.getY()));
            graphics2D.draw(link);
            graphics2D.fillOval((int) (toPoint.getX() - fromPoint.getX()) - 5, (int) (toPoint.getY() - fromPoint.getY()) - 5, 10, 10);

            /*
            if (paintLinkLegend) {
                // generate StreamLink description
                // get size of node description
                Font origFont = graphics2D.getFont();
                Font smallFont = new Font(origFont.getFontName(), origFont.getStyle(), origFont.getSize() - 3);
                graphics2D.setFont(smallFont);
                Font f = graphics2D.getFont();
                FontRenderContext frc = graphics2D.getFontRenderContext();
                String slDescription = "" + streamWaypoint.getNetworkLink().fromInterface.getIpAddress() + " --> " + streamWaypoint.getNetworkLink().toInterface.getIpAddress() + ", " + Converters.bandwidthToString(streamWaypoint.getMediaApplication().getMediaMaxBandwidth());
                Rectangle2D tRect = f.getStringBounds(slDescription, frc);
                LineMetrics lMetrics = f.getLineMetrics(slDescription, frc);
                int boxWidth = (int) Math.round(tRect.getWidth() + 10);
                int boxHeight = (int) Math.round(tRect.getHeight() + 5);
                // draw it
                // TODO - it would be great to find some better (more precise) position, as the shim{X,Y} may be rather way off the link
                final float posCoeff = 0.6f;
                graphics2D.setColor(new Color(streamWaypoint.getColor().getRed(), streamWaypoint.getColor().getGreen(), streamWaypoint.getColor().getBlue(), 50));
                graphics2D.fillRoundRect((int) Math.round(posCoeff * posShimX) + (int) Math.round(-0.5 * boxWidth), (int) Math.round(posCoeff * posShimY) + (int) Math.round(-0.5 * boxHeight), boxWidth, boxHeight, 20, 20);
                graphics2D.setColor(new Color(0, 0, 0, 50));
                graphics2D.drawString(slDescription, (int) Math.round(posCoeff * posShimX) + Math.round(-0.5 * tRect.getWidth()), (int) Math.round(posCoeff * posShimY) + Math.round(-0.5 * tRect.getHeight() + lMetrics.getAscent()));

                // restore original font
                graphics2D.setFont(origFont);
            }
            */

            // restore the original position
            graphics2D.translate(-fromShim.getMyShimX(), -fromShim.getMyShimY());
            return false;
        }

            @Override
            public boolean paintWaypoint(Graphics2D graphics2D, JXMapViewer jxMapViewer, Waypoint waypoint) {
                if (waypoint instanceof UniverseWaypoint) {
                    return paintUniverseWaypoint(graphics2D, jxMapViewer, (UniverseWaypoint)waypoint);
                } else if (waypoint instanceof StreamWaypoint) {
                    return paintStreamWaypoint(graphics2D, jxMapViewer, (StreamWaypoint)waypoint);
                }
                return false;
            }
        };
    private final UniverseNetworkPainter painter = new UniverseNetworkPainter(nodeRenderer);
    
    class ColorGenerator {
        Color getColor(int port) {
            return new Color((31 * port) % 256, (47 * port) % 256, (71 * port) % 256, 150);
        }
    }
    private final ColorGenerator colorGenerator = new ColorGenerator();
    

    public UniverseVisualizer(JSONObject localNode) {
        try {
            localNodeUuid = getNodeUuid(localNode);
            updateNode(localNode);
            startPosition = getNodeLocation(localNode).toGeoPosition();
        } catch (JSONException ex) {
            localNodeUuid = null;
            Logger.getLogger(this.getClass()).error("Failed to initialize map Visualizer: " + ex.toString(), ex);
        }
        
        // this.networkTopologyGraph = g;
        checkAndAdjustShims();
        
        initSwingGui();

        OpenStreetsTileFactory tf = new OpenStreetsTileFactory();
        tf.setThreadPoolSize(4);
        setupFactory(tf);

        // startPosition will never be null

        this.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent event) {
                onMouseClicked(event.getPoint());
            }
        });
        this.addMouseWheelListener(new MouseWheelListener()
        {
            public void mouseWheelMoved(MouseWheelEvent event) {
                if(event.getWheelRotation() > 0){
                    UniverseVisualizer.this.zoomSlider.setValue(zoomSlider.getValue() +1);
                }else{
                    UniverseVisualizer.this.zoomSlider.setValue(zoomSlider.getValue() -1);
                } }});

        //this.setRestrictOutsidePanning(true);
        //this.setPaintBorderInsets(true);
        //this.setHorizontalWrapped(true);
        this.setOverlayPainter(painter);
    }
    private void initSwingGui(){
        this.setLayout(new GridBagLayout());

        zoomInButton = new javax.swing.JButton();
        zoomOutButton = new javax.swing.JButton();
        zoomSlider = new javax.swing.JSlider();
        JPanel jPanel = new JPanel();
        jPanel.setOpaque(false);
        jPanel.setLayout(new java.awt.GridBagLayout());

        zoomInButton.setText("-");
        zoomInButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        zoomInButton.setMaximumSize(new java.awt.Dimension(20, 20));
        zoomInButton.setMinimumSize(new java.awt.Dimension(20, 20));
        zoomInButton.setOpaque(false);
        zoomInButton.setPreferredSize(new java.awt.Dimension(20, 20));
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UniverseVisualizer.this.zoomSlider.setValue(zoomSlider.getValue() +1);
            }});

        zoomOutButton.setText("+");
        zoomOutButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        zoomOutButton.setMaximumSize(new java.awt.Dimension(20, 20));
        zoomOutButton.setMinimumSize(new java.awt.Dimension(20, 20));
        zoomOutButton.setOpaque(false);
        zoomOutButton.setPreferredSize(new java.awt.Dimension(20, 20));
        zoomOutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UniverseVisualizer.this.zoomSlider.setValue(zoomSlider.getValue() -1);

        }});

        zoomSlider.setOpaque(false);
        zoomSlider.setForeground(Color.black);
        zoomSlider.setMajorTickSpacing(1);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setMinimumSize(new java.awt.Dimension(35, 100));
        zoomSlider.setPreferredSize(new java.awt.Dimension(35, 230));
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                UniverseVisualizer.this.setZoom(zoomSlider.getValue());
        }});

        GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = java.awt.GridBagConstraints.VERTICAL;
        c.anchor = java.awt.GridBagConstraints.PAGE_START;
        jPanel.add(zoomSlider, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;        
        c.fill = java.awt.GridBagConstraints.NONE;
        c.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        c.weighty = 1.0;
        jPanel.add(zoomOutButton, c);

        c.gridx = 1;
        c.gridy = 1;
        c.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        c.weightx = 1.0;
        c.weighty = 1.0;
        jPanel.add(zoomInButton, c);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LAST_LINE_START;
        c.insets = new Insets(5,5,5,5);
        jPanel.setPreferredSize(new Dimension(40, 250));
        this.add(jPanel, c);

        c.anchor = GridBagConstraints.FIRST_LINE_START;
        this.selectedNodeWaypointInfoPanel = new NodeWaipointInfoPanel(this);
        selectedNodeWaypointInfoPanel.setPreferredSize(new Dimension(200, 200));
        this.add(selectedNodeWaypointInfoPanel, c);

    }

/*
    public void setMyStreamWaypoints( List<StreamWaypoint> myStreamWaypoints){
        for(StreamWaypoint wp : myStreamWaypoints){
            this.painter.getWaypoints().add(wp);
        }
    }
*/
    private void onMouseClicked(Point point) {
        lastClickedWaypoint = null;
        for(Waypoint p : this.painter.getWaypoints()){
            if (!(p instanceof UniverseWaypoint)) continue;
            
            UniverseWaypoint uwpt = (UniverseWaypoint)p;
            if (uwpt.wasClickedOnMap((int)point.getX(), (int)point.getY())) {
                lastClickedWaypoint = uwpt;
                this.repaint();
                this.selectedNodeWaypointInfoPanel.repaint();
                return;
            }
        }
    }

    public UniverseWaypoint getLastClickedWaypoint() {
        return lastClickedWaypoint;
    }

    public void setupFactory(TileFactory newFactory){
        
        if(newFactory != null && this.currentFactory != newFactory){
            this.currentFactory = newFactory;

            zoomSlider.setMaximum(this.currentFactory.getInfo().getMaximumZoomLevel());
            zoomSlider.setMinimum(this.currentFactory.getInfo().getMinimumZoomLevel());
            this.setTileFactory(this.currentFactory);
            this.zoomSlider.setValue(this.currentFactory.getInfo().getMaximumZoomLevel() -2);
            this.setAddressLocation(this.startPosition);
            this.repaint();
        }
    }

/*
    public final void addPlanElement(PlanElement el) {
        throw new UnsupportedOperationException("Plan elements not supported yet!");
    }
    
    public final void removePlanElement(PlanElement el) {
        throw new UnsupportedOperationException("Plan elements not supported yet!");
    }
*/  
    
    /**
     * adjust shims of NetworkNodes
     */ 
    private void checkAndAdjustShims() {
        System.out.println("Adjusting shims");
        
        HashMap<GeoLocation, HashSet<String> > identicalPositions = new HashMap<>();
        for (Map.Entry<String, JSONObject> entry : nodes.entrySet()) {
            GeoLocation pt;
            try {
                pt = getNodeLocation(entry.getValue());
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("Failed to get location for node " + getNodeName(entry.getValue()) + ": " + ex.toString(), ex);
                continue;
            }
            
            if (identicalPositions.get(pt) == null) identicalPositions.put(pt, new HashSet<String>());
            identicalPositions.get(pt).add(entry.getKey());
        }

        System.out.println("There are " + identicalPositions.keySet().size() + " distinct locations:");
        System.out.println("  " + identicalPositions);
        
        for (HashSet<String> nodeUuids : identicalPositions.values()) {
            if (nodeUuids.size() <= 1) continue;

            ShimGenerator shimGenerator = new CircleShimGenerator(35, nodes.size());
            for (String nodeUuid : nodeUuids) {
                nodeShims.put(nodeUuid, shimGenerator.generateShim());
            }
        }
        
        
    }
    
    public void goHome() {
        
    }

    private void updateNodes(JSONObject nodes) {
        
        if (nodes == null) return;
        
        Iterator<Object> nodeKeys = nodes.keys();
        HashSet<String> deadNodes = new HashSet<>(this.nodes.keySet());
        
        while (nodeKeys.hasNext()){
            Object primaryKey = nodeKeys.next();
            
            if (!(primaryKey instanceof String)) {
                Logger.getLogger(this.getClass()).error("Got key which is not an uuid string during parsing the received network");
                continue;
            }
            
            String nodeUuid = (String)primaryKey;
            try {
                updateNode(nodes.getJSONObject(nodeUuid));
                deadNodes.remove(nodeUuid);
            } catch (JSONException ex) {
                Logger.getLogger(this.getClass()).error("Key " + nodeUuid + " does not point to JSONObject representing the networkNode: " + ex.toString(), ex);
                continue;
            }
            
            
        }
        for (String deadNode : deadNodes) {
            removeNode(deadNode);
        }
        
        checkAndAdjustShims();    
    }

    public void updatePlan(JSONObject plan) {
        if (plan == null) return;
        if (plan.length() == 0) return;
        
        painter.getWaypoints().clear();
        streamWaypointMap.clear();
        
        // application
        for (Map.Entry<String, JSONObject> node : nodes.entrySet()) {
            String sourceNodeUuid = node.getKey();

            HashMap<String, List<StreamWaypoint>> targetNodeToWaypointMap = streamWaypointMap.get(sourceNodeUuid);
            
            for (String appUuid : appsByNode.get(sourceNodeUuid)) {
                try {
                    JSONObject el = plan.getJSONObject(appUuid);

                    JSONArray targets = el.getJSONArray("targets");            
                    for (int i = 0; i < targets.length(); ++i) {
                        JSONObject target = targets.getJSONObject(i);

                        String targetIfaceUuid = ifaceByAddress.get(target.getString("ip"));
                        String targetNodeUuid = nodesByInterfaces.get(targetIfaceUuid);

                        int portNumber = Integer.parseInt(target.getString("port"));

                        StreamWaypoint waypoint = new StreamWaypoint(sourceNodeUuid, targetNodeUuid, colorGenerator.getColor(portNumber), target);

                        if (targetNodeToWaypointMap.get(targetNodeUuid) == null) {
                            targetNodeToWaypointMap.put(targetNodeUuid, new LinkedList<StreamWaypoint>());
                        }
                        targetNodeToWaypointMap.get(targetNodeUuid).add(waypoint);
                    }
                } catch (JSONException | NullPointerException ex) {
                    Logger.getLogger(this.getClass()).error("Found mallformed component: " + ex.toString());
                    continue;
                }
            }

            for (List<StreamWaypoint> list : targetNodeToWaypointMap.values()) {
                double size = list.size();
                double increment = 0.5 / size;

                double sum = 0;

                for (StreamWaypoint waypoint : list) {
                    sum += increment;
                    waypoint.shim = sum;
                    painter.getWaypoints().add(waypoint);
                }
            }
        }
    }

    public void update(JSONObject nodes, JSONObject plan) {
        
        updateNodes(nodes);
        updatePlan(plan);
        repaint();
    }
}
