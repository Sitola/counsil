/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

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
    
    
    /**
     * List of listeners
     */
    protected final List<InteractionMenuListener> interactionMenuListeners = new ArrayList<>();
    
        
    /**
     * Creates menu and sets its parameters
     *
     * @param role role of current user
     * @param position menu position
     * @param iml initial menu to return to
     */
    InteractionMenu(String role, Position position, InitialMenuLayout iml) {

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
        throw new UnsupportedOperationException("NOT SUPPORTED YET."); //To change body of generated methods, choose Tools | Templates.
    }

    private void addBasicButtons() {

        JButton refreshButton = new JButton();
        refreshButton.setText(getResource().getString("REFRESH"));
        refreshButton.addActionListener((ActionEvent evt) -> {
            refreshButtonActionPerformed();
        });
        
        JButton settingsButton = new JButton();
        settingsButton.setText(getResource().getString("SETTINGS"));
        settingsButton.addActionListener((ActionEvent evt) -> {
            settingsButtonActionPerformed();
        });

        JButton exitButton = new JButton();
        exitButton.setText(getResource().getString("EXIT"));
        exitButton.addActionListener((ActionEvent evt) -> {
            exitButtonActionPerformed();
        });

        buttons.add(refreshButton);        
        buttons.add(settingsButton);
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
            
            button.setFont(new java.awt.Font("Tahoma", 0, 18));
            button.setPreferredSize(new java.awt.Dimension(130, 31));
            
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

    
    static ResourceBundle getResource() {
        return java.util.ResourceBundle.getBundle("resources");
    }
    

    /**
     * Starts exiting program when "Exit" button is clicked
     */
    private void exitButtonActionPerformed() {
        String message = getResource().getString("EXIT_CONFIRMATION");
        String title = getResource().getString("EXIT_TITLE");
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
}
