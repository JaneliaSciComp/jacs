package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class NeuronSeparationPipelineService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    @Override
    protected String getGridServicePrefixName() {
        return "neuSep";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the Neuron Separation pipeline.");
        }

        createShellScript(writer);
        setJobIncrementStop(1);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
        Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        NeuronSeparatorPipelineTask task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
        NeuronSeparatorResultNode parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);

        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + parentNode.getObjectId() + " resultDir=" + parentNode.getDirectoryPath());

        String fileList = NeuronSeparatorHelper.getFileListString(task);
        String cmdLine = "cd "+ parentNode.getDirectoryPath()+";export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:" +
                SystemConfigurationProperties.getString("Executables.ModuleBase")+"genelib/mylib;"+
                SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib/sampsepNA -nr -pj "+
                parentNode.getDirectoryPath()+" neuronSeparatorPipeline "+ addQuotesToCsvString(fileList) + " >neuSepOutput.txt 2>&1 ";

        StringBuffer script = new StringBuffer();
        script.append("set -o errexit\n");
        script.append(cmdLine).append("\n");

        String[] lsmFilePaths = NeuronSeparatorHelper.getLSMFilePaths(task);
    	String lsmFilePathsFilename = parentNode.getDirectoryPath()+"/"+"lsmFilePaths.txt";
        script.append("echo '"+lsmFilePaths[0]+"' > "+lsmFilePathsFilename).append("\n");
        script.append("echo '"+lsmFilePaths[1]+"' >> "+lsmFilePathsFilename).append("\n");
    
        script.append(getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[0])).append("\n");
        script.append(getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[1])).append("\n");

        logger.info("NeuronSeparatorPipelineService script=\n" + script);
        
        writer.write(script.toString());
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    private String getScriptToCreateLsmMetadataFile(NeuronSeparatorResultNode parentNode, String lsmPath) throws ServiceException {

        File lsmFile = new File(lsmPath);
        if (!lsmFile.exists()) {
            throw new ServiceException("Could not find LSM file "+lsmFile.getAbsolutePath());
        }
        File lsmDataFile=new File(parentNode.getDirectoryPath()+"/"+createLsmMetadataFilename(lsmFile)+".metadata");
        String cmdLine = "cd " + parentNode.getDirectoryPath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "singleNeuronTools/lsm_metadata_dump.pl " +
                addQuotes(lsmPath) + " " + addQuotes(lsmDataFile.getAbsolutePath());

        return cmdLine;
    }

    private String addQuotes(String s) {
    	return "\""+s+"\"";
    }

    private String addQuotesToCsvString(String csvString) {
        String[] clist=csvString.split(",");
        StringBuffer sb=new StringBuffer();
        for (int i=0;i<clist.length;i++) {
            sb.append("\"");
            sb.append(clist[i].trim());
            sb.append("\"");
            if (i<clist.length-1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
    
    private String createLsmMetadataFilename(File lsmFile) {
        return lsmFile.getName().replaceAll("\\s+","_");
    }
}