package utils;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import agc.MatchMaker;
import agc.ApplicationGroupController;
import monitoring.NetworkMonitor;
import monitoring.NetworkNodeMonitor;
import monitoring.AGCMonitor;
import myJXTA.MyJXTAConnector;
import appControllers.ControllerImpl;

/**
 * Sets up default logging for CoUniverse
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 11.10.2007
 * Time: 19:59:24
 */
public class MyLogger {

    public static void setup() {

        Logger logger;

        // Root logger setup
        logger = Logger.getRootLogger();
        logger.removeAllAppenders();
        //logger.setAdditivity(false);        
        PatternLayout layout = new PatternLayout("%-6r: %-5p [%C{1}]: %m%n");
        //PatternLayout layout = new PatternLayout("%-5p [%C{1}]: %m%n");
        ConsoleAppender appender = new ConsoleAppender(layout, "system.out");
        logger.addAppender(appender);
        
        // MatchMaker logger setup
        logger = Logger.getLogger(MatchMaker.class);
        logger.setLevel(Level.INFO);
        
        // ApplicationGroupController logger setup
        logger = Logger.getLogger(ApplicationGroupController.class);
        logger.setLevel(Level.DEBUG);

        // NetworkMonitor logger setup
        logger = Logger.getLogger(NetworkMonitor.class);
        logger.setLevel(Level.DEBUG);

        // NetworkNodeMonitor logger setup
        logger = Logger.getLogger(NetworkNodeMonitor.class);
        logger.setLevel(Level.INFO);

        // AGCMonitor logger setup
        logger = Logger.getLogger(AGCMonitor.class);
        logger.setLevel(Level.DEBUG);      

        // MatchMaker logger setup
        logger = Logger.getLogger(ControllerImpl.class);
        logger.setLevel(Level.INFO);

         // MediaApplicationMonitor logger setup
        logger = Logger.getLogger(ApplicationProxyJNI.class);
        logger.setLevel(Level.DEBUG);

        // NetworkRepresentation logger setup 
        logger = Logger.getLogger("NetworkRepresentation");
        logger.setLevel(Level.INFO);

        // myGUI logger setup 
        logger = Logger.getLogger("myGUI");
        logger.setLevel(Level.INFO);

        // myJXTA logger setup 
        logger = Logger.getLogger("myJXTA");
        logger.setLevel(Level.INFO);

        // utils logger setup 
        logger = Logger.getLogger("utils");
        logger.setLevel(Level.INFO);

        // utils logger setup
        logger = Logger.getLogger(ProxyNodeConnection.class);
        logger.setLevel(Level.INFO);

        // NetworkMonitor logger setup
        logger = Logger.getLogger(NetworkMonitor.class);
        logger.setLevel(Level.INFO);

        // NetworkMonitor logger setup
        logger = Logger.getLogger(MyJXTAConnector.class);
        logger.setLevel(Level.DEBUG);

        // UniversePeer logger setup - this needs to be referenced as String as it is not part of any package
        logger = Logger.getLogger("UniversePeer");
        logger.setLevel(Level.DEBUG);

    }

}
