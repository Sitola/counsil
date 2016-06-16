/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author xminarik
 */
public class OptionsMainMenuWindow extends JFrame{
    JPanel mainPanel, visualitationPanel, videoAudioPanel, miscsPanel;
    JTabbedPane mainTabPanel;
    Font fontButtons;
    Font fontBoxes;
    List<VideoDevice> videoDevices;
    List<AudioDevice> audioIn;
    List<AudioDevice> audioOut;
    boolean correctUv;
    Process uvProcess;
    JSONObject configuration;
    // constructor
    OptionsMainMenuWindow(Font fontButtons, Font fontBoxes, File configurationFile)
    {
        super( "CoUnSil " + "options" );
        this.fontButtons = fontButtons;
        this.fontBoxes = fontBoxes;
        videoDevices = null;
        audioIn = null;
        audioOut = null;
        correctUv = false;
        
        configuration = readJsonFile(configurationFile);
        //setSize( 150, 100 );
        //setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        uvProcess = null;
        mainPanel = new JPanel();
        //mainPanel.setLayout(new GridBagLayout());
        setLayout(new GridBagLayout());
        visualitationPanel = new JPanel();
        visualitationPanel.setLayout(new GridLayout(8, 1));
        videoAudioPanel = new JPanel();
        videoAudioPanel.setLayout(new GridBagLayout());
        miscsPanel = new JPanel();
        mainTabPanel = new JTabbedPane();
        mainTabPanel.addTab("visualitation", visualitationPanel);
        mainTabPanel.addTab("video & audio", videoAudioPanel);
        mainTabPanel.addTab("misc", miscsPanel);
        
        addWindowListener(new WindowAdapter() {//action on close button (x)
            public void windowClosing(WindowEvent e) {
                if(uvProcess != null){
                    uvProcess.destroy();
                }
            }
        });
        
        JButton saveButton = new JButton("save");
        saveButton.setFont(fontButtons);
        saveButton.addActionListener((ActionEvent event) -> {
            if(uvProcess != null){
                uvProcess.destroy();
            }
        });
        JButton discardButton = new JButton("discard");
        discardButton.setFont(fontButtons);
        discardButton.addActionListener((ActionEvent event) -> {
            if(uvProcess != null){
                uvProcess.destroy();
            }
        });
        setVideoAudioPanel();
        setMiscsPanel();
        
        
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
        //zvetsovanie pri vyvolani checkBox
        //farba pri vyvolani ?? textove pole, vyber rgb, byber z predvolenych
        //farba pri hlaseni ?? textove pole, vyber rgb, byber z predvolenych
        
    }
    
