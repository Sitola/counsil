package monitoring;

/**
 * Created by IntelliJ IDEA.
 * User: xsuchom1
 * Date: Feb 21, 2009
 * Time: 7:14:43 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AGCMonitorListener {
     // this is to be called when computation of AGC plan has started
    public void onPlanStarted();
     // this is to be called when computation of AGC plan has finished
    public void onPlanFinished();
      // this is to be called when AGC plan has been distributed
    public void onPlanDistributed();
      // this is to be called when AGC plan has been deployed
    public void onPlanDeployed();
}
