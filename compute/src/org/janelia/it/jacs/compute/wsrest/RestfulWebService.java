package org.janelia.it.jacs.compute.wsrest;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.DataSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.status.CurrentTaskStatus;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.NotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

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
@Path("/")
public class RestfulWebService {

    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * Retrieve LSM summary imagery that match specified LSM's.
     * @param lsms comma separated list of LSM names
     *
     * @return list of data sets that match the specified filter criteria.
     *
     * @throws NotFoundException
     *   if no matching data sets can be found.
     */
//    @GET
//    @Path("lsm")
//    @Produces(MediaType.APPLICATION_XML)
//    @Formatted
//    public Response getSummaryFilesForLSMs(@QueryParam("lsms")String lsms) {
//        final String context = "getSummaryFilesForLSMs: ";
//
//        logger.info(context +"lsms=" + lsms);
//
//        Response response;
//        StringBuilder paths = new StringBuilder();
//        try {
//            final EntityBeanLocal localEntityBean = EJBFactory.getLocalEntityBean();
//            if (null==lsms || lsms.length()==0) {
//                throw new IllegalArgumentException("LSM list cannot be empty");
//            }
//
//            String[] lsmFileNames = lsms.split(",");
//            List<String> lsmNames = new ArrayList<>();
//            for (String lsmFileName : lsmFileNames) {
//                lsmNames.add(lsmFileName.trim());
//            }
//
//            List<String> summaryFiles = localEntityBean.getSummaryFilesForLSMs(lsmNames);
//            for (String summaryFile : summaryFiles) {
//                paths.append(summaryFile).append("\n");
//            }
//            response = Response.status(Response.Status.ACCEPTED).entity(paths.toString()).build();
//        }
//        catch (IllegalArgumentException e) {
//            response = getErrorResponse(context, Response.Status.BAD_REQUEST, e.getMessage(), e);
//        }
//        catch (Exception e) {
//            response = getErrorResponse(context,
//                    Response.Status.INTERNAL_SERVER_ERROR,
//                    "failed to run getSummaryFilesForLSMs for " + lsms,
//                    e);
//        }
//
//        logger.info(context + "exit, returning " + getResponseString(response));
//
//        return response;
//    }

    /**
     * Retrieve data sets that match specified filter criteria.
     *
     * @param  userList             list of user login names for filter
     *                              (or null if all are desired).
     *
     * @param  includeOnlySageSync  if defined (not null), indicates
     *                              that only sage sync data sets should
     *                              be returned.
     *
     * @return list of data sets that match the specified filter criteria.
     *
     * @throws NotFoundException
     *   if no matching data sets can be found.
     */
    @GET
    @Path("dataSet")
    @Produces(MediaType.APPLICATION_XML)
    @Formatted
    @Wrapped(element = "dataSetList")
    public List<DataSet> getDataSets(
            @QueryParam("user") List<String> userList,
            @QueryParam("includeOnlySageSync") String includeOnlySageSync)
            throws NotFoundException{

        List<Entity> entityList = null;
        try {
            final AnnotationBeanRemote annotationBean =
                    EJBFactory.getRemoteAnnotationBean();
            if ((userList == null) || (userList.size() == 0)) {
                entityList = annotationBean.getAllDataSets();
            } else {
                entityList = annotationBean.getUserDataSets(userList);
            }
        } catch (Exception e) {
            logger.error("getDataSets: failed retrieval, userList=" +
                         userList, e);
        }

        final boolean includeOnlySageSyncFlag =
                Boolean.parseBoolean(includeOnlySageSync);
        final List<DataSet> dataSetList =
                toDataSetList(entityList,
                              includeOnlySageSyncFlag);

        if ((dataSetList == null) || (dataSetList.size() == 0)) {
            StringBuilder msg = new StringBuilder(256);
            msg.append("There are no");
            if (includeOnlySageSyncFlag) {
                msg.append(" SAGE Sync ");
            }
            msg.append(" data sets");
            if ((userList != null) && (userList.size() > 0)) {
                    msg.append(" for the following user(s): ");
                    msg.append(userList);
            }
            msg.append('.');

            throw new NotFoundException(msg.toString());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getDataSets: exit, returning " + dataSetList.size() +
                    " data sets, userList=" + userList +
                    ", includeOnlySageSync=" + includeOnlySageSyncFlag);
        }

