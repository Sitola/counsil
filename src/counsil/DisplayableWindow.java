/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JWindow;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents displayable window - pair of transparent and non-transparent
 * windows
 *
 * @author desanka
 */
class DisplayableWindow extends JFrame {

    /**
     * Height of window
     */
    private int height;

    /**
     * Width of window
     */
    private int width;

    /**
     * Position of winodw
     */
    private Position position;

    /**
     * Window role
     */
    public String role;

    /**
     * True if window is currently talking, false otherwise
     */
    private boolean talking;

    /**
     * True if window wants to speak, false otherwise
     */
    private boolean alerting;

    /**
     * Content window
     */
    wddman.Window content;

    /**
     * Top transparent window
     */
    JWindow transparent;

    /**
     * Window title
     */
    String title;

    /**
     * list of window on click listeners
     */
    private List<WindowClickEventListener> windowListeners;

    /**
     * Initializes arguments, creates transparent window to non-transparent
     * window
     *
     * @param title title of new window
     * @param role role of window
     * @throws WDDManException
     * @throws UnsupportedOperatingSystemException
     */
    DisplayableWindow(WDDMan wd, String title, String role) throws WDDManException, UnsupportedOperatingSystemException {
        this.windowListeners = new ArrayList<>();

        while ((content = wd.getWindowByTitle(title)) == null) {
            System.err.println("My name should be" + title);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(DisplayableWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        position = new Position(content.getLeft(), content.getTop());
        width = content.getWidth();
        height = content.getHeight();

        this.title = title;
        this.role = role;
        talking = false;
        alerting = false;

        /*transparent = new JWindow();
        transparent.setName(title + "TW");
        transparent.setLocationRelativeTo(null);
        transparent.setSize(width, height);
        transparent.setLocation(position.x, position.y);
        transparent.setAlwaysOnTop(true);
        transparent.setBackground(new Color(0, 0, 0, (float) 0.0025));
        transparent.setVisible(true);

        if (role.equals("teacher")) {
            transparent.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    windowListeners.stream().forEach((listener) -> {
                        listener.windowClickActionPerformed();
                    });
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });
        }*/

    }

    /**
     * adds listener of onclick window event
     *
     * @param listener
     */
    public void addWindowClickEventListener(WindowClickEventListener listener) {
        windowListeners.add(listener);
    }

    /**
     * Applies DisplayableWindow parameters to actual windows
     *
     * @param wd wddman instance
     * @throws WDDManException
     */
    public void adjustWindow(WDDMan wd) throws WDDManException {

        System.out.print("W: " + width + " H: " + height + " X: " + position.x + " Y: " + position.y + " R: " + title + "\n");

        content.move(position.x, position.y);
        content.resize(position.x, position.y, width, height);

        /*transparent.setSize(width, height);
        transparent.setLocation(position.x, position.y);
        transparent.setBackground(new Color(0, 0, 0, (float) 0.0025));*/
    }

    /**
     * Un/shows blue frame if was user chosen to speak
     */
    public void talk() {
        /*if (talking) {
            transparent.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        } else {
            alerting = false;
            transparent.getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLUE));
        }
        talking = !talking;*/

    }

    /**
     * Un/shows red frame if user wants to speak
     */
    public void alert() {
        /*if (alerting) {
            transparent.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        } else {
            transparent.getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.RED));
        }
        alerting = !alerting;*/

    }

    /**
     * Checks if DisplayableWindow is associated with argument title
     *
     * @param title
     * @return true, if Displayable window contains wddman window
     */
    public Boolean contains(String title) {
        return title.equals(this.title);

    }

    Position getPosition() {
        return position;
    }

    void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public wddman.Window getContent() {
        return content;
    }

    public JWindow getTransparent() {
        return transparent;
    }

    public String getRole() {
        return role;
    }

}
