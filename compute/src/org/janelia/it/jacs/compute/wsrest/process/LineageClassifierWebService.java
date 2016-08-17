package org.janelia.it.jacs.compute.wsrest.process;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.lineageClassifier.LineageClassifierTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.eq;

/**
 * This provides a hook for executing the NeuronFragmentWeightsService
 *
 * Created by schauderd on 5/13/16.
 */
@Path("/process")
@Api(value = "Janelia Workstation Pipelines")
public class LineageClassifierWebService extends ResourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(LineageClassifierWebService.class);
    private static final String RESOURCE_NAME = "NeuronLineageClassifier";

    /**
     * Create and launch neuron separation weights discovery and calculation pipeline.
     * TODO: This won't work till we move Task and Node persistence out of Hibernate so the RESTful war can kick off pipelines
     * @param subjectKey id of the person or system submitting this request
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/lineageClassifier")
    public Response launchNeuronWeightsPipeline(
            @QueryParam("subjectKey") String subjectKey,
            @QueryParam("nameList") final List<String> sampleNameList) {
        LineageClassifierTask lineageClassifierTask = new LineageClassifierTask();
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        try {
            final ComputeBeanRemote remoteComputeBean = EJBFactory.getRemoteComputeBean();

            if (subjectKey == null) {
                throw new IllegalArgumentException("owner parameter is not defined");
            }
            lineageClassifierTask.setOwner(subjectKey);

            List<Document> sampleResults = sample.find(eq("name", new Document("$in", sampleNameList))).into(new ArrayList());
        List<String> foobar = new ArrayList<>();
for (int i=0; i<sampleResults.size(); i++) {
    foobar.add(sampleResults.get(i).getLong("_id").toString());
}
        lineageClassifierTask.setSampleIdList(StringUtils.join(foobar, ","));
            lineageClassifierTask = (LineageClassifierTask) remoteComputeBean.saveOrUpdateTask(lineageClassifierTask);
            remoteComputeBean.submitJob(RESOURCE_NAME, lineageClassifierTask.getObjectId());
        } catch (RemoteException | ComputeException e) {
            logger.error("Lineage Classifier exception", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
e.printStackTrace();

    }
        return Response
                .status(Response.Status.CREATED)
                .entity(new JsonTask(lineageClassifierTask))
                .build();
    }

}
