package org.janelia.it.jacs.compute.service.v3d;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.v3d.V3DPipelineTask;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 8, 2010
 * Time: 10:18:16 PM
 */
public class V3DPipelineService extends SubmitDrmaaJobService {
    private static final String temporaryDirectory = SystemConfigurationProperties.getString("computeserver.ScratchDir");
    protected Logger _logger;

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.processData = processData;
        // Permit the task to be predefined elsewhere
        if (this.task == null) {
            this.task = ProcessDataHelper.getTask(processData);
        }
        resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        this.jobSet = new HashSet<String>();
        // ensure the SGE dirs exist
        FileUtil.ensureDirExists(getSGEConfigurationDirectory());
        FileUtil.ensureDirExists(getSGEOutputDirectory());
        FileUtil.ensureDirExists(getSGEErrorDirectory());
        this._logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
    }

    @Override
    protected String getGridServicePrefixName() {
        return task.getTaskName();
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        String fullCmd = V3DHelper.getV3DGridCommandPrefix();
        fullCmd+=getCommandLines();
        writer.write(fullCmd);
        writer.write(V3DHelper.getV3DGridCommandSuffix());
        boolean success = new File(getSGEConfigurationDirectory()+File.separator+task.getTaskName()+"Configuration.1").createNewFile();
        if (!success) { throw new ServiceException("Unable to create SGE Configuration directory"); }
        success = new File(getSGEOutputDirectory()+File.separator+task.getTaskName()+"Output").createNewFile();
        if (!success) { throw new ServiceException("Unable to create SGE Output directory"); }
        setJobIncrementStop(1);
    }

    public String getCommandLines() throws Exception {
        StringBuilder sbuf = new StringBuilder();
        String inputPaths = task.getParameter(V3DPipelineTask.PARAM_INPUT_FILE_PATHS);
        List<String> inputFiles = Task.listOfStringsFromCsvString(inputPaths);
        if (inputFiles.size()>1) {
            throw new ServiceException("Functionality not yet implemented.");
        }
        String lastDirectoryPointer=resultFileNode.getDirectoryPath();
        File inputImageDirectory = new File(inputPaths);
//        File slideGroupInfoFile = new File(inputPaths+File.separator+"slide_group_info.txt");
//        if (!slideGroupInfoFile.exists()) {
//            throw new ServiceException("Cannot find the slide_group_info.txt file in "+inputImageDirectory.getAbsolutePath());
//        }
//        HashMap<String, FilePair> filePairings = new HashMap<String, FilePair>();
//        Scanner scanner = new Scanner(slideGroupInfoFile);
//        String identifier=null;
//        while (scanner.hasNextLine()){
//            String[] pieces = scanner.nextLine().split("\t");
//            if (null==filePairings.get(pieces[1])) {
//                filePairings.put(pieces[1], new FilePair(pieces[1], pieces[0]));
//            }
//            else {
//                filePairings.get(pieces[1]).setFilename2(pieces[0]);
//            }
//            if (null==identifier) {
//                identifier = pieces[2];
//            }
//        }
        if (Boolean.valueOf(task.getParameter(V3DPipelineTask.PARAM_RUN_MERGE))){
            // Assume the path is a directory and get the image info
            lastDirectoryPointer = lastDirectoryPointer+File.separator+"merged";
//            for (FilePair filePair : filePairings.values()) {
//                if (filePair.isPairingComplete()) {
                    File[] childFiles = inputImageDirectory.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            return s.toLowerCase().endsWith(".lsm");
                        }
                    });
                    String tmpUniqueName = "pipeline-"+inputImageDirectory.getName()+".v3draw";
                    tmpUniqueName = tmpUniqueName.replaceAll(" ","_");
                    String tmpOutputFilename = lastDirectoryPointer+File.separator+tmpUniqueName;
                    sbuf.append(V3DHelper.getFormattedMergeCommand(childFiles[0].getAbsolutePath(),
                            childFiles[1].getAbsolutePath(), tmpOutputFilename)).append("\n");
//                }
//            }
        }
//        if (Boolean.valueOf(task.getParameter(V3DPipelineTask.PARAM_RUN_STITCH))) {
//            sbuf.append(V3DHelper.getFormattedStitcherCommand(lastDirectoryPointer)).append("\n");
//        }
//        if (Boolean.valueOf(task.getParameter(V3DPipelineTask.PARAM_RUN_BLEND))) {
//            String tmpUniqueName = identifier+"-"+inputImageDirectory.getName()+"-stitched.v3draw";
//            tmpUniqueName = tmpUniqueName.replaceAll(" ","_");
//            String tmpOutputFilename = inputFiles.get(0)+File.separator+"stitched"+tmpUniqueName;
//            sbuf.append(V3DHelper.getFormattedBlendCommand(lastDirectoryPointer,tmpOutputFilename)).append("\n");
//        }
        return sbuf.toString();
    }

    public String getServiceName() {
        return this.getClass().getSimpleName();
    }

    private class FilePair {
        private String pairTag;
        private String filename1;
        private String filename2;

        public FilePair(String pairTag, String filename1) {
            this.pairTag = pairTag;
            this.filename1 = filename1;
        }

        public String getFilename1() {
            return filename1;
        }

        public String getFilename2() {
            return filename2;
        }

        public void setFilename2(String filename2) {
            this.filename2 = filename2;
        }

        public String getPairTag() {
            return pairTag;
        }

        public boolean isPairingComplete() {
            return (null!=filename1&&!"".equals(filename1)) &&
                   (null!=filename2&&!"".equals(filename2));
        }
    }

}
