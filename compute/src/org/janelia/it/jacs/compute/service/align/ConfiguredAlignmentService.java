package org.janelia.it.jacs.compute.service.align;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Run a configured brain aligner script. Parameters:
 *   ALIGNMENT_SCRIPT_NAME - the configured alignment script to run
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredAlignmentService extends AbstractAlignmentService {

	protected static final String ALIGNER_SCRIPT_CMD = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ScriptPath");
	protected static final String BRAIN_ALIGNER_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.BrainAlignerDir");
	protected static final String CONFIG_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ConfigDir");
	protected static final String TEMPLATE_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.TemplateDir");
	protected static final String TOOLKITS_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ToolkitsDir");

	protected String scriptFile;
	protected String mountingProtocol;
	
    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            this.scriptFile = (String)processData.getItem("ALIGNMENT_SCRIPT_NAME");
            if (scriptFile==null) {
                throw new ServiceException("Input parameter ALIGNMENT_SCRIPT_NAME may not be null");
            }
    
            entityLoader.populateChildren(sampleEntity);
            Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
            if (supportingData!=null) {
                this.mountingProtocol = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL, alignedArea);
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
	protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
			InterruptedException, ServiceException {

		logger.info("Running configured aligner "+ALIGNER_SCRIPT_CMD+" ("
				+ " resultNodeId=" + resultFileNode.getObjectId() 
				+ " outputDir=" + resultFileNode.getDirectoryPath()
				+")");
		
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd() + "\n");
        script.append("cd " + resultFileNode.getDirectoryPath() + "\n");
		script.append(getAlignerCommand());
		script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
	}
    
    protected String getAlignerCommand() {

        StringBuffer cmd = new StringBuffer();
        cmd.append("sh "+ALIGNER_SCRIPT_CMD);
        cmd.append(" " + BRAIN_ALIGNER_DIR + "/" + scriptFile);
        cmd.append(" -o " + resultFileNode.getDirectoryPath());
        cmd.append(" -c " + CONFIG_DIR + "/systemvars.apconf");
        cmd.append(" -t " + TEMPLATE_DIR);
        cmd.append(" -k " + TOOLKITS_DIR);
        if (mountingProtocol!=null) {
            cmd.append(" -m '\"" + mountingProtocol+"\"'");
        }
        if (input1!=null) {
            cmd.append(" -i " + getInputParameter(input1));
            cmd.append(" -e "+input1.getInputSeparationFilename());
        }
        if (input2!=null) {
            cmd.append(" -j " + getInputParameter(input2));
            cmd.append(" -f "+input2.getInputSeparationFilename());
        }
        return cmd.toString();
    }
    
    protected String getInputParameter(AlignmentInputFile input) {
        StringBuilder sb = new StringBuilder();
        sb.append(input.getInputFilename());
        sb.append(",");
        sb.append(input.getNumChannels());
        sb.append(",");
        sb.append(input.getRefChannelOneIndexed());
        sb.append(",");
        sb.append(input.getOpticalResolution());
        sb.append(",");
        sb.append(input.getPixelResolution());
        return sb.toString();
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 128;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
        File outputDir = new File(resultFileNode.getDirectoryPath());
    	try {
        	File[] propertiesFiles = outputDir.listFiles(new FilenameFilter() {
    			@Override
    			public boolean accept(File dir, String name) {
    				return name.endsWith(".properties");
    			}
    		});
    		
        	List<String> filenames = new ArrayList<String>();
        	String defaultFilename = null;
        	
        	for(File propertiesFile : propertiesFiles) {
        	    Properties properties = new Properties();
        	    properties.load(new FileReader(propertiesFile));
        		
        	    String filename = properties.getProperty("alignment.stack.filename");
        	    File file = new File(outputDir, filename);
        	    if (!file.exists()) {
        	        throw new MissingDataException("File does not exist: "+file);
        	    }
        	    
        	    filenames.add(file.getCanonicalPath());
        	    if ("true".equals(properties.getProperty("default"))) {
        	        defaultFilename = file.getCanonicalPath();
        	    }
        	}
        	
        	if (filenames.isEmpty()) {
        	    throw new MissingDataException("No outputs defined for alignment: "+outputDir);
        	}
        	
            logger.info("Putting '"+filenames+"' in ALIGNED_FILENAMES");
            processData.putItem("ALIGNED_FILENAMES", filenames);
            
            if (defaultFilename==null) {
                logger.warn("No default output defined for alignment: "+outputDir);
                defaultFilename = filenames.get(0);
            }
            
            logger.info("Putting '"+defaultFilename+"' in ALIGNED_FILENAME");
            processData.putItem("ALIGNED_FILENAME", defaultFilename);

    	}
    	catch (IOException e) {
    		throw new MissingDataException("Error getting alignment outputs: "+outputDir,e);
    	}
    }
}
