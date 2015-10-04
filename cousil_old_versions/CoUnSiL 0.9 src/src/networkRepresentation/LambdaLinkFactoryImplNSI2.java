package networkRepresentation;

import net.glambda.nsi2.impl.EventListener;
import net.glambda.nsi2.util.NSITextDump;
import net.glambda.nsi2.util.TypesBuilder;
import nsi2.NSI2Client;
import nsi2.reply.ReserveCommitReply;
import nsi2.reply.ReserveReply;
import org.apache.log4j.Logger;
import org.ogf.schemas.nsi._2013._07.connection._interface.QuerySummarySyncFailed;
import org.ogf.schemas.nsi._2013._07.connection._interface.ServiceException;
import org.ogf.schemas.nsi._2013._07.connection.types.*;
import org.ogf.schemas.nsi._2013._07.services.types.StpType;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 6.9.13
 * Time: 10:02
 * To change this template use File | Settings | File Templates.
 */
public class LambdaLinkFactoryImplNSI2 implements LambdaLinkAbstractFactory {
    static Logger logger = Logger.getLogger("lambda");

    private NSI2Client client;
    String connectionId;
    boolean hasRequester;

    //private final String providerURI = "https://bod2.surfnet.nl/nsi/v2/provider"; // "https://agg.dlp.surfnet.nl/nsi-v2/ConnectionServiceProvider"
    //private final String providerNSA = "urn:ogf:network:aist.go.jp:nsa";
    //private final String providerURI = "http://163.220.30.174:28080/nsi2/services/ConnectionProvider";

    //private final String providerNSA = "urn:ogf:network:netherlight.net:2013:nsa:bod";
    //private final String providerURI = "https://agg.dlp.surfnet.nl/nsi-v2/ConnectionServiceRequester";

    private final String providerNSA = "urn:ogf:network:aist.go.jp:nsa";
    private final String providerURI = "http://163.220.30.174:28080/nsi2/services/ConnectionProvider";

    private final String requesterNSA = "urn:ogf:network:aist.go.jp:2013:nsa";
    private String requesterURI = null;
    private final static AtomicInteger requesterPort = new AtomicInteger(8080);
    private final String OAuth = null; // "44e53ca6-5b48-4503-b605-388e48085b36";


    /*private final String destNetworkId = "urn:ogf:network:czechlight.cesnet.cz:2013:topology:a-gole:testbed";
    private final String srcNetworkId = "urn:ogf:network:uvalight.net:2013:";
    private final String destLocalId = "urn:ogf:network:czechlight.cesnet.cz:2013:port:a-gole:testbed:556";
    private final String srcLocalId = "urn:ogf:network:uvalight.net:2013:ps";*/


    final String destNetworkId = "urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed";
    final String srcNetworkId = "urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed";
    final String destLocalId = "urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:282";
    final String srcLocalId = "urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1";

    // listener can be null if you don't want it
    private static final EventListener listener = null; // new SampleEventListener();

    private static final String httpUser = null,
            httpPassword = null;

    static {
        //System.setProperty("javax.net.ssl.trustStore", "/home/demo/nsi2/java/common/etc/nsi-trust.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        //System.setProperty("javax.net.ssl.keyStorePassword", "abcdef");

    }

    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public LambdaLinkFactoryImplNSI2(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI2.logger.debug("Created LambdaLinkFactoryImplNSI2");

        try {
            requesterURI = "http://" + getIp() + ":" + requesterPort.getAndIncrement();
        } catch (Exception e) {
            requesterURI = null;
        }

        if (requesterURI == null) {
            System.out.println("replyTo will be null. You must use QuerySync to get status");
            hasRequester = false;
        } else {
            hasRequester = true;
        }

        long replyWaitMsec = 60 * 1000L; // [msec]

        if (OAuth != null) {
        client = new NSI2Client(providerNSA, providerURI, requesterNSA, requesterURI, replyWaitMsec,
                OAuth, listener);
        } else { // HTTP auth
            client = new NSI2Client(providerNSA, providerURI,
                    requesterNSA, requesterURI, replyWaitMsec,
                    httpUser, httpPassword, listener);
        }
        connectionId = null;
    }

