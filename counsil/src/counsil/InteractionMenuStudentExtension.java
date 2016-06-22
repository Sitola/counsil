/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import static counsil.InteractionMenu.getResource;
import java.awt.event.ActionEvent;
import java.util.Locale;
import javax.swing.JButton;

/**
 *
 * @author Desanka
 */
public class InteractionMenuStudentExtension extends InteractionMenu {
    
    /**
     * 
     * @param role
     * @param position
     * @param iml  
     */
    public InteractionMenuStudentExtension(String role, Position position, InitialMenuLayout iml) {
        super(role, position, iml);

        buttons.add(createAndInitAlertButton());
    }

    
    /**
     * Create and init alert button
     * @return new alert button
     */
    private JButton createAndInitAlertButton() {

        JButton button = new JButton();
        button.setText(getResource().getString("RAISE_HAND"));
        button.addActionListener((ActionEvent evt) -> {
            alertButtonActionPerformed();
        });
        
        return button;
    }

    
    /**
     * Starts raise/lower hand interaction when button is clicked
     */
    private void alertButtonActionPerformed() {
        interactionMenuListeners.stream().forEach((listener) -> {
            listener.raiseHandActionPerformed();
        });
    }    
}
