package monitoring;

import networkRepresentation.EndpointNodeInterface;

/**
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Nov 3, 2008
 * Time: 2:27:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NetworkNodeMonitorListener {

     // this is to be called when one Interface of node goes down
    public void onNodeInterfaceDown(EndpointNodeInterface nodeInterface);

     // this is to be called when one Interface of node goes up
    public void onNodeInterfaceUp(EndpointNodeInterface nodeInterface);

}
