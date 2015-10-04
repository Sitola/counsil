package mediaAppFactory;

import appControllers.ControllerImpl;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class ApplicationFactory {
    
    public interface Generator {
        MediaApplication newApplication();
        Class getAppClass();
        ControllerImpl createController(MediaApplication app) throws IllegalArgumentException ;
    }
    
    private class PortAssigner {
        
        private static final int FIRST_PORT = 5020;
        private static final int PORT_STEP = 4;
        
        private LinkedList<String> freePorts = new LinkedList<String>();
        private int highestAssignedPort;
        
        private HashMap<MediaApplication, String> appToPortMap = new HashMap<MediaApplication, String>();
        
        public PortAssigner() {
            highestAssignedPort = FIRST_PORT;
        }
        
        public synchronized String getPort(MediaApplication application) {
            String port = appToPortMap.get(application);
             
            if (port != null) return port;
                
            if (! freePorts.isEmpty()) {
                port = freePorts.pop();
            }
            
            if (port == null) {
                port = "" + highestAssignedPort;
                highestAssignedPort += PORT_STEP;
            }

            appToPortMap.put(application, port);
            
            return port;
        }
        
        public synchronized void freePort(String port) {
            if (freePorts.contains(port)) throw new IllegalArgumentException("The port was already freed and is pending for new assignmnet");
            if (! appToPortMap.values().contains(port)) throw new IllegalArgumentException("The port is not mapped to any application!");
            
            freePorts.offer(port);
        }
    }
    
    private final HashMap<String, Generator> generatorsByNickname;
    private final HashMap<String, Generator> generatorsByClassName;
    private final PortAssigner portAssigner = new PortAssigner();
    
    private static ApplicationFactory instance = null;
    private static int nextAppId = 0;

    private ApplicationFactory() {
        generatorsByNickname = new HashMap<>();
        generatorsByClassName = new HashMap<>();
    }
    
    public static synchronized ApplicationFactory getInstance() {
        if (instance == null) {
            instance = new ApplicationFactory();
        }
        return instance;
    }
    
    /**
     * Register new application type that can be created by this factory
     * @param name application type name
     * @param generator generator used to create that type of application
     * @throws IllegalArgumentException 
     */
    public void registerGenerator(String name, Generator generator) throws IllegalArgumentException {
        try {
            synchronized (this) {
                if (generatorsByNickname.containsKey(name)) throw new IllegalArgumentException("Generator name " + name + " is already registered");
            
                String appClassName = generator.getAppClass().getCanonicalName();
                if (generatorsByClassName.containsKey(appClassName)) throw new IllegalArgumentException("Generator for application class " + appClassName + " is already registered");
            
                generatorsByNickname.put(name, generator);
                generatorsByClassName.put(appClassName, generator);
            }
        } catch (NullPointerException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Unable to register generator due to some of it's functions returning null: " + ex.toString(), ex);
        }
    }
    
    public Set<String> getGeneratorNicknames() {
        return generatorsByNickname.keySet();
    }
    
    public static MediaApplication newApplication(String name) throws IllegalArgumentException {
        Generator g = getInstance().generatorsByNickname.get(name);
        if (g == null) {
            throw new IllegalArgumentException("No such generator (\"" + name + "\") registered");
        }
        MediaApplication app = g.newApplication();
        app.setPreferredReceivingPort(getInstance().portAssigner.getPort(app));
        app.setLocalNodeSerialId(nextAppId++);
        return app;
    }
    public static ControllerImpl newController(MediaApplication app) {
        String appClassName = app.getClass().getCanonicalName();
        Generator g = getInstance().generatorsByClassName.get(appClassName);
        if (g == null) return null;
        ControllerImpl ctl = g.createController(app);
        System.out.println("Controller for " + app + " created!");
        
        return ctl;
    }
    public static String getGeneratorNickname(MediaApplication app){
        String appClassName = app.getClass().getCanonicalName();
        Generator g = getInstance().generatorsByClassName.get(appClassName);
        if (g == null) return null;
        
        for (Map.Entry<String, Generator> ge : getInstance().generatorsByNickname.entrySet()){
            if (g.equals(ge.getValue())) return ge.getKey();
        }
        return null;
    }

    // TODO: JavaDoc
    public static MediaApplication filterByUuidOrName(String identifier, Collection<MediaApplication> apps) {
        UUID unique = null;
        try {
            unique = UUID.fromString(identifier);
        } catch (IllegalArgumentException ex) {
            // not an uuid
        }

        for (MediaApplication app : apps) {
            if (((unique == null) && (app.getApplicationName().equals(identifier))) || ((unique != null) && (app.getUuid().equals(unique.toString())))) {
                return app;
            }
        }
        return null;
    }
}
