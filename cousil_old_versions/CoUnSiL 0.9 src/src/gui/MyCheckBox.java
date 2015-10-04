package gui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;

/**
 *
 * @author martin
 */
public class MyCheckBox extends javax.swing.JCheckBox {
    private String myText;
    private int shift;

    public MyCheckBox(String text, boolean state) {
        super("", state);
        if("".equals(text)) throw new IllegalArgumentException("Text of Label is empty.");        
        this.myText = text;
        setFont(new Font("Lucida Bright", Font.ITALIC, 15));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        this.setPreferredSize(new Dimension(320, 27));
        this.shift = 5;
        setRolloverEnabled(false);
    }





    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint redtowhite = new GradientPaint(0, 0, new Color(255, 255, 255), 0, getPreferredSize().height / 2, new Color(0, 0, 0), true);
        g2.setPaint(redtowhite);

        g2.fillRoundRect(1, 1, getPreferredSize().width - 3 , getPreferredSize().height - 3, 10, 10);


        super.paint(g);

        g2.setColor(new Color(255, 255, 255));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 10.0f));

        g2.draw(new RoundRectangle2D.Double(1, 1, getPreferredSize().width - 3, getPreferredSize().height - 3, 10, 10));

        Font font = new Font("Lucida Bright", Font.ITALIC, 15);
        TextLayout tl = new TextLayout(getMyText(), font, g2.getFontRenderContext());
        int startPositionOfText = getPreferredSize().width - (int) tl.getBounds().getWidth();
        startPositionOfText = startPositionOfText / 2;
        g2.setColor(new Color(0, 0, 0));

        g2.drawString(getMyText(), startPositionOfText + 9, getHeight() / 2 + 7);


        g2.setColor(new Color(255, 255, 255));

        g2.drawString(getMyText(), startPositionOfText + 7, getHeight() / 2 + 5);

        g2.dispose();
        this.setIgnoreRepaint(true);

    }


    public String getMyText() {
        return this.myText;
    }
}