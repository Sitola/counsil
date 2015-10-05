package gui.mapVisualization;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 7, 2008
 * Time: 1:08:08 PM
 * To change this template use File | Settings | File Templates.
 */


import gui.MyButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.plaf.DimensionUIResource;

/**
 *
 * @author martin
 */
public class BottomPanel extends JPanel {
    public MyButton addApplicationButton = new MyButton("Add application");
    public MyButton removeApplicationButton = new MyButton("Remove Application");
    public MyButton selectSourceButton = new MyButton("Select Source");

    public MyButton goHomeButton = new MyButton("Go Home");

    
    public BottomPanel(boolean init) {
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new DimensionUIResource(800, 45));
        this.setMinimumSize(new DimensionUIResource(600, 35));
        
        if(init){
            init();
        }
        this.setOpaque(true);
    }


    private void init() {
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.2;
        c.anchor = GridBagConstraints.EAST;
        this.add( this.addApplicationButton, c);


        c.weightx = 0.1;
        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        this.add(this.removeApplicationButton, c);

        c.weightx = 0.1;
        c.gridx = 2;
        this.add(this.selectSourceButton, c);

        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.6;
        c.gridx = 3;
        this.add(this.goHomeButton, c);
        
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
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);


        g2.setColor(new Color(0,0, 0));
        Font font = new Font("Lucida Bright", Font.ITALIC, 14);
        g2.setFont(font);

        /*
        g2.setColor(new Color(0, 0, 0));
        g2.drawString("© tlachy@mail.muni.cz", 10 , getHeight() / 2 + 7 );


        g2.setColor(new Color(255, 255, 255));
        g2.drawString("© tlachy@mail.muni.cz" , 12, getHeight() / 2 + 5 );
        */
        
        paintChildren(g2);
        g.dispose();
    }

}