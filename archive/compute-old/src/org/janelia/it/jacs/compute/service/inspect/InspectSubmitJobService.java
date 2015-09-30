
package org.janelia.it.jacs.compute.service.inspect;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.inspect.InspectTask;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:47:21 PM
 */
public class InspectSubmitJobService extends SubmitDrmaaJobService {

    //  There must be at least one config file, even if it is not used
    private static final String CONFIG_PREFIX = "inspectConfiguration.";
    private String depotLocation;

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "inspect";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        String tmpName=task.getParameter(InspectTask.PARAM_archiveFilePath);
        // Cut off any extra slash.  We only want the "bug" name
        if (tmpName.endsWith("/")){ tmpName=tmpName.substring(0,tmpName.length()-1); }
        String critterName = tmpName.substring(tmpName.lastIndexOf("/")+1);
        // Things are stored in archive due to size, so go grab it for execution
        depotLocation=resultFileNode.getDirectoryPath()+File.separator+critterName;
        createShellScript(writer);
        File jobsDir = new File(depotLocation+File.separator+"jobs");
        File[] jobsFiles = jobsDir.listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir.getAbsolutePath()+File.separator+name).isFile() && name.toLowerCase().endsWith(".in");
            }
        });
        int configCounter=1;
        for (File jobFile : jobsFiles) {
            FileWriter configFileWriter = new FileWriter(new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + configCounter));
            try {
                configFileWriter.write(jobFile.getAbsolutePath());
            }
            finally {
                configFileWriter.close();
            }
            configCounter++;
        }
        setJobIncrementStop(configCounter-1);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
        String codePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
        String pipelineCmd = codePath+SystemConfigurationProperties.getString("Inspect.Cmd");

        StringBuffer script = new StringBuffer();
        script.append("set -o errexit\n");
        script.append("cd ").append(depotLocation).append("\n");
        script.append(pipelineCmd).append("\n");
        writer.write(script.toString());
    }

    @Override
    protected String getSGEQueue() {
        return "-l medium";
    }
}