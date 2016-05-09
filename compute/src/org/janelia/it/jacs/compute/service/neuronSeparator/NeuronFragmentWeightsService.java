package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;

import static java.util.Arrays.asList;

/**
 * Process through the database, calculating neuron fragment voxel sizes for those that don't have the metadata. Parameters:
 *   NEURON_FRAGMENT_WEIGHTS_FOLDER - location to write out neuron fragment data
 *   NEURON_SEPARATION_INDEX - current index in to list of neuron separations to process
 *   NEURON_SEPARATION_MAP - stores all neuron separations
 *   NEURON_SEPARATION_DIR
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronFragmentWeightsService extends AbstractDomainService {

    final public String MODE_SEPARATIONLIST="SEPARATIONLIST";
    final public String MODE_KICKOFFWEIGHTS="WEIGHTSPRELOAD";
    final public String MODE_PROCESSRESULTS="PROCESSRESULTS";

    public transient static final String PARAM_testRun = "is test run";

    private SampleHelperNG sampleHelper;
    protected List<Long> neuronSeparationIds;
    protected Map<Long, String> neuronSeparationsToProcess;

    public void execute() throws Exception {
        logger.info("Getting MODE");
        String mode = processData.getString("MODE");

        if (mode.equals(MODE_SEPARATIONLIST)) {
            findUnprocessedNeuronSeparations();
        } else if (mode.equals(MODE_KICKOFFWEIGHTS)) {
            kickoffNeuronWeights();
        } else if (mode.equals(MODE_PROCESSRESULTS)) {
            processNeuronWeightsResults();
        } else {
            throw new Exception("Do not recognize mode="+mode);
        }

    }

    private void findUnprocessedNeuronSeparations() {
        logger.info("Getting list of neuron separations without weights");

        MongoClient m = DomainDAOManager.getInstance().getDao().getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
       // {$unwind: "$objectiveSamples"}, {$unwind: "$objectiveSamples.pipelineRuns"},{$unwind: "$objectiveSamples.pipelineRuns.results"},{$unwind: "$objectiveSamples.pipelineRuns.results.results"},{$match: {"objectiveSamples.pipelineRuns.results.results.class":"org.janelia.it.jacs.model.domain.sample.NeuronSeparation"}},{$project: {neuronSeparation: "$objectiveSamples.pipelineRuns.results.results"}}]
        jsonResult = sample.aggregate(asList(
                new Document("$match", new Document("status", "Error")),
                new Document("$group", new Document("_id", "$dataSet")
                        .append("count", new Document("$sum", "1")))))
                .into(new ArrayList());

        neuronSeparationIds = new ArrayList();
        for (Document neuronSep : jsonResult) {
            if (neuronSep.get("hasWeights")==null) {
                // get the filepath and separation ids and add to list to process
                neuronSeparationIds.add((Long)neuronSep.get("_id"));
                neuronSeparationsToProcess.put((Long)neuronSep.get("_id"), (String)neuronSep.get("filepath"));
            }
        }

        logger.info("findUnprocessedNeuronSeparations() putting vars in processData, separationListSize="+neuronSeparationsToProcess.size());
        processData.putItem("NEURON_SEPARATION_INDEX", neuronSeparationIds);
        processData.putItem("NEURON_SEPARATION_MAP", neuronSeparationsToProcess);
    }

    private void kickoffNeuronWeights() {
        logger.info("Getting list of neuron separations without weights");
        Long neuronSeparationId = processData.getLong("NEURON_SEPARATION_INDEX");
        Map neuronSeparationsToProcess = (Map)processData.getItem("NEURON_SEPARATION_MAP");
        processData.putItem("NEURON_SEPARATION_DIR", neuronSeparationsToProcess.get(neuronSeparationId));

    }

    private void processNeuronWeightsResults() {
        // read all the files out of the output directory and process them into the database
        logger.info("Getting list of neuron separations without weights");

    }


}
