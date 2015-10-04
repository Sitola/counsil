package gui;

import gui.constants.GuiConstants;
import mediaAppFactory.MediaApplication;
import networkRepresentation.EndpointNetworkNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ApplicationsSettingPanel extends JPanel {
    private MyButton addButton;
    private MyButton editButton;
    private MyButton removeButton;

    private JScrollPane listScroller;
    private JList applicationsList;
    private DefaultListModel applications;

    private JFrame baseFrame;

    private EndpointNetworkNode node;

    public ApplicationsSettingPanel(JFrame parentFrame) {
        super(new GridBagLayout());
        //setPreferredSize(new Dimension(700, 410));
        listScroller = new JScrollPane(this.applicationsList = new JList());
        applicationsList.setModel(this.applications = new DefaultListModel());
        applicationsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        applicationsList.setAutoscrolls(true);

        this.baseFrame = parentFrame;

        addButton = new MyButton("add");
        editButton = new MyButton("edit");
        removeButton = new MyButton("remove");

        removeButton.setEnabled(false);
        editButton.setEnabled(false);

        this.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addButtonClicked();}
        });
        this.editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editButtonClicked();}
        });
        this.removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeButtonClicked();}
        });

        applicationsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean enabled = (applicationsList.getModel().getSize() != 0 && applicationsList.getSelectedIndex() != -1);
                editButton.setEnabled(enabled);
                removeButton.setEnabled(enabled);}
        });
        init();
    }


    public void setComponentsFromConfig(EndpointNetworkNode node){
        this.applications.clear();
        if(node.getNodeApplications() != null &&  !  node.getNodeApplications().isEmpty()){
            for(MediaApplication ni : node.getNodeApplications()){
                this.applications.addElement(ni);
            }
        }
        
        this.node = node;
    }

    public void setConfigFromComponents(EndpointNetworkNode node){
        node.getNodeApplications().clear();
        for(int i = 0; i < this.applications.size(); i++){
            ((MediaApplication) this.applications.get(i)).setParentNode(node);
            node.getNodeApplications().add(  (MediaApplication) this.applications.get(i));
        }
    }
    

    public void setInterfaces(java.util.List<MediaApplication> application) {
        this.applications.clear();
        for (MediaApplication n : application) {
            this.applications.addElement(n);
        }
    }

    public java.util.List<MediaApplication> getMediaApplications() {
        List<MediaApplication> list = new ArrayList<MediaApplication>(this.applications.getSize());
        for (int i = 0; i < this.applications.getSize(); i++) {
            list.add((MediaApplication) this.applications.get(i));
        }
        return list;
    }

    private void editButtonClicked() {
        int currentIndex = this.applicationsList.getSelectedIndex();
        ApplicationDialog dialog = new ApplicationDialog(this.baseFrame);
        dialog.setMediaApplication((MediaApplication)this.applications.getElementAt(currentIndex));

        dialog.setVisible(true);
        if (dialog.getHasBeenApplicationSet()) {
            applications.set(currentIndex, dialog.getMediaApplication());
        }
        dialog.dispose();
    }

    private void removeButtonClicked() {
        MediaApplication currentApp = (MediaApplication) this.applicationsList.getSelectedValue();

        if (currentApp != null) {
            this.applications.removeElement(currentApp);
            this.applicationsList.setSelectedIndex(-1);
            // node.removeApplication(currentApp);
        }

    }

    private void addButtonClicked() {
        ApplicationDialog applicationDialog = new ApplicationDialog(this.baseFrame);
        applicationDialog.setVisible(true);
        if (applicationDialog.getHasBeenApplicationSet() && applicationDialog.getMediaApplication() != null) {
            this.applications.addElement(applicationDialog.getMediaApplication());
            this.applicationsList.setSelectedIndex(this.applications.getSize() - 1);
        }
        applicationDialog.dispose();
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 0.33;
        c.weighty = 0.2;

        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(15,0,0,0);

        add(new MyTextLabel(GuiConstants.progressPanelStates[3], 27, 22, 200, 30), c);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0,0,0,0);

        c.gridy = 1;
        c.weighty = 0.7;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 25, 5, 25);
        add(this.listScroller, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = 2;
        c.gridwidth = 1;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        add(this.addButton, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        add(this.editButton, c);

        c.gridx = 2;
        c.weightx = 0.34;
        c.anchor = GridBagConstraints.LINE_START;
        add(this.removeButton, c);

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



