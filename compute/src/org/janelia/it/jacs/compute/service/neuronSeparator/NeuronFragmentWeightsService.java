package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.persist.results.initial.CreateBlastFileNodeException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronFragmentWeightsResultNode;
import org.janelia.it.jacs.shared.img_3d_loader.V3dMaskFileLoader;
import org.janelia.it.jacs.shared.utils.FileUtil;

import static java.util.Arrays.asList;

/**
 * Process through the database, calculating neuron fragment voxel sizes for those that don't have the metadata. Parameters:
 *   NEURON_FRAGMENT_WEIGHTS_FOLDER - location to write out neuron fragment data
 *   NEURON_SEPARATION_INDEX - current index in to list of neuron separations to process
 *   NEURON_SEPARATION_MAP - stores all neuron separations
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronFragmentWeightsService extends AbstractDomainService {

    final public String MODE_SEPARATIONLIST="SEPARATIONLIST";
    final public String MODE_NODE_CREATION="NODECREATION";
    final public String MODE_GENERATEWEIGHTS="GENERATEWEIGHTS";
    final public String MODE_PROCESSRESULTS="PROCESSRESULTS";
    final public int BATCH_SIZE = 1000;

    public transient static final String PARAM_testRun = "is test run";

    private SampleHelperNG sampleHelper;
    protected List<String> neuronSeparationsToProcess;

    public void execute() throws Exception {
        logger.info("Getting MODE");
        String mode = processData.getString("MODE");

        if (mode.equals(MODE_SEPARATIONLIST)) {
            findUnprocessedNeuronSeparations();
        } else if (mode.equals(MODE_NODE_CREATION)) {
            createResultNode();
        } else if (mode.equals(MODE_GENERATEWEIGHTS)) {
            generateWeightsForSingleNeuronSeparation();
        } else if (mode.equals(MODE_PROCESSRESULTS)) {
            processNeuronWeightsResults();
        } else {
            throw new Exception("Do not recognize mode="+mode);
        }
    }

    private void createResultNode() throws Exception {
        Task task = ProcessDataHelper.getTask(processData);
        String sessionName = ProcessDataHelper.getSessionRelativePath(processData);

        // if we get this far then we assume that no result persist exists and create one
        Task loadedTask = EJBFactory.getLocalComputeBean().getTaskById(task.getObjectId());
        NeuronFragmentWeightsResultNode neuronFileNode = new NeuronFragmentWeightsResultNode(loadedTask.getOwner(), loadedTask, "NeuronFragmentWeights",
                "NeuronFragmentWeights for createtask " + loadedTask.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(neuronFileNode);

        FileUtil.ensureDirExists(neuronFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(neuronFileNode.getDirectoryPath());

        processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_ID, neuronFileNode.getObjectId());
        processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_DIR, neuronFileNode.getDirectoryPath());
        logger.debug("Created Neuron Weights Directory and placed in processData id=" + neuronFileNode.getObjectId());
    }

    private void findUnprocessedNeuronSeparations() throws Exception {
        logger.info("Getting list of neuron separations without weights");

        MongoClient m = DomainDAOManager.getInstance().getDao().getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        List<Document> jsonResult = new ArrayList<>();
        jsonResult = sample.aggregate(asList(
                new Document("$unwind", "$objectiveSamples"),
                new Document("$unwind", "$objectiveSamples.pipelineRuns"),
                new Document("$unwind", "$objectiveSamples.pipelineRuns.results"),
                new Document("$unwind", "$objectiveSamples.pipelineRuns.results.results"),
                new Document("$match", new Document("objectiveSamples.pipelineRuns.results.results.class",
                        "org.janelia.it.jacs.model.domain.sample.NeuronSeparation")),
                new Document("$project", new Document("separationId",
                        "$objectiveSamples.pipelineRuns.results.results.id")
                        .append("filepath","$objectiveSamples.pipelineRuns.results.results.filepath")
                        .append("hasWeights", "$objectiveSamples.pipelineRuns.results.results.hasWeights"))))
                .into(new ArrayList());

        neuronSeparationsToProcess = new ArrayList();
        int count = 1;
        StringBuilder batchArgs;
        batchArgs = new StringBuilder("{");
        for (Document neuronSep : jsonResult) {
            if (!neuronSep.getBoolean("hasWeights")) {
                batchArgs.append("\"" + neuronSep.get("separationId") + "\":\"" + neuronSep.get("filepath")+"\"");
                if (count==BATCH_SIZE) {
                    count = 1;
                    batchArgs.append("}");
                    // get the filepath and separation ids and add to list to process
                    neuronSeparationsToProcess.add(batchArgs.toString());
                    batchArgs = new StringBuilder("{");
                } else {
                    batchArgs.append(",");
                    count++;
                }
            }
        }
        String lastVal = batchArgs.toString();
        lastVal = lastVal.substring(0,lastVal.length()-1) + "}";
        neuronSeparationsToProcess.add(lastVal);

        logger.info("findUnprocessedNeuronSeparations() putting vars in processData, separationListSize=" + neuronSeparationsToProcess.size());
        processData.putItem("NEURON_SEPARATION_BATCH_LIST", neuronSeparationsToProcess);
    }

    private NeuronSeparation getNeuronSeparation(Sample parentSample, long separationId) {
        List<ObjectiveSample> objSamples = parentSample.getObjectiveSamples();
        for (ObjectiveSample objSample : objSamples) {
            for (SamplePipelineRun sampleRun : objSample.getPipelineRuns()) {
                for (PipelineResult result : sampleRun.getResults()) {
                    for (PipelineResult subresult : result.getResults()) {
                        if (subresult.getClass()==NeuronSeparation.class && subresult.getId()==separationId) {
                            return (NeuronSeparation)subresult;
                        }
                    }
                }
            }

        }
        return null;
    }

    private void updateFragments (Map fragWeights, Long separationId, org.jongo.MongoCollection fragmentCollection) throws Exception {
        Iterator fragKeys = fragWeights.keySet().iterator();
        while (fragKeys.hasNext()) {
            String key = (String)fragKeys.next();
            int number = Integer.parseInt(key)-1; // we store it in our system 1 less than Neuron Sep code labels
            NeuronFragment fragment = fragmentCollection.findOne("{separationId:#,number:#}",separationId, number)
                    .as(NeuronFragment.class);
            fragment.setVoxelWeight(Integer.parseInt((String) fragWeights.get(key)));
            DomainDAL.getInstance().save(fragment.getOwnerKey(), fragment);
        }
    }


    // read all the files out of the output directory and process them into the database
    private void processNeuronWeightsResults() throws Exception {
        logger.info("Storing neuron weights back into MongoDB");
        String outputDir = processData.getString("RESULT_FILE_NODE_DIR") + File.separator + "sge_output";
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        org.jongo.MongoCollection fragmentCollection = dao.getCollectionByName("fragment");

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(outputDir))) {
            ObjectMapper mapper = new ObjectMapper();
            for (Path path : directoryStream) {
                List<Map> results = mapper.readValue(Files.newInputStream(path), new TypeReference<List<Map>>(){});
                for (int i=0; i<results.size(); i++) {
                    Map fragWeights = results.get(i);
                    long separationId = Long.parseLong((String)fragWeights.get("separationId"));
                    fragWeights.remove("separationId");

                    NeuronFragment baseFrag = fragmentCollection.findOne("{separationId:#,number:#}",separationId, 0)
                            .as(NeuronFragment.class);
                    if (baseFrag!=null) {
                        Reference sampleRef = baseFrag.getSample();
                        Sample neuronSepSample = (Sample)dao.getDomainObject(null, sampleRef);
                        if (neuronSepSample!=null) {
                            NeuronSeparation neuronSep = getNeuronSeparation(neuronSepSample, separationId);
                            if (neuronSep!=null) {
                                neuronSep.setHasWeights(true);
                                DomainDAL.getInstance().save(null,neuronSepSample);
                                updateFragments(fragWeights,separationId, fragmentCollection);
                            }
                        }
                    }
                }
            }
        }
    }

    // given a newly created neuron separation, generate the voxel weights from the separation
    private void generateWeightsForSingleNeuronSeparation() throws Exception {
        logger.info("Generating weights for a single neuron separation and persist to the DB");
       /* DomainDAO dao = DomainDAOManager.getInstance().getDao();
        org.jongo.MongoCollection fragmentCollection = dao.getCollectionByName("fragment");

        Long sampleId = Long.parseLong(processData.getString("SAMPLE_ENTITY_ID"));
        Long separationId = processData.getLong("RESULT_ENTITY_ID");
        Sample sample = (Sample)dao.getDomainObject(null, Reference.createFor(Sample.class, sampleId));
        NeuronSeparation neuronSep = getNeuronSeparation(sample, separationId);

        String filepath = neuronSep.getFilepath();
        V3dMaskFileLoader neuronFragments = new V3dMaskFileLoader();
        neuronFragments.loadVolumeFile(filepath + File.separator + "ConsolidatedLabel.v3dpbd");
        byte[] byteArray = neuronFragments.getTextureByteArray();
        Map finalFragmentCounts = new HashMap<>();
        for (int i=0; i<byteArray.length; i++) {
            if (byteArray[i]!=0) {
                String key = Byte.toString(byteArray[i]);
                if (finalFragmentCounts.containsKey(key)) {
                    finalFragmentCounts.put(key, ((Integer)finalFragmentCounts.get(key)).intValue() + 1);
                } else {
                    finalFragmentCounts.put(key, 1);
                }
            }
        }

        // update each neuron fragment with voxel size
        neuronSep.setHasWeights(true);
        DomainDAL.getInstance().save(sample.getOwnerKey(),sample);
        updateFragments(finalFragmentCounts,separationId, fragmentCollection);*/
    }



}
