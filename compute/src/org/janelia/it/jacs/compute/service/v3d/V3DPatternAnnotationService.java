package org.janelia.it.jacs.compute.service.v3d;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 2/2/12
 * Time: 7:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class V3DPatternAnnotationService extends SubmitDrmaaJobService {

        List<String> sampleIdList;
        List<String> sampleNameList;
        List<String> patternAnnotationPathList;
        List<String> alignedStackPathList;
        String resourceDirPath;
        int patternChannel;

    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        sampleIdList=(List<String>)processData.getItem("SAMPLE_ID_LIST");
        sampleNameList=(List<String>)processData.getItem("SAMPLE_NAME_LIST");
        patternAnnotationPathList=(List<String>)processData.getItem("PATTERN_ANNOTATION_PATH");
        alignedStackPathList=(List<String>)processData.getItem("ALIGNED_STACK_PATH_LIST");
        resourceDirPath=processData.getString("RESOURCE_DIR_PATH");
        patternChannel=new Integer(processData.getString("PATTERN_CHANNEL").trim());

        if (sampleNameList==null) {
            logger.info("V3DPatternAnnotationService init() sampleName list is null");
        } else {
            logger.info("V3DPatternAnnotationService init() sampleName list contains "+sampleNameList.size()+" entries");
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
        int i = 0;
        int configIndex = 1;
        logger.info("V3DPatternAnnotationService createJobScriptAndConfigurationFiles() start");
        for(String sampleName : sampleNameList) {
            logger.info("V3DPatternAnnotationService createJobScriptAndConfigurationFiles() sampleName="+sampleName);
            String patternAnnotationPath = patternAnnotationPathList.get(i);
            String alignedStackPath = alignedStackPathList.get(i);
            writeInstanceFiles(sampleName, patternAnnotationPath, alignedStackPath, configIndex++);
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
        script.append(V3DHelper.getHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(V3DHelper.getPatternAnnotationCommand("$ALIGNED_STACK_PATH", patternChannel, "$SAMPLE_NAME", "\""+resourceDirPath+"\"", "$PATTERN_ANNOTATION_PATH"));
        script.append("\n");
        script.append(V3DHelper.getHeadlessGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve 4 out of the 8 slots on a node. This gives us 12 GB of memory.
    	jt.setNativeSpecification("-pe batch 4");
    	return jt;
    }
}