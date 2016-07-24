/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author xminarik
 */
public final class OptionsMainMenuWindow extends JFrame{
    JPanel mainPanel, visualitationPanel, videoAudioPanel, miscsPanel;
    JTabbedPane mainTabPanel;
    Font fontButtons;
    List<VideoDevice> videoDevices;
    List<AudioDevice> audioIn;
    List<AudioDevice> audioOut;
    List<IPServerSaved> ipAddresses;
    boolean correctUv;
    boolean havePortaudio;
    Process uvProcess;
    JSONObject configuration;
    String uvPathString;
    String layoutPathString;
    JTextArea verificationText;
    Color raiseHandColor;
    Color talkingColor;
    JColorChooser raiseHandcolorChooser;   
    JColorChooser talkingColorChooser;
    JTextField setResizeAmount;
    File configurationFile;
    JTextField myIpSetTextField;
    InitialMenuLayout imt;    //need to reload server choosing window
    String displaySetting;
    boolean presentationUsed;
    
    //need to be global so they can be set from different tab
    JComboBox mainCameraBox;
    JComboBox mainCameraPixelFormatBox;
    JComboBox mainCameraFrameSizeBox;
    JComboBox mainCameraFPSBox;
    JComboBox presentationBox;
    JComboBox presentationPixelFormatBox;
    JComboBox presentationFrameSizeBox;
    JComboBox presentationFPSBox;
    JComboBox displayBox;
    JComboBox displaySettingBox;
    JComboBox audioInComboBox;
    JComboBox audioOutComboBox;
    JComboBox languageCombobox;
    JTextField cameraSettingText;
    JTextField presentationSettingText;
    
    ResourceBundle languageBundle;
    
