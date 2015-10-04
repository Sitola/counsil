package gui;


import networkRepresentation.EndpointNodeInterface;
import networkRepresentation.LambdaLinkEndPoint;
import networkRepresentation.LambdaLinkEndPointNSI2;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class InterfaceDialog extends JDialog {
    private MyButton okButton;
    private MyButton cancelButton;

    private MyCheckBox duplexCheckBox;

    private JTextField deviceNameField;
    private JTextField IPv4field;
    private JTextField IPv4MaskField;
    private JTextField subnetField;
    private JTextField bandwidthField;

    private JTextField lambdaLinkEndpointNetworkId;
    private JTextField lambdaLinkEndpointLocalId;

    private MyCheckBox taggedCheckBox;
    private JTextField lambdaLinkEndpointVlanField;

    private EndpointNodeInterface nodeInterface = null;
    private LambdaLinkEndPoint lambdaLinkEndPoint = null;

    private boolean hasBeenInterfaceSet = false;
    //private MyTextLabel textLabel;


    public InterfaceDialog(Frame frame) {
        super(frame, true);
        this.setSize(new Dimension(600, 400));
        this.setMyContentPanePanel();


        this.setTitle("Add Interface dialog");
        this.okButton = new MyButton("OK");
        this.cancelButton = new MyButton("Cancel");
        this.getRootPane().setDefaultButton(this.okButton);
        this.okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonClicked();
            }
        });
        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cancelButtonClicked();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelButtonClicked();
            }
        });
        init();
    }

    private void cancelButtonClicked() {
        this.setVisible(false);
    }


    private void okButtonClicked() {
        if (this.nodeInterface == null) {
            this.nodeInterface = new EndpointNodeInterface();
        }

        if ("".equals(this.deviceNameField.getText())) {
            JOptionPane.showMessageDialog(this, "Device name cannot be emtpy");
            return;
        } else {
            this.nodeInterface.setNodeInterfaceName(deviceNameField.getText());
        }
        
        if ("".equals(this.IPv4field.getText())) {
            JOptionPane.showMessageDialog(this, "IP cannot be emtpy");
            return;
        } else {
            this.nodeInterface.setIpAddress(IPv4field.getText());
        }

        if ("".equals(this.IPv4MaskField.getText())) {
            JOptionPane.showMessageDialog(this, "IP mask cannot be emtpy");
            return;
        } else {
            this.nodeInterface.setNetMask(IPv4MaskField.getText());
        }
        
        if ("".equals(this.subnetField.getText())) {
            JOptionPane.showMessageDialog(this, "Subnetwork cannot be emtpy");
            return;
        } else {
            this.nodeInterface.setSubnet(subnetField.getText());
        }

        double bandwidth;
        try {
            bandwidth = Double.parseDouble(this.bandwidthField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Bandwidth has wrong format");
            return;
        }
        if (bandwidth < 0 || bandwidth > 100000000000L) {
            JOptionPane.showMessageDialog(this, "Bandwidth is in impossible scale");
            return;
        }
        this.nodeInterface.setBandwidth(bandwidth);
        this.nodeInterface.setFullDuplex(this.duplexCheckBox.isSelected());


        if (!this.lambdaLinkEndpointNetworkId.getText().equals("")) {
            if (lambdaLinkEndPoint == null) {
                lambdaLinkEndPoint = new LambdaLinkEndPointNSI2(this.lambdaLinkEndpointNetworkId.getText(), this.lambdaLinkEndpointLocalId.getText(), this.taggedCheckBox.isSelected(), this.lambdaLinkEndpointVlanField.getText());
            } else {
                LambdaLinkEndPointNSI2 lambdaLinkEndPointNSI2 = (LambdaLinkEndPointNSI2) lambdaLinkEndPoint;
                lambdaLinkEndPointNSI2.setLambdaLinkNetworkId(this.lambdaLinkEndpointNetworkId.getText());
                lambdaLinkEndPointNSI2.setLambdaLinkLocalId(this.lambdaLinkEndpointLocalId.getText());
                lambdaLinkEndPoint.setLambdaLinkEndpointTagged(this.taggedCheckBox.isSelected());
                lambdaLinkEndPoint.setLambdaLinkEndpointVlan(this.lambdaLinkEndpointVlanField.getText());
            }
        } else {
            lambdaLinkEndPoint = null;
        }

        this.nodeInterface.setLambdaLinkEndpoint(lambdaLinkEndPoint);

        this.hasBeenInterfaceSet = true;
        this.setVisible(false);
    }


    public void setInterface(EndpointNodeInterface ni) {
        this.nodeInterface = ni;
        if (ni != null) {
            if (ni.getNodeInterfaceName() != null) {
                this.deviceNameField.setText(ni.getNodeInterfaceName());
            }
            
            if (ni.getIpAddress() != null) {
                this.IPv4field.setText(ni.getIpAddress());
            }
            
            if (ni.getNetMask() != null) {
                this.IPv4MaskField.setText(ni.getNetMask());
            }
            
            if (ni.getSubnet() != null) {
                this.subnetField.setText(ni.getSubnet());
            }

            if (ni.getLambdaLinkEndpoint() != null) {
                LambdaLinkEndPoint lambdaEndPoint =  ni.getLambdaLinkEndpoint();
                if (lambdaEndPoint instanceof LambdaLinkEndPointNSI2) {
                    LambdaLinkEndPointNSI2 lambdaLinkEndPointNSI2 = (LambdaLinkEndPointNSI2) lambdaEndPoint;
                    this.lambdaLinkEndpointNetworkId.setText(lambdaLinkEndPointNSI2.getLambdaLinkNetworkId());
                    this.lambdaLinkEndpointLocalId.setText(lambdaLinkEndPointNSI2.getLambdaLinkLocalId());
                    this.taggedCheckBox.setSelected(lambdaLinkEndPointNSI2.isLambdaLinkEndpointTagged());
                    this.lambdaLinkEndpointVlanField.setText(lambdaLinkEndPointNSI2.getLambdaLinkEndpointVlan());
                }
            }

            this.bandwidthField.setText(Double.toString(ni.getBandwidth()));
            this.duplexCheckBox.setSelected(ni.isFullDuplex());
        }
    }

    public boolean getHasBeenInterfaceSet() {
        return this.hasBeenInterfaceSet;
    }

    public EndpointNodeInterface getInterface() {
        return this.nodeInterface;
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.2;
        c.weighty = 0.2;
        c.gridwidth = 2;

        this.getContentPane().add(new MyTextLabel("New Interface", 27, 22, 200, 30), c);

        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 0.10;
        c.anchor = GridBagConstraints.EAST;
        this.getContentPane().add(new MyTextLabel("Device Name:", 16, 17), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("SubNet Name:", 16, 17), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("IPv4 address:", 16, 17), c);
        
        c.gridy++;
        this.getContentPane().add(new MyTextLabel("IPv4 mask:", 16, 17), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Bandwidth:", 16, 17), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Lambda Network ID:", 16, 17), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Lambda Local ID:", 16, 17), c);

        int gridMax = c.gridy;

        c.insets = new Insets(0, 10, 5, 20);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.8;
        c.gridx = 1;
        c.gridy = 0;

        c.gridy++;
        this.getContentPane().add(deviceNameField = new JTextField(), c);
        c.gridy++;
        this.getContentPane().add(subnetField = new JTextField("world"), c);
        c.gridy++;
        this.getContentPane().add(IPv4field = new JTextField("127.0.0.1"), c);
        c.gridy++;
        this.getContentPane().add(IPv4MaskField = new JTextField("255.0.0.0"), c);
        c.gridy++;
        this.getContentPane().add(bandwidthField = new JTextField("0"), c);
        c.gridy++;
        this.getContentPane().add(lambdaLinkEndpointNetworkId = new JTextField(), c);
        c.gridy++;
        this.getContentPane().add(lambdaLinkEndpointLocalId = new JTextField(), c);
        
        c.weighty = 0.15;
        c.gridx = 1;
        c.gridy = gridMax + 1;
        c.gridwidth = 2;
        c.weightx = 0.3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        this.getContentPane().add(taggedCheckBox = new MyCheckBox("Lambda EndPoint Tagged", false), c);

        c.weighty = 0.10;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0.2;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.getContentPane().add(new MyTextLabel("Lambda End Point VLAN:", 16, 17), c);

        c.gridx = 1;
        c.weightx = 1.0 - c.weightx;
        this.getContentPane().add(lambdaLinkEndpointVlanField = new JTextField(), c);

        taggedCheckBox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                lambdaLinkEndpointVlanField.setEnabled(taggedCheckBox.isSelected());
            }
        });
        lambdaLinkEndpointVlanField.setEnabled(taggedCheckBox.isSelected());

        c.weighty = 0.15;
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0.3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        this.getContentPane().add(duplexCheckBox = new MyCheckBox("full duplex", true), c);

        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        this.getContentPane().add(this.okButton, c);

        c.insets = new Insets(0, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_END;
        this.getContentPane().add(this.cancelButton, c);
    }


    private void setMyContentPanePanel() {
        this.setContentPane(new JPanel(new GridBagLayout()) {
            @Override
            public void paint(Graphics g) {
                this.setPreferredSize(InterfaceDialog.this.getSize());

                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(Color.white);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint backgroundColor = new GradientPaint(0, 0, new Color(0, 0, 0),
                        getPreferredSize().width - 5, getPreferredSize().height - 5, new Color(196, 196, 255),
                        true);
                g2.setPaint(backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                paintChildren(g2);
                g2.dispose();
            }
        });
    }
}
