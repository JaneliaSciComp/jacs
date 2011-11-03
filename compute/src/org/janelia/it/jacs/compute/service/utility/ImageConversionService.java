package org.janelia.it.jacs.compute.service.utility;

import java.io.FileWriter;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Convert any number of images, in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageConversionService extends ParallelFileProcessingService {

    protected static final String CONVERT_BASE_CMD = 
    		SystemConfigurationProperties.getString("ImageMagick.Bin.Path")+"/convert";
    
    @Override
    protected String getGridServicePrefixName() {
        return "convert";
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        StringBuffer script = new StringBuffer();
        script.append(CONVERT_BASE_CMD+" $INPUT_FILENAME $OUTPUT_FILENAME");
        script.append("\n");
        writer.write(script.toString());
    }
}