        return dataSetList;
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
     * @throws Failure
     *   if any failures occur during processing.
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    @Formatted
    @Path("sageLoader")
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
     * NOTE: This is a placeholder API that does nothing at the moment.
     *       It should ultimately subsume the sageLoader API.
     *
     * @param  owner     id of the person or system submitting this request
     *                   (must have write access to the data set).
     *
     * @param  dataSet   data set for the sample(s) to be removed.
     *
     * @param  lsmPaths  list of lsm file paths for which SAGE data has recently been created or changed.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    @Path("owner/{owner}/dataSet/{dataSet}/lsmPipelines")
    public Response launchLsmPipelines(
            @PathParam("owner")String owner,
            @PathParam("dataSet")String dataSet,
            List<String> lsmPaths) {

        final String context = "launchLsmPipelines: ";

        logger.info(context +"entry, owner=" + owner + ", dataSet=" + dataSet + ", lsmPaths=" + lsmPaths);

        // TODO: derive/pull indexer parameters (configPath, grammar, lab) for data set

        // TODO: pull current sample data from SAGE for specified lsms

        // TODO: find all existing samples for specified lsms

        // TODO: sort samples into new, changed, and unchanged sets

        // TODO: for each changed sample, schedule processing job (retire, remove, index new lsms, rerun pipelines, ...)

        // TODO: for each new sample, schedule processing job (index lsms, run pipelines, ...)

        // TODO: for each scheduled job, add link to response for status checking

        return Response.status(Response.Status.OK).entity("{\"result\": \"TBD\"}").build();
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
    @Formatted
    @Path("owner/{owner}/dataSet/{dataSet}/samplesWithSlideCode/{slideCode}")
    public Response removeSlideCodeSamplesFromDataSet(
            @PathParam("owner")String owner,
            @PathParam("dataSet")String dataSet,
            @PathParam("slideCode")String slideCode) {

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

            final EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
            final List<Entity> slideCodeSamples =
                    entityBean.getUserEntitiesWithAttributeValueAndTypeName(subjectKey,
                                                                            EntityConstants.ATTRIBUTE_SLIDE_CODE,
                                                                            slideCode,
                                                                            EntityConstants.TYPE_SAMPLE);

            logger.info(context + "found " + slideCodeSamples.size() + " samples for slide code " + slideCode +
                        " (subjectKey is " + subjectKey + ")");

            String sampleDataSetIdentifier;
            Long sampleEntityId;
            for (Entity sampleEntity : slideCodeSamples) {
                sampleDataSetIdentifier = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                if (dataSet.equals(sampleDataSetIdentifier)) {
                    sampleEntityId = sampleEntity.getId();
                    entityBean.deleteEntityTreeById(subjectKey, sampleEntityId, true);
                    logger.info(context + "deleted sample entity " + sampleEntityId);
                }
            }

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

    /**
     * @param  taskId   identifies the task to check.
     *
     * @param  uriInfo  URI information for the current request.
     *
     * @return current status information for the specified task.
     *
     * @throws Failure
     *   if the specified task cannot be found or any other failures occur during retrieval.
     */
    @GET
    @Path("task/{taskId}/currentStatus")
    @Produces(MediaType.APPLICATION_XML)
    @Formatted
    public Response getCurrentTaskStatus(@PathParam("taskId") Long taskId,
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

    /**
     * @return list of all supported entity types.
     */
    @GET
    @Path("entityType")
    @Produces(MediaType.APPLICATION_XML)
    @Formatted
    @Wrapped(element = "entityTypeList")
    public List<EntityType> getEntityTypes() {

        List<EntityType> list = null;
        try {
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            list = entityBean.getEntityTypes();
        } catch (Exception e) {
            logger.error("getEntityTypes: failed retrieval", e);
        }

        if (list == null) {
            list = new ArrayList<>();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getEntityTypes: exit, returning " + list.size() +
                         " entity types");
        }

        return list;
    }

    /**
     * Wraps {@link Entity} objects in the specified list as {@link DataSet}
     * objects so that JAXB can marshall the resulting list more clearly.
     *
     * @param  entityList           list of entity objects to wrap.
     * @param  includeOnlySageSync  indicates whether list should be filtered
     *                              to only include entities with a
     *                              defined sage sync attribute.
     *
     * @return list of wrapped objects.
     */
    private List<DataSet> toDataSetList(List<Entity> entityList,
                                        boolean includeOnlySageSync) {
        List<DataSet> dataSetList = null;
        if (entityList != null) {

            dataSetList = new ArrayList<>(entityList.size());
            DataSet dataSet;
            for (Entity entity : entityList) {

                if (entity != null) {
                    dataSet = new DataSet(entity);
                    if ((! includeOnlySageSync) || dataSet.hasSageSync()) {
                        dataSetList.add(dataSet);
                    }

                }
            }

        }

        return dataSetList;
    }

    private String getNormalizedBaseUrlString(UriInfo uriInfo) {
        StringBuilder sb = new StringBuilder(uriInfo.getBaseUri().toString());
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private Response getErrorResponse(String context,
                                      Response.Status status,
                                      String errorMessage,
                                      Exception e)  {
        final RestfulWebServiceFailure failure = new RestfulWebServiceFailure(errorMessage, e);
        logger.error(context + errorMessage, e);
        return Response.status(status).entity(failure).build();
    }

    private String getResponseString(Response response) {
        return response.getStatus() + ": " + response.getEntity();
    }
}
