/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

import agc.PlanElementBean;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mediaAppFactory.MediaApplicationProducer;

/**
 *
 * @author maara
 */
public class UltraGridProducerController extends UltraGridController implements ProducerController {

    String currentTargetIp = null;
    String currentTargetPort = null;
    String currentFormat = null;

    public UltraGridProducerController(MediaApplicationProducer application) {
        super(application);
    }
    
    @Override
    public void runApplication() throws IOException {
        super.runApplication();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UltraGridProducerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        change("pause");
    }
    
    @Override
    public void applyPatch(PlanElementBean patch) {
        
        String targetIP = null, targetPort = null;
        String format = null;
        
        if (patch == null) {
            try {
                change("pause");
            } catch (IOException ex) {
                logger.warn("Failed to pause " + this);
            }
            currentTargetIp = "";
            currentTargetPort = "";
            currentFormat = "";
            return;
        }
        
        if (patch != null) {
            targetIP   = patch.getTargetIPs()  .isEmpty() ? null : patch.getTargetIPs()  .get(0);
            targetPort = patch.getTargetPorts().isEmpty() ? null : patch.getTargetPorts().get(0);
            format     = patch.getFormats()    .isEmpty() ? null : patch.getFormats()    .get(0);
        }

        boolean changedIp = !paramsEqual(currentTargetIp, targetIP);
        boolean changedPort = !paramsEqual(currentTargetPort, targetPort);
        boolean changedFormat = !paramsEqual(currentFormat, format);
        boolean changed = changedIp || changedPort || changedFormat;
        if (!changed) return;
        
        try {
            if (targetIP == null) {
                isActive.set(false);
                change("pause");
//                controllerFrame.writeMessage("Paused", this);
                return;
            }
            change("pause");
            if (changedPort) {
                change("sender-port " + targetPort);
                currentTargetPort = targetPort;
            }
            if (changedIp) {
                change("receiver " + targetIP);
                currentTargetIp = targetIP;
            }
            if (changedFormat) {
                currentFormat = format;
                if (format != null) {
                    change("compress " + format);
                }
            }
            change("play");
//            controllerFrame.writeMessage("Setting new target to: "+targetIP+":"+targetPort, this);
            
            isActive.set(true);
        } catch (IOException ex) {
            logger.error("Failed to change target of <"+application.getApplicationName()+"> to "+targetIP, ex);
        }
        logger.info("<"+application.getApplicationName()+"> sending to "+targetIP);
    }

}
