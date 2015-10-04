package appControllers;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mediaAppFactory.ApplicationFactory;
import mediaAppFactory.MediaApplication;
import mediaApplications.DummyConsumer;
import mediaApplications.DummyDistributor;
import mediaApplications.DummyProducer;
import mediaApplications.RumHD;
import mediaApplications.UltraGridConsumer;
import mediaApplications.UltraGridProducer;

/**
 * @author Lukáš Ručka, 359687
 *
 */
public class AppControllerPairing {
    public static void initialize() {
      ApplicationFactory.getInstance().registerGenerator("RumHD", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new RumHD();
            }
            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                if (!(app instanceof RumHD)) {
                    throw new IllegalArgumentException("Wrong application type for this controller");
                }
                return new RumHDController((RumHD) app);
            }
            @Override
            public Class getAppClass() {
                return RumHD.class;
            }
        });
        ApplicationFactory.getInstance().registerGenerator("Ultragrid producer", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new UltraGridProducer();
            }

            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                if (!(app instanceof UltraGridProducer)) {
                    throw new IllegalArgumentException("Wrong application type for this controller");
                }
                return new UltraGridProducerController((UltraGridProducer) app);
            }
            @Override
            public Class getAppClass() {
                return UltraGridProducer.class;
            }
        });
        
        ApplicationFactory.getInstance().registerGenerator("Ultragrid consumer", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new UltraGridConsumer();
            }

            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                if (!(app instanceof UltraGridConsumer)) {
                    throw new IllegalArgumentException("Wrong application type for this controller");
                }
                return new UltraGridConsumerController((UltraGridConsumer) app);
            }
            @Override
            public Class getAppClass() {
                return UltraGridConsumer.class;
            }
        });

        
      ApplicationFactory.getInstance().registerGenerator("Distributor dummy", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new DummyDistributor();
            }
            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                try {
                    return new DummyUGController("Ditributor", "Distributor dummy");
                } catch (InterruptedException | InvocationTargetException ex) {
                    Logger.getLogger(AppControllerPairing.class.getName()).log(Level.SEVERE, "Failed to create controller!", ex);
                }
                return null;
            }
            @Override
            public Class getAppClass() {
                return DummyDistributor.class;
            }
        });
        ApplicationFactory.getInstance().registerGenerator("Producer dummy", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new DummyProducer();
            }

            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                try {
                    return new DummyUGController("Producer", "Producer dummy");
                } catch (InterruptedException | InvocationTargetException ex) {
                    Logger.getLogger(AppControllerPairing.class.getName()).log(Level.SEVERE, "Failed to create controller!", ex);
                }
                return null;
            }
            @Override
            public Class getAppClass() {
                return DummyProducer.class;
            }
        });
        
        ApplicationFactory.getInstance().registerGenerator("Consumer dummy", new ApplicationFactory.Generator() {
            @Override
            public MediaApplication newApplication() {
                return new DummyConsumer();
            }

            @Override
            public ControllerImpl createController(MediaApplication app) throws IllegalArgumentException {
                try {
                    return new DummyUGController(app.getApplicationCmdOptions(), "Consumer dummy");
                } catch (InterruptedException | InvocationTargetException ex) {
                    Logger.getLogger(AppControllerPairing.class.getName()).log(Level.SEVERE, "Failed to create controller!", ex);
                }
                return null;
            }
            @Override
            public Class getAppClass() {
                return DummyConsumer.class;
            }
        });
    
    }
}
