/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import core.UiStub;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.swing.DefaultComboBoxModel;
import javax.swing.UIManager;
import networkRepresentation.EndpointNetworkNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author luksoft
 */
public class SimpleSwitcher extends javax.swing.JFrame {
    private static class AppEntry {

        public AppEntry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
        
        
        public String uuid;
        public String name;

        @Override
        public int hashCode() {
            return uuid.hashCode()*name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof AppEntry)) return false;
            
            AppEntry e = (AppEntry)o;
            
            return e.uuid.equals(uuid) && name.equals(e.name);
        }
        
        public String toString() {
            return name + " (" + uuid + ")";
        }
    }
    
    private static class EntryComparator implements Comparator<AppEntry> {
        @Override
        public int compare(AppEntry t, AppEntry t1) {
            return t1.toString().compareTo(t1.toString());
        }
    }

    private void setSelectedSite(String appUuid){
        String selectedSite = siteForApp.get(appUuid);
        if (selectedSite == null) return;
        
        cboxSites.setSelectedIndex(modelApps.getIndexOf(selectedSite));
        revalidate();
    }
    
    private class AppSelectionListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent ie) {
            setSelectedSite(modelApps.getElementAt(cboxApps.getSelectedIndex()).uuid);
        }
    }
    private class SelectedListener implements KeyListener {

        @Override
        public void keyReleased(KeyEvent ke) {
            if (ke.getKeyCode() != KeyEvent.VK_ENTER) return;
            
            if (cboxApps.getSelectedIndex() < 0) return;
            if (cboxApps.getSelectedIndex() >= modelApps.getSize()) return;

            if (cboxSites.getSelectedIndex() < 0) return;
            if (cboxSites.getSelectedIndex() >= modelSites.getSize()) return;


            String commandSet = "set config .applications." + modelApps.getElementAt(cboxApps.getSelectedIndex()).uuid+".sourceSite \"" + modelSites.getElementAt(cboxSites.getSelectedIndex()) + "\"";
            uiTasks.addTask(new GuiTask() {
                String command;

                GuiTask setCommand(String cmd) { command = cmd; return this; }

                @Override
                public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
                    uiEndpoint.swallowRest();
                    uiEndpoint.getOutput().println(command);

                    if (!uiEndpoint.awaitResponse()) {
                        statusbarError("Timeout for reply has exceeded!");
                        return;
                    }

                    JSONObject response = uiEndpoint.processInput();
                    if (!confirmState(response, UiStub.StatusOk)) {
                        if (response.has(UiStub.KeyMessage)) {
                            statusbarError(response.optString(UiStub.KeyMessage, ""));
                        } else {
                            statusbarError("Server-side user interface generated an uncommented error.");
                        }
                        return;
                    }

                    if (response.has(UiStub.KeyMessage)) {
                        statusbarInfo(response.optString(UiStub.KeyMessage, ""));
                    }

                    uiEndpoint.getOutput().println("commit");
                    if (!uiEndpoint.awaitResponse()) {
                        statusbarError("Timeout for reply has exceeded!");
                        return;
                    }

                    response = uiEndpoint.processInput();
                    if (!confirmState(response, UiStub.StatusOk)) {
                        if (response.has(UiStub.KeyMessage)) {
                            statusbarError(response.optString(UiStub.KeyMessage, ""));
                        } else {
                            statusbarError("Server-side user interface generated an uncommented error.");
                        }
                        return;
                    }

                    if (response.has(UiStub.KeyMessage)) {
                        statusbarInfo(response.optString(UiStub.KeyMessage, ""));
                    }
                }
            }.setCommand(commandSet));            
        }

        @Override
        public void keyTyped(KeyEvent ke) {
            return;
        }

        @Override
        public void keyPressed(KeyEvent ke) {
            return;
        }
    
    }
    
    DefaultComboBoxModel<AppEntry> modelApps = new DefaultComboBoxModel<>();
    DefaultComboBoxModel<String> modelSites = new DefaultComboBoxModel<>();
    HashMap<String, String> siteForApp = new HashMap<String, String>();
    
    private boolean confirmState(JSONObject response, String state) throws JSONException {
        return (response.getString(UiStub.KeyStatus).equals(state));
    }
    
    private void updateAppsModel(Set<AppEntry> data){
        Iterator<AppEntry> newSitesIterator = data.iterator();
        AppEntry newSite = null;
        boolean shouldAdvance = true;

        for (int i = 0; i < modelApps.getSize(); ) {
            if (shouldAdvance) {
                if (newSitesIterator.hasNext()) {
                    newSite = newSitesIterator.next();
                    shouldAdvance = false;
                } else {
                    modelApps.removeElement(i);
                    continue;
                }
            }

            if (!data.contains(modelApps.getElementAt(i))) {
                modelApps.removeElementAt(i);
            } else if (modelApps.getElementAt(i).equals(newSite)) {
                ++i;
                shouldAdvance = true;
                newSite = null;
                continue;
            } else if (modelApps.getElementAt(i).toString().compareTo(newSite.toString()) < 0) {
                ++i;
                continue;
            } else {
                modelApps.insertElementAt(newSite, i);
                shouldAdvance = true;
                newSite = null;
            }
        }

        if (newSite != null) {
            modelApps.addElement(newSite);
        }
        while (newSitesIterator.hasNext()) {
            modelApps.addElement(newSitesIterator.next());
        }

        cboxApps.setModel(modelApps);
        
        if ((cboxApps.getSelectedIndex() < 0) && (modelApps.getSize() > 0)) {
            cboxApps.setSelectedIndex(0);
            setSelectedSite(modelApps.getElementAt(0).uuid);
        }
    }
    
    private void updateSitesModel(Set<String> data){
        Iterator<String> newSitesIterator = data.iterator();
        String newSite = null;
        boolean shouldAdvance = true;

        for (int i = 0; i < modelSites.getSize(); ) {
            if (shouldAdvance) {
                if (newSitesIterator.hasNext()) {
                    newSite = newSitesIterator.next();
                    shouldAdvance = false;
                } else {
                    modelSites.removeElement(i);
                    continue;
                }
            }

            if (!data.contains(modelSites.getElementAt(i))) {
                modelSites.removeElementAt(i);
            } else if (modelSites.getElementAt(i).equals(newSite)) {
                ++i;
                shouldAdvance = true;
                newSite = null;
                continue;
            } else if (modelSites.getElementAt(i).toString().compareTo(newSite.toString()) < 0) {
                ++i;
                continue;
            } else {
                modelSites.insertElementAt(newSite, i);
                shouldAdvance = true;
                newSite = null;
            }
        }

        if (newSite != null) {
            modelSites.addElement(newSite);
        }
        while (newSitesIterator.hasNext()) {
            modelSites.addElement(newSitesIterator.next());
        }
        
        cboxSites.setModel(modelSites);
    }
    
    private interface GuiTask {
        void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException;
    }
    private class GuiTasker extends Thread {
        private BlockingQueue<GuiTask> taskQueue = new LinkedBlockingDeque<GuiTask>();
        private UiStub.UiChannelEndpoint uiEndpoint;
        
        private class Poision implements GuiTask {
            @Override
            public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }
        
        public GuiTasker(UiStub.UiChannelEndpoint uiEndpoint) {
            this.uiEndpoint = uiEndpoint;
        }
        
        public void addTask(GuiTask task) {
            taskQueue.add(task);
        }
        public void addUniqueTask(GuiTask task) {
            if (!taskQueue.contains(task)) {
                taskQueue.add(task);
            }
        }
        
        public void poisonWorker(){
            taskQueue.add(new Poision());
        }
        
        @Override
        public void run() {
            while (true) {
                GuiTask nextTask = null;
                try {
                    nextTask = taskQueue.take();
                } catch (InterruptedException ex) {
                    continue;
                }
                
                if (nextTask instanceof Poision) {
                    return;
                }
                
                try {
                    nextTask.execute(uiEndpoint);
                } catch (IOException | JSONException ex) {
                    Logger.getLogger(this.getClass()).error("Failed running task of " + nextTask.getClass().getCanonicalName() + " reason: "+ex.toString());
                } catch (NullPointerException ex) {
                    Logger.getLogger(this.getClass()).fatal("\n\nBUG - gui encountered NullPointerExcepiton: " + ex.toString());
                }
            }
        }
    }
    private class NodeCacheUpdaterTask implements GuiTask {
        public static final int UpdateInterval = 300; // miliseconds
        
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("show universe");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, response.getString(UiStub.KeyStatus));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Server-side user interface generated an uncommented error.");
                }
                return;
            }

            JSONObject universe = null;
            JSONObject currentNetworkNodes = null;
            try {
                universe = response.getJSONObject(UiStub.KeyResult);
                currentNetworkNodes = universe.getJSONObject("nodes");
            } catch (JSONException ex) {
                Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.ERROR, "Received response does not contain JSON-represented result!", ex);
                return;
            }
            
            processNodes(currentNetworkNodes);
        }
        
        void processNodes(JSONObject nodes) {
            TreeSet<String> nodeSites = new TreeSet<>();
            
            Iterator<String> nodeKeys = nodes.keys();
            while (nodeKeys.hasNext()) {
                String nodeid = nodeKeys.next();
                JSONObject nodeEntry = null;
                try {
                    nodeEntry = nodes.getJSONObject(nodeid);
                } catch (JSONException ex) {
                    ;
                } finally {
                    if (nodeEntry == null) continue;
                }
                
                String sitename = nodeEntry.optString(EndpointNetworkNode.ConfigKeySiteName, null);
                if (sitename == null) continue;
                nodeSites.add(sitename);
            }
            updateSitesModel(nodeSites);
        }
    }
    private class AppCacheUpdaterTask implements GuiTask {
        public static final int UpdateInterval = 300; // miliseconds
        
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint) throws IOException, JSONException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("show applications");

            if (!uiEndpoint.awaitResponse()) {
                throw new IOException("Timeout for reply has exceeded!");
            }

            JSONObject response = uiEndpoint.processInput();

            if (!confirmState(response, UiStub.StatusOk)) {
                if (response.has(UiStub.KeyMessage)) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, response.getString(UiStub.KeyStatus));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Server-side user interface generated an uncommented error.");
                }
                return;
            }

            JSONObject applications = null;
            try {
                applications = response.getJSONObject(UiStub.KeyResult);
            } catch (JSONException ex) {
                Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.ERROR, "Received response does not contain JSON-represented result!", ex);
                return;
            }
            
            processApps(applications);
        }
        
        void processApps(JSONObject applications) {
            TreeSet<AppEntry> apps = new TreeSet<>(new EntryComparator());
            Iterator<String> appKeys = applications.keys();
            while (appKeys.hasNext()) {
                try {
                    String uuid = appKeys.next();
                    JSONObject appEntry = applications.getJSONObject(uuid);
                    appEntry = appEntry.getJSONObject("requested");
                    String selectedSource = appEntry.optString("sourceSite", null);
                    if (selectedSource == null) return;
                    
                    siteForApp.put(uuid, selectedSource);
                    apps.add(new AppEntry(uuid, appEntry.optString("name", "Unidentified")));
                    
                } catch (JSONException ex) {
                    continue;
                }
            }
            updateAppsModel(apps);
        }
    }
    private class ShutdownTask implements GuiTask {
        @Override
        public void execute(UiStub.UiChannelEndpoint uiEndpoint)throws JSONException, IOException {
            uiEndpoint.swallowRest();
            uiEndpoint.getOutput().println("shutdown");
            if (uiEndpoint.awaitResponse()) {
                status(uiEndpoint.processInput());
            } else {
                Logger.getLogger("Failed to receive reply from connected CoUniverse");
            }
            SimpleSwitcher.this.uiTasks.poisonWorker();
            SimpleSwitcher.this.dispose();
        }
    }
    
    public SimpleSwitcher(InputStream fromStub, OutputStream toStub) {
        initComponents();
        this.uiTasks = new GuiTasker(
            new UiStub.UiChannelEndpoint(
                new BufferedReader(new InputStreamReader(fromStub)), 
                new PrintStream(toStub)));
    }
    
    void status(JSONObject reply) {
        try {
            if (confirmState(reply, UiStub.StatusOk)) {
                statusbarInfo(reply.optString(UiStub.KeyMessage, "Done"));
            } else {
                statusbarInfo(reply.optString(UiStub.KeyMessage, "Error executing command"));
            }
        } catch (JSONException error) {
            statusbarError("Error parsing server response");
            Logger.getLogger(this.getClass()).error("BUG: Received reply does not contain requred filed " + UiStub.KeyStatus + ": " + reply.toString());
        }
    }
    void statusbarError(String message) {
        statusbar.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        statusbar.setText(message);
    }
    void statusbarInfo(String message) {
        statusbar.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        statusbar.setText(message);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            statusbarInfo("Commit changes by pressing enter while site combobox is focused");
            uiTasks.start();
            // start periodic updater
            new Thread() {
                @Override
                public void run() {
                    boolean previousState = false;
                    
                    while ((previousState && SimpleSwitcher.this.isVisible()) || !previousState) {
                        previousState = SimpleSwitcher.this.isVisible();
                        uiTasks.addUniqueTask(new NodeCacheUpdaterTask());
                        uiTasks.addUniqueTask(new AppCacheUpdaterTask());
                        try {
                            Thread.sleep(NodeCacheUpdaterTask.UpdateInterval);
                        } catch (InterruptedException ex) {
                            continue;
                        }
                    }
                }
            }.start();
        } else {
            uiTasks.poisonWorker();
        }
