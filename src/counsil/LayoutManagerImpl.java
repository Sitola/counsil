/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
     * JSON configure file object
     */
    private JSONObject input;
    
    /*
    * recalculate new layout from JSON layout file and array of nodes or something with specify role 
    * return layout with position nad id|name 
    * !!!!!!!nearly finished!!!!!!!
    * to do: finish when field is higher then wider; check if I did't forgot implement some future :/
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
            int fieldX = 0;
            int fieldY  = 0;
            String role = null;
            List<DisplayableWindow> winList;
            double windowRatio = 1;             //windows ratio, use same ratio for all windows
            try {
                field = fields.getJSONObject(i);
                fieldWidth = field.getInt("width");
                fieldHeight = field.getInt("height");
                fieldX = field.getInt("x");
                fieldY = field.getInt("y");
                role = field.getString("role");
                windowRatio = field.getDouble("windowRatio");
            } catch (JSONException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            double fieldRatio = fieldWidth / fieldHeight;  //field ratio
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
                int rowsLim = 0;        //number of rows that may be used
                if(fieldRatio/windowRatio >= 1){    // field is better filled with windows horizontly  
                    rowsLim = upperRowLimit(fieldRatio, windowRatio, winList.size());
                    if(unusedSpace(fieldRatio, windowRatio, rowsLim, winList.size()) <= unusedSpace(fieldRatio, windowRatio, rowsLim-1, winList.size())){ // cose if is better use rowsLim rows or rowsLim-1 rows
                        // distribute windows using rowsLim
                        distributeWindows(new Position(fieldX, fieldY), fieldHeight, fieldWidth, windows, rowsLim, fieldRatio, windowRatio);
                    }else{
                        // distribute windows using rowsLim-1
                        distributeWindows(new Position(fieldX, fieldY), fieldHeight, fieldWidth, windows, rowsLim - 1, fieldRatio, windowRatio);
                    }
                }else{                              // field is better filled with windows verticly
                    //to do: if field is better filed horizontly
                }
            }
            
        }
    }
    
    private void distributeWindows(Position fieldPosition, int fieldHeight, int fieldWidth, List<DisplayableWindow> winsToPlace, int rows, double fieldRatio, double windowRatio){
        Vector<Integer> numWindowsInRows = howManyWindowsInRows(winsToPlace.size(), rows);
        int numWindowsPlaced = 0;
        for(int i=0; i<rows; i++){
            Position rowPosition = new Position(fieldPosition.x, fieldPosition.y + (fieldHeight * i) / rows);
            List<DisplayableWindow> windowsInRow = winsToPlace.subList(numWindowsPlaced, numWindowsPlaced + numWindowsInRows.get(i) - 1);
            numWindowsPlaced += numWindowsInRows.get(i);
            distributeWindowsInRow(rowPosition, fieldHeight/rows, fieldWidth, windowsInRow, fieldRatio * rows, windowRatio);    //rowRatio = fieldRatio * rows <= fieldRatio = fieldWidth / fieldHeight , rowRatio = rowWidth / rowHeight, rowHeight = fieldHeight / rows, rowWidth = fieldwidth 
        }
    }
    
    private void distributeWindowsInRow(Position rowPosition, int rowHeight, int rowWidth, List<DisplayableWindow> winsToPlace, double rowRatio, double windowRatio){
        if(rowRatio/windowRatio <= winsToPlace.size()){
            //space under and over the windows
            int windowWidth = rowWidth / winsToPlace.size();
            int windowHeight = (int) Math.round(rowWidth / windowRatio);
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
            int windowWidth = (int) Math.round(windowRatio / windowHeight);
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
        Vector<Integer> numWindowsInRows = new Vector<>(5);   //maximum of 5 rows should be enough
        if (rows > 5){
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, "too many rows");
            return numWindowsInRows;
        }
        if (rows < 1){
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, "too little rows");
            return numWindowsInRows;
        }
        for(int i=0; i<rows; i++){     //calculate how many windows is in every row 
            int windowsInRow;
            if(numWindows % rows > i){
                windowsInRow = 1 - (int)Math.ceil(numWindows/rows);
            }else{
                windowsInRow = 1 - Math.floorDiv(numWindows, rows);
            }
            numWindowsInRows.set(i, windowsInRow);
        }
        return numWindowsInRows;
    }
    
    
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   rows - # rows
    */
    private double unusedSpace(double R, double r, int rows, int n){
        Vector<Integer> numWindowsInRows = howManyWindowsInRows(n, rows);   
        
        double sumEmptySpace = 0;
        for(int i=0; i<rows; i++){
            if((R*rows)/r <= numWindowsInRows.get(i)){  //choose if is empty space on sides or over and under the windows
                sumEmptySpace += 1 - unusedSpaceUnder(R, r, rows, numWindowsInRows.get(i));
            }else{
                sumEmptySpace += 1 - unusedSpaceLeft(R, r, rows, numWindowsInRows.get(i));
            }
        }
        return sumEmptySpace/rows;  //return everage of rows empty space
    }
    
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   k - # of rows
    */
    private double unusedSpaceUnder(double R, double r, int k, int n){
        return (R*k)/(r * Math.ceil(n/k));    // (R * k) / (r * ⌊n/k⌋)
    }
       
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    *   k - # of rows
    */
    private double unusedSpaceLeft(double R, double r, int k, int n){
        return (R * k * Math.ceil(n/k))/(r );    // (R * k * ⌊n/k⌋) / r
    }       
    
    /*
    *   R - field ratio
    *   r - window ratio
    *   n - # of windows
    */
    private int upperRowLimit(double R, double r, int n){
        int k = 1;  //# of rows
        while(k * Math.floor((R*k)/r) >= n){     //  k * ⌈(R*k)/r⌉ == n limit  
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
              
        String entireFileText = new Scanner(new File("layoutConfig.json")).useDelimiter("\\A").next();
        input = new JSONObject(entireFileText);       
        
        menu = new InteractionMenu(getMenuUserRole(), getMenuPostion());  
               
        try {
            wd = new WDDMan();
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }  
        
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
     * Gets menu position from configure file
     * @return menu position
     * @throws JSONException 
     */
    private Position getMenuPostion() throws JSONException {
        Position position = new Position();
        position.x = (int) input.getJSONObject("menu").get("x");
        position.y = (int) input.getJSONObject("menu").get("y");
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
            windows.add(newWin);
        } catch (WDDManException | UnsupportedOperatingSystemException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        recalculate(); 
        applyChanges();

    }
    
    /**
     * Removes window from layout
     * @param title window title
     */
    @Override
    public void removeNode(String title){
        
        wddman.Window wddWin;
        
        try {
            wddWin = wd.getWindowByTitle(title);
            for (DisplayableWindow window : windows){           
                
                if (window.contains(wddWin)) {
                    windows.remove(window);
                    break;
                }             
            }
        } catch (WDDManException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
          recalculate(); 
          applyChanges();
    }

 
   
}
