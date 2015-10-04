/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import static counsil.Layout.DEBUG_MODE_LAYOUT;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Peter
 */
public class LayoutCreator {

    public static Layout createLayoutFromConfigFile(String path, String type) {
        JSONObject object = null;
        Layout returnLayout = null;

        try {

            object = pickJSONObjectFromLayoutFile(path, type);

        } catch (IOException | ParseException ex) {
            Logger.getLogger(Layout.class.getName()).log(Level.SEVERE, null, ex);

        }

        try {
            returnLayout = constructLayoutFromJSON(object);
        } catch (IOException ex) {
            Logger.getLogger(Layout.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (DEBUG_MODE_LAYOUT) {
            System.out.println("Entering role is: " + type);
        }
        returnLayout.setNodeRole(type);
        // returnLayout.addHandlersAndRolesToLayout(windows,type);

        if (DEBUG_MODE_LAYOUT) {
            System.out.println("role in create: " + returnLayout.getNodeRole());
        }

        return returnLayout;
    }

    private static JSONObject pickJSONObjectFromLayoutFile(String path, String type) throws IOException, ParseException {

        //read it
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        String JSONConstructionString = "";
        while ((line = reader.readLine()) != null) {
            JSONConstructionString += line;
        }

        JSONObject layoutObject;
        JSONParser parser = new JSONParser();
        layoutObject = (JSONObject) parser.parse(JSONConstructionString);

        JSONObject layoutsSelected = ((JSONObject) layoutObject.get("layouts"));
        if (DEBUG_MODE_LAYOUT) {
            System.out.println("layouts " + layoutsSelected.toString());
        }
        type = "student";
        JSONObject roleSelected = (JSONObject) layoutsSelected.get(type + "Layout");
        if (DEBUG_MODE_LAYOUT) {
            System.out.println("selection " + roleSelected.toString());
        }
        return roleSelected;
    }

    private static Layout constructLayoutFromJSON(JSONObject input) throws IOException {
        Layout newLayout = new Layout();
        //System.out.println(" input null " + input==null?"ahoj":"pico");
        if (DEBUG_MODE_LAYOUT) {
            System.out.println(input.toString());
        }
        if (((String) input.get("split")).equals("VERTICAL")) {
            newLayout.setSplit(Split.VERTICAL);
        }
        if (((String) input.get("split")).equals("HORIZONTAL")) {
            newLayout.setSplit(Split.HORIZONTAL);
        }
        if (((String) input.get("split")).equals("NO")) {
            newLayout.setSplit(Split.NO);
        }

        if (input.get("streamNumber").equals("NA")) {
            newLayout.setStreamNumber(StreamNumber.NA);
        }
        if (input.get("streamNumber").equals("SINGLE")) {
            newLayout.setStreamNumber(StreamNumber.SINGLE);
        }
        if (input.get("streamNumber").equals("VARIABLE")) {
            newLayout.setStreamNumber(StreamNumber.VARIABLE);
        }

        newLayout.setDisplayedType((String) input.get("displayedType"));

        int newWeight = Integer.parseInt((String) input.get("weight"));
        newLayout.setWeight(newWeight);

        ArrayList<Layout> childrenLayouts = new ArrayList<>();

        if (newLayout.getSplit() != Split.NO) {

            JSONArray children;
            children = (JSONArray) input.get("children");

            for (Object child : children) {
                childrenLayouts.add(constructLayoutFromJSON((JSONObject) child));
            }
        }

        newLayout.setChildLayouts(childrenLayouts);

        return newLayout;
    }

}
