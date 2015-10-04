package gui.mapVisualization;


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import org.codehaus.jettison.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 23, 2008
 * Time: 7:17:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class MapVisualizerPanel extends JPanel {
    private UniverseVisualizer map;
    private BottomPanel bottomPanel;

    public MapVisualizerPanel(JSONObject localNode) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        // this.map = new UniverseVisualizer(startPosition, SampleTopologies.getGLIFDemoTopology().getNetworkTopologyGraph());
        this.map = new UniverseVisualizer(localNode);// startPosition, new PartiallyKnownNetworkTopology().getNetworkTopologyGraph());
        map.setPreferredSize(new Dimension(1100, 600));
        this.add(map, c);

        c.gridy = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        this.bottomPanel = new BottomPanel(true);
        this.add(this.bottomPanel, c);

        //map.setFactory(UniverseVisualizer.TileProviderEnum.OpenStreetMaps);
    }
    
    public BottomPanel getBottomPanel() {
        return bottomPanel;
    }
    
    public UniverseVisualizer getMapVisualizer() {
        return map;
    }
}
