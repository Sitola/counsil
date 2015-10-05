package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.font.TextLayout;


public class MyTextLabel extends JLabel {
    private int sizeOfText;
    private int botomInsets;

    public MyTextLabel(String s, int sizeOfText, int botomInsets, int width, int height) {
        super(s);
        if("".equals(s)) throw new IllegalArgumentException("Text of Label is empty.");
        this.setFont(new Font("SansSerif", Font.ITALIC, sizeOfText));
        this.botomInsets = botomInsets;
        this.setPreferredSize( new Dimension(width, height));
        this.setOpaque(true);
    }
    
    public MyTextLabel(String s, int sizeOfText, int botomInsets) {
        super(s);
        if("".equals(s)) throw new IllegalArgumentException("Text of Label is empty.");
        this.setFont(new Font("SansSerif", Font.ITALIC, sizeOfText));
        this.botomInsets = botomInsets;
        this.setOpaque(true);
    }

    public void paint(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(this.getFont());

        TextLayout tl = new TextLayout(getText(), this.getFont(), g2.getFontRenderContext());
        int horizontalPositionOfText = getPreferredSize().width - (int) tl.getBounds().getWidth();

        horizontalPositionOfText = horizontalPositionOfText / 2;

        g2.setColor(new Color(0, 0, 0));
        g2.drawString(getText(), horizontalPositionOfText +1, botomInsets + 1 );

        g2.setColor(new Color(255, 255, 255));
        g2.drawString(getText(), horizontalPositionOfText -1, botomInsets -1);

        //g2.draw(new RoundRectangle2D.Double(1, 1, getPreferredSize().width -3 , getPreferredSize().height - 3 , 5, 5));
        g2.dispose();
    }
}
