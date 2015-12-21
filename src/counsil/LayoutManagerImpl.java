/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.awt.EventQueue;
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
        JSONArray fields = null;
        try {//try set all roles from input to be filled in
            fields = input.getJSONArray("fields");
            for(int i=0; i < fields.length(); i++){
                numRoles.put(fields.getJSONObject(i).getString("role"), new ArrayList<>());
            }
        } catch (JSONException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //for each role in input put windows to distribute in them
        for (DisplayableWindow win : windows) {
            if(numRoles.containsKey(win.getRole())){
                List<DisplayableWindow> intIncrem = numRoles.get(win.getRole());
                intIncrem.add(win);
                //numRoles.replace(win.getRole(), intIncrem); //not shure if needed
            }
        }
        //distribute windows in their field
        for(int i=0; i < fields.length(); i++){
            JSONObject field;
            int fieldWidth = 1;
            int fieldHeight = 1;
            double relativeFieldWidth = 1.0;
            double relativeFieldHeight = 1.0;
            double relativeFieldX = 0.0;
            double relativeFieldY = 0.0;
            boolean menuOnLeft = false;
            int borderR = 0;
            int borderL = 0;
            int borderU = 0; 
            int borderD = 0;
            int fieldX = 0;
            int fieldY  = 0;
            String role = null;
            List<DisplayableWindow> winList;
            double windowRatio = 1;             //windows ratio, use same ratio for all windows
            try {
                field = fields.getJSONObject(i);
                relativeFieldWidth = field.getDouble("width");
                relativeFieldHeight = field.getDouble("height");
                if(field.getString("menuSide").compareTo("left") == 0){
                    menuOnLeft = true;
                }
                borderR = field.getInt("borderR");
                borderL = field.getInt("borderL");
                borderU = field.getInt("borderU");
                borderD = field.getInt("borderD");
                relativeFieldX = field.getDouble("x");
                relativeFieldY = field.getDouble("y");
                role = field.getString("role");
                windowRatio = field.getDouble("windowRatio");
            } catch (JSONException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                fieldHeight = (int) (wd.getScreenHeight() * relativeFieldHeight);
                fieldWidth = (int) (wd.getScreenWidth() * relativeFieldWidth);
                fieldX = (int) (wd.getScreenWidth() * relativeFieldX);
                fieldY = (int) (wd.getScreenHeight() * relativeFieldY);
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            //check if menu is not in field, if is make field smaller
            if(menu != null){
                if(menuOnLeft){
                    if(fieldX < menu.getX() + menu.getWidth()){
                        //check if menu and field are on same height
                        if((fieldY < menu.getY() + menu.getHeight()) && (fieldY > menu.getY()) || ((menu.getY() < fieldY + fieldHeight) && (menu.getY() > fieldY))){
                            System.out.println("nuf1");
                            fieldWidth -= menu.getX() + menu.getWidth() - fieldX; 
                            fieldX = menu.getX() + menu.getWidth();
                        }
                    }
                }else{
                    if(fieldX + fieldWidth > menu.getX()){
                        //check if menu and field are on same height
                        if((fieldY < menu.getY() + menu.getHeight()) && (fieldY > menu.getY()) || ((menu.getY() < fieldY + fieldHeight) && (menu.getY() > fieldY))){
                            System.out.println("nuf2");
                            fieldWidth -= fieldX + fieldWidth - menu.getX();
                        }
                    }
                }
            }
            
            //move and shrink actual field depending on boarders size
            //could have add more checks, but for now is not necessary 
            fieldHeight = fieldHeight - borderU - borderD;
            fieldWidth = fieldWidth - borderL - borderR;
            fieldX = fieldX + borderL;
            fieldY = fieldY + borderU;
            if(fieldHeight < 1){
                fieldHeight = 1;
            }
            if(fieldWidth < 1){
                fieldWidth = 1;
            }
            
            double fieldRatio = (double)fieldWidth / (double)fieldHeight;  //field ratio
            winList = numRoles.get(role);           //list of windows to distribute in this field
            
            //distribution of windows in field
            if(winList.size() == 0){
                
            }else if(winList.size() == 1){        //# windows = 1
                //centralize / fill field with window
                DisplayableWindow win = winList.get(0);
                win.setPosition(new Position(fieldX, fieldY));
                win.setWidth(fieldWidth);
                win.setHeight(fieldHeight);
            }else{  //# windows > 1
                int rowsLim;        //number of rows that may be used
               // if(fieldRatio/windowRatio >= 1){    // field is better filled with windows horizontly 
                //it work with fields with ratio < 1, but result may not be as expected, because it prefer spliting to rows not columbs, if needed can be change in future 
                    rowsLim = upperRowLimit(fieldRatio, windowRatio, winList.size());
                    if(unusedSpace(fieldRatio, windowRatio, rowsLim, winList.size()) <= unusedSpace(fieldRatio, windowRatio, rowsLim-1, winList.size())){ // chose if is better use rowsLim rows or rowsLim-1 rows
                        // distribute windows using rowsLim
                        distributeWindows(new Position(fieldX, fieldY), fieldHeight, fieldWidth, winList, rowsLim, fieldRatio, windowRatio);
                    }else{
                        // distribute windows using rowsLim-1
                        distributeWindows(new Position(fieldX, fieldY), fieldHeight, fieldWidth, winList, rowsLim - 1, fieldRatio, windowRatio);
                    }
            //    }else{                              // field is better filled with windows verticly
                    //to do: if field is better filed horizontly
               // }
            }
            
        }
    }
    
    private void distributeWindows(Position fieldPosition, int fieldHeight, int fieldWidth, List<DisplayableWindow> winsToPlace, int rows, double fieldRatio, double windowRatio){
        Vector<Integer> numWindowsInRows = howManyWindowsInRows(winsToPlace.size(), rows);
        int numWindowsPlaced = 0;
        for(int i=0; i<rows; i++){
            Position rowPosition = new Position(fieldPosition.x, fieldPosition.y + (fieldHeight * i) / rows);
            List<DisplayableWindow> windowsInRow = winsToPlace.subList(numWindowsPlaced, numWindowsPlaced + numWindowsInRows.get(i));
            numWindowsPlaced += numWindowsInRows.get(i);
            distributeWindowsInRow(rowPosition, fieldHeight/rows, fieldWidth, windowsInRow, fieldRatio * (double)rows, windowRatio);    //rowRatio = fieldRatio * rows <= fieldRatio = fieldWidth / fieldHeight , rowRatio = rowWidth / rowHeight, rowHeight = fieldHeight / rows, rowWidth = fieldwidth 
        }
    }
    
    private void distributeWindowsInRow(Position rowPosition, int rowHeight, int rowWidth, List<DisplayableWindow> winsToPlace, double rowRatio, double windowRatio){
        if(rowRatio/windowRatio <= winsToPlace.size()){
            //space under and over the windows
            int windowWidth = rowWidth / winsToPlace.size();
            int windowHeight = (int) Math.round(windowWidth / windowRatio);
            System.out.println(windowHeight);
            int lastPosition = rowPosition.x;
            for (DisplayableWindow win : winsToPlace) {
                Position windowPosition = new Position(lastPosition, rowPosition.y + (rowHeight - windowHeight) / 2);
                win.setPosition(windowPosition);
                win.setHeight(windowHeight);
                win.setWidth(windowWidth);
                lastPosition += windowWidth;
            }
        }else{
            //space left and right of windows
            int windowHeight = rowHeight;
            int windowWidth = (int) Math.round(windowRatio * windowHeight);
            int freeSpace = rowWidth - windowWidth * winsToPlace.size();
            int lastPosition = rowPosition.x + freeSpace / (winsToPlace.size() + 1);
            for (DisplayableWindow win : winsToPlace) {
                Position windowPosition = new Position(lastPosition, rowPosition.y);
                win.setPosition(windowPosition);
                win.setHeight(windowHeight);
                win.setWidth(windowWidth);
                lastPosition += windowWidth + freeSpace / (winsToPlace.size() + 1);
            }
        }
    }
    
    /*
    *   calculate how to evenly split windows in rows
    *   maximum 5 rows
    *   return example vector[1] == 5 mean in second row is 5 windows
    */
    private Vector<Integer> howManyWindowsInRows(int numWindows, int rows){
        Vector<Integer> numWindowsInRows = new Vector<>(5, 3);   //maximum of 5 rows should be enough, vector increese by 3 if needed
        if (rows < 1){
            return numWindowsInRows;
        }
        for(int i=0; i<rows; i++){     //calculate how many windows is in every row 
            int windowsInRow = (numWindows / rows);
            if(numWindows % rows > i){
                windowsInRow  += 1;
            }
            numWindowsInRows.add(i, windowsInRow);
        }
        return numWindowsInRows;
    }
    
    
    /*
    *   @return should be value between 0 and 1
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   rows - # rows
    */
    private double unusedSpace(double R, double r, int rows, int n){
        Vector<Integer> numWindowsInRows = howManyWindowsInRows(n, rows);
        double sumEmptySpace = 0;
        for(int i=0; i<rows; i++){
            //R*rows - row ratio; r * n - ratio of windows in row
            if((R*rows) <= (r * numWindowsInRows.get(i))){  //choose if is empty space on sides or over and under the windows
                sumEmptySpace += 1 - unusedSpaceUnder(R, r, rows, numWindowsInRows.get(i));
            }else{
                sumEmptySpace += 1 - unusedSpaceLeft(R, r, rows, numWindowsInRows.get(i));
            }
        }
        if(rows != 0 ){
            return sumEmptySpace/rows;  //return everage of rows empty space
        }else{
            return 1;       //return information all spaces are unused
        }

    }
    
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   k - # of rows
    */
    private double unusedSpaceUnder(double R, double r, int k, int n){
        return (R*k)/(r*n);    // (R * k) / (r * n⌋)
    }
       
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   k - # of rows 
    */
    private double unusedSpaceLeft(double R, double r, int k, int n){
        return (n * r)/(R * k);    // (n* r) / (R * k)
    }       
    
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    */
    private int upperRowLimit(double R, double r, int n){
        int k = 1;  //# of rows
        while((k * Math.floor((R*k)/r)) < n){     //  k * ⌈(R*k)/r⌉ == n limit  
            k++;
        }
        return k;
    }
    
    /**
     * Inicializes layout
     * @throws org.json.JSONException
     * @throws java.io.FileNotFoundException
     */
    public LayoutManagerImpl() throws JSONException, FileNotFoundException, IOException{
       
        
        windows = new ArrayList<>(); 

        try {
            wd = new WDDMan();            
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }  
        
        String entireFileText = new Scanner(new File("layoutConfig.json")).useDelimiter("\\A").next();
        input = new JSONObject(entireFileText);       
        EventQueue.invokeLater(new Runnable() {
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
                                    listener.muteActionPerformed(getWindowTitleByRole(input.getJSONObject("menu").get("role").toString()));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });
                        }

                        @Override
                        public void unmuteActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.unmuteActionPerformed(getWindowTitleByRole(input.getJSONObject("menu").get("role").toString()));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }); 
                        }

                        @Override
                        public void increaseActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.volumeIncreasedActionPerformed(getWindowTitleByRole(input.getJSONObject("menu").get("role").toString()));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });                         
                        }

                        @Override
                        public void decreaseActionPerformed() {
                            layoutManagerListeners.stream().forEach((listener) -> {
                                try {
                                    listener.volumeDecreasedActionPerformed(getWindowTitleByRole(input.getJSONObject("menu").get("role").toString()));
                                } catch (JSONException ex) {
                                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });}

                        private String getWindowTitleByRole(String title) {
                            if (title.equals("teacher")){
                                for (DisplayableWindow win : windows){
                                    if (win.getRole().equals("interpreter")){
                                        return win.getTitle();
                                    }
                                }
                            }
                            else if (title.equals("interpreter")){
                                 for (DisplayableWindow win : windows){
                                    if (win.getRole().equals("teacher")){
                                        return win.getTitle();
                                    }
                                }
                            }                                                
                            return null;
                        }
                    });
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });     
      
