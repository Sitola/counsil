package myGUI;

import appControllers.Controller;
import appControllers.ControllerImpl;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * GUI for LocalController. Main feature is to display STDOUT and STDERR of applications being run.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 10.9.2007
 * Time: 9:25:19
 */
@Deprecated
public class ControllerFrame extends JFrame {
// TODO: Vysekat komplet, pripadne abstrahovat prez nejaky listenery a streamy
    
    static Logger logger = Logger.getLogger("myGUI");

    JTabbedPane appTabbedPane;
    ArrayList<JPanel> appPanes;
    ConcurrentHashMap<Controller, JPanel> appPanesMap;
    ConcurrentHashMap<Controller, JTextPane> textPaneMap;
    ConcurrentHashMap<Controller, ControllerPaneWriter> writerMap;

    public ControllerFrame() {
        appTabbedPane = new JTabbedPane();
        appPanes = new ArrayList<JPanel>();
        appPanesMap = new ConcurrentHashMap<Controller, JPanel>();
        textPaneMap = new ConcurrentHashMap<Controller, JTextPane>();
        writerMap = new ConcurrentHashMap<Controller, ControllerPaneWriter>();
        this.setTitle("Local AppControllers");
        this.getContentPane().add(appTabbedPane);
        this.pack();
        this.setSize(this.getPreferredSize());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });
        
        this.setVisible(true);
    }

    public void addController(Controller controller) {
        JPanel panel = createAppProxyPanel(controller);
        
        appPanes.add(panel);
        appPanesMap.put(controller, panel);
        appTabbedPane.addTab(controller.getControllerName(), panel);
        
        this.pack();
        appTabbedPane.invalidate();
    }
    
    public void addControllerWriter(Controller controller, Process process) {
        removeControllerWriter(controller);
        
        JTextPane textPane = textPaneMap.get(controller);
        ControllerPaneWriter writer = new ControllerPaneWriter(controller.getProcess(), textPane);
        writerMap.put(controller, writer);

        writer.run();
    }

    public void removeController(Controller controller) {
        removeControllerWriter(controller);
        JPanel panel = appPanesMap.get(controller);
        int tabIndex = -1;
        for (int i = 0; i < appTabbedPane.getTabCount(); i++) {
            if (appTabbedPane.getComponentAt(i) == panel) {
                tabIndex = i;
                break;
            }
        }
        assert tabIndex != -1;
        appTabbedPane.removeTabAt(tabIndex);
        appPanesMap.remove(controller, panel);
        
        textPaneMap.remove(controller);
        appPanes.remove(panel);
        this.pack();
        appTabbedPane.invalidate();
    }
    
    public void removeControllerWriter(Controller controller) {
        if (writerMap.get(controller) != null) {
            ControllerPaneWriter writer = writerMap.get(controller);
            writer.stop();
            writerMap.remove(controller);
        }
    }

    private void onExit() {
        this.setVisible(false);
        dispose();
    }


    private JPanel createAppProxyPanel(Controller localController) {

        JTextPane textPane = new JTextPane();
        textPane.setPreferredSize(new Dimension(800, 600));
        textPane.setEditable(false);
        textPane.setDocument(new DefaultStyledDocument());
        StyledDocument d = textPane.getStyledDocument();

        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style regular = d.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Monospaced");
        Style s;
        s = d.addStyle("error", regular);
        StyleConstants.setForeground(s, Color.RED);
        
        s = d.addStyle("message", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.BLUE);

        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.getViewport().add(textPane);
        jScrollPane.setAutoscrolls(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(jScrollPane, BorderLayout.CENTER);

        textPaneMap.put(localController, textPane);

        return panel;
    }
    
    public void writeMessage(String message, Controller controller) {
        writerMap.get(controller).writeMsg(message);
    }
}
