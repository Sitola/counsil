package gui;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 7, 2008
 * Time: 1:08:08 PM
 * To change this template use File | Settings | File Templates.
 */


import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 *
 * @author martin
 */
public class MyButtonPanel extends JPanel{
    protected MyButton backButton = new MyButton(" < BACK ");
    protected MyButton nextButton = new MyButton(" RECONFIGURE ");
    protected MyButton cancelButton = new MyButton(" CANCEL ");

    public MyButtonPanel() {
        this.setLayout(new GridBagLayout());
        addButtons();
        backButton.setEnabled(false);
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

    private void addButtons() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.75;
        c.anchor = GridBagConstraints.EAST;
        this.add( this.backButton, c);

        c.gridx = 1;
        c.weightx = 0.25;
        c.anchor = GridBagConstraints.WEST;
        this.add(this.nextButton, c);

        //c.gridx = 2;
        //c.weightx = 0.25;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 0, 10);
        this.add(this.cancelButton, c);

    }

}