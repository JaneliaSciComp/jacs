/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.api;

//import javax.annotation.security.RolesAllowed;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPoint;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPointCollection;
import org.apache.log4j.Logger;

/*

Below: a script, sufficient to test initial version of this.

#!/bin/bash
FILE=newNeuronPost.txt
CONTEXT=rest-v1
SRVR_URL=http://foster-ws:8180

echo
echo TESTING COLLECTION ADD
echo \{ \"guid\":7777, \"name\":\"My First Collection\", \"versionNumber\":3 \} > ${FILE}
curl -H"Content-Type:application/json"  --request POST --data @${FILE} ${SRVR_URL}/${CONTEXT}/NeuronAPI/addCollection/7777/

echo
echo TESTING SAMPLE MOD
curl -H"Content-Type:text/plain"  --request PUT ${SRVR_URL}/${CONTEXT}/NeuronAPI/setCollectionSampleID/7777/?sampleID=1011343133

echo
echo TESTING POINT ADD
echo \{ \"pointGUID\":9999, \"collectionGUID\":8888, \"neuronGUID\":9996, \"x\":74000, \"y\":46000, \"z\":17000, \"structureID\":2, \"parentPointGUID\":9997 \} >${FILE}
curl -H"Content-Type:application/json"  --request POST --data @${FILE} ${SRVR_URL}/${CONTEXT}/NeuronAPI/addPointJSON/9999/

#
echo
echo TESTING POINT ADD
curl -H"Content-Type:text/plain"  --request POST ${SRVR_URL}/${CONTEXT}/NeuronAPI/addPointQP/9999/?pointGUID=9999\&collectionGUID=8888\&neuronGUID=9996\&x=74000\&y=46000\&z=17000\&structureID=2\&parentPointGUID=9997

echo
echo TESTING DELETE POINT
curl -H"Content-Type:text/plain"  --request DELETE ${SRVR_URL}/${CONTEXT}/NeuronAPI/removePoint/9999/

echo
echo TESTING DELETE NEURON
curl -H"Content-Type:text/plain"  --request DELETE ${SRVR_URL}/${CONTEXT}/NeuronAPI/removeNeuron/9997/

echo
echo TESTING DELETE COLLECTION
curl -H"Content-Type:text/plain"  --request DELETE ${SRVR_URL}/${CONTEXT}/NeuronAPI/removeCollection/8888/
echo
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

    @POST
    @Path("/addCollection/{collectionGUID}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addCollection(AnnotationPointCollection collection) {
        try {
            if (collection.creationDate == null) {
                collection.creationDate = new Date();
            }
            addCollectionImpl(collection);
            return Response.ok(new GenericEntity<>("Collection Added", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
        
    }

    @PUT
    @Path("/setCollectionSampleID/{collectionGUID}")
    public Response setCollectionSampleID(
            @PathParam("collectionGUID") Long collectionGUID,
            @QueryParam("sampleID") Long sampleID) {
        try {
            setCollectionSampleIDImpl(sampleID, collectionGUID);
            return Response.ok(new GenericEntity<>("Collection Updated", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    /**
     * Add a point at x,y,z. Use collection, neuron from LVV/WS. structure ID is 
     * from SWC specification.
     */
    @POST
    @Path("/addPointQP/{pointGUID}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addPointQP(@PathParam("pointGUID") Long pointGUID, 
                           @QueryParam("collectionGUID") Long collectionGUID, 
                           @QueryParam("neuronGUID") Long neuronGUID, 
                           @QueryParam("x") int x, @QueryParam("y") int y, @QueryParam("z") int z, 
                           @QueryParam("structureID") int structureID,
                           @QueryParam("parentPointGUID") Long parentPointGUID) {
        try {
            addPointImpl(pointGUID, collectionGUID, neuronGUID, x, y, z, structureID, parentPointGUID);
            return Response.ok(new GenericEntity<>("Point Added", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }
    
    /**
     * Add a point at x,y,z.  Use a neuron ID from LVV/WS.
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
                    point.pointGUID, point.collectionGUID, point.neuronGUID, 
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
    @Path("/removeCollection/{collectionGUID}")
    public Response removeCollection(@PathParam("collectionGUID") Long collectionGUID) {
        try {
            removeCollectionImpl(collectionGUID);
            return Response.ok(new GenericEntity<>("Collection Removed", String.class)).build();
        } catch (Exception ex) {
            return errorResponse(ex);
        }
    }

    public Response errorResponse(Exception ex) {
        return Response.serverError()
                .header("error-message", ex.getMessage())
                .build();
    }
    
    private void addPointImpl(
            Long pointGUID, Long collectionGUID, Long neuronGUID, 
            int x, int y, int z, 
            int structureID, 
            Long parentPointGUID) {
        log.info(String.format(
                "Adding point: %d,%d,%d under UID %d w/ parent %d.  Adding to neuron %d.  In collection %d.", 
                x, y, z, 
                pointGUID, parentPointGUID, 
                neuronGUID, collectionGUID)
        );
    }
    
    private void removePointImpl(Long pointGUID) {
        log.info("Removed point " + pointGUID);
    }
    private void removeNeuronImpl(Long neuronGUID) {
        log.info("Removed neuron " + neuronGUID);
    }
    private void removeCollectionImpl(Long collectionGUID) {
        log.info("Removed collection " + collectionGUID);
    }
    private void addCollectionImpl(AnnotationPointCollection collection) {
        log.info(String.format("Creating collection '%s':%d.  Date: %s.", collection.name, collection.guid, collection.creationDate.toString()));
    }
    private void setCollectionSampleIDImpl(Long sampleID, Long collectionGUID) {
        log.info("Setting the sampleID to " + sampleID + " for collection " + collectionGUID);
    }

}
