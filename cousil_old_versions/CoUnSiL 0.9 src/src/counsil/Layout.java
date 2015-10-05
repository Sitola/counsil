/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import networkRepresentation.EndpointUserRole;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import wddman.OperatingSystem;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;
import wddman.Window;

/**
 *
 * @author Peter
 */
public class Layout {

    public static final boolean DEBUG_MODE_LAYOUT = false;

    /**
     * @return the displayedType
     */
    public String getDisplayedType() {
        return displayedType;
    }

    /**
     * @param displayedType the displayedType to set
     */
    public void setDisplayedType(String displayedType) {
        this.displayedType = displayedType;
    }

    private StreamNumber streamNumber;
    private Split split;
    private int weight;
    private String displayedType;
    private String nodeRole;
    private String application;
    //private ArrayList<Window> windows;
    private ArrayList<Displayable> windows = new ArrayList<>();
    private ArrayList<Layout> childLayouts;
    private int childNumber;

    private int top;
    private int left;
    private int height;
    private int width;

    static int screenWidth;
    static int screenHeight;
    private WDDMan manipulator = null;

    {
        try {
            manipulator = new WDDMan();
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(Layout.class.getName()).log(Level.SEVERE, null, ex);
        }
        int startPanelHeight = 0;
        try {
            if (manipulator.getRunningOperatingSystem() == OperatingSystem.WINDOWS) {
                Window start = manipulator.getWindowByTitle("Start");
                startPanelHeight = start.getHeight();
            }

        } catch (WDDManException ex) {
            Logger.getLogger(Layout.class.getName()).log(Level.SEVERE, null, ex);
        }
        //sets width and height to screen
        try {
            screenHeight = manipulator.getScreenHeight() - startPanelHeight;
            screenWidth = manipulator.getScreenWidth();
            //set width and height to screen for root, will be set otherwise by recomputation
            setHeight(screenHeight);
            setWidth(screenWidth);
        } catch (WDDManException ex) {
            System.out.println(ex.getMessage());
        }

    }

    public JSONObject saveToJSON() throws IOException {

        JSONObject output = new JSONObject();

        output.put("split", this.getSplit());
        output.put("weight", this.getWeight());
        output.put("nodeRole", this.getNodeRole());
        output.put("application", this.getApplication());
        output.put("childNumber", this.getChildNumber());

        if (getSplit() != Split.NO) {
            int childNumbering = 0;

            for (Layout child : this.getChildLayouts()) {
                childNumbering++;
                output.put("child" + childNumbering, child.saveToJSON());

            }

        }

        return output;

    }

    /**
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * @return the nodeRole
     */
    public String getNodeRole() {
        return nodeRole;
    }

    /**
     * @param nodeRole the nodeRole to set
     */
    public void setNodeRole(String nodeRole) {
        this.nodeRole = nodeRole;
    }

    /**
     * @return the application
     */
    public String getApplication() {
        return application;
    }

    /**
     * @param application the application to set
     */
    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * @return the childLayouts
     */
    public ArrayList<Layout> getChildLayouts() {
        return childLayouts;
    }

    /**
     * @param childLayouts the childLayouts to set
     */
    public void setChildLayouts(ArrayList<Layout> childLayouts) {
        this.childLayouts = childLayouts;
    }

    /**
     * @return the split
     */
    public Split getSplit() {
        return split;
    }

    /**
     * @param split the split to set
     */
    public void setSplit(Split split) {
        this.split = split;
    }

    /**
     * 0 - NO splittling 1 - VERTICAL splitting 2 - HORIZONTAL splitting
     *
     */
    /**
     *
     * @param handlers handlers for the windows on desktop - names and assigned
     * roles
     * @param path path to the config file
     * @param role role of the layout owner - to know what to display
     * @return
     */
    //TODO
    @Deprecated
    void addHandlersAndRolesToLayout(HashMap<EndpointUserRole, ArrayList<Displayable>> handlers, EndpointUserRole role) {
        if (split == Split.NO) {

            this.windows = handlers.get(displayedType);

        } else {
            for (Layout child : childLayouts) {
                child.setNodeRole(role.getMyRole());
                child.addHandlersAndRolesToLayout(handlers, role);

                //System.out.println("Setchildrole? " + child.getNodeRole().getMyRole());
            }
        }
    }

