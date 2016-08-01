package org.janelia.it.jacs.compute.wsrest.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.*;
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
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.domain.SageArtifactExportService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.json.JsonLineStatus;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.slf4j.LoggerFactory;


@Path("/process")
@Api(value = "Janelia Workstation Domain Data")
public class ReleaseWebService extends ResourceConfig {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DomainObjectWebService.class);
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
    @Path("release")
    @ApiOperation(value = "Gets Release Information",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully got release information", response=Response.class),
            @ApiResponse( code = 500, message = "Internal Server Error getting Release Information" )
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public List<LineRelease> getReleasesInfo(@ApiParam @QueryParam("subjectKey") final String subjectKey) {
        log.debug("getReleasesInfo({})",subjectKey);
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getDomainObjects(subjectKey, LineRelease.class);
        }
        catch (Exception e) {
            log.error("Error occurred getting releases info",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
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
    public List<LineRelease> getReleaseInfo(@ApiParam @QueryParam("subjectKey") final String subjectKey, @ApiParam @PathParam("releaseName")String releaseName) {
        log.debug("getReleaseInfo({}, {})", subjectKey, releaseName);
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.getDomainObjectsByName(subjectKey, LineRelease.class, releaseName);
        }
        catch (Exception e) {
            log.error("Error occurred getting release info",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("release")
    @ApiOperation(value = "Creates a Line Release using the DomainObject parameter of the DomainQuery",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully created a new release",
                    response = DataSet.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating a release" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LineRelease createRelease(DomainQuery query) {
        log.debug("createRelease({})", query);
        DomainDAL dao = DomainDAL.getInstance();
        try {
            LineRelease release = (LineRelease)query.getDomainObject();
            return dao.createLineRelease(query.getSubjectKey(), release.getName(), release.getReleaseDate(), release.getLagTimeMonths(), release.getDataSets());
        }
        catch (Exception e) {
            log.error("Error occurred creating release",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("release")
    @ApiOperation(value = "Updates a Line Release using the DomainObject parameter of the DomainQuery",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully updated a release",
                    response = DataSet.class),
            @ApiResponse( code = 500, message = "Internal Server Error updating a release" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LineRelease updateRelease(@ApiParam DomainQuery query) {
        log.debug("updateRelease({})", query);
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), (LineRelease)query.getDomainObject());
        }
        catch (Exception e) {
            log.error("Error occurred updating data set",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("release")
    @ApiOperation(value = "Removes the Line Release using the release Id",
            notes = ""
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully removed a release"),
            @ApiResponse( code = 500, message = "Internal Server Error removing a release" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeRelease(@ApiParam @QueryParam("subjectKey") final String subjectKey,
                              @ApiParam @QueryParam("releaseId") final String releaseId) {
        log.debug("removeDataSet({}, {})", subjectKey, releaseId);
        DomainDAL dao = DomainDAL.getInstance();
        Reference releaseRef = Reference.createFor(LineRelease.class, new Long(releaseId));
        try {
            DomainObject domainObj = dao.getDomainObject(subjectKey, releaseRef);
            dao.deleteDomainObject(subjectKey, domainObj);
        }
        catch (Exception e) {
            log.error("Error occurred removing release",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
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

        log.debug("getReleaseStatus({})", releaseName);

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
                    log.error("User "+release.getOwnerKey()+" is missing top-level folder '"+DomainConstants.NAME_FLY_LINE_RELEASES+"'");
                    continue;
                }
                TreeNode releaseFolder = sampleHelper.createOrVerifyChildFolder(topLevelFolder, release.getName(), false);
                if (releaseFolder==null) {
                    log.error("User "+release.getOwnerKey()+" is missing release folder '"+release.getName()+"'");
                    continue;
                }

                // Get all annotators
                Set<String> annotatorKeys = new HashSet<>(release.getAnnotators());
                annotatorKeys.add(release.getOwnerKey());

                // Walk the release folder hierarchy
                for(TreeNode lineFolder : dal.getDomainObjectsAs(releaseFolder.getChildren(), TreeNode.class)) {

                    // Get all sample annotations
                    Multimap<Long, Annotation> annotationsByTarget = HashMultimap.create();
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
            log.error("Error occurred getting release status",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.status(Response.Status.OK).entity(lines).build();
    }
}
