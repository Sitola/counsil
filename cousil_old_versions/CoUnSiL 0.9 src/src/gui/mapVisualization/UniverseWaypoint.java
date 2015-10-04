package gui.mapVisualization;

import java.awt.Rectangle;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.Waypoint;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: May 5, 2008
 * Time: 3:30:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UniverseWaypoint extends Waypoint {
    final String nodeUuid;
    GeoPosition nodePosition;
    Rectangle areaOnMap = new Rectangle(0,0,15,8);



    public void setMapOriginPosition(int x, int y) {
        this.areaOnMap.setLocation(x + this.getShimX(), y + this.getShimY());
    }


    //because the size of waypoint on map can be various

    public void setSize(int width, int height) {
        this.areaOnMap.setSize( width, height);
    }

    public boolean wasClickedOnMap(int x, int y){
        // this.areaOnMap.contains does not work, since the upper-left corner of it lies in the middle of the bubble
        return
                areaOnMap.x - (areaOnMap.width/2) < x &&
                areaOnMap.x + (areaOnMap.width/2) > x &&
                areaOnMap.y - (areaOnMap.height/2) < y &&
                areaOnMap.y + (areaOnMap.height/2) > y;
    }

    public UniverseWaypoint(String nodeUuid, GeoPosition pos) {
        this.nodeUuid = nodeUuid;
        this.nodePosition = pos;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    @SuppressWarnings({"DesignForExtension"})
    public void setPosition(GeoPosition geoPosition) {
    }

    public GeoPosition getPosition() {
        return nodePosition;
    }

    public int getShimX() {
        return 0; // TODO: Shim  this.networkNode.getShim().getMyShimX();
    }

    public int getShimY() {
        return 0; // TODO: Shim  this.networkNode.getShim().getMyShimY();
    }
}
