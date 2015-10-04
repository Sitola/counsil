/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.logging.Level;
import java.util.logging.Logger;
import networkRepresentation.EndpointNetworkNode;
import networkRepresentation.EndpointUserRole;
import wddman.WDDManException;
import wddman.Window;

/**
 *
 * @author Jarek Kala
 */
public class WindowController implements Displayable{

    private final Window window;
    private final InvisibleOverlayFrame invisibleOverlayFrame;
    private int width = 0;
    private int height = 0;

    public WindowController(Window window, CounsilNetworkNodeLight endpointNetworkNode) {
        this.window = window;
        invisibleOverlayFrame = new InvisibleOverlayFrame(endpointNetworkNode);
        try {
            int x = window.getLeft();
            int y = window.getTop();
            width = window.getWidth();
            height = window.getHeight();
            invisibleOverlayFrame.resize(x, y, width, height);
        } catch (WDDManException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void resize(int x, int y, int width, int height) {
        try {
            window.resize(x, y, width, height);
            this.width = width;
            this.height = height;
        } catch (WDDManException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
        invisibleOverlayFrame.resize(x, y, width, height);
    }
    
    @Override
    public void changePosition(int x, int y){
         try {
            window.move(x, y);
        } catch (WDDManException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
        invisibleOverlayFrame.setLocation(x, y);       
    }

    public void raiseHand() {
        invisibleOverlayFrame.raiseHand();
    }

    public void talk() {
        invisibleOverlayFrame.talk();
    }

    public void stopRaisingHand() {
        invisibleOverlayFrame.stopRaisingHand();
    }

    public void stopTalking() {
        invisibleOverlayFrame.stopTalking();
    }
    
    public void close() {
        try {
            window.close();
        } catch (WDDManException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
        invisibleOverlayFrame.close();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
