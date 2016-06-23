/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Huvart
 */
public class CustomPreviewPanel extends JPanel{
    JLabel j1 = new JLabel("This is a custom preview pane", JLabel.CENTER);

    JLabel j2 = new JLabel("", JLabel.CENTER);
    

    public CustomPreviewPanel(Dimension size) {
        super(new GridBagLayout());
        setPreferredSize(size);
        super.setBorder(new SoftBevelBorder(0));
    }

    public void setForeground(Color c) {
        super.setForeground(c);
        super.setBackground(c);
    }
}
