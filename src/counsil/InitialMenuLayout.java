/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import org.jnativehook.NativeHookException;
import wddman.WDDManException;

/**
 *
 * @author xminarik
 */
public class InitialMenuLayout extends JFrame {
    
   //String ipAddress;
    JTextField errorMessageField;
    JPanel roomPanel;
    String[] layoutArray;
    ButtonGroup roomGroup;
    
    Position position;
    private final Font font;
    private final WebClient webClient;
    int port;
    String ipAddress;
    
    private final JFrame serverChooseWindow;
    private final JFrame settingRoomWindow;
    private final JFrame errorWindow;
    private final JFrame ipSettingWindow;
    
    private OptionsMainMenuWindow optionMainMenuWindow;
        
    //JSONObject roomList;
    JSONObject roomConfiguration;
    JSONObject clientConfig;

    String roomName;
    String nameList[];
    
    JFormattedTextField ipField[];
    Integer ipValue[];
    
    File configurationFile;
    
    /**
     * to init and end couniverse part of counsil
     */
    SessionManager sm;
    
    class JTextFieldLimit extends PlainDocument {
        private int limit;
        JTextFieldLimit(int limit) {
          super();
          this.limit = limit;
        }

        @Override
        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
          if (str == null)
            return;

          if ((getLength() + str.length()) <= limit) {
            super.insertString(offset, str, attr);
          }
        }
    }
    
    InitialMenuLayout(Position cenerPosition, File clientConfigurationFile) {
        webClient = new WebClient();
        
        errorMessageField = null;
        roomPanel = null;
        layoutArray = new String[1];
        layoutArray[0] = "layoutConfigStatic";
        roomGroup = new ButtonGroup();
        
        roomName = "none";
        //roomList = null;
        font = new Font("Tahoma", 0, 18);
        roomConfiguration = null;
        
        serverChooseWindow = new JFrame();
        settingRoomWindow = new JFrame();
        errorWindow = new JFrame();
        ipSettingWindow = new JFrame();
        
        optionMainMenuWindow = null;
        configurationFile = clientConfigurationFile;
        
        //OptionsMainMenuWindow a = new OptionsMainMenuWindow(font, new Font("Tahoma", 0, 13), clientConfigurationFile);
        position = cenerPosition;
        
        clientConfig = readJsonFile(clientConfigurationFile);
        port = 8080; //defoult value change in future
        ipAddress = "";
        nameList = null;
        
        initSettingRoomWindow();
        initServerChooseWindow();
        initIpSettingWindow();
        initErrorWindow();
        openServerChooseWindow();
    }
    
    final void initServerChooseWindow(){
        if(serverChooseWindow == null){
            return;
        }
        serverChooseWindow.getContentPane().removeAll();
        serverChooseWindow.setTitle("CoUnSil");
        serverChooseWindow.setVisible(false);
        
        JSONArray ipAddresses;
        JButton[] buttonAdresses = null;
        JPanel mainPanel;
        if(clientConfig.has("server ips")){
            try {
                ipAddresses = clientConfig.getJSONArray("server ips");
                buttonAdresses = new JButton[ipAddresses.length()];
                for(int i=0;i<ipAddresses.length();i++){
                    String buttonServerName = ipAddresses.getJSONObject(i).getString("name");
                    String serverIP = ipAddresses.getJSONObject(i).getString("ip");
                    String serverPortString = ipAddresses.getJSONObject(i).getString("port");
                    
                    buttonAdresses[i] = new JButton(buttonServerName);
                    buttonAdresses[i].setFont(font);
                    buttonAdresses[i].addActionListener((ActionEvent event) -> {
                        if(optionMainMenuWindow != null){
                            optionMainMenuWindow.discardAction();
                            optionMainMenuWindow = null;
                        }
                        int serverPort;
                        try{
                            serverPort = Integer.valueOf(serverPortString);
                        }catch(NumberFormatException error){
                            serverPort = 80; //defout value
                        }
                        port = serverPort;
                        JSONObject roomListNames = getServerRoomList();
                        if(roomListNames != null){
                            ipAddress = serverIP;
                            openSettingRoomWindow(roomListNames);
                        }else{
                            openErrorWindow("problem to connect to server, check if server is running");
                        }
                    });
                }
            } catch (JSONException ex) {
                Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(6, 1));
        if(buttonAdresses != null){
            mainPanel.setLayout(new GridLayout(buttonAdresses.length + 3, 1));
            for (JButton buttonAdresse : buttonAdresses) {
                mainPanel.add(buttonAdresse);
            }
        }else{
            mainPanel.setLayout(new GridLayout(2, 1));
        }
        JButton differentServerButton = new JButton("different");
        differentServerButton.setFont(font);
        differentServerButton.addActionListener((ActionEvent event) -> {
            openIpSettingWindow();
            //close this window and open window to set own ip addres
        });
        JButton optionsButton = new JButton("options");
        optionsButton.setFont(font);
        optionsButton.addActionListener((ActionEvent event) -> {
            optionMainMenuWindow = new OptionsMainMenuWindow(font, new Font("Tahoma", 0, 13), configurationFile, this);
        });
        JButton exitButton = new JButton("exit");
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });
        mainPanel.add(differentServerButton);
        mainPanel.add(optionsButton);
        mainPanel.add(exitButton);
        serverChooseWindow.getContentPane().add(mainPanel);
        serverChooseWindow.pack();
        serverChooseWindow.setLocation(position.x - serverChooseWindow.getWidth()/2, position.y - serverChooseWindow.getHeight()/2);
    }
    
    final void initErrorWindow(){        
        if(errorWindow == null){
            return;
        }
        errorWindow.setTitle("CoUnSil");
        errorWindow.setVisible(false);
        
        JPanel jButtonPanel = new JPanel();
        JPanel jMainPanel = new JPanel();
        jMainPanel.setLayout(new BorderLayout());
        errorMessageField = new JTextField("nedokumentovana chyba");
        errorMessageField.setFont(font);
        errorMessageField.setEditable(false);
        JButton okButton = new JButton("OK");
        okButton.setFont(font);
        okButton.addActionListener((ActionEvent e) -> {
            openServerChooseWindow();
            //go to choose server window
        });
        jMainPanel.add(errorMessageField, BorderLayout.NORTH);
        jButtonPanel.add(okButton);
        jMainPanel.add(okButton, BorderLayout.SOUTH);
        errorWindow.getContentPane().add(jMainPanel);
        
        errorWindow.pack();
        errorWindow.setLocation(position.x - errorWindow.getWidth() / 2, position.y - errorWindow.getHeight());
    }
    
    final void  initSettingRoomWindow(){
        if(settingRoomWindow == null){
            return;
        }
        settingRoomWindow.setTitle("CoUnSil");
        settingRoomWindow.setVisible(false);
        
        //create panels, room panel is declared globaly to be able simply change rooms as download from server
        JPanel rolePanel, layoutPanel, actionPanel, mainPanel, audioPanel, namePanel;
        rolePanel = new JPanel();
        rolePanel.setLayout(new GridLayout(3, 1));
        layoutPanel = new JPanel();
        layoutPanel.setLayout(new GridLayout(layoutArray.length, 1));
        roomPanel = new JPanel();
        //room layout will be set when we know how many room there is
        actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayout(4, 1));
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        audioPanel = new JPanel();
        audioPanel.setLayout(new GridLayout(1,1));
        namePanel = new JPanel();
        namePanel.setLayout(new GridLayout(1,1));
        
        
        ButtonGroup layoutGroup =  new ButtonGroup();
        ButtonGroup roleGroup = new ButtonGroup();
        
        //create buttons
        //role
        JRadioButton studentButton = new JRadioButton("student");
        studentButton.setFont(font);
        JRadioButton teacherButton = new JRadioButton("teacher");
        teacherButton.setFont(font);
        JRadioButton interpreterButton = new JRadioButton("interpreter");
        interpreterButton.setFont(font);
        roleGroup.add(studentButton);
        roleGroup.add(teacherButton);
        roleGroup.add(interpreterButton);
        roleGroup.setSelected(studentButton.getModel(), true);
        rolePanel.add(studentButton);
        rolePanel.add(teacherButton);
        rolePanel.add(interpreterButton);
        //audio
        JCheckBox audioCheckBox = new JCheckBox("audio");
        audioCheckBox.setFont(font);
        audioCheckBox.setSelected(false);
        audioPanel.add(audioCheckBox);
        //set name
        JTextField setNameInfoField = new JTextField("meno");
        setNameInfoField.setFont(font);
        setNameInfoField.setEditable(false);
        JTextField setNameSettingField = new JTextField();
        setNameSettingField.setFont(font);
        setNameSettingField.setEditable(true);
        setNameSettingField.setColumns(10);
        //namePanel.add(setNameInfoField);
        namePanel.add(setNameSettingField);
        //action
        JButton startButton = new JButton("start");
        startButton.setFont(font);
        startButton.addActionListener((ActionEvent event) -> {
            //login to room
            String role = getSelectedRadioButtonText(roleGroup);
            String layout = getSelectedRadioButtonText(layoutGroup);
            String room = getSelectedRadioButtonText(roomGroup);
            if(rolePanel.getComponentCount() > 0){
                startCounsil(role, audioCheckBox.isSelected(), setNameSettingField.getText());
            }else{
                openErrorWindow("necakana chyba č.1");
            }
        });
        JButton leaveButton = new JButton("leave server");
        leaveButton.setFont(font);
        leaveButton.addActionListener((ActionEvent event) -> {
            openServerChooseWindow();
        });
        JButton exitButton = new JButton("exit");
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });
        JButton aboutButton = new JButton("about");
        aboutButton.setFont(font);
        aboutButton.addActionListener((ActionEvent event) -> {
            //start about window
        });
        actionPanel.add(startButton);
        actionPanel.add(leaveButton);
        actionPanel.add(exitButton);
        actionPanel.add(aboutButton);
        //layout
        for(int i=0;i<layoutArray.length;i++){
            JRadioButton layoutButton = new JRadioButton(layoutArray[i]);
            layoutButton.setFont(font);
            layoutPanel.add(layoutButton);
            layoutGroup.add(layoutButton);
            if(i==0){//select first layout
                layoutGroup.setSelected(layoutButton.getModel(), true);
            }
        }
        
        //map layouts 
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;
        rolePanel.setBorder(BorderFactory.createTitledBorder("role"));
        mainPanel.add(rolePanel, constraints);
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 1;
        namePanel.setBorder(BorderFactory.createTitledBorder("meno"));
        mainPanel.add(namePanel, constraints);
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 2;
        layoutPanel.setBorder(BorderFactory.createTitledBorder("layout"));
        mainPanel.add(layoutPanel, constraints);
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.gridheight = 2;
        constraints.gridy = 0;
        roomPanel.setBorder(BorderFactory.createTitledBorder("room"));
        mainPanel.add(roomPanel, constraints);
        constraints.weightx = 0.5;
        constraints.gridx = 2;
        constraints.gridheight = 1;
        constraints.gridy = 0;
        mainPanel.add(actionPanel, constraints);
        constraints.weightx = 0.5;
        constraints.gridx = 2;
        constraints.gridheight = 1;
        constraints.gridy = 1;
        audioPanel.setBorder(BorderFactory.createTitledBorder("audio"));
        mainPanel.add(audioPanel, constraints);
        settingRoomWindow.getContentPane().add(mainPanel);
        settingRoomWindow.pack();
        settingRoomWindow.setLocation(position.x - settingRoomWindow.getWidth()/2, position.y - settingRoomWindow.getHeight()/2);
    }
    
    final void initIpSettingWindow(){
        if(ipSettingWindow == null){
            return;
        }
        ipSettingWindow.setTitle("Counsil");
        ipSettingWindow.setVisible(false);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1));
        
        JTextFieldLimit ipText = new JTextFieldLimit(15);
        JTextField ipField = new JTextField();
        ipField.setFont(font);
        ipField.setDocument(ipText);
        
        JButton connectButton = new JButton("connect");
        connectButton.setFont(font);
        connectButton.addActionListener((ActionEvent event) -> {
            String loadedString = null;
            try {
                loadedString = ipText.getText(0, ipText.getLength());
            } catch (BadLocationException ex) {
                Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(ipFormatCorrect(loadedString)){
                JSONObject roomNameList = getServerRoomList();
                ipAddress = loadedString;
                if(roomNameList != null){
                    openSettingRoomWindow(roomNameList);
                }else{
                    openErrorWindow("cannot connect to this server " + loadedString);
                }
            }else{
                JOptionPane.showMessageDialog(new Frame(), loadedString + " nie je spravne definovana adresa", "Chyba", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("cancel");
        cancelButton.setFont(font);
        cancelButton.addActionListener((ActionEvent event) -> {
            openServerChooseWindow();
        });
        
        ipField.setPreferredSize(new Dimension(160, 20));
        mainPanel.add(ipField);
        mainPanel.add(connectButton);
        mainPanel.add(cancelButton);
        
        ipSettingWindow.getContentPane().add(mainPanel);
        ipSettingWindow.pack();
        ipSettingWindow.setLocation(position.x - ipSettingWindow.getWidth()/2, position.y - ipSettingWindow.getHeight()/2);
    }
       
    public String getSelectedRadioButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }
    
    private boolean ipFormatCorrect(String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    
    /**
     * return list of rooms form server (server address is getIpAddres())
     * @param room
     * @return list of rooms form server (server address is getIpAddres())
     */
    private JSONObject getServerRoomList() {
        try {
            if(ipAddress.compareTo("0.0.0.0") == 0){
                return null;
            }
            String serverResponse = webClient.getRoomList(ipAddress, port);
            System.out.println(serverResponse);
            if(serverResponse == null){
                return null;
            }
            return new JSONObject(serverResponse);
        } catch (JSONException | IOException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * return room configuration from server (server address is getIpAddres())
     * @param room
     * @return room configuration from server (server address is getIpAddres())
     */
    private JSONObject getRoomConfiguraton(String room){
        try {
            if(ipAddress.compareTo("0.0.0.0") == 0){
                return null;
            }
            String serverResponse = webClient.getRoom(ipAddress, port, room);
            System.out.println(serverResponse);
            if(serverResponse == null){
                return null;
            }
            return new JSONObject(serverResponse);
        } catch (JSONException | IOException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    void openServerChooseWindow(){
        closeAllWindows();
        serverChooseWindow.setVisible(true);
    }
    
    void openIpSettingWindow(){
        closeAllWindows();
        ipSettingWindow.setVisible(true);
    }
    
    void openErrorWindow(String message){
        closeAllWindows();
        errorMessageField.setText(message);
        errorWindow.pack();
        errorWindow.setVisible(true);
    }
    
    void openSettingRoomWindow(JSONObject roomNameList){
        closeAllWindows();
        JSONArray roomList = null;
        try {
            roomList = roomNameList.getJSONArray("names");
        } catch (JSONException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        roomPanel.removeAll();
        //this.ipAddress = ipAddress;
        settingRoomWindow.setTitle(ipAddress);
        if(roomList != null){
            roomPanel.setLayout(new GridLayout(roomList.length()+1, 1));
            for(int i=0;i<roomList.length();i++){
                try {
                    String roomName = roomList.getJSONObject(i).getString("name");
                    JRadioButton roomButton = new JRadioButton(roomName);
                    roomButton.setFont(font);
                    roomPanel.add(roomButton);
                    roomGroup.add(roomButton);
                    if(i==0){
                        roomGroup.setSelected(roomButton.getModel(), true);
                    }
                } catch (JSONException ex) {
                    Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        settingRoomWindow.setVisible(true);
        settingRoomWindow.pack();
        settingRoomWindow.setLocation(position.x - settingRoomWindow.getWidth()/2, position.y - settingRoomWindow.getHeight()/2);
    }
    
    /**
     * close all initial menu windows
     */
    final void closeAllWindows(){
        serverChooseWindow.setVisible(false);
        errorWindow.setVisible(false);
        ipSettingWindow.setVisible(false);
        settingRoomWindow.setVisible(false);
    }
    
    
    /**
     * start cousil
     * @param role of the user
     */
    final void startCounsil(String role, boolean audio, String name){
        closeAllWindows();
        try {
            setConfiguration(role, audio, name);
            LayoutManagerImpl lm;
            lm = new LayoutManagerImpl(role, this);
            sm = new SessionManagerImpl(lm);
            sm.initCounsil();
        } catch (JSONException | IOException | WDDManException | InterruptedException | NativeHookException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * close counsil and re-start initial menu
     */
    final void closeCounsil(){
        sm.stopCounsil();
        sm = null;
        //startIpSetWindow();
    }
    
    /**
     * create nodeConfig.json from others configuration files
     */
    final void setConfiguration(String role, boolean audio, String name) throws InterruptedException{
        JSONObject infoFromServer = getRoomConfiguraton(roomName);
        if(infoFromServer == null){
            openErrorWindow("cannot get room configuration from server");
            throw new InterruptedException("cannot get room configuration from server");
        }
        roomConfiguration = new JSONObject();
        JSONObject connector = new JSONObject();
        JSONObject localNode = new JSONObject();
        JSONObject consumer = new JSONObject();
        try {
            connector.put("serverAddress", ipAddress);
            connector.put("serverPort", infoFromServer.getInt("comunication port"));
            connector.put("startServer", "false");

            String userName = role + "_" + name + "_" + infoFromServer.getString("connect number");
            JSONObject interfaceInside = new JSONObject();
            interfaceInside.put("name", userName);
            interfaceInside.put("address", clientConfig.getString("this ip"));
            interfaceInside.put("subnetName", "world");
            interfaceInside.put("bandwidth", 1000);
            interfaceInside.put("isFullDuplex", true);
            interfaceInside.put("properties", new JSONObject());
            JSONArray interfaces = new JSONArray();
            interfaces.put(interfaceInside);

            JSONObject properties = new JSONObject();
            properties.put("role", role);
            properties.put("videoProducer", clientConfig.getString("producer settings"));
            properties.put("audioProducer", clientConfig.getString("audio producer"));
            properties.put("audioConsumer", clientConfig.getString("audio consumer"));
            properties.put("videoConsumer", clientConfig.getString("consumer settings"));
            properties.put("audio", audio);
            
            if(clientConfig.has("presentation producer")){
                properties.put("presentationProducer", clientConfig.getString("presentation producer"));
            }

            localNode.put("name", userName);
            localNode.put("interfaces", interfaces);
            localNode.put("properties", properties);

            JSONObject templates = new JSONObject();
            JSONObject producerJSON = new JSONObject();
            producerJSON.put("path", clientConfig.getString("ultragrid path"));
            producerJSON.put("arguments", "");
            JSONObject distributorJSON = new JSONObject();
            distributorJSON.put("path", clientConfig.getString("distributor path"));
            distributorJSON.put("arguments", "8M");
            JSONObject consumerJSON = new JSONObject();
            consumerJSON.put("path", clientConfig.getString("ultragrid path"));
            consumerJSON.put("arguments", "");
            templates.put("producer", producerJSON);
            templates.put("distributor", distributorJSON);
            templates.put("consumer", consumerJSON);

            roomConfiguration.put("connector", connector);
            roomConfiguration.put("localNode", localNode);
            roomConfiguration.put("templates", templates);

            FileWriter file = new FileWriter("nodeConfig.json");
                   
            file.write(roomConfiguration.toString());
            file.flush();
            file.close();
        } catch (JSONException | IOException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    final JSONObject getConfiguration(){
        return roomConfiguration;
    }
    
    JSONObject readJsonFile(File jsonFile){
        try {
            String entireFileText = new Scanner(jsonFile).useDelimiter("\\A").next();
            return new JSONObject(entireFileText);
        } catch (JSONException | FileNotFoundException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    void loadClientConfigurationFromFile(){
        clientConfig = readJsonFile(configurationFile);
    }
}
