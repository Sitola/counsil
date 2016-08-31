package counsil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.text.Normalizer;
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
    
    /**
     * text field for error message displayed in error window
     */
    JTextField errorMessageField;
    
    /**
     * panel for rooms, rooms are set after initialization on specific place (where
     * this panel is)
     */
    JPanel roomPanel;
    
    /**
     * list of layouts files
     */
    List<LayoutFile> layoutList;
    
    /**
     * button group for rooms
     */
    ButtonGroup roomGroup;
    
    /**
     * center position where windows are going to be displayed
     */
    Position position;
    
    /**
     * global font for text
     */
    private final Font font;
    
    /**
     * web client to get information from server
     */
    private final WebClient webClient;
    
    /**
     * global port, some buttons set it and call function which is using it
     */
    int port;
    
    /**
     * global ip address, some buttons set it and call function which is using it
     */
    String ipAddress;
    
    /**
     * setting if this is student only device
     */
    boolean studentOnly;
    
    String role;
    String name;
    JCheckBox audioCheckBox;
    boolean logedIn;
    
    /**
     * 5 main windows
     */
    private final JFrame serverChooseWindow;
    private final JFrame settingRoomWindow;
    private final JFrame errorWindow;
    private final JFrame ipSettingWindow;
    private final JFrame roleNameWindow;
    
    /**
     * class for optionsMainMenu to be able to manipulate with it, mostly to close it
     */
    public OptionsMainMenuWindow optionMainMenuWindow;
        
    /**
     * configuration from server about the room
     */
    JSONObject roomConfiguration;
    
    /**
     * configuration from this pc
     */
    JSONObject clientConfig;
    
    /**
     * configuration file
     */
    File configurationFile;
    
    /**
     * lenguage localization
     */
    ResourceBundle languageBundle;
    
    /**
     * to init and end couniverse part of counsil
     */
    SessionManager sm;
    
    /**
     * constructor
     * @param centerPosition position of center where windows are going to be display
     * @param clientConfigurationFile client configuration file
     * @param buttonFont font for buttons
     */
    InitialMenuLayout(Position centerPosition, File clientConfigurationFile, Font buttonFont) {
        webClient = new WebClient();
        sm = null;
        
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
        roleNameWindow = new JFrame();
        
        optionMainMenuWindow = null;
        configurationFile = clientConfigurationFile;
        
        position = centerPosition;
        
        loadClientConfigurationFromFile();
        
        port = 0;
        ipAddress = ""; 
        role = "";
        name = "";
        logedIn = false;

        initSettingRoomWindow();
        initServerChooseWindow();
        initIpSettingWindow();
        initErrorWindow();
        initRoleNameWindow();
        openRoleNameWindow();
    }
    
    /**
     * initialization of window where user set name and role
     */
    final void initRoleNameWindow(){
        if(roleNameWindow == null){
            return;
        }
        roleNameWindow.getContentPane().removeAll();
        roleNameWindow.setTitle(languageBundle.getString("COUNSIL"));
        roleNameWindow.setVisible(false);
        roleNameWindow.addWindowListener(new WindowAdapter() {//action on close button (x)
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        
        JPanel mainPanel = new JPanel();
        JPanel namePanel = new JPanel();
        JPanel rolePanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        namePanel.setLayout(new GridLayout(1, 1));
        rolePanel.setLayout(new GridLayout(1, 3));
        
        rolePanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("ROLE")));
        namePanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("NAME")));
        
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
        role = languageBundle.getString("STUDENT");
        rolePanel.add(studentButton);
        rolePanel.add(teacherButton);
        rolePanel.add(interpreterButton);
        
        studentButton.addActionListener((ActionEvent event) -> {
            role = languageBundle.getString("STUDENT");
        });
        teacherButton.addActionListener((ActionEvent event) -> {
            role = languageBundle.getString("TEACHER");
        });
        interpreterButton.addActionListener((ActionEvent event) -> {
            role = languageBundle.getString("INTERPRETER");
        });
        
        teacherButton.setEnabled(!studentOnly);
        interpreterButton.setEnabled(!studentOnly);
        
        //set name
        JTextField setNameSettingField = new JTextField();
        setNameSettingField.setEditable(true);
        setNameSettingField.setColumns(10);
        namePanel.add(setNameSettingField);
        
        //"next" button
        JButton nextButton = new JButton(languageBundle.getString("CONTINUE"));
        nextButton.setFont(font);
        nextButton.addActionListener((ActionEvent e) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            if(setNameSettingField.getText().isEmpty()){
                openErrorWindow(languageBundle.getString("ERROR_EMPTY_NAME"));
            }else{
                logedIn = true;
                name = setNameSettingField.getText();
                name = Normalizer.normalize(name, Normalizer.Form.NFD);
                name = name.replaceAll("[^\\p{ASCII}]", "");            //transform ščťžýáíé.. to sctzyaie..
                openServerChooseWindow();
            }
        });
        
        //option button
        JButton optionsButton = new JButton(languageBundle.getString("OPTIONS"));
        optionsButton.setFont(font);
        optionsButton.addActionListener((ActionEvent event) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            optionMainMenuWindow = new OptionsMainMenuWindow(font, configurationFile, this, languageBundle, role);
        });
        
        //exit button
        JButton exitButton = new JButton(languageBundle.getString("EXIT"));
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent e) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            System.exit(0);
        });

        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_ROLE")){
            rolePanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            studentButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            teacherButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
            interpreterButton.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_ROLE"));
        }
        if(languageBundle.containsKey("ROOM_SETTING_TOOL_TIP_NAME")){
            namePanel.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_NAME"));
            setNameSettingField.setToolTipText(languageBundle.getString("ROOM_SETTING_TOOL_TIP_NAME"));
        }
        
        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(namePanel, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(rolePanel, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 2;
        mainPanel.add(nextButton, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 3;
        mainPanel.add(optionsButton, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 4;
        mainPanel.add(exitButton, mainPanelConstraints);
        
        roleNameWindow.getContentPane().add(mainPanel);
        roleNameWindow.pack();
        roleNameWindow.setLocation(position.x - roleNameWindow.getWidth()/2, position.y - roleNameWindow.getHeight()/2);
    
    }
    
    /**
     * initialization of window where user can choose server to connect from saved 
     * servers, go to window where he can set ip address to connect or open options
     * menu window
     */
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
                            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, error);
                            serverPort = 80; //defout value
                        }
                        ipAddress = serverIP;
                        port = serverPort;
                        JSONObject roomListNames = getServerRoomList();
                        if(roomListNames != null){
                            openSettingRoomWindow(roomListNames);
                        }else{
                            openErrorWindow(languageBundle.getString("ERROR_CANNOT_CONNECT_TO_SERVER"));
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
            optionMainMenuWindow = new OptionsMainMenuWindow(font, configurationFile, this, languageBundle, role);
        });
        JButton nameChangeButton = new JButton(languageBundle.getString("USER_CHANGE"));
        nameChangeButton.setFont(font);
        nameChangeButton.addActionListener((ActionEvent event) -> {
            if(optionMainMenuWindow != null){
                optionMainMenuWindow.dispose();
                optionMainMenuWindow = null;
            }
            logedIn = false;
            openRoleNameWindow();
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
        mainPanel.add(nameChangeButton, mainPanelConstraints);
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 4;
        mainPanel.add(exitButton, mainPanelConstraints);
        serverChooseWindow.getContentPane().add(mainPanel);
        serverChooseWindow.pack();
        serverChooseWindow.setLocation(position.x - serverChooseWindow.getWidth()/2, position.y - serverChooseWindow.getHeight()/2);
    }
    
    /**
     * initialization of window to show some errors if error force user to return
     * to initial window
     */
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
                if(logedIn){
                    openServerChooseWindow();
                }else{
                    openRoleNameWindow();
                }
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
            if(logedIn){
                openServerChooseWindow();
            }else{
                openRoleNameWindow();
            }
        });
        jMainPanel.add(errorMessageField, BorderLayout.NORTH);
        jButtonPanel.add(okButton);
        jMainPanel.add(okButton, BorderLayout.SOUTH);
        errorWindow.getContentPane().add(jMainPanel);
        
        errorWindow.pack();
        errorWindow.setLocation(position.x - errorWindow.getWidth() / 2, position.y - errorWindow.getHeight());
    }
    
    /**
     * initialization of window to set user information before connecting to server
     * user set name, role, room, sound and layout
     * also information about counsil is in this window
     */
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
        JPanel layoutPanel, actionPanel, mainPanel, audioPanel, rightMainPanel, leftMainPanel, aboutPanel, anotherPanel;
        
        rightMainPanel = new JPanel();
        rightMainPanel.setLayout(new GridBagLayout());
        leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new GridBagLayout());
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayout(1,1));
        anotherPanel = new JPanel();
        anotherPanel.setLayout(new GridBagLayout());
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
        
        
        ButtonGroup layoutGroup =  new ButtonGroup();

        //audio
        audioCheckBox = new JCheckBox(languageBundle.getString("AUDIO"));
      
        audioPanel.add(audioCheckBox);
        
        //action
        JButton startButton = new JButton(languageBundle.getString("START"));
        startButton.setFont(font);
        startButton.addActionListener((ActionEvent event) -> {
            //login to room
            String unlocalizedRole = getRoleFromLocalization(role);
            String layout = getSelectedRadioButtonText(layoutGroup);
            String room = getSelectedRadioButtonText(roomGroup);
            startCounsil(unlocalizedRole, audioCheckBox.isSelected(), name, layout, room);
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
        leftMainPanel.add(roomPanel, leftMainPanelConstraints);
        leftMainPanelConstraints.gridx = 0;
        leftMainPanelConstraints.gridy = 1;
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
    
    
    /**
     * initialization of window where user can connect to server on specific address
     * and port, main usage is to test new servers and if someone doesn't want to
     * save ip address
     */
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
                    JOptionPane.showMessageDialog(new Frame(), loadedString + languageBundle.getString("UNDEFINED_ADDRESS"), languageBundle.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
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
    
    //TODO: opravit return hodnoty tak, aby se zobrazovaly role lokalizovane
    /**
     * function for transforming localize role to unlocalize role
     * @param localizeRole current localize role
     * @return unlocalize role
     */
    public String getRoleFromLocalization(String localizeRole){
        if(languageBundle.getString("INTERPRETER").equals(localizeRole)){
            return "interpreter";
        }else if(languageBundle.getString("TEACHER").equals(localizeRole)){
            return "teacher";
        }else {
            return "student";
        }
    }
    
    /**
     * function to return localized role from unlocalized
     * @param unlocalizedRole unlocalized role
     * @return localized role
     */
    public String getRoleToLocalization(String unlocalizedRole){
        if(unlocalizedRole.equals("interpreter")){
            return languageBundle.getString("INTERPRETER");
        }else if(unlocalizedRole.equals("teacher")){
            return languageBundle.getString("TEACHER");
        }else {
            return languageBundle.getString("STUDENT");
        }
    }
    
    // TODO: Doplnit popis metody a komentar
    /**
     * function to verify if ip address have correct format
     * @param ip ip address to be checked
     * @return boolean if ip address have correct format
     */
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
    
    /**
     * open role name window
     */
    void openRoleNameWindow(){
        closeAllWindows();
        roleNameWindow.setVisible(true);
    }
    
    /**
     * open server choose window
     */
    void openServerChooseWindow(){
        closeAllWindows();
        serverChooseWindow.setVisible(true);
    }
    
    /**
     * open ip seeting window
     */
    void openIpSettingWindow(){
        closeAllWindows();
        ipSettingWindow.setVisible(true);
    }
    
    /**
     * open error window
     * @param message print this message in window
     */
    void openErrorWindow(String message){
        closeAllWindows();
        errorMessageField.setText(message);
        errorWindow.pack();
        errorWindow.setVisible(true);
    }
    
    /**
     * open setting room window
     * @param roomNameList 
     */
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
        
        if(!role.equals(languageBundle.getString("STUDENT"))){
            audioCheckBox.setSelected(true);
            audioCheckBox.setEnabled(false);
        }else{
            audioCheckBox.setSelected(false);
            audioCheckBox.setEnabled(true);
        }
        
        settingRoomWindow.setVisible(true);
        settingRoomWindow.pack();
        settingRoomWindow.setLocation(position.x - settingRoomWindow.getWidth()/2, position.y - settingRoomWindow.getHeight()/2);
    }
    
    /**
     * close all initial menu windows
     */
    final void closeAllWindows(){
        roleNameWindow.setVisible(false);
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
            ObjectNode configNode = (ObjectNode) new ObjectMapper().readTree(roomConfiguration.toString());
            
            LayoutManagerImpl lm;
            lm = new LayoutManagerImpl(role, this, scaleRatio, layoutFile, languageBundle, font);
            sm = new SessionManagerImpl(lm, talkingColor, riseHandColor, languageBundle, configNode);
            sm.initCounsil();
        } catch (JSONException | IOException | WDDManException | InterruptedException | NativeHookException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * close counsil and re-open initial menu
     */
    final void closeCounsil(){

        if(sm == null){
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, "error cannot stop counsil, lost pointer to session manager");
            openErrorWindow(languageBundle.getString("ERROR_CANNOT_DISCONNECT_FROM_SERVER") + "\n" + languageBundle.getString("PLEASE_EXIT_APPLICATION_BEFORE_FARTHER_USAGE"));
        }else{
            sm.stop();
            openServerChooseWindow();
        }
    }
    
    /**
     * function to return short name of set role
     * @param role role name in current language
     * @return short name
     */
    final String shortNameRole(String role){
        if(languageBundle.getString("INTERPRETER").equals(role)){
            return "i";
        }else if(languageBundle.getString("TEACHER").equals(role)){
            return "t";
        }else {
            return "s";
        }
    }
    
    /**
     * create roomConfiguration from others configuration files, used for couniverse
     * @param role unloczlized role
     * @param audio if to use audio
     * @param name user name
     * @param room room name
     * @throws InterruptedException 
     */
    final void setConfiguration(String role, boolean audio, String name, String room) throws InterruptedException{
        
        String localizedRole = getRoleToLocalization(role);
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

            String userName = name + " (" + shortNameRole(localizedRole) + infoFromServer.getString("connect number") + ")";
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
            
            if(localizedRole.equals(languageBundle.getString("TEACHER"))){
                if(clientConfig.has("presentation producer")){
                    properties.put("presentationProducer", clientConfig.getString("presentation producer"));
                }
            }

            localNode.put("name", userName);
            localNode.put("interfaces", interfaces);
            localNode.put("properties", properties);

            JSONObject templates = new JSONObject();
            JSONObject producerJSON = new JSONObject();
            producerJSON.put("path", clientConfig.getString("ultragrid path"));
            producerJSON.put("arguments", "");
            JSONObject consumerJSON = new JSONObject();
            consumerJSON.put("path", clientConfig.getString("ultragrid path"));
            consumerJSON.put("arguments", "");
            templates.put("producer", producerJSON);
            templates.put("consumer", consumerJSON);

            roomConfiguration.put("connector", connector);
            roomConfiguration.put("localNode", localNode);
            roomConfiguration.put("templates", templates);
            
        } catch (JSONException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    /**
     * get configuration json
     * @return configuration saved in JSON file
     */
    final JSONObject getConfiguration(){
        return roomConfiguration;
    }
    
    /**
     * read configuration file and save it in JSON
     * @param jsonFile configuration file
     * @return configuration JSON
     */
    JSONObject readJsonFile(File jsonFile){
        try {
            String entireFileText = new Scanner(jsonFile).useDelimiter("\\A").next();
            return new JSONObject(entireFileText);
        } catch (JSONException | FileNotFoundException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * load client configuration file
     */
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
        try {
            studentOnly = clientConfig.getBoolean("student only");
        } catch (JSONException ex) {
            studentOnly = false;
        }
    }
    
    /**
     * get layout file
     * @param layoutName
     * @return class containing layout name and layout file
     */
    LayoutFile getLayoutFile(String layoutName){
        for(int i = 0; i < layoutList.size(); i++){
            if(layoutList.get(i).layoutName.equals(layoutName)){
                return layoutList.get(i);
            }
        }
        return new LayoutFile();    //or throw error
    }
}

/**
 * class to save and work with layout name and layout file
 * @author xminarik
 */
class LayoutFile{
    public String layoutName;
    public File file;
}

/*
 * class for limitting length of text, primary use for ip address field
 */
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