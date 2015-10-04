package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import mediaAppFactory.MediaApplication;
import mediaAppFactory.MediaApplicationConsumer;
import networkRepresentation.NetworkSite;
import java.util.ArrayList;
import java.util.Collection;


public class RemoveApplicationDialog extends JDialog {
    private MyButton okButton;
    private MyButton cancelButton;

    private JComboBox availableApplicationsBox;

    private ArrayList<MediaApplication> availableApplications;
    
    private MediaApplication mediaApplication = null;

    private boolean hasBeenApplicationSet = false;


    public RemoveApplicationDialog(Frame frame, Collection<MediaApplication> apps) {
        super(frame, true);
        this.setSize(new Dimension(700, 350));
        this.setMyContentPanePanel();
        
        this.setTitle("Remove application dialog");
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

        availableApplications = new ArrayList<MediaApplication>(apps);
        
        init();
        
        availableApplicationsBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
            }
        });
    }

    private void cancelButtonClicked() {
        this.setVisible(false);
    }


    private void okButtonClicked() {

        this.mediaApplication = availableApplications.get(availableApplicationsBox.getSelectedIndex());
        this.hasBeenApplicationSet = true;
        this.setVisible(false);

    }


    public void setMediaApplication(MediaApplication ma) {
        
        throw new UnsupportedOperationException("");

    }

    public boolean getHasBeenInterfaceSet() {
        return this.hasBeenApplicationSet;
    }

    public MediaApplication getMediaApplication() {
        return this.mediaApplication;
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        c.weighty = 0.2;
        c.gridwidth = 2;

        this.getContentPane().add(new MyTextLabel("Stop Media Application", 27, 22, 320, 30), c);

        c.gridy++;
        c.gridwidth = 1;
        c.weighty = 0.10;
        this.getContentPane().add(new MyTextLabel("Select application to stop:", 16, 17, 130, 23), c);

        c.insets = new Insets(0, 10, 5, 20);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.7;
        c.gridx = 1;

        // this.getContentPane().add(this.sourceSiteField = new JTextField(), c);

        c.gridy--;
        // this.getContentPane().add(this.commantLineParameters = new JTextField(), c);

        c.gridy--;
        // this.getContentPane().add(this.applicationPath = new JTextField(), c);

        c.gridy--;
        this.getContentPane().add(this.availableApplicationsBox = new JComboBox(), c);

        c.gridy = 6;
        c.gridwidth = 1;
        c.weighty = 0.40;
        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.NONE;
        this.getContentPane().add(this.okButton, c);

        c.insets = new Insets(0, 10, 5, 5);
        c.anchor = GridBagConstraints.LAST_LINE_END;
        this.getContentPane().add(this.cancelButton, c);

        for (MediaApplication app : availableApplications) {
            availableApplicationsBox.addItem(app.toString());
        }
        
    }


    private void setMyContentPanePanel() {
        this.setContentPane(new JPanel(new GridBagLayout()) {
            @Override
            public void paint(Graphics g) {
                this.setPreferredSize(RemoveApplicationDialog.this.getSize());

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
