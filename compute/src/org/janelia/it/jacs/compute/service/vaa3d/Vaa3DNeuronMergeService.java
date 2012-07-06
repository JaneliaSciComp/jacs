package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;

import java.io.File;
import java.io.FileWriter;

/**
 * Merge neuron fragments.
 * Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 * 
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class Vaa3DNeuronMergeService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "neuMergeConfiguration.";

    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "neuMerge";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        EntityHelper helper = new EntityHelper();
        Entity separationEntity = EJBFactory.getLocalEntityBean().getEntityById(task.getParameter(NeuronMergeTask.PARAM_separationEntityId));

//        String originalImageFilePath = separationEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
//        String consolidatedSignalLabelIndexFilePath;
//        String commaSeparatedFragmentList;
//        String newOutputMIPPath;
//        String newOutputStackPath;
//
//        if (inputFileNode==null) {
//            throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
//        }

        writeInstanceFiles();
        setJobIncrementStop(1);
//        createShellScript(writer, originalImageFilePath, consolidatedSignalLabelIndexFilePath,
//                commaSeparatedFragmentList, newOutputMIPPath, newOutputStackPath);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath());
        }
    }

    /**
     * Write the shell script that runs the stitcher on the merged files.
     * @param writer
     * @param
     * @param
     * @throws Exception
     */
    private void createShellScript(FileWriter writer, String originalImageFilePath, String consolidatedSignalLabelIndexFilePath,
                                   String commaSeparatedFragmentList, String newOutputMIPPath, String newOutputStackPath) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedNeuronMergeCommand(originalImageFilePath, consolidatedSignalLabelIndexFilePath,
                commaSeparatedFragmentList, newOutputMIPPath, newOutputStackPath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
        SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
        // Reserve 4 slots on a node. This gives us 12 GB of memory.
        jt.setNativeSpecification("-pe batch 4");
        return jt;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

}
