package org.janelia.it.jacs.compute.wsrest.process;

import java.rmi.RemoteException;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.status.CurrentTaskStatus;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.LSMProcessingTask;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.apache.log4j.Logger;

/**
 * Defines RESTful web service entry points.
 *
 * <p>
 * The web-rest-ws.xml file references the resteasy components that are
 * used to load an instance of this class as a JAX-RS service.
 * </p>
 * <p>
 * The application.xml file defines the root context for this service.
 * A root context of "/rest-v1" would make resources available at: <br/>
 * http://[compute-server]:8180/rest-v1/[annotated path]
 * (e.g. http://jacs:8180/rest-v1/dataSet ).
 * </p>
 *
 * @author Eric Trautman
 */
@Path("/process")
@Api(value = "Janelia Workstation Pipelines")
public class SAGEWebService extends ResourceConfig {
    private final Logger logger = Logger.getLogger(this.getClass());

    @Context
    SecurityContext securityContext;

    public SAGEWebService() {
        register(JacksonFeature.class);
    }


    /**
     * Runs the Informatics sageLoader script.
     *
     * Sample POST request URL:
     *
     *   http://saffordt-ws1:8180/rest-v1/sageLoader?
     *       owner=system&
     *       item=20121219%2FFLFL_20121221182844754_29687.lsm&
     *       config=%2Fgroups%2Fscicomp%2Finformatics%2Fdata%2Fflylightflip_light_imagery-config.xml&
     *       grammar=%2Fusr%2Flocal%2Fpipeline%2Fgrammar%2Fflylightflip.gra&
     *       lab=flylight&
     *       debug=yes
     *
     * @param  owner        id of the person or system submitting this request.
     *
     * @param  item         the normalized (forward slash) relative path of the item to load
     *                      (e.g. 20121219/FLFL_20121221182844754_29687.lsm).
     *
     * @param  line         optional line for the item (e.g. GMR_10A01_AE_01).
     *                      This is only used in for testing in development environments
     *                      to work around the hard-coded production SAGE database connection
     *                      in the informatics get_sage_line_name script which is used by many grammars.
     *
     * @param  configPath   the normalized (forward slash) absolute path of the loader configuration file
     *                      (e.g. /groups/scicomp/informatics/data/flylightflip_light_imagery-config.xml).
     *
     * @param  grammarPath  the normalized (forward slash) absolute path of the loader grammar file
     *                      (e.g. /usr/local/pipeline/grammar/flylightflip.gra).
     *
     * @param  lab          name of the lab that owns the item being loaded
     *                      (e.g. flylight or rubin).
     *
     * @param  debug        optional flag indicating that debug information should be printed to stdout.
     *
     * @param  lockPath     optional lock file path.
     *                      If specified, subsequent requests with the same lock file path will be blocked
     *                      (on the cluster) for up to 60 seconds.
     *                      If lock is not released within 60 seconds, job will fail.
     *                      Lock file must be on network filesystem to work as it will be accessed from
     *                      different cluster nodes.
     *
     * @param  uriInfo      URI information for the current request.
     *
     * @return HTTP accepted (202) status with task current status information that includes
     *         link to check for status later.
     *
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    @Path("/sageLoader")
    public Response runSageLoader(
            @QueryParam("owner")String owner,
            @QueryParam("item")String item,
            @QueryParam("line")String line,
            @QueryParam("config")String configPath,
            @QueryParam("grammar")String grammarPath,
            @QueryParam("lab")String lab,
            @QueryParam("debug")String debug,
            @QueryParam("lock")String lockPath,
            @Context UriInfo uriInfo) {

        final String context = "runSageLoader: ";

        logger.info(context +"entry, owner=" + owner +
                ", item=" + item +
                ", line=" + line +
                ", configPath=" + configPath +
                ", grammarPath=" + grammarPath +
                ", lab=" + lab +
                ", debug=" + debug +
                ", lockPath=" + lockPath);

        Response response;
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();

            if (owner == null) {
                throw new IllegalArgumentException("owner parameter is not defined");
            } else {
                final User user = remoteComputeBean.getUserByNameOrKey(owner);
                if (user == null) {
                    throw new IllegalArgumentException("invalid owner parameter '" + owner + "' specified");
                }
            }

            SageLoaderTask task = new SageLoaderTask(owner,
                    new ArrayList<Event>(),
                    item,
                    line,
                    configPath,
                    grammarPath,
                    lab,
                    debug,
                    lockPath);

            task = (SageLoaderTask) remoteComputeBean.saveOrUpdateTask(task);
            final Long taskId = task.getObjectId();
            logger.info(context + "task " + taskId + " saved for item " + item);

            remoteComputeBean.submitJob("SageLoader", task.getObjectId());
            logger.info(context + "submitted job for task " + taskId);

            response = getCurrentTaskStatus(taskId, uriInfo);
            final Object taskStatusEntity = response.getEntity();

            if (taskStatusEntity instanceof CurrentTaskStatus) {
                response = Response.status(Response.Status.ACCEPTED).entity(taskStatusEntity).build();
            }
            // else simply return error response from getCurrentTaskStatus

        } catch (IllegalArgumentException e) {
            response = getErrorResponse(context, Response.Status.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            response = getErrorResponse(context,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "failed to run sageLoader for " + item,
                    e);
        }

        logger.info(context + "exit, returning " + getResponseString(response));

        return response;
    }

    /**
     * Create and launch pipeline processing tasks for the samples associated with a list of lsm files.
     *
     * @param  owner id of the person or system submitting this request
     *
     * @param lsmProcessingParams input parameters encapsulated in a LSMProcessingTask
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/lsmPipelines")
    public Response launchLsmPipelines(
            @QueryParam("owner") String owner,
            LSMProcessingTask lsmProcessingParams) {

        final String context = "launchLsmPipelines: ";
        LSMProcessingTask lsmProcessingTask;
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();

            if (owner == null) {
                throw new IllegalArgumentException("owner parameter is not defined");
            } else {
                final User user = remoteComputeBean.getUserByNameOrKey(owner);
                if (user == null) {
                    throw new IllegalArgumentException("invalid owner parameter '" + owner + "' specified");
                }
            }
            lsmProcessingParams.setOwner(owner);
            lsmProcessingTask = (LSMProcessingTask) remoteComputeBean.saveOrUpdateTask(lsmProcessingParams);
            remoteComputeBean.dispatchJob(lsmProcessingTask.getJobName(), lsmProcessingTask.getObjectId());
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument", e);
            Response response = getErrorResponse(context, Response.Status.BAD_REQUEST, e.getMessage(), e);
            return response;
        } catch (RemoteException | ComputeException e) {
            logger.error("LSM Processing exception", e);
            Response response = getErrorResponse(context,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "failed to run lsm processing for " + owner + ":" + lsmProcessingParams,
                    e);
            return response;
        }
        return Response
                .status(Response.Status.CREATED)
                .entity(new JsonTask(lsmProcessingTask))
                .build();
    }

    /**
     * Deletes all samples associated with the specifed data set and slide code.
     * This supports a temporary solution that allows tmog to delete projecttechres samples once the techs
     * have completed their quality control checks.
     * In the future, we expect to manage the quality control workflow inside the workstation (without tmog).
     * This API can be removed at that time.
     *
     * @param  owner      id of the person or system submitting this request
     *                    (currently must be 'projtechres' to ensure we don't inadvertently remove important data).
     *
     * @param  dataSet    data set for the sample(s) to be removed.
     *
     * @param  slideCode  slide code for the sample(s) to be removed.
     */
    @DELETE
    @Produces(MediaType.APPLICATION_XML)
    @Path("/samplesWithSlideCode")
    public Response removeSlideCodeSamplesFromDataSet(
            @QueryParam("owner")String owner,
            @QueryParam("dataSet")String dataSet,
            @QueryParam("slideCode")String slideCode) {

        final String context = "removeSlideCodeSamplesFromDataSet: ";

        logger.info(context + "entry, owner=" + owner +
                ", dataSet=" + dataSet + ", slideCode=" + slideCode);

        Response response;
        try {

            if (owner == null) {
                throw new IllegalArgumentException("owner parameter is not defined");
            } else if (! owner.equals("projtechres")) {
                throw new IllegalArgumentException("only projtechres samples may be removecd");
            }

            if (dataSet == null) {
                throw new IllegalArgumentException("dataSet parameter is not defined");
            }

            if (slideCode == null) {
                throw new IllegalArgumentException("slideCode parameter is not defined");
            }

            final ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            final Subject subject = computeBean.getSubjectByNameOrKey(owner);

            if (subject == null) {
                throw new IllegalArgumentException("owner '" + owner + "' does not exist");
            }

            final String subjectKey = subject.getKey();

            DomainDAL dal = DomainDAL.getInstance();
            final Sample slideCodeSample = DomainDAL.getInstance().getSampleBySlideCode(subjectKey, dataSet, slideCode);

            logger.info(context + "found " + slideCodeSample + " for slide code " + slideCode +
                    " (subjectKey is " + subjectKey + ")");

            dal.deleteDomainObject(subjectKey, slideCodeSample);
            logger.info(context + "deleted sample entity " + slideCodeSample.getId());
            response = Response.status(Response.Status.OK).build();

        } catch (IllegalArgumentException e) {
            response = getErrorResponse(context, Response.Status.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            response = getErrorResponse(context,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "failed to remove " + dataSet + " samples for slideCode " + slideCode,
                    e);
        }

        logger.info(context + "exit, returning " + getResponseString(response));

        return response;
    }

    private String getNormalizedBaseUrlString(UriInfo uriInfo) {
        StringBuilder sb = new StringBuilder(uriInfo.getBaseUri().toString());
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private Response getErrorResponse(String context, Response.Status status, String errorMessage, Exception e)  {
        final RestfulWebServiceFailure failure = new RestfulWebServiceFailure(errorMessage, e);
        if (e != null) {
            logger.error(context + errorMessage, e);
        }
        return Response.status(status).entity(failure).build();
    }

    private String getResponseString(Response response) {
        return response.getStatus() + ": " + response.getEntity();
    }

    private Response getCurrentTaskStatus(@PathParam("taskId") Long taskId,
                                         @Context UriInfo uriInfo) {

        final String context = "getCurrentTaskStatus: ";
        logger.info(context + "entry, taskId=" + taskId);
        Response response;
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();
            final Task task = remoteComputeBean.getTaskById(taskId);

            if (task == null) {
                response = getErrorResponse(context,
                        Response.Status.NOT_FOUND,
                        "task " + taskId + " does not exist",
                        null);
            } else {
                final CurrentTaskStatus currentTaskStatus = new CurrentTaskStatus(task);
                final String href = getNormalizedBaseUrlString(uriInfo) + "task/" + taskId + "/currentStatus";
                currentTaskStatus.setHref(href);
                response = Response.status(Response.Status.OK).entity(currentTaskStatus).build();
            }

        } catch (Exception e) {
            response = getErrorResponse(context,
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "failed to retrieve status for task " + taskId,
                    e);
        }
        logger.info(context + "exit, returning " + getResponseString(response));
        return response;
    }
}
