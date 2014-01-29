package org.janelia.it.jacs.compute.wsrest;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.DataSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.status.CurrentTaskStatus;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.NotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
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

    private Logger logger = Logger.getLogger(this.getClass());

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
    @Produces("application/xml")
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
    @Produces("application/xml")
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
    @Produces("application/xml")
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
    @Produces("application/xml")
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
            list = new ArrayList<EntityType>();
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

            dataSetList = new ArrayList<DataSet>(entityList.size());
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
