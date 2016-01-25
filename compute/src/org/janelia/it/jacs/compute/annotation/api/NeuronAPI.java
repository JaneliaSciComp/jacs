/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.api;

//import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPoint;

/*

Below: a script, sufficient to test initial version of this.

#!/bin/bash
FILE=newNeuronPost.txt
CONTEXT=rest-v1

echo \{ \"pointGUID\":9999, \"sampleID\":8888, \"neuronGUID\":9996, \"x\":74000, \"y\":46000, \"z\":17000, \"structureID\":2, \"parentPointGUID\":9997 \} >${FILE}
curl -H"Content-Type:application/json"  --request POST --data @${FILE} http://foster-ws:8180/${CONTEXT}/NeuronAPI/addPointJSON/9999/

#
curl -H"Content-Type:text/plain"  --request POST http://foster-ws:8180/${CONTEXT}/NeuronAPI/addPointQP/9999/?pointGUID=9999\&sampleID=8888\&neuronGUID=9996\&x=74000\&y=46000\&z=17000\&structureID=2\&parentPointGUID=9997

curl -H"Content-Type:text/plain"  --request DELETE http://foster-ws:8180/rest-v1/NeuronAPI/removePoint/9999/
curl -H"Content-Type:text/plain"  --request DELETE http://foster-ws:8180/rest-v1/NeuronAPI/removeNeuron/9997/
curl -H"Content-Type:text/plain"  --request DELETE http://foster-ws:8180/rest-v1/NeuronAPI/removeSample/9996/
*/

/**
 * Exposes a web service for adding/removing neurons, and neurons' points.
 *
 * @author fosterl
 */
//@RequestScoped
//@RolesAllowed("mouselight")
@Path("/NeuronAPI")
public class NeuronAPI {
    private Logger log = Logger.getLogger(NeuronAPI.class);
    /**
     * Add a point at x,y,z. Use sample, neuron from LVV/WS. structure ID is 
     * from SWC specification.
     */
    @POST
    @Path("/addPointQP/{pointGUID}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addPointQP(@PathParam("pointGUID") Long pointGUID, 
                           @QueryParam("sampleID") Long sampleID, 
                           @QueryParam("neuronGUID") Long neuronGUID, 
                           @QueryParam("x") int x, @QueryParam("y") int y, @QueryParam("z") int z, 
                           @QueryParam("structureID") int structureID,
                           @QueryParam("parentPointGUID") Long parentPointGUID) {
        try {
            addPointImpl(pointGUID, sampleID, neuronGUID, x, y, z, structureID, parentPointGUID);
            return Response.ok(new GenericEntity<>("Point Added", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }
    
    /**
     * Add a point at x,y,z.  Use a sample and neuron ID from LVV/WS.
     * @param point all info about the point to be added to the neuron.
     */
    @POST
    @Path("/addPointJSON/{pointGUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addPointJSON(AnnotationPoint point) {
        // Need to hatch out the JSON.
        
        try {
            addPointImpl(
                    point.pointGUID, point.sampleID, point.neuronGUID, 
                    point.x, point.y, point.z, 
                    point.structureID,
                    point.parentPointGUID
            );
            return Response.ok(new GenericEntity<>("Point Added", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/removePoint/{pointGUID}")
    public Response removePoint(@PathParam("pointGUID") Long pointGUID) {
        try {
            removePointImpl(pointGUID);
            return Response.ok(new GenericEntity<>("Point Removed", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/removeNeuron/{neuronGUID}")
    public Response removeNeuron(@PathParam("neuronGUID") Long pointGUID) {
        try {
            removeNeuronImpl(pointGUID);
            return Response.ok(new GenericEntity<>("Neuron Removed", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/removeSample/{sampleID}")
    public Response removeSample(@PathParam("sampleID") Long sampleID) {
        try {
            removeSampleImpl(sampleID);
            return Response.ok(new GenericEntity<>("Sample Removed", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    public Response errorResponse(Exception ex) {
        return Response.serverError()
                .header("error-message", ex.getMessage())
                .build();
    }
    
    private void addPointImpl(Long pointGUID, Long sampleID, Long neuronGUID, int x, int y, int z, int structureID, Long parentPointGUID) {
        log.info(String.format("Adding point: %d,%d,%d under UID %d w/ parent %d.  Adding to neuron %d.", x, y, z, pointGUID, parentPointGUID, neuronGUID));        
    }
    
    private void removePointImpl(Long pointGUID) {
        log.info("Removed point " + pointGUID);
    }
    private void removeNeuronImpl(Long neuronGUID) {
        log.info("Removed neuron " + neuronGUID);
    }
    private void removeSampleImpl(Long sampleID) {
        log.info("Removed sample " + sampleID);
    }
}
