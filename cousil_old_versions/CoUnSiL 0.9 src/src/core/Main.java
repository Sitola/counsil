package core;

import gui.SimpleSwitcher;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import networkRepresentation.EndpointNetworkNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import utils.ConfigUtils;

/**
 * @author Lukáš Ručka, 359687
 */
public class Main {
    private enum Mode { ModeUnselected, ModeRun, ModeHelp, ModeGenDefault, ModeRemoteSwitcher };
    private static final String CmdDefault = "--default";
    private static final String CmdConfig = "--config";
    private static final String CmdHelp = "--help";
    private static final String OptPatchConfig = "--patch-config";
    private static final String CmdRemoteSwitcher = "--remote-gui";

    public static final String ConfigKeyLogLevel = "logLevel";
    
    @Deprecated
    // Used during tests
    public static final boolean USE_DUMMY_APPLICATIONS = true;

    private static final ControlPeer universePeer = new ControlPeer();

    private static class Options {
        public String[] args = null;
        public String configFilePath = ConfigUtils.getConfigPath();
        public Mode mode = Mode.ModeUnselected;
        public boolean startCli = false;
        public JSONObject configPatch = null;
        public String remote = null;
    }

    private static Options parseArgs(String[] args) {
        Options retval = new Options();
        retval.args = args;

        for (int i = 0; i < args.length; ++i){
            if (args[i].equals(CmdHelp)) {
                assert retval.mode == Mode.ModeUnselected;
                retval.mode = Mode.ModeHelp;
            } else if (args[i].equals(CmdDefault)) {
                assert retval.mode == Mode.ModeUnselected;
                retval.mode = Mode.ModeGenDefault;

                if ((i+1 < args.length) && (!args[i+1].startsWith("-"))) {
                    retval.configFilePath = args[i+1];
                    ++i;
                }
            } else if (args[i].equals(CmdConfig)) {
                if (!(i+1 < args.length)) {
                    System.err.println("Config file name expected after " + CmdConfig + "!");
                    System.exit(1);
                }
                retval.configFilePath = args[i+1];
                ++i;
            } else if (args[i].equals(CmdRemoteSwitcher)) {
                if (!(i+1 < args.length)) {
                    System.err.println("Ip:port expected after" + CmdRemoteSwitcher + "!");
                    System.exit(1);
                }
                retval.remote = args[i+1];
                retval.mode = Mode.ModeRemoteSwitcher;
                ++i;
            } else if (args[i].equals(OptPatchConfig)) {
                if (!(i+1 < args.length)) {
                    System.err.println("JSONObject expected after option");
                    System.exit(1);
                }
                try {
                    retval.configPatch = new JSONObject(args[i+1]);
                } catch (JSONException ex) {
                    System.err.println("Argurment following " + OptPatchConfig + " is not a valid json object representation!");
                    System.exit(1);
                }
                ++i;
            }

        }

        if (retval.mode == Mode.ModeUnselected)
            retval.mode = Mode.ModeRun;

        return retval;
    }

    private static void imprintDefaultLogLevel(JSONObject conf) {
        try {
            conf.put(ConfigKeyLogLevel, "WARN");
        } catch (JSONException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
            System.exit(1);
        }
    }

    private static void commandDefaultCfg(Options opt) {
        try {
            JSONObject fullConfig = new JSONObject();
            JSONObject ennDefaultConfig = new EndpointNetworkNode().defaultConfig();
            ConfigUtils.mergeConfig(fullConfig, ennDefaultConfig);

            JSONObject p2pDefaultConfig = P2pConfigWorker.getInstance().defaultConfig();
            ConfigUtils.mergeConfig(fullConfig, p2pDefaultConfig);

            JSONObject uiDefaultConfig = UserInterfaceManager.getInstance().defaultConfig();
            ConfigUtils.mergeConfig(fullConfig, uiDefaultConfig);

            imprintDefaultLogLevel(fullConfig);

            if (ConfigUtils.saveConfig(opt.configFilePath, fullConfig)) {
                System.out.println("Written config into \"" + opt.configFilePath + "\".");
                return;
            }
        } catch (JSONException ex) {
            Logger.getLogger(Main.class).log(Level.FATAL, "Failed to write default config: " + ex.toString());
        }
    }

    private static void setupLogger(JSONObject cfg){
        Level level = Level.WARN;
        String txtLevel = null;

        try {
            txtLevel = cfg.getString(ConfigKeyLogLevel);
            Level.toLevel(txtLevel);
        } catch (JSONException ex) {
            // use default
        } catch (IllegalArgumentException ex) {
            System.err.println("Unrecognized log level " + txtLevel + ", possible values include: "
                + "OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL");
            System.exit(2);
        }

        // todo - find way to shut jxta up
        Properties props = new Properties();
        props.put("log4j.rootLogger=", "OFF");
        props.put("log4j.logger.net.jxta=", "OFF");
        props.put("log4j.logger.net.jxta.logging.Logging=", "OFF");
        PropertyConfigurator.configure(props);
    }

    private static void commandHelp(Options opt) {
        System.out.println("Usage: java -jar CoUniverse.jar [options]");
        System.out.println("Available options:");
        System.out.println("\t"+CmdHelp+"\tDisplay this help");
        System.out.println("\t"+CmdDefault+" [filename]\tGenerate default configuration and write it to <filename>, which defaults to nodeConfig.json");
        System.out.println("\t"+CmdConfig+" <filename>\tUse config file <filename>");
        System.out.println("\t"+CmdRemoteSwitcher+" <host>:<port>\tRemote management for couniverse running at host:port");
        System.out.println("\t"+OptPatchConfig+"\t <json object> patch config object loaded from config file with given json object.");
        
        System.exit(1);
    }
    
