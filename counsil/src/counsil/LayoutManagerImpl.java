package counsil;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;
import org.json.JSONException;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents structure which manipulates with layout
 *
 * @author xdaxner
 */
public class LayoutManagerImpl implements LayoutManager {

    /**
     * Represents size of inactive border of the window
     */
    final static int WINDOW_BORDER_OFFSET = 50;

    /**
     * List of current active windows
     */
    private List<DisplayableWindow> windows = new ArrayList<>();

    /**
     * Instance of the wddman
     */
    private WDDMan wd;

    /**
     * Instance of CoUnSIl menu
     */
    private InteractionMenu menu;

    /**
     * List of layout manager listeners
     */
    private List<LayoutManagerListener> layoutManagerListeners = new ArrayList<>();

    /**
     * Instance of layout calculator
     */
    private LayoutCalculator calculator;

    /**
     * Listener for mouse events
     */
    private NativeMouseInputListener mouseListener;

    /**
     * Ratio which is applied while scaling windows up/down
     */
    private int scaleRatio;

    /**
     * Initializes layout
     *
     * @param role of this layout
     * @param iml initialMenuLayout to return when counsil exit
     * @param scaleRatio ratio to be scaled
     * @param layoutFile
     * @param languageBundle
     * @param font
     * @throws org.json.JSONException
     * @throws java.io.FileNotFoundException
     * @throws wddman.WDDManException
     * @throws org.jnativehook.NativeHookException
     */
    public LayoutManagerImpl(String role, InitialMenuLayout iml, int scaleRatio, File layoutFile, ResourceBundle languageBundle, Font font) throws JSONException, FileNotFoundException, IOException, WDDManException, NativeHookException {

        this.calculator = new LayoutCalculator(role, layoutFile);
        this.scaleRatio = scaleRatio;

        try {
            this.wd = new WDDMan();
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        // create menu
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (STUDENT.equals(role.toUpperCase())) {
                        menu = new InteractionMenuStudentExtension(calculator.getMenuRole(), calculator.getMenuPostion(), iml, languageBundle, font);
                    } else {
                        menu = new InteractionMenu(calculator.getMenuRole(), calculator.getMenuPostion(), iml, languageBundle, font);
                    }
                    menu.publish();
                    menu.addInteractionMenuListener(new InteractionMenuListener() {

                        @Override
                        public void raiseHandActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                listener.alertActionPerformed();
                            });
                        }

                        @Override
                        public void refreshActionPerformed() {
                            refreshLayout();
                        }
                    });
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        addMouseListener();
    }

    /**
     * Scales window size down
     *
     * @param name
     */
    @Override
    public void downScale(String name) {

        DisplayableWindow window = getDisplayableWindowByTitle(name);
        if (window != null) {
            try {
                window.loadCurrentInfo();

                int newX = window.getPosition().x + getPositionChange();
                int newY = window.getPosition().y + getPositionChange();

                window.setPosition(new Position(newX, newY));
                window.setHeight(window.getHeight() - scaleRatio);
                window.setWidth(window.getWidth() - scaleRatio);

                window.adjustWindow();

            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Gets change in position while resizing
     *
     * @param size
     * @return
     */
    private int getPositionChange() {
        return scaleRatio / 2;
    }

    /**
     * Scales window size up
     *
     * @param name
     */
    @Override
    public void upScale(String name) {

        DisplayableWindow window = getDisplayableWindowByTitle(name);
        if (window != null) {
            try {
                window.loadCurrentInfo();

                int newX = window.getPosition().x - getPositionChange();
                int newY = window.getPosition().y - getPositionChange();

                window.setPosition(new Position(newX, newY));
                window.setHeight(window.getHeight() + scaleRatio);
                window.setWidth(window.getWidth() + scaleRatio);

                window.adjustWindow();

            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Adds new node window to layout
     *
     * @param title window title
     * @param role tole of window user
     */
    @Override
    public void addNode(String title, String role) {
        try {
            synchronized (windows) {
                windows.add(new DisplayableWindow(wd, title, role));
            }
        } catch (WDDManException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * adds listener of layout manager events
     *
     * @param listener
     */
    @Override
    public void addLayoutManagerListener(LayoutManagerListener listener) {
        layoutManagerListeners.add(listener);
    }

    /**
     * Removes window from layout
     *
     * @param title window title
     */
    @Override
    public void removeNode(String title) {
        synchronized (windows) {
            for (Iterator<DisplayableWindow> iter = windows.iterator(); iter.hasNext();) {
                DisplayableWindow window = iter.next();
                if (window.contains(title)) {
                    iter.remove();
                    break;
                }
            }
        }
    }

    /**
     * Recalculates layout and applies changes
     */
    @Override
    public void refreshLayout() {
        synchronized (windows) {
            calculator.recalculate(windows);
        }
        applyChanges();

        setMenuPosition();
    }

    /**
     * Adds mouse listener to Counsil
     *
     * @throws NativeHookException
     */
    private void addMouseListener() throws NativeHookException {

        mouseListener = new NativeMouseInputListener() {
            @Override
            public void nativeMouseClicked(NativeMouseEvent nme) {

                Point clickPoint = nme.getPoint();

                try {
                    if (wasClickedInMenu(clickPoint)) {
                        return;
                    }
                    DisplayableWindow clicked = findClickedWindow(clickPoint);
                    if (clicked != null) {
                        Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, clicked.getTitle() + " was clicked.");
                        sendClickToListeners(clicked, nme.getButton());
                    }
                } catch (WDDManException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void nativeMousePressed(NativeMouseEvent nme) {
                // Layout manager does not use this action, it does not need to implement it
                return;
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent nme) {
                // Layout manager does not use this action, it does not need to implement it
                return;
            }

            @Override
            public void nativeMouseMoved(NativeMouseEvent nme) {
                // Layout manager does not use this action, it does not need to implement it
                return;
            }

            @Override
            public void nativeMouseDragged(NativeMouseEvent nme) {
                // Layout manager does not use this action, it does not need to implement it
                return;
            }

            /**
             * checks if point was clicked in menu
             *
             * @param point
             * @return
             */
            private boolean wasClickedInMenu(Point point) {
                return menu != null
                        && menu.getAlignmentX() < point.x
                        && menu.getAlignmentY() < point.y
                        && menu.getAlignmentX() + menu.getWidth() > point.x
                        && menu.getAlignmentY() + menu.getHeight() > point.y;
            }
        };
        registerMouseListener();
    }

    /**
     * Sends commands to session manager Either to restart consumer(window) or
     * to change its size
     *
     * @param clicked window that was clicked
     * @param button button which was used for click
     */
    private void sendClickToListeners(DisplayableWindow clicked, int button) {
        layoutManagerListeners.stream().forEach((listener) -> {
            try {
                switch (button) {
                    case 1: {
                        if (!STUDENT.equals(calculator.getMenuRole().toUpperCase())
                                && STUDENT.equals(clicked.getRole().toUpperCase())) {
                            listener.windowChoosenActionPerformed(clicked.getTitle());
                        }
                        break;
                    }
                    case 2: {
                        clicked.close();
                    }
                }
            } catch (JSONException | WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    private static final String STUDENT = "STUDENT";

    /**
     * Registers mouse listener
     *
     * @throws NativeHookException
     */
    private void registerMouseListener() throws NativeHookException {
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeMouseListener(mouseListener);
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    /**
     * Finds clicked window and alerts listeners
     */
    private DisplayableWindow findClickedWindow(Point position) throws WDDManException {
        synchronized (windows) {
            for (DisplayableWindow window : windows) {
                window.loadCurrentInfo();
                if ((window.getPosition().x + WINDOW_BORDER_OFFSET <= position.x)
                        && (window.getPosition().y + WINDOW_BORDER_OFFSET <= position.y)
                        && (window.getPosition().x + window.getWidth() - WINDOW_BORDER_OFFSET >= position.x)
                        && (window.getPosition().y + window.getHeight() - WINDOW_BORDER_OFFSET >= position.y)) {
                    return window;
                }
            }
        }
        return null;
    }

    /**
     * Applies calculated layout
     */
    private void applyChanges() {
        synchronized (windows) {
            for (DisplayableWindow window : windows) {
                try {
                    window.adjustWindow();
                } catch (WDDManException ex) {
                    Logger.getLogger(LayoutManagerImpl.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Gets window by title
     *
     * @param title
     * @return
     */
    private DisplayableWindow getDisplayableWindowByTitle(String title) {
        synchronized (windows) {
            for (DisplayableWindow window : windows) {
                if (window.contains(title)) {
                    return window;
                }
            }
        }
        return null;
    }

    /**
     * sets position of the menu
     */
    private void setMenuPosition() {
        if (menu != null) {
            Position position = calculator.getMenuPostion();
            menu.setLocation(position.x, position.y);
        }
    }
}
