package org.janelia.it.jacs.compute.wsrest.process;


import java.rmi.RemoteException;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronWeightsTask;
import org.janelia.it.jacs.model.user_data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provides a hook for executing the NeuronFragmentWeightsService
 *
 * Created by schauderd on 5/13/16.
 */
@Path("/process")
@Api(value = "Janelia Workstation Pipelines")
public class NeuronWeightsWebService  extends ResourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(NeuronWeightsWebService.class);
    private static final String RESOURCE_NAME = "NeuronFragmentWeights";

    /**
     * Create and launch neuron separation weights discovery and calculation pipeline.
     * TODO: This won't work till we move Task and Node persistence out of Hibernate so the RESTful war can kick off pipelines
     * @param subjectKey id of the person or system submitting this request
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/neuron/weights")
    public Response launchNeuronWeightsPipeline(
            @QueryParam("subjectKey") String subjectKey) {
        NeuronWeightsTask neuronWeightsTask = new NeuronWeightsTask();
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();

            if (subjectKey == null) {
                throw new IllegalArgumentException("owner parameter is not defined");
            }
            neuronWeightsTask.setOwner(subjectKey);
            neuronWeightsTask = (NeuronWeightsTask) remoteComputeBean.saveOrUpdateTask(neuronWeightsTask);
            remoteComputeBean.submitJob(RESOURCE_NAME, neuronWeightsTask.getObjectId());
        } catch (RemoteException | ComputeException e) {
            logger.error("Neuron Fragment Weights Discovery/Processing exception", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response
                .status(Response.Status.CREATED)
                .entity(new JsonTask(neuronWeightsTask))
                .build();
    }

}
