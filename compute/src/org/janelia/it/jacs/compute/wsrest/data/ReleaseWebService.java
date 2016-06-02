package org.janelia.it.jacs.compute.wsrest.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.domain.SageArtifactExportService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.json.JsonLineStatus;
import org.janelia.it.jacs.model.entity.json.JsonRelease;
import org.janelia.it.jacs.model.status.RestfulWebServiceFailure;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;


@Path("/info")
@Api(value = "Janelia Workstation Domain Data")
public class ReleaseWebService extends ResourceConfig {
    private final Logger logger = Logger.getLogger(this.getClass());

    @Context
    SecurityContext securityContext;

    public ReleaseWebService() {
        register(JacksonFeature.class);
    }


    /**
     * Get release information.
     */
    @GET
    @Path("releases")
    @ApiOperation(value = "Gets Release Information",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got release information", response=Response.class),
            @ApiResponse( code = 500, message = "Internal Server Error getting Release Information" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleasesInfo() {

        List<JsonRelease> releaseList = new ArrayList<>();

        try {
            for(LineRelease release : DomainDAL.getInstance().getDomainObjects(null, LineRelease.class)) {
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
    @ApiOperation(value = "Gets Release Information given release information",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got release information", response=Response.class),
            @ApiResponse( code = 500, message = "Internal Server Error getting Release Information" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseInfo(
            @ApiParam @PathParam("releaseName")String releaseName) {

        List<JsonRelease> releaseList = new ArrayList<>();

        try {
            for(LineRelease release : DomainDAL.getInstance().getDomainObjectsByName(null, LineRelease.class, releaseName)) {
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
    @ApiOperation(value = "Gets status of annotations for a release",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got status of annotations", response=Response.class),
            @ApiResponse( code = 500, message = "Internal Server Error got status of annotations" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public Response getReleaseStatus(
            @ApiParam @PathParam("releaseName")String releaseName) {

        final String context = "getReleaseStatus: ";

        final Map<String,JsonLineStatus> lines = new HashMap<>();

        try {
            final DomainDAL dal = DomainDAL.getInstance();
            final ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            final SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, null, logger);

            // Consider all releases with a given name
            for(LineRelease release : DomainDAL.getInstance().getLineReleases(null)) {

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
                for(TreeNode lineFolder : dal.getDomainObjectsAs(releaseFolder.getChildren(), TreeNode.class)) {

                    // Get all sample annotations
                    Multimap<Long, Annotation> annotationsByTarget = HashMultimap.<Long, Annotation>create();
                    for (Annotation annotation : dal.getAnnotations(null, lineFolder.getChildren())) {
                        annotationsByTarget.put(annotation.getTarget().getTargetId(), annotation);
                    }

                    // Count of samples in this release for this fly line
                    int numSamples = 0;
                    // Count of representative samples marked for export
                    int numRepresentatives = 0;

                    for(Sample sample : dal.getDomainObjectsAs(lineFolder.getChildren(), Sample.class)) {
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

                    JsonLineStatus status = lines.get(lineFolder.getName());
                    if (status==null) {
                        status = new JsonLineStatus();
                        lines.put(lineFolder.getName(), status);
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
}
