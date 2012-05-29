package org.janelia.it.jacs.compute.service.tic;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tic.TicTask;

import java.io.File;
import java.io.FileFilter;
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
        //Get the input file prefix
        String inputFilePath = task.getParameter(TicTask.PARAM_inputFilePrefix);
        if (null==inputFilePath||0==inputFilePath.length()) {
            logger.error("TIC Pipeline cannot be run with no input file prefix.");
            throw new ServiceException("Found no input file prefix to run against.");
        }
        String transformationFilePath = task.getParameter(TicTask.PARAM_transformationFile);
        String correctionFactorFilePath = task.getParameter(TicTask.PARAM_intensityCorrectionFactorFile);
        String borderValue = (task.getParameter(TicTask.PARAM_borderValue)==null || "".equals(task.getParameter(TicTask.PARAM_borderValue)))?
                "\"\"":task.getParameter(TicTask.PARAM_borderValue);
        String microscopeSettingsFilePath = task.getParameter(TicTask.PARAM_microscopeSettingsFile);
        // Write out the avgReadNoise and avgDark settings file
        FileWriter noiseWriter=null;
        String noiseFile = resultFileNode.getDirectoryPath()+File.separator+"noiseAndDarkSettings.txt";
        List<String> noise = Task.listOfStringsFromCsvString(task.getParameter(TicTask.PARAM_avgReadNoise));
        List<String> dark = Task.listOfStringsFromCsvString(task.getParameter(TicTask.PARAM_avgDark));
        try {
            noiseWriter = new FileWriter(new File(noiseFile));
            for (int i = 0; i < noise.size(); i++) {
                noiseWriter.write(noise.get(i)+", "+dark.get(i)+"\n");
            }
        }
        finally {
            if (null!=noiseWriter) {
                noiseWriter.close();
            }
        }

        File tmpInputFile = new File(inputFilePath);
        String targetPrefixMinusLast = tmpInputFile.getName().substring(0, tmpInputFile.getName().lastIndexOf("_"));
        final String targetPrefix = targetPrefixMinusLast.substring(0,targetPrefixMinusLast.lastIndexOf("_"));
        File tmpDir = tmpInputFile.getParentFile();
        File[] relatedFiles = tmpDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().startsWith(targetPrefix);
            }
        });

        for (int i = 0; i < relatedFiles.length; i++) {
            FileWriter fw = new FileWriter(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + (i+1));
            File tmpFile= relatedFiles[i];
            String tmpName=tmpFile.getName();
            String outputPath = resultFileNode.getDirectoryPath()+File.separator+tmpName.substring(0,tmpName.lastIndexOf(".")).replaceAll(" ","_");
            try {
                // Path to the input file
                fw.write(tmpFile.getAbsolutePath()+"\n");
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
        String fishQuantCmd = basePath + SystemConfigurationProperties.getString("TIC.FishQuant.Cmd");

        // Takes a list of files, smart enough to figure out the file type based on extension
        writer.write("set -o errexit\n");
        writer.write("read INPUT_FILE\n");
        writer.write("read INPUT_FILE_NAME\n");
        writer.write("read OUTPUT_DIR\n");
        writer.write("mkdir $OUTPUT_DIR\n");
//        writer.write("mkdir /scratch/"+task.getObjectId()+"/"+"mcr_cache_root.$RANDOM");
        writer.write("cp $INPUT_FILE $OUTPUT_DIR"+File.separator+"$INPUT_FILE_NAME\n");
        writer.write(MatlabHelper.MATLAB_EXPORT + MatlabHelper.getCacheRootExportCommand("/scratch/jacs/"+task.getObjectId()+"/mcr_cache_root.$RANDOM")+"\n");

        if (null!=task.getParameter(TicTask.PARAM_runApplyCalibrationToFrame) && Boolean.valueOf(task.getParameter(TicTask.PARAM_runApplyCalibrationToFrame))) {
            String fullReconstructionCmd = reconstructionCmd + " $OUTPUT_DIR"+File.separator+" $INPUT_FILE_NAME "+transformationFilePath+" "+borderValue+"\n";
            writer.write(fullReconstructionCmd);
        }
        if (null!=task.getParameter(TicTask.PARAM_runIlluminationCorrection) && Boolean.valueOf(task.getParameter(TicTask.PARAM_runIlluminationCorrection))) {
            String fullCorrectionCmd = correctionCmd + " $OUTPUT_DIR"+File.separator+"Reconstructed"+File.separator+" "+correctionFactorFilePath+" "+noiseFile+"\n";
            writer.write(fullCorrectionCmd);
        }
        if (null!=task.getParameter(TicTask.PARAM_runFQBatch) && Boolean.valueOf(task.getParameter(TicTask.PARAM_runFQBatch))) {
            String fileListPath = "$OUTPUT_DIR"+File.separator+"Reconstructed"+File.separator+"corrected"+File.separator;
            writer.write("ls -1 "+fileListPath+"*.tif > "+fileListPath+"batchList.txt\n");
            String fullFQCmd = fishQuantCmd + " "+microscopeSettingsFilePath+" "+fileListPath+"batchList.txt\n";
            writer.write(fullFQCmd);
            writer.write("mv "+fileListPath+"FISH-QUANT__all_spots_* $OUTPUT_DIR/$INPUT_FILE_NAME.all_spots.txt\n");
        }
        setJobIncrementStop(relatedFiles.length);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
        SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
        // Reserve all 3 slots.
        jt.setNativeSpecification("-pe batch 1");
        return jt;
    }

}