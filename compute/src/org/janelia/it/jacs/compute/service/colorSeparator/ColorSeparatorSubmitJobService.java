
package org.janelia.it.jacs.compute.service.colorSeparator;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:47:21 PM
 */
public class ColorSeparatorSubmitJobService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "colorSepConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "colorSep";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        List<String> filePathList = Task.listOfStringsFromCsvString(task.getParameter(ColorSeparatorTask.PARAM_inputFileList));
        // We run this on a array of images
        if (null==filePathList||0==filePathList.size()) {
            throw new ServiceException("A list of files must be provided!");
        }
        String resultsDirPrefix = resultFileNode.getDirectoryPath()+File.separator+"colorSepResults";

        // Loop and create config files for all executions.  Index must start at 1 for SGE
        int index = 1;
        for (String tmpInputFile : filePathList) {
            String tmpResultDirName = resultsDirPrefix+index;
            FileUtil.ensureDirExists(tmpResultDirName);
            FileWriter configWriter = new FileWriter(getSGEConfigurationDirectory()+File.separator+CONFIG_PREFIX+index);
            try {
                configWriter.write(tmpInputFile+"\n");
                configWriter.write(tmpResultDirName+"\n");
            }
            finally {
                configWriter.close();
            }
            index++;

        }
        createShellScript(writer);
        setJobIncrementStop(filePathList.size());
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");

        int numColors = 5;
        // Cmdline: ColorSep 'image_name' 'number of colors' 'destination_folder'
        String pipelineCmd = basePath + SystemConfigurationProperties.getString("ColorSep.PipelineCmd")+" "+
                "$INPUT_PATH"+" "+numColors+" $OUTPUT_PATH";
//        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
//        String tmpDirectoryName = properties.getProperty("Upload.ScratchDir");

        StringBuffer script = new StringBuffer();
        script.append("set -o errexit\n");
        script.append("read INPUT_PATH\n");
        script.append("read OUTPUT_PATH\n");
        script.append("cd ").append(resultFileNode.getDirectoryPath()).append("\n");
        script.append(pipelineCmd).append("\n");
        writer.write(script.toString());
    }

}