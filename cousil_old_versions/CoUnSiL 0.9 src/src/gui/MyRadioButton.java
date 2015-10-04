package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.TextLayout;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 8, 2008
 * Time: 5:36:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyRadioButton extends JRadioButton {
    private int sizeOfText;
    private Dimension dimensionOfText;
    private String myText;

    public MyRadioButton(String text, boolean switched, int sizeOfText) {
        super("",switched);
        this.myText = text;
        this.sizeOfText = sizeOfText;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setRolloverEnabled(false);
    }

    public void paint(Graphics graphics) {

        Graphics2D g2 = (Graphics2D) graphics;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font = new Font("SansSerif", Font.ITALIC, this.sizeOfText);
        g2.setFont(font);


        TextLayout tl = new TextLayout(this.myText, font, g2.getFontRenderContext());
        int horizontalPositionOfText = getWidth() - (int) tl.getBounds().getWidth();
        int verticalPositionOfText = getHeight() - (int) tl.getBounds().getHeight();
        this.dimensionOfText = new Dimension(horizontalPositionOfText, verticalPositionOfText);
        super.paint(g2);


        //g2.setColor(new Color(255, 255, 255));
        //g2.draw(new RoundRectangle2D.Double(0, 0, getPreferredSize().width - 3, getPreferredSize().height - 3, 30, 30));


        horizontalPositionOfText = horizontalPositionOfText / 2;
        verticalPositionOfText = verticalPositionOfText / 2;

        g2.setColor(new Color(0, 0, 0));
        g2.drawString(this.myText, horizontalPositionOfText +6, verticalPositionOfText + 11 );

        g2.setColor(new Color(255, 255, 255));
        g2.drawString(this.myText, horizontalPositionOfText +4, verticalPositionOfText +9);

        g2.dispose();
    }

    public Dimension getTextDimension(){
        return this.dimensionOfText;
    }


}
