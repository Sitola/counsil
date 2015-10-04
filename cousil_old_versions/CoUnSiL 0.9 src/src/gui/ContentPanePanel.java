package gui;

import gui.constants.GuiConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.*;

/**
 *
 * @author martin
 */
public class ContentPanePanel extends JPanel {
    public static GridBagConstraints constraintsOfMainPanel = new GridBagConstraints();
    MyProgressPanel progressPanel;
    MyButtonPanel buttonPanel;
    JComponent mainPanel;

    public ContentPanePanel() {
        super(new GridBagLayout());

        constraintsOfMainPanel.gridx = 1;
        constraintsOfMainPanel.gridy = 1;
        constraintsOfMainPanel.gridwidth = 2;
        constraintsOfMainPanel.fill = GridBagConstraints.BOTH;
        constraintsOfMainPanel.weightx = 0.75;
        constraintsOfMainPanel.weighty = 0.5;

        this.setBackground(Color.white);

        createButtonPanel();        
        createProgressPanel();
        createTitlePanel();
    }


    private void createButtonPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weighty = 0.1;
        c.weightx = 0.25;
        c.fill = GridBagConstraints.BOTH;
        buttonPanel = new MyButtonPanel();
        buttonPanel.setPreferredSize(new Dimension(810, 70));
        this.add(buttonPanel, c);
    }

    private void createProgressPanel() {
        progressPanel = new MyProgressPanel(GuiConstants.progressPanelStates);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = 40;
        c.ipady = 20;
        c.weightx = 0.25;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        this.add(progressPanel, c);
    }

    private void createTitlePanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0;
        MyTitlePanel myTitlePanel = new MyTitlePanel(31);
        myTitlePanel.setPreferredSize(new Dimension(600, 60));
        myTitlePanel.setMinimumSize(new Dimension(600, 60));
        this.add(myTitlePanel, c);
    }

    public void setMainPanel(JPanel panel){
        if(mainPanel != null){
            this.remove(this.mainPanel);
            this.mainPanel.setVisible(false);
        }
        this.mainPanel = panel;
        mainPanel.setPreferredSize(new Dimension(700, 320));        //setPreferredSize(new Dimension(700, 410));
        this.add(panel, constraintsOfMainPanel);
        mainPanel.setVisible(true);
        this.validate();
    }

    public void setMapPanel(JPanel panel){
        if(mainPanel != null){
            this.remove(this.mainPanel);
            this.mainPanel.setVisible(false);
        }
        this.remove(this.progressPanel);
        this.progressPanel = null;
        this.remove(this.buttonPanel);
        this.buttonPanel = null;

        //mainPanel.setPreferredSize(new Dimension(700, 320));
        constraintsOfMainPanel.weightx = 1;
        constraintsOfMainPanel.weighty = 0.6;
        constraintsOfMainPanel.gridx = 0;
        constraintsOfMainPanel.gridy = 1;
        this.add(panel, constraintsOfMainPanel);
        mainPanel.setVisible(true);
        this.validate();
        this.repaint();
    }


}