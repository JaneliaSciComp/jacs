package org.janelia.it.jacs.compute.service.v3d;

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
 * Generate MIPs for any number of 3d volumes in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MIPGenerationService extends ParallelFileProcessingService {

	private Set<Integer> outputFlipy = new HashSet<Integer>();
	
    @Override
    protected String getGridServicePrefixName() {
        return "mip";
    }
    
    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
    	int configIndex = 1;
    	while (true) {
    		String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);
    		if (outputFilename == null) break;
    	    String isFlipy = (String)processData.getItem("OUTPUT_FLIPY_"+configIndex);	
    		if (isFlipy != null && isFlipy.equals("true")) {
    			outputFlipy.add(configIndex);
    		}
    		configIndex++;
    	}
    }

    @Override
    protected void writeInstanceFile(FileWriter fw, File inputFile, File outputFile, int configIndex) throws IOException {
        super.writeInstanceFile(fw, inputFile, outputFile, configIndex);
        fw.write((outputFlipy.contains(configIndex)?"-flipy":"") + "\n");
    }

    @Override
    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        StringBuffer script = new StringBuffer();
        script.append("read EXTRA_OPTIONS\n");
        script.append(V3DHelper.getHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(V3DHelper.getFormattedMIPCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", "$EXTRA_OPTIONS"));
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
