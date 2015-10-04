package gui;

/**
 *
 * @author martin
 */

import java.awt.*;
import java.awt.font.TextLayout;
import javax.swing.*;
import java.awt.geom.*;
import javax.swing.plaf.DimensionUIResource;

public class MyButton extends JButton {

    private String myText;
    private boolean isSelected;

    public MyButton(String text) {
        this.myText = text.toUpperCase();
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        this.isSelected = false;
        setPreferredSize(new DimensionUIResource(133, 31));
    }


    public void setText(String s) {
        this.myText = s.toUpperCase();
    }



    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // button colors & gradients
        GradientPaint background = new GradientPaint(0, 0, new Color(255, 255, 255), 0, getPreferredSize().height / 2, new Color(0, 0, 0), true);
        if (getModel().isRollover()) {
            background = new GradientPaint(0, 0, new Color(255, 255, 255), getPreferredSize().width, getPreferredSize().height, new Color(0, 0, 0), true);
        }
        if (getModel().isPressed() || this.isSelected) {
            background = new GradientPaint(0, 0, new Color(255, 255, 255), 0, getPreferredSize().height / 2, new Color(210, 210, 210), true);
        }


        g2.setPaint(background);

        g2.fillRoundRect(1, 1, getPreferredSize().width - 3, getPreferredSize().height - 3, 10, 10);


        super.paint(g);

        // button borders
        if (isEnabled()) { 
            g2.setColor(new Color(255, 255, 255));
        } else {
            g2.setColor(new Color(0, 0, 0));
        }

        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 20.0f));

        g2.draw(new RoundRectangle2D.Double(1, 1, getPreferredSize().width - 3, getPreferredSize().height - 3, 10, 10));

        Font font = new Font("SansSerif", Font.ITALIC, 12);
        g2.setFont(font);
        TextLayout tl = new TextLayout(this.myText, font, g2.getFontRenderContext());
        int startPositionOfText = getPreferredSize().width - (int) tl.getBounds().getWidth();
        startPositionOfText = startPositionOfText / 2;
        
        // button text
        if (!this.isSelected) { 
            g2.setColor(new Color(0, 0, 0));
            g2.drawString(this.myText, startPositionOfText, getHeight() / 2 + 7);

            g2.setColor(new Color(255, 255, 255));
            g2.drawString(this.myText, startPositionOfText - 2, getHeight() / 2 + 5);
        } else {
            g2.setColor(new Color(0, 0, 0));
            g2.drawString(this.myText, startPositionOfText, getHeight() / 2 + 5);
        }

        g2.dispose();

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(133, 31);
    }


    public void setEnabled(boolean b) {
        super.setEnabled(b);

    }

    public void setSelected(boolean b) {
        if(b != this.isSelected){
            this.isSelected = b;
            this.repaint();
        }

    }

    public boolean isSelected() {
        return this.isSelected;
    }
}