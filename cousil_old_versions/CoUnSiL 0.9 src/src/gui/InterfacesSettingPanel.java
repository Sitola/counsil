package gui;

import gui.constants.GuiConstants;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointNodeInterface;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class InterfacesSettingPanel extends JPanel {
    private MyButton addButton;
    private MyButton editButton;
    private MyButton removeButton;

    private JScrollPane listScroller;
    private JList interfacesList;
    private DefaultListModel interfaces;

    private JFrame baseFrame;
    
    EndpointNetworkNode node;


    public InterfacesSettingPanel(JFrame parentFrame) {
        super(new GridBagLayout());
        listScroller = new JScrollPane(this.interfacesList = new JList());
        interfacesList.setModel(this.interfaces = new DefaultListModel());
        interfacesList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        interfacesList.setAutoscrolls(true);

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

        interfacesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean enabled = (interfacesList.getModel().getSize() != 0 && interfacesList.getSelectedIndex() != -1);
                editButton.setEnabled(enabled);
                removeButton.setEnabled(enabled);}
        });
        init();
    }


    public void setComponentsFromConfig(EndpointNetworkNode node){
        this.interfaces.clear();
        if(node.getNodeInterfaces() != null &&  !  node.getNodeInterfaces().isEmpty()){
            for (EndpointNodeInterface ni : node.getNodeInterfaces()){
                this.interfaces.addElement(ni);
            }
        }
    }

    public void setConfigFromComponents(EndpointNetworkNode node){
        node.getNodeInterfaces().clear();
        for(int i = 0; i < this.interfaces.size(); i++){
            node.getNodeInterfaces().add(  (EndpointNodeInterface) this.interfaces.get(i));
        }
    }


    /*
    public void setInterfaces(List<NodeInterface> interfaces) {
        this.interfaces.clear();
        for (NodeInterface n : interfaces) {
            this.interfaces.addElement(n);
        }
    }

    public List<NodeInterface> getInterfaces() {
        List<NodeInterface> list = new ArrayList<NodeInterface>(this.interfaces.getSize());
        for (int i = 0; i < this.interfaces.getSize(); i++) {
            list.add((NodeInterface) this.interfaces.get(i));
        }
        return list;
    }
    */

    private void editButtonClicked() {
        int currentIndex = this.interfacesList.getSelectedIndex();
        InterfaceDialog dialog = new InterfaceDialog(this.baseFrame);
        dialog.setInterface((EndpointNodeInterface)this.interfaces.getElementAt(currentIndex));

        dialog.setVisible(true);
        if (dialog.getHasBeenInterfaceSet()) {
            interfaces.set(currentIndex, dialog.getInterface());
        }
        dialog.dispose();
    }

    private void removeButtonClicked() {
        EndpointNodeInterface iface = (EndpointNodeInterface) this.interfacesList.getSelectedValue(); 
        
        if (iface != null) {
            this.interfaces.removeElement(iface);
            this.interfacesList.setSelectedIndex(-1);        
            // this.node.removeInterface(iface);
        }

    }

    private void addButtonClicked() {
        InterfaceDialog interfaceDialog = new InterfaceDialog(this.baseFrame);
        interfaceDialog.setVisible(true);
        if (interfaceDialog.getHasBeenInterfaceSet() && interfaceDialog.getInterface() != null) {
            this.interfaces.addElement(interfaceDialog.getInterface());
            this.interfacesList.setSelectedIndex(this.interfaces.getSize() - 1);
        }
        interfaceDialog.dispose();
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

        add(new MyTextLabel(GuiConstants.progressPanelStates[2], 27, 22, 200, 30), c);

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
