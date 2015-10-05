package appControllers;

import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import mediaAppFactory.MediaApplication;
import org.apache.log4j.Logger;

public abstract class ControllerImpl implements Controller {
    public static interface OutputReader {
        void attachStderr(InputStream stderr);
        void attachStdout(InputStream stdout);
        void detachStderr();
        void detachStdout();
    }

    
    final MediaApplication application;  // reference to the media application to work with
    static Logger logger = Logger.getLogger(ControllerImpl.class);
    protected Process process;
    protected Thread processShutdownhook;
    protected OutputReader readers;
    
    // jestli tuhle hruzu jeste nekdy uvidim, tak se radeji strelim do hlavy. gonzales

    private final CopyOnWriteArrayList<ApplicationEventListener> listeners = new CopyOnWriteArrayList<>();

    protected ControllerImpl(MediaApplication application) {
        this.application = application;
        this.readers = null;
    }
    
    public void setReaders(OutputReader r) {
        readers = r;
    }
    
    public MediaApplication getApplication() {
        return application;
    }

    static void infoOnException(Throwable e, String s) {
        if (logger.isDebugEnabled()) {
            e.printStackTrace();
        }
        logger.error(s);
    }

    @Override
    public abstract String toString();

    @Override
    public void registerApplicationStatusListener(ApplicationEventListener listener) {
        synchronized(listeners) {
            if (listener == null) throw new NullPointerException("Cannot register null listener!");
            listeners.add(listener);
        }
    }
    
    @Override
    public void unregisterApplicationStatusListener(ApplicationEventListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }
    
    @Override
    public Process getProcess() {
        return this.process;
    }

    @Override
    public String getControllerName() {
        return getApplication().toString();
    }

    public boolean paramsEqual(String s1, String s2) {
        return (s1 == null ? s2 == null : s1.equals(s2));
    }
    
    public final void reportTransmissionDown(Object o) {
        synchronized(listeners) {
            for (ApplicationEventListener listener : listeners) {
                try {
                    listener.onTransmissionDown(o);
                } catch (Exception ex) {
                    logger.error("Exception thrown upon transmission lost report by event listener " + listener, ex);
                }
            }
        }
    }
    
    public final void reportPacketLoss(Object o) {
        synchronized(listeners) {
            for (ApplicationEventListener listener : listeners) {
                try {
                    listener.onPacketLoss(o);
                } catch (Exception ex) {
                    logger.error("Exception thrown upon packet loss report by event listener " + listener, ex);
                }
            }
        }
    }
    
    public final void reportSendingCommand(Object o) {
        synchronized(listeners) {
            for (ApplicationEventListener listener : listeners) {
                try {
                    listener.onCommandSent(o);
                } catch (Exception ex) {
                    logger.error("Exception thrown upon sending command by event listener " + listener, ex);
                }
            }
        }
    }

    public final void reportMessageReception(Object o) {
        synchronized(listeners) {
            for (ApplicationEventListener listener : listeners) {
                try {
                    listener.onMessageReceived(o);
                } catch (Exception ex) {
                    logger.error("Exception thrown upon message reception reporting by event listener " + listener, ex);
                }
            }
        }
    }
    
    public final void reportDebugMessage(Object o) {
        synchronized(listeners) {
            for (ApplicationEventListener listener : listeners) {
                try {
                    LinkedList<String> hierarchy = new LinkedList<>((LinkedList<String>) (((Serializable[]) o)[0]));
                    hierarchy.addFirst(this.application.getApplicationName());
                    Serializable[] payload = new Serializable[] {
                        hierarchy,
                        ((Serializable[]) o)[1]
                    };
                        
                    listener.onDebugMessage(payload);
                } catch (Exception ex) {
                    logger.error("Exception thrown upon message reception reporting by event listener " + listener, ex);
                }
            }
        }
    }
    
    
    
    /**
     * Call to destroy the controller
     */
    public void exit() {
        // TODO: make abstract and implement in the sub-classes
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
