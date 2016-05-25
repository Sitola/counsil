/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
 * @author desanka
 */
public class InteractionMenu extends JFrame {
        
    /**
     * Represents buttons in current menu instance
     */
    private final List<JButton> buttons  = new ArrayList<>();

    private void refreshButtonActionPerformed() {
        
        interactionMenuListeners.stream().forEach((listener) -> {            
            listener.refreshActionPerformed();
        });
    }

    private void settingsButtonActionPerformed() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Represents button types
     */
    private enum ButtonType { 
         ATTENTION, REFRESH, SETTINGS, ABOUT, EXIT,
    }
    
 
    /**
     * Instance of initial menu to return after counsil session end
     */
    private final InitialMenuLayout initialMenuLayout;
    
    /**
     * List of raiseHandButton listeners
     */
    private final List<InteractionMenuListener> interactionMenuListeners = new ArrayList<>();
  
            
    /**
     * adds listener of button
     * @param listener
     */
    public void addInteractionMenuListener(InteractionMenuListener listener) {
        interactionMenuListeners.add(listener);
    }
       
    
    /**
     * Initializes menu
     * @param role role of current user
     * @param position menu position
     */
    InteractionMenu(String role, Position position, InitialMenuLayout iml){    
        
        super("CoUnSil");       
        initialMenuLayout = iml;  
        setLayout(new GridBagLayout());
        setUndecorated(true);         
        getRootPane().setBorder(BorderFactory.createTitledBorder(role));
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setLocation(position.x, position.y);
        
        initComponents(getButtonsByRole(role));   
      
        buttons.stream().forEach((button) -> {
            add(button);
        });
     
        JFrame.setDefaultLookAndFeelDecorated(true);
        setVisible(true);
    }
    
   /**
    * Creates list of button types according to user role
    * @param role
    * @return list of buttons types, which will menu contain
    */
    private List<InteractionMenu.ButtonType> getButtonsByRole(String role){
         List<InteractionMenu.ButtonType> list = new ArrayList<>();
                  
         if ("student".equals(role.toLowerCase())){
              list.add(InteractionMenu.ButtonType.ATTENTION);             
         }        
         list.add(InteractionMenu.ButtonType.REFRESH);
         list.add(InteractionMenu.ButtonType.SETTINGS);
         list.add(InteractionMenu.ButtonType.ABOUT);
         list.add(InteractionMenu.ButtonType.EXIT);         
         
         return list;
    }
    
    /**
     * Creates buttons for menu, according to button types
     * @param descriptions types of buttons, to be used
     */
    private void initComponents(List<InteractionMenu.ButtonType> descriptions){   
       
        for (ButtonType type : descriptions) {
            
            JButton button = new JButton();
            button.setFont(new java.awt.Font("Tahoma", 0, 18));
            button.setMaximumSize(new java.awt.Dimension(150, 25));
            button.setMinimumSize(new java.awt.Dimension(109, 25));
            button.setPreferredSize(new java.awt.Dimension(130, 31));

            setSpecificAttributes(button, type);  
            buttons.add(button);        
        }        
        
        setAlwaysOnTop(true);  
        setResizable(false);        
       
        GroupLayout layout = new GroupLayout(getContentPane());               
                   
        GroupLayout.ParallelGroup verticalGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        buttons.stream().forEach((button) -> {
            verticalGroup.addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        });
               
        GroupLayout.SequentialGroup horizontalGroup = layout.createSequentialGroup();
        buttons.stream().forEach((button) -> {
            horizontalGroup.addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        });
        
        layout.setHorizontalGroup(horizontalGroup);
        layout.setVerticalGroup(verticalGroup);
        getContentPane().setLayout(layout); 

        pack();  
 
    }
    
    /**
     * Sets specific attributes to menu button (title, action, ...)
     * @param button instance of button
     * @param type type to be button associated with
     */
    private void setSpecificAttributes (JButton button, InteractionMenu.ButtonType type){
       
       if (null != type)switch (type) {
            case EXIT:
                button.setText("Exit");
                button.addActionListener((ActionEvent evt) -> {
                    exitButtonActionPerformed();
                });  break;
            case ABOUT:
                button.setText("About");
                button.addActionListener((ActionEvent evt) -> {
                    aboutButtonActionPerformed();
                });  break;
            case ATTENTION:
                button.setText("Raise hand");
                button.addActionListener((ActionEvent evt) -> {
                    attentionButtonActionPerformed();
                }); break;    
            case SETTINGS:
                button.setText("Settings");
                button.addActionListener((ActionEvent evt) -> {
                    settingsButtonActionPerformed();
                }); break;  
            case REFRESH:
                button.setText("Refresh");
                button.addActionListener((ActionEvent evt) -> {
                    refreshButtonActionPerformed();
                });  break;
            default:
                break;
        }           
       
    }   
    
   /**
    * Shows message after "About" button is clicked
    */
   private void aboutButtonActionPerformed() {                                                 
        JOptionPane.showMessageDialog(null, " CoUnSiL\n" +"(CoUniverse for Sign Language)\n" +"\n" +"\n" +
            "Videoconferencing environment for remote interpretation of sign language.");           
    }   
   
    /**
    * Starts exiting program when "Exit" button is clicked
    */
    private void exitButtonActionPerformed() {                                                 
        String message = "Do you really want to quit CoUnSiL?";
        String title = "Quit CoUnSiL?";
        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {                        
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(InteractionMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
            initialMenuLayout.closeCounsil();
            this.dispose();           
        }
    }   
   
    /**
     * Starts raise/lower hand interaction when button is clicked    
     */
    private void attentionButtonActionPerformed() {                                                
        interactionMenuListeners.stream().forEach((listener) -> {            
            listener.raiseHandActionPerformed();
        });
    }  
}
