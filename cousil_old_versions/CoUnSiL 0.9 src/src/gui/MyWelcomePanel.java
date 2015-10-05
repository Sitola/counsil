package gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 7, 2008
 * Time: 7:05:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyWelcomePanel extends JPanel {

    public MyWelcomePanel() {
        this.add(new JLabel(new ImageIcon("src/GUI/pictures/logo-sitola.png")));
        this.setPreferredSize(new Dimension(600, 310));
    }
    
}
