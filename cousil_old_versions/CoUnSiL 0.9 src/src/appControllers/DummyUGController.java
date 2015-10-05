/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers;

import agc.PlanElementBean;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author maara
 */
public class DummyUGController extends ControllerImpl {

    private volatile DummyUGFrame frame;
    private final String descriptor;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public DummyUGController(final String name, final String applicationType) throws InterruptedException, InvocationTargetException {
        super(new mediaApplications.RumHD(name));
        descriptor = name + " (Dummy " + applicationType + ")";

        java.awt.EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new DummyUGFrame();
                frame.setVisible(true);
                frame.nameLabel.setText(name);
                frame.typeLabel.setText(applicationType);
                frame.setTitle(name);
            }
        });
    }
    
    @Override
    public String toString() {
        return descriptor;
    }

    @Override
    public void runApplication() throws IOException {
        
        if (running.get()) {
            Logger.getLogger(this.getClass().getCanonicalName()).warning("Attempting to start already running application! Call ignored");
            return;
        }
        
        frame.statusLabel.setText("Running");
        frame.portLabel.setText("null");
        frame.targetsTextArea.setText("");
    }

    @Override
    public void stopApplication() {

        if (! running.get()) {
            Logger.getLogger(this.getClass().getCanonicalName()).warning("Attempting to stop idle application! Call ignored");
            return;
        }
        
        frame.statusLabel.setText("Stopped");
        frame.portLabel.setText("null");
        frame.targetsTextArea.setText("");
    }

    @Override
    public void applyPatch(final PlanElementBean patch) throws IOException {

        if (patch == null) {
            stopApplication();
            return;
        }
        
        if (! running.get()) {
            runApplication();
        }
        
        if (running.get()) {
            Logger.getLogger(this.getClass().getCanonicalName()).warning("Attempting to patch stopped application! Call ignored");
            return;
        }
        
        frame.portLabel.setText(patch.getSourcePort());
        StringBuilder sb = new StringBuilder();
        for (PlanElementBean.Target target : patch.getTargets()) {
            sb.append("" + target.getTargetIp() + ":" + target.getTargetPort() + " @" + target.getFormat());
            sb.append("\n");
        }
        frame.targetsTextArea.setText(sb.toString());
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
    
}
