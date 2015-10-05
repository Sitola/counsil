package core;

import gui.MainFrame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;
import mediaAppFactory.MediaApplication;
import myJXTA.MyJXTAConnector;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.LogicalNetworkLink;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import utils.ConfigUtils;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class UiStub extends Thread {
    public static final String CmdShow = "show";
    public static final String CmdShutdown = "shutdown";
    public static final String CmdHelp = "help";
    public static final String CmdExit = "exit";
    public static final String CmdDiscard = "discard";
    public static final String CmdSet = "set";
    public static final String CmdSave = "save";
    public static final String CmdCommit = "commit";


    public static final String argShowConfig = "config";
    public static final String KeyStatus = "status";
    public static final String KeyMessage = "message";
    public static final String KeyResult = "result";
    public static final String StatusOk = "OK";
    public static final String StatusError = "ERROR";
    public static final char tokenComment = '#';

    public static class UiChannelEndpoint {
        protected BufferedReader input;
        protected PrintStream output;
        private int timeout;
        boolean swallowPrompt;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int msec) {
            this.timeout = timeout;
        }

        public BufferedReader getInput() {
            return input;
        }

        public PrintStream getOutput() {
            return output;
        }

        public UiChannelEndpoint(BufferedReader input, PrintStream output) {
            this(input, output, true);
        }
        public UiChannelEndpoint(BufferedReader input, PrintStream output, boolean swallowPrompt) {
            this.output = output;
            this.input = input;
            this.swallowPrompt = swallowPrompt;
            timeout = 2000;
        }

        public void swallowRest() throws IOException {
            boolean foundEmptyLine = false;
            String cache = "";
            boolean previousCR = false;

            while (!foundEmptyLine && input.ready()) {
                int c = input.read();
                if (c < 0) return;

                boolean lineDone = false;

                if (previousCR) {
                    lineDone = true;
                } else if (c == '\n') {
                    lineDone = true;
                } else if (c == '\r') {
                    previousCR = true;
                } else {
                    previousCR = false;
                    lineDone = false;
                }

                if (lineDone) {
                    foundEmptyLine = cache.trim().isEmpty();
                    cache = "";
                    if (c == '\n') continue;
                }

                cache += Character.toString((char)c);
            }
        }

        public boolean awaitResponse() throws IOException {
            // timeouted read
            int msec = 50;
            for (int i = 0; i < timeout && !input.ready(); i += msec) {
                try {
                    Thread.sleep(msec);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.DEBUG, null, ex);
                }
            }

            return input.ready();
        }

        public JSONObject processInput() throws JSONException, IOException {
            String data = "";

            boolean foundEmptyLine = false;
            while (!foundEmptyLine) {
                String line = input.readLine();
                foundEmptyLine = line.trim().isEmpty();
                data += line;
            }

            if (swallowPrompt) {
                swallowRest();
            }

            if (data.isEmpty()) return null;

            return new JSONObject(data);
        }

        public void close() throws IOException {
            input.close();
            output.close();
        }

    }
    private static class ServerUiEndpoint extends UiChannelEndpoint {
        public static final char QuoteChar = '"';

        public static boolean isString(int token) {
            switch (token) {
                case QuoteChar:
                case StreamTokenizer.TT_WORD:
                    return true;
                default:
                    return false;
            }
        }

        private final StreamTokenizer tokenizer;

        public ServerUiEndpoint(InputStream inStream, OutputStream outStream) {
            super(new BufferedReader(new InputStreamReader(inStream)), new PrintStream(outStream));
            tokenizer = createTokenizer(input);
        }

        public StreamTokenizer getTokenizer() {
            return tokenizer;
        }

        public static StreamTokenizer createTokenizer(Reader input) {
            StreamTokenizer tokenizer = new StreamTokenizer(input);
            tokenizer.resetSyntax();
            tokenizer.commentChar(tokenComment);
            //tokenizer.quoteChar(tokenQuote);
            tokenizer.eolIsSignificant(true);
            tokenizer.lowerCaseMode(false);
            tokenizer.slashSlashComments(false);
            tokenizer.slashStarComments(false);
            tokenizer.ordinaryChar(',');
            tokenizer.wordChars(' ', 255);
            tokenizer.whitespaceChars(0, ' ');
            tokenizer.quoteChar(QuoteChar);
    //        tokenizer.quoteChar('\"');
    //        tokenizer.quoteChar('\'');
    //        tokenizer.quoteChar('"');
    //        tokenizer.quoteChar('\'');
            tokenizer.wordChars('a', 'z');
            tokenizer.wordChars('A', 'Z');
            tokenizer.wordChars('0', '9');
            tokenizer.wordChars('_', '_');
            tokenizer.wordChars('.', '.');
            tokenizer.wordChars('+', '+');
            tokenizer.wordChars('-', '-');
            tokenizer.wordChars('[', '[');
            tokenizer.wordChars(']', ']');
            tokenizer.wordChars('{', '{');
            tokenizer.wordChars('}', '}');
            tokenizer.whitespaceChars(' ', ' ');
            tokenizer.whitespaceChars('\t', '\t');
            tokenizer.whitespaceChars('\n', '\n');
            tokenizer.whitespaceChars('\r', '\r');
            return tokenizer;
        }

        public String getRest() throws IOException {
            boolean previousCR = false;
            boolean foundEmptyLine = false;
            String cache = "";

            while (!foundEmptyLine && input.ready()) {
                int c = input.read();
                if (c < 0) break;

                cache += Character.toString((char)c);
                boolean lineDone = false;

                if (previousCR) {
                    lineDone = true;
                } else if (c == '\n') {
                    lineDone = true;
                } else if (c == '\r') {
                    previousCR = true;
                } else {
                    previousCR = false;
                    lineDone = false;
                }

                if (lineDone) {
                    foundEmptyLine = cache.trim().isEmpty();
                    if (c == '\n') continue;
                }
            }

            return cache;
        }

        public void prompt() {
            output.print("> ");
            output.flush();
        }
    }

    private abstract class Command {
        abstract boolean execute() throws JSONException, IOException;
        abstract String help();
    }


    class ShutdownTask extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            System.err.println("Left universe");
            Main.getUniversePeer().applicationControl().stopMediaApplications();
            Main.getUniversePeer().leaveUniverse();

            try {
                // todo maara - vyhodit a opravit vypinani jxty
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            System.exit(0);

            return null;
        }
        @Override
        public void done() {}
    }


    HashMap<String, Command> commands;

    public static AtomicBoolean stopFlag = new AtomicBoolean(false);

    ServerUiEndpoint uiEndpoint;
    boolean localStop;
    Map<String, JSONObject> pendingConfigCache;
    Map<String, JSONObject> lastKnownConfigCache;

    public PrintStream getOutput() {
        return uiEndpoint.getOutput();
    }

    public BufferedReader getInput() {
        return uiEndpoint.getInput();
    }

    public UiStub(OutputStream outStream, InputStream inStream) {
    //stopFlag = new AtomicBoolean(false);
        uiEndpoint = new ServerUiEndpoint(inStream, outStream);
        localStop = false;
        pendingConfigCache = new HashMap<>();
        lastKnownConfigCache = new HashMap<>();
        commands = new HashMap<>();

        commands.put(CmdHelp, new Command() {
            @Override
            public boolean execute() throws JSONException, IOException {
                StringBuilder commandName = new StringBuilder();
                while (ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())){
                    if (commandName.length() != 0) commandName.append(" ");
                    commandName.append(uiEndpoint.getTokenizer().sval);
                }

                if (commandName.length() == 0) {
                    return listCommands();
                } else {
                    return helpCommand(commandName.toString());
                }
            }

            private boolean listCommands() throws JSONException {
                StringBuilder available = new StringBuilder();
                for (Map.Entry<String, Command> cmd : commands.entrySet()){
                    // list only base commands
                    if (cmd.getKey().contains(" ")) continue;
                    available.append(" ");
                    available.append(cmd.getKey());
                }

                available.insert(0, "Available commands:");

                markOk(available.toString());
                return true;
            }

            private boolean helpCommand(String command) throws JSONException{
                Command cmd = commands.get(command);
                if (cmd == null) {
                    markError("No such command: " + command);
                    return true;
                }

                markOk(cmd.help());
                return true;
            }

            @Override
            public String help() {
                return "Usage: help [command [argument]*] ; show help for given command.";
            }
        });
        commands.put(CmdShutdown, new Command(){
            @Override
            public boolean execute() throws JSONException, IOException {
                ShutdownTask sh = new ShutdownTask();
                sh.execute();
                markOk("Shutting down node.");
                uiEndpoint.getOutput().flush();
                stopFlag.set(true);
                return false;
            }

            @Override
            public String help() {
                return "Usage: shutdown ; Shuts down this CoUniverse instance";
            }

        });
        commands.put(CmdExit, new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                markOk("Closing pipes");
                uiEndpoint.close();
                localStop = true;
                return false;
            }

            @Override
            public String help() {
                return "Usage: exit ; disconnects the interface (CoUniverse is left running)";
            }
        });
        commands.put(CmdDiscard, new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                pendingConfigCache.clear();
                markOk("Pending changes discarded");
                return true;
            }

            @Override
            public String help() {
                return "Usage: discard ; Discards pending configuration (that is all 'set config' commands since last 'commit'.";
            }
        });
        commands.put(CmdCommit, new Command(){

            @Override
            public boolean execute() throws JSONException, IOException {
                for (Map.Entry<String, JSONObject> entry : pendingConfigCache.entrySet()) {
                    Map.Entry<String, Configurable> manager = ConfigUtils.ConfigManager.getClosestRootManager(entry.getKey());
                    if (manager == null) {
                        markError("Failed to get manager for " + entry.getKey());
                        return true;
                    }
                    manager.getValue().loadConfig(entry.getValue());
                }
                pendingConfigCache.clear();
                markOk("Changes sucessfully commited");
                Main.getUniversePeer().sendNodeInformations();
                return true;
            }

            @Override
            public String help() {
                return "Usage: commit ; Commit pending configuration changes (configuration changes set using 'set config' are pending and not propagated to CoUniverse until 'commit'.";
            }

        });
        commands.put(CmdSave, new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                String filename = unescape(uiEndpoint.getRest().trim());
                if (filename.isEmpty()) filename = ConfigUtils.ConfigFileName;
                if (ConfigUtils.saveConfig(filename, ConfigUtils.ConfigManager.getConfig("."))) {
                    markOk("Config file saved as \"" + (new File(filename)).getCanonicalPath() + "\"");
                } else {
                    markError("Failed to save config file");
                }
                return true;
            }

            @Override
            public String help() {
                return "Usage: save [config filename] ; saves currently used configuration (that is 'commited') into config file (defaults to " + ConfigUtils.ConfigFileName + ")";
            }
        });
        commands.put(CmdShow, new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())){
                    markError(help());
                    return true;
                }

                return findAndExecute("show "+uiEndpoint.getTokenizer().sval);
            }

            @Override
            public String help() {
                StringBuilder available = new StringBuilder();
                for (Map.Entry<String, Command> cmd : commands.entrySet()){
                    // list only base commands
                    if (!cmd.getKey().startsWith("show ")) continue;
                    if (available.length() != 0) available.append(" | ");
                    available.append(cmd.getKey());
                }

                available.insert(0, "Usage: show <mode> ; available modes:");
                return available.toString();
            }
        });
        commands.put(CmdSet, new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())){
                    markError(help());
                    return true;
                }

                return findAndExecute("set "+uiEndpoint.getTokenizer().sval);
            }

            @Override
            public String help() {
                StringBuilder available = new StringBuilder();
                for (Map.Entry<String, Command> cmd : commands.entrySet()){
                    // list only base commands
                    if (cmd.getKey().startsWith("set ")) continue;
                    if (available.length() != 0) available.append(" | ");
                    available.append(cmd.getKey());
                }

                available.insert(0, "Usage: set <mode> <key> <value> ; sets key to value (mode dependent behaviour). Available modes:");
                return available.toString();
            }
        });
        commands.put("show plan", new Command(){

            @Override
            public boolean execute() throws JSONException, IOException {
                JSONObject planReport = Main.getUniversePeer().getLatestUniverseState().reportPlan();
                if (planReport != null) {
                    markOk(null, planReport);
                } else {
                    markError("No plan available yet");
                }
                return true;
            }

            @Override
            public String help() {
                return "Usage: show plan ; Shows generated plan received through the P2P layer (if available).";
            }

        });
        commands.put("show config", new Command(){

            @Override
            public boolean execute() throws JSONException, IOException {
                int token = uiEndpoint.getTokenizer().nextToken();

                if (!ServerUiEndpoint.isString(token)) {
                    StringBuilder builder = new StringBuilder();
                    for (String path : ConfigUtils.ConfigManager.getRegisteredPaths()) {
                        if (builder.length() > 0) builder.append(", ");
                        builder.append(path);
                    }
                    builder.insert(0, "Expected path, but path argument not given. Possible paths: ");
                    markError(builder.toString());
                    return true;
                }

                // do not trim as we support quotation
                String path = uiEndpoint.getTokenizer().sval;
                return showConfig(path);
            }

            private boolean showConfig(String subpath) throws JSONException {
                Map.Entry<String, JSONObject> entry = ConfigUtils.ConfigManager.getClosestRootConfig(subpath);
                if (entry == null) {
                    markError("Invalid path \"" + subpath + "\"");
                    return true;
                }
                lastKnownConfigCache.put(entry.getKey(), entry.getValue());

                markOk(entry.getKey(), entry.getValue());
                return true;
            }

            @Override
            public String help() {
                StringBuilder result = new StringBuilder();
                for (String tree : ConfigUtils.ConfigManager.getRegisteredPaths()) {
                    result.append(" ");
                    result.append(tree);
                }
                result.insert(0, "Usage: show config <tree> ; shows current (running) configuration. Available trees:");
                return result.toString();
            }

        });
        commands.put("show rendezvous", new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                try {
                    markOk(URI.create("tcp://" + InetAddress.getLocalHost().getHostAddress() + ":" + MyJXTAConnector.DEFAULT_COUNIVERSE_TCP_PORT).toString(), null);
                } catch (UnknownHostException ex) {
                    markError(ex.toString(), null);
                }
                return true;
            }

            @Override
            public String help() {
                return "Usage: show rendezvous ; Show rendezvous uri of this instance (currently only format hint)";
            }
        });
        commands.put("show pending", new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {
                JSONObject result = new JSONObject(pendingConfigCache);
                markOk("Configuration pending for save", result);
                return true;
            }

            @Override
            public String help() {
                return "Usage: show pending ; Show pending configuration since last 'discard' or 'commit'.";
            }
        });
        commands.put("set config", new Command() {

            @Override
            public boolean execute() throws JSONException, IOException {

                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting path");
                    return true;
                }

                String path = uiEndpoint.getTokenizer().sval;
                if (path.isEmpty()) {
                    markError("Config path cannot be empty");
                    return true;
                }

                String value = unescape(uiEndpoint.getRest());
//                if (value.trim().isEmpty()) {
//                    markError("Value cannot be empty");
//                    return true;
//                }

                try {
                    return setConfig(path, value.trim());
                } catch (JSONException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Failed to set configuration: " + ex.toString());
                } catch (IllegalArgumentException ex) {
                    markError(ex.toString());
                }
                return true;
            }

            private boolean updateConfig(Object target, String path, Object value) throws JSONException {
                int dotpos = path.indexOf(".");
                String subkey = dotpos < 0 ? path : path.substring(0, dotpos);
                String nextkey = dotpos < 0 ? "" : path.substring(dotpos+1);

                int index = 0;
                if (target instanceof JSONArray) {
                    try {
                        index = Integer.parseInt(subkey);
                    } catch (NumberFormatException ex) {
                        markError("Type mismatch: expected integer index, but got \"" + subkey + "\" instead.");
                        return false;
                    }
                }

                // not an array, consider updating
                if (nextkey.isEmpty()) {
                    if (target instanceof JSONArray) {
                        JSONArray arr = (JSONArray)target;
                        arr.put(index, value);
                    } else if (target instanceof JSONObject) {
                        JSONObject obj = (JSONObject)target;
                        obj.remove(subkey);
                        obj.put(subkey, value);
                    } else {
                        throw new IllegalArgumentException("Only JSONObject and JSONArray targets are supported");
                    }
                    return true;
                } else {
                    if (target instanceof JSONArray) {
                        JSONArray arr = (JSONArray)target;
                        return updateConfig(arr.get(index), nextkey, value);
                    } else if (target instanceof JSONObject) {
                        JSONObject obj = (JSONObject)target;
                        return updateConfig(obj.get(subkey), nextkey, value);
                    } else {
                        throw new IllegalArgumentException("Only JSONObject and JSONArray targets are supported");
                    }
                }
            }

            private String canonizePath(String path) {
                path = path.replaceAll("[.][.]", ".");
                if (!path.startsWith(".")) path = "." + path;
                    return path;
            }

            private boolean setConfig(String path, String data) throws JSONException {
                path = canonizePath(path);
                data = data.trim();

                Object value = null;

                try {
                    if (data.startsWith("{")) {
                        value = new JSONObject(data);
                    } else if (data.startsWith("[")) {
                        value = new JSONArray(data);
                    }

                    value = new Double(data);
                } catch (JSONException ex){
                    markError("Failed to correctly identify input data type: " + ex.toString());
                    return true;
                } catch (NumberFormatException ex) {
                    ;// not a problem, just not a number
                }

                // consider input data string
                if (value == null) {
                    value = data;
                }

                Map.Entry<String, Configurable> original = ConfigUtils.ConfigManager.getClosestRootManager(path);
                String subpath = path.substring(original.getKey().length()+1);

                if (!pendingConfigCache.containsKey(original.getKey())) {
                    JSONObject active = original.getValue().activeConfig();
                    pendingConfigCache.put(original.getKey(), active);
                    if (!lastKnownConfigCache.containsKey(original.getKey())) {
                        lastKnownConfigCache.put(original.getKey(), active);
                    }
                }

                String warnings = new String();
                for (Map.Entry<String, JSONObject> updated : pendingConfigCache.entrySet()) {
                    JSONObject cachedConfig = lastKnownConfigCache.get(updated.getKey());
                    if (cachedConfig == null) {
                        Logger.getLogger(this.getClass()).log(Level.FATAL, "BUG: config cache does not contain key for updated value!");
                        continue;
                    }

                    if (!lastKnownConfigCache.get(updated.getKey()).equals(ConfigUtils.ConfigManager.getClosestRootConfig(updated.getKey()).getValue())) {
                        if (!warnings.isEmpty()) warnings += ", ";
                        warnings += "\"" + updated.getKey() + "\"";
                    }
                }

                boolean showWarning = true;
                if (subpath.isEmpty()) {
                    if (!(value instanceof JSONObject)) {
                        markError("Type error when setting tree root (expected JSONObject)");
                    }
                    pendingConfigCache.put(original.getKey(), (JSONObject)value);
                } else {
                    JSONObject target = pendingConfigCache.get(original.getKey());
                    showWarning = updateConfig(target, subpath, value);
                }

                if (showWarning) {
                    if (warnings.isEmpty()) {
                        markOk("Configuration change pending");
                    } else {
                        markOk("Configuration changed while waiting for commit (keys: " + warnings + "), consider either review, commiting or reset");
                    }
                }

                return true;
            }

            @Override
            public String help() {
                return "Usage: set config <treeish> <value> ; where treeish is dot-delimited config tree (eg. .object.attribute.array.integer-index) and value is either json object, json array, \"string\" or number";
            }
        });
        commands.put("show applications", new Command() {

            @Override
            boolean execute() throws JSONException, IOException {
                markOk("Applications configured on this node", Main.getUniversePeer().applicationControl().reportStatus());
                return true;
            }

            @Override
            String help() {
                return "Usage: show applications ; Show list of applications configured to run on this node";
            }

        });
        commands.put("show application", new Command() {

            @Override
            boolean execute() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application. See 'show applications'.");
                    return true;
                }

                String appId = uiEndpoint.getTokenizer().sval;
                
                MediaApplication app = Main.getUniversePeer().applicationControl().getAppByUuidOrName(appId);
                if (app == null) {
                    markError("No such application");
                    return true;
                }
                
                markOk("Detail for application " + app.getUuid(), Main.getUniversePeer().applicationControl().reportDetail(app));
                
                
                return true;
            }

            @Override
            String help() {
                return "Usage: show application <identifier>, Show details of application running on this node; where <identifier> is either application name or application UUID.";
            }

        });
        commands.put("show log", new Command() {

            @Override
            boolean execute() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application. See 'show applications'.");
                    return true;
                }

                String appId = uiEndpoint.getTokenizer().sval;
                
                MediaApplication app = Main.getUniversePeer().applicationControl().getAppByUuidOrName(appId);
                if (app == null) {
                    markError("No such application");
                    return true;
                }
                
                String log = Main.getUniversePeer().applicationControl().statusMediaApplication(app);
                if (log == null) {
                    markError("Application wanished before log was received");
                    return true;
                }
                
                JSONObject report = new JSONObject();
                report.put("log", log);
                
                markOk("Last log for application " + app.getUuid(), report);
                return true;
            }

            @Override
            String help() {
                return "Usage: show application <identifier>, Show details of application running on this node; where <identifier> is either application name or application UUID.";
            }

        });
        commands.put("stop", new Command() {

            @Override
            boolean execute() throws JSONException, IOException {

                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application");
                    return true;
                }

                String appid = uiEndpoint.getTokenizer().sval;
                MediaApplication app = Main.getUniversePeer().applicationControl().getAppByUuidOrName(appid);
                if (app == null) {
                    markError("No application identified by name or (possibly invalid) uuid " + appid + " found.");
                    return true;
                }

                Main.getUniversePeer().applicationControl().stopMediaApplication(app);
                markOk("Application stopped", app.activeConfig());

                return true;
            }

            @Override
            String help() {
                return "Usage: stop <identifier> ; stop running application, for identifier semantics see 'show application'";
            }
        });
        commands.put("start", new Command() {

            private boolean attemptPatch(MediaApplication app) throws JSONException {
                JSONObject configuration = app.activeConfig();
                JSONObject patch = null;

                String rest = null;
                try {
                    rest = uiEndpoint.getRest();
                } catch (IOException ex) {
                    return true;
                }
                if (rest.trim().isEmpty()) return true;

                try {
                    patch = new JSONObject(rest);
                } catch (JSONException ex) {
                    markError("Config patch \"" + rest + "\" is not a valid JSON object");
                    return false;
                }

                ConfigUtils.mergeConfig(configuration, patch);
                try {
                    app.loadConfig(configuration);
                } catch (IllegalArgumentException ex) {
                    markError("Unable to load patched configuration, application will not be started.", configuration);
                    return false;
                }
                return true;
            }

            private MediaApplication getTemplate() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application template to instantiate");
                    return null;
                }

                String templateName = uiEndpoint.getTokenizer().sval;

                MediaApplication app = null;
                try {
                    app = Main.getUniversePeer().getLocalNode().getApplicationTemplates().instantiateLocalTemplate(templateName);
                } catch (IOException ex) {
                    markError("Failed to start application due to: " + ex.toString(), app.activeConfig());
                    return null;
                } catch (IllegalArgumentException ex) {
                    markError(ex.toString());
                    return null;
                }

                return app;
            }

            private MediaApplication getInstance(String generatorName) throws JSONException {
                MediaApplication app = null;
                try {
                    app = mediaAppFactory.ApplicationFactory.newApplication(generatorName);
                } catch (IllegalArgumentException ex) {
                    markError(ex.toString());
                    return null;
                }
                if (app == null) {
                    markError("Generator failed to create application instance");
                    return null;
                }
                return app;
            }

            private MediaApplication decideApp() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application name or template to instantiate");
                    return null;
                }

                String templateName = uiEndpoint.getTokenizer().sval;
                if ("template".equals(templateName)) {
                    return getTemplate();
                } else {
                    return getInstance(templateName);
                }
            }

            @Override
            boolean execute() throws JSONException, IOException {
                MediaApplication app = decideApp();
                if (app == null) return true;

                // attempt patching
                if (!attemptPatch(app)) return true;

                try {
                    Main.getUniversePeer().applicationControl().runMediaApplication(app);
                } catch (IOException ex) {
                    markError("Application failed to start: " + ex.toString());
                    return true;
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(this.getClass()).log(Level.FATAL, "BUG: starting new application, but ControlPeer claims application is allready running!: " + ex.toString());
                    markError("Application is allready running: " + ex.toString());
                    return true;
                }

                markOk("Application started", app.activeConfig());
                return true;
            }

            @Override
            String help() {
                return "Usage: start <application name> [patch]; or start template <template name> [patch] ; "
                    + "Instantiate and run application, either by completly new instance or from application template. Application UUID is always generated at random."
                    + "Started application can be configured in the .applications.<app-uuid> subtree. "
                    + "Possible application names can be listed by 'help defapp'. Existing template names can be listed by 'show config .templates'."
                    + "Patch argument is JSON object containing values to replace default application configuration (template). "
                    + "\n\nExample usage: 'defapp konzument \"Ultragrid producer\"' 'set config .templates.konzument.sourceSite \"Prague\"' 'commit' 'start template konzument'\n\n"
                    + "\n\nExample usage: 'start \"Ultragrid producer\" {\"sourceSite\":\"Prague\"}'\n\n";
            }
        });
        commands.put("defapp", new Command(){
            @Override
            boolean execute() throws JSONException, IOException {
                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application template name");
                    return true;
                }
                String templateName = uiEndpoint.getTokenizer().sval;

                if (!ServerUiEndpoint.isString(uiEndpoint.getTokenizer().nextToken())) {
                    markError("Missing argument denoting application template type");
                    return true;
                }
                String templateType = uiEndpoint.getTokenizer().sval;

                try {
                    EndpointNetworkNode.ApplicationTemplatesManager templates = Main.getUniversePeer().getLocalNode().getApplicationTemplates();
                    MediaApplication app = templates.createTemplate(templateName, templateType);

                    if (app == null) {
                        markError("No generator for application " + uiEndpoint.getTokenizer().sval + " exists, probably typo");
                    } else {
                        markOk("Template " + templateName + " successfully generated and ready to configure (use set/show config)");
                    }
                } catch (IllegalArgumentException ex) {
                    markError(ex.toString());
                }
                return true;
            }

            @Override
            String help() {
                StringBuilder response = new StringBuilder();

                for (String appname : mediaAppFactory.ApplicationFactory.getInstance().getGeneratorNicknames()) {
                    if (response.length() != 0) response.append(", ");
                    response.append("\"" + appname + "\"");
                }
                response.insert(0, "Usage: defapp template-name application ; Defines new application template with it's own configuration under '.templates.applicationTemplates.template-name'. Template name is a new string template name whilst application is one-of hardcoded CoUniverse applications. Available applications: ");

                return response.toString();
            }

        });
        commands.put("show universe", new Command() {

            @Override
            boolean execute() throws JSONException, IOException {
                JSONObject result = new JSONObject();
                JSONObject nodes = new JSONObject();
                result.put("nodes", nodes);
                for (EndpointNetworkNode node : Main.getUniversePeer().getLatestUniverseState().getUniverseEndpoints()) {
                    JSONObject nodeReport = node.activeConfig();
                    JSONArray applicationsReport = new JSONArray();
                    nodeReport.put("applications", applicationsReport);
                    
                    for (MediaApplication app : node.getNodeApplications()) {
                        applicationsReport.put(app.activeConfig());
                    }
                    
                    nodes.put(node.getUuid(), nodeReport);
                }
                
                JSONArray links = new JSONArray();
                result.put("links", links);
                for (LogicalNetworkLink link : Main.getUniversePeer().getLatestUniverseState().getUniverseNetwork()) {
                    JSONObject linkReport = new JSONObject();
                    links.put(link.reportStatus());
                }
                
                markOk("Known collaboration universe", result);
                
                return true;
            }

            @Override
            String help() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        
        });
    }

    public boolean findAndExecute(String command) throws JSONException, IOException{
        Command cmd = commands.get(command);
        
        if (tryMatchConsument(command)){
            return true;
        }
        
        if (cmd == null) {
            markError("No such command: " + command);
            return true;
        }

        return cmd.execute();
    }
    
    public boolean tryMatchConsument(String command) throws JSONException {
        synchronized (Main.getUniversePeer().applicationControl()) {
            Collection<MediaApplication> apps = Main.getUniversePeer().applicationControl().getMediaApplications();

            if (apps.size() != 1) return false;

            Collection<EndpointNetworkNode> universeNodes = Main.getUniversePeer().getLatestUniverseState().getUniverseEndpoints();
            HashSet<String> siteNames = new HashSet<>();

            for (EndpointNetworkNode unode : universeNodes) {
                siteNames.add(unode.getNodeSite().getSiteName());
            }

            Pattern p = Pattern.compile(command, Pattern.CASE_INSENSITIVE);
            String candidateSite = null;

            for (String siteName : siteNames) {
                if (p.matcher(siteName).matches()) {
                    if (candidateSite == null) {
                        candidateSite = siteName;
                    } else {
                        return false;
                    }
                }
            }

            if (candidateSite == null) return false;

            MediaApplication app = apps.iterator().next();
            JSONObject currentConfig = app.activeConfig();
            currentConfig.put("sourceSite", candidateSite);
            app.loadConfig(currentConfig);
            Main.getUniversePeer().sendNodeInformations();
            markOk("Successfully switched sourceSite");

            return false;
        }
    }

    public void run(){
        try {
            if (!stopFlag.get() && !localStop) {
                uiEndpoint.prompt();
            }

            while (!stopFlag.get() && !localStop) {
                if (!uiEndpoint.input.ready()) {
                    try {
                        Thread.sleep(50);
                    } finally {
                        continue;
                    }
                }

                boolean showPrompt = processInput();

                if (showPrompt && !stopFlag.get() && !localStop) {
                    uiEndpoint.prompt();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(UiStub.class.getName()).log(Level.FATAL, "Cannot read input channel!", ex);
        } catch (JSONException ex) {
            Logger.getLogger(UiStub.class.getName()).log(Level.ERROR, "Failed to compose json message to be sent to client", ex);
        }
    }

    private static String unescape(String s) {
        s = s.trim();
        Matcher m = Pattern.compile("^\"(.+)\"$").matcher(s);
        return (m.matches()) ? m.replaceAll("$1") : s;
    }

    /**
     * @return Whether prompt shall be shown in next iteration
     */
    private boolean processInput() throws IOException, JSONException {
        int tokenType = uiEndpoint.getTokenizer().nextToken();
        switch (tokenType) {
            case StreamTokenizer.TT_EOF:
                stopFlag.set(true);
                return false;
            case StreamTokenizer.TT_EOL:
                return false;
            case StreamTokenizer.TT_NUMBER:
                markError("Line " + Integer.toString(uiEndpoint.getTokenizer().lineno()) + ": First line of continuous input has to start with command name!");
                uiEndpoint.swallowRest();
                return false;
        }

        // ok, input detected, find out command
        String command = uiEndpoint.getTokenizer().sval;
        return findAndExecute(command);
    };

    private void markOk(String message) throws JSONException {
        markOk(message, null);
    }
    private void markError(String message) throws JSONException {
        markError(message, null);
    }
    private void markOk(String message, JSONObject result) throws JSONException {
        JSONObject report = new JSONObject();
        report.put(KeyStatus, StatusOk);

        if (message != null)
            report.put(KeyMessage, message);

        if (result != null)
            report.put(KeyResult, result);

        uiEndpoint.getOutput().println(report.toString(2)+"\n");
    }
    private void markError(String message, JSONObject result) throws JSONException {
        JSONObject report = new JSONObject();
        report.put(KeyStatus, StatusError);

        if (message != null)
            report.put(KeyMessage, message);

        if (result != null)
                report.put(KeyResult, result);

        uiEndpoint.getOutput().println(report.toString(2)+"\n");
    }


}




