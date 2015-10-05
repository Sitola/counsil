package gui;


import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextLayout;
import javax.swing.*;
import java.awt.geom.*;

public class MyLabel extends JButton {

    private String text;
    private Font font;
    private GradientPaint background;
    private float thicknessOfBorderStroke;
    private boolean focused;

    public MyLabel(String s) {
        super(s);
        this.thicknessOfBorderStroke = 2.0f;
        this.background = new GradientPaint(0, 0,  new Color(255, 255,255) , 0, getPreferredSize().height/2 ,new Color(0, 0, 0), true);
        text = s;
        this.focused = false;
        setBorderPainted(false);
        setContentAreaFilled(false);
        font = new Font("Lucida Bright", Font.ITALIC, 18);
        this.setFont(font);
        setFocusPainted(false);
        this.setOpaque(false);
    }




    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(this.background);
        g2.fillRoundRect(1, 1, getPreferredSize().width - 3, getPreferredSize().height - 3, 40, 40);

        g2.setColor(new Color(255,255, 255));
        g2.setStroke(new BasicStroke(this.thicknessOfBorderStroke, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 10.0f));

        g2.draw(new RoundRectangle2D.Double(1, 1, getPreferredSize().width - 3, getPreferredSize().height - 3, 40, 40));

        TextLayout tl = new TextLayout(text, font, g2.getFontRenderContext());
        int startPositionOfText = getPreferredSize().width - (int) tl.getBounds().getWidth();
        startPositionOfText = startPositionOfText / 2;


        if(! this.focused){
            g2.setColor(new Color(0, 0, 0));
            g2.drawString(text, startPositionOfText +2, getHeight() / 2 + 7 );
            g2.setColor(new Color(255, 255, 255));
            g2.drawString(text , startPositionOfText, getHeight() / 2 + 5 );
        }else{

            g2.setColor(new Color(0, 0, 0));
            g2.drawString(text, startPositionOfText +2, getHeight() / 2 + 7 );
            
            g2.setColor(new Color(255, 255, 255));
            //g2.drawString(text , startPositionOfText, getHeight() / 2 + 5 );
            


        }

        g2.dispose();
    }

    public void paintFocus(){
        this.focused = true;
        this.thicknessOfBorderStroke = 4.0f;

        this.background = new GradientPaint(0, 0,  new Color(255, 255,255),
               0, getPreferredSize().height/2 ,new Color(150, 150, 150), true);
        this.repaint();
    }

    public void unpaintFocus(){
        this.focused = false;
        this.thicknessOfBorderStroke = 2.0f;
        this.background = new GradientPaint(0, 0,  new Color(255, 255,255),
               0, getPreferredSize().height/2 ,new Color(0, 0, 0), true);
        this.repaint();
    }


    @Override
    public Dimension getPreferredSize() {
        return new Dimension(153, 31);
    }


}