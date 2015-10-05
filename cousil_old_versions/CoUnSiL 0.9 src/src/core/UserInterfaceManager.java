package core;

import gui.MainFrame;
import gui.SimpleSwitcher;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class UserInterfaceManager implements Configurable {
    public static final String ConfigKeyTelnet = "telnetPort";
    public static final String ConfigKeyGui = "gui";
    public static final String ConfigKeyCli = "cli";

    private TelnetUi telnetInterface;
    private MainFrame guiInterface;
    private SimpleSwitcher switcherInterface;
    private UiStub cliInterface;

    private static UserInterfaceManager instance = null;

    private UserInterfaceManager() {
        this.telnetInterface = null;
        this.guiInterface = null;
        this.cliInterface = null;
    }

    public static UserInterfaceManager getInstance() {
        if (instance == null) instance = new UserInterfaceManager();
        return instance;
    }

    @Override
    public boolean loadConfig(JSONObject configuration) throws IllegalArgumentException {
        boolean startGui = configuration.optBoolean(ConfigKeyGui, true);
        boolean startCli = configuration.optBoolean(ConfigKeyCli, false);
        boolean startSwitcher = configuration.optBoolean("switcher", false);
        int telnetPort = configuration.optInt(ConfigKeyTelnet, -1);

        boolean retval = false;

        if ((guiInterface != null) != startGui) {
            retval |= true;

            if (guiInterface != null) {
                guiInterface.setVisible(false);
                guiInterface = null;
            } else {
                PipedOutputStream uiIn = new PipedOutputStream();
                PipedInputStream uiOut = new PipedInputStream();

                UiStub uiControls = null;
                try {
                    uiControls = new UiStub(new PipedOutputStream(uiOut), new PipedInputStream(uiIn));
                    uiControls.start();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.FATAL, "Unable to open internal UI I/O channel streams!", ex);
                }

                guiInterface = new MainFrame(uiOut, uiIn);

                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        guiInterface.setVisible(true);
                    }
                });
            }
        }

        if ((switcherInterface != null) != startSwitcher) {
            retval |= true;

            if (switcherInterface != null) {
                switcherInterface.setVisible(false);
                switcherInterface = null;
            } else {
                PipedOutputStream uiIn = new PipedOutputStream();
                PipedInputStream uiOut = new PipedInputStream();

                UiStub uiControls = null;
                try {
                    uiControls = new UiStub(new PipedOutputStream(uiOut), new PipedInputStream(uiIn));
                    uiControls.start();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.FATAL, "Unable to open internal UI I/O channel streams!", ex);
                }

                switcherInterface = new SimpleSwitcher(uiOut, uiIn);

                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        switcherInterface.setVisible(true);
                    }
                });
            }
        }
        
        if ((cliInterface != null) != startCli) {
            retval |= true;

            if (cliInterface != null) {
                cliInterface.localStop = true;
                cliInterface = null;
            } else {
                cliInterface = new UiStub(System.out, System.in);
                cliInterface.start();
            }
        }

        // telnet
        int oldPort = -1;
        if (telnetInterface != null) {
            oldPort = telnetInterface.getPort();
            if ((telnetPort < 0) || (telnetPort != oldPort)) {
                telnetInterface.localStop = true;
                telnetInterface = null;
                retval |= true;
            }
        }
        if (telnetPort >= 0 && telnetPort != oldPort) {
            try {
                telnetInterface = new TelnetUi(telnetPort);
                retval |= true;
                telnetInterface.start();
            } catch (IOException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, "Failed to (re)start telnet interface", ex);
            }
        }

        return retval;
    }

    @Override
    public JSONObject activeConfig() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(ConfigKeyGui, guiInterface != null);
        root.put(ConfigKeyCli, cliInterface != null);
        root.put(ConfigKeyTelnet, telnetInterface != null ? telnetInterface.getPort() : -1);
        root.put("switcher", switcherInterface != null);
        return root;
    }

    @Override
    public JSONObject defaultConfig() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(ConfigKeyGui, true);
        root.put(ConfigKeyCli, false);
        root.put(ConfigKeyTelnet, TelnetUi.DefaultPort);
        return root;
    }

}
