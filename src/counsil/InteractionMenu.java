/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.JOptionPane;

/**
 *
 * @author desanka
 */
public class InteractionMenu extends javax.swing.JFrame {

    private boolean raisedHand;
    
    //! todo documentation
    private final List<javax.swing.JButton> buttons;

    //! todo documentation
    public enum ButtonType { 
        ABOUT, EXIT, ATTENTION, MUTE, VOLUME 
    }

    //! todo documentation
    public InteractionMenu(String role) {  
        
        raisedHand = false;
        buttons = new ArrayList<>();        
        initComponents(getButtonsByRole(role));  
    }
    
    //! todo documentation
    private List<ButtonType> getButtonsByRole(String role){
         List<ButtonType> list = new ArrayList<>();
                  
         if ("student".equals(role.toLowerCase())){
              list.add(ButtonType.ATTENTION);             
         }
         else {
             list.add(ButtonType.MUTE);
             list.add(ButtonType.VOLUME);
         }
         
         list.add(ButtonType.ABOUT);
         list.add(ButtonType.EXIT);
         
         return list;
    }

    //! todo documentation
    private void initComponents(List<ButtonType> descriptions){        
       
        descriptions.stream().forEach((type) -> {
            
            javax.swing.JButton button = new javax.swing.JButton();
            button.setFont(new java.awt.Font("Tahoma", 0, 18));
            button.setMaximumSize(new java.awt.Dimension(109, 25));
            button.setMinimumSize(new java.awt.Dimension(109, 25));
            button.setPreferredSize(new java.awt.Dimension(117, 31));
            
            setSpecificAttributes(button, type);
        });
        
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setResizable(false);
        
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);        
        
        ParallelGroup hGroup = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        buttons.stream().forEach((button) -> {
            hGroup.addComponent(button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        });
        
        layout.setHorizontalGroup(hGroup);
        
        
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        buttons.stream().forEach((button) -> {
            vGroup.addComponent(button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        });
        
        layout.setVerticalGroup(vGroup);

        pack();      
        
        
    }
    
   //! todo documentation
   private void setSpecificAttributes (javax.swing.JButton button, ButtonType type){
       
       if (type == ButtonType.EXIT){
           button.setText("Exit");
           button.addActionListener((java.awt.event.ActionEvent evt) -> {
               ExitButtonActionPerformed();
           });
       }
       else if (type == ButtonType.ABOUT){
           button.setText("About");
           button.addActionListener((java.awt.event.ActionEvent evt) -> {
               AboutButtonActionPerformed();
           });
       }
       else if (type == ButtonType.ATTENTION){
            button.setText("Raise hand");
            button.addActionListener((java.awt.event.ActionEvent evt) -> {
                AttentionButtonActionPerformed(button);
            });
       }
       else if (type == ButtonType.MUTE){
           button.setText("Mute");
           //! todo
       }
       
       else if (type == ButtonType.VOLUME){
           button.setText("Volume");
           //! todo
       }     
       
    }   
   
   //! todo documentation
   private void AboutButtonActionPerformed() {                                                 
        JOptionPane.showMessageDialog(null, "THIS IS ABOUT TEXT!"); 
        //!todo about text
        //! todo will it even work?
       
    }   
   
    //! todo documentation
    private void ExitButtonActionPerformed() {                                                 
        String message = "Do you really want to quit CoUnSil?";
        String title = "Quit CoUnSil?";
        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            //! todo why sleep?             
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(InteractionMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
            dispose();
            System.exit(0);
        }
    }   
   
    //! todo documentation
    private void AttentionButtonActionPerformed(javax.swing.JButton button) {                                                 
        if (raisedHand) {
            button.setText("Raise hand");
            //! todo
        } else {
            button.setText("Lower hand"); 
            //! todo
        }
        raisedHand = !raisedHand;

    }  
   
   
   
   
   
   
   
   
   
   
   
   
   
    
    
    //! remove
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        WantToTalkButton = new javax.swing.JButton();
        DisconnectButton = new javax.swing.JButton();
        ResetLayoutJButton = new javax.swing.JButton();

        jButton1.setText("jButton1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setResizable(false);

        WantToTalkButton.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        WantToTalkButton.setText("Raise hand");
        WantToTalkButton.setMaximumSize(new java.awt.Dimension(109, 25));
        WantToTalkButton.setMinimumSize(new java.awt.Dimension(109, 25));
        WantToTalkButton.setPreferredSize(new java.awt.Dimension(117, 31));
        WantToTalkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                WantToTalkButtonActionPerformed(evt);
            }
        });

        DisconnectButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        DisconnectButton.setText("Quit");
        DisconnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisconnectButtonActionPerformed(evt);
            }
        });

        ResetLayoutJButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        ResetLayoutJButton.setText("Reset layout");
        ResetLayoutJButton.setPreferredSize(new java.awt.Dimension(117, 31));
        ResetLayoutJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetLayoutJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(DisconnectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(WantToTalkButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(ResetLayoutJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(WantToTalkButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ResetLayoutJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DisconnectButton, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void DisconnectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DisconnectButtonActionPerformed

    }//GEN-LAST:event_DisconnectButtonActionPerformed

    private void WantToTalkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_WantToTalkButtonActionPerformed

    }//GEN-LAST:event_WantToTalkButtonActionPerformed

    private void ResetLayoutJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetLayoutJButtonActionPerformed

    }//GEN-LAST:event_ResetLayoutJButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(InteractionMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(InteractionMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(InteractionMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InteractionMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new InteractionMenu().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DisconnectButton;
    private javax.swing.JButton ResetLayoutJButton;
    private javax.swing.JButton WantToTalkButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables



