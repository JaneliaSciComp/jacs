
package org.janelia.it.jacs.compute.service.inspect;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.inspect.InspectTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.inspect.InspectResultNode;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * @author Todd Safford
 */
public class InspectMSGFService extends SubmitDrmaaJobService {

    //  There must be at least one config file, even if it is not used
    private static final String CONFIG_PREFIX = "inspectMSGFConfiguration.";
    private String depotLocation;

    @Override
    protected String getGridServicePrefixName() {
        return "inspectMSGF";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        try {
            Logger _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            FileNode resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            String tmpName=task.getParameter(InspectTask.PARAM_archiveFilePath);
            // Cut off any extra slash.  We only want the "bug" name
            if (tmpName.endsWith("/")){ tmpName=tmpName.substring(0,tmpName.length()-1); }
            String critterName = tmpName.substring(tmpName.lastIndexOf("/")+1);
            // Things are stored in archive due to size, so go grab it for execution
            depotLocation= resultFileNode.getDirectoryPath()+ File.separator+critterName;
            createShellScript(writer);
            File resultsXDir = new File(depotLocation+File.separator+"ResultsX");
            File pvalueDir = new File(resultsXDir.getAbsolutePath()+File.separator+ InspectResultNode.TAG_PVALUE_DIR);
            File[] pvalueFiles = pvalueDir.listFiles(new FilenameFilter(){
                public boolean accept(File dir, String name) {
                    return name.endsWith(".res");
                }
            });

            // Example archive path /usr/local/archive/projects/PGP/Arthrobacter.FB24/
            int configCounter=1;
            for (File pvalueFile : pvalueFiles) {
                FileWriter configFileWriter = new FileWriter(new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + configCounter));
                try {
                    configFileWriter.write(pvalueFile.getAbsolutePath()+"\n");
                    configFileWriter.write(configCounter+"\n");
                }
                finally {
                    configFileWriter.close();
                }
                configCounter++;
            }
            setJobIncrementStop(configCounter-1);
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private void createShellScript(FileWriter writer) throws IOException {
        // NOTE: May need to run on the grid for each
        String javaPath = SystemConfigurationProperties.getString("Java.Path");
        String fullCmd = javaPath+ " -Xmx1000M -jar /usr/local/depot/projects/PGP/MSGF/MSGF.jar -inspect $PVALUE_FILE"+
                " -d "+depotLocation+File.separator+"mzxml -x 0 > $PVALUE_FILE_INDEX.msgf";
        StringBuffer script = new StringBuffer();
        script.append("set -o errexit\n");
        script.append("read PVALUE_FILE\n");
        script.append("read PVALUE_FILE_INDEX\n");
        script.append("cd "+depotLocation+File.separator+"ResultsX/"+InspectResultNode.TAG_MSGF_DIR+"\n");
        script.append(fullCmd).append("\n");
        script.append("bzip2 "+InspectResultNode.TAG_MSGF_DIR+File.separator+
                "$PVALUE_FILE_INDEX.msgf "+InspectResultNode.TAG_PEPNOVO_DIR+"/$PVALUE_FILE_INDEX.res "+
                InspectResultNode.TAG_PVALUE_DIR+"/$PVALUE_FILE_INDEX.res");
        writer.write(script.toString());
   }

    @Override
    protected String getSGEQueue() {
        return "-l medium";
    }

}