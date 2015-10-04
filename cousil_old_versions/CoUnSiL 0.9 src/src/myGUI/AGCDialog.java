package myGUI;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.*;

@Deprecated
public class AGCDialog extends JDialog {
    static Logger logger = Logger.getLogger("myGUI");

    private JButton buttonExit = new JButton("EXIT");
    private JButton buttonStartMatchMaker = new JButton("START");
    private JButton buttonOnceMatchMaker = new JButton("ONCE");
    private JPanel panel = new JPanel();
    
    private boolean shouldStartMatchMaker = false;
    private boolean shouldContinueMatchMaker = false;

    public AGCDialog() {
        
        // setModal(true);
        // getRootPane().setDefaultButton(buttonExit);
        // getContentPane().add(buttonExit);
        // getContentPane().add(startMatchMakerButton);
        
        buttonExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onExit();
            }
        });

        // call onExit() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });

        buttonStartMatchMaker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startMatchMaker();
            }
        });

        buttonOnceMatchMaker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runOnceMatchMaker();
            }
        });

        setSize(400, 400);
        panel.add(buttonExit);
        panel.add(buttonStartMatchMaker);
        panel.add(buttonOnceMatchMaker);
        add(panel);
        
        this.pack();
        this.setVisible(true);
    }

    public boolean shouldProceedWithPlanning() {
        if (this.shouldContinueMatchMaker) return this.shouldStartMatchMaker;
        if (this.shouldStartMatchMaker) {
            shouldStartMatchMaker = false;
            return true;
        }
        return false;
    }

    private void startMatchMaker() {
        if (shouldContinueMatchMaker) {
            shouldContinueMatchMaker = false;
            shouldStartMatchMaker = false;
            buttonStartMatchMaker.setText("START");
        } else {
            shouldContinueMatchMaker = true;
            shouldStartMatchMaker = true;
            buttonStartMatchMaker.setText("STOP");
        }
    }
    
    private void runOnceMatchMaker() {
        shouldContinueMatchMaker = false;
        shouldStartMatchMaker = true;
    }

    private void onExit() {
        this.setVisible(false);
        dispose();
        System.exit(0);
    }
}
