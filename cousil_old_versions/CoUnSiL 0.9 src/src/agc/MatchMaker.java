package agc;

import networkRepresentation.LogicalNetworkLink;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;


/**
 * MatchMakerUtils class is responsible for matching of available network topology with available MediaApplications in the network.
 * It uses constranit-based approach for scheduling MediaStreams onto specific links.
 * <p/>
 * BEWARE OF OGRES! This class is not thread safe and even worse, it requires that results are obtained while the
 * network topology hasn't changed in meantime! This will improve later as this is just a fast prototype.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 17:55:44
 */
public abstract class MatchMaker {

    static Logger logger = Logger.getLogger(MatchMaker.class);

    /**
     * Do the actual matching and create the master plan.
     * <p/>
     *
     * @return true if match was found, otherwise false
     */
    public abstract boolean doMatch();

    /**
     * Get result of scheduling in form of plan.
     * <p/>
     *
     * @return plan expressed as array of PlanElements
     */
    public abstract PlanElementBean.Plan getPlan();
    
    public abstract List<LogicalNetworkLink> getUsedLinks();

}