    private static void configureAndInit(Options opt) {
        JSONObject cfg = null;

        try {
            Logger.getLogger(Main.class).info("Loading configuration from "+opt.configFilePath);
            cfg = ConfigUtils.loadConfig(opt.configFilePath);
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
            System.err.println("Path \"" + opt.configFilePath + "\" was not recognized as valid URL and thus interpreted as file path. However, CoUniverse was not able to load such file (" + ex.getMessage() + ").");

            if (opt.configFilePath.equals(ConfigUtils.getConfigPath())) {
                System.err.println("You can generate the default config file (manual edit required) by running CoUniverse with " + CmdDefault + " solely.");
            }

            System.exit(2);
        }

        if (opt.configPatch != null) {
            try {
                ConfigUtils.mergeConfig(cfg, opt.configPatch);
            } catch (IllegalArgumentException ex) {
                System.out.println("Unable to merge config patch onto existing config.");
            }
        }
        
        /** @todo dirty hack, stupid jxta keesp starting too soon and failing **/
        P2pConfigWorker.getInstance().disable();

        ConfigUtils.ConfigManager.registerManager(".localNode", universePeer.getLocalNode());
        ConfigUtils.ConfigManager.registerManager(".p2p", P2pConfigWorker.getInstance());
        ConfigUtils.ConfigManager.registerManager(".ui", UserInterfaceManager.getInstance());
        ConfigUtils.ConfigManager.registerManager(".templates", universePeer.getLocalNode().getApplicationTemplates());
        ConfigUtils.ConfigManager.registerManager(".applications", universePeer.getLocalNode().getApplicationManager());

        if (!universePeer.getLocalNode().loadConfig(cfg)) {
            Logger.getLogger(Main.class).log(Level.FATAL, "Unable to replace (initial) node configuration, quitting.");
            System.exit(3);
        }
        
        try {
            if (!universePeer.getLocalNode().getApplicationTemplates().loadConfig(cfg)) {
                Logger.getLogger(Main.class).log(Level.ERROR, "Unable to load application template definitions");            
            }
        } catch (JSONException | IllegalArgumentException ex) {
            Logger.getLogger(Main.class).log(Level.ERROR, "Unable to load application template definitions: " + ex.toString());            
        }
        
        if (!UserInterfaceManager.getInstance().loadConfig(cfg)) {
            Logger.getLogger(Main.class).log(Level.FATAL, "Unable to replace (initial) user interface subsystem configuration, quitting.");
            System.exit(3);
        }
        if (!P2pConfigWorker.getInstance().loadConfig(cfg)) {
            Logger.getLogger(Main.class).log(Level.FATAL, "Unable to replace (initial) P2P subsystem configuration, quitting.");
            System.exit(3);
        }
        setupLogger(cfg);
    }

    private static void commandRun(Options opt) {
        // this has to be done first

        // load application template generators
        appControllers.AppControllerPairing.initialize();
        // configure components
        configureAndInit(opt);

        // attention, this is the big stuff - connect to jxta
        P2pConfigWorker.getInstance().enable();
        P2pConfigWorker.getInstance().execute();
        
        //gui.GuiMain.main(opt.args);
    }

    private static void commandRemoteSwitcher(Options opt) {
        int port = TelnetUi.DefaultPort;
        String host = "127.0.0.1";
        
        int splitter = opt.remote.lastIndexOf(":");
        if (splitter < 0) {
            host = opt.remote;
        } else {
            host = opt.remote.substring(0, splitter);
            port = Integer.parseInt(opt.remote.substring(splitter + 1));
        }
        
        Socket client = new Socket();
        try {
            System.out.println("Connecting to: " + host + ":" + port);
            SocketAddress couniverseAddr = new InetSocketAddress(host, port);
            client.connect(couniverseAddr);
            if (!client.isConnected()) {
                System.err.println("failed to connect to " + host + ":" + port);
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(Main.class).fatal(ex.toString(), ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(Main.class).fatal(ex.toString(), ex);
            System.exit(1);
        }
        
        SimpleSwitcher gui = null;
        try {
            gui = new SimpleSwitcher(client.getInputStream(), client.getOutputStream());
            new Thread(){
                SimpleSwitcher gui;
                
                Thread setGui(SimpleSwitcher gui){
                    this.gui = gui;
                    return this;
                }

                @Override
                public void run() {
                    gui.setVisible(true);
                }
            }.setGui(gui).start();
        } catch (IOException ex) {
            UiStub.stopFlag.set(true);
            Logger.getLogger(Main.class).fatal(ex.toString(), ex);
        }
    }

    public static void main(String[] args) {
        Options opt = parseArgs(args);
        assert opt != null;

        switch (opt.mode) {
            case ModeHelp:
                commandHelp(opt);
            case ModeRun:
                commandRun(opt);
                break;
            case ModeRemoteSwitcher:
                commandRemoteSwitcher(opt);
                break;
            case ModeGenDefault:
                commandDefaultCfg(opt);
                break;
            default:
                System.out.println("TODO: Unrecognized run mode. Consult class core.Main.");
        }
    }

    public static ControlPeer getUniversePeer() { return universePeer; }
}