//            statusbarInfo("Connected");
        super.setVisible(visible);
    }

        /**
         * This method is called from within the constructor to initialize the
         * form. WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
        @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        statusbar = new javax.swing.JLabel();
        cboxApps = new javax.swing.JComboBox();
        cboxSites = new javax.swing.JComboBox();

        setTitle("CoUniverse");
        setAutoRequestFocus(false);
        setBounds(new java.awt.Rectangle(0, 0, 0, 0));
        setName("mainFrame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        statusbar.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        statusbar.setText("Starting & loading configuration...");

        cboxApps.setModel(modelApps);
        cboxApps.addItemListener(new AppSelectionListener());

        cboxSites.setModel(modelSites);
        cboxSites.addKeyListener(new SelectedListener());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusbar, javax.swing.GroupLayout.DEFAULT_SIZE, 927, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cboxApps, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboxSites, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cboxApps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cboxSites, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statusbar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        uiTasks.addTask(new ShutdownTask());
    }//GEN-LAST:event_formWindowClosed

    public void setupLookAndFeel() {
            /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
    * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
    */
    try {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                javax.swing.UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (ClassNotFoundException ex) {
            Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.DEBUG, null, ex);
    } catch (InstantiationException ex) {
            Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.DEBUG, null, ex);
    } catch (IllegalAccessException ex) {
            Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.DEBUG, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SimpleSwitcher.class.getName()).log(Level.DEBUG, null, ex);
    }
    //</editor-fold>
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox cboxApps;
    private javax.swing.JComboBox cboxSites;
    private javax.swing.JLabel statusbar;
    // End of variables declaration//GEN-END:variables

    private GuiTasker uiTasks = null;
}
