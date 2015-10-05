/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appControllers.demo;

import agc.PlanElementBean;
import appControllers.ApplicationEventListener;
import appControllers.RumHDController;
import java.io.IOException;
import mediaApplications.RumHD;
import org.codehaus.jettison.json.JSONException;


/**
 *
 * @author maara
 */
public class RumHDDirectInput extends RumHDController {
    
    public RumHDDirectInput(RumHD rumHD) {
        super(rumHD);
    }
    
    private void sleep(long milis) {
        long deadline = System.currentTimeMillis() + milis;
        while (deadline > System.currentTimeMillis()) {
            long timeToSleep = deadline - System.currentTimeMillis();
            if (timeToSleep <= 0) return;
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException ex) {
            }
        }
    }
    
    private void run() throws IOException {
        System.out.println("Starting application");
        runApplication();
        System.out.println("Application started");
        sleep(5000);

        System.out.println("Creating plan element:");
        PlanElementBean pe = new PlanElementBean();
        pe.setSourcePort("5058");
        pe.addTarget(new PlanElementBean.Target("127.0.0.1", null, "none", "5050"));
        pe.addTarget(new PlanElementBean.Target("127.0.0.1", null, "none", "5054"));
        System.out.println("Applying patch: " + pe);
        applyPatch(pe);
        System.out.println("Patch applied");
        sleep(20000);

        System.out.println("Creating no-target plan element on different source port:");
        pe = new PlanElementBean();
        pe.setSourcePort("5062");
        System.out.println("Applying patch: " + pe);
        applyPatch(pe);
        System.out.println("Patch applied");
        sleep(5000);

        System.out.println("Creating plan element on different source port:");
        pe = new PlanElementBean();
        pe.setSourcePort("5058");
        pe.addTarget(new PlanElementBean.Target("127.0.0.1", null, "none", "5050"));
        pe.addTarget(new PlanElementBean.Target("127.0.0.1", null, "none", "5054"));
        System.out.println("Applying patch: " + pe);
        applyPatch(pe);
        System.out.println("Patch applied");
        sleep(20000);
        
        stopApplication();
    }
    
    public static void main(String args[]) throws IOException {
        RumHD rumHD = new RumHD("RUM HD TEST");
        
        rumHD.setPreferredReceivingPort("5030");
        rumHD.setupApplication("hd-rum-transcode", "8M");
        
        System.out.println("Creating controller for application " + rumHD + ":");
        System.out.println("  " + rumHD.getApplicationCmdOptions());
        System.out.println("  " + rumHD.getApplicationName());
        System.out.println("  " + rumHD.getApplicationPath());
        System.out.println("  " + rumHD.getPreferredReceivingPort());
        System.out.println("  " + rumHD.getShortDescription());
        System.out.println("  " + rumHD.getUuid());
        System.out.println("  " + rumHD.getCompressionFormats());
        System.out.println("  " + rumHD.getDecompressionFormats());
        try {
            System.out.println("  " + rumHD.reportStatus());
            System.out.println("  " + rumHD.activeConfig());
        } catch (JSONException ex) {
        }
        RumHDDirectInput ctrl = new RumHDDirectInput(rumHD);

        ApplicationEventListener listener = new ApplicationEventListener() {
            @Override
            public void onCommandSent(Object command) {
                System.out.println("  MESSAGE >> " + command);
            }            
            
            @Override
            public void onMessageReceived(Object message) {
                System.out.println("  MESSAGE << " + message);
            }
        };
        ctrl.registerApplicationStatusListener(listener);

        ctrl.run();
    }
}
