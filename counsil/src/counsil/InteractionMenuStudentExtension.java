package counsil;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JButton;

/**
 * Represents menu run in application in student state (with additional buttons)
 * @author xdaxner
 */
public class InteractionMenuStudentExtension extends InteractionMenu {

    /** 
     * @param role
     * @param position
     * @param initialMenu  
     * @param languageBundle  
     * @param font  
     */
    public InteractionMenuStudentExtension(String role, Position position, InitialMenuLayout initialMenu, ResourceBundle languageBundle, Font font) {
      
        super(role, position, initialMenu, languageBundle, font);    
        buttons.add(createAndInitAlertButton());
    }
 
    /**
     * Create and init alert button
     * @return new alert button
     */
    private JButton createAndInitAlertButton() {

        JButton button = new JButton();
        button.setText(languageBundle.getString("RAISE_HAND"));
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
            listener.alertActionPerformed();
        });
    }    
}
