package org.janelia.it.jacs.compute.wsrest.data;


import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.util.ActivityLogHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class FilterWebService extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(FilterWebService.class);

    @Context
    SecurityContext securityContext;
    ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    public FilterWebService() {
        register(JacksonFeature.class);
    }

    @PUT
    @Path("/filter")
    @ApiOperation(value = "Creates a Filter",
            notes = "uses the DomainObject parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully created a Filter", response=DomainObject.class),
            @ApiResponse( code = 500, message = "Internal Server Error creating a Filter" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter createFilter(@ApiParam DomainQuery query) {
        log.debug("createFilter({})",query);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), (Filter)query.getDomainObject());
        }
        catch (Exception e) {
            log.error("Error occurred creating Search Filter ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(query.getSubjectKey(), "PUT", "/data/filter", stopWatch.getTime());
        }
    }

    @POST
    @Path("/filter")
    @ApiOperation(value = "Updates a Filter",
            notes = "uses the DomainObject parameter of the DomainQuery"
    )
    @ApiResponses(value = {
            @ApiResponse( code = 200, message = "Successfully Updated a Filter", response=DomainObject.class),
            @ApiResponse( code = 500, message = "Internal Server Error Updating a Filter" )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Filter updateFilter(@ApiParam DomainQuery query) {
        log.debug("updateFilter({})",query);
        StopWatch stopWatch = new StopWatch();
        DomainDAL dao = DomainDAL.getInstance();
        try {
            return dao.save(query.getSubjectKey(), (Filter)query.getDomainObject());
        }
        catch (Exception e) {
            log.error("Error occurred updating search filter ",e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            activityLog.logRESTServiceCall(query.getSubjectKey(), "POST", "/data/filter", stopWatch.getTime());
        }
    }
}