    private ReservationRequestCriteriaType makeDummyReservationRequestCriteriaType(LambdaLink lambdaLink, long capacity, int srcvlan, int destvlan, Calendar start, Calendar end) {
        capacity = 250; // TODO: remove
        //
        ScheduleType schedule = TypesBuilder.makeScheduleType(start, end);
        LambdaLinkEndPointNSI2 fromEndpoint = (LambdaLinkEndPointNSI2) lambdaLink.getFromLambdaLinkEndPoint();
        StpType srcstp = TypesBuilder.makeStpType(fromEndpoint.getLambdaLinkNetworkId(), fromEndpoint.getLambdaLinkLocalId());
        LambdaLinkEndPointNSI2 toEndpoint = (LambdaLinkEndPointNSI2) lambdaLink.getToLambdaLinkEndPoint();
        StpType deststp = TypesBuilder.makeStpType(toEndpoint.getLambdaLinkNetworkId(), toEndpoint.getLambdaLinkLocalId());
        ReservationRequestCriteriaType crit =
                TypesBuilder.makeReservationRequestCriteriaType(schedule, srcstp, srcvlan, deststp,
                        destvlan, capacity);
        crit.setVersion(0); // SurfNET does not accept version==null, despite NSI2 specs allows it
        return crit;
    }

    // DUMMY VALUE, can be null
    private final static String DEFAULT_DESCRIPTION = "CoUniverse lambda reservation.";
    private final static int TIME_ADJUST = 10; // TODO: for testing only, needs to be at least >5 minutes because prolongation of reservation is 5 min before end

    private int currentVersion = 0;

    public void allocate(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI2.logger.debug("Begin LambdaLinkFactoryImplNSI2.allocate()");

        // NOTE: connectionId must be null for first reservation
        String connectionId = null;
        String globalReservationId = lambdaLink.lambdaReservationId;
        String description = DEFAULT_DESCRIPTION;

        int srcvlan = 0;
        int dstvlan = 0;
        if (lambdaLink.getFromLambdaLinkEndPoint().isLambdaLinkEndpointTagged())
            srcvlan = Integer.parseInt(lambdaLink.getFromLambdaLinkEndPoint().getLambdaLinkEndpointVlan());
        if (lambdaLink.getToLambdaLinkEndPoint().isLambdaLinkEndpointTagged())
            dstvlan = Integer.parseInt(lambdaLink.getToLambdaLinkEndPoint().getLambdaLinkEndpointVlan());

        Calendar start = Calendar.getInstance();
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MINUTE, TIME_ADJUST);
        start.add(Calendar.SECOND, 20);
        lambdaLink.setLastAllocationAttempt(end.getTimeInMillis());

        ReservationRequestCriteriaType criteria = makeDummyReservationRequestCriteriaType(lambdaLink,
                new Double(lambdaLink.getBandwidth()).longValue(), srcvlan, dstvlan, start, end);

        LambdaLinkFactoryImplNSI2.logger.info("Allocating lambda link: " + lambdaLink);
        LambdaLinkFactoryImplNSI2.logger.info("New lambda link criteria: " + NSITextDump.toString(criteria));

