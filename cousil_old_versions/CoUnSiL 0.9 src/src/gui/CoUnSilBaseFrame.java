/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import core.ControlPeer;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.*;
import myGUI.ControllerFrame;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.NetworkSite;
import networkRepresentation.Session;

/**
 *
 * @author
 * Milan
 */
public class CoUnSilBaseFrame extends JFrame {

    private ControlPeer universePeer;
    private EndpointNetworkNode networkNode;
    private ControllerFrame localControllerFrame;
    private boolean isConnected;
    
    /**
     * Creates
     * new
     * form
     * CustomBaseFrame
     * @param title
     */
        
    public CoUnSilBaseFrame(String title) {
        super(title);
        initManager();
        initComponents();
        this.isConnected = false;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });
        }

        private void initManager(){
            try {
                this.initNetworkNode();
                }
                catch (Exception e){
                System.exit(1);
                }
        }
    /**
     * Initialize GuiUniversePeer using config
     * @throws Exception 
     */
        private void initNetworkNode() throws Exception{
            /*
            try {
                ControlPeer.initialize(true,"../nodeConfig.xml");
                networkNode = GuiUniversePeer.loadNetworkNode();
                                
                } catch (MalformedURLException mue) {
                    
                    JOptionPane.showMessageDialog(this, "The nodeConfig.xml URL is malformed", "",JOptionPane.WARNING_MESSAGE);
                    throw new Exception(mue);
                                        
                } catch (FileNotFoundException e) {
                    
                    JOptionPane.showMessageDialog(this, "Unable to read the nodeConfig.xml", "",JOptionPane.WARNING_MESSAGE);
                    throw new Exception(e);
                    
                } catch (IOException ioe) {
                    
                    JOptionPane.showMessageDialog(this, "Unable to read the nodeConfig.xml", "",JOptionPane.WARNING_MESSAGE);
                    throw new Exception(ioe);
                    
                }
            */
        }
        /**
         * Sets node site name dependant on chosen selection of session
         */
        private void setNodeSiteName(){
            StringBuilder sb = new StringBuilder((String)((Session)jListSessions.getSelectedValue()).getSessionName()).append("_").append(networkNode.getNodeName());
            networkNode.setNodeSite(new NetworkSite(sb.toString()));
        }
        /**
         * Join universe after session selection
         */
        private void onConnect(){
            /*
           this.setTitle(networkNode.getNodeName());
           universePeer = new ControlPeer(networkNode);
           universePeer.joinUniverse(true,networkNode.getRendezvousURI());
           universePeer.saveConfig();
           new RunApplications().execute();
            */
        }       
              
        private void onExit() {
            new LeaveUniverseTask().execute();
            new StopApplicationsTask().execute();
            this.dispose();        
            System.exit(0);
        }

    /**
     * This
     * method
     * is
     * called
     * from
     * within
     * the
     * constructor
     * to
     * initialize
     * the
     * form.
     * WARNING:
     * Do
     * NOT
     * modify
     * this
     * code.
     * The
     * content
     * of
     * this
     * method
     * is
     * always
     * regenerated
     * by
     * the
     * Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jListSessions = new javax.swing.JList(networkNode.getSessions().toArray());
        jButtonSettings = new javax.swing.JButton();
        jButtonConnect = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jListSessions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListSessions.setCellRenderer(new DefaultListCellRenderer () {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (renderer instanceof JLabel && value instanceof Session) {
                    ((JLabel) renderer).setText(((Session) value).getSessionName());
                }
                return renderer;
            }
        });
        jListSessions.setToolTipText("");
        jScrollPane2.setViewportView(jListSessions);

        jButtonSettings.setText("Settings");
        jButtonSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSettingsActionPerformed(evt);
            }
        });

        jButtonConnect.setText("Connect");
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonSettings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonConnect, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonSettings)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonConnect))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSettingsActionPerformed
        RoleSettingPanel roles = new RoleSettingPanel(networkNode);
        roles.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        roles.setVisible(true);
        roles.setMinimumSize(roles.getPreferredSize());
    }//GEN-LAST:event_jButtonSettingsActionPerformed

    private void jButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectActionPerformed
        /*
        if(!isConnected){
            if(jListSessions.isSelectionEmpty()){
                JOptionPane.showMessageDialog(null, "Select which session you want to connect.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            else{
                networkNode.setNodeName();
                this.setNodeSiteName();
                this.onConnect();
                this.isConnected=true;
                this.jButtonConnect.setText("Disconnect");
            }
        }
        else{
            new StopApplicationsTask().execute();
            new LeaveUniverseTask().execute();
            localControllerFrame.dispose();
            this.isConnected=false;
            this.jButtonConnect.setText("Connect");
            }
        */
    }//GEN-LAST:event_jButtonConnectActionPerformed
           
    /**
     * @param
     * args
     * the
     * command
     * line
     * arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JButton jButtonSettings;
    private javax.swing.JList jListSessions;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables

            
        class RunApplications extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            universePeer.runApplications(localControllerFrame = new ControllerFrame());
            localControllerFrame.setVisible(true);
            return null;
        }
        @Override
        public void done() {
        }
        }
        
        class StopApplicationsTask extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            /*
            if (universePeer != null) {
                universePeer.stopApplications();
            }
                */
            return null;
                
        }
        @Override
        public void done(){
        }
        }
        
        class LeaveUniverseTask extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            if(universePeer != null){
                universePeer.leaveUniverse();
            }
            return null;
        }
        @Override
        public void done() {}
        }

}
