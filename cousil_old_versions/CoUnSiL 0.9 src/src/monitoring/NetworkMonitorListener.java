package monitoring;

import networkRepresentation.LogicalNetworkLink;

/**
 * Event listener interface for NetworkMonitor
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 18.9.2007
 * Time: 11:51:26
 */
public interface NetworkMonitorListener {

    // this is to be called when network link is found non-functional
    public void onNetworkLinkLost(LogicalNetworkLink networkLink);

    // this is to be called when network link is found functional again
    public void onNetworkLinkReestablished(LogicalNetworkLink networkLink);

    // this is to be called when network links is found to be flapping
    public void onNetworkLinkFlap(LogicalNetworkLink networkLink);
    
    // this is to be called when the link latency differs significantly
    public void onNetworkLinkLatencyChange(LogicalNetworkLink networkLink);

    /*
    interface should include following two methods according simon suchomel's uml
    i just don't know the difference beetween Down and Lost
    onNetworkLinkUp is implemented in GuiController but it is commented due to UniversePeer
     */
    //public void onNetworkLinkDown(NetworkLink networkLink);
    //public void onNetworkLinkUp(NetworkLink networkLink);





}
