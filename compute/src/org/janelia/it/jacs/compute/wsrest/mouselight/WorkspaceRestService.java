package org.janelia.it.jacs.compute.wsrest.mouselight;

import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


/**
 * Created by murphys on 3/24/2016.
 */

@Path("/")
public class WorkspaceRestService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRestService.class);

    @GET
    @Path("mouselight")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public WorkspaceRestResponse getMessage() {
        log.info("getMessage() invoked");
        return new WorkspaceRestResponse();
    }

}