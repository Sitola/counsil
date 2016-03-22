/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.EventQueue;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;

/**
 * Represents structure which manipulates with layout
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
    private List<DisplayableWindow> windows;
    
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
     * JSON configure file object
     */
    private JSONObject input;
    
    /*
    * recalculate new layout from JSON layout file and array of nodes or something with specify role 
    * return layout with position nad id|name 
    *
    * it work with fields with ratio < 1, but result may not be as expected, because it 
    * prefer spliting to rows not columbs, if needed can be change in future
    */
    private void recalculate(){
        if(input == null){
            return;
        }
        Map<String, List<DisplayableWindow>> numRoles;
        numRoles = new HashMap();
        JSONObject fields = null;
        try {//try set all roles from input to be filled in
            fields = input.getJSONObject("windows");
        } catch (JSONException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        Vector<String> roles = new Vector<>();
        roles.add("presentation");
        roles.add("interpreter");
        roles.add("teacher");
        roles.add("student");
        
        for(int i=0;i<roles.size();i++){
            numRoles.put(roles.get(i), new ArrayList<>());
        }
        
        //for each role in input put windows to distribute in them
        for (DisplayableWindow win : windows) {
            if(numRoles.containsKey(win.getRole())){
                List<DisplayableWindow> intIncrem = numRoles.get(win.getRole());
                intIncrem.add(win);
            }
        }
        
        //distribute windows in their field
        for(int i=0; i < roles.size(); i++){
            JSONArray roleWindowsConfig;
            int windowWidth = 1;
            int windowHeight = 1;
            int windowX = 0;
            int windowY  = 0;
            List<DisplayableWindow> winList;
            try {
                roleWindowsConfig = fields.getJSONArray(roles.get(i));
            } catch (JSONException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            
            winList = numRoles.get(roles.get(i));
            for (int j=0; j<winList.size();j++) {
                DisplayableWindow win = winList.get(j);
                if(j < roleWindowsConfig.length()){
                    JSONObject windowConfig;
                    try {
                        windowConfig = roleWindowsConfig.getJSONObject(j);
                        windowWidth = windowConfig.getInt("width");
                        windowHeight = windowConfig.getInt("height");
                        windowX = windowConfig.getInt("x");
                        windowY  = windowConfig.getInt("y");
                    } catch (JSONException ex) {
                        Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    win.setHeight(windowHeight);
                    win.setWidth(windowWidth);
                    win.setPosition(new Position(windowX, windowY));
                }
            }
        }
    }
    
    /**
     * Initializes layout
     * @throws org.json.JSONException
     * @throws java.io.FileNotFoundException
     * @throws wddman.WDDManException
     */
    public LayoutManagerImpl() throws JSONException, FileNotFoundException, IOException, WDDManException, NativeHookException{
        
        windows = new ArrayList<>(); 
        try {
            wd = new WDDMan();            
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }  
        
        String entireFileText = new Scanner(new File("layoutConfigStatic.json")).useDelimiter("\\A").next();
        input = new JSONObject(entireFileText);       
        
        // create menu
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {                      
                    menu = new InteractionMenu(getMenuUserRole(), getMenuPostion());
                    menu.addInteractionMenuListener(new InteractionMenuListener() {
                           
                        @Override
                        public void raiseHandActionPerformed() { 
                            layoutManagerListeners.stream().forEach((listener) -> {                                   
                                listener.alertActionPerformed();
                            });
                        }

                        @Override
                        public void muteActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {                             
                                try {
                                    listener.muteActionPerformed(getWindowTitleByRole(getMenuUserRole()));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                               
                            });
                        }

                        @Override
                        public void unmuteActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.unmuteActionPerformed(getWindowTitleByRole(getWindowTitleByRole(getMenuUserRole())));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }); 
                        }

                        @Override
                        public void increaseActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.volumeIncreasedActionPerformed(getWindowTitleByRole(getWindowTitleByRole(getMenuUserRole())));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });                         
                        }

                        @Override
                        public void decreaseActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.volumeDecreasedActionPerformed(getWindowTitleByRole(getWindowTitleByRole(getMenuUserRole())));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });}

                        private String getWindowTitleByRole(String title) {
                            if (getFirstDisplayableWindowByRole("teacher") != null){
                                return getFirstDisplayableWindowByRole("teacher").getTitle();
                            }
                            else if (getFirstDisplayableWindowByRole("interpreter") != null){
                                return getFirstDisplayableWindowByRole("interpreter").getTitle();
                            }
                            else if (getFirstDisplayableWindowByRole("student") != null){
                                return getFirstDisplayableWindowByRole("student").getTitle();
                            }                                  
                            return null;
                        }
                    });
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });     
        
        // adding listener if role is interpreter
        if (getMenuUserRole().equals("interpreter")) {  
                NativeMouseInputListener mouseListener =  new NativeMouseInputListener() {
                @Override
                public void nativeMouseClicked(NativeMouseEvent nme) {
                    Point location = nme.getPoint();                        
                    
                    synchronized(eventLock){  
                        //! find window which was clicked on
                        for (DisplayableWindow window : windows){
                            if (window.getPosition().x <= location.x){
                                if (window.getPosition().y <= location.y){
                                    if (window.getPosition().x + window.getWidth() >= location.x){
                                        if (window.getPosition().y + window.getHeight() >= location.y){
                                            System.err.println(window.getTitle() + " was CLICKED!");
                                            layoutManagerListeners.stream().forEach((listener) -> {
                                                listener.windowChosenActionPerformed(window.getTitle());
                                            });
                                            break;
                                        }
                                    }
                                }
                            }
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
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            GlobalScreen.addNativeMouseListener(mouseListener);
            logger.setUseParentHandlers(false);
        }
                  
        // move windows
        synchronized(eventLock){  
            recalculate();
            applyChanges();
        }
        
    }  
    
    /**
     * gets role from configure file
     * @return role of current user
     * @throws JSONException 
     */
    private String getMenuUserRole() throws JSONException{
        return input.getJSONObject("menu").get("role").toString();
    }

    /**
     * Gets menu position from configure file
     * @return menu position
     * @throws JSONException 
     */
    private Position getMenuPostion() throws JSONException {
        Position position = new Position();
        if(wd == null){
            position.x = 0;
            position.y = 0;
        }else{
            position.x = input.getJSONObject("menu").getInt("x");
            position.y = input.getJSONObject("menu").getInt("y");
        }
        
        return position;
    }
    
   /**
    * Applies calculated layout 
    */
    private void applyChanges(){      
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
     * @param title
     * @return 
     */
    private DisplayableWindow getDisplayableWindowByTitle(String title){
        for (DisplayableWindow window : windows){
            if (window.getTitle().equals(title)){
                return window;
            }
        }
        return null;
    }
    
    /**
     * Gets window by role
     * @param role
     * @return 
     */
    private DisplayableWindow getFirstDisplayableWindowByRole(String role){
        for (DisplayableWindow window : windows){
            if (window.getRole().equals(role)){
                return window;
            }
        }
        return null;
    }
    
    private DisplayableWindow getDisplayableWindowByParameters(String[] parameters){
        for (DisplayableWindow window : windows){
            Boolean match = true;
            for (String parameter : parameters){
                if (!window.getTitle().toUpperCase().contains(parameter.toUpperCase())) {
                    match = false;
                    break;
                }
            }
            if (match = true) return window;
        }
        return null;
    }
           
    
    /**
     * Swaps position of first teacher window and specified window
     * @param title 
     */
    @Override
    public void swapPosition(String title){
        
        String[] paramArray = {"teacher", "video"};
        
        DisplayableWindow teacher = getDisplayableWindowByParameters(paramArray);
        DisplayableWindow student = getDisplayableWindowByTitle(title);
        
        Position pos = teacher.getPosition();
        int width = teacher.getWidth();
        int height = teacher.getHeight();
        
        teacher.setPosition(student.getPosition());
        teacher.setHeight(student.getHeight());
        teacher.setWidth(student.getWidth());
        
        student.setPosition(pos);
        student.setHeight(height);
        student.setWidth(width);
    }
    
     /**
     * Adds new node window to layout
     * @param title window title
     * @param role tole of window user
     */
    @Override
    public void addNode(String title, String role){
        try {
            DisplayableWindow newWin = new DisplayableWindow(wd, title, role);                
            windows.add(newWin);
        } catch (WDDManException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        synchronized(eventLock){  
            recalculate();
            applyChanges();
        } 
    }
    
     /**
     * adds listener of layout manager events
     * @param listener
     */
    @Override
    public void addLayoutManagerListener(LayoutManagerListener listener) {
        layoutManagerListeners.add(listener);
    }
    
    
    /**
     * Removes window from layout
     * @param title window title
     */
    @Override
    public void removeNode(String title){ 
        for (Iterator<DisplayableWindow> iter = windows.iterator(); iter.hasNext();){
            DisplayableWindow window = iter.next();
            if (window.contains(title)) {                               
                iter.remove();
                break;             
            }
        }     
        
        synchronized(eventLock){  
            recalculate();
            applyChanges();
        }
    }
 
    /**
     * Refreshes layout
     */
    @Override
    public void refresh(){
        synchronized(eventLock){     
            applyChanges();
         }
    }
    
}
