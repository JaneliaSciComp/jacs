package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Intersect any number of paired images. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   FILE_PAIRS - a list of CombinedFile
 *   INTERSECTION_METHOD - integer indicating the intersection method to use:
 *      0: minimum value 
 *      1: geometric mean 
 *      2: scaled product
 *   KERNEL_SIZE - integer indicating the size of the gaussian kernel used for blurring the first stack
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DBulkIntersectionService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "intersectConfiguration.";
    
    private int randomPort;
    private int intersectionMethod;
    private int kernelSize;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        Object intersectionMethodStr = processData.getItem("INTERSECTION_METHOD");
        if (intersectionMethodStr==null) {
        	throw new ServiceException("Input parameter INTERSECTION_METHOD may not be null");
        }
        intersectionMethod = Integer.parseInt(intersectionMethodStr.toString());

        Object kernelSizeStr = processData.getItem("KERNEL_SIZE");
        if (kernelSizeStr==null) {
        	throw new ServiceException("Input parameter KERNEL_SIZE may not be null");
        }
        kernelSize = Integer.parseInt(kernelSizeStr.toString());
    }

    @Override
    protected String getGridServicePrefixName() {
        return "intersect";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        
        Object filePairsObj = processData.getItem("FILE_PAIRS");
        if (filePairsObj==null) {
        	throw new ServiceException("Input parameter FILE_PAIRS may not be null");
        }

        if (filePairsObj instanceof List) {
        	List<CombinedFile> combinedFiles = (List<CombinedFile>)filePairsObj;

            int configIndex = 1;
            randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
            for(CombinedFile combinedFile : combinedFiles) {
            	writeInstanceFiles(combinedFile, configIndex++);
            }
            
        	createShellScript(writer);
            setJobIncrementStop(configIndex-1);
        }
        else {
        	throw new ServiceException("Input parameter FILE_PAIRS must be an ArrayList<CombinedFile>");
        }
    }

    private void writeInstanceFiles(CombinedFile combinedFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(combinedFile.getFilepath1() + "\n");
            fw.write(combinedFile.getFilepath2() + "\n");
            fw.write(combinedFile.getOutputFilepath() + "\n");
            fw.write(intersectionMethod + "\n");
            fw.write(kernelSize + "\n");
            fw.write((randomPort+configIndex) + "\n");
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
        script.append("read FILENAME_1\n");
        script.append("read FILENAME_2\n");
        script.append("read OUTPUT_FILENAME\n");
        script.append("read METHOD\n");
        script.append("read KERNEL_SIZE\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedIntersectionCommand("$FILENAME_1", "$FILENAME_2", "$OUTPUT_FILENAME", "$METHOD", "$KERNEL_SIZE"));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 24;
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Bulk intersection core dumped");
    	}

        Object filePairsObj = processData.getItem("FILE_PAIRS");
    	List<CombinedFile> combinedFiles = (List<CombinedFile>)filePairsObj;
        for(CombinedFile combinedFile : combinedFiles) {
        	File outputFile = new File(combinedFile.getOutputFilepath());
        	if (!outputFile.exists()) {
        		throw new MissingGridResultException(file.getAbsolutePath(), "Missing intersection output "+outputFile.getAbsolutePath());
        	}
        }
	}
    
}
