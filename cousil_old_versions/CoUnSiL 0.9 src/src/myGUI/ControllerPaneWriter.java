package myGUI;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;

/**
 * This is an application proxy class to facilitate running and terminating external applications,
 * e.g. by local peer agents as initiated by Application Group Controller.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 27.7.2007
 * Time: 11:54:56
 */

@Deprecated
public class ControllerPaneWriter {
    static Logger logger = Logger.getLogger("utils");

    private final Process proc;  // Process object used to run the application
    private final AtomicBoolean 
            printOut = new AtomicBoolean(true), 
            printErr = new AtomicBoolean(true), 
            terminated = new AtomicBoolean(false);
    private Thread stdOutReader;
    private Thread stdErrReader;
    
    private JTextPane textPane;

    private void write(String text, String style) {
        SwingUtilities.invokeLater(new Runnable() {
            private String text, style;
            public void run() {
                try {
                    StyledDocument d = textPane.getStyledDocument();
                    d.insertString(d.getLength(), text+"\n", d.getStyle(style));
                } catch (BadLocationException ex) {
                }
            }
            public Runnable setParam(String text, String style) {
                this.text = text;
                this.style = style;
                return this;
            }
        }.setParam(text, style));
    }
    public void writeOut(String text) {
        write(text, "regular");
    }
    public void writeErr(String text) {
        write(text, "error");
    }
    public void writeMsg(String text) {
        write(text, "message");
    }

    /**
     * A constructor that creates an instance of application proxy.
     *
     * @param path      file to exec including the path
     * @param opts      command line options passed on to the file
     * @param monitored specifies whether the application will be monitored
     * @throws ApplicationProxyException when file to exec doesn't exist or is not accesible
     */
    public ControllerPaneWriter(Process proc, JTextPane textPane) {
        this.proc = proc;
        this.textPane = textPane;
    }

    /**
     * This runs (starts) the application.
     *
     * @throws ApplicationProxyException when starting the application fails for some reason
     */
    synchronized public void run() {
        if (textPane == null) {
            throw new NullPointerException("The text pane to write is null!");
        }
        
        if (proc == null) return; // TODO: remove and adapt to changing processes
        
        stdErrReader = new Thread(new Runnable() {
            public void run() {
                BufferedReader b = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                String str;
                //noinspection EmptyCatchBlock
                try {
                    while (!terminated.get()) {
                        str = b.readLine();
                        if (str == null) {
                            break;
                        }

                        writeErr(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        stdOutReader = new Thread(new Runnable() {
            public void run() {
                BufferedReader b = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String str;
                //noinspection EmptyCatchBlock
                try {
                    while (!terminated.get()) {
                        str = b.readLine();
                        if (str == null) {
                            break;
                        }
                        writeOut(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        stdErrReader.start();
        stdOutReader.start();
    }
    
    synchronized public void stop() {
        this.terminated.set(true);
    }
    
    /*
    synchronized public void run() throws ApplicationProxyException {
        Runtime runtime = Runtime.getRuntime();
        // final String cmdLine = "./appwrapper " + this.path + " " + this.opts;
        final String cmdLine = this.path + " " + this.opts;
        try {
            terminated.set(false);
        } catch (IOException e) {
            throw new ApplicationProxyException("Failed to exec the application: " + e.getMessage());
        }

        if (localControllerFrame != null) {
            // we have GUI
            final ApplicationProxy ap = this;
            //noinspection EmptyCatchBlock
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        localControllerFrame.writeCmd(ap, cmdLine);
                    }
                });
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            }
            stdErrReader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader b = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    String str;
                    //noinspection EmptyCatchBlock
                    try {
                        while (!terminated.get()) {
                            str = b.readLine();
                            if (str == null) {
                                break;
                            }
                            final String str1 = str;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    localControllerFrame.writeStdErr(ap, str1);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            stdOutReader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader b = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    String str;
                    //noinspection EmptyCatchBlock
                    try {
                        while (!terminated.get()) {
                            str = b.readLine();
                            if (str == null) {
                                break;
                            }
                            final String str1 = str;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    localControllerFrame.writeStdOut(ap, str1);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            // we don't have GUI, thus using console
            System.out.println(this.toString() + " cmd: " + cmdLine);
            stdErrReader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader b = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    String str;
                    //noinspection EmptyCatchBlock
                    try {
                        while (!terminated.get()) {
                            str = b.readLine();
                            if (str == null) {
                                break;
                            }
                            System.err.println(this.toString() + " stderr: " + str);
                            System.err.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.err.flush();
                    //noinspection EmptyCatchBlock
                    try {
                        b.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            stdOutReader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader b = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    String str;
                    //noinspection EmptyCatchBlock
                    try {
                        while (!terminated.get()) {
                            str = b.readLine();
                            if (str == null) {
                                break;
                            }
                            System.out.println(this.toString() + " stdout: " + str);
                            System.out.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.flush();
                    //noinspection EmptyCatchBlock
                    try {
                        b.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        stdErrReader.start();
        stdOutReader.start();

        this.killed = false;
    }
    */
    
}
