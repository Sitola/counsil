package gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import mediaApplications.RumHD;
import mediaApplications.UltraGridConsumer;
import mediaApplications.UltraGridProducer;
import networkRepresentation.NetworkSite;


public class ApplicationDialog extends JDialog {
    private MyButton okButton;
    private MyButton cancelButton;

    private JTextField applicationPath;
    private JTextField commantLineParameters;
    private JTextField sourceSiteField;
    private JComboBox appType;

    private static final String UltraGridProducerLabel = "UltraGrid producer";
    private static final String UltraGridConsumerLabel = "UltraGrid Gbps consumer";
    private static final String RumHDLabel = "Rum HD";

    private MediaApplication mediaApplication = null;

    private boolean hasBeenApplicationSet = false;


    public ApplicationDialog(Frame frame) {
        super(frame, true);
        this.setSize(new Dimension(700, 350));
        this.setMyContentPanePanel();
        
        this.setTitle("Add application dialog");
        this.okButton = new MyButton("OK");
        this.cancelButton = new MyButton("Cancel");
        this.getRootPane().setDefaultButton(this.okButton);
        this.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                okButtonClicked();
            }
        });
        this.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cancelButtonClicked();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancelButtonClicked();
            }
        });

        init();
        
        appType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSourceSiteEditability();
            }
        });
        setSourceSiteEditability();
    }

    private void cancelButtonClicked() {
        this.setVisible(false);
    }


    private void okButtonClicked() {
        if (mediaApplication == null) {
            // this only happens when creating a new application
            if (UltraGridProducerLabel.equals(appType.getSelectedItem())) {
                mediaApplication = new UltraGridProducer();
            } else if (UltraGridConsumerLabel.equals(appType.getSelectedItem())) {
                mediaApplication = new UltraGridConsumer();
            } else if (RumHDLabel.equals(appType.getSelectedItem())) {
                mediaApplication = new RumHD();
            } else {
                assert false : "Application listed in the code is not handled!";
            }
        }

        if ("".equals(applicationPath.getText())) {
            JOptionPane.showMessageDialog(this, "Application path cannot be emtpy");
            return;
        } else {
            this.mediaApplication.setApplicationPath(applicationPath.getText());
        }

        /*
         if ("".equals(commantLineParameters.getText())) {
              //paremeters probably can be emtpy but they should have some format
             //maybe they can be checked by regular expresions

             //JOptionPane.showMessageDialog(this, "Paremeters cannot be emtpy");
            //return;
        } else { */
        this.mediaApplication.setApplicationCmdOptions(commantLineParameters.getText());
        //}

        if (sourceSiteField.isEditable()) {
            if ("".equals(sourceSiteField.getText())) {
                ((MediaApplicationConsumer) mediaApplication).setSourceSite(null);
            } else {
                ((MediaApplicationConsumer) mediaApplication).setSourceSite(new NetworkSite(sourceSiteField.getText()));
                //MediaApplications.UltraGrid1500Producer cannot be cast to MediaAppFactory.MediaApplicationConsumer
            }
        }

        this.hasBeenApplicationSet = true;
        this.setVisible(false);

    }


    public void setMediaApplication(MediaApplication ma) {
        if (ma != null) {
            this.mediaApplication = ma;

            if (ma instanceof UltraGridProducer) {
                appType.setSelectedItem(UltraGridProducerLabel);
            } else if (ma instanceof UltraGridConsumer) {
                appType.setSelectedItem(UltraGridConsumerLabel);
            } else if (ma instanceof RumHD) {
                appType.setSelectedItem(RumHDLabel);
            } else {
                assert false : "Application listed in the code is not handled!";
            }

            appType.setEnabled(false); //disallow changing application type

            if(ma.getApplicationPath() != null){
                applicationPath.setText(ma.getApplicationPath());
            }
            if(ma.getApplicationCmdOptions() != null){
                commantLineParameters.setText(ma.getApplicationCmdOptions());
            }
            

            if (ma instanceof MediaApplicationConsumer) {
                if (((MediaApplicationConsumer) ma).getSourceSite() == null) {
                    sourceSiteField.setText("");
                } else {
                    sourceSiteField.setText(((MediaApplicationConsumer) ma).getSourceSite().getSiteName());
                }
                sourceSiteField.setEditable(true);
            }
        }

    }

    public boolean getHasBeenApplicationSet() {
        return this.hasBeenApplicationSet;
    }

    public MediaApplication getMediaApplication() {
        return this.mediaApplication;
    }


    private void setSourceSiteEditability() {
        if (UltraGridConsumerLabel.equals(appType.getSelectedItem())) {
            sourceSiteField.setEditable(true);
        } else {
            sourceSiteField.setEditable(false);
        }
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        c.weighty = 0.2;
        c.gridwidth = 2;

        this.getContentPane().add(new MyTextLabel("New Media Application", 27, 22, 320, 30), c);

        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 0.10;
        this.getContentPane().add(new MyTextLabel("Type:", 16, 17, 130, 23), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Path:", 16, 17, 130, 23), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Command-line parameters:", 16, 17, 220, 23), c);

        c.gridy++;
        this.getContentPane().add(new MyTextLabel("Source site:", 16, 17, 130, 23), c);

        c.insets = new Insets(0, 10, 5, 20);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.7;
        c.gridx = 1;

        this.getContentPane().add(this.sourceSiteField = new JTextField(), c);

        c.gridy--;
        this.getContentPane().add(this.commantLineParameters = new JTextField(), c);

        c.gridy--;
        this.getContentPane().add(this.applicationPath = new JTextField(), c);

        c.gridy--;
        this.getContentPane().add(this.appType = new JComboBox(), c);

        c.gridy = 6;
        c.gridwidth = 1;
        c.weighty = 0.40;
        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.NONE;
        this.getContentPane().add(this.okButton, c);

        c.insets = new Insets(0, 10, 5, 5);
        c.anchor = GridBagConstraints.LAST_LINE_END;
        this.getContentPane().add(this.cancelButton, c);

        appType.addItem(UltraGridProducerLabel);
        appType.addItem(UltraGridConsumerLabel);
        appType.addItem(RumHDLabel);
    }


    private void setMyContentPanePanel() {
        this.setContentPane(new JPanel(new GridBagLayout()) {
            @Override
            public void paint(Graphics g) {
                this.setPreferredSize(ApplicationDialog.this.getSize());

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
