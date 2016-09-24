package counsil;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;

/**
 * Represents menu run in application in default state (without additional
 * buttons)
 *
 * @author xdaxner
 */
public class InteractionMenu extends JFrame {

    /**
     * Represents buttons in current menu instance
     */
    protected final List<JButton> buttons = new ArrayList<>();

    /**
     * Instance of initial menu to return after counsil session end
     */
    private final InitialMenuLayout initialMenu;

    /**
     * Currently used font
     */
    public Font font;

    /**
     * List of listeners
     */
    protected final List<InteractionMenuListener> interactionMenuListeners = new ArrayList<>();

    /**
     * Currently used language bundle
     */
    protected final ResourceBundle languageBundle;

    /**
     * Creates menu and sets its parameters
     *
     * @param role role of current user
     * @param position menu position
     * @param initialMenu initial menu to return to
     */
    InteractionMenu(String role, Position position, InitialMenuLayout initialMenu, ResourceBundle languageBundle, Font font) {

        super("CoUnSil");

        this.initialMenu = initialMenu;
        this.languageBundle = languageBundle;
        this.font = font;

        setLayout(new GridBagLayout());
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createTitledBorder(role));
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setLocation(position.x, position.y);
        setDefaultLookAndFeelDecorated(false);

        overrideJOptionPaneBundle();
    }

    /**
     * Initializes menu buttons, adds them to menu and sets menu as visible
     */
    public void publish() {
        addBasicButtons();
        initComponents();
        setVisible(true);
    }

    /**
     * Adds interacion menu listener to the list of currently listening
     * listeners
     *
     * @param listener
     */
    public void addInteractionMenuListener(InteractionMenuListener listener) {
        interactionMenuListeners.add(listener);
    }

    /**
     * Sends refreshActionPerformed to all listening listeners
     */
    private void refreshButtonActionPerformed() {
        interactionMenuListeners.stream().forEach((listener) -> {
            listener.refreshActionPerformed();
        });
    }
    
    /**
     * Sends saveLayoutActionPerformed to all listening listeners
     */
    private void saveLayoutButtonActionPerformed() {
       
        JFrame dialogFrame = new JFrame(languageBundle.getString("SAVE_LAYOUT"));        
        String fileName = JOptionPane.showInputDialog(dialogFrame, languageBundle.getString("SAVE_LAYOUT"));        
                
        if (!"".equals(fileName.replaceAll("\\s+",""))) {
            interactionMenuListeners.stream().forEach((listener) -> {
                listener.saveLayoutActionPerformed(fileName);
            });
        }
    }

    /**
     * Adds buttons which are used by all the roles to button list
     */
    private void addBasicButtons() {

        JButton refreshButton = new JButton();
        refreshButton.setText(languageBundle.getString("REFRESH"));
        refreshButton.addActionListener((ActionEvent evt) -> {
            refreshButtonActionPerformed();
        });
        
        JButton saveLayoutButton = new JButton();
        saveLayoutButton.setText(languageBundle.getString("SAVE_LAYOUT"));
        saveLayoutButton.addActionListener((ActionEvent evt) -> {
            saveLayoutButtonActionPerformed();
        }); 
       
        JButton exitButton = new JButton();
        exitButton.setText(languageBundle.getString("EXIT"));
        exitButton.addActionListener((ActionEvent evt) -> {
            exitButtonActionPerformed();
        });
        
        buttons.add(refreshButton);
        buttons.add(saveLayoutButton);
        buttons.add(exitButton);
    }

    /**
     * Creates buttons for menu, according to button types
     *
     * @param descriptions types of buttons, to be used
     */
    private void initComponents() {

        GroupLayout layout = new GroupLayout(getContentPane());
        GroupLayout.SequentialGroup horizontalGroup = layout.createSequentialGroup();
        GroupLayout.ParallelGroup verticalGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);

        // add buttons to menu grid
        buttons.stream().forEach((button) -> {
            button.setPreferredSize(new java.awt.Dimension(130, 31));
            button.setFont(font);

            verticalGroup
                .addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);

            horizontalGroup
                .addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        });

        // set menu grid and layout
        layout.setHorizontalGroup(horizontalGroup);
        layout.setVerticalGroup(verticalGroup);
        getContentPane().setLayout(layout);

        pack();
    }

    /**
     * Starts exiting program when "Exit" button is clicked
     */
    private void exitButtonActionPerformed() {
        String message = languageBundle.getString("EXIT_CONFIRMATION");
        String title = languageBundle.getString("EXIT_TITLE");
        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            initialMenu.closeCounsil();
            this.dispose();
        }
    }

    /**
     * Sets custom localization to exit message
     */
    private void overrideJOptionPaneBundle() {
        UIManager.put("OptionPane.yesButtonText", languageBundle.getString("YES"));
        UIManager.put("OptionPane.noButtonText", languageBundle.getString("NO"));
        UIManager.put("OptionPane.cancelButtonText", languageBundle.getString("CANCEL"));
    }    
}
