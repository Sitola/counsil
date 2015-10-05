package gui.mapVisualization;

import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 22, 2008
 * Time: 2:28:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class GoogleTileFactory extends DefaultTileFactory {
    public static final String VERSION = "2.92";
    public static final int minimumZoomLevel = 7;
    public static final int maximumZoomLevel = 15;
    private static final int totalMapZoom = 17;
    private static final int tileSize = 256;
    private static final boolean xr2l = true;
    private static final boolean yt2b = true;
    private static final String baseURL = "http://mt2.google.com/mt?n=404&v=w" + VERSION;
    private static final String xparam = "x";
    private static final String yparam = "y";
    private static final String zparam = "zoom";

    public GoogleTileFactory() {
        super(new TileFactoryInfo(
                minimumZoomLevel,
                maximumZoomLevel,
                totalMapZoom,
                tileSize,// tile size is 256 and x/y orientation is normal
                xr2l,
                yt2b,
                baseURL,
                xparam,
                yparam,
                zparam));
    }

    public UniverseVisualizer.TileProviderEnum getEnumDescription(){
        return null; //MyMapVisualizer.TileProviderEnum.GoogleMaps;
    }
}