<<<<<<< HEAD

=======
        try {
            wd = new WDDMan();            
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }  
>>>>>>> c03428c059c1e9c44415fc9942027f06fcb55295
        
        recalculate();
        applyChanges();
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
     * User is alerting
     * @param node
     * @throws WDDManException 
     */
    @Override
    public void alert(String node) throws WDDManException{
        windows.stream().filter((w) -> (w.contains(node))).forEach((w) -> {
            w.alert();
        });
    }
    
    /**
     * User is talking
     * @param node
     * @throws WDDManException 
     */
    @Override
    public void talk(String node)throws WDDManException{
        windows.stream().filter((w) -> (w.contains(node))).forEach((w) -> {
            w.talk(); 
        });
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
            try {
                position.x = (int) (input.getJSONObject("menu").getDouble("x") * wd.getScreenWidth());
                position.y = (int) (input.getJSONObject("menu").getDouble("y") * wd.getScreenHeight());
                if(position.x + 150 > wd.getScreenWidth() ){  //150 is set by menu creating in InteractionMenu.java if change have to update here also
                    position.x = wd.getScreenWidth() - 150;
                }
                if(position.y + 500 > wd.getScreenHeight()){  //500 is set by menu creating in InteractionMenu.java if change have to update here also
                    position.y = wd.getScreenHeight()- 500;
                }
            } catch (WDDManException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
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
     * Adds new node window to layout
     * @param title window title
     * @param role tole of window user
     */
    @Override
    public void addNode(String title, String role){
        try {
            DisplayableWindow newWin = new DisplayableWindow(wd, title, role);    
            newWin.addWindowClickEventListener(new WindowClickEventListener() {
               
                @Override
                public void windowClickActionPerformed() {
                   layoutManagerListeners.stream().forEach((listener) -> {
                    listener.windowChosenActionPerformed(newWin.getTitle());
                   });
                }

                               
            });
            windows.add(newWin);
        } catch (WDDManException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        recalculate(); 
        applyChanges();
        applyChanges();

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
        
          recalculate(); 
          applyChanges();
    }
 
   
}
