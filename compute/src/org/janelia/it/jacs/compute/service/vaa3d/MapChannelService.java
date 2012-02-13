package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.utility.ParallelFileProcessingService;

import java.io.FileWriter;

/**
 * Execute a channel mapping on an arbitrary number of files in parallel. 
 */
public class MapChannelService extends ParallelFileProcessingService {

    protected String mapChannelString="";

    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        mapChannelString=processData.getString("MAP_CHANNEL_STRING");
        if (mapChannelString==null || mapChannelString.trim().length()==0) {
            throw new Exception("MAP_CHANNEL_STRING parameter must not be empty");
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "mapchannels";
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getMapChannelCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", "\"" + mapChannelString + "\""));
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
