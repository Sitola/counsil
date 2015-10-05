package gui;

import gui.constants.GuiConstants;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 *
 * @author martin
 */
public class MyBasicConfigPanel extends JPanel {

    private JFileChooser fc;
    private JTextField path;
    private MyButton openButton;

    // private MyCheckBox outDialogCheckBox;
    private MyCheckBox enableRendezvous;
    private MyCheckBox enableAgc;
    private JTextPane rendezvousSeedUris;
    private JTextPane rendezvousUris;

    public MyBasicConfigPanel() {
        super(new GridBagLayout(), false);
        fc = new JFileChooser();
        init();
    }

    public MyCheckBox getEnableAgc() {
        return enableAgc;
    }

    public MyCheckBox getEnableRendezvous() {
        return enableRendezvous;
    }

//    public MyCheckBox getOutDialogCheckBox() {
//        return outDialogCheckBox;
//    }

    public String getConfigFilePath(){
        return this.path.getText();
    }

    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0.15;

        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(15,0,0,0);

        add(new MyTextLabel(GuiConstants.progressPanelStates[0], 27, 22, 200, 30), c);

        c.anchor = GridBagConstraints.CENTER;
//        c.insets = new Insets(0,0,0,0);
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy++;
        c.gridx = 0;
        add(this.enableRendezvous = new MyCheckBox("Rendezvous node", false), c);

//        c.weighty = 0.15;
//        c.gridy = 1;
//        c.weightx = 2;
//        add(this.outDialogCheckBox = new MyCheckBox("Application output dialog", false), c);

//        c.weighty = 0.15;
        c.gridx++;
        add(this.enableAgc = new MyCheckBox("Enable AGC", false), c);

        c.weighty = 0.15;
        c.gridx = 0;
                c.gridy++;
        add(new JLabel("Rendezvous seed URLs"), c);

        c.weighty = 0.15;
        c.gridx++;
        add(this.rendezvousSeedUris = new JTextPane(), c);

        c.weighty = 0.15;
        c.gridx = 0;
        c.gridy++;
        add(new JLabel("Rendezvous URLs"), c);

        c.weighty = 0.15;
        c.gridx++;
        add(this.rendezvousSeedUris = new JTextPane(), c);

        c.gridy++;

        c.weighty = 0.15;
        c.gridx = 0;
                c.gridy++;
        add(new JLabel("Config file:"), c);

        c.gridx++;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(10, 30, 10, 5);
        c.anchor = GridBagConstraints.LAST_LINE_START;
        add(this.path = new JTextField("nodeConfig.json"), c);

        this.openButton = new MyButton("Open file...");
        c.gridx++;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 7, 23);
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        add(openButton, c);

        openButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                openButtonClicked();}
        });

    }

    public void openButtonClicked() {
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File configFile = fc.getSelectedFile();
                path.setText(configFile.getAbsolutePath());
            }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint backgroundColor = new GradientPaint(0, 0, new Color(0, 0, 0),
            getWidth() - 5, getHeight() - 5, new Color(196, 196, 255),
            true);
        g2.setPaint(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);

        paintChildren(g);
        g2.dispose();
    }
}


