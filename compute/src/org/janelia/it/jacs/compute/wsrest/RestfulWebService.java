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
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.domain.SageArtifactExportService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.json.JsonDataSet;
import org.janelia.it.jacs.model.entity.json.JsonLineStatus;
import org.janelia.it.jacs.model.entity.json.JsonRelease;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.status.CurrentTaskStatus;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.LSMProcessingTask;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
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
    public List<JsonDataSet> getDataSets(
            @QueryParam("user") List<String> userList,
            @QueryParam("includeOnlySageSync") String includeOnlySageSync)
            throws NotFoundException{

        List<DataSet> dataSets = new ArrayList<>();
        try {
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            if ((userList == null) || (userList.size() == 0)) {
                dataSets.addAll(dao.getDataSets(null));
            } 
            else {
                for(String subjectKey : userList) {
                    dataSets.addAll(dao.getDataSets(subjectKey));
                }
            }
        } catch (Exception e) {
            logger.error("getDataSets: failed retrieval, userList=" +
                         userList, e);
        }

        final boolean includeOnlySageSyncFlag =
                Boolean.parseBoolean(includeOnlySageSync);
        final List<JsonDataSet> dataSetList =
                toDataSetList(dataSets, includeOnlySageSyncFlag);

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
    @Path("owner/{owner}/lsmPipelines")
    public Response launchLsmPipelines(
            @PathParam("owner")String owner,
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

            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            Sample slideCodeSample = dao.getSampleBySlideCode(subjectKey, dataSet, slideCode);
            logger.info(context + "found " + slideCodeSample + " for slide code " + slideCode +
                        " (subjectKey is " + subjectKey + ")");
            dao.remove(subjectKey, slideCodeSample);
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
     * Get release information. 
     */
    @GET
    @Path("release")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseInfo() {

        List<JsonRelease> releaseList = new ArrayList<>();

        try {
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            for(LineRelease release : dao.getDomainObjects(null, LineRelease.class)) {
                releaseList.add(new JsonRelease(release));
            }
        }
        catch (Exception e) {
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

        List<JsonRelease> releaseList = new ArrayList<>();
        
        try {
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            for(LineRelease release : dao.getDomainObjectsByName(null, LineRelease.class, releaseName)) {
                releaseList.add(new JsonRelease(release));
            }
        }
        catch (Exception e) {
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
        
        final Map<String,JsonLineStatus> lines = new HashMap<>();
        
        try {
            final DomainDAO dao = DomainDAOManager.getInstance().getDao();
            final ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            final SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, null, logger);
            
            // Consider all releases with a given name
            for(LineRelease release : dao.getLineReleases(null)) {
                
                // Find the release folder
                TreeNode topLevelFolder = sampleHelper.createOrVerifyRootEntity(release.getOwnerKey(), DomainConstants.NAME_FLY_LINE_RELEASES, false);
                if (topLevelFolder==null) {
                    logger.error("User "+release.getOwnerKey()+" is missing top-level folder '"+DomainConstants.NAME_FLY_LINE_RELEASES+"'");
                    continue;
                }
                TreeNode releaseFolder = sampleHelper.createOrVerifyChildFolder(topLevelFolder, release.getName(), false);
                if (releaseFolder==null) {
                    logger.error("User "+release.getOwnerKey()+" is missing release folder '"+release.getName()+"'");
                    continue;
                }
                
                // Get all annotators
                Set<String> annotatorKeys = new HashSet<>(release.getAnnotators());
                annotatorKeys.add(release.getOwnerKey());
                
                // Walk the release folder hierarchy
                for(ObjectSet sampleSet : dao.getDomainObjectsAs(releaseFolder.getChildren(), ObjectSet.class)) {

                    List<Reference> refs = DomainUtils.getReferencesForMembers(sampleSet);
                    
                    // Get all sample annotations
                    Multimap<Long, Annotation> annotationsByTarget = HashMultimap.<Long, Annotation>create();
                    for (Annotation annotation : dao.getAnnotations(null, refs)) {
                        annotationsByTarget.put(annotation.getTarget().getTargetId(), annotation);
                    }
                    
                    // Count of samples in this release for this fly line
                    int numSamples = 0;
                    // Count of representative samples marked for export
                    int numRepresentatives = 0;
                    
                    for(Sample sample : dao.getDomainObjectsAs(refs, Sample.class)) {
                        boolean export = false;
                        for(Annotation annotation : annotationsByTarget.get(sample.getId())) {
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
                    
                    JsonLineStatus status = lines.get(sampleSet.getName());
                    if (status==null) {
                        status = new JsonLineStatus();
                        lines.put(sampleSet.getName(), status);
                    }
                    
                    status.addSamples(numSamples);
                    status.addRepresentatives(numRepresentatives);
                    status.getReleaseIds().add(release.getId().toString());
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
    private List<JsonDataSet> toDataSetList(List<DataSet> entityList,
                                        boolean includeOnlySageSync) {
        List<JsonDataSet> dataSetList = null;
        if (entityList != null) {
            dataSetList = new ArrayList<>(entityList.size());
            for (DataSet dataSet : entityList) {
                if (dataSet != null) {
                    JsonDataSet jsonDataSet = new JsonDataSet(dataSet);
                    if ((! includeOnlySageSync) || jsonDataSet.hasSageSync()) {
                        dataSetList.add(jsonDataSet);
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