    int getTotalWeight() {
        int totalWeight = 0;
        for (Layout child : getChildLayouts()) {
            totalWeight += child.getWeight();
        }
        return totalWeight;
    }

    //TODO
    void countCoordinatesForLayout(Split lastSplit) {
        //System.out.println("in countCoord "+ this.getSplit());
        if (DEBUG_MODE_LAYOUT) {
            System.out.println("Window coord  count begin top: " + top + " left: " + left + " width: " + width + " height: " + height);
        }
        //System.out.println("Split: " + this.split);
        //System.out.println("Main role in coordCount: " + nodeRole);
        //System.out.println("Displayed role in coordCount: " + displayedRole);
        int offset = 0;

        switch (this.getSplit()) {
            case HORIZONTAL: {
                for (Layout child : getChildLayouts()) {

                    //int childWeight = child.getWeight()/getTotalWeight();
                    child.setHeight((height * child.getWeight()) / getTotalWeight());
                    child.setWidth(width);
                    child.setLeft(left);
                    child.setTop(top + offset);
                    offset += height * child.getWeight() / getTotalWeight();

                    //System.out.println("Child frame coordinates in HORIZONTAL  top: " + child.getTop() + 
                    //       " left: " + child.getLeft() +  " width: " +child.getWidth() + " height: " + child.getHeight());
                    child.countCoordinatesForLayout(Split.HORIZONTAL);
                }
                break;
            }
            case VERTICAL: {
                for (Layout child : getChildLayouts()) {
                    // System.out.println("number of child layouts: " + getChildLayouts().size());
                    //int childWeight = child.getWeight()/getTotalWeight();

                    child.setHeight(height);
                    child.setWidth((width * child.getWeight()) / getTotalWeight());
                    child.setTop(top);
                    child.setLeft(left + offset);

                    offset += width * child.getWeight() / getTotalWeight();
                    // System.out.println("Child frame coordinates in VERTICAL top: " + child.getTop() + 
                    //  " left: " + child.getLeft() +  " width: " +child.getWidth() + " height: " + child.getHeight());

                    child.countCoordinatesForLayout(Split.VERTICAL);
                }
                break;
            }
            case NO: {
                //System.out.println("in NO switch: " +this.streamNumber+ " lastSplit " + lastSplit);
                //System.out.println("Frame coordinates in NO - top: " + top + " left: " + left +  " width: " +width + " height: " + height);
                if (streamNumber == StreamNumber.VARIABLE) {
                    placeVariableNumberOfStreams();
                } else if (streamNumber == StreamNumber.SINGLE) {
                    placeSingleStream(lastSplit);
                }
                break;
            }
        }

    }

