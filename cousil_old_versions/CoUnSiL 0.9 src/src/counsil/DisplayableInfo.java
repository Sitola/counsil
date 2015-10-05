/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import mediaAppFactory.MediaApplication;
import networkRepresentation.EndpointNetworkNode;

/**
 *
 * @author Peter
 */
public class DisplayableInfo {

    private Displayable streamWindow;
    private String type;
    private String handler;
    private MediaApplication app;
    private CounsilNetworkNodeLight endpointNetworkNode;

    public DisplayableInfo(String handler, String type, Displayable streamWindow, MediaApplication app, CounsilNetworkNodeLight endpointNetworkNode) {
        this.handler = handler;
        this.type = type;
        this.streamWindow = streamWindow;
        this.app = app;
        this.endpointNetworkNode = endpointNetworkNode;
    }

    /**
     * @return the streamWindow
     */
    public Displayable getStreamWindow() {
        return streamWindow;
    }

    /**
     * @param streamWindow the streamWindow to set
     */
    public void setStreamWindow(Displayable streamWindow) {
        this.streamWindow = streamWindow;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param role the role to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * @param handler the handler to set
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * @return the app
     */
    public MediaApplication getApp() {
        return app;
    }

    /**
     * @param app the app to set
     */
    public void setApp(MediaApplication app) {
        this.app = app;
    }

    public CounsilNetworkNodeLight getEndpointNetworkNode() {
        return endpointNetworkNode;
    }
}
