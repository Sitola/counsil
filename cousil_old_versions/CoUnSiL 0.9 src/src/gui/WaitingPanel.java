package gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: xtlach
 * Date: Apr 16, 2008
 * Time: 10:36:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class WaitingPanel extends JPanel {

    private JProgressBar progressBar;
    private MyTextLabel textLabel;

    public WaitingPanel(String s) {
        setLayout(new GridBagLayout());
        this.setOpaque(false);
        setPreferredSize(new Dimension(700, 410));
        textLabel = new MyTextLabel(s, 16, 20, 350, 40);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.5;
        c.anchor = GridBagConstraints.PAGE_END;
        this.add(textLabel, c);

        c.gridy = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        this.add(progressBar, c);
    }

    public void setText(String s){
        this.textLabel.setText(s);
        this.repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint backgroundColor = new GradientPaint(0, 0, new Color(0, 0, 0),
                getWidth() - 5, getHeight() - 5, new Color(196, 196, 255),
                true);
        g2.setPaint(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);

        paintChildren(g);
        g2.dispose();
    }
}
