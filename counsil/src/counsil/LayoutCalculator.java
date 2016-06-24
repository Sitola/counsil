/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.io.File;
import java.io.FileNotFoundException;
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

/**
 * Class that calculates layout
 * @author Dax
 */
public class LayoutCalculator {
    
    /**
     * JSON configure file object
     */
    private final JSONObject input;
    
    /**
     * Role of current Counsil session
     */
    private final String role;
    
    /**
     * current menu position
     */
    private Position menuPosition;
    
    /**
     * Creates layout calculator
     *
     * @param myRole role to create correct layout
     * @throws java.io.FileNotFoundException
     * @throws org.json.JSONException
     */
    public LayoutCalculator(String myRole, File layoutFile) throws FileNotFoundException, JSONException {
        Scanner scanner = new Scanner(new File("layoutConfigStatic.json"));
        String entireFileText = scanner.useDelimiter("\\A").next();
        input = new JSONObject(entireFileText);
        role = myRole;        
            
        menuPosition = new Position();
        menuPosition.x = input.getJSONObject("startingMenu").getInt("x");
        menuPosition.y = input.getJSONObject("startingMenu").getInt("y");
    }

    /**
     * Gets role from configure file
     *
     * @return role of current user
     * @throws JSONException
     */
    public String getMenuRole() throws JSONException {
        return role;
    }
    
    /**
    * recalculate new layout from JSON layout file and array of nodes or something with specify role 
    * return layout with position nad id|name 
    *
    * it work with fields with ratio < 1, but result may not be as expected, because it 
    * prefer spliting to rows not columbs, if needed can be change in future
     * @param windows
     */
    public void recalculate(List<DisplayableWindow> windows) {
        if (input == null) {
            return;
        }
        Map<String, List<DisplayableWindow>> numRoles;
        numRoles = new HashMap();
        //-----------------
        JSONArray layouts = null;
        try {//load layout
            layouts = input.getJSONArray("layouts");
        } catch (JSONException ex) {
            Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        //-----------------
        Vector<String> roles = new Vector<>();
        roles.add("presentation");
        roles.add("interpreter");
        roles.add("teacher");
        roles.add("student");

        for (int i = 0; i < roles.size(); i++) {
            numRoles.put(roles.get(i), new ArrayList<>());
        }

        //distribute windows by their role
        for (DisplayableWindow win : windows) {
            if (numRoles.containsKey(win.getRole())) {
                numRoles.get(win.getRole()).add(win);
            }
        }
        
        //choose layout
        JSONObject layout = getCorrectLayout(layouts, numRoles);
        JSONObject windowLayout = null;
        JSONObject menuLayout = null;
        try {
            windowLayout = layout.getJSONObject("windows");
            menuLayout = layout.getJSONObject("menu");
        } catch (JSONException ex) {
            Logger.getLogger(LayoutCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(windowLayout == null || menuLayout == null){//something wrong with layout, don't move windows
            return;
        }
        
        //calculate windows position, do it for ecery role
        for (int i = 0; i < roles.size(); i++) {
            JSONArray roleWindowsConfig;
            int windowWidth = 1;
            int windowHeight = 1;
            int windowX = 0;
            int windowY = 0;
            List<DisplayableWindow> winList;
            try {
                if(windowLayout.has(roles.get(i))){
                    roleWindowsConfig = windowLayout.getJSONArray(roles.get(i));
                }else{
                    //missing array of windows position for this role, go for next role
                    continue;
                }
            } catch (JSONException ex) {
                Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            winList = numRoles.get(roles.get(i));
            for (int j = 0; j < winList.size(); j++) {
                DisplayableWindow win = winList.get(j);
                if (j < roleWindowsConfig.length()) {
                    JSONObject windowConfig;
                    try {
                        windowConfig = roleWindowsConfig.getJSONObject(j);
                        windowWidth = windowConfig.getInt("width");
                        windowHeight = windowConfig.getInt("height");
                        windowX = windowConfig.getInt("x");
                        windowY = windowConfig.getInt("y");
                    } catch (JSONException ex) {
                        Logger.getLogger(LayoutManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    win.setHeight(windowHeight);
                    win.setWidth(windowWidth);
                    win.setPosition(new Position(windowX, windowY));
                }
            }
        }
        
        //set menu position
        try {
            menuPosition.x = menuLayout.getInt("x");
            menuPosition.y = menuLayout.getInt("y");
        } catch (JSONException ex) {
            Logger.getLogger(LayoutCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
   
    /**
     * Gets menu position from configure file
     *
     * @return menu position
     */
    public Position getMenuPostion(){
        
        Position position = new Position();
        position.x = menuPosition.x;
        position.y = menuPosition.y;
        return menuPosition;
    }
    
    /**
     * 
     * @param numRoles calculated map of current windows divided to roles
     * @return choose correct layout based on conditions
     */
    private JSONObject getCorrectLayout(JSONArray layouts, Map<String, List<DisplayableWindow>> numRoles){
        JSONObject ret = null;
        for(int i=0;i<layouts.length();i++){
            JSONObject layout;
            JSONArray conditions;
            boolean correct = true;
            try {
                layout = layouts.getJSONObject(i);
                conditions = layout.getJSONArray("conditions");
                for(int j=0;j<conditions.length();j++){
                    JSONObject condition = conditions.getJSONObject(j);
                    correct = correct && checkConndition(condition, numRoles);
                }
                if(correct){
                    //found first layout have fullfiled conditions for this number of windows in each role
                    ret = layout;
                    System.out.println("layout no. " + i);
                    break;
                }
            } catch (JSONException ex) {
                Logger.getLogger(LayoutCalculator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(ret == null){    //if none layout is possible choose first if exist
            if(layouts.length() >= 1){
                System.out.println("using first layout, none is correct");
                try {
                    ret = layouts.getJSONObject(0);
                } catch (JSONException ex) {
                    Logger.getLogger(LayoutCalculator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return ret;
    }
    
    private boolean checkConndition(JSONObject condition, Map<String, List<DisplayableWindow>> numRoles) throws JSONException{
        if(condition.has("role")){
            String conditionRole = condition.getString("role");
            if(condition.has("count")){
                int count = condition.getInt("count");
                return numRoles.get(conditionRole).size() == count;
            }
            if(condition.has("less")){
                int less = condition.getInt("less");
                return numRoles.get(conditionRole).size() < less;
            }
            if(condition.has("more")){
                int more = condition.getInt("more");
                return numRoles.get(conditionRole).size() > more;
            }
        }
        if(condition.has("my role")){
            String myrole = condition.getString("my role");
            return myrole.equalsIgnoreCase(role);
        }
        //defoult value true (incorrectly writen condition is ignored)
        return true;
    }
    
}
