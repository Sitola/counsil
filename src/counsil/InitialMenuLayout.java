/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.BorderLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;
import org.jnativehook.NativeHookException;
import wddman.WDDManException;

/**
 *
 * @author xminarik
 */
public class InitialMenuLayout extends JFrame {

    private final JFrame ipSetWindow;
    private final JFrame errorWindow;
    private final JFrame roomChooseWindow;
    private final JFrame roleChooseWindow;
    private final Font font;
    private String serverResponse;
    private final WebClient webClient;
    JFormattedTextField ipField[];
    Integer ipValue[];
    JSONObject roomList;
    JSONObject roomConfiguration;
    JSONObject clientConfig;
    String errorMessage;
    String roomName;
    Position position;
    String nameList[];
    
    /**
     * to init and end couniverse part of counsil
     */
    SessionManager sm;
        
    class IPTextFieldVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            JFormattedTextField tf = (JFormattedTextField) input;
            String strInput = tf.getText();
            if("".compareTo(strInput.trim()) == 0){
                strInput = "0";
            }
            Integer ipNumber = Integer.parseInt(strInput.trim());
            if(ipNumber > 255){ipNumber = 255;}
            if(ipNumber < 0){ipNumber = 0;}
            tf.setText(ipNumber.toString());
            return true;
        } 
    }
    
    InitialMenuLayout(Position cenerPosition, JSONObject clientConfiguration) {
        webClient = new WebClient();
        ipSetWindow = new JFrame();
        errorMessage = "nedokumentovana chyba";
        roomName = "none";
        roomList = null;
        font = new Font("Tahoma", 0, 18);
        roomConfiguration = null;
        errorWindow = new JFrame();
        roomChooseWindow = new JFrame();
        roleChooseWindow = new JFrame();
        position = cenerPosition;
        clientConfig = clientConfiguration;
        nameList = null;
        initIpSetWindow();
        initErrorWindow();
        initRoomChooseWindow();
        initRoleChooseWindow();
        startIpSetWindow();
    }
    
    final void initRoomChooseWindow(){
        if(roomChooseWindow == null){
            return;
        }
        roomChooseWindow.setTitle("CoUnSil");
        
        roomChooseWindow.setVisible(false);
    }
    
    final void initErrorWindow(){        
        if(errorWindow == null){
            return;
        }
        errorWindow.setTitle("CoUnSil");
        
        errorWindow.setVisible(false);
    }
    
    final void initIpSetWindow(){
        JPanel jMainPanel, jIPPanle;
                
        if(ipSetWindow == null){
            return;
        }
        
        //setUndecorated(true);
        ipSetWindow.setTitle("CoUnSil");
        
        jIPPanle = new JPanel();
        jMainPanel = new JPanel();
        jIPPanle.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 5));
        jMainPanel.setLayout(new GridLayout(4, 1));
        JLabel dot[] = new JLabel[3];
        JTextField infoField = new JTextField("Zadajte IP adresu servru na ktory sa chcete pripojit");
        infoField.setFont(font);
        infoField.setEditable(false);
        ipField = new JFormattedTextField[4];
        
        //set ip addres fields
        try {
            MaskFormatter ipMask = new MaskFormatter("***");
            ipMask.setValidCharacters("0123456789 ");
            for(int i=0; i<4; i++){
                ipField[i] = new JFormattedTextField(ipMask);
                ipField[i].setInputVerifier(new InitialMenuLayout.IPTextFieldVerifier());
                ipField[i].setColumns(3);
                ipField[i].setFont(font);
                jIPPanle.add(ipField[i]);
                if(i<3){
                    dot[i] = new JLabel(".");
                    dot[i].setFont(font);
                    jIPPanle.add(dot[i]);
                }
            } 
        } catch (ParseException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        JButton connectButton = new JButton("pripojiť na server");
        connectButton.setFont(font);
        connectButton.addActionListener((ActionEvent e) -> {
            roomList = getServerRoomList();
            readIpAddres();
            cleanIPField();
            if(roomList == null){
                errorMessage = "cannot connect to server";
                startErrorWindow();
            }else{
                startRoomChooseWindow(roomList);
            }
        });
        JButton exitButton = new JButton("exit");
        exitButton.setFont(font);
        exitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });
        
        jMainPanel.add(infoField);
        jMainPanel.add(jIPPanle);
        jMainPanel.add(connectButton);
        jMainPanel.add(exitButton);
        ipSetWindow.getContentPane().add(jMainPanel);
        //Display the window.
        ipSetWindow.pack();
        ipSetWindow.setVisible(false);
    }
    
    final void initRoleChooseWindow(){ 
                        
        roleChooseWindow.setVisible(false);
    }
    
    private void readIpAddres(){
        if(ipField == null){
            return;
        }
        ipSetWindow.setVisible(false);
        ipValue = new Integer[4];
        String addres = null;
        for(int i=0;i<ipField.length;i++){
            String tmp = ipField[i].getText().trim();
            if("".compareTo(tmp) == 0){
                tmp = "0";
            }
            ipValue[i] = Integer.parseInt(tmp);
        }
    }
    
    /**
     * 
     * @return ip addres set in ipField (if empty is 0)
     */
    private String getIpAddres(){
        if(ipField == null){
            return null;
        }
        if((ipValue == null) || (ipValue.length != 4)){
            readIpAddres();
        }  
        return ipValue[0].toString()+"."+ipValue[1].toString()+"."+ipValue[2].toString()+"."+ipValue[3].toString();
    }
    
    /**
     * return list of rooms form server (server address is getIpAddres())
     * @param room
     * @return list of rooms form server (server address is getIpAddres())
     */
    private JSONObject getServerRoomList() {
        try {
            String ipAddres = getIpAddres();
            if(ipAddres.compareTo("0.0.0.0") == 0){
                return null;
            }
            serverResponse = webClient.getRoomList(ipAddres, 80);
            System.out.println(serverResponse);
            if(serverResponse == null){
                return null;
            }
            return new JSONObject(serverResponse);
        } catch (JSONException | IOException ex) {
            serverResponse = null;
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
            String ipAddres = getIpAddres();
            if(ipAddres.compareTo("0.0.0.0") == 0){
                return null;
            }
            serverResponse = webClient.getRoom(ipAddres, 80, room);
            System.out.println(serverResponse);
            if(serverResponse == null){
                return null;
            }
            return new JSONObject(serverResponse);
        } catch (JSONException | IOException ex) {
            serverResponse = null;
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * open initial menu window for room chooesing
     * @param roomList 
     */
    private void startRoomChooseWindow(JSONObject roomList) {
        roomChooseWindow.setVisible(true);
        errorWindow.setVisible(false);
        ipSetWindow.setVisible(false);
        roleChooseWindow.setVisible(false);
        JPanel jMainPanel = new JPanel();
        
        if(roomList.isNull("names")){
            System.out.println("somthing is wrong");
            errorMessage = "server je ok, nemá zapnuté žiadne miestnosti";
            startErrorWindow();
            return;
        }
        
        roomChooseWindow.getContentPane().removeAll();
        //nameList = null;
        try {
            JSONArray rooms = roomList.getJSONArray("names");
            nameList = new String[rooms.length()];
            for (int i=0; i<rooms.length(); i++){
                JSONObject room = rooms.getJSONObject(i);
                nameList[i] = room.getString("name");
            }
        } catch (JSONException ex) {
            Logger.getLogger(InitialMenuLayout.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(nameList == null){
            errorMessage = "server je ok, chyba pri spracovani zoznamu miestnosti";
            startErrorWindow();
        }
        jMainPanel.setLayout(new GridLayout(nameList.length + 1, 1));
        JButton roomButon[] = new JButton[nameList.length];
        for (int i=0;i<nameList.length;i++) {
            roomButon[i] = new JButton(nameList[i]);
            roomButon[i].setFont(font);
            roomButon[i].addActionListener((ActionEvent e) -> {
                roomName = e.getActionCommand();
                startRoleChooseWindow();
            });
            jMainPanel.add(roomButon[i]);
        }
        JButton leaveButton =  new JButton("odpojiť zo servera");
        leaveButton.setFont(font);
        leaveButton.addActionListener((ActionEvent e) -> {
            cleanIPField();
            ipValue = null;
            startIpSetWindow();
        });
        jMainPanel.add(leaveButton);
        roomChooseWindow.getContentPane().add(jMainPanel);
        roomChooseWindow.pack();
        roomChooseWindow.setLocation(position.x - roomChooseWindow.getWidth() / 2, position.y - roomChooseWindow.getHeight() / 2);
    }
    
    /**
     * open initial menu window for errors
     */
    final void startErrorWindow(){
        roomChooseWindow.setVisible(false);
        errorWindow.setVisible(true);
        ipSetWindow.setVisible(false);
        roleChooseWindow.setVisible(false);
        
        errorWindow.getContentPane().removeAll();
        JPanel jButtonPanel = new JPanel();
        JPanel jMainPanel = new JPanel();
        jMainPanel.setLayout(new BorderLayout());
        JTextField infoField = new JTextField(errorMessage);
        infoField.setFont(font);
        infoField.setEditable(false);
        JButton okButton = new JButton("OK");
        okButton.setFont(font);
        okButton.addActionListener((ActionEvent e) -> {
            cleanIPField();
            ipValue = null;
            startIpSetWindow();
        });
        jMainPanel.add(infoField, BorderLayout.NORTH);
        jButtonPanel.add(okButton);
        jMainPanel.add(jButtonPanel, BorderLayout.SOUTH);
        errorWindow.getContentPane().add(jMainPanel);
        
        errorWindow.pack();
        errorWindow.setLocation(position.x - errorWindow.getWidth() / 2, position.y - errorWindow.getHeight());
    }
    
    /**
     * open initial menu window set ip addres
     */
    final void startIpSetWindow(){
        roomChooseWindow.setVisible(false);
        errorWindow.setVisible(false);
        ipSetWindow.setVisible(true);
        roleChooseWindow.setVisible(false);
        
        ipSetWindow.pack();
        ipSetWindow.setLocation(position.x - ipSetWindow.getWidth() / 2, position.y - ipSetWindow.getHeight() / 2);
    }
    
    /**
     * clean ip field so they can be set again
     */
    final void cleanIPField(){
        for(int i=0; i<4;i++){
            ipField[i].setText("");
        }
    }
    
    /**
     * open initial menu window role choose
     */
    final void startRoleChooseWindow(){
        roomChooseWindow.setVisible(false);
        errorWindow.setVisible(false);
        ipSetWindow.setVisible(false);
        roleChooseWindow.setVisible(true);
        
        JPanel jMainPanel = new JPanel();
        roleChooseWindow.getContentPane().removeAll();
        
        roleChooseWindow.setTitle("CoUnSil");
        
        JTextField roomLabel = new JTextField("miestnost: " + roomName);
        roomLabel.setEditable(false);
        
        JButton student = new JButton("študent");
        JButton teacher = new JButton("učiteľ");
        JButton interpreter = new JButton("prekladateľ");
        JButton changeRoom = new JButton("iná miestnosť");
        JButton leaveServer = new JButton("odpojiť zo servera");
        
        roomLabel.setFont(font);
        student.setFont(font);
        teacher.setFont(font);
        interpreter.setFont(font);
        changeRoom.setFont(font);
        leaveServer.setFont(font);
        
        
        student.addActionListener((ActionEvent e) -> {
            closeAllWindows();
            startCounsil("student");
        });
        teacher.addActionListener((ActionEvent e) -> {
            closeAllWindows();
            startCounsil("teacher");
        });
        interpreter.addActionListener((ActionEvent e) -> {
            closeAllWindows();
            startCounsil("interpreter");
        });
        changeRoom.addActionListener((ActionEvent e) -> {
            startRoomChooseWindow(roomList);
        });
        leaveServer.addActionListener((ActionEvent e) -> {
            cleanIPField();
            ipValue = null;
            startIpSetWindow();
        });
        
        jMainPanel.setLayout(new GridLayout(6, 1));
        
        jMainPanel.add(roomLabel);
        jMainPanel.add(student);
        jMainPanel.add(teacher);
        jMainPanel.add(interpreter);
        jMainPanel.add(changeRoom);
        jMainPanel.add(leaveServer);
        
        roleChooseWindow.getContentPane().add(jMainPanel);
        roleChooseWindow.pack();
        roleChooseWindow.setLocation(position.x - roleChooseWindow.getWidth() / 2, position.y - roleChooseWindow.getHeight() / 2);
    }
    
    /**
     * close all initial menu windows
     */
    final void closeAllWindows(){
        roomChooseWindow.setVisible(false);
        errorWindow.setVisible(false);
        ipSetWindow.setVisible(false);
        roleChooseWindow.setVisible(false);
    }
    
    
    /**
     * start cousil
     * @param role of the user
     */
    final void startCounsil(String role){
        try {
            setConfiguration(role);
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
        startIpSetWindow();
    }
    
    /**
     * create nodeConfig.json from others configuration files
     */
    final void setConfiguration(String role) throws InterruptedException{
        JSONObject infoFromServer = getRoomConfiguraton(roomName);
        if(infoFromServer == null){
            throw new InterruptedException("cannot get room configuration from server");
        }
        roomConfiguration = new JSONObject();
        JSONObject connector = new JSONObject();
        JSONObject localNode = new JSONObject();
        JSONObject consumer = new JSONObject();
        try {
            connector.put("serverAddress", getIpAddres());
            connector.put("serverPort", infoFromServer.getInt("comunication port"));
            connector.put("startServer", "false");

            String name = infoFromServer.getString("name") + "_" + role + "_" + clientConfig.getString("name");
            JSONObject interfaceInside = new JSONObject();
            interfaceInside.put("name", name);
            interfaceInside.put("address", clientConfig.getString("this ip"));
            interfaceInside.put("subnetName", infoFromServer.getString("name"));
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
            properties.put("audio", clientConfig.getBoolean("audio"));
            
            if(clientConfig.has("presentation producer")){
                properties.put("presentationProducer", clientConfig.getString("presentation producer"));
            }

            localNode.put("name", name);
            localNode.put("interfaces", interfaces);
            localNode.put("properties", properties);

            JSONObject templates = new JSONObject();
            JSONObject JSONproducer = new JSONObject();
            JSONproducer.put("path", clientConfig.getString("ultragrid path"));
            System.out.println("path to freedom" + clientConfig.getString("ultragrid path"));
            JSONproducer.put("arguments", "");
            JSONObject JSONdistributor = new JSONObject();
            JSONdistributor.put("path", clientConfig.getString("distributor path"));
            JSONdistributor.put("arguments", "8M");
            JSONObject JSONconsumer = new JSONObject();
            JSONconsumer.put("path", clientConfig.getString("ultragrid path"));
            JSONconsumer.put("arguments", "");
            templates.put("producer", JSONproducer);
            templates.put("distributor", JSONdistributor);
            templates.put("consumer", JSONconsumer);

            roomConfiguration.put("connector", connector);
            roomConfiguration.put("localNode", localNode);
            roomConfiguration.put("templates", templates);

            FileWriter file = new FileWriter("nodeConfig.json");
                   
        System.out.println(roomConfiguration.toString());
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
}
