package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.utility.ParallelFileProcessingService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Compress any number of 3d volumes in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DConversionService extends ParallelFileProcessingService {
    
	private Set<Integer> output8bit = new HashSet<Integer>();
	
    @Override
    protected String getGridServicePrefixName() {
        return "vaa3dconvert";
    }

    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
    	int configIndex = 1;
    	while (true) {
    		String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);
    		if (outputFilename == null) break;
    	    String is8bit = (String)processData.getItem("OUTPUT_8BIT_"+configIndex);	
    		if (is8bit != null && is8bit.equals("true")) {
    			output8bit.add(configIndex);
    		}
    		configIndex++;
    	}
    }

    protected void writeInstanceFile(FileWriter fw, File inputFile, File outputFile, int configIndex) throws IOException {
        super.writeInstanceFile(fw, inputFile, outputFile, configIndex);
        fw.write((output8bit.contains(configIndex)?"8":"") + "\n");
    }
    
    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        writer.write("read SAVE_TO_8BIT\n");
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedConvertCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", "$SAVE_TO_8BIT"));
        script.append("\n");
        script.append(Vaa3DHelper.getHeadlessGridCommandSuffix());
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
