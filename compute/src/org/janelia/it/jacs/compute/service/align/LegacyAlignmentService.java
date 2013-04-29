package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run a legacy alignment algorithm which does not conform to the new algorithm interface used by the 
 * ConfiguredAlignmentService.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class LegacyAlignmentService extends AbstractAlignmentService {

	// These are WAGs... 
	protected static final long LARGE_FILE_SIZE_THRESHOLD_UNCOMPRESSED = (long)(4.0*1024*1024*1024);
	protected static final long LARGE_FILE_SIZE_THRESHOLD_COMPRESSED = (long)(2.0*1024*1024*1024);
	
    @Override
	protected abstract void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException;

    @Override
    protected int getRequiredMemoryInGB() {

    	// For large input files we need more memory
    	File inputFile = new File(input1.getInputFilename());
    	long fileSize = inputFile.length();
    	if ((input1.getInputFilename().endsWith("raw") && fileSize>LARGE_FILE_SIZE_THRESHOLD_UNCOMPRESSED) || 
    			(input1.getInputFilename().endsWith("pbd") && fileSize>LARGE_FILE_SIZE_THRESHOLD_COMPRESSED)) {
    		logger.info("Input file size "+fileSize+" exceeds threshold. Will use 16 nodes for processing.");
    		return 96;
    	}
    	
    	return 24;
    }


    @Override
    public void postProcess() throws MissingDataException {
        
        super.postProcess();
        
        processData.putItem("ALIGNED_FILENAME", outputFile.getAbsolutePath());
        
        List<String> filenames = new ArrayList<String>();
        filenames.add(outputFile.getAbsolutePath());
        processData.putItem("ALIGNED_FILENAMES", filenames);
    }
}
