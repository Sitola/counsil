/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

/**
 * Default status listener. Override the required events, others may be left empty
 */
public abstract class ApplicationEventListener {

    /**
     * Called if the transmission goes down
     * @param o Should be null
     */
    public void onTransmissionDown(Object o) {
    }

    /**
     * Called if packet loss is detected
     * @param o tuple {packet amount sent during monitored period, lost packet amount}
     */
    public void onPacketLoss(Object o) {
    }

    /**
     * Called when controller issues a command to the application.
     * @param command command sent to the application
     */
    public void onCommandSent(Object command) {
    }

    /**
     * Called if the application reports back to CoUniverse
     * @param message message received from the application
     */
    public void onMessageReceived(Object message) {
    }
    
    /**
     * Called if the underlying process terminates
     * @param message Process termination message-if known
     */
    public void onProccessTerminated(Object message) {
    }
    
    /**
     * Report a message in debug mode
     * @param message Debug message in format Serializable[LinkedList(String) sourceDescriptor, String text]
     */
    public void onDebugMessage(Object message) {
    }
}
