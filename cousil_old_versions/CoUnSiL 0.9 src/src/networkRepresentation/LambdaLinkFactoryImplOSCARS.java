package networkRepresentation;

import net.es.oscars.client.Client;
import net.es.oscars.wsdlTypes.*;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 15.1.2009
 * Time: 15:15:59
 */
public class LambdaLinkFactoryImplOSCARS implements LambdaLinkAbstractFactory {
    static Logger logger = Logger.getLogger("NetworkRepresentation");

    private Client oscarsClient;

    public LambdaLinkFactoryImplOSCARS(LambdaLink lambdaLink) {
        LambdaLinkEndPointOSCARS lambdaLinkEndPointOSCARS = (LambdaLinkEndPointOSCARS) lambdaLink.getFromLambdaLinkEndPoint();
        String url = ((LambdaLinkEndPointOSCARS) lambdaLink.getFromLambdaLinkEndPoint()).getLambdaLinkEndpointIDC();
        // Axis2 repo must be in $CWD or in $CLASSPATH
        String repo = "repo";

        this.oscarsClient = new Client();

        try {
            this.oscarsClient.setUp(true, url, repo);
        } catch (AxisFault e) {
            e.printStackTrace();
        }
    }

    public void allocate(LambdaLink lambdaLink) {
        ResCreateContent request = new ResCreateContent();
        PathInfo pathInfo = new PathInfo();
        Layer2Info layer2Info = new Layer2Info();

        /* Set request parameters */
        layer2Info.setSrcEndpoint(((LambdaLinkEndPointOSCARS) lambdaLink.getFromLambdaLinkEndPoint()).getLambdaLinkEndpoint());
        layer2Info.setDestEndpoint(((LambdaLinkEndPointOSCARS) lambdaLink.getToLambdaLinkEndPoint()).getLambdaLinkEndpoint());

        VlanTag srcVtag = new VlanTag();
        srcVtag.setString(lambdaLink.getFromLambdaLinkEndPoint().getLambdaLinkEndpointVlan());
        srcVtag.setTagged(lambdaLink.getFromLambdaLinkEndPoint().isLambdaLinkEndpointTagged());
        layer2Info.setSrcVtag(srcVtag);

        VlanTag destVtag = new VlanTag();
        destVtag.setString(lambdaLink.getToLambdaLinkEndPoint().getLambdaLinkEndpointVlan());
        destVtag.setTagged(lambdaLink.getToLambdaLinkEndPoint().isLambdaLinkEndpointTagged());
        layer2Info.setDestVtag(destVtag);

        pathInfo.setPathSetupMode("timer-automatic");
        request.setStartTime(System.currentTimeMillis() / 1000);
        request.setEndTime(System.currentTimeMillis() / 1000 + 60 * 60);
        // TODO: is this typcasting correct?
        request.setBandwidth((int) lambdaLink.bandwidth);
        request.setDescription("CoUniverse lambda reservation.");
        pathInfo.setLayer2Info(layer2Info);
        request.setPathInfo(pathInfo);

        LambdaLinkFactoryImplOSCARS.logger.info("Allocating lambda link: " + lambdaLink);
        try {
            /* Alocate the lambda */
            CreateReply response = this.oscarsClient.createReservation(request);

            lambdaLink.lambdaReservationId = response.getGlobalReservationId();
            /* Print repsponse information */
            LambdaLinkFactoryImplOSCARS.logger.info("  GRI: " + lambdaLink.lambdaReservationId);
            LambdaLinkFactoryImplOSCARS.logger.info("  Status: " + response.getStatus());

            // TODO I2 winter joint techs hack
            while (!(lambdaLink.status.equals("ACTIVE") || lambdaLink.status.equals("FINISHED"))) {
                this.query(lambdaLink);
                TimeUtils.sleepFor(5000);
            }
            lambdaLink.isAllocated = true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        assert (lambdaLink.lambdaReservationId != null);

        LambdaLinkFactoryImplOSCARS.logger.info("Lambda link " + lambdaLink + " allocated with GRI: " + lambdaLink.lambdaReservationId);

        this.oscarsClient.cleanUp();
    }

    public void deallocate(LambdaLink lambdaLink) {
        if (!lambdaLink.status.equals("FINISHED")) {
            /* Send teardownPathContent request and print response */
            String cancelResponse;
            LambdaLinkFactoryImplOSCARS.logger.info("Deallocating lambda link: " + lambdaLink);
            if (lambdaLink.getLambdaReservationId() != null) {
                try {
                    GlobalReservationId gri = new GlobalReservationId();
                    gri.setGri(lambdaLink.lambdaReservationId);
                    cancelResponse = this.oscarsClient.cancelReservation(gri);
                    LambdaLinkFactoryImplOSCARS.logger.info("  GRI: " + lambdaLink.lambdaReservationId);
                    LambdaLinkFactoryImplOSCARS.logger.info("  Status: " + cancelResponse);

                    lambdaLink.isAllocated = false;
                    this.oscarsClient.cleanUp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                LambdaLinkFactoryImplOSCARS.logger.debug("Lambda link was not allocated yet.");
            }
        } else {
            LambdaLinkFactoryImplOSCARS.logger.warn("Cannot cancel lambda link " + lambdaLink + " reservation with status FINISHED");
        }
    }

    public void modify(LambdaLink lambdaLink) {
        LambdaLinkFactoryImplOSCARS.logger.info("Modyfying lambda link: " + lambdaLink);

        ModifyResReply modifyResponse;
        if (lambdaLink.getLambdaReservationId() != null) {
            try {
                GlobalReservationId gri = new GlobalReservationId();
                gri.setGri(lambdaLink.lambdaReservationId);

                ModifyResContent request = new ModifyResContent();

                request.setGlobalReservationId(lambdaLink.lambdaReservationId);
                request.setBandwidth((int) lambdaLink.bandwidth);
                request.setDescription("CoUniverse lambda reservation.");
                // TODO set the times
                request.setStartTime(System.currentTimeMillis() / 1000);
                request.setEndTime(System.currentTimeMillis() / 1000 + 60 * 60);

                modifyResponse = this.oscarsClient.modifyReservation(request);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                date.setTime(modifyResponse.getReservation().getStartTime() * 1000L);
                String startTime = df.format(date);

                date.setTime(modifyResponse.getReservation().getEndTime() * 1000L);
                String endTime = df.format(date);

                LambdaLinkFactoryImplOSCARS.logger.info("  GRI: " + lambdaLink.lambdaReservationId);
                LambdaLinkFactoryImplOSCARS.logger.info("  Status: " + modifyResponse.getReservation().getStatus());
                LambdaLinkFactoryImplOSCARS.logger.info("  New startTime: " + startTime);
                LambdaLinkFactoryImplOSCARS.logger.info("  New endTime: " + endTime);

                this.oscarsClient.cleanUp();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LambdaLinkFactoryImplOSCARS.logger.debug("Lambda link was not allocated yet.");
        }
    }

    public void query(LambdaLink lambdaLink) {
        /* Send refreshPathContent request and print response */
        ResDetails queryResponse;
        String lambdaStatus = "UNKNOWN";

        // TODO move query logs to debug
        LambdaLinkFactoryImplOSCARS.logger.info("Querying lambda link: " + lambdaLink);
        if (lambdaLink.getLambdaReservationId() != null) {
            try {
                GlobalReservationId gri = new GlobalReservationId();
                gri.setGri(lambdaLink.lambdaReservationId);
                queryResponse = this.oscarsClient.queryReservation(gri);
                lambdaStatus = queryResponse.getStatus();
                LambdaLinkFactoryImplOSCARS.logger.info("GRI: " + queryResponse.getGlobalReservationId());
                LambdaLinkFactoryImplOSCARS.logger.info("Status: " + lambdaStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LambdaLinkFactoryImplOSCARS.logger.debug("Lambda link was not allocated yet.");
        }

        this.oscarsClient.cleanUp();

        try
        {
            lambdaLink.status = Enum.valueOf(LambdaLink.Status.class, lambdaStatus);
        }
        catch(IllegalArgumentException ex)
        {
            lambdaLink.status = LambdaLink.Status.PENDING;
        }
    }

    public void queryAndModify(LambdaLink lambdaLink) {
        ResDetails queryResponse;
        String lambdaStatus = "UNKNOWN";

        if (lambdaLink.getLambdaReservationId() != null) {
            try {
                GlobalReservationId gri = new GlobalReservationId();
                gri.setGri(lambdaLink.lambdaReservationId);
                queryResponse = this.oscarsClient.queryReservation(gri);
                lambdaStatus = queryResponse.getStatus();
                LambdaLinkFactoryImplOSCARS.logger.info("GRI: " + queryResponse.getGlobalReservationId());
                LambdaLinkFactoryImplOSCARS.logger.info("Status: " + lambdaStatus);

                long currentEndTime = queryResponse.getEndTime();
                long now = System.currentTimeMillis() / 1000;

                if (now > currentEndTime) {
                    LambdaLinkFactoryImplOSCARS.logger.warn("Cannot modify lambda link " + lambdaLink + " in state FINISHED.");
                } else if (currentEndTime - now < 5 * 60) {
                    this.modify(lambdaLink);
                }

                this.oscarsClient.cleanUp();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LambdaLinkFactoryImplOSCARS.logger.debug("Lambda link was not allocated yet.");
        }

        try
        {
            lambdaLink.status = Enum.valueOf(LambdaLink.Status.class, lambdaStatus);
        }
        catch(IllegalArgumentException ex)
        {
            lambdaLink.status = LambdaLink.Status.PENDING;
        }
    }
}
