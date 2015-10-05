package gui.mapVisualization;

import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 22, 2008
 * Time: 2:36:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpenStreetsTileFactory extends DefaultTileFactory {
    private static final int max = 17;

    public OpenStreetsTileFactory() {
        super(new TileFactoryInfo(1, max - 2, max, 256, true, true, // tile size is 256 and x/y orientation is normal
                "http://tile.openstreetmap.org",//5/15/10.png",
                "x", "y", "z") {

            public String getTileUrl(int x, int y, int zoom) {
                zoom = max - zoom;
                String url = this.baseURL + "/" + zoom + "/" + x + "/" + y + ".png";
                return url;
            }});
    }

    public UniverseVisualizer.TileProviderEnum getEnumDescription(){
        return UniverseVisualizer.TileProviderEnum.OpenStreetMaps;
    }

}
