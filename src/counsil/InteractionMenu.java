/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
     * Represents current state of raise hand button
     */
    private boolean raisedHand;
    
    /**
     * Represents current state of volume slider
     */
    private boolean volumeSliderShown;
    
    /**
     * Represents buttons in current menu instance
     */
    private final List<JButton> buttons;

    /**
     * Represents button types
     */
    private enum ButtonType { 
        ABOUT, EXIT, ATTENTION, MUTE, VOLUME 
    }
    
    /**
     * Represents current state of sound
     */
    private boolean muted;
    
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
    InteractionMenu(String role, Position position){    
        
        super("CoUnSil");         
        setLayout(new GridBagLayout());
        setUndecorated(true);         
        getRootPane().setBorder(BorderFactory.createTitledBorder(role));
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setLocation(position.x, position.y);
                
        buttons = new ArrayList<>();
        raisedHand = false;
        muted = false;
        volumeSliderShown = false;
        
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
         else {
             list.add(InteractionMenu.ButtonType.MUTE);
             list.add(InteractionMenu.ButtonType.VOLUME);
         }
         
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
                   
        GroupLayout.ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        buttons.stream().forEach((button) -> {
            hGroup.addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        });
               
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        buttons.stream().forEach((button) -> {
            vGroup.addComponent(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        });
        
        layout.setHorizontalGroup(hGroup);
        layout.setVerticalGroup(vGroup);
        getContentPane().setLayout(layout); 

        pack();  
 
    }
    
    /**
     * Sets specific attributes to menu button (title, action, ...)
     * @param button instance of button
     * @param type type to be button associated with
     */
    private void setSpecificAttributes (JButton button, InteractionMenu.ButtonType type){
       
       if (type == InteractionMenu.ButtonType.EXIT){
           button.setText("Exit");
           button.addActionListener((ActionEvent evt) -> {
               exitButtonActionPerformed();
           });
       }
       else if (type == InteractionMenu.ButtonType.ABOUT){
           button.setText("About");
           button.addActionListener((ActionEvent evt) -> {
               aboutButtonActionPerformed();
           });
       }
       else if (type == InteractionMenu.ButtonType.ATTENTION){
            button.setText("Raise hand");
            button.addActionListener((ActionEvent evt) -> {
                attentionButtonActionPerformed(button);
            });
       }
       else if (type == InteractionMenu.ButtonType.MUTE){
           button.setText("Mute");
           button.addActionListener((ActionEvent evt) -> {
               muteButtonActionPerformed(button);
           });           
       }
       
       else if (type == InteractionMenu.ButtonType.VOLUME){
           button.setText("Volume");
           button.addActionListener((ActionEvent evt) -> {
               volumeButtonActionPerformed();
           }); 
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
        String message = "Do you really want to quit CoUnSil?";
        String title = "Quit CoUnSil?";
        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {                        
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(InteractionMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
            dispose();
            System.exit(0);
        }
    }   
   
    /**
     * Starts raise/lower hand interaction when button is clicked
     * @param button clicked button 
     */
    private void attentionButtonActionPerformed(JButton button) {                                                 
        if (raisedHand) {
            button.setText("Raise hand");            
        } else {
            button.setText("Lower hand");             
        }
        raisedHand = !raisedHand;
        
        interactionMenuListeners.stream().forEach((listener) -> {            
            listener.raiseHandActionPerformed();
        });
    }  
    
   
    
    /**
     * Mutes/unmutes sound
     * @param button clicked button
     */
    private void muteButtonActionPerformed(JButton button) {       
        
        if (muted) {
            button.setText("Mute");     
             interactionMenuListeners.stream().forEach((listener) -> {
                listener.unmuteActionPerformed();
            });
        } else {
            button.setText("Unmute");   
            interactionMenuListeners.stream().forEach((listener) -> {
                listener.muteActionPerformed();
            });
        }        
        muted = !muted;        
    }
    
    private void volumeButtonActionPerformed() {        
        if (!volumeSliderShown){
            volumeSliderShown = true;
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {                
                    final VolumeSlider slider = new VolumeSlider();                  
                    slider.setLocation(getLocation().x, getLocation().y + 200);                    
                    slider.addVolumeSliderListener(new VolumeSliderListener() {

                        @Override
                        public void increaseActionPerformed() {
                            interactionMenuListeners.stream().forEach((listener) -> {
                                listener.increaseActionPerformed();
                            }); 
                        }

                        @Override
                        public void decreaseActionPerformed() {
                            interactionMenuListeners.stream().forEach((listener) -> {
                                listener.decreaseActionPerformed();
                            }); 
                        }                        
                    });
                }               
            });  
        }
    }
    
}