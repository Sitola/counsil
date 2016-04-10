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
     * Creates layout calculator
     *
     * @throws java.io.FileNotFoundException
     * @throws org.json.JSONException
     */
    public LayoutCalculator() throws FileNotFoundException, JSONException {

        String entireFileText = new Scanner(new File("layoutConfigStatic.json")).useDelimiter("\\A").next();
        input = new JSONObject(entireFileText);
        role = input.getJSONObject("menu").get("role").toString();
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

        for (int i = 0; i < roles.size(); i++) {
            numRoles.put(roles.get(i), new ArrayList<>());
        }

        //for each role in input put windows to distribute in them
        for (DisplayableWindow win : windows) {
            if (numRoles.containsKey(win.getRole())) {
                List<DisplayableWindow> intIncrem = numRoles.get(win.getRole());
                intIncrem.add(win);
            }
        }

        //distribute windows in their field
        for (int i = 0; i < roles.size(); i++) {
            JSONArray roleWindowsConfig;
            int windowWidth = 1;
            int windowHeight = 1;
            int windowX = 0;
            int windowY = 0;
            List<DisplayableWindow> winList;
            try {
                roleWindowsConfig = fields.getJSONArray(roles.get(i));
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
    }
   
    /**
     * Gets menu position from configure file
     *
     * @return menu position
     * @throws JSONException
     */
    public Position getMenuPostion() throws JSONException {
        
        Position position = new Position();
        position.x = input.getJSONObject("menu").getInt("x");
        position.y = input.getJSONObject("menu").getInt("y");
        return position;
    }
}
