/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;

/**
 *
 * @author Desanka
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
    
    
    public Font font;

    /**
     * List of listeners
     */
    protected final List<InteractionMenuListener> interactionMenuListeners = new ArrayList<>();
    private final ResourceBundle languageBundle;

    /**
     * Creates menu and sets its parameters
     *
     * @param role role of current user
     * @param position menu position
     * @param iml initial menu to return to
     */
    InteractionMenu(String role, Position position, InitialMenuLayout iml, ResourceBundle languageBundle, Font font) {

        super("CoUnSil");

        initialMenu = iml;

        setLayout(new GridBagLayout());
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createTitledBorder(role));
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setLocation(position.x, position.y);
        setDefaultLookAndFeelDecorated(false);
        this.languageBundle = languageBundle;
        this.font = font;
        overrideJOptionPaneBundle();
    }

    /**
     * Initializes buttons and sets menu as visible
     */
    public void publish() {

        addBasicButtons();
        initComponents();
        setVisible(true);
    }

    public void addInteractionMenuListener(InteractionMenuListener listener) {

        interactionMenuListeners.add(listener);
    }

    private void refreshButtonActionPerformed() {

        interactionMenuListeners.stream().forEach((listener) -> {
            listener.refreshActionPerformed();
        });
    }

    private void settingsButtonActionPerformed() {

        initialMenu.optionMainMenuWindow = new OptionsMainMenuWindow(font, initialMenu.configurationFile, initialMenu, languageBundle);
    }

    private void addBasicButtons() {

        JButton refreshButton = new JButton();
        refreshButton.setText(languageBundle.getString("REFRESH"));
        refreshButton.addActionListener((ActionEvent evt) -> {
            refreshButtonActionPerformed();
        });

        JButton exitButton = new JButton();
        exitButton.setText(languageBundle.getString("EXIT"));
        exitButton.addActionListener((ActionEvent evt) -> {
            exitButtonActionPerformed();
        });

        buttons.add(refreshButton);
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

        buttons.stream().forEach((button) -> {

            button.setPreferredSize(new java.awt.Dimension(130, 31));
            button.setFont(font);

            verticalGroup
                    .addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);

            horizontalGroup
                    .addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        });

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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(InteractionMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
            initialMenu.closeCounsil();
            this.dispose();
        }
    }

    private void overrideJOptionPaneBundle() {
        UIManager.put("OptionPane.yesButtonText", languageBundle.getString("YES"));
        UIManager.put("OptionPane.noButtonText", languageBundle.getString("NO"));
        UIManager.put("OptionPane.cancelButtonText", languageBundle.getString("CANCEL"));        
    }
}