    void placeVariableNumberOfStreams() {
        Bound bound = (height < width) ? Bound.HEIGHT : Bound.WIDTH;

        int longDimOccupiedSpace = 0;
        int longDimFreeSpace;
        int windowHeight;
        int windowWidth;
        windows.clear();

        if (DEBUG_MODE_LAYOUT) {
            System.out.println("Displayable info length: " + UserController.getDisplayableInfo().size());
        }

        for (DisplayableInfo info : UserController.getDisplayableInfo()) {

            if (info.getType().equals(displayedType)) {

                windows.add(info.getStreamWindow());
                if (DEBUG_MODE_LAYOUT) {
                    System.out.println("Info type: " + info.getType() + "SW: " + info.getStreamWindow());
                    System.out.println("Winsize: " + windows.size());
                }
            }
//            windows.add(info.getStreamWindow());
        }

        if (windows.size() == 0) {
            return;
        }
        if (windows.size() == 1) {
            placeSingleStream(Split.NO);
            return;
        }

        for (Displayable window : windows) {

            windowHeight = window.getHeight();
            windowWidth = window.getWidth();

                //System.out.println("h and w of student " + windowHeight + " " + windowWidth);
            //if height is boundary, adjust width accordingly and vice versa
            float ratio = (bound == Bound.HEIGHT) ? (float) height / (float) windowHeight : (float) width / (float) windowWidth;
            windowWidth *= ratio;
            windowHeight *= ratio;

                //System.out.println("Frame coordinates for the VAR frames - top: " + top + " left: " + left + " width: " + windowWidth + " height: " + windowHeight);
            longDimOccupiedSpace += (bound == Bound.HEIGHT) ? windowWidth : windowHeight;

                //System.out.println("node role in varStreams: " + nodeRole);
            //System.out.println("wWidth before: " + windowWidth);
            window.resize(0, 0, windowWidth, windowHeight);
                //System.out.println("wWidth after: " + encapsulator.getWidth());

        }

        longDimFreeSpace = ((bound == Bound.HEIGHT) ? width : height) - longDimOccupiedSpace;

        if (DEBUG_MODE_LAYOUT) {
            System.out.println("occupiedSpace: " + longDimOccupiedSpace + " totalspace: " + width + " freeSpace: " + longDimFreeSpace);
        }
        int spacing = longDimFreeSpace / (windows.size() - 1);

        int windowTop = this.top;
        int windowLeft = this.left;

        for (Displayable window : windows) {
            window.changePosition(windowLeft, windowTop);
            if (bound == Bound.HEIGHT) {
                //System.out.println("wWidth: " + window.getWidth() + " spacing: " + spacing + " offset: " + windowLeft);
                windowLeft += spacing + window.getWidth();

            } else if (bound == Bound.WIDTH) {
                windowTop += spacing + window.getHeight();

            }
        }

    }

    void placeSingleStream(Split lastSplit) {

        windows.clear();

        for (DisplayableInfo info : UserController.getDisplayableInfo()) {
               // System.out.println("size UC: " + UserController.getDisplayableInfo().size() + " type: " + info.getType() + " class: " + info.getStreamWindow());

            if (info.getType().equals(displayedType)) {
                //   System.out.println("Got match");
                windows.add(info.getStreamWindow());
            }
        }
        //System.out.println("HERE");
        if (windows.isEmpty()) {
            return;
        }

        Displayable window = windows.get(0);
        //System.out.println("HERE");
        int windowHeight = window.getHeight();
        int windowWidth = window.getWidth();
        Bound bound = (((float) height / (float) windowHeight) < (float) width / (float) windowWidth) ? Bound.HEIGHT : Bound.WIDTH;

        float ratio = (bound == Bound.HEIGHT) ? (float) height / (float) windowHeight : (float) width / (float) windowWidth;

        windowWidth *= ratio;
        windowHeight *= ratio;

        //ADJUST top/left to middle
        if (bound == Bound.WIDTH) {
            top += (height - windowHeight) / 2;
        } else if (bound == Bound.HEIGHT) {
            left += (width - windowWidth) / 2;
        }

        //    System.out.println("Window coord final top: " + top + " left: " + left +  " width: " +windowWidth + " height: " + windowHeight+ " windowAdress: " + windows.get(0));
        window.resize(left, top, windowWidth, windowHeight);
        //window.move(left,top);

    }

    /**
     * @return the childNumber
     */
    public int getChildNumber() {
        return childNumber;
    }

    /**
     * @param childNumber the childNumber to set
     */
    public void setChildNumber(int childNumber) {
        this.childNumber = childNumber;
    }

    /**
     * @return the streamNumber
     */
    public StreamNumber getStreamNumber() {
        return streamNumber;
    }

    /**
     * @param streamNumber the streamNumber to set
     */
    public void setStreamNumber(StreamNumber streamNumber) {
        this.streamNumber = streamNumber;
    }

    /**
     * @return the windows
     */
    public ArrayList<Displayable> getWindows() {
        return windows;
    }

    /**
     * @param windows the windows to set
     */
    public void setWindows(ArrayList<Displayable> windows) {
        this.windows = windows;
    }

    /**
     * @return the top
     */
    public int getTop() {
        return top;
    }

    /**
     * @param top the top to set
     */
    public void setTop(int top) {
        this.top = top;
    }

    /**
     * @return the left
     */
    public int getLeft() {
        return left;
    }

    /**
     * @param left the left to set
     */
    public void setLeft(int left) {
        this.left = left;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

}
