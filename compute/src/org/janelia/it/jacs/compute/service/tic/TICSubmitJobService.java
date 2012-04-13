package org.janelia.it.jacs.compute.service.tic;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.geci.MatlabHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tic.TicTask;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class TICSubmitJobService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "ticConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "tic";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        //ProfileComparisonTask profileComparisonTask = (ProfileComparisonTask) task;
        //Get the input file list
        List<String> inputFileList = Task.listOfStringsFromCsvString(processData.getString("INPUT_FILES"));
        String calibrationFilePath = task.getParameter(TicTask.PARAM_calibrationFile);
        String correctionFactorFilePath = task.getParameter(TicTask.PARAM_correctionFactorFile);
        // Creating the config files for the Drmaa Template
        for (int i = 0; i < inputFileList.size(); i++) {
            String tmpInputFile = inputFileList.get(i);
            FileWriter fw = new FileWriter(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + (i+1));
            File tmpFile= new File(tmpInputFile);
            String tmpName=tmpFile.getName();
            String outputPath = resultFileNode.getDirectoryPath()+File.separator+tmpName.substring(0,tmpName.lastIndexOf("."));
            try {
                // Path to the input file
                fw.write(tmpInputFile+"\n");
                // Input file name
                fw.write(tmpName+"\n");
                // Result Node path to the specific results
                fw.write(outputPath+"\n");
            }
            finally {
                fw.close();
            }
        }

        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
        String reconstructionCmd = basePath + SystemConfigurationProperties.getString("TIC.Reconstruction.Cmd");
        String correctionCmd = basePath + SystemConfigurationProperties.getString("TIC.Correction.Cmd");
//        String fishQuantCmd = basePath + SystemConfigurationProperties.getString("TIC.FishQuant.Cmd");

        // Takes a list of files, smart enough to figure out the file type based on extension
        writer.write("read INPUT_FILE\n");
        writer.write("read INPUT_FILE_NAME\n");
        writer.write("read OUTPUT_DIR\n");
        writer.write("mkdir $OUTPUT_DIR\n");
        writer.write("cp $INPUT_FILE $OUTPUT_DIR"+File.separator+".\n");
        String fullReconstructionCmd = reconstructionCmd + " $OUTPUT_DIR"+File.separator+" $INPUT_FILE_NAME "+calibrationFilePath+"\n";
        fullReconstructionCmd = MatlabHelper.MATLAB_EXPORT + fullReconstructionCmd;
        writer.write(fullReconstructionCmd);
        String fullCorrectionCmd = correctionCmd + " $OUTPUT_DIR"+File.separator+"Reconstructed"+File.separator+" "+correctionFactorFilePath+"\n";
        writer.write(fullCorrectionCmd);
        setJobIncrementStop(inputFileList.size());
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

}