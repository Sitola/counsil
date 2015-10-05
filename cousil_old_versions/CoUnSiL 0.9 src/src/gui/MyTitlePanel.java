package gui;


import java.awt.*;
import java.awt.font.TextLayout;
import javax.swing.JPanel;

/**
 * 
 * @author martin
 */
public class MyTitlePanel extends JPanel {

    private Dimension origins;
    private int sizeOfText;


    public MyTitlePanel(int sizeOfText) {
        this.origins = new Dimension(810, 70);
        setPreferredSize(this.origins);
        this.sizeOfText = sizeOfText;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setPaint(Color.white);
        g2.fillRect(1, 1, getWidth() - 3, getHeight() - 3);

        GradientPaint background = new GradientPaint(0, 0,  new Color(255, 255, 255),
                0, getHeight() / 2 ,new Color(0, 0, 0), true);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(background);
        g2.fillRoundRect(0, 0,  getWidth() ,  getHeight(), 40, 40);


        g2.setColor(new Color(0,0, 0));
        
        //double x = getWidth() / this.origins.getWidth() ;
        //int plus = (int) ((x - 1) / 0.025);

        Font font = new Font("Lucida Bright", Font.ITALIC, sizeOfText + 2);
        g2.setFont(font);
        TextLayout tl = new TextLayout("Collaboration Universe", font, g2.getFontRenderContext());
        int startPositionOfText = getWidth() - (int) tl.getBounds().getWidth();
        startPositionOfText = startPositionOfText / 2 ;

        g.setColor(new Color(0, 0, 0));
        g2.drawString("Collaboration Universe", startPositionOfText +2 , getHeight() / 2 + 12  );


        g2.setColor(new Color(255, 255, 255));
        g2.drawString("Collaboration Universe" , startPositionOfText, getHeight() / 2 + 10 );

        g.dispose();
    }
}