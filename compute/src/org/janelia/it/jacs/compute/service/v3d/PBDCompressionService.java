package org.janelia.it.jacs.compute.service.v3d;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Compress any number of 3d volumes to PBD format, in parallel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PBDCompressionService extends SubmitDrmaaJobService {

	private static final int DISPLAY_PORT = 9469;
    private static final String CONFIG_PREFIX = "pbdConfiguration.";

    private List<File> inputFiles = new ArrayList<File>();
    private List<File> outputFiles = new ArrayList<File>();
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
        FileNode outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	throw new ServiceException("Input parameter OUTPUT_FILE_NODE may not be null");
        }
        
    	int configIndex = 1;
    	while (true) {
    		String inputFilename = (String)processData.getItem("INPUT_FILENAME_"+configIndex);	
    		if (inputFilename == null || configIndex>100) break;
    		String outputFilename = (String)processData.getItem("OUTPUT_FILENAME_"+configIndex);	

        	File inputFile = new File(outputFileNode.getDirectoryPath(), inputFilename);
        	File outputFile = new File(outputFileNode.getDirectoryPath(), outputFilename);
        	inputFiles.add(inputFile);
        	outputFiles.add(outputFile);
    		
    		configIndex++;
    	}
    }

    @Override
    protected String getGridServicePrefixName() {
        return "pbd";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

    	int i = 0;
    	int configIndex = 1;
    	for(File inputFile : inputFiles) {
    		File outputFile = outputFiles.get(i++);
    		writeInstanceFiles(inputFile, outputFile, configIndex++);
    	}
    	createShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(File inputFile, File outputFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(inputFile.getAbsolutePath() + "\n");
            fw.write(outputFile.getAbsolutePath() + "\n");
            fw.write((DISPLAY_PORT+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILENAME\n");
        script.append("read OUTPUT_FILENAME\n");
        script.append("read DISPLAY_PORT\n");
        script.append(V3DHelper.getV3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(V3DHelper.getFormattedPBDCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME"));
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

    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Stitch/blend core dumped for "+resultFileNode.getDirectoryPath());
    	}

    	for(File outputFile : outputFiles) {
        	if (!outputFile.exists()) {
        		throw new MissingDataException("MIP output file not found: "+outputFile.getAbsolutePath());
        	}
    	}
	}
}
