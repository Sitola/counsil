package counsil;

import java.awt.EventQueue;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
     * Lock for gui and other stuff
     */
    final private Object eventLock = new Object();

    /**
     * List of current active windows
     */
    private List<DisplayableWindow> windows = new ArrayList<>();

    /**
     * Instance of wddman
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
     * Calculates layout
     */
    private LayoutCalculator calculator;

    /**
     * Listener for mouse events
     */
    private NativeMouseInputListener mouseListener;

    /**
     * ratio which is applied while scaling windows up/down
     */
    private int scaleRatio;

    /**
     * Initializes layout
     *
     * @param role of this layout
     * @param iml initialMenuLayout to return when counsil exit
     * @throws org.json.JSONException
     * @throws java.io.FileNotFoundException
     * @throws wddman.WDDManException
     * @throws org.jnativehook.NativeHookException
     */
    public LayoutManagerImpl(String role, InitialMenuLayout iml) throws JSONException, FileNotFoundException, IOException, WDDManException, NativeHookException {

        calculator = new LayoutCalculator(role);
        scaleRatio = 10;

        try {
            wd = new WDDMan();
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        // create menu
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {

                    menu = new InteractionMenu(calculator.getMenuRole(), calculator.getMenuPostion(), iml);  
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
     * set ratio
     *
     * @param newRatio
     */
    @Override
    public void initializeScaleRatio(int newRatio) {
        scaleRatio = newRatio;
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
                
                int newX = window.getPosition().x + getPositionChange(window.getHeight());
                int newY = window.getPosition().y + getPositionChange(window.getWidth());

                window.setPosition(new Position(newX, newY));
                window.setHeight(window.getHeight() * (100 - scaleRatio) / 100);
                window.setWidth(window.getWidth() * (100 - scaleRatio) / 100);

                window.adjustWindow();

            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Gets change in position while resizing
     * @param size
     * @return 
     */
    private int getPositionChange(int size){
        return size / scaleRatio / 2;
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
                
                int newX = window.getPosition().x - getPositionChange(window.getHeight());
                int newY = window.getPosition().y - getPositionChange(window.getWidth());

                window.setPosition(new Position(newX, newY));
                window.setHeight(window.getHeight() * 100 / (100 - scaleRatio));
                window.setWidth(window.getWidth() * 100 / (100 - scaleRatio));

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
            synchronized (eventLock) {
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
        synchronized (eventLock) {
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
     * recalculates layout and applies changes
     */
    @Override
    public void refreshLayout() {
        synchronized (eventLock) {
            calculator.recalculate(windows);
            applyChanges();
        }
        setMenuPosition();
    }

    /**
     * add mouse listener to Counsil
     *
     * @throws NativeHookException
     */
    private void addMouseListener() throws NativeHookException {

        mouseListener = new NativeMouseInputListener() {
            @Override
            public void nativeMouseClicked(NativeMouseEvent nme) {
                try {
                    DisplayableWindow clicked = findClickedWindow(nme.getPoint());
                    if (clicked != null){
                        System.err.println(clicked.getTitle() + " was CLICKED!");
                        sendClickToListeners(clicked, nme.getButton());
                    }                   
                } catch (WDDManException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void nativeMousePressed(NativeMouseEvent nme) {
                // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent nme) {
                // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void nativeMouseMoved(NativeMouseEvent nme) {
                // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void nativeMouseDragged(NativeMouseEvent nme) {
                // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            switch (button) {
                case 1: {
                    try {
                        if (!calculator.getMenuRole().equals("student") && clicked.getRole().equals("student")) {                            
                            listener.windowChoosenActionPerformed(clicked.getTitle());
                        }
                    } catch (JSONException ex) {
                        Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
                case 2:
                    listener.windowRestartActionPerformed(clicked.getTitle());
            }
        });
    }

    /**
     * registers mouse listener
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
     * finds clicked window and alerts listeners
     */
    private DisplayableWindow findClickedWindow(Point position) throws WDDManException {
        for (DisplayableWindow window : windows) {
            synchronized (eventLock) {
                window.loadCurrentInfo();
                if ((window.getPosition().x <= position.x)
                        && (window.getPosition().y <= position.y)
                        && (window.getPosition().x + window.getWidth() >= position.x)
                        && (window.getPosition().y + window.getHeight() >= position.y)) {
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
       for (DisplayableWindow window : windows){
            try {
                window.adjustWindow();
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
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
        for (DisplayableWindow window : windows) {
            if (window.contains(title)) {
                return window;
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
