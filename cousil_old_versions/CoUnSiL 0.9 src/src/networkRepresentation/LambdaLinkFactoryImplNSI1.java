package networkRepresentation;

import java.util.Calendar;
import net.glambda.gridars.nsi.util.ConnectionId;
import net.glambda.gridars.nsi.util.CorrelationId;
import net.glambda.gridars.nsi.util.NSITextDump;
import nsi.NSIClient;
import org.apache.log4j.Logger;
import org.ogf.schemas.nsi._2011._10.connection._interface.*;
import org.ogf.schemas.nsi._2011._10.connection.types.*;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 6.9.13
 * Time: 10:02
 * To change this template use File | Settings | File Templates.
 */
public class LambdaLinkFactoryImplNSI1 implements LambdaLinkAbstractFactory {
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    private NSIClient client;

    private static final long timeout = 120 * 1000L; // [msec] // DUMMY

    private String requesterURI = null; // can this be null? in NSI2 api reportedly so
    private String reqNSA = "urn:ogf:network:nsa:aist"; // DUMMY
    private String provNSA = "urn:ogf:network:nsa:aist"; // DUMMY
    private String connId;
    private String globalId;

    public LambdaLinkFactoryImplNSI1(LambdaLink lambdaLink) {
        try {
            client = new NSIClient(requesterURI, "http://provider.uri", null, null);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException();
        }
    }

    private static ReserveRequestType makeReserveReq(String replyTo, String reqNSA, String provNSA,
                                                     String globalId, String connId, String desc, Calendar start, Calendar end, int bwMbps,
                                                     String src, String dest) {
        ReserveRequestType req = new ReserveRequestType();
        req.setCorrelationId(CorrelationId.getNewId());
        req.setReplyTo(replyTo);
        //
        org.ogf.schemas.nsi._2011._10.connection.types.ReserveType rsv = new org.ogf.schemas.nsi._2011._10.connection.types.ReserveType();
        req.setReserve(rsv);
        rsv.setRequesterNSA(reqNSA);
        rsv.setProviderNSA(provNSA);
        //
        ReservationInfoType info = new ReservationInfoType();
        rsv.setReservation(info);
        info.setGlobalReservationId(globalId);
        info.setDescription(desc);
        info.setConnectionId(connId);
        //
        ServiceParametersType sp = new ServiceParametersType();
        info.setServiceParameters(sp);
        //
        org.ogf.schemas.nsi._2011._10.connection.types.ScheduleType sched = new org.ogf.schemas.nsi._2011._10.connection.types.ScheduleType();
        sp.setSchedule(sched);
        sched.setStartTime(start);
        sched.setEndTime(end);
        //
        BandwidthType bw = new BandwidthType();
        sp.setBandwidth(bw);
        bw.setDesired(bwMbps);
        //
        PathType path = new PathType();
        info.setPath(path);
        path.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        ServiceTerminationPointType ap = new ServiceTerminationPointType();
        path.setSourceSTP(ap);
        ap.setStpId(src);
        ServiceTerminationPointType zp = new ServiceTerminationPointType();
        path.setDestSTP(zp);
        zp.setStpId(dest);

        return req;
    }

    private static ReserveRequestType makeDummyReserveReq(String replyTo, String reqNSA,
                                                          String provNSA, String globalId, String connId,
                                                          long capacity, int srcvlan, int destvlan, Calendar start, Calendar end) {
        String desc = "CoUniverse lambda reservation.";
        String src = "urn:ogf:network:stp:aist.ets:ps-80"; // DUMMY VALUE
        String dest = "urn:ogf:network:stp:aist.ets:tok-80"; // DUMMY VALUE
        return makeReserveReq(replyTo, reqNSA, provNSA, globalId, connId, desc, start, end,
                (int) (capacity / 1000 / 1000),
                src, dest);
    }

    private static GenericRequestType makeGenericReq(String reqNSA, String provNSA, String connId) {
        GenericRequestType gr = new GenericRequestType();
        gr.setRequesterNSA(reqNSA);
        gr.setProviderNSA(provNSA);
        gr.setConnectionId(connId);
        gr.setSessionSecurityAttr(null);
        return gr;
    }

