package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.utility.ParallelFileProcessingService;

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
    
	private Set<Integer> output8bit = new HashSet<>();

    boolean global8bitFlag=false;
	
    @Override
    protected String getGridServicePrefixName() {
        return "vaa3dconvert";
    }

    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        String global8bitString=processData.getString("OUTPUT_8BIT");
        if (global8bitString!=null && global8bitString.toLowerCase().equals("true")) {
            global8bitFlag=true;
        }
    	
    	int configIndex = 1;
    	while (true) {
    	    // This might be better, but it needs to be tested:
    	    //if (!(inputFiles.size()>configIndex || outputFiles.size()>configIndex)) break;    	    
    		String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);
    		if (outputFilename == null) break;
    	    String is8bit = (String)processData.getItem("OUTPUT_8BIT_"+configIndex);	
    		if (is8bit != null && is8bit.equals("true")) {
    			output8bit.add(configIndex);
    		}
    		configIndex++;
    	}
    }

    protected void writeInstanceFile(FileWriter fw, String inputFile, String outputFile, int configIndex) throws IOException {
        super.writeInstanceFile(fw, inputFile, outputFile, configIndex);
        if (global8bitFlag) {
            fw.write("8" + "\n");
        } else {
            fw.write((output8bit.contains(configIndex)?"8":"") + "\n");
        }
    }
    
    protected void writeShellScript(FileWriter writer) throws Exception {
    	super.writeShellScript(writer);
        writer.write("read SAVE_TO_8BIT\n");
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3dHeadlessGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedConvertScriptCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", "$SAVE_TO_8BIT") + "\n");
        script.append("\n");
        script.append(Vaa3DHelper.getHeadlessGridCommandSuffix());
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 64;
    }
}
