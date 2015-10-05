package gui.mapVisualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import javax.swing.JPanel;
import networkRepresentation.EndpointNetworkNode;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: May 6, 2008
 * Time: 12:18:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeWaipointInfoPanel extends JPanel {
    private final UniverseVisualizer visualizer;

    public NodeWaipointInfoPanel(UniverseVisualizer visualizer) {
        this.visualizer = visualizer;
        this.setLayout(null);
    }
    
    public void paint(Graphics graphics) {
        if (visualizer.getLastClickedWaypoint() == null) return;
    
        if (visualizer.getLastClickedWaypoint() == null) return;
        String selectedNodeUuid = visualizer.getLastClickedWaypoint().getNodeUuid();
        JSONObject editedNode = visualizer.getNode(selectedNodeUuid);

        Graphics2D g = (Graphics2D) graphics;
        
        g.setPaint(new Color(0, 0, 0, 50));
        g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 15, 15);
        //g.fillRoundRect(50, 10, 182 , 30, 10, 10);
        g.setPaint(Color.WHITE);
        g.setFont(new Font("Dialog", Font.BOLD, 14));
        //g.drawString(this.nodeWaipoint.getNetworkNode().getNodeName(), 13,23);
        String nodeName = editedNode.optString(EndpointNetworkNode.ConfigKeyNodeName);
        
        if(nodeName.length() > 20){
            g.drawString(nodeName.subSequence(0,21) +"...", 16,23);
        }else{
            g.drawString(nodeName, 16,23);
        }
        
        String nodeSite = editedNode.optString(EndpointNetworkNode.ConfigKeySiteName);
        if(nodeSite.length() > 19){
            g.drawString(nodeSite.subSequence(0,20) +"...", 22,48);
        }else{
            g.drawString(nodeSite, 22,48);
        }
        
        int y = 73;
        for (Map.Entry<String, JSONObject> appEntry : this.visualizer.getNodeApplications(selectedNodeUuid).entrySet()) {
            String appDesc = appEntry.getValue().optString("name", appEntry.getValue().optString("uuid", ""));
            if (appDesc.isEmpty()) {
                Logger.getLogger(this.getClass()).error("Unable to read app identification");
                continue;
            }
            
            if(appDesc.length() > 20){
                g.drawString(appDesc.subSequence(0,21) +"...", 16,y);
            }else{
                g.drawString(appDesc, 16,y);
            }
            
            y += 20;
            if (y > 200) break;
        }
        
    }
}
