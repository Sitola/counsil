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
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.jnativehook.NativeHookException;
import wddman.WDDManException;

/**
 *
 * @author xminarik
 */
public final class InitialMenuLayout{
    
    JTextField errorMessageField;
    JPanel roomPanel;
    List<LayoutFile> layoutList;
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
    
    public OptionsMainMenuWindow optionMainMenuWindow;
        
    JSONObject roomConfiguration;
    JSONObject clientConfig;

    String nameList[];
    
    JFormattedTextField ipField[];
    Integer ipValue[];
    
    File configurationFile;
    ResourceBundle languageBundle;
    /**
     * to init and end couniverse part of counsil
     */
    SessionManager sm;
    
    class JTextFieldLimit extends PlainDocument {

        private final int limit;
        
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
    
    InitialMenuLayout(Position centerPosition, File clientConfigurationFile, Font buttonFont) {
        webClient = new WebClient();
        
        errorMessageField = null;
        roomPanel = null;
        layoutList = new ArrayList<>();
        roomGroup = new ButtonGroup();
        
        this.font = buttonFont;                
        roomConfiguration = null;
        
        serverChooseWindow = new JFrame();
        settingRoomWindow = new JFrame();
        errorWindow = new JFrame();
        ipSettingWindow = new JFrame();
        
        optionMainMenuWindow = null;
        configurationFile = clientConfigurationFile;
        
        position = centerPosition;
        
        loadClientConfigurationFromFile();
        
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
        serverChooseWindow.setTitle(languageBundle.getString("COUNSIL"));
        serverChooseWindow.setVisible(false);
        serverChooseWindow.addWindowListener(new WindowAdapter() {//action on close button (x)
           
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        JSONArray ipAddresses;
        JButton[] buttonAdresses = null;
        JPanel mainPanel;
        JPanel ipPanel;
        mainPanel = new JPanel();
        ipPanel = new JPanel();
        ipPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("SERVERS")));
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
                        ipAddress = serverIP;
                        port = serverPort;
                        JSONObject roomListNames = getServerRoomList();
                        if(roomListNames != null){
                            openSettingRoomWindow(roomListNames);
                        }else{
                            openErrorWindow(languageBundle.getString("ERROR_CAN_NOT_CONNECT_TO_SERVER"));
                        }
                    });
                }
            } catch (JSONException ex) {
                Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ipPanel.setLayout(new GridBagLayout());
        GridBagConstraints ipPanelConstraints = new GridBagConstraints();
        ipPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        ipPanelConstraints.insets = new Insets(2,2,2,2);
        ipPanelConstraints.weightx = 0.5;
        ipPanelConstraints.gridx = 0;
        ipPanelConstraints.gridy = 0;
        ipPanelConstraints.gridheight = 1;
        ipPanelConstraints.gridwidth = 1;
        if(buttonAdresses != null){
            for (JButton buttonAdresse : buttonAdresses) {
                ipPanelConstraints.gridy++;
                ipPanel.add(buttonAdresse, ipPanelConstraints);
            }
        }
        JButton differentServerButton = new JButton(languageBundle.getString("DIFFERENT_SERVER"));
        differentServerButton.setFont(font);
        differentServerButton.addActionListener((ActionEvent event) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            openIpSettingWindow();
            //close this window and open window to set own ip address
        });
        JButton optionsButton = new JButton(languageBundle.getString("OPTIONS"));
        optionsButton.setFont(font);
        optionsButton.addActionListener((ActionEvent event) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
                        
            optionMainMenuWindow = new OptionsMainMenuWindow(font, configurationFile, this, languageBundle);
        });
        JButton exitButton = new JButton(languageBundle.getString("EXIT"));
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent event) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            System.exit(0);
        });
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.insets = new Insets(2,2,2,2);
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridheight = 1;
        mainPanelConstraints.gridwidth = 1;
        mainPanel.add(ipPanel, mainPanelConstraints);
        mainPanelConstraints.insets = new Insets(10,2,2,2);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(differentServerButton, mainPanelConstraints);
        mainPanelConstraints.insets = new Insets(2,2,2,2);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 2;
        mainPanel.add(optionsButton, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 3;
        mainPanel.add(exitButton, mainPanelConstraints);
        serverChooseWindow.getContentPane().add(mainPanel);
        serverChooseWindow.pack();
        serverChooseWindow.setLocation(position.x - serverChooseWindow.getWidth()/2, position.y - serverChooseWindow.getHeight()/2);
    }
    
    final void initErrorWindow(){        
        if(errorWindow == null){
            return;
        }
        errorWindow.getContentPane().removeAll();
        errorWindow.setTitle(languageBundle.getString("COUNSIL"));
        errorWindow.setVisible(false);
        errorWindow.addWindowListener(new WindowAdapter() {//action on close button (x)
          
            @Override
            public void windowClosing(WindowEvent e) {
                openServerChooseWindow();
            }
        });
        
        JPanel jButtonPanel = new JPanel();
        JPanel jMainPanel = new JPanel();
        jMainPanel.setLayout(new BorderLayout());
        
        errorMessageField = new JTextField(languageBundle.getString("ERROR_UNDOCUMENTED"));       
        errorMessageField.setEditable(false);
        JButton okButton = new JButton(languageBundle.getString("OK_BUTTON"));
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
        settingRoomWindow.getContentPane().removeAll();
        settingRoomWindow.setTitle(languageBundle.getString("COUNSIL"));
        settingRoomWindow.setVisible(false);
        settingRoomWindow.addWindowListener(new WindowAdapter() {//action on close button (x)
           
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        //create panels, room panel is declared globaly to be able simply change rooms as download from server
        JPanel rolePanel, layoutPanel, actionPanel, mainPanel, audioPanel, namePanel, rightMainPanel, leftMainPanel, aboutPanel, anotherPanel;
        
        rightMainPanel = new JPanel();
        rightMainPanel.setLayout(new GridBagLayout());
        leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new GridBagLayout());
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayout(1,1));
        anotherPanel = new JPanel();
        anotherPanel.setLayout(new GridBagLayout());
        rolePanel = new JPanel();
        rolePanel.setLayout(new GridLayout(1, 3));
        layoutPanel = new JPanel();
        layoutPanel.setLayout(new GridBagLayout());
        roomPanel = new JPanel();
        //room layout will be set when we know how many room there is
        actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayout(3, 1));
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
        JRadioButton studentButton = new JRadioButton(languageBundle.getString("STUDENT"));
        studentButton.setFont(font);
        JRadioButton teacherButton = new JRadioButton(languageBundle.getString("TEACHER"));
        teacherButton.setFont(font);
        JRadioButton interpreterButton = new JRadioButton(languageBundle.getString("INTERPRETER"));
        interpreterButton.setFont(font);
        roleGroup.add(studentButton);
        roleGroup.add(teacherButton);
        roleGroup.add(interpreterButton);
        roleGroup.setSelected(studentButton.getModel(), true);
        rolePanel.add(studentButton);
        rolePanel.add(teacherButton);
        rolePanel.add(interpreterButton);
        //audio
        JCheckBox audioCheckBox = new JCheckBox(languageBundle.getString("AUDIO"));
      
        audioCheckBox.setSelected(false);
        audioPanel.add(audioCheckBox);
        //set name
        JTextField setNameInfoField = new JTextField(languageBundle.getString("NAME"));
        setNameInfoField.setEditable(false);
        JTextField setNameSettingField = new JTextField();
        setNameSettingField.setEditable(true);
        setNameSettingField.setColumns(10);
        //namePanel.add(setNameInfoField);
        namePanel.add(setNameSettingField);
        //action
        JButton startButton = new JButton(languageBundle.getString("START"));
        startButton.setFont(font);
        startButton.addActionListener((ActionEvent event) -> {
            //login to room
            String role = getSelectedRadioButtonText(roleGroup);
            role = getRoleFromLocalization(role);
            String layout = getSelectedRadioButtonText(layoutGroup);
            String room = getSelectedRadioButtonText(roomGroup);
            if(rolePanel.getComponentCount() > 0){
                startCounsil(role, audioCheckBox.isSelected(), setNameSettingField.getText(), layout, room);
            }else{
                openErrorWindow(languageBundle.getString("STARTUP_ERROR"));
            }
        });
        JButton backButton = new JButton(languageBundle.getString("BACK"));
        backButton.setFont(font);
        backButton.addActionListener((ActionEvent event) -> {
            openServerChooseWindow();
        });
        JButton exitButton = new JButton(languageBundle.getString("EXIT"));
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });
        actionPanel.add(startButton);
        actionPanel.add(backButton);
        actionPanel.add(exitButton);
        //layout
        GridBagConstraints layoutPanelConstraints = new GridBagConstraints();
        layoutPanelConstraints.weightx = 0.5;
        for(int i=0;i<layoutList.size();i++){
            JRadioButton layoutButton = new JRadioButton(layoutList.get(i).layoutName);
            layoutButton.setFont(font);
            layoutPanelConstraints.gridx = i % 2;
            layoutPanelConstraints.gridy = i / 2;
            layoutPanel.add(layoutButton, layoutPanelConstraints);
            layoutGroup.add(layoutButton);
            if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_LAYOUT")){
                layoutButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_LAYOUT"));
            }
            if(i==0){//select first layout
                layoutGroup.setSelected(layoutButton.getModel(), true);
            }
        }
        //about
        JTextArea aboutText = new JTextArea(languageBundle.getString("ABOUT_MESSAGE"));
        aboutText.setColumns(11);
        aboutText.setRows(7);
        aboutText.setEditable(false);
        aboutText.setBackground(settingRoomWindow.getBackground());
        aboutText.setLineWrap(true);
        aboutText.setWrapStyleWord(true);
        aboutPanel.add(aboutText);
        
        //tool tips
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_NAME")){
            namePanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_NAME"));
            setNameSettingField.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_NAME"));
        }
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_ROLE")){
            rolePanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            studentButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            teacherButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            interpreterButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
        }
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_ROOM")){
            roomPanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROOM"));
            //this tool tip also set to all room buttons
        }
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_AUDIO")){
            audioPanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_AUDIO"));
            audioCheckBox.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_AUDIO"));
        }
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_LAYOUT")){
            layoutPanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_LAYOUT"));
            //this tool tip also set to all layouts buttons
        }
        
        rolePanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("ROLE")));
        namePanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("NAME")));
        roomPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("ROOM")));
        anotherPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("ADVANCED_PANEL")));
        
        //map layouts 
        GridBagConstraints anotherPanelConstraints = new GridBagConstraints();
        anotherPanelConstraints.weightx = 0.5;
        anotherPanelConstraints.gridx = 0;
        anotherPanelConstraints.gridy = 0;
        anotherPanel.add(audioPanel, anotherPanelConstraints);
        anotherPanelConstraints.gridx = 0;
        anotherPanelConstraints.gridy = 2;
        anotherPanel.add(layoutPanel, anotherPanelConstraints);
        anotherPanelConstraints.gridx = 0;
        anotherPanelConstraints.gridy = 1;
        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(anotherPanel.getPreferredSize().width, 1));
        anotherPanel.add(separator, anotherPanelConstraints);
        
        GridBagConstraints rightMainPanelConstraints = new GridBagConstraints();
        rightMainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        rightMainPanelConstraints.weightx = 0.5;
        rightMainPanelConstraints.gridx = 0;
        rightMainPanelConstraints.gridy = 0;
        rightMainPanel.add(actionPanel, rightMainPanelConstraints);
        rightMainPanelConstraints.insets = new Insets(5, 3, 3, 0);
        rightMainPanelConstraints.gridx = 0;
        rightMainPanelConstraints.gridy = 1;
        rightMainPanel.add(aboutPanel, rightMainPanelConstraints);
        
        GridBagConstraints leftMainPanelConstraints = new GridBagConstraints();
        leftMainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        leftMainPanelConstraints.weightx = 0.5;
        leftMainPanelConstraints.gridx = 0;
        leftMainPanelConstraints.gridy = 0;
        leftMainPanel.add(namePanel, leftMainPanelConstraints);
        leftMainPanelConstraints.gridx = 0;
        leftMainPanelConstraints.gridy = 1;
        leftMainPanel.add(rolePanel, leftMainPanelConstraints);
        leftMainPanelConstraints.gridx = 0;
        leftMainPanelConstraints.gridy = 2;
        leftMainPanel.add(roomPanel, leftMainPanelConstraints);
        leftMainPanelConstraints.gridx = 0;
        leftMainPanelConstraints.gridy = 3;
        leftMainPanel.add(anotherPanel, leftMainPanelConstraints);
        
        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(leftMainPanel, mainPanelConstraints);
        mainPanelConstraints.anchor = GridBagConstraints.PAGE_START;
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(rightMainPanel, mainPanelConstraints);
        settingRoomWindow.getContentPane().add(mainPanel);
        settingRoomWindow.pack();
        settingRoomWindow.setLocation(position.x - settingRoomWindow.getWidth()/2, position.y - settingRoomWindow.getHeight()/2);
    }
    
    final void initIpSettingWindow(){
        if(ipSettingWindow == null){
            return;
        }
        ipSettingWindow.getContentPane().removeAll();
        ipSettingWindow.setTitle(languageBundle.getString("COUNSIL"));
        ipSettingWindow.setVisible(false);
        ipSettingWindow.addWindowListener(new WindowAdapter() {//action on close button (x)
          
            @Override
            public void windowClosing(WindowEvent e) {
                openServerChooseWindow();
            }
        });
        JPanel mainPanel = new JPanel();
        JPanel setPanel = new JPanel();
        
        JTextFieldLimit ipText = new JTextFieldLimit(15);
        JTextField ipField = new JTextField();
        ipField.setPreferredSize(new Dimension(160, 20));        
        ipField.setEditable(true);
        ipField.setDocument(ipText);
        ipField.setBorder(BorderFactory.createEmptyBorder());
        JTextField ipTextInfoField = new JTextField(languageBundle.getString("IP"));
        ipTextInfoField.setEditable(false);
        ipTextInfoField.setBorder(BorderFactory.createEmptyBorder());
        JTextField portField = new JTextField();
        portField.setEditable(true);
        portField.setBorder(BorderFactory.createEmptyBorder());
        JTextField portFieldInfoText = new JTextField(languageBundle.getString("PORT"));
        portFieldInfoText.setEditable(false);
        portFieldInfoText.setBorder(BorderFactory.createEmptyBorder());
        
        JButton connectButton = new JButton(languageBundle.getString("CONNECT"));
        connectButton.setFont(font);
        connectButton.addActionListener((ActionEvent event) -> {
            String loadedString = null;
            try {
                loadedString = ipText.getText(0, ipText.getLength());
                if(ipFormatCorrect(loadedString)){
                    ipAddress = loadedString;
                    try{
                        port = Integer.parseInt(portField.getText());
                        JSONObject roomNameList = getServerRoomList();
                        if(roomNameList != null){
                            openSettingRoomWindow(roomNameList);
                        }else{
                            openErrorWindow(languageBundle.getString("CANNOT_CONNECT_TO_THIS_SERVER") + loadedString);
                        }
                    }catch(NumberFormatException e){
                        JOptionPane.showMessageDialog(new Frame(),languageBundle.getString("INCORRECT_PORT"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                    }
                }else{
                    JOptionPane.showMessageDialog(new Frame(), loadedString + languageBundle.getString("UNDEFINE_ADDRESS"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                }
            } catch (BadLocationException ex) {
                JOptionPane.showMessageDialog(new Frame(), loadedString + languageBundle.getString("ERROR_UNDOCUMENTED"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            }
            
        });
        
        JButton cancelButton = new JButton(languageBundle.getString("CANCEL"));
        cancelButton.setFont(font);
        cancelButton.addActionListener((ActionEvent event) -> {
            openServerChooseWindow();
        });
        
        setPanel.setLayout(new GridBagLayout());
        GridBagConstraints setPanelConstraints = new GridBagConstraints();
        setPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        setPanelConstraints.insets = new Insets(5,5,5,5);
        setPanelConstraints.weightx = 0.5;
        setPanelConstraints.gridx = 0;
        setPanelConstraints.gridy = 0;
        setPanel.add(ipTextInfoField, setPanelConstraints);
        setPanelConstraints.gridx = 1;
        setPanelConstraints.gridy = 0;
        setPanel.add(ipField, setPanelConstraints);
        setPanelConstraints.gridx = 0;
        setPanelConstraints.gridy = 1;
        setPanel.add(portFieldInfoText, setPanelConstraints);
        setPanelConstraints.gridx = 1;
        setPanelConstraints.gridy = 1;
        setPanel.add(portField, setPanelConstraints);
                
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(setPanel, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(connectButton, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 2;
        mainPanel.add(cancelButton, mainPanelConstraints);
        
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
    
    public String getRoleFromLocalization(String localizeRole){
        if(languageBundle.getString("INTERPRETER").equals(localizeRole)){
            return "interpreter";
        }else if(languageBundle.getString("TEACHER").equals(localizeRole)){
            return "teacher";
        }else {
            return "student";
        }
        
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
            roomPanel.setLayout(new GridBagLayout()); 
            GridBagConstraints roomPanelConstraints = new GridBagConstraints();
            roomPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
            roomPanelConstraints.weightx = 0.5;
            for(int i=0;i<roomList.length();i++){
                try {
                    String roomName = roomList.getJSONObject(i).getString("name");
                    JRadioButton roomButton = new JRadioButton(roomName);
                    roomButton.setFont(font);
                    roomPanelConstraints.gridx = i % 2;
                    roomPanelConstraints.gridy = i / 2;
                    roomPanel.add(roomButton, roomPanelConstraints);
                    roomGroup.add(roomButton);
                    if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_ROOM")){
                        roomButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROOM"));
                    }
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
    final void startCounsil(String role, boolean audio, String name, String layout, String room){
        
        closeAllWindows();
        try {
            setConfiguration(role, audio, name, room);
            int scaleRatio = clientConfig.getInt("talking resizing");
            File layoutFile = getLayoutFile(layout).file;
            JSONObject riseHandColorJson = clientConfig.getJSONObject("raise hand color");
            Color riseHandColor = new Color(riseHandColorJson.getInt("red"), riseHandColorJson.getInt("green"), riseHandColorJson.getInt("blue"));
            JSONObject talkingColorJson = clientConfig.getJSONObject("talking color");
            Color talkingColor = new Color(talkingColorJson.getInt("red"), talkingColorJson.getInt("green"), talkingColorJson.getInt("blue"));
            
            LayoutManagerImpl lm;
            lm = new LayoutManagerImpl(role, this, scaleRatio, layoutFile, languageBundle, font);
            sm = new SessionManagerImpl(lm, talkingColor, riseHandColor, languageBundle);
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
        openServerChooseWindow();
    }
    
    /**
     * create nodeConfig.json from others configuration files
     */
    final void setConfiguration(String role, boolean audio, String name, String room) throws InterruptedException{
        
        JSONObject infoFromServer = getRoomConfiguraton(room);
        if(infoFromServer == null){
            openErrorWindow(languageBundle.getString("NO_ROOM_CONFIG_ERROR"));
            throw new InterruptedException(languageBundle.getString("NO_ROOM_CONFIG_ERROR"));
        }
        roomConfiguration = new JSONObject();
        JSONObject connector = new JSONObject();
        JSONObject localNode = new JSONObject();
        try {
            connector.put("serverAddress", ipAddress);
            connector.put("serverPort", infoFromServer.getInt("comunication port"));
            connector.put("startServer", false);

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
            properties.put("room", room);
            
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
        layoutList.clear();
        if(clientConfig.has("layout path")){
            try {
                String layoutPath = clientConfig.getString("layout path");
                File layoutDir = new File(layoutPath);
                if(layoutDir.isDirectory()){
                    File[] filesInLayoutDir = layoutDir.listFiles();
                    for (File filesInLayoutDir1 : filesInLayoutDir) {
                        LayoutFile newLayout = new LayoutFile();
                        if (filesInLayoutDir1.isFile()) {
                            newLayout.file = filesInLayoutDir1;
                            newLayout.layoutName = filesInLayoutDir1.getName();
                            layoutList.add(newLayout);
                        }
                    }
                    if(layoutList.isEmpty()){
                        JOptionPane.showMessageDialog(new Frame(), languageBundle.getString("ERROR_LAYOUT_NOT_FOUND"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                    }
                }else{
                    if(!layoutDir.exists()){
                        JOptionPane.showMessageDialog(new Frame(), languageBundle.getString("ERROR_DIRECTORY_DOES_NOT_EXIST"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (JSONException ex) {
                Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }       
        String languageResourcesName = "resources_en_EN";
        if(clientConfig.has("language")){
            try {
                switch (clientConfig.getString("language")) {
                    case "Slovenský":
                        languageResourcesName = "resources_sk_SK";
                        break;
                    case "Český":
                        languageResourcesName = "resources_cs_CZ";
                        break;
                    case "English":
                    default:
                        languageResourcesName = "resources_en_EN";
                        break;
                }
            } catch (JSONException ex) {
                Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        languageBundle = ResourceBundle.getBundle(languageResourcesName);
        
    }
    
    LayoutFile getLayoutFile(String layoutName){
        for(int i = 0; i < layoutList.size(); i++){
            if(layoutList.get(i).layoutName.equals(layoutName)){
                return layoutList.get(i);
            }
        }
        return new LayoutFile();    //or throw error
    }
}

class LayoutFile{
    public String layoutName;
    public File file;
}