/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

import agc.PlanElementBean;
import appControllers.ApplicationEventListener;
import java.io.IOException;

/**
 *
 * @author maara
 */
public interface Controller {

    public void registerApplicationStatusListener(ApplicationEventListener listener);
    public void unregisterApplicationStatusListener(ApplicationEventListener listener);
    
    /**
     * Starts the application.
     *
     * @throws java.io.IOException
     */
    public abstract void runApplication() throws IOException;

    /**
     * Terminates the application
     */
    public abstract void stopApplication();
    
    /**
     * Set the new working configuration
     * @param patch If <p>null</p>, the application will be paused 
     * @throws java.io.IOException 
     */
    public void applyPatch(PlanElementBean patch) throws IOException;
    
    public boolean isRunning();

    public String getControllerName();
    
    // TODO: Remove. Only usage is in GUI, which can be substituted by Controller directly
    @Deprecated
    public Process getProcess();
}
