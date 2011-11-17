package org.janelia.it.jacs.compute.service.v3d;

import java.io.FileWriter;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.utility.ParallelFileProcessingService;

/**
 * Generate MIPs for any number of 3d volumes in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MIPGenerationService extends ParallelFileProcessingService {

    @Override
    protected String getGridServicePrefixName() {
        return "mip";
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        StringBuffer script = new StringBuffer();
        script.append(V3DHelper.getHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(V3DHelper.getFormattedMIPCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", true));
        script.append("\n");
        script.append(V3DHelper.getV3DGridCommandSuffix());
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
