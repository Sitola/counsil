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
import javax.swing.JPanel;
import javax.swing.JTextField;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.GeneralNetworkNode;
import utils.GeoLocation;


public class NodeSettingPanel extends JPanel {
    private JTextField nodeNameField;
    private JTextField geoLatitudeField;
    private JTextField geoLongitudeField;
    private JTextField siteNameField;
    
    public NodeSettingPanel() {
        super(new GridBagLayout());
        init();
    }


    public void setComponentsFromConfig(EndpointNetworkNode node) {
        if (node != null) {

            if (node.getNodeName() != null) {
                this.nodeNameField.setText(node.getNodeName());
            }
            if (node.getNodeSite() != null && node.getNodeSite().getSiteName() != null) {
                this.siteNameField.setText(node.getNodeSite().getSiteName());
            }
            this.geoLatitudeField.setText(Double.toString(node.getGeoLocation().getLatitude()));
            this.geoLongitudeField.setText(Double.toString(node.getGeoLocation().getLongitude()));
        }

    }

    public void setConfigFromComponents(EndpointNetworkNode node) {
        if(node != null){
//            node.setUuid(this.nodeUuidField.getText());
           
            if(node.getNodeSite() != null){
                node.getNodeSite().setSiteName(this.siteNameField.getText());
            }
            
            node.setGeoLocation(new GeoLocation(
                Double.parseDouble(this.geoLatitudeField.getText()),  
                Double.parseDouble(this.geoLongitudeField.getText())
            ));
        }
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weighty = 0.2;

        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(15, 0, 0, 0);

        add(new MyTextLabel(GuiConstants.progressPanelStates[1], 27, 22, 200, 30), c);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);

        c.weightx = 0;
        c.weighty = 0.13;
        c.gridwidth = 1;
        c.gridy = 1;
        c.insets = new Insets(5, 5, 5, 5);
        add(new MyTextLabel("Node Name:", 16, 17, 160, 23), c);

        c.gridy = 2;
        add(new MyTextLabel("Network Site:", 16, 17, 160, 23), c);

        c.gridy = 3;
        add(new MyTextLabel("Geo Latitude:", 16, 17, 160, 23), c);

        c.gridy = 4;
        c.gridheight = 2;
        c.insets = new Insets(15, 0, 30, 0);
        add(new MyTextLabel("Geo Longtitude:", 16, 17, 160, 23), c);

        c.gridy = 4;
        c.insets = new Insets(15, 0, 30, 18);
        c.fill = GridBagConstraints.HORIZONTAL;
        add(geoLongitudeField = new JTextField(), c);

        c.gridheight = 1;
        c.gridy = 3;
        c.insets = new Insets(15, 0, 0, 18);
        add(geoLatitudeField = new JTextField(), c);

        c.gridy = 2;
        add(siteNameField = new JTextField(), c);

        c.gridy = 1;
        add(nodeNameField = new JTextField(), c);

/*
        c.weightx = 0.3;
        c.weighty = 0.15;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 6;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        add(this.isLocalNodeCheckBox = new MyCheckBox("Local Node", true), c);
*/    }

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
    
    public String getNodeName() {
        return nodeNameField.getText();
    }
    
}