    private static ProvisionRequestType makeProvisionReq(String replyTo, String reqNSA,
                                                         String provNSA, String connId) {
        ProvisionRequestType req = new ProvisionRequestType();
        req.setCorrelationId(CorrelationId.getNewId());
        req.setReplyTo(replyTo);
        req.setProvision(makeGenericReq(reqNSA, provNSA, connId));
        return req;
    }

    public void reserve(LambdaLink lambdaLink) {


        Calendar start = Calendar.getInstance(); // DUMMY VALUE
        Calendar end = Calendar.getInstance();
        end.add(Calendar.MINUTE, 60); // DUMMY VALUE

        ReserveRequestType reserveReq = makeDummyReserveReq(requesterURI, reqNSA, provNSA, globalId, connId,
                new Double(lambdaLink.getBandwidth()).longValue(), 0, 0, start, end);
        LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString(reserveReq));
        Object reserverReply = null;
        try {
            reserverReply = client.reserve(reserveReq, timeout);
        } catch (org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException();
        }
        if (reserverReply instanceof ReserveConfirmedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString((ReserveConfirmedRequestType) reserverReply));
        } else if (reserverReply instanceof ReserveFailedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.error(NSITextDump.toString((ReserveFailedRequestType) reserverReply));
        } else {
            System.err.println("TIMEOUT: cannot receive reply");
        }
    }

    public void provision() {
        ProvisionRequestType provisionReq = makeProvisionReq(requesterURI, reqNSA, provNSA, connId);
        LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString(provisionReq));
        // NOTE: ProvisionConf/Failed will be sent after startTime if this
        // method is called before startTime (i.e. Auto provision).
        // timeout=0 means not to wait
        Object provisionReply = null;
        try {
            provisionReply = client.provision(provisionReq, timeout);
        } catch (org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException();
        }
        if (provisionReply instanceof ProvisionConfirmedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString((ProvisionConfirmedRequestType) provisionReply));
        } else if (provisionReply instanceof ProvisionFailedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.error(NSITextDump.toString((ProvisionFailedRequestType) provisionReply));
        } else {
            System.err.println("TIMEOUT: cannot receive reply");
        }
    }

    public void allocate(LambdaLink lambdaLink) {
        connId = ConnectionId.getNewId();
        globalId = "aist:" + System.currentTimeMillis(); // DUMMY VALUE

        reserve(lambdaLink);
        provision();
    }

    private static ReleaseRequestType makeReleaseReq(String replyTo, String reqNSA, String provNSA,
                                                     String connId) {
        ReleaseRequestType req = new ReleaseRequestType();
        req.setCorrelationId(CorrelationId.getNewId());
        req.setReplyTo(replyTo);
        req.setRelease(makeGenericReq(reqNSA, provNSA, connId));
        return req;
    }

    private static void release(NSIClient client, String replyTo, String reqNSA,
                                    String provNSA, String connId, long timeout) throws org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException {
        ReleaseRequestType req = makeReleaseReq(replyTo, reqNSA, provNSA, connId);
        LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString(req));
        Object reply = client.release(req, timeout);
        if (reply instanceof ReleaseConfirmedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString((ReleaseConfirmedRequestType) reply));
        } else if (reply instanceof ReleaseFailedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.error(NSITextDump.toString((ReleaseFailedRequestType) reply));
        } else {
            System.err.println("TIMEOUT: cannot receive reply");
        }
    }

    private static TerminateRequestType makeTerminateReq(String replyTo, String reqNSA,
                                                         String provNSA, String connId) {
        TerminateRequestType req = new TerminateRequestType();
        req.setCorrelationId(CorrelationId.getNewId());
        req.setReplyTo(replyTo);
        req.setTerminate(makeGenericReq(reqNSA, provNSA, connId));
        return req;
    }

    private static void terminate(NSIClient client, String replyTo, String reqNSA,
                                      String provNSA, String connId, long timeout) throws org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException {
        TerminateRequestType req = makeTerminateReq(replyTo, reqNSA, provNSA, connId);
        LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString(req));
        Object reply = client.terminate(req, timeout);
        if (reply instanceof TerminateConfirmedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.info(NSITextDump.toString((TerminateConfirmedRequestType) reply));
        } else if (reply instanceof TerminateFailedRequestType) {
            LambdaLinkFactoryImplNSI1.logger.error(NSITextDump.toString((TerminateFailedRequestType) reply));
        } else {
            System.err.println("TIMEOUT: cannot receive reply");
        }
    }

    public void deallocate(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI1.logger.info("Deallocating lambda link: " + lambdaLink);
        try {
            release(client, requesterURI, reqNSA, provNSA, connId, timeout);
            terminate(client, requesterURI, reqNSA, provNSA, connId, timeout);
        } catch (org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void modify(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplNSI1.logger.info("Modyfying lambda link: " + lambdaLink);

        reserve(lambdaLink);
    }

    private static QueryRequestType makeQueryReq(String replyTo, String reqNSA, String provNSA,
                                                 String globalId, String connId) {
        QueryRequestType req = new QueryRequestType();
        req.setCorrelationId(CorrelationId.getNewId());
        req.setReplyTo(replyTo);
        org.ogf.schemas.nsi._2011._10.connection.types.QueryType query = new org.ogf.schemas.nsi._2011._10.connection.types.QueryType();
        req.setQuery(query);
        query.setRequesterNSA(reqNSA);
        query.setProviderNSA(provNSA);
        query.setOperation(QueryOperationType.SUMMARY);
        //
        QueryFilterType filter = new QueryFilterType();
        query.setQueryFilter(filter);
        if (connId != null) {
            filter.getConnectionId().add(connId);
        } else if (globalId != null) {
            filter.getGlobalReservationId().add(globalId);
        }
        return req;
    }

    public void query(LambdaLink lambdaLink) {
        LambdaLink.Status lambdaStatus = LambdaLink.Status.UNKNOWN;
        QueryRequestType req = makeQueryReq(requesterURI, reqNSA, provNSA, globalId, connId);
        LambdaLinkFactoryImplOSCARS.logger.debug("Querying lambda state: " + NSITextDump.toString(req));

        try {
            Object reply = client.query(req, timeout);
            if (reply instanceof QueryConfirmedRequestType) {
                LambdaLinkFactoryImplOSCARS.logger.debug("Lambda queried successfully: " + NSITextDump.toString((QueryConfirmedRequestType) reply));
                QueryConfirmedRequestType replyConfirmed = (QueryConfirmedRequestType) reply;
                ConnectionStateType stateType = replyConfirmed.getQueryConfirmed().getReservationSummary().get(0).getConnectionState();

                switch (stateType) {
                    case INITIAL:
                    case RESERVING:
                    case RESERVED:
                    case SCHEDULED:
                    case PROVISIONING:
                    case RELEASING:
                        lambdaStatus = LambdaLink.Status.PENDING;
                        break;
                    case CLEANING:
                    case TERMINATING:
                    case TERMINATED:
                        lambdaStatus = LambdaLink.Status.FINISHED;
                        break;

                    case AUTO_PROVISION:
                    case PROVISIONED:
                        lambdaStatus = LambdaLink.Status.ACTIVE;
                        break;
                }
            } else if (reply instanceof QueryFailedRequestType) {
                LambdaLinkFactoryImplOSCARS.logger.error("Lambda query failed: " + NSITextDump.toString((QueryFailedRequestType) reply));
            } else {
                System.err.println("TIMEOUT: cannot receive reply");
            }
        } catch (org.ogf.schemas.nsi._2011._10.connection._interface.ServiceException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        lambdaLink.status = lambdaStatus;
    }

    public void queryAndModify(LambdaLink lambdaLink) {
        query(lambdaLink);

        long currentEndTime = lambdaLink.getLastAllocationAttempt();
        long now = System.currentTimeMillis();

        if (now > currentEndTime) {
            LambdaLinkFactoryImplOSCARS.logger.warn("Cannot modify lambda link " + lambdaLink + " in state FINISHED.");
        } else if (currentEndTime - now < 5 * 60 * 1000) {
            this.modify(lambdaLink);
        }
    }
}