        ReserveReply reply = null;
        try {
            LambdaLinkFactoryImplNSI2.logger.debug("Lambda reserve");
            reply = client.reserve(connectionId, globalReservationId, description, criteria);

            if (reply.getConfirm() != null) {
                ReservationConfirmCriteriaType conf = reply.getConfirm();
                LambdaLinkFactoryImplNSI2.logger.info("Allocated lambda link " + reply.getConnectionId()
                        + ": " + conf);
                currentVersion = conf.getVersion();
            } else if (reply.getServiceException() != null) {
                LambdaLinkFactoryImplNSI2.logger.info("Failed to allocate lambda link: "
                        + reply.getServiceException() + ". State "
                        + reply.getConnectionStates());
                return;
            }
            connectionId = reply.getConnectionId();

            LambdaLinkFactoryImplNSI2.logger.info("Lambda connection ID: " + connectionId);

            ReserveCommitReply reserveReply = client.reserveCommit(connectionId);
            if (reserveReply.getServiceException() == null) {
                LambdaLinkFactoryImplNSI2.logger.info("ReserveCommitConfirmed");
            } else if (reserveReply.getServiceException() != null) {
                LambdaLinkFactoryImplNSI2.logger.info("Failed to commit reserve request: "
                        + reply.getServiceException() + ". State "
                        + reply.getConnectionStates());
                return;
            }
            client.provision(connectionId);

        } catch (ServiceException e) {
            LambdaLinkFactoryImplNSI2.logger.error("Lambda link reservation failed:\n" + e + "\n" + e.getStackTrace() +
            "Fault info:\n" + e.getFaultInfo());
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SOAPFaultException e) {
            LambdaLinkFactoryImplNSI2.logger.error("Lambda link reservation failed:\n" + e + "\n" +
                    "Fault code:\n" + e.getFault().getFaultCode());
            e.printStackTrace();
        }
    }

    public void deallocate(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI2.logger.info("Deallocating lambda link: " + lambdaLink);
        try {
            client.release(connectionId);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        try {
            client.terminate(connectionId);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    public void modify(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI2.logger.info("Modyfying lambda link: " + lambdaLink);

        ScheduleType schedule;
        Calendar start = Calendar.getInstance();
        Calendar end = (Calendar) start.clone();
        start.add(Calendar.SECOND, 20); // it seems that SurfNET does not allow past timestamps (it becomes past before it
        end.add(Calendar.MINUTE, TIME_ADJUST);  // this is just to make the reservations go away faster
        schedule = TypesBuilder.makeScheduleType(start, end);

        lambdaLink.setLastAllocationAttempt(end.getTimeInMillis());

        ReservationRequestCriteriaType criteria =
                TypesBuilder.makeReservationRequestCriteriaType(schedule,
                    (new Double(lambdaLink.bandwidth)).longValue());
        criteria.setVersion(++currentVersion);

        LambdaLinkFactoryImplNSI2.logger.info("New lambda link criteria: " + criteria);
        try {
            ReserveReply reply =
                    client.reserve(connectionId, lambdaLink.lambdaReservationId, DEFAULT_DESCRIPTION, criteria);
            if (reply.getConfirm() != null) {
                ReservationConfirmCriteriaType conf = reply.getConfirm();
                LambdaLinkFactoryImplNSI2.logger.info("Reservation change succeeded: "
                        + conf);
                currentVersion = conf.getVersion();

                LambdaLinkFactoryImplNSI2.logger.info("  GRI: " + lambdaLink.lambdaReservationId);
                LambdaLinkFactoryImplNSI2.logger.info("  Status: " + reply.getConnectionStates().getReservationState().value());
                LambdaLinkFactoryImplNSI2.logger.info("  New startTime: " + start);
                LambdaLinkFactoryImplNSI2.logger.info("  New endTime: " + end);

            } else if (reply.getServiceException() != null) {
                LambdaLinkFactoryImplNSI2.logger.info("Lambda link reallocation failed: "
                        + reply.getServiceException());
                LambdaLinkFactoryImplNSI2.logger.info("Lambda link reallocation failed: "
                        + reply.getConnectionStates());
            }
        } catch (ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void query(LambdaLink lambdaLink) {
        LambdaLink.Status lambdaStatus = LambdaLink.Status.UNKNOWN;
        QuerySummaryConfirmedType summary = null;

        QueryType queryType = new QueryType();
        queryType.getConnectionId().add(connectionId);
        // QuerySummarySync
        try {
            summary = client.querySummarySync(queryType);
            LambdaLinkFactoryImplNSI2.logger.info("Querying lambda link: " + lambdaLink);

            if (summary != null) {
                List<QuerySummaryResultType> reservationsStatus =  Collections.unmodifiableList(summary.getReservation());

                if (reservationsStatus.size() > 0) {
                    QuerySummaryResultType reservation = reservationsStatus.get(0);

                    if (reservation.getConnectionStates().getDataPlaneStatus().isActive()) {
                        lambdaStatus = LambdaLink.Status.ACTIVE;
                    } else if (reservation.getConnectionStates().getLifecycleState() == LifecycleStateEnumType.PASSED_END_TIME) {
                        lambdaStatus = LambdaLink.Status.FINISHED;
                    } else if (reservation.getConnectionStates().getLifecycleState() == LifecycleStateEnumType.FAILED) {
                        lambdaStatus = LambdaLink.Status.FAILED;
                    } else {
                        lambdaStatus = LambdaLink.Status.PENDING;
                    } // unhandled LambdaLink.Status.CANCELLED

                    LambdaLinkFactoryImplNSI2.logger.info("GRI: " + reservation.getGlobalReservationId());
                    LambdaLinkFactoryImplNSI2.logger.info("Status: " + lambdaStatus);
                }
            }
        } catch (QuerySummarySyncFailed querySummarySyncFailed) {
            querySummarySyncFailed.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            LambdaLinkFactoryImplNSI2.logger.info("Querying lambda link failed");
        }

        lambdaLink.status = lambdaStatus;
    }

    public void queryAndModify(LambdaLink lambdaLink) {
        query(lambdaLink);

        long currentEndTime = lambdaLink.getLastAllocationAttempt();
        long now = System.currentTimeMillis();

        if (now > currentEndTime) {
            LambdaLinkFactoryImplNSI2.logger.warn("Cannot modify lambda link " + lambdaLink + " in state FINISHED.");
        } else if (currentEndTime - now < 5 * 60 * 1000) {
            this.modify(lambdaLink);
        }
    }
}
