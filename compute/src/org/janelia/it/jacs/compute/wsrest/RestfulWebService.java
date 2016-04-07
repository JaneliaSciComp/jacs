package org.janelia.it.jacs.compute.wsrest;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.service.entity.SageArtifactExportService;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.DataSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.entity.json.JsonRelease;
import org.janelia.it.jacs.model.entity.json.JsonLineStatus;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.status.CurrentTaskStatus;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.LSMProcessingTask;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.NotFoundException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
     * @param  owner id of the person or system submitting this request
     *
     * @param lsmProcessingParams input parameters encapsulated in a LSMProcessingTask
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    @Path("owner/{owner}/dataSet/{dataSet}/lsmPipelines")
    public Response launchLsmPipelines(
            @PathParam("owner")String owner,
            @PathParam("dataSet")String dataSet,
            LSMProcessingTask lsmProcessingParams,
            @Context UriInfo uriInfo) {

        final String context = "launchLsmPipelines: ";
        logger.info(context +"entry, owner=" + owner +
                ", dataset=" + dataSet +
                ", lsms=" + lsmProcessingParams.getLsmNames().toString());
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
            lsmProcessingParams.setDataSetName(dataSet);
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

        JsonTask result = new JsonTask(lsmProcessingTask);
        result.setTaskStatusUrl(getNormalizedBaseUrlString(uriInfo) + "task/" + lsmProcessingTask.getObjectId() + "/currentStatus");

        return Response
                .status(Response.Status.CREATED)
                .entity(result)
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
     * Get release information. 
     */
    @GET
    @Path("release")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseInfo() {

        final EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        List<JsonRelease> releaseList = new ArrayList<>();

        try {
            for(Entity releaseEntity : entityBean.getEntitiesByTypeName(null, EntityConstants.TYPE_FLY_LINE_RELEASE)) {
                releaseList.add(new JsonRelease(releaseEntity));
            }
        }
        catch (ComputeException e) {
            logger.error("Problem getting releases",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return Response.status(Response.Status.OK).entity(releaseList).build();
    }
    
    /**
     * Get release information. 
     */
    @GET
    @Path("release/{releaseName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseInfo(
            @PathParam("releaseName")String releaseName) {

        final EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        List<JsonRelease> releaseList = new ArrayList<>();
        
        try {
            for(Entity releaseEntity : entityBean.getEntitiesByNameAndTypeName(null, releaseName, EntityConstants.TYPE_FLY_LINE_RELEASE)) {
                releaseList.add(new JsonRelease(releaseEntity));
            }
        }
        catch (ComputeException e) {
            logger.error("Problem getting releases",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return Response.status(Response.Status.OK).entity(releaseList).build();
    }

    /**
     * Get status of annotations for a release. 
     */
    @GET
    @Path("release/{releaseName}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseStatus(
            @PathParam("releaseName")String releaseName) {

        final String context = "getReleaseStatus: ";
        final AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
        final EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        final EntityBeanEntityLoader entityLoader = new EntityBeanEntityLoader(entityBean);
        final Map<String,JsonLineStatus> lines = new HashMap<>();
        
        try {
            // Consider all releases with a given name
            for(Entity releaseEntity : entityBean.getEntitiesByNameAndTypeName(null, releaseName, EntityConstants.TYPE_FLY_LINE_RELEASE)) {
                
                // Find the release folder
                Entity releaseFolder = null;
                for(Entity folder : entityBean.getEntitiesByNameAndTypeName(releaseEntity.getOwnerKey(), releaseName, EntityConstants.TYPE_FOLDER)) {
                    if (!folder.getOwnerKey().equals(releaseEntity.getOwnerKey())) continue;
                    if (releaseFolder!=null) {
                        return getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR,
                                "Multiple annotation folders for release " + releaseName);
                    }
                    releaseFolder = folder;
                }
                
                // Get all annotators
                Set<String> annotatorKeys = new HashSet<>();
                String annotatorsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATORS);
                if (annotatorsStr != null) {
                    for (String key : annotatorsStr.split(",")) {
                        annotatorKeys.add(key);
                    }
                }
                annotatorKeys.add(releaseEntity.getOwnerKey());
                
                // Walk the release folder hierarchy
                entityLoader.populateChildren(releaseFolder);
                for(Entity flylineFolder : EntityUtils.getChildrenOfType(releaseFolder, EntityConstants.TYPE_FOLDER)) {
                    entityLoader.populateChildren(flylineFolder);

                    // Get all sample annotations
                    Multimap<String, Entity> annotationsByTarget = HashMultimap.<String, Entity>create();
                    for (Entity annotation : annotationBean.getAnnotationsForChildren(null, flylineFolder.getId())) {
                        String targetId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
                        annotationsByTarget.put(targetId, annotation);
                    }
                    
                    // Count of samples in this release for this fly line
                    int numSamples = 0;
                    // Count of representative samples marked for export
                    int numRepresentatives = 0;
                    
                    for(Entity sample : EntityUtils.getChildrenOfType(flylineFolder, EntityConstants.TYPE_SAMPLE)) {
                        boolean export = false;
                        for(Entity annotation : annotationsByTarget.get(sample.getId().toString())) {
                            if (!annotatorKeys.contains(annotation.getOwnerKey())) {
                                continue;
                            }
                            if (annotation.getName().equals(SageArtifactExportService.ANNOTATION_EXPORT_20X)) {
                                export = true;
                            }
                            else if (annotation.getName().equals(SageArtifactExportService.ANNOTATION_EXPORT_63X)) { 
                                export = true;
                            }
                        }
                        if (export) {
                            numRepresentatives++;
                        }
                        numSamples++;
                    }
                    
                    JsonLineStatus status = lines.get(flylineFolder.getName());
                    if (status==null) {
                        status = new JsonLineStatus();
                        lines.put(flylineFolder.getName(), status);
                    }
                    
                    status.addSamples(numSamples);
                    status.addRepresentatives(numRepresentatives);
                    status.getReleaseIds().add(releaseEntity.getId().toString());
                }
            }
        }
        catch (Exception e) {
            return getErrorResponse(context, Response.Status.INTERNAL_SERVER_ERROR,
                    "Problem getting release status" + releaseName);
        }
        
        return Response.status(Response.Status.OK).entity(lines).build();
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

    private Response getErrorResponse(String context, Response.Status status, String errorMessage)  {
        return getErrorResponse(context, status, errorMessage, null);
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
}