    // constructor
    OptionsMainMenuWindow(Font fontButtons, File configurationFile, InitialMenuLayout initialMenuLayout, ResourceBundle lenguageBundle)
    {
        super(lenguageBundle.getString("COUNSIL_OPTIONS"));
        this.fontButtons = fontButtons;
        this.languageBundle = lenguageBundle;
        videoDevices = new ArrayList<>();
        audioIn = new ArrayList<>();
        audioOut = new ArrayList<>();
        ipAddresses = new ArrayList<>();
        correctUv = false;
        havePortaudio = false;
        uvPathString = "";
        layoutPathString = "";
        verificationText = new JTextArea();
        verificationText.setBackground(this.getBackground());
        verificationText.setEditable(false);
        verificationText.setBorder(BorderFactory.createEmptyBorder());
        raiseHandcolorChooser = new JColorChooser(new Color(0, 0, 0));
        talkingColorChooser = new JColorChooser(new Color(0, 0, 0));
        this.configurationFile = configurationFile;
        myIpSetTextField = new JTextField();
        imt = initialMenuLayout;
        
        mainCameraBox = new JComboBox();
        mainCameraPixelFormatBox = new JComboBox();
        mainCameraFrameSizeBox = new JComboBox();
        mainCameraFPSBox = new JComboBox();
        presentationBox = new JComboBox();
        presentationPixelFormatBox = new JComboBox();
        presentationFrameSizeBox = new JComboBox();
        presentationFPSBox = new JComboBox();
        displayBox = new JComboBox();
        displaySettingBox = new JComboBox();
        audioInComboBox = new JComboBox();
        audioOutComboBox = new JComboBox();
        languageCombobox = new JComboBox();
        
        cameraSettingText = new JTextField();
        presentationSettingText = new JTextField();
        cameraSettingText.setEditable(false);
        presentationSettingText.setEditable(false);
        cameraSettingText.setBorder(BorderFactory.createEmptyBorder());
        presentationSettingText.setBorder(BorderFactory.createEmptyBorder());
        
        configuration = readJsonFile(configurationFile);
        
        raiseHandColor = new Color(255, 0, 0);
        if(configuration.has("raise hand color")){
            JSONObject raiseHandColorJson;
            try {
                raiseHandColorJson = configuration.getJSONObject("raise hand color");
                if(raiseHandColorJson.has("red") && raiseHandColorJson.has("green") && raiseHandColorJson.has("blue")){
                int red = raiseHandColorJson.getInt("red");
                int green = raiseHandColorJson.getInt("green");
                int blue = raiseHandColorJson.getInt("blue");
                raiseHandColor = new Color(red, green, blue);
            }
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        talkingColor = new Color(0, 255, 255);
        if(configuration.has("talking color")){
            JSONObject raiseHandColorJson;
            try {
                raiseHandColorJson = configuration.getJSONObject("talking color");
                if(raiseHandColorJson.has("red") && raiseHandColorJson.has("green") && raiseHandColorJson.has("blue")){
                    int red = raiseHandColorJson.getInt("red");
                    int green = raiseHandColorJson.getInt("green");
                    int blue = raiseHandColorJson.getInt("blue");
                    talkingColor = new Color(red, green, blue);
                }
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        if(configuration.has("talking resizing")){
            try {
                setResizeAmount = new JTextField(String.valueOf((int)configuration.getDouble("talking resizing")));
            } catch (JSONException ex) {
                setResizeAmount = new JTextField();
            }
        }else{
            setResizeAmount = new JTextField();
        }
        if(configuration.has("presentation")){
            try {
                presentationUsed = configuration.getBoolean("presentation");
            } catch (JSONException ex) {
                presentationUsed = false;
            }
        }
        setResizeAmount.setBorder(BorderFactory.createEmptyBorder());
        uvProcess = null;
        mainPanel = new JPanel();
        setLayout(new GridBagLayout());
        visualitationPanel = new JPanel();
        videoAudioPanel = new JPanel();
        videoAudioPanel.setLayout(new GridBagLayout());
        miscsPanel = new JPanel();
        mainTabPanel = new JTabbedPane();
        mainTabPanel.addTab(lenguageBundle.getString("VISUALITATION"), visualitationPanel);
        mainTabPanel.addTab(lenguageBundle.getString("AUDIO_VIDEO"), videoAudioPanel);
        mainTabPanel.addTab(lenguageBundle.getString("MISC"), miscsPanel);
        
        addWindowListener(new WindowAdapter() {//action on close button (x)
            @Override
            public void windowClosing(WindowEvent e) {
                if(uvProcess != null){
                    uvProcess.destroy();
                }
                dispose();
            }
        });
        
        JButton saveButton = new JButton(lenguageBundle.getString("SAVE"));
        saveButton.setFont(fontButtons);
        saveButton.addActionListener((ActionEvent event) -> {
            if(uvProcess != null){
                uvProcess.destroy();
            }
            saveSettingAction();
        });
        JButton discardButton = new JButton(lenguageBundle.getString("DISCARD"));
        discardButton.setFont(fontButtons);
        discardButton.addActionListener((ActionEvent event) -> {
            if(uvProcess != null){
                uvProcess.destroy();
            }
            dispose();
        });
        setVisualitationPanel();
        setVideoAudioPanel();
        setMiscsPanel();
        
        //setting fields
                
        //try if ultragrid is functional
        ultragridOK(uvPathString, verificationText);
        //load posibylities
        try {
            videoDevices = loadVideoDevicesAndSettings(uvPathString);
            audioIn = read_audio_devices_in_or_out(uvPathString, true);
            audioOut = read_audio_devices_in_or_out(uvPathString, false);
        } catch (IOException ex) {
            videoDevices = new ArrayList<>();
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        setAudioJComboBox(audioInComboBox, audioIn);
        setAudioJComboBox(audioOutComboBox, audioOut);
        
        setAllJComboBoxesVideosetting(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
        setAllJComboBoxesVideosetting(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        
        setJComboBoxDisplay(displayBox, displaySettingBox);
        
        try {
            String videoSetting = configuration.getString("producer settings");
            SetVideoSettingFromConfig(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices, videoSetting);
            if(presentationUsed){
                String presentationSetting = configuration.getString("presentation producer");
                SetVideoSettingFromConfig(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices, presentationSetting);
            }
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridheight = 1;
        mainPanelConstraints.gridwidth = 3;
        add(mainTabPanel, mainPanelConstraints);
        mainPanelConstraints.anchor = GridBagConstraints.LAST_LINE_END;
        mainPanelConstraints.ipady = 0;
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.gridheight = 1;
        mainPanelConstraints.gridwidth = 1;
        add(saveButton, mainPanelConstraints);
        mainPanelConstraints.weightx = 0.5;
        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.gridheight = 1;
        mainPanelConstraints.gridwidth = 1;
        add(discardButton, mainPanelConstraints);
        setVisible(true);
        setResizable(false);
        pack();

    }

    private void setVisualitationPanel(){
        JPanel raiseHandColorPanel = new JPanel();
        JPanel talkingColorPanel = new JPanel();
        JPanel resazingSizePanel = new JPanel();
        JPanel lenguagePanel = new JPanel();
        
        JTextField lenguageInfoTextField = new JTextField(languageBundle.getString("LANGUAGE"));
        lenguageInfoTextField.setEditable(false);
        lenguageInfoTextField.setBorder(BorderFactory.createEmptyBorder());
        languageCombobox.setEditable(false);
        languageCombobox.setLightWeightPopupEnabled(true);
        String setLenguage = "";
        try {
            setLenguage = configuration.getString("language");
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        fillLanguageComboBox(languageCombobox, setLenguage);
        lenguagePanel.setBorder(new TitledBorder(languageBundle.getString("LANGUAGE")));     
               
        JTextField setResizeAmountTextInfo = new JTextField(languageBundle.getString("RESAZING_INFO_TEXT"));
        JTextField setResizePixelSign = new JTextField(languageBundle.getString("PIXELS")); 
        setResizeAmountTextInfo.setBorder(BorderFactory.createEmptyBorder());
        setResizePixelSign.setBorder(BorderFactory.createEmptyBorder());
        setResizeAmount.setEditable(true);
        setResizeAmountTextInfo.setEditable(false);
        setResizePixelSign.setEditable(false); 
        setResizeAmountTextInfo.setHorizontalAlignment(JTextField.RIGHT);
        setResizeAmount.setHorizontalAlignment(JTextField.CENTER);
        setResizePixelSign.setHorizontalAlignment(JTextField.LEFT);
        setResizeAmount.setColumns(3);
        resazingSizePanel.setBorder(new TitledBorder(languageBundle.getString("RESAZING")));
        
        
        raiseHandcolorChooser.setPreviewPanel(new CustomPreviewPanel(new Dimension(100, 100)));
        raiseHandcolorChooser.setLayout(new FlowLayout());
        //Remove the default chooser panels
        AbstractColorChooserPanel raiseHandColorPanelsToRemove[] = raiseHandcolorChooser.getChooserPanels();
        for (int i = 0; i < raiseHandColorPanelsToRemove.length; i ++) {
            raiseHandcolorChooser.removeChooserPanel(raiseHandColorPanelsToRemove[i]);
        }
        raiseHandcolorChooser.addChooserPanel(new RGBChooserPanel(languageBundle));
        raiseHandcolorChooser.setColor(raiseHandColor);
        raiseHandColorPanel.setBorder(new TitledBorder(languageBundle.getString("COLOR_RISE_HAND")));
        raiseHandColorPanel.add(raiseHandcolorChooser);
        
        
        talkingColorChooser.setPreviewPanel(new CustomPreviewPanel(new Dimension(100, 100)));
        talkingColorChooser.setLayout(new FlowLayout());
        //Remove the default chooser panels
        AbstractColorChooserPanel talkingColorPanelsToRemove[] = talkingColorChooser.getChooserPanels();
        for (int i = 0; i < talkingColorPanelsToRemove.length; i ++) {
            talkingColorChooser.removeChooserPanel(talkingColorPanelsToRemove[i]);
        }
        talkingColorChooser.addChooserPanel(new RGBChooserPanel(languageBundle));
        talkingColorChooser.setColor(talkingColor);
        talkingColorPanel.setBorder(new TitledBorder(languageBundle.getString("COLOR_TALKING")));
        talkingColorPanel.add(talkingColorChooser);
        
        if(languageBundle.containsKey("VIS_TOOL_TIP_LENGUAGE")){
            languageCombobox.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_LENGUAGE"));
        }
        if(languageBundle.containsKey("VIS_TOOL_TIP_RESAZING")){
            setResizeAmountTextInfo.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_RESAZING"));
            setResizeAmount.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_RESAZING"));
            setResizePixelSign.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_RESAZING"));
        }
        if(languageBundle.containsKey("VIS_TOOL_TIP_COLOR_RISE_HAND")){
            raiseHandColorPanel.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_COLOR_RISE_HAND"));
        }
        if(languageBundle.containsKey("VIS_TOOL_TIP_COLOR_TALKING")){
            talkingColorPanel.setToolTipText(languageBundle.getString("VIS_TOOL_TIP_COLOR_TALKING"));
        }
        
        lenguagePanel.setLayout(new GridBagLayout());
        GridBagConstraints lenguagePanelConstrains = new GridBagConstraints();
        lenguagePanelConstrains.insets = new Insets(5,5,5,5);
        lenguagePanelConstrains.weightx = 0.5;
        lenguagePanelConstrains.gridheight = 1;
        lenguagePanelConstrains.gridwidth = 1;
        lenguagePanelConstrains.gridx = 1;
        lenguagePanelConstrains.gridy = 0;
        /*lenguagePanel.add(lenguageInfoTextField, lenguagePanelConstrains);
        lenguagePanelConstrains.gridx = 1;
        lenguagePanelConstrains.gridy = 0;*/
        
        lenguagePanelConstrains.anchor = GridBagConstraints.CENTER;
        lenguagePanel.add(languageCombobox, lenguagePanelConstrains);
        
        resazingSizePanel.setLayout(new GridBagLayout());
        GridBagConstraints resazingSizePanelConstrains = new GridBagConstraints();
        resazingSizePanelConstrains.insets = new Insets(5,5,5,5);
        //resazingSizePanelConstrains.weightx = 0.5;
        resazingSizePanelConstrains.gridheight = 1;
        resazingSizePanelConstrains.gridwidth = 1;
        resazingSizePanelConstrains.gridx = 0;
        resazingSizePanelConstrains.gridy = 0;
        resazingSizePanel.add(setResizeAmountTextInfo, resazingSizePanelConstrains);
        resazingSizePanelConstrains.gridx = 1;
        resazingSizePanelConstrains.gridy = 0;
        resazingSizePanel.add(setResizeAmount, resazingSizePanelConstrains);
        resazingSizePanelConstrains.gridx = 2;
        resazingSizePanelConstrains.gridy = 0;
        resazingSizePanel.add(setResizePixelSign, resazingSizePanelConstrains);
        
        visualitationPanel.setLayout(new GridBagLayout());
        GridBagConstraints visualitationPanelConstrains = new GridBagConstraints();
        visualitationPanelConstrains.fill = GridBagConstraints.HORIZONTAL;
        visualitationPanelConstrains.weightx = 0.5;
        visualitationPanelConstrains.gridheight = 1;
        visualitationPanelConstrains.gridwidth = 1;
        visualitationPanelConstrains.gridx = 0;
        visualitationPanelConstrains.gridy = 0;
        visualitationPanel.add(lenguagePanel, visualitationPanelConstrains);
        visualitationPanelConstrains.gridx = 0;
        visualitationPanelConstrains.gridy = 1;
        visualitationPanel.add(resazingSizePanel, visualitationPanelConstrains);
        visualitationPanelConstrains.gridx = 0;
        visualitationPanelConstrains.gridy = 2;
        visualitationPanel.add(raiseHandColorPanel, visualitationPanelConstrains);
        visualitationPanelConstrains.gridx = 0;
        visualitationPanelConstrains.gridy = 3;
        visualitationPanel.add(talkingColorPanel, visualitationPanelConstrains);
    }
    
    private void setVideoAudioPanel(){
        
        
        JPanel mainCameraPanel = new JPanel();
        mainCameraPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("CAMERA")));
        JPanel presetationPanel = new JPanel();
        presetationPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("PRESENTATION")));
        JPanel displayPanel = new JPanel();
        displayPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("DISPLAY")));
        JPanel audioPanel = new JPanel();
        audioPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("AUDIO")));
        
        //setting boxes, buttons and lables
        //boxes
        //createt gloabay so they can be change globay
        
        //set not editable
        mainCameraBox.setEditable(false);
        mainCameraPixelFormatBox.setEditable(false);
        mainCameraFrameSizeBox.setEditable(false);
        mainCameraFPSBox.setEditable(false);
        presentationBox.setEditable(false);
        presentationPixelFormatBox.setEditable(false);
        presentationFrameSizeBox.setEditable(false);
        presentationFPSBox.setEditable(false);
        displayBox.setEditable(false);
        displaySettingBox.setEditable(false);
        audioInComboBox.setEditable(false);
        audioOutComboBox.setEditable(false);
       
        //set action
        mainCameraBox.addActionListener((ActionEvent event) -> {
            actionSetCameraDeviceBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
        });
        mainCameraPixelFormatBox.addActionListener((ActionEvent event) -> {
            actionSetCameraPixelFormatBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
        });
        mainCameraFrameSizeBox.addActionListener((ActionEvent event) -> {
            actionSetCameraFrameSizeBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
        });
        mainCameraFPSBox.addActionListener((ActionEvent event) -> {
            actionSetFPSBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
        });
        presentationBox.addActionListener((ActionEvent event) -> {
            actionSetCameraDeviceBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        });
        presentationPixelFormatBox.addActionListener((ActionEvent event) -> {
            actionSetCameraPixelFormatBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        });
        presentationFrameSizeBox.addActionListener((ActionEvent event) -> {
            actionSetCameraFrameSizeBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        });
        presentationFPSBox.addActionListener((ActionEvent event) -> {
            actionSetFPSBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        });

        //info fields
        JTextField displayDeviceText = new JTextField(languageBundle.getString("DISPLAY_SETTING"));
        JTextField displaySettingText = new JTextField(languageBundle.getString("DISPLAY_SETTING_ADVANCED"));
        JTextField cameraDeviceText = new JTextField(languageBundle.getString("CAMERA"));
        JTextField cameraPixelFormatText = new JTextField(languageBundle.getString("FORMAT"));
        JTextField cameraFrameSizeText = new JTextField(languageBundle.getString("SIZE"));
        JTextField cameraFPSText = new JTextField(languageBundle.getString("FPS"));
        JTextField presentationDeviceText = new JTextField(languageBundle.getString("DEVICE"));
        JTextField presentationPixelFormatText = new JTextField(languageBundle.getString("FORMAT"));
        JTextField presentationFrameSizeText = new JTextField(languageBundle.getString("SIZE"));
        JTextField presentationFPSText = new JTextField(languageBundle.getString("FPS"));
        JTextField audioInText = new JTextField(languageBundle.getString("AUDIO_IN"));
        JTextField audioOutText = new JTextField(languageBundle.getString("AUDIO_OUT"));
      	JTextField cameraSettingInfoText = new JTextField(languageBundle.getString("DEVICE_SETTING"));
        JTextField presentationSettingInfoText = new JTextField(languageBundle.getString("DEVICE_SETTING"));

        displayDeviceText.setEditable(false);
        displaySettingText.setEditable(false);
        cameraDeviceText.setEditable(false);
        cameraPixelFormatText.setEditable(false);
        cameraFrameSizeText.setEditable(false);
        cameraFPSText.setEditable(false);
        presentationDeviceText.setEditable(false);
        presentationPixelFormatText.setEditable(false);
        presentationFrameSizeText.setEditable(false);
        presentationFPSText.setEditable(false);
        audioInText.setEditable(false);
        audioOutText.setEditable(false);
        cameraSettingInfoText.setEditable(false);
        presentationSettingInfoText.setEditable(false);
        displayDeviceText.setBorder(BorderFactory.createEmptyBorder());
        displaySettingText.setBorder(BorderFactory.createEmptyBorder());
        cameraDeviceText.setBorder(BorderFactory.createEmptyBorder());
        cameraPixelFormatText.setBorder(BorderFactory.createEmptyBorder());
        cameraFrameSizeText.setBorder(BorderFactory.createEmptyBorder());
        cameraFPSText.setBorder(BorderFactory.createEmptyBorder());
        presentationDeviceText.setBorder(BorderFactory.createEmptyBorder());
        presentationPixelFormatText.setBorder(BorderFactory.createEmptyBorder());
        presentationFrameSizeText.setBorder(BorderFactory.createEmptyBorder());
        presentationFPSText.setBorder(BorderFactory.createEmptyBorder());
        audioInText.setBorder(BorderFactory.createEmptyBorder());
        audioOutText.setBorder(BorderFactory.createEmptyBorder());
        cameraSettingInfoText.setBorder(BorderFactory.createEmptyBorder());
        presentationSettingInfoText.setBorder(BorderFactory.createEmptyBorder());
        if(languageBundle.containsKey("AV_TOOL_TIP_DISPLAY_DEVICE")){
            displayDeviceText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_DISPLAY_DEVICE"));
            displayBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_DISPLAY_DEVICE"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_DISPLAY_SETTING")){
            displaySettingText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_DISPLAY_SETTING"));
            displaySettingBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_DISPLAY_SETTING"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_CAMERA_DEVICE")){
            cameraDeviceText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_DEVICE"));
            mainCameraBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_DEVICE"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_CAMERA_PIXEL_FORMAT")){
            cameraPixelFormatText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_PIXEL_FORMAT"));
            mainCameraPixelFormatBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_PIXEL_FORMAT"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_CAMERA_SIZE")){
            cameraFrameSizeText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_SIZE"));
            mainCameraFrameSizeBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_SIZE"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_CAMERA_FPS")){
            cameraFPSText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_FPS"));
            mainCameraFPSBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_FPS"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_PRESENTATION_DEVICE")){
            presentationDeviceText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_DEVICE"));
            presentationBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_DEVICE"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_PRESENTATION_PIXEL_FORMAT")){
            presentationPixelFormatText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_PIXEL_FORMAT"));
            presentationPixelFormatBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_PIXEL_FORMAT"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_PRESENTATION_SIZE")){
            presentationFrameSizeText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_SIZE"));
            presentationFrameSizeBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_SIZE"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_PRESENTATION_FPS")){
            presentationFPSText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_FPS"));
            presentationFPSBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_FPS"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_AUDIO_IN")){
            audioInText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_AUDIO_IN"));
            audioInComboBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_AUDIO_IN"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_AUDIO_OUT")){
            audioOutText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_AUDIO_OUT"));
            audioOutComboBox.setToolTipText(languageBundle.getString("AV_TOOL_TIP_AUDIO_OUT"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_CAMERA_DEVICE_SETTING")){
            cameraSettingText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_DEVICE_SETTING"));
            cameraSettingInfoText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_CAMERA_DEVICE_SETTING"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_PRESENTATION_DEVICE_SETTING")){
            presentationSettingText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_DEVICE_SETTING"));
            presentationSettingInfoText.setToolTipText(languageBundle.getString("AV_TOOL_TIP_PRESENTATION_DEVICE_SETTING"));
        }
        //buttons
        JButton testCameraButton = new JButton(languageBundle.getString("TEST_CAMERA"));
        JButton testPresentationButton = new JButton(languageBundle.getString("TEST_PRESENTATION"));
        testCameraButton.setFont(fontButtons);
        testPresentationButton.setFont(fontButtons);
        if(languageBundle.containsKey("AV_TOOL_TIP_TEST_CAMERA")){
            testCameraButton.setToolTipText(languageBundle.getString("AV_TOOL_TIP_TEST_CAMERA"));
        }
        if(languageBundle.containsKey("AV_TOOL_TIP_TEST_PRESENTATION")){
            testPresentationButton.setToolTipText(languageBundle.getString("AV_TOOL_TIP_TEST_PRESENTATION"));
        }
        testCameraButton.addActionListener((ActionEvent event) -> {
            try {
                String reciveSetting = "";
                if(displayBox.getItemCount() > 0){
                    reciveSetting = displayBox.getSelectedItem().toString();
                    if(displaySettingBox.getItemCount() > 0){
                        if(displaySettingBox.getSelectedItem().toString().equals("nodecorate")){
                            reciveSetting += ":" + displaySettingBox.getSelectedItem().toString();
                        }
                    }
                }
                String outputSetting = getVideoSettings(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
                startUltragrid(uvPathString, reciveSetting, outputSetting);
            } catch (IOException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        testPresentationButton.addActionListener((ActionEvent event) -> {
            try {
                String reciveSetting = "";
                if(displayBox.getItemCount() > 0){
                    reciveSetting = displayBox.getSelectedItem().toString();
                    if(displaySettingBox.getItemCount() > 0){
                        if(displaySettingBox.getSelectedItem().toString().equals("nodecorate")){
                            reciveSetting += ":" + displaySettingBox.getSelectedItem().toString();
                        }
                    }
                }
                String outputSetting = getVideoSettings(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
                startUltragrid(uvPathString, reciveSetting, outputSetting);
            } catch (IOException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //check box
        JCheckBox presentationCheckBox = new JCheckBox(languageBundle.getString("PRESENTATION"));
        presentationCheckBox.addItemListener((ItemEvent e) -> {
            boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
            presetationPanel.setVisible(isSelected);
            testPresentationButton.setVisible(isSelected);
            presentationUsed = isSelected;
            this.pack();
        });
        
        //putting boxes to panel
        mainCameraPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainCameraPanelConstrains = new GridBagConstraints();
        mainCameraPanelConstrains.insets = new Insets(5,5,5,5);
        mainCameraPanelConstrains.weightx = 0.5;
        mainCameraPanelConstrains.gridheight = 1;
        mainCameraPanelConstrains.gridwidth = 1;
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 0;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_END;
        mainCameraPanel.add(cameraDeviceText, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 1;
        mainCameraPanelConstrains.gridy = 0;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_START;
        mainCameraPanel.add(mainCameraBox, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 1;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_END;
        mainCameraPanel.add(cameraPixelFormatText, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 1;
        mainCameraPanelConstrains.gridy = 1;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_START;
        mainCameraPanel.add(mainCameraPixelFormatBox, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 2;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_END;
        mainCameraPanel.add(cameraFrameSizeText, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 1;
        mainCameraPanelConstrains.gridy = 2;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_START;
        mainCameraPanel.add(mainCameraFrameSizeBox, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 3;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_END;
        mainCameraPanel.add(cameraFPSText, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 1;
        mainCameraPanelConstrains.gridy = 3;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_START;
        mainCameraPanel.add(mainCameraFPSBox, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 4;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_END;
        mainCameraPanel.add(cameraSettingInfoText, mainCameraPanelConstrains);
        mainCameraPanelConstrains.gridx = 1;
        mainCameraPanelConstrains.gridy = 4;
        mainCameraPanelConstrains.anchor = GridBagConstraints.LINE_START;
        mainCameraPanel.add(cameraSettingText, mainCameraPanelConstrains);
        
        presetationPanel.setLayout(new GridBagLayout());        
        GridBagConstraints presetationPanelConstrains = new GridBagConstraints();        
        presetationPanelConstrains.insets = new Insets(5,5,5,5);
        presetationPanelConstrains.weightx = 0.5;
        presetationPanelConstrains.gridx = 0;
        presetationPanelConstrains.gridy = 0;
        presetationPanelConstrains.gridheight = 1;
        presetationPanelConstrains.gridwidth = 1;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_END;
        presetationPanel.add(presentationDeviceText, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 1;
        presetationPanelConstrains.gridy = 0;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_START;
        presetationPanel.add(presentationBox, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 0;
        presetationPanelConstrains.gridy = 1;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_END;
        presetationPanel.add(presentationPixelFormatText, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 1;
        presetationPanelConstrains.gridy = 1;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_START;
        presetationPanel.add(presentationPixelFormatBox, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 0;
        presetationPanelConstrains.gridy = 2;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_END;
        presetationPanel.add(presentationFrameSizeText, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 1;
        presetationPanelConstrains.gridy = 2;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_START;
        presetationPanel.add(presentationFrameSizeBox, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 0;
        presetationPanelConstrains.gridy = 3;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_END;
        presetationPanel.add(presentationFPSText, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 1;
        presetationPanelConstrains.gridy = 3;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_START;
        presetationPanel.add(presentationFPSBox, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 0;
        presetationPanelConstrains.gridy = 4;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_END;
        presetationPanel.add(presentationSettingInfoText, presetationPanelConstrains);
        presetationPanelConstrains.gridx = 1;
        presetationPanelConstrains.gridy = 4;
        presetationPanelConstrains.anchor = GridBagConstraints.LINE_START;
        presetationPanel.add(presentationSettingText, presetationPanelConstrains);
        
        displayPanel.setLayout(new GridBagLayout());        
        GridBagConstraints displayPanelConstrains = new GridBagConstraints();
        displayPanelConstrains.insets = new Insets(5,5,5,5);
        displayPanelConstrains.weightx = 0.5;
        displayPanelConstrains.gridx = 0;
        displayPanelConstrains.gridy = 0;
        displayPanelConstrains.gridheight = 1;
        displayPanelConstrains.gridwidth = 1;
        displayPanelConstrains.anchor = GridBagConstraints.LINE_END;
        displayPanel.add(displayDeviceText, displayPanelConstrains);
        displayPanelConstrains.gridx = 1;
        displayPanelConstrains.gridy = 0;
        displayPanelConstrains.anchor = GridBagConstraints.LINE_START;
        displayPanel.add(displayBox, displayPanelConstrains);
        displayPanelConstrains.gridx = 0;
        displayPanelConstrains.gridy = 1;
        displayPanelConstrains.anchor = GridBagConstraints.LINE_END;
        displayPanel.add(displaySettingText, displayPanelConstrains);
        displayPanelConstrains.gridx = 1;
        displayPanelConstrains.gridy = 1;
        displayPanelConstrains.anchor = GridBagConstraints.LINE_START;
        displayPanel.add(displaySettingBox, displayPanelConstrains);
        
        audioPanel.setLayout(new GridBagLayout());
        GridBagConstraints AudioConstrains = new GridBagConstraints();
        AudioConstrains.insets = new Insets(5,5,5,5);
        AudioConstrains.anchor = GridBagConstraints.LINE_END;
        AudioConstrains.weightx = 0.5;
        AudioConstrains.gridheight = 1;
        AudioConstrains.gridwidth = 1;
        AudioConstrains.gridx = 0;
        AudioConstrains.gridy = 0;
        audioPanel.add(audioInText, AudioConstrains);
        AudioConstrains.anchor = GridBagConstraints.LINE_START;
        AudioConstrains.gridx = 1;
        AudioConstrains.gridy = 0;
        audioPanel.add(audioInComboBox, AudioConstrains);
        AudioConstrains.anchor = GridBagConstraints.LINE_END;
        AudioConstrains.gridx = 0;
        AudioConstrains.gridy = 1;
        audioPanel.add(audioOutText, AudioConstrains);
        AudioConstrains.anchor = GridBagConstraints.LINE_START;
        AudioConstrains.gridx = 1;
        AudioConstrains.gridy = 1;
        audioPanel.add(audioOutComboBox, AudioConstrains);
        
        videoAudioPanel.setLayout(new GridBagLayout());
        GridBagConstraints videoAudioConstrains = new GridBagConstraints();
        videoAudioConstrains.fill = GridBagConstraints.HORIZONTAL;
        videoAudioConstrains.insets = new Insets(5,5,5,5);
        videoAudioConstrains.anchor = GridBagConstraints.LINE_END;
        videoAudioConstrains.weightx = 0.5;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 1;
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 0;

        videoAudioConstrains.anchor = GridBagConstraints.CENTER;
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 3;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 2;
        videoAudioPanel.add(verificationText, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 4;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 3;
        videoAudioPanel.add(audioPanel, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 5;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 3;
        videoAudioPanel.add(displayPanel, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 6;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 3;
        videoAudioPanel.add(mainCameraPanel, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 7;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 1;
        videoAudioPanel.add(presentationCheckBox, videoAudioConstrains);
        videoAudioConstrains.gridx = 2;
        videoAudioConstrains.gridy = 7;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 1;
        videoAudioConstrains.ipadx = 30;
        videoAudioPanel.add(testCameraButton, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 8;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 3;
        videoAudioConstrains.ipadx = 0;
        videoAudioPanel.add(presetationPanel, videoAudioConstrains);
        videoAudioConstrains.gridx = 2;
        videoAudioConstrains.gridy = 9;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 1;
        videoAudioPanel.add(testPresentationButton, videoAudioConstrains);
        
        presetationPanel.setVisible(false);
        testPresentationButton.setVisible(false);
        presentationCheckBox.setSelected(presentationUsed);
    }
    
    private void setMiscsPanel(){
        
        JPanel myIpAddressPanel = new JPanel();
        myIpAddressPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("MY_IP")));
        JPanel serverIpSettingPanel = new JPanel();
        serverIpSettingPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("SERVER_IP_SETTING")));
        JPanel addressPanel = new JPanel();
        addressPanel.setBorder(BorderFactory.createTitledBorder(languageBundle.getString("PATHS")));
        
        myIpSetTextField.setEditable(true);
        myIpSetTextField.setColumns(10);
        myIpSetTextField.setBorder(BorderFactory.createEmptyBorder());
        JTextField myIpSetTextFieldInfoText = new JTextField(languageBundle.getString("MY_IP"));
        myIpSetTextFieldInfoText.setEditable(false);
        myIpSetTextFieldInfoText.setBorder(BorderFactory.createEmptyBorder());
        
        JTextField serverIpAddresChangeTextField = new JTextField();
        JTextField serverIpNameChange = new JTextField();
        JTextField serverIpPortChange = new JTextField();
        serverIpAddresChangeTextField.setColumns(10);
        serverIpNameChange.setColumns(13);
        serverIpPortChange.setColumns(13);
        serverIpAddresChangeTextField.setBorder(BorderFactory.createEmptyBorder());
        serverIpNameChange.setBorder(BorderFactory.createEmptyBorder());
        serverIpPortChange.setBorder(BorderFactory.createEmptyBorder());
        
        //text to explane fields
        JTextField serverIpAddresChangeTextFieldInfoText = new JTextField(languageBundle.getString("IP_ADDRESS_SERVER"));
        JTextField serverIpNameChangeInfoText = new JTextField(languageBundle.getString("SERVER_NAME"));
        JTextField serverIpPortChangeInfoText = new JTextField(languageBundle.getString("SERVER_PORT"));
        JTextField uvPathInfoText = new JTextField(languageBundle.getString("UV_PATH"));
        JTextField layoutPathInfoText = new JTextField(languageBundle.getString("LAYOUT_PATH"));
        serverIpAddresChangeTextFieldInfoText.setEditable(false);
        serverIpNameChangeInfoText.setEditable(false);
        serverIpPortChangeInfoText.setEditable(false);
        uvPathInfoText.setEditable(false);
        layoutPathInfoText.setEditable(false);
        serverIpAddresChangeTextFieldInfoText.setHorizontalAlignment(JTextField.RIGHT);
        serverIpNameChangeInfoText.setHorizontalAlignment(JTextField.RIGHT);
        serverIpPortChangeInfoText.setHorizontalAlignment(JTextField.RIGHT);
        uvPathInfoText.setHorizontalAlignment(JTextField.RIGHT);
        layoutPathInfoText.setHorizontalAlignment(JTextField.RIGHT);
       
        serverIpAddresChangeTextFieldInfoText.setBorder(BorderFactory.createEmptyBorder());
        serverIpNameChangeInfoText.setBorder(BorderFactory.createEmptyBorder());
        serverIpPortChangeInfoText.setBorder(BorderFactory.createEmptyBorder());
        uvPathInfoText.setBorder(BorderFactory.createEmptyBorder());
        layoutPathInfoText.setBorder(BorderFactory.createEmptyBorder());
        
        JComboBox serverIpSelect = new JComboBox();
        serverIpSelect.setEditable(false);
        serverIpSelect.addActionListener((ActionEvent event) -> {
            if(serverIpSelect.getItemCount() > 0){
                int selectedIndex = serverIpSelect.getSelectedIndex();
                serverIpAddresChangeTextField.setText(ipAddresses.get(selectedIndex).address);
                serverIpNameChange.setText(ipAddresses.get(selectedIndex).name);
                serverIpPortChange.setText(ipAddresses.get(selectedIndex).port);
            }
        });
        
        JButton reloadUltragridButton = new JButton(languageBundle.getString("RESCAN_ULTRAGRID"));
        JButton addNewServerButton = new JButton(languageBundle.getString("ADD"));
        JButton saveChangesInServerButton = new JButton(languageBundle.getString("USE"));
        JButton deleteCurrentServerButton = new JButton(languageBundle.getString("DELETE"));
        reloadUltragridButton.setFont(fontButtons);
        addNewServerButton.setFont(fontButtons);
        saveChangesInServerButton.setFont(fontButtons);
        deleteCurrentServerButton.setFont(fontButtons);
        reloadUltragridButton.addActionListener((ActionEvent event) -> {
            ultragridOK(uvPathString, verificationText);
            try {
                videoDevices = loadVideoDevicesAndSettings(uvPathString);
                audioIn = read_audio_devices_in_or_out(uvPathString, true);
                audioOut = read_audio_devices_in_or_out(uvPathString, false);
            } catch (IOException ex) {
                videoDevices = new ArrayList<>();
                audioIn =  new ArrayList<>();
                audioOut = new ArrayList<>();
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            setAudioJComboBox(audioInComboBox, audioIn);
            setAudioJComboBox(audioOutComboBox, audioOut);
            setAllJComboBoxesVideosetting(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, cameraSettingText, videoDevices);
            setAllJComboBoxesVideosetting(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, presentationSettingText, videoDevices);
        });
        addNewServerButton.addActionListener((ActionEvent event) -> {
            IPServerSaved newServer = new IPServerSaved();
            newServer.address = "0.0.0.0";
            newServer.name = "new server";
            newServer.port = "80";
            ipAddresses.add(newServer);
            setServerIpsComboBox(ipAddresses, serverIpSelect);
            serverIpSelect.setSelectedIndex(ipAddresses.size() - 1);
        });
        saveChangesInServerButton.addActionListener((ActionEvent event) -> {
            if(serverIpSelect.getItemCount() > 0){
                int selectedIndex = serverIpSelect.getSelectedIndex();
                ipAddresses.get(selectedIndex).address = serverIpAddresChangeTextField.getText();
                ipAddresses.get(selectedIndex).name = serverIpNameChange.getText();
                ipAddresses.get(selectedIndex).port = serverIpPortChange.getText();
                setServerIpsComboBox(ipAddresses, serverIpSelect);
                serverIpSelect.setSelectedIndex(selectedIndex);
            }
        });
        deleteCurrentServerButton.addActionListener((ActionEvent event) -> {
            if(serverIpSelect.getItemCount() > 0){
                int selectedIndex = serverIpSelect.getSelectedIndex();
                ipAddresses.remove(selectedIndex);
                serverIpAddresChangeTextField.setText("");
                serverIpNameChange.setText("");
                serverIpPortChange.setText("");
                setServerIpsComboBox(ipAddresses, serverIpSelect);
            }
        });
        
        //path fields
        //creating and setting
        if(configuration.has("ultragrid path")){
            try {
                uvPathString = configuration.getString("ultragrid path");
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
        if(configuration.has("layout path")){
            try {
                layoutPathString = configuration.getString("layout path");
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        JTextField uvSystemPath = new JTextField(uvPathString);
        JTextField layoutSystemPath = new JTextField(layoutPathString);
        uvSystemPath.setColumns(20);
        layoutSystemPath.setColumns(20);
        uvSystemPath.setBorder(BorderFactory.createEmptyBorder());
        layoutSystemPath.setBorder(BorderFactory.createEmptyBorder());
        uvSystemPath.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                propagateText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                propagateText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                propagateText();
            }
            
            public void propagateText(){
                uvPathString = uvSystemPath.getText();
            }
        });
        
        layoutSystemPath.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                propagateText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                propagateText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                propagateText();
            }
            
            public void propagateText(){
                layoutPathString = layoutSystemPath.getText();
            }
        });
        JFileChooser uvFileChooser = new JFileChooser();
        JFileChooser layoutFileChooser = new JFileChooser();
        layoutFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton setUvSystemPathButton = new JButton("...");
        JButton setLayoutSystemPathButton = new JButton("...");
        setUvSystemPathButton.setFont(fontButtons);
        setLayoutSystemPathButton.setFont(fontButtons);
        //setting action path choosing
        setUvSystemPathButton.addActionListener((ActionEvent event) -> {
            int returnVal = uvFileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                uvSystemPath.setText(uvFileChooser.getSelectedFile().getPath());
            }
        });
        
        setLayoutSystemPathButton.addActionListener((ActionEvent event) -> {
            int returnVal = layoutFileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                layoutSystemPath.setText(layoutFileChooser.getSelectedFile().getPath());
            }
        });
        
        
        if(languageBundle.containsKey("MISC_TOOL_TIP_MY_IP")){
            myIpSetTextField.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_MY_IP"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_IP_ADDRESS")){
            serverIpAddresChangeTextFieldInfoText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_IP_ADDRESS"));
            serverIpAddresChangeTextField.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_IP_ADDRESS"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_NAME")){
            serverIpNameChangeInfoText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_NAME"));
            serverIpNameChange.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_NAME"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_PORT")){
            serverIpPortChangeInfoText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_PORT"));
            serverIpPortChange.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_PORT"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_ADD_NEW")){
            addNewServerButton.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_ADD_NEW"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_USE")){
            saveChangesInServerButton.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_USE"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_SERVER_DELETE")){
            deleteCurrentServerButton.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_SERVER_DELETE"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_UV_PATH")){
            uvPathInfoText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_UV_PATH"));
            uvSystemPath.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_UV_PATH"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_LAYOUT_PATH")){
            layoutPathInfoText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_LAYOUT_PATH"));
            layoutSystemPath.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_LAYOUT_PATH"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_RELOAD_UV")){
            reloadUltragridButton.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_RELOAD_UV"));
        }
        if(languageBundle.containsKey("MISC_TOOL_TIP_UV_STATUS_TEXT")){
            verificationText.setToolTipText(languageBundle.getString("MISC_TOOL_TIP_UV_STATUS_TEXT"));
        }
        
        myIpAddressPanel.setLayout(new GridBagLayout());
        GridBagConstraints myIpAddressConstraints = new GridBagConstraints();
        //myIpAddressConstraints.fill = GridBagConstraints.HORIZONTAL;
        myIpAddressConstraints.insets = new Insets(5,5,5,5);
        myIpAddressConstraints.weightx = 0.5;
        myIpAddressConstraints.gridheight = 1;
        myIpAddressConstraints.gridwidth = 1;
        myIpAddressConstraints.gridx = 0;
        myIpAddressConstraints.gridy = 0;
        myIpAddressPanel.add(myIpSetTextField, myIpAddressConstraints);
        
        serverIpSettingPanel.setLayout(new GridBagLayout());
        GridBagConstraints serverIpSettingPanelConstraints = new GridBagConstraints();
        serverIpSettingPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        serverIpSettingPanelConstraints.insets = new Insets(5,5,5,5);
        serverIpSettingPanelConstraints.weightx = 0.5;
        serverIpSettingPanelConstraints.gridheight = 1;
        serverIpSettingPanelConstraints.gridwidth = 1;
        serverIpSettingPanelConstraints.gridx = 1;
        serverIpSettingPanelConstraints.gridy = 0;
        serverIpSettingPanel.add(serverIpAddresChangeTextFieldInfoText, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 2;
        serverIpSettingPanelConstraints.gridy = 0;
        serverIpSettingPanel.add(serverIpAddresChangeTextField, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 3;
        serverIpSettingPanelConstraints.gridy = 0;
        serverIpSettingPanel.add(addNewServerButton, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 0;
        serverIpSettingPanelConstraints.gridy = 1;
        serverIpSettingPanel.add(serverIpSelect, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 1;
        serverIpSettingPanelConstraints.gridy = 1;
        serverIpSettingPanel.add(serverIpNameChangeInfoText, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 2;
        serverIpSettingPanelConstraints.gridy = 1;
        serverIpSettingPanel.add(serverIpNameChange, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 3;
        serverIpSettingPanelConstraints.gridy = 1;
        serverIpSettingPanel.add(saveChangesInServerButton, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 1;
        serverIpSettingPanelConstraints.gridy = 2;
        serverIpSettingPanel.add(serverIpPortChangeInfoText, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 2;
        serverIpSettingPanelConstraints.gridy = 2;
        serverIpSettingPanel.add(serverIpPortChange, serverIpSettingPanelConstraints);
        serverIpSettingPanelConstraints.gridx = 3;
        serverIpSettingPanelConstraints.gridy = 2;
        serverIpSettingPanel.add(deleteCurrentServerButton, serverIpSettingPanelConstraints);
        
        addressPanel.setLayout(new GridBagLayout());
        GridBagConstraints addressPanelConstraints = new GridBagConstraints();
        addressPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        addressPanelConstraints.insets = new Insets(5,5,5,5);
        addressPanelConstraints.weightx = 0.5;
        addressPanelConstraints.gridheight = 1;
        addressPanelConstraints.gridwidth = 1;
        addressPanelConstraints.gridx = 0;
        addressPanelConstraints.gridy = 0;
        addressPanel.add(uvPathInfoText, addressPanelConstraints);
        addressPanelConstraints.gridx = 1;
        addressPanelConstraints.gridy = 0;
        addressPanel.add(uvSystemPath, addressPanelConstraints);
        addressPanelConstraints.gridx = 2;
        addressPanelConstraints.gridy = 0;
        addressPanel.add(setUvSystemPathButton, addressPanelConstraints);        
        addressPanelConstraints.gridx = 0;
        addressPanelConstraints.gridy = 2;
        addressPanel.add(layoutPathInfoText, addressPanelConstraints);
        addressPanelConstraints.gridx = 1;
        addressPanelConstraints.gridy = 2;
        addressPanel.add(layoutSystemPath, addressPanelConstraints);
        addressPanelConstraints.gridx = 2;
        addressPanelConstraints.gridy = 2;
        addressPanel.add(setLayoutSystemPathButton, addressPanelConstraints);
        
        miscsPanel.setLayout(new GridBagLayout());
        GridBagConstraints miscsPanelConstraints = new GridBagConstraints();
        miscsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        miscsPanelConstraints.gridheight = 1;
        miscsPanelConstraints.gridwidth = 3;
        miscsPanelConstraints.gridx = 0;
        miscsPanelConstraints.gridy = 0;
        miscsPanel.add(myIpAddressPanel, miscsPanelConstraints);
        miscsPanelConstraints.gridx = 0;
        miscsPanelConstraints.gridy = 1;
        miscsPanel.add(serverIpSettingPanel, miscsPanelConstraints);
        miscsPanelConstraints.gridx = 0;
        miscsPanelConstraints.gridy = 2;
        miscsPanel.add(addressPanel, miscsPanelConstraints);
        miscsPanelConstraints.fill = GridBagConstraints.NONE;
        miscsPanelConstraints.anchor = GridBagConstraints.CENTER;
        miscsPanelConstraints.weightx = 0.5;
        miscsPanelConstraints.gridx = 0;
        miscsPanelConstraints.gridy = 3;
        miscsPanelConstraints.gridheight = 1;
        miscsPanelConstraints.gridwidth = 2;
        miscsPanel.add(verificationText, miscsPanelConstraints);
        miscsPanelConstraints.anchor = GridBagConstraints.LINE_END;
        miscsPanelConstraints.gridx = 2;
        miscsPanelConstraints.gridy = 3;
        miscsPanelConstraints.gridheight = 1;
        miscsPanelConstraints.gridwidth = 1;
        miscsPanel.add(reloadUltragridButton, miscsPanelConstraints);
        
        ipAddresses = loadIpAddreses();
        setServerIpsComboBox(ipAddresses, serverIpSelect);
        String myIpLoaded = "";
        try {
            myIpLoaded = configuration.getString("this ip");
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        myIpSetTextField.setText(myIpLoaded);
    }
    
    private List<VideoDevice> loadVideoDevicesAndSettings(String uvPath) throws IOException{
        if(!correctUv){
            return new ArrayList<>();
        }
        List<VideoDevice> ret = new ArrayList<>();
        if(correctUv){
            String osName = System.getProperty("os.name");
            String uvVideoSetting;
            if(osName.toUpperCase().contains("WINDOWS")){
                uvVideoSetting = "dshow";
                ret = getVideoDevicesAndSettingsWindows(uvPath, uvVideoSetting);
            }else if(osName.toUpperCase().contains("LINUX")){
                uvVideoSetting = "v4l2";
                ret = getVideoDevicesAndSettingsLinux(uvPath, uvVideoSetting);
            }else if(osName.toUpperCase().contains("MAC")){
                uvVideoSetting = "avfoundation";
                ret = getVideoDevicesAndSettingsMac(uvPath, uvVideoSetting);
            }else{      //probably should log incorrect os system
                return null;
            }
        }
        return ret;
    }
    
    
    List<VideoDevice> getVideoDevicesAndSettingsLinux(String uvAddress, String uvVideoSetting) throws IOException{
        Process uvProcess = new ProcessBuilder(uvAddress, "-t", uvVideoSetting + ":help").start();
        InputStream is = uvProcess.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        
        boolean loadCamera = false;
        List<VideoDevice> videoInputs = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if(loadCamera){ //lines inside the device setting 
                if(line.length() == 0){     //end of divice setting
                    loadCamera = false;
                }else{
                    VideoDevice lastVideoDevice = videoInputs.get(videoInputs.size()-1);   //get last video device
                    if(line.contains("Pixel format")){  //line with new pixel format
                        Pattern pixelFormatPattern = Pattern.compile("Pixel format ((.*?)[(].*[)])");
                        Matcher pixelFormatMatcher = pixelFormatPattern.matcher(line);
                        VideoPixelFormat pf = new VideoPixelFormat();
                        if(pixelFormatMatcher.find()){
                            pf.name = pixelFormatMatcher.group(1);
                            String pixelFormatString = pixelFormatMatcher.group(2);
                            pf.pixelFormat = pixelFormatString.replaceAll("\\s+","");
                        }
                        
                        pf.name = line.substring(line.indexOf("Pixel format")+12, line.indexOf('.'));
                        lastVideoDevice.vpf.add(pf);
                    }else{                              //line with frame size
                        VideoPixelFormat lastPixelFormat = lastVideoDevice.vpf.get(lastVideoDevice.vpf.size()-1);
                        VideoFrameSize fs = new VideoFrameSize();
                        Pattern resolutionPattern = Pattern.compile("[\\d]+x[\\d]+");
                        Matcher resolutionMatcher = resolutionPattern.matcher(line);
                        if(resolutionMatcher.find()){
                            fs.widthXheight = resolutionMatcher.group();
                        }
                        Pattern fpsPattern = Pattern.compile("(([\\d]+)/([\\d]+))");
                        Matcher  fpsMatcher = fpsPattern.matcher(line);
                        fs.fps = new ArrayList<>();
                        while(fpsMatcher.find()){
                            VideoFPS vfps = new VideoFPS();
                            vfps.fps = fpsMatcher.group(3);
                            vfps.setting = "v4l2:dev=" + lastVideoDevice.device + ":fmt=" + lastPixelFormat.pixelFormat + ":size=" + fs.widthXheight
                                                    + ":tpf=" + fpsMatcher.group(1);
                            fs.fps.add(vfps);
                        }
                        lastPixelFormat.vfs.add(fs);
                    }
                }
            }else{  // out of device setting
                if(line.contains("Device")){
                    loadCamera = true;
                    VideoDevice vd = new VideoDevice();
                    Pattern devicePattern = Pattern.compile("Device ((/.*?)/(.*?)) ([(].*[)])");
                    Matcher deviceMatcher = devicePattern.matcher(line);
                    if(deviceMatcher.find()){
                        vd.name = deviceMatcher.group(3) + " " + deviceMatcher.group(4);
                        vd.device = deviceMatcher.group(1);
                    }
                    videoInputs.add(vd);
                }
            }
        }
        
        return videoInputs;
    }
    
    List<VideoDevice> getVideoDevicesAndSettingsWindows(String uvAddress, String uvVideoSetting) throws IOException{
        
        Process uvProcess = new ProcessBuilder(uvAddress, "-t", uvVideoSetting + ":help").start();
        InputStream is = uvProcess.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        
        List<VideoDevice> videoInputs = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            Pattern devicePattern = Pattern.compile("^(Device (\\d+):.)");
            Matcher deviceMatcher = devicePattern.matcher(line);
            if(deviceMatcher.find()){
                VideoDevice vd = new VideoDevice();
                vd.name = deviceMatcher.group(1);               
                vd.device = deviceMatcher.group(2);
                vd.vpf = new ArrayList<>();
                videoInputs.add(vd);
            }
            
            Pattern modePattern = Pattern.compile("Mode\\s+(\\d+):\\s(.*?)\\s*(\\d*x\\d*)\\s*@([0-9\\.]*)");
            Matcher modeMatcher = modePattern.matcher(line);
            while(modeMatcher.find()){
                VideoDevice vd = videoInputs.get(videoInputs.size() - 1);
                int pixelFormatPosition = 0;
                int widthXheightPosition = 0;
                boolean found_item = false;             //maybe not most elegant
                for(int i=0;i<vd.vpf.size();i++){
                    if(vd.vpf.get(i).pixelFormat.equals(modeMatcher.group(2))){
                        pixelFormatPosition = i;
                        found_item = true;
                    }
                }
                if(!found_item){
                    VideoPixelFormat vpf = new VideoPixelFormat();
                    vpf.pixelFormat = modeMatcher.group(2);
                    vpf.name = modeMatcher.group(2);
                    vpf.vfs = new ArrayList<>();
                    vd.vpf.add(vpf);
                    pixelFormatPosition = vd.vpf.size() - 1;
                }
                found_item = false;
                for(int i=0;i<vd.vpf.get(pixelFormatPosition).vfs.size();i++){
                    if(vd.vpf.get(pixelFormatPosition).vfs.get(i).widthXheight.equals(modeMatcher.group(3))){
                        widthXheightPosition = i;
                        found_item = true;
                    }
                }
                if(!found_item){
                    VideoFrameSize vfs = new VideoFrameSize();
                    vfs.widthXheight = modeMatcher.group(3);
                    vfs.fps = new ArrayList<>();
                    vd.vpf.get(pixelFormatPosition).vfs.add(vfs);
                    widthXheightPosition = vd.vpf.get(pixelFormatPosition).vfs.size() - 1;
                }
                VideoFPS vfps = new VideoFPS();
                vfps.fps = modeMatcher.group(4);
                vfps.setting = "dshow" + ":" + vd.device + ":" + modeMatcher.group(1);
                vd.vpf.get(pixelFormatPosition).vfs.get(widthXheightPosition).fps.add(vfps);
            }
        }
        return videoInputs;
    }
    
    List<VideoDevice> getVideoDevicesAndSettingsMac(String uvAddress, String uvVideoSetting) throws IOException{
        Process uvProcess = new ProcessBuilder(uvAddress, "-t", uvVideoSetting + ":help").start();
        InputStream is = uvProcess.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        
        List<VideoDevice> videoInputs = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            Pattern devicePattern = Pattern.compile("^[\\*]?(\\d*):\\s*(.*)");
            Matcher deviceMatcher = devicePattern.matcher(line);
            if(deviceMatcher.find()){
                VideoDevice vd = new VideoDevice();
                vd.name = deviceMatcher.group(2);
                vd.device = deviceMatcher.group(1);
                vd.vpf = new ArrayList<>();
                videoInputs.add(vd);
            }
            
            Pattern modePattern = Pattern.compile("(\\d+): (.*?) (\\d+x\\d+) \\(max frame rate (\\d*) FPS\\)");
            Matcher modeMatcher = modePattern.matcher(line);
            while(modeMatcher.find()){
                if(videoInputs.isEmpty()){    //something strange happend, stop process
                    return videoInputs;
                }
                VideoPixelFormat vpf = new VideoPixelFormat();
                VideoFrameSize vfs = new VideoFrameSize();;
                int max_fps = 0;
                String partialSetting;
                List<VideoFPS> listVfps = new ArrayList<>();
                VideoDevice vd = videoInputs.get(videoInputs.size() - 1);
                partialSetting = uvVideoSetting + ":device=" + vd.device + ":mode=" + modeMatcher.group(1) ;
                vpf.pixelFormat = modeMatcher.group(2);
                vpf.name = modeMatcher.group(2);
                vfs.widthXheight = modeMatcher.group(3);
                max_fps = Integer.parseInt(modeMatcher.group(4));
                for(int i = max_fps; i > 0; i--) {
                    VideoFPS vfps = new VideoFPS();
                    vfps.setting = partialSetting + ":framerate=" + String.valueOf(i);
                    vfps.fps = String.valueOf(i);
                    listVfps.add(vfps);
                }
                boolean found_item = false;             //maybe not most elegant
                for(int i=0;i<vd.vpf.size();i++){
                    if(vd.vpf.get(i).pixelFormat.equals(vpf.pixelFormat)){
                        vpf = vd.vpf.get(i);
                        found_item = true;
                    }
                }
                if(!found_item){
                    vpf.vfs = new ArrayList<>();
                    vd.vpf.add(vpf);
                }
                found_item = false;
                for(int i=0;i<vpf.vfs.size();i++){
                    if(vpf.vfs.get(i).widthXheight.equals(vfs.widthXheight)){
                        vfs = vpf.vfs.get(i);
                        found_item = true;
                    }
                }
                if(!found_item){
                    vfs.fps = new ArrayList<>();
                    vpf.vfs.add(vfs);
                }
                vfs.fps = listVfps;
            }
        }
                        
        
        return videoInputs;
    }
    
    String getVideoSettings(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices){        
        String ret = "";
        if((devicesBox.getItemCount() > 0) && (formatBox.getItemCount() > 0) && (frameSizeBox.getItemCount() > 0) && (fpsBox.getItemCount() > 0)){
            String camera = devicesBox.getSelectedItem().toString();
            String format = formatBox.getSelectedItem().toString();
            String resolution = frameSizeBox.getSelectedItem().toString();
            String fps = fpsBox.getSelectedItem().toString();
            List<VideoPixelFormat> videoPixelFormats = null;
            List<VideoFrameSize> videoFrameSizes = null;
            List<VideoFPS> videoFPS = null;
            for (int i=0; i<videoDevices.size();i++){
                if(videoDevices.get(i).name.compareTo(camera) == 0){
                   videoPixelFormats = videoDevices.get(i).vpf;
                }
            }
            if(videoPixelFormats != null){
                for (int i=0; i<videoPixelFormats.size();i++){
                    if(videoPixelFormats.get(i).name.compareTo(format) == 0){
                       videoFrameSizes = videoPixelFormats.get(i).vfs;
                    }
                }
            }
            if(videoFrameSizes != null){
                for (int i=0; i<videoFrameSizes.size();i++){
                    if(videoFrameSizes.get(i).widthXheight.compareTo(resolution) == 0){
                       videoFPS = videoFrameSizes.get(i).fps;
                    }
                }
            }
            if(videoFPS != null){
                for (int i=0; i<videoFPS.size();i++){
                    if(videoFPS.get(i).fps.compareTo(fps) == 0){
                       ret = videoFPS.get(i).setting;
                    }
                }
            }
        }
        return ret;
    }
    
    private void setJComboBoxDevices(JComboBox devicesBox, List<VideoDevice> videoDevices){
        devicesBox.removeAllItems();
        if(videoDevices != null){
            for(int i=0;i<videoDevices.size();i++){
                devicesBox.addItem(videoDevices.get(i).name);
            }
        }
    }
    
    private void setJComboBoxFormat(JComboBox formatBox, JComboBox deviceBox, List<VideoDevice> videoDevices){
        formatBox.removeAllItems();
        String device;
        if((deviceBox == null) || (deviceBox.getItemCount() == 0)){
            return;
        }else{
            device = deviceBox.getSelectedItem().toString();
        }
        if(videoDevices != null){
            for(int i=0;i<videoDevices.size();i++){
                if(device.compareTo(videoDevices.get(i).name) == 0){
                    for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                        formatBox.addItem(videoDevices.get(i).vpf.get(j).name);
                    }
                    return;
                }
            }
        }
    }
    
    private void setJComboBoxFrameSize(JComboBox frameSizeBox, JComboBox deviceBox, JComboBox formatBox, List<VideoDevice> videoDevices){
        frameSizeBox.removeAllItems();
        String device;
        String format;
        if((deviceBox == null) || (deviceBox.getItemCount() == 0) || (formatBox == null) || (formatBox.getItemCount() == 0)){
            return;
        }else{
            device = deviceBox.getSelectedItem().toString();
            format = formatBox.getSelectedItem().toString();
        }
        if(videoDevices != null){
            for(int i=0;i<videoDevices.size();i++){
                if(device.compareTo(videoDevices.get(i).name) == 0){
                    for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                        if(format.compareTo(videoDevices.get(i).vpf.get(j).name) == 0){
                            for(int k=0;k<videoDevices.get(i).vpf.get(j).vfs.size();k++){
                                frameSizeBox.addItem(videoDevices.get(i).vpf.get(j).vfs.get(k).widthXheight);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
    
    private void setJComboBoxFPS(JComboBox fpsBox, JComboBox deviceBox, JComboBox formatBox, JComboBox widthXheightBox, List<VideoDevice> videoDevices){
        fpsBox.removeAllItems();
        String device;
        String format;
        String widthXheight;
        if((deviceBox == null) || (deviceBox.getItemCount() == 0) || (formatBox == null) || (formatBox.getItemCount() == 0) ||
               (widthXheightBox == null) || (widthXheightBox.getItemCount() == 0)){
            return;
        }else{
            device = deviceBox.getSelectedItem().toString();
            format = formatBox.getSelectedItem().toString();
            widthXheight = widthXheightBox.getSelectedItem().toString();
        }
        if(videoDevices != null){
            for(int i=0;i<videoDevices.size();i++){
                if(device.compareTo(videoDevices.get(i).name) == 0){
                    for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                        if(format.compareTo(videoDevices.get(i).vpf.get(j).name) == 0){
                            for(int k=0;k<videoDevices.get(i).vpf.get(j).vfs.size();k++){
                                if(widthXheight.compareTo(videoDevices.get(i).vpf.get(j).vfs.get(k).widthXheight) == 0){
                                    for(int l=0;l<videoDevices.get(i).vpf.get(j).vfs.get(k).fps.size();l++){
                                        fpsBox.addItem(videoDevices.get(i).vpf.get(j).vfs.get(k).fps.get(l).fps);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    void setAllJComboBoxesVideosetting(JComboBox deviceBox, JComboBox formatBox, JComboBox widthXheightBox, JComboBox fpsBox, JTextField settingVerification, List<VideoDevice> videoDevices){
        setJComboBoxDevices(deviceBox, videoDevices);
        setJComboBoxFormat(formatBox, deviceBox, videoDevices);
        setJComboBoxFrameSize(widthXheightBox, deviceBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, deviceBox, formatBox, widthXheightBox, videoDevices);
        actionSetFPSBox(deviceBox, formatBox, widthXheightBox, fpsBox, settingVerification, videoDevices);
    }
    
    private void actionSetCameraDeviceBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox, JTextField settingVerification,
                                            List<VideoDevice> videoDevices){
        
        setJComboBoxFormat(formatBox, devicesBox, videoDevices);
        setJComboBoxFrameSize(frameSizeBox, devicesBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
        actionSetFPSBox(devicesBox, formatBox, frameSizeBox, fpsBox, settingVerification, videoDevices);
    }
    
    private void actionSetCameraPixelFormatBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox, JTextField settingVerification,
                                            List<VideoDevice> videoDevices){

        setJComboBoxFrameSize(frameSizeBox, devicesBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
        actionSetFPSBox(devicesBox, formatBox, frameSizeBox, fpsBox, settingVerification, videoDevices);
    }
    
    private void actionSetCameraFrameSizeBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox, JTextField settingVerification,
                                            List<VideoDevice> videoDevices){

        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
        actionSetFPSBox(devicesBox, formatBox, frameSizeBox, fpsBox, settingVerification, videoDevices);
    }
    
    private void actionSetFPSBox(JComboBox deviceBox, JComboBox formatBox, JComboBox widthXheightBox, JComboBox fpsBox, JTextField settingVerification,
                                            List<VideoDevice> videoDevices){
        String device;
        String format;
        String widthXheight;
        String fps;
        if((deviceBox == null) || (deviceBox.getItemCount() == 0) || (formatBox == null) || (formatBox.getItemCount() == 0) ||
               (widthXheightBox == null) || (widthXheightBox.getItemCount() == 0) || (fpsBox == null) || (fpsBox.getItemCount() == 0)) {
            return;
        }else{
            device = deviceBox.getSelectedItem().toString();
            format = formatBox.getSelectedItem().toString();
            widthXheight = widthXheightBox.getSelectedItem().toString();
            fps = fpsBox.getSelectedItem().toString();
        }
        if(videoDevices != null){
            for(int i=0;i<videoDevices.size();i++){
                if(device.compareTo(videoDevices.get(i).name) == 0){
                    for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                        if(format.compareTo(videoDevices.get(i).vpf.get(j).name) == 0){
                            for(int k=0;k<videoDevices.get(i).vpf.get(j).vfs.size();k++){
                                if(widthXheight.compareTo(videoDevices.get(i).vpf.get(j).vfs.get(k).widthXheight) == 0){
                                    for(int l=0;l<videoDevices.get(i).vpf.get(j).vfs.get(k).fps.size();l++){
                                        if(videoDevices.get(i).vpf.get(j).vfs.get(k).fps.get(l).fps.equals(fps)){
                                            settingVerification.setText(videoDevices.get(i).vpf.get(j).vfs.get(k).fps.get(l).setting);
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private List<AudioDevice> read_audio_devices_in_or_out(String uvAddress, boolean audio_in) throws IOException{
        if(!correctUv){
            return new ArrayList<>();
        }
        if(!havePortaudio){
            return new ArrayList<>();
        }
        Process uvProcess = new ProcessBuilder(uvAddress, "-s", "portaudio:help").start();
        InputStream is = uvProcess.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        
        List<AudioDevice> audioDevices = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            Pattern devicePattern = Pattern.compile("portaudio(:\\d+) : (.+) \\(output channels: (\\d+); input channels: (\\d+)\\)");
            Matcher deviceMatcher = devicePattern.matcher(line);
            if(deviceMatcher.find()){
                if(audio_in){
                    if(!deviceMatcher.group(4).equals("0")){    //have some input chanels
                        AudioDevice new_device = new AudioDevice();
                        new_device.name = deviceMatcher.group(2);
                        new_device.setting = deviceMatcher.group(1);
                        audioDevices.add(new_device);
                    }
                }else{
                    if(!deviceMatcher.group(3).equals("0")){    //have some input chanels
                        AudioDevice new_device = new AudioDevice();
                        new_device.name = deviceMatcher.group(2);
                        new_device.setting = deviceMatcher.group(1);
                        audioDevices.add(new_device);
                    }
                }
            }
            
        }
        return audioDevices;
    }
    
    void ultragridOK(String uvAddress, JTextArea verificationTextField){
        if(uvProcess != null){
            uvProcess.destroyForcibly();
        }
        
        verificationTextField.setRows(1);
        //initiial tests if it can be ultragrid
        if(uvAddress.isEmpty()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText(languageBundle.getString("EMPTY_PATH"));
            correctUv = false;
            return;
        }
        File uvFile = new File(uvAddress);
        if(!uvFile.exists()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText(languageBundle.getString("INVALID_PATH")+ " " + uvFile.getName() + ".");
            correctUv = false;
            return;
        }
        if(uvFile.isDirectory()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText(languageBundle.getString("FILE")+ " " + uvFile.getName() + " " + languageBundle.getString("IS_DIRECTORY"));
            correctUv = false;
            return;
        }
        if(!uvFile.canExecute()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText(languageBundle.getString("FILE")+ " " + uvFile.getName() + " " + languageBundle.getString("CAN_NOT_BE_EXECUTED"));
            correctUv = false;
            return;
        }
        
        String outMessage = "";
        boolean correctUvOutput = false;
        boolean correctUvreturnValue = false;
        
        try {
            // mac return uv help value 10, -v seems be ok
            uvProcess = new ProcessBuilder(uvAddress, "-v").start();  //may add output check, maby later, or some other checks
            
            InputStream is = uvProcess.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            boolean firsLine = true;
            int linesCheckMaxLimit = 100;
            int i = 0;
            
            while (((line = br.readLine()) != null) && (linesCheckMaxLimit > i)) {
                i++;
                Pattern glPattern = Pattern.compile("OpenGL \\.*+ (no|yes)");
                Matcher glMatcher = glPattern.matcher(line);
                
                
                Pattern sdlPattern = Pattern.compile("SDL \\.*+ (no|yes)");
                Matcher sdlMatcher = sdlPattern.matcher(line);
                
                Pattern portaudioPattern = Pattern.compile("Portaudio \\.*+ (no|yes)");
                Matcher portaudioMatcher = portaudioPattern.matcher(line);
                
                if(firsLine){
                    Pattern ultragridPattern = Pattern.compile("UltraGrid ");
                    Matcher ultragridMatcher = ultragridPattern.matcher(line);
                    correctUvOutput = ultragridMatcher.find();
                    firsLine = false;
                }
                
                if(glMatcher.find()){
                    if(glMatcher.group(1).equals("yes")){
                        outMessage += "gl " + languageBundle.getString("FOUND") + "\n";
                    }else{
                        outMessage += "gl " + languageBundle.getString("NOT_FOUND") + "\n";
                    }
                }
                
                if(sdlMatcher.find()){
                    if(sdlMatcher.group(1).equals("yes")){
                        outMessage += "sdl " + languageBundle.getString("FOUND") + "\n";
                    }else{
                        outMessage += "sdl " + languageBundle.getString("NOT_FOUND") + "\n";
                    }
                }
                
                if(portaudioMatcher.find()){
                    if(portaudioMatcher.group(1).equals("yes")){
                        outMessage += "portaudio " + languageBundle.getString("FOUND") + "\n";
                        havePortaudio = true;
                    }else{
                        outMessage += "portaudio " + languageBundle.getString("NOT_FOUND") + "\n";
                        havePortaudio = false;
                    }
                }
            }
            correctUvreturnValue = (uvProcess.exitValue() == 0);
        } catch (IllegalThreadStateException | IOException ex){
            uvProcess.destroyForcibly();
        }
        uvProcess.destroyForcibly();
        //probably overkill destroing process, but I realy dont want to allow process to survive
        
        if(correctUvreturnValue && correctUvOutput){
            verificationTextField.setForeground(Color.getHSBColor((float)0.39, (float)1, (float)0.8));
            verificationTextField.setRows(4);
            outMessage += "UltraGid ";
            outMessage += languageBundle.getString("FOUND");
            verificationTextField.setText(outMessage);
            correctUv = true;
        }else{
            if(correctUvreturnValue){
                verificationTextField.setForeground(Color.red);
                verificationTextField.setRows(1);
                verificationTextField.setText(languageBundle.getString("ERROR"));
            }else{
                verificationTextField.setForeground(Color.red);
                verificationTextField.setRows(1);
                verificationTextField.setText("UltraGid " + languageBundle.getString("CAN_NOT_BE_EXECUTED"));
            }
        }        
    }
    
    private void startUltragrid(String uvAddress, String uvReciveSetting, String uvSendSetting) throws IOException{
        if(uvProcess != null){
            uvProcess.destroyForcibly();
        }
        if(correctUv){
            ProcessBuilder pb = new ProcessBuilder(uvAddress, "-d", uvReciveSetting, "-t", uvSendSetting);
            Process tmpProcess = pb.start();
            uvProcess = tmpProcess;
                    
        }
    }

    private void setJComboBoxDisplay(JComboBox displayBox, JComboBox displaySettingBox) {
        displayBox.removeAllItems();
        displayBox.addItem("gl");
        displayBox.addItem("sdl");
        displaySettingBox.removeAllItems();
        displaySettingBox.addItem("none");
        displaySettingBox.addActionListener((ActionEvent event) -> {
            setCorrectDisplaySetting(displayBox, displaySettingBox);
        });
        displayBox.addActionListener((ActionEvent event) -> {
            boolean sdlSelected = displayBox.getSelectedItem().equals("sdl");
            displaySettingBox.removeAllItems();
            displaySettingBox.addItem("none");
            setCorrectDisplaySetting(displayBox, displaySettingBox);
            if(sdlSelected){
                displaySettingBox.addItem("nodecorate");
            }
        });
        if(configuration.has("consumer settings")){
            try {
                String displaySettingLine = configuration.getString("consumer settings");
                String[] partDisplaySettings = displaySettingLine.split(":");
                if(partDisplaySettings[0].equals("gl")){
                    displayBox.setSelectedItem("gl");
                }else{
                    displayBox.setSelectedItem("sdl");
                    if(partDisplaySettings.length > 1){
                        if(partDisplaySettings[1].equals("nodecorate")){
                            displaySettingBox.setSelectedItem("nodecorate");
                        }else{
                            displaySettingBox.setSelectedItem("none");
                        }
                    }
                }
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    private void setCorrectDisplaySetting(JComboBox displayBox, JComboBox displaySettingBox) {
        if(displayBox == null){
            return;
        }
        if(displaySettingBox == null){
            return;
        }
        if(displayBox.getItemCount() == 0){
            return;
        }
        if(displaySettingBox.getItemCount() == 0){
            return;
        }
        boolean sdlSelected = displayBox.getSelectedItem().equals("sdl");
        boolean nodecorateSelected = displaySettingBox.getSelectedItem().equals("nodecorate");
        if(sdlSelected){
            if(nodecorateSelected){
                displaySetting = "sdl:nodecorate";
            }else{
                displaySetting = "sdl";
            }
        }else{
            displaySetting = "gl";
        }
    }
    
    private void setAudioJComboBox(JComboBox audioBox, List<AudioDevice> audioDevicis){
        audioBox.removeAllItems();
        for(int i=0;i<audioDevicis.size();i++){
            audioBox.addItem(audioDevicis.get(i).name);
        }
    }
    
    private void SetVideoSettingFromConfig(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices, String videoSetting){
        for(int i=0;i<videoDevices.size();i++){
            for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                for(int k=0;k<videoDevices.get(i).vpf.get(j).vfs.size();k++){
                    for(int l=0;l<videoDevices.get(i).vpf.get(j).vfs.get(k).fps.size();l++){
                        if(videoDevices.get(i).vpf.get(j).vfs.get(k).fps.get(l).setting.equals(videoSetting)){
                            devicesBox.setSelectedIndex(i);
                            formatBox.setSelectedIndex(j);
                            frameSizeBox.setSelectedIndex(k);
                            fpsBox.setSelectedIndex(l);
                            return;
                        }
                    }
                }
            }
        }

    }
    
    JSONObject readJsonFile(File jsonFile){
        try {
            String entireFileText = new Scanner(jsonFile).useDelimiter("\\A").next();
            return new JSONObject(entireFileText);
        } catch (JSONException | FileNotFoundException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void discardAction(){
        dispose();
    }

    private void saveSettingAction() {
        JSONObject newClinetConfiguration = new JSONObject();
        JSONObject raiseHandColorJson = new JSONObject();
        JSONObject talkingColorJson = new JSONObject();
        JSONArray serverIps = new JSONArray();
        
        String producerSetting = getVideoSettings(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
        String presentationSetting = getVideoSettings(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        String audioInSetting = getAudioSetting(audioInComboBox, audioIn);
        String audioOutSetting = getAudioSetting(audioOutComboBox, audioOut);
        
        int resizeValue;
        try{
            resizeValue = Integer.valueOf(setResizeAmount.getText());
        }catch(NumberFormatException e){
            resizeValue = 0;
        }
        
        try {
            for(int i=0;i<ipAddresses.size();i++){
                JSONObject newIp = new JSONObject();
                newIp.put("ip", ipAddresses.get(i).address);
                newIp.put("name", ipAddresses.get(i).name);
                newIp.put("port", ipAddresses.get(i).port);
                serverIps.put(newIp);
            }
            
            raiseHandColorJson.put("red", raiseHandcolorChooser.getColor().getRed());
            raiseHandColorJson.put("blue", raiseHandcolorChooser.getColor().getBlue());
            raiseHandColorJson.put("green", raiseHandcolorChooser.getColor().getGreen());
            
            talkingColorJson.put("red", talkingColorChooser.getColor().getRed());
            talkingColorJson.put("blue", talkingColorChooser.getColor().getBlue());
            talkingColorJson.put("green", talkingColorChooser.getColor().getGreen());
            
            newClinetConfiguration.put("this ip", myIpSetTextField.getText());
            newClinetConfiguration.put("ultragrid path", uvPathString);
            newClinetConfiguration.put("layout path", layoutPathString);
            newClinetConfiguration.put("producer settings", producerSetting);
            newClinetConfiguration.put("consumer settings", displaySetting);
            newClinetConfiguration.put("audio consumer", audioInSetting);
            newClinetConfiguration.put("audio producer", audioOutSetting);
            if(presentationUsed){
                newClinetConfiguration.put("presentation producer", presentationSetting);
                newClinetConfiguration.put("presentation", presentationUsed);
            }else{
                newClinetConfiguration.put("presentation", presentationUsed);
            }
            newClinetConfiguration.put("language", getLanguage());
            newClinetConfiguration.put("raise hand color", raiseHandColorJson);
            newClinetConfiguration.put("talking color", talkingColorJson);
            newClinetConfiguration.put("server ips", serverIps);
            newClinetConfiguration.put("talking resizing", resizeValue);
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(configurationFile);
            fileWriter.write(newClinetConfiguration.toString());
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        imt.loadClientConfigurationFromFile();
        imt.initServerChooseWindow();
        imt.initSettingRoomWindow();
        imt.initErrorWindow();
        imt.initIpSettingWindow();
        imt.openServerChooseWindow();
        dispose();
    }
    
    private List<IPServerSaved> loadIpAddreses(){
        List<IPServerSaved> ret = new ArrayList<>();
        JSONArray ipAddreses;
        try {
            ipAddreses = configuration.getJSONArray("server ips");
            for(int i=0;i<ipAddreses.length();i++){
                IPServerSaved ipAddress = new IPServerSaved();
                JSONObject loadedIP = ipAddreses.getJSONObject(i);
                ipAddress.address = loadedIP.getString("ip");
                ipAddress.name = loadedIP.getString("name");
                ipAddress.port = loadedIP.getString("port");
                ret.add(ipAddress);
            }
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    private void setServerIpsComboBox(List<IPServerSaved> ipAddresses, JComboBox serverIpSelect) {
        serverIpSelect.removeAllItems();
        for(int i=0;i<ipAddresses.size();i++){
            serverIpSelect.addItem(ipAddresses.get(i).address);
        }
    }
    
    private String getAudioSetting(JComboBox audioBox, List<AudioDevice> audioDevicis){
        if(audioBox == null || audioBox.getItemCount() == 0){
            return "";
        }
        int selectedIndex = audioBox.getSelectedIndex();
        return audioDevicis.get(selectedIndex).setting;
    }

    private void fillLanguageComboBox(JComboBox languageCombobox, String setLanguage) {
        languageCombobox.removeAllItems();
        languageCombobox.addItem("Slovenský");
        languageCombobox.addItem("Český");
        languageCombobox.addItem("English");
        if(!setLanguage.isEmpty()){
            languageCombobox.setSelectedItem(setLanguage);
        }
    }
    
    private String getLanguage(){
        if(languageCombobox.getItemCount() > 0){
            return languageCombobox.getSelectedItem().toString();
        }else{
            return "";
        }
    }
}


class IPServerSaved{
    public String name;
    public String address;
    public String port;
}

class VideoDevice{
    public String name;
    public String device;
    public List<VideoPixelFormat> vpf = new ArrayList<>();
}

class VideoPixelFormat{
    public String name;
    public String pixelFormat;
    public List<VideoFrameSize> vfs = new ArrayList<>();
}

class VideoFrameSize{
    public String widthXheight;
    public List<VideoFPS> fps;
}

class VideoFPS{
    String fps;
    String setting;
}

class AudioDevice{
    String name;
    String setting;
}