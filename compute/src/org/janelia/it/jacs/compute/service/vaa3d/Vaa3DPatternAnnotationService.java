package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 2/2/12
 * Time: 7:33 PM
 */
public class Vaa3DPatternAnnotationService extends SubmitDrmaaJobService {

        List<String> sampleIdList;
        List<String> sampleNameList;
        List<String> patternAnnotationPathList;
        List<String> alignedStackPathList;
        String resourceDirPath;
        int patternChannel;

    @Override
    public void init(IProcessData processData) throws Exception {
        super.init(processData);
        sampleIdList=(List<String>)processData.getItem("SAMPLE_ID_LIST");
        sampleNameList=(List<String>)processData.getItem("SAMPLE_NAME_LIST");
        patternAnnotationPathList=(List<String>)processData.getItem("PATTERN_ANNOTATION_PATH");
        alignedStackPathList=(List<String>)processData.getItem("ALIGNED_STACK_PATH_LIST");
        resourceDirPath=processData.getString("RESOURCE_DIR_PATH");
        patternChannel=new Integer(processData.getString("PATTERN_CHANNEL").trim());

        if (sampleNameList==null) {
            logger.info("Vaa3DPatternAnnotationService init() sampleName list is null");
        } else {
            logger.info("Vaa3DPatternAnnotationService init() sampleName list contains "+sampleNameList.size()+" entries");
        }

        for (String sampleName : sampleNameList) {
            logger.info("Received sampleName="+sampleName);
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "patternAnnotation";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;
        logger.info("Vaa3DPatternAnnotationService createJobScriptAndConfigurationFiles() start");
        for(String sampleName : sampleNameList) {
            logger.info("Vaa3DPatternAnnotationService createJobScriptAndConfigurationFiles() sampleName="+sampleName);
            String patternAnnotationPath = patternAnnotationPathList.get(configIndex-1);
            String alignedStackPath = alignedStackPathList.get(configIndex-1);
            writeInstanceFiles(sampleName, patternAnnotationPath, alignedStackPath, configIndex);
            configIndex++;
        }
        writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    protected void writeInstanceFiles(String sampleName, String patternAnnotationPath, String alignedStackPath, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, sampleName, patternAnnotationPath, alignedStackPath, configIndex);
        }
        catch (IOException e) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e);
        }
        finally {
            fw.close();
        }
    }

    protected void writeInstanceFile(FileWriter fw, String sampleName, String patternAnnotationPath, String alignedStackPath, int configIndex) throws IOException {
        fw.write(sampleName+"\n");
        fw.write(patternAnnotationPath+"\n");
        fw.write(alignedStackPath+"\n");
     }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read SAMPLE_NAME\n");
        script.append("read PATTERN_ANNOTATION_PATH\n");
        script.append("read ALIGNED_STACK_PATH\n");
        script.append(Vaa3DHelper.getVaa3dHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getPatternAnnotationCommand("$ALIGNED_STACK_PATH", patternChannel, "$SAMPLE_NAME", "\"" + resourceDirPath + "\"", "$PATTERN_ANNOTATION_PATH"));
        script.append("\n");
        script.append(Vaa3DHelper.getHeadlessGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 12;
    }

    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());

    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), getGridServicePrefixName()+" core dumped for "+resultFileNode.getDirectoryPath());
    	}
	}
}