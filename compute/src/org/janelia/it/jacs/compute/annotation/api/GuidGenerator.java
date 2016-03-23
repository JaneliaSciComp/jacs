/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.api;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;

/**
 * Create a group of GUIDs (unique ids).  Returns them as <cr>-delimited longs
 * as strings.  This is technically an invalid RESTful interface, because the
 * same input _always_ gives a different answer.
 *
 * @author fosterl
 */
@Path("/GuidGenerator")
public class GuidGenerator {
    // Setting this to avoid making enormous buffer to return.
    private static final Integer MAX_BLOCK_SIZE = 5000;
    
    /**
     * Given a count, make that many IDs and return them.
     *   RETURN FORMAT 
     *     [0-9]+(\n[0-9]+)*
     * 
     * @param count how many?
     * @return ids as <cr>-separated long-strings.
     */
    @GET
    @Path("/getNextGuids/{count}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNextGuids(@PathParam("count") int count) {
        try {
            if (count > MAX_BLOCK_SIZE) {
                throw new IllegalArgumentException("Exceeded maximum of " + MAX_BLOCK_SIZE);
            }
            List<Long> ids = TimebasedIdentifierGenerator.generateIdList(count);
            StringBuilder idBuf = new StringBuilder();
            for (Long id: ids) {
                if (idBuf.length() > 0) {
                    idBuf.append("\n");
                }
                idBuf.append(id);
            }
            return Response.ok(new GenericEntity<>(idBuf.toString(), String.class)).build();
        } catch (Exception ex) {
            return Response.serverError()
                    .header("error-message", ex.getMessage())
                    .build();
        }
    }
    
    /**
     * Tell how many the maximum number of ids is.
     * 
     * @return an int value as string.  FORMAT [0-9]+
     */
    @GET
    @Path("/getMaxCount")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMaxCount() {
        return Response.ok(new GenericEntity<>(MAX_BLOCK_SIZE.toString(), String.class)).build();
    }
}
