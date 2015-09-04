package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Merge neuron fragments.
 * Parameters:
 *
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class Vaa3DNeuronMergeService extends SubmitDrmaaJobService {

    protected static final String CONVERT_BASE_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ImageMagick.Bin.Path")+"/convert";

    protected static final String CONVERT_LIB_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ImageMagick.Lib.Path");

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "neuMergeConfiguration.";

    public void init(IProcessData processData) throws Exception {
        //processData.
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "neuMerge";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        ArrayList<Integer> tmpFragmentNumberList = new ArrayList<Integer>();
        String commaSeparatedFragmentIdList=task.getParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList);
        for (String tmpFragmentOid : Task.listOfStringsFromCsvString(commaSeparatedFragmentIdList)) {
            // TAS 4/17/2014 At some point the neuron-fragment-editor vaa3d plug-in was off by one.  Adding 1 to the values going into the fragment list
            tmpFragmentNumberList.add(Integer.valueOf(entityBean.getEntityById(tmpFragmentOid).getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER))+1);
        }
        String commaSeparatedFragmentList = Task.csvStringFromCollection(tmpFragmentNumberList);
        Entity separationEntity = entityBean.getEntityById(task.getParameter(NeuronMergeTask.PARAM_separationEntityId));
        String tmpPath = separationEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).getValue();
        String originalImageFilePath = tmpPath+File.separator+"ConsolidatedSignal.v3dpbd";
        String consolidatedSignalLabelIndexFilePath=tmpPath+File.separator+"ConsolidatedLabel.v3dpbd";


        // Get the next Neuron Fragment Number for this Separation
        String originalOutputMIPPath=resultFileNode.getDirectoryPath()+File.separator+"CuratedNeuronMIP.tif";
        String newOutputStackPath=resultFileNode.getDirectoryPath()+File.separator+"CuratedNeuronStack.v3dpbd";

        writeInstanceFiles();
        setJobIncrementStop(1);
        createShellScript(writer, originalImageFilePath, consolidatedSignalLabelIndexFilePath,
                commaSeparatedFragmentList, originalOutputMIPPath, newOutputStackPath);
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
        String finalOutputMIPPath = resultFileNode.getDirectoryPath()+File.separator+"CuratedNeuronMIP.png";
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedNeuronMergeCommand(originalImageFilePath, consolidatedSignalLabelIndexFilePath,
                commaSeparatedFragmentList, newOutputMIPPath, newOutputStackPath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("export LD_LIBRARY_PATH="+CONVERT_LIB_DIR+"\n");
        script.append(CONVERT_BASE_CMD).append(" -flip ").append(newOutputMIPPath).append(" ").append(finalOutputMIPPath).append(" \n");
        script.append("rm -f ").append(newOutputMIPPath).append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 12;
    }

    @Override
    protected boolean isShortJob() {
    	return true;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

}
