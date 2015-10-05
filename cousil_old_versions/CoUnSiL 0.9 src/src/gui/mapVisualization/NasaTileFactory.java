package gui.mapVisualization;

import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.jdesktop.swingx.mapviewer.wms.WMSTileFactory;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 22, 2008
 * Time: 2:36:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class NasaTileFactory extends WMSTileFactory {  //WMSTileFactory extends DefautlTileFactory


    public NasaTileFactory() {
        super(new WMSService("http://wms.jpl.nasa.gov/wms.cgi?","BMNG"));
    }

    public UniverseVisualizer.TileProviderEnum getEnumDescription(){
        return null; //MyMapVisualizer.TileProviderEnum.NasaMaps;
    }
}
