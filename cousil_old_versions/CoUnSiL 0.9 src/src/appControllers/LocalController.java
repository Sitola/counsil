package appControllers;

import myGUI.ControllerPaneWriter;
import mediaAppFactory.MediaApplication;

import myGUI.ControllerFrame;

/**
 * Application controller for steering applications running on the same node as the controller.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 31.7.2007
 * Time: 16:37:34
 */
public abstract class LocalController extends ControllerImpl {

    private ControllerPaneWriter applicationProxy = null;

    public LocalController(MediaApplication application, ControllerFrame localControllerFrame) {
        super(application);
        // applicationProxy = new ApplicationProxy(application.getApplicationPath(), application.getApplicationCmdOptions(), true);
    }

    public ControllerPaneWriter getApplicationProxy() {
        return applicationProxy;
    }

    public void setApplicationProxy(ControllerPaneWriter applicationProxy) {
        this.applicationProxy = applicationProxy;
    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return super.application.getApplicationName() + " (" + super.application.getApplicationCmdOptions() + ") (from LocalController)";
    }
}
