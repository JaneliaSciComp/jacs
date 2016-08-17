package org.janelia.it.jacs.compute.service.neuronClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.neuron.NeuronLineageClassifierResultNode;
import org.janelia.it.jacs.shared.img_3d_loader.V3dSignalFileLoader;
import org.janelia.it.jacs.shared.utils.FileUtil;

import static com.mongodb.client.model.Filters.eq;

/**
 * Kick off a prediction step for a fly neuron "lineage" classification, generating probabilities for each lineage
 *   SAMPLE_ID - sample that will be processed for metadata
 *   LSMIMAGE_ID - lsm that will be processed directly for lineage classification
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronClassifierService extends AbstractDomainService {

    final public String MODE_TRAINING="TRAINING";
    final public String MODE_PREDICTION="PREDICTION";
    final public String MODE_NODE_CREATION = "NODECREATION";
    final public String MODE_PROCESSRESULTS="PROCESSRESULTS";

    private SampleHelperNG sampleHelper;
    protected List<String> neuronSeparationsToProcess;

    public void execute() throws Exception {
        logger.info("Getting MODE");
        String mode = processData.getString("MODE");

        if (mode.equals(MODE_TRAINING)) {
            generateModelsFromTraining();
        } else if (mode.equals(MODE_PREDICTION)) {
            generatePrediction();
        } else if (mode.equals(MODE_NODE_CREATION)) {
            createResultNode();
        } else if (mode.equals(MODE_PROCESSRESULTS)) {
            processLineageClassifierResults();
        } else {
            throw new Exception("Do not recognize mode="+mode);
        }
    }

    private void createResultNode() throws Exception {
        Task task = ProcessDataHelper.getTask(processData);
        String sessionName = ProcessDataHelper.getSessionRelativePath(processData);

        // if we get this far then we assume that no result persist exists and create one
        Task loadedTask = EJBFactory.getLocalComputeBean().getTaskById(task.getObjectId());
        NeuronLineageClassifierResultNode neuronFileNode = new NeuronLineageClassifierResultNode(loadedTask.getOwner(), loadedTask, "LineageClassifier",
                "Neuron Lineages for createtask " + loadedTask.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(neuronFileNode);

        FileUtil.ensureDirExists(neuronFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(neuronFileNode.getDirectoryPath());

        processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_ID, neuronFileNode.getObjectId());
        processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_DIR, neuronFileNode.getDirectoryPath());
        logger.debug("Created Lineage Classifier Directory and placed in processData id=" + neuronFileNode.getObjectId());
    }

    private void generateModelsFromTraining() throws Exception {
        logger.info("Starting query to retrieve annotation and image information for training model data");

        String[] sampleIdList = processData.getString("SAMPLE_ID_LIST").split(",");
        List<String> jsonResults = new ArrayList<String>();

        for (int i=0; i<sampleIdList.length; i++) {
            Long sampleId = Long.parseLong(sampleIdList[i]);

            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            MongoClient m = dao.getMongo();
            MongoDatabase db = m.getDatabase("jacs");
            MongoCollection<Document> annotation = db.getCollection("annotation");
            List<Document> annotationResults;
            Document metadata = new Document();

            Document data = findLSMInfo(sampleId);
            if (data!=null) {
                List<String> annotationList = new ArrayList<>();
                annotationResults = annotation.find(eq("target", "Sample#"+sampleId)).into(new ArrayList());

                for (Document annotationDoc : annotationResults) {
                    if (annotationDoc.get("key") != null) {
                        annotationList.add(annotationDoc.getString("key"));
                    }
                }
                if (annotationList.size()>0) {
                    data.put("annotations", annotationList);
                }
            }
            jsonResults.add(data.toJson());
        }
        String finalJson = "[" + StringUtils.join(jsonResults, ",") + "]";
        logger.info("generatePrediction() putting vars in processData, metadata=" + finalJson);
        OutputStream foo = Files.newOutputStream(Paths.get("/groups/jacs/jacsDev/servers/schauderd-ws1/dump/input.json"));
        foo.write(finalJson.getBytes());
        processData.putItem("METADATA", finalJson);

    }

    private void generatePrediction() throws Exception {
        logger.info("Starting query to retrieve annotation and image information for creating lineage classification probabilities");

        String sampleIdString = processData.getString("SAMPLE_ID");
        Long sampleId = Long.parseLong(sampleIdString);

        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> annotation = db.getCollection("annotation");
        List<Document> annotationResults;
        Document metadata = new Document();

        Document data = findLSMInfo(sampleId);

        if (data!=null) {
            logger.info("generatePrediction() putting vars in processData, metadata=" + data.toJson());
            processData.putItem("METADATA", data.toJson());
        }
    }


    // read all the files out of the output directory and process them into the database
    private void processLineageClassifierResults() throws Exception {
        logger.info("Storing lineage classifiers back into flat file");
        String outputDir = processData.getString("RESULT_FILE_NODE_DIR") + File.separator + "sge_output";

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(outputDir))) {
            ObjectMapper mapper = new ObjectMapper();
            for (Path path : directoryStream) {
                List<Map> results = mapper.readValue(Files.newInputStream(path), new TypeReference<List<Map>>(){});

            }
        }
    }

    private String retrieveSampleAlignmentResult(Document objectiveSample) {
        List<Document> pipelineRuns = (List)objectiveSample.get("pipelineRuns");
        for (Document pipelineRun: pipelineRuns) {
            List<Document> results = (List)pipelineRun.get("results");
            for (Document result: results) {
                if (result.getString("class").equals("org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult")) {
                    Map<String,String> files = (Map)result.get("files");
                    if (files.get("LosslessStack")!=null) {
                        // get the V3dpbd file and copy it into the temp file location
                        return files.get("LosslessStack");
                    }
                }

            }
        }
        return null;
    }


    private Document findLSMInfo(Long sampleId) {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        MongoClient m = dao.getMongo();
        MongoDatabase db = m.getDatabase("jacs");
        MongoCollection<Document> sample = db.getCollection("sample");
        MongoCollection<Document> image = db.getCollection("image");

        List<Document> results;
        List<Document> imageResults;
        results = sample.find(eq("_id", sampleId)).batchSize(1000000).into(new ArrayList());

        Document sampleDoc = results.get(0);

        if (sampleDoc!=null) {
            List<Document> objectiveSamples = (List<Document>) sampleDoc.get("objectiveSamples");
            for (Document objectiveSample : objectiveSamples) {
                if (!(objectiveSample.getString("objective")!=null && objectiveSample.getString("objective").equals("40x")))
                    continue;
                // find the sample alignment result
                String losslessStack = retrieveSampleAlignmentResult(objectiveSample);

                List<Document> tiles = (List<Document>) objectiveSample.get("tiles");
                Document tileDoc = tiles.get(0);
                if (tileDoc!=null) {
                    String[] lsmStringIds = tileDoc.get("lsmReferences").toString().replace("[", "").replace("]", "").replace("LSMImage#", "").split(",");
                    if (lsmStringIds!=null && lsmStringIds.length>0) {
                        Long lsmId = Long.parseLong(lsmStringIds[0].trim());
                        imageResults = image.find(eq("_id", lsmId)).into(new ArrayList());
                        if (imageResults.size()>0) {
                            Document imageResult = imageResults.get(0);
                            losslessStack = copyV3dPbdToLocalRaw(sampleDoc.getString("name"), losslessStack);
                            imageResult.put("losslessStack", losslessStack);
                            return imageResult;
                        }

                    }
                }
            }
        }
        return null;
    }

    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2Mb
    private static final int EOF = -1;

    private int convertPbdToRaw (String dumpFile) throws Exception {
        V3dSignalFileLoader signal = new V3dSignalFileLoader();
        signal.loadVolumeFile(dumpFile);
        byte[] buffer0 = new byte[43];
        ByteBuffer buffer = ByteBuffer.wrap(buffer0);
        String magic = "raw_image_stack_by_hpeng";
        byte[] b = magic.getBytes("UTF-8");
        buffer.put(b);
        String endianness = "B";
        buffer.put(endianness.getBytes("UTF-8"));
        short dataType = 4;
        buffer.putShort(dataType);
        buffer.putInt(signal.getSx());
        buffer.putInt(signal.getSy());
        buffer.putInt(signal.getSz());
        buffer.putInt(signal.getChannelCount());
        OutputStream moo = Files.newOutputStream(Paths.get(dumpFile.replaceFirst("v3dpbd","raw")));
        moo.write(buffer.array());
        moo.write(signal.getTextureByteArray());
        moo.close();
        return 1;
    }

    private String copyV3dPbdToLocalRaw(String sampleName, String path) {
        InputStream input = null;
        FileOutputStream output = null;
        GetMethod getMethod = null;

        String fullUrl = "http://jacs-webdav:8080/JFS/api/file" + path;
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(2); // default is 2
        managerParams.setMaxTotalConnections(20);            // default is 20
        HttpClient httpClient = new HttpClient(mgr);
        String dumpFile = "/groups/jacs/jacsDev/servers/schauderd-ws1/dump/" + sampleName + ".v3dpbd";
        final int responseCode;
        try {
            getMethod = new GetMethod(fullUrl);
            responseCode = httpClient.executeMethod(getMethod);
            if (responseCode == HttpServletResponse.SC_OK) {
                input = getMethod.getResponseBodyAsStream();
                output = new FileOutputStream(new File(dumpFile));
                byte[] buffer = new byte[BUFFER_SIZE];
                int n;
                while (EOF != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
                if (convertPbdToRaw (dumpFile)==1) {
                    Files.delete(Paths.get(dumpFile));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "/groups/jacs/jacsDev/servers/schauderd-ws1/dump/" + sampleName + ".raw";

    }

}
