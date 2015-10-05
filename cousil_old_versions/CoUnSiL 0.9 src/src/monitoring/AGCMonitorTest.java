package monitoring;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Feb 27, 2009
 * Time: 10:48:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class AGCMonitorTest {
    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    private class AGCMonitorListenerImplement implements AGCMonitorListener {
        // this is to be called when computation of AGC plan has started
       public void onPlanStarted() {
            System.out.println("Caled onPlanStarted on " + this);
        }
        // this is to be called when computation of AGC plan has finished
       public void onPlanFinished() {
            System.out.println("Caled onPlanFinished on " + this);
        }// this is to be called when AGC plan has been distributed
       public void onPlanDistributed() {
           System.out.println("Caled onPlanDistributed on " + this);
        }// this is to be called when AGC plan has been deployed
       public void onPlanDeployed() {
           System.out.println("Caled onPlanDeployed on " + this);
        }
    }

    @Test
    public void testEvents() {
        try {
            AGCMonitorListener lis1 = new AGCMonitorListenerImplement();
            AGCMonitorListener lis2 = new AGCMonitorListenerImplement();
            AGCMonitor agcMon = new AGCMonitor();

            agcMon.registerAGCMonitorListener(lis1);
            agcMon.registerAGCMonitorListener(lis2);

            agcMon.startMonitoring();

            agcMon.setAGCPlanStatus(AGCMonitor.AGCPlanStatus.STARTED);
            Thread.sleep(1000);
            agcMon.setAGCPlanStatus(AGCMonitor.AGCPlanStatus.FINISHED);
            Thread.sleep(1000);
            agcMon.setAGCPlanStatus(AGCMonitor.AGCPlanStatus.DISTRIBUTED);
            Thread.sleep(1000);
            agcMon.setAGCPlanStatus(AGCMonitor.AGCPlanStatus.DEPLOYED);
            Thread.sleep(1000);

            agcMon.stopMonitoring();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
