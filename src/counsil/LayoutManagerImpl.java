/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * @author desanka, xminarik
 */
public class LayoutManagerImpl implements LayoutManager {

    /**
     * Lock for gui and other stuff
     */
    final private Object eventLock = new Object();

    /**
     * List of current active windows
     */
    private List<DisplayableWindow> windows = new ArrayList<>();;

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

    private LayoutCalculator calculator;

    /**
     * Initializes layout
     *
     * @throws org.json.JSONException
     * @throws java.io.FileNotFoundException
     * @throws wddman.WDDManException
     * @throws org.jnativehook.NativeHookException
     */
    public LayoutManagerImpl() throws JSONException, FileNotFoundException, IOException, WDDManException, NativeHookException {

        calculator = new LayoutCalculator();
        
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
                    menu = new InteractionMenu(calculator.getMenuRole(), calculator.getMenuPostion());                  
                    menu.addInteractionMenuListener(new InteractionMenuListener() {

                        @Override
                        public void raiseHandActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                listener.alertActionPerformed();
                            });
                        }
                    });
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        // adding listener if role is interpreter
        NativeMouseInputListener mouseListener = new NativeMouseInputListener() {
            @Override
            public void nativeMouseClicked(NativeMouseEvent nme) {
                Point location = nme.getPoint();
                int button = nme.getButton();

                //! find window which was clicked on
                for (DisplayableWindow window : windows) {
                    if ((window.getPosition().x <= location.x)
                            && (window.getPosition().y <= location.y)
                            && (window.getPosition().x + window.getWidth() >= location.x)
                            && (window.getPosition().y + window.getHeight() >= location.y)){

                        System.err.println(window.getTitle() + " was CLICKED!");                     
                        layoutManagerListeners.stream().forEach((listener) -> {
                            try {
                                if ((button == 1) 
                                    && (calculator.getMenuRole().equals("interpreter") || calculator.getMenuRole().equals("teacher"))
                                    && (!window.getRole().equals("interpreter")) 
                                    && (!window.getTitle().contains("PRESENTATION"))) {
                                    
                                    listener.windowChoosenActionPerformed(window.getTitle());
                                } else if (button == 2) {
                                    listener.windowRestartActionPerformed(window.getTitle());
                                }                                    
                            } catch (JSONException ex) {
                                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                        break;
                    }                         
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

        // register listener to screen, turn off loggers
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeMouseListener(mouseListener);
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        recalculateAndApply();
    }

    /**
     * Applies calculated layout
     */
    private void applyChanges() {
        windows.stream().forEach((window) -> {
            try {
                window.adjustWindow(wd);
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    /**
     * Gets window by title
     *
     * @param title
     * @return
     */
    private DisplayableWindow getDisplayableWindowByTitle(String title) {
        for (DisplayableWindow window : windows) {
            if (window.getTitle().equals(title)) {
                return window;
            }
        }
        return null;
    }

    /**
     * Swaps position of new and old window and specified window
     *
     * @param newWindowName
     * @param oldWindowName
     */
    @Override
    public void swapPosition(String newWindowName, String oldWindowName) {

        DisplayableWindow newWindow = getDisplayableWindowByTitle(newWindowName);
        DisplayableWindow oldWindow = getDisplayableWindowByTitle(oldWindowName);
        
         System.out.print("Talking node:" + oldWindowName);
        if (oldWindow != null){            

            String tempRole = newWindow.getRole();
            newWindow.setRole(oldWindow.getRole());
            oldWindow.setRole(tempRole);

            Position temporaryPosition = newWindow.getPosition();
            System.out.print("Temp position after init " + temporaryPosition.x + " " + temporaryPosition.y);
            newWindow.setPosition(oldWindow.getPosition());
            System.out.print("Temp position after newWinset " + temporaryPosition.x + " " + temporaryPosition.y);
            System.out.print("New position after init " + newWindow.getPosition().x + " " + newWindow.getPosition().y);

            oldWindow.setPosition(temporaryPosition);

            int temporarySize = newWindow.getWidth();
            newWindow.setWidth(oldWindow.getWidth());
            oldWindow.setWidth(temporarySize);
            temporarySize = newWindow.getHeight();
            newWindow.setHeight(oldWindow.getHeight());
            oldWindow.setHeight(temporarySize);

            refresh();
        }
        else {
            newWindow.setRole("teacher");
            recalculateAndApply();
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
                DisplayableWindow newWin = new DisplayableWindow(wd, title, role);
                windows.add(newWin);
            }
        } catch (WDDManException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        recalculateAndApply();
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
        recalculateAndApply();
    }

    /**
     * Refreshes layout
     */
    @Override
    public void refresh() {
        synchronized (eventLock) {
            applyChanges();
        }
    }

    /**
     * recalculates layout and applies changes
     */
    private void recalculateAndApply() {
        synchronized (eventLock) {
            calculator.recalculate(windows);
            applyChanges();
        }
    }

    /**
     * refreshes layout to default position
     */
    @Override
    public void refreshToDefaultLayout() {
        
        for (DisplayableWindow win : windows){
            win.setRole(win.getDefaultRole());
        }
        recalculateAndApply();
    }

}