    private void setVideoAudioPanel(){
        
        
        JPanel mainCameraPanel = new JPanel();
        mainCameraPanel.setBorder(BorderFactory.createTitledBorder("kamera"));
        JPanel presetationPanel = new JPanel();
        presetationPanel.setBorder(BorderFactory.createTitledBorder("prezentacia"));
        JPanel displayPanel = new JPanel();
        displayPanel.setBorder(BorderFactory.createTitledBorder("zobrazovanie"));
        JPanel audioPanel = new JPanel();
        audioPanel.setBorder(BorderFactory.createTitledBorder("audio"));
        
        //setting boxes, buttons and lables
        //boxes
        //create
        JComboBox mainCameraBox = new JComboBox();
        JComboBox mainCameraPixelFormatBox = new JComboBox();
        JComboBox mainCameraFrameSizeBox = new JComboBox();
        JComboBox mainCameraFPSBox = new JComboBox();
        JComboBox presentationBox = new JComboBox();
        JComboBox presentationPixelFormatBox = new JComboBox();
        JComboBox presentationFrameSizeBox = new JComboBox();
        JComboBox presentationFPSBox = new JComboBox();
        JComboBox displayBox = new JComboBox();
        JComboBox displaySettingBox = new JComboBox();
        JComboBox audioInComboBox = new JComboBox();
        JComboBox audioOutComboBox = new JComboBox();
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
        //set font
        mainCameraBox.setFont(fontBoxes);
        mainCameraPixelFormatBox.setFont(fontBoxes);
        mainCameraFrameSizeBox.setFont(fontBoxes);
        mainCameraFPSBox.setFont(fontBoxes);
        presentationBox.setFont(fontBoxes);
        presentationPixelFormatBox.setFont(fontBoxes);
        presentationFrameSizeBox.setFont(fontBoxes);
        presentationFPSBox.setFont(fontBoxes);
        displayBox.setFont(fontBoxes);
        displaySettingBox.setFont(fontBoxes);
        audioInComboBox.setFont(fontBoxes);
        audioOutComboBox.setFont(fontBoxes);
        //set action
        mainCameraBox.addActionListener((ActionEvent event) -> {
            actionSetCameraDeviceBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
        });
        mainCameraPixelFormatBox.addActionListener((ActionEvent event) -> {
            actionSetCameraPixelFormatBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
        });
        mainCameraFrameSizeBox.addActionListener((ActionEvent event) -> {
            actionSetCameraFrameSizeBox(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
        });
        mainCameraFPSBox.addActionListener((ActionEvent event) -> {
            
        });
        presentationBox.addActionListener((ActionEvent event) -> {
            actionSetCameraDeviceBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        });
        presentationPixelFormatBox.addActionListener((ActionEvent event) -> {
            actionSetCameraPixelFormatBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        });
        presentationFrameSizeBox.addActionListener((ActionEvent event) -> {
            actionSetCameraFrameSizeBox(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        });
        presentationFPSBox.addActionListener((ActionEvent event) -> {
            
        });
        displayBox.addActionListener((ActionEvent event) -> {
            
        });
        displaySettingBox.addActionListener((ActionEvent event) -> {
            
        });
        audioInComboBox.addActionListener((ActionEvent event) -> {
            
        });
        audioOutComboBox.addActionListener((ActionEvent event) -> {
            
        });
        //path fields
        //creating and setting
        String uvPathFromConfig = "";
        String mirrorPathFromConfig = "";
        if(configuration.has("ultragrid path")){
            try {
                uvPathFromConfig = configuration.getString("ultragrid path");
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(configuration.has("distributor path")){
            try {
                mirrorPathFromConfig = configuration.getString("distributor path");
            } catch (JSONException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        JTextField uvSystemPath = new JTextField(uvPathFromConfig);
        JTextField mirrorSystemPath = new JTextField(mirrorPathFromConfig);
        uvSystemPath.setColumns(20);
        mirrorSystemPath.setColumns(20);
        uvSystemPath.setFont(fontBoxes);
        mirrorSystemPath.setFont(fontBoxes);
        JFileChooser uvFileChooser = new JFileChooser();
        JFileChooser mirrorFileChooser = new JFileChooser();
        JButton setUvSystemPathButton = new JButton("...");
        JButton setMirrorSystemPathButton = new JButton("...");
        setUvSystemPathButton.setFont(fontBoxes);
        setMirrorSystemPathButton.setFont(fontBoxes);
        //setting action path choosing
        setUvSystemPathButton.addActionListener((ActionEvent event) -> {
            int returnVal = uvFileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                uvSystemPath.setText(uvFileChooser.getSelectedFile().getPath());
            }
        });
        setMirrorSystemPathButton.addActionListener((ActionEvent event) -> {
            int returnVal = mirrorFileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                mirrorSystemPath.setText(mirrorFileChooser.getSelectedFile().getPath());
            }
        });
        //info fields
        JTextField uvPathInfoText = new JTextField("uv path");
        JTextField mirrorPathInfoText = new JTextField("hd-rum-transcode path");
        JTextField displayDeviceText = new JTextField("zariadenie");
        JTextField displaySettingText = new JTextField("moznosti");
        JTextField cameraDeviceText = new JTextField("kamera");
        JTextField cameraPixelFormatText = new JTextField("format");
        JTextField cameraFrameSizeText = new JTextField("velkost");
        JTextField cameraFPSText = new JTextField("fps");
        JTextField presentationDeviceText = new JTextField("zdroj");
        JTextField presentationPixelFormatText = new JTextField("format");
        JTextField presentationFrameSizeText = new JTextField("velkost");
        JTextField presentationFPSText = new JTextField("fps");
        JTextField verificationText = new JTextField();
        JTextField audioInText = new JTextField("zvuk in");
        JTextField audioOutText = new JTextField("zvuk out");
        uvPathInfoText.setFont(fontBoxes);
        mirrorPathInfoText.setFont(fontBoxes);
        displayDeviceText.setFont(fontBoxes);
        displaySettingText.setFont(fontBoxes);
        cameraDeviceText.setFont(fontBoxes);
        cameraPixelFormatText.setFont(fontBoxes);
        cameraFrameSizeText.setFont(fontBoxes);
        cameraFPSText.setFont(fontBoxes);
        presentationDeviceText.setFont(fontBoxes);
        presentationPixelFormatText.setFont(fontBoxes);
        presentationFrameSizeText.setFont(fontBoxes);
        presentationFPSText.setFont(fontBoxes);
        audioInText.setFont(fontBoxes);
        audioOutText.setFont(fontBoxes);
        verificationText.setFont(new Font(fontBoxes.getName(), Font.BOLD, fontBoxes.getSize() +3));
        uvPathInfoText.setEditable(false);
        mirrorPathInfoText.setEditable(false);
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
        verificationText.setEditable(false);
        audioInText.setEditable(false);
        audioOutText.setEditable(false);
        uvPathInfoText.setBorder(BorderFactory.createEmptyBorder());
        mirrorPathInfoText.setBorder(BorderFactory.createEmptyBorder());
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
        verificationText.setBorder(BorderFactory.createEmptyBorder());
        audioInText.setBorder(BorderFactory.createEmptyBorder());
        audioOutText.setBorder(BorderFactory.createEmptyBorder());
        //buttons
        JButton reloadUltragridButton = new JButton("restart");
        JButton testCameraButton = new JButton("test kamera");
        JButton testPresentationButton = new JButton("test prezentacia");
        reloadUltragridButton.setFont(fontBoxes);
        testCameraButton.setFont(fontBoxes);
        testPresentationButton.setFont(fontBoxes);
        reloadUltragridButton.addActionListener((ActionEvent event) -> {
            ultragridOK(uvSystemPath.getText(), verificationText);
            try {
                videoDevices = loadVideoDevicesAndSettings(uvSystemPath.getText());
            } catch (IOException ex) {
                videoDevices = new ArrayList<>();
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            setAllJComboBoxesVideosetting(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
            setAllJComboBoxesVideosetting(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        });
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
                System.out.println(outputSetting);
                startUltragrid("uv", reciveSetting, outputSetting);
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
                System.out.println(outputSetting);
                startUltragrid("uv", reciveSetting, outputSetting);
            } catch (IOException ex) {
                Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //check box
        JCheckBox presentationCheckBox = new JCheckBox("prezentacia");
        presentationCheckBox.setFont(fontBoxes);
        presentationCheckBox.addItemListener((ItemEvent e) -> {
            boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
            presetationPanel.setVisible(isSelected);
            testPresentationButton.setVisible(isSelected);
            this.pack();
        });
        
        
        
        
        //putting boxes to panel
        mainCameraPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainCameraPanelConstrains = new GridBagConstraints();
        mainCameraPanelConstrains.insets = new Insets(5,5,5,5);
        mainCameraPanelConstrains.weightx = 0.5;
        mainCameraPanelConstrains.gridx = 0;
        mainCameraPanelConstrains.gridy = 0;
        mainCameraPanelConstrains.gridheight = 1;
        mainCameraPanelConstrains.gridwidth = 1;
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
        videoAudioPanel.add(uvPathInfoText, videoAudioConstrains);
        videoAudioConstrains.anchor = GridBagConstraints.LINE_START;
        videoAudioConstrains.gridx = 1;
        videoAudioConstrains.gridy = 0;
        videoAudioPanel.add(uvSystemPath, videoAudioConstrains);
        videoAudioConstrains.gridx = 2;
        videoAudioConstrains.gridy = 0;
        videoAudioConstrains.ipadx = 0;
        videoAudioPanel.add(setUvSystemPathButton, videoAudioConstrains);
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 1;
        videoAudioPanel.add(mirrorPathInfoText, videoAudioConstrains);
        videoAudioConstrains.gridx = 1;
        videoAudioConstrains.gridy = 1;
        videoAudioPanel.add(mirrorSystemPath, videoAudioConstrains);
        videoAudioConstrains.gridx = 2;
        videoAudioConstrains.gridy = 1;
        videoAudioConstrains.ipadx = 0;
        videoAudioPanel.add(setMirrorSystemPathButton, videoAudioConstrains);
        videoAudioConstrains.anchor = GridBagConstraints.CENTER;
        videoAudioConstrains.gridx = 0;
        videoAudioConstrains.gridy = 3;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 2;
        videoAudioPanel.add(verificationText, videoAudioConstrains);
        videoAudioConstrains.anchor = GridBagConstraints.LINE_START;
        videoAudioConstrains.gridx = 2;
        videoAudioConstrains.gridy = 3;
        videoAudioConstrains.gridheight = 1;
        videoAudioConstrains.gridwidth = 1;
        videoAudioPanel.add(reloadUltragridButton, videoAudioConstrains);
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
        
        //start setting
                
        //try if ultragrid is functional
        ultragridOK(uvSystemPath.getText(), verificationText);
        //load posibylities
        try {
            videoDevices = loadVideoDevicesAndSettings(uvSystemPath.getText());
            audioIn = read_audio_devices_in_or_out(uvPathFromConfig, true);
            audioOut = read_audio_devices_in_or_out(uvPathFromConfig, false);
        } catch (IOException ex) {
            videoDevices = new ArrayList<>();
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        setAudioJComboBox(audioInComboBox, audioIn);
        setAudioJComboBox(audioOutComboBox, audioOut);
        
        setAllJComboBoxesVideosetting(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices);
        setAllJComboBoxesVideosetting(presentationBox, presentationPixelFormatBox, presentationFrameSizeBox, presentationFPSBox, videoDevices);
        
        setJComboBoxDisplay(displayBox, displaySettingBox);
        
        presetationPanel.setVisible(false);
        testPresentationButton.setVisible(false);
        presentationCheckBox.setSelected(false);
        
        try {
            String videoSetting = configuration.getString("producer settings");
            SetVideoSettingFromConfig(mainCameraBox, mainCameraPixelFormatBox, mainCameraFrameSizeBox, mainCameraFPSBox, videoDevices, videoSetting);
        } catch (JSONException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }
    
    private void setMiscsPanel(){
        //moja ip addresa
        //pridat odobrat zo zoznamu servrov, ??zmenit ich prezyvku
        //cesty k layoutom
        
        JPanel myIpAddressPanel = new JPanel();
        myIpAddressPanel.setBorder(BorderFactory.createTitledBorder("moja ip"));
        JPanel serverIpSettingPanel = new JPanel();
        serverIpSettingPanel.setBorder(BorderFactory.createTitledBorder("server ip setting"));
        JPanel layoutPanel = new JPanel();
        layoutPanel.setBorder(BorderFactory.createTitledBorder("rozlozenie"));
        
        JComboBox serverIpSelect = new JComboBox();
        serverIpSelect.setFont(fontBoxes);
        serverIpSelect.setEditable(false);
        
        JTextField myIpSetTextField = new JTextField();
        myIpSetTextField.setEditable(true);
        myIpSetTextField.setFont(fontBoxes);
        
        JTextField serverIpAddresChangeTextField = new JTextField();
        JTextField serverIpNameChange = new JTextField();
        serverIpAddresChangeTextField.setColumns(20);
        serverIpNameChange.setColumns(20);
        serverIpAddresChangeTextField.setFont(fontBoxes);
        serverIpNameChange.setFont(fontBoxes);
        
        myIpAddressPanel.setLayout(new GridBagLayout());
        GridBagConstraints myIpAddressConstraints = new GridBagConstraints();
        myIpAddressConstraints.gridheight = 1;
        myIpAddressConstraints.gridwidth = 3;
        myIpAddressConstraints.gridx = 0;
        myIpAddressConstraints.gridy = 0;
        videoAudioPanel.add(myIpSetTextField, myIpAddressConstraints);
        
        miscsPanel.setLayout(new GridBagLayout());
        GridBagConstraints miscsPanelConstraints = new GridBagConstraints();
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
        miscsPanel.add(layoutPanel, miscsPanelConstraints);
    }
    
    private List<VideoDevice> loadVideoDevicesAndSettings(String uvPath) throws IOException{
        List<VideoDevice> ret = null;
        if(correctUv){
            String osName = System.getProperty("os.name");
            String uvVideoSetting;
            if(osName.contains("Windows")){
                uvVideoSetting = "dshow";
                ret = getVideoDevicesAndSettingsWindows(uvPath, uvVideoSetting);
            }else if(osName.contains("Linux")){
                uvVideoSetting = "v4l2";
                ret = getVideoDevicesAndSettingsLinux(uvPath, uvVideoSetting);
            }else if(osName.contains("Mac")){
                uvVideoSetting = "macsomethingvideoblablabla";
                ret = null;
            }else{      //probably should log incorrect os system
                return null;
            }
        }else{
            ret = new ArrayList<>();
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
            
            Pattern modePattern = Pattern.compile("Mode\\s+(\\d+):\\s(.*?)\\s*(\\d*x\\d*)\\s*@([0-9\\.])");
            Matcher modeMatcher = modePattern.matcher(line);
            while(modeMatcher.find()){
                if(videoInputs.size() == 0){    //something strange happend, stop process
                    return videoInputs;
                }
                VideoPixelFormat vpf = new VideoPixelFormat();
                VideoFrameSize vfs = new VideoFrameSize();
                VideoFPS vfps = new VideoFPS();
                VideoDevice vd = videoInputs.get(videoInputs.size() - 1);
                vfps.setting = ":" + vd.device + ":" + modeMatcher.group(1);
                vpf.pixelFormat = modeMatcher.group(2);
                vpf.name = modeMatcher.group(2);
                vfs.widthXheight = modeMatcher.group(3);
                vfps.fps = modeMatcher.group(4);
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
                vfs.fps.add(vfps);
            }
        }
                        
        
        return videoInputs;
    }
    
    String getVideoSettings(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices){        
        String ret = null;
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
    
    void setAllJComboBoxesVideosetting(JComboBox deviceBox, JComboBox formatBox, JComboBox widthXheightBox, JComboBox fpsBox, List<VideoDevice> videoDevices){
        setJComboBoxDevices(deviceBox, videoDevices);
        setJComboBoxFormat(formatBox, deviceBox, videoDevices);
        setJComboBoxFrameSize(widthXheightBox, deviceBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, deviceBox, formatBox, widthXheightBox, videoDevices);
    }
    
    private void actionSetCameraDeviceBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices){
        
        setJComboBoxFormat(formatBox, devicesBox, videoDevices);
        setJComboBoxFrameSize(frameSizeBox, devicesBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
            
    }
    
    private void actionSetCameraPixelFormatBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices){

        setJComboBoxFrameSize(frameSizeBox, devicesBox, formatBox, videoDevices);
        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
    }
    
    private void actionSetCameraFrameSizeBox(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices){

        setJComboBoxFPS(fpsBox, devicesBox, formatBox, frameSizeBox, videoDevices);
    }
    
    private List<AudioDevice> read_audio_devices_in_or_out(String uvAddress, boolean audio_in) throws IOException{
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
    
    void ultragridOK(String uvAddress, JTextField verificationTextField){
        if(uvProcess != null){
            uvProcess.destroyForcibly();
        }
        
        //initiial tests if it can be ultragrid
        if(uvAddress.isEmpty()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText("prazdna adresa");
            correctUv = false;
            return;
        }
        File uvFile = new File(uvAddress);
        if(!uvFile.exists()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText("cesta k suboru je neplatna");
            correctUv = false;
            return;
        }
        if(uvFile.isDirectory()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText("casta je adresar");
            correctUv = false;
            return;
        }
        if(!uvFile.canExecute()){
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText("subor nie je spustitelny");
            correctUv = false;
            return;
        }
            
        try {
            uvProcess = new ProcessBuilder(uvAddress, "help").start();  //may add output check, maby later, or some other checks
            TimeUnit.MILLISECONDS.sleep(300);
            correctUv = (uvProcess.exitValue() == 0);
        } catch (IllegalThreadStateException ex){
            uvProcess.destroyForcibly();
        } catch (IOException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(OptionsMainMenuWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        uvProcess.destroyForcibly();
        //probably overkill destroing process, but I realy dont want to allow process to survive
        
        if(correctUv){
            verificationTextField.setForeground(Color.getHSBColor((float)0.39, (float)1, (float)0.8));
            verificationTextField.setText("ultragid je ok");
        }else{
            verificationTextField.setForeground(Color.red);
            verificationTextField.setText("ultragrid sa neda spustit");
        }        
    }
    
    private void startUltragrid(String uvAddress, String uvReciveSetting, String uvSendSetting) throws IOException{
        if(uvProcess != null){
            uvProcess.destroyForcibly();
        }
        if(correctUv){
            ProcessBuilder pb = new ProcessBuilder("uv", "-d", uvReciveSetting, "-t", uvSendSetting);
            
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
        displayBox.addActionListener((ActionEvent event) -> {
            boolean sdlSelected = displayBox.getSelectedItem().equals("sdl");
            displaySettingBox.removeAllItems();
            displaySettingBox.addItem("none");
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
    
    private void setAudioJComboBox(JComboBox audioBox, List<AudioDevice> audioDevicis){
        audioBox.removeAll();
        for(int i=0;i<audioDevicis.size();i++){
            audioBox.addItem(audioDevicis.get(i).name);
        }
    }
    
    private void SetVideoSettingFromConfig(JComboBox devicesBox, JComboBox formatBox, JComboBox frameSizeBox, JComboBox fpsBox,
                                            List<VideoDevice> videoDevices, String videoSetting){
        String[] videoSetingPart = videoSetting.split(":", 1);
        if(videoSetingPart.length == 2){
            for(int i=0;i<videoDevices.size();i++){
                for(int j=0;j<videoDevices.get(i).vpf.size();j++){
                    for(int k=0;k<videoDevices.get(i).vpf.get(j).vfs.size();k++){
                        for(int l=0;l<videoDevices.get(i).vpf.get(j).vfs.get(k).fps.size();l++){
                            if(videoDevices.get(i).vpf.get(j).vfs.get(k).fps.get(l).setting.equals(videoSetingPart[1])){
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
    
    public void discardAction(){
        dispose();
    }
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