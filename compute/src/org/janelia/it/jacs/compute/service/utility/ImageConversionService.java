package org.janelia.it.jacs.compute.service.utility;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.FileWriter;
import java.util.Map;

/**
 * Convert any number of images, in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageConversionService extends ParallelFileProcessingService {

    protected static final String CONVERT_BASE_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
    		SystemConfigurationProperties.getString("ImageMagick.Bin.Path")+"/convert";

    protected static final String CONVERT_LIB_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ImageMagick.Lib.Path");
    
    @Override
    public void init(IProcessData processData) throws Exception {
        String altWorkingPath=processData.getString("ALTERNATE_WORKING_DIR_PATH");
        Map<String, FileNode> nodeMap=(Map<String, FileNode>)processData.getItem("IMAGE_CONVERSION_RESULT_NODE_MAP");
        if (altWorkingPath!=null && nodeMap!=null) {
            FileNode resultFileNode=nodeMap.get(altWorkingPath);
            if (resultFileNode!=null) {
                processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
                processData.putItem("OUTPUT_FILE_NODE", resultFileNode);
            }
        }
        super.init(processData);
    }

    
    @Override
    protected String getGridServicePrefixName() {
        return "convert";
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        StringBuffer script = new StringBuffer();
        script.append("export LD_LIBRARY_PATH="+CONVERT_LIB_DIR+"\n");
        script.append(CONVERT_BASE_CMD+" $INPUT_FILENAME $OUTPUT_FILENAME\n");
        writer.write(script.toString());
    }
}
