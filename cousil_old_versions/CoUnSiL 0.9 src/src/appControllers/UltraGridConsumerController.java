/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

import agc.PlanElementBean;
import java.io.IOException;
import mediaApplications.UltraGridConsumer;

/**
 *
 * @author maara
 */
class UltraGridConsumerController extends UltraGridController implements ConsumerController {

    String currentSourcePort = null;

    public UltraGridConsumerController(UltraGridConsumer application) {
        super(application);
    }

    @Override
    public void applyPatch(PlanElementBean patch) throws IOException {
        if (patch == null) {
//            controllerFrame.writeMessage("Paused", this);
            currentSourcePort = null;
            change("pause");
            
            return;
        }

        final String sourcePort = patch.getSourcePort();
        boolean changed = !paramsEqual(currentSourcePort, sourcePort);
        if (changed) {
            change("receiver-port " + sourcePort);
            currentSourcePort = sourcePort;
            change("play");
        }
    }
    
}
