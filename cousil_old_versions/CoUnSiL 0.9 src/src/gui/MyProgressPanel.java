package gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author martin
 */
public class MyProgressPanel extends JPanel {
    private int numberOfStates;
    private List<MyLabel> states;
    private int nextState;

    public MyProgressPanel(String[] statesOfProgressPanel) {
        this.nextState = 0;
        this.numberOfStates = statesOfProgressPanel.length;
        states = new ArrayList<MyLabel>(this.numberOfStates);
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);

        for (int i = 0; i < statesOfProgressPanel.length; i++) {
            createState(statesOfProgressPanel[i], i);
        }
    }



    public int next(){
        if(this.nextState <= this.numberOfStates - 1){
            if(this.nextState != 0){
                this.states.get(this.nextState - 1).unpaintFocus();
            }
            this.states.get(this.nextState).paintFocus();
            this.nextState = this.nextState + 1;
        }
        return this.nextState - 1;

    }
    public int back(){
        if(this.nextState > 1){
            this.states.get(this.nextState-1).unpaintFocus();
            if(this.nextState > 1){
                this.states.get(this.nextState - 2).paintFocus();
            }
            this.nextState = this.nextState - 1;
        }
        return this.nextState - 1;
    }


       @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.white);
        g2.fillRect(1, 1, getWidth() - 3, getHeight() - 3);

        GradientPaint redtowhite = new GradientPaint
                (0, this.getPreferredSize().height / 2, new Color(0, 0, 0),
                getPreferredSize().width, getPreferredSize().height, new Color(196, 196, 255),
                false);
        g2.setPaint(redtowhite);
        g2.fillRoundRect(1, 1, (getWidth() - 3), (getHeight() - 3), 40, 40);

        super.paintChildren(g);
        g2.dispose();

    }

    private void createState(String string, int i) {

        if(i == 0){
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            JPanel panel = new JPanel(){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }
            };
            panel.setOpaque(false);
            this.add(panel, c);
        }

        MyLabel myLabel = new MyLabel(string);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2*i+1;
        c.anchor = GridBagConstraints.LINE_START;

        this.add(myLabel, c);
        this.states.add(myLabel);

        GridBagConstraints c1 = new GridBagConstraints();
        c1.gridx = 0;
        c1.gridy = 2*i +2;
        JPanel panel = new JPanel(){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }
        };
        panel.setOpaque(false);
        this.add(panel, c1);

    }
}