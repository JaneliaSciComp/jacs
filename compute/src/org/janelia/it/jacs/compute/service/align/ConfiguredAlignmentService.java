package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

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
    protected String tissueOrientation;
    protected String genderCode;

    @Override
    public void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            this.scriptFile = data.getRequiredItemAsString("ALIGNMENT_SCRIPT_NAME");

            entityLoader.populateChildren(sampleEntity);
            Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
            if (supportingData!=null) {
                this.mountingProtocol = sampleHelper.getConsensusLsmAttributeValue(alignedAreas, EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL);
                this.tissueOrientation = sampleHelper.getConsensusLsmAttributeValue(alignedAreas, EntityConstants.ATTRIBUTE_TISSUE_ORIENTATION);
                this.genderCode = sanitizeGender(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, EntityConstants.ATTRIBUTE_GENDER));
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

	@Override
    protected void createShellScript(FileWriter writer) throws IOException, ParameterException, MissingDataException,
            InterruptedException, ServiceException {

        contextLogger.info("Running configured aligner " + ALIGNER_SCRIPT_CMD + " (" + " resultNodeId=" +
                           resultFileNode.getObjectId() + " outputDir=" + resultFileNode.getDirectoryPath() + ")");

        final String script = Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n" +
                              Vaa3DHelper.getVaa3dLibrarySetupCmd() + "\n" +
                              "cd " + resultFileNode.getDirectoryPath() + "\n" +
                              getAlignerCommand() + "\n" +
                              Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n";
        writer.write(script);
    }

    protected String getAlignerCommand() throws ServiceException {

        StringBuilder cmd = new StringBuilder();
        cmd.append("sh ").append(ALIGNER_SCRIPT_CMD);
        cmd.append(" ").append(BRAIN_ALIGNER_DIR).append("/").append(scriptFile);
        cmd.append(" ").append(getGridResourceSpec().getSlots());
        cmd.append(" -o ").append(resultFileNode.getDirectoryPath());
        cmd.append(" -c ").append(CONFIG_DIR).append("/systemvars.apconf");
        cmd.append(" -t ").append(TEMPLATE_DIR);
        cmd.append(" -k ").append(TOOLKITS_DIR);
        if (mountingProtocol!=null) {
            cmd.append(" -m '\"").append(mountingProtocol).append("\"'");
        }
        if ("face_down".equals(tissueOrientation)) {
            cmd.append(" -z zflip");
        }
        if (genderCode!=null) {
            cmd.append(" -g ").append(genderCode);
        }
        if (input1!=null) {
        	if (StringUtils.isEmpty(input1.getPixelResolution())) {
        		throw new ServiceException("Primary input has no pixel resolution");
        	}
        	else if (StringUtils.isEmpty(input1.getOpticalResolution())) {
        		throw new ServiceException("Primary input has no optical resolution");
        	}
            cmd.append(" -i ").append(getInputParameter(input1));
            if (input1.getInputSeparationFilename()!=null) {
                cmd.append(" -e ").append(input1.getInputSeparationFilename());
            }
        }
        else {
        	throw new ServiceException("No primary input for alignment");
        }
        if (input2!=null) {
        	if (StringUtils.isEmpty(input2.getPixelResolution())) {
        		throw new ServiceException("Secondary input has no pixel resolution");
        	}
        	else if (StringUtils.isEmpty(input2.getOpticalResolution())) {
        		throw new ServiceException("Secondary input has no optical resolution");
        	}
            cmd.append(" -j ").append(getInputParameter(input2));
            if (input2.getInputSeparationFilename()!=null) {
                cmd.append(" -f ").append(input2.getInputSeparationFilename());
            }
        }
        return cmd.toString();
    }

    protected String getInputParameter(AlignmentInputFile input) {
        return StringUtils.emptyIfNull(input.getFilepath()) + "," +
               StringUtils.emptyIfNull(input.getNumChannels()) + "," +
               StringUtils.emptyIfNull(input.getRefChannelOneIndexed()) + "," +
               StringUtils.emptyIfNull(input.getOpticalResolution()) + "," +
               StringUtils.emptyIfNull(input.getPixelResolution());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	if (Objective.OBJECTIVE_20X.getName().equals(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE))) {
    		// If this is only a 20x alignment, we can probably get away with half a node.
    		return 64;
    	}
    	// For most alignments we just take the full node because we don't know the exact memory requirements. 
        return 128;
    }

    @Override
    public void postProcess() throws MissingDataException {

        final File outputDir = new File(resultFileNode.getDirectoryPath());

        if (! outputDir.exists()) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Output directory missing for alignment: " + outputDir);
        }

        try {
            final Collection<File> propertiesFiles = getPropertiesFiles(outputDir);

            if (propertiesFiles.size() == 0) {
                throw new MissingGridResultException(outputDir.getAbsolutePath(), "alignment output directory " + outputDir.getAbsolutePath() +
                        " does not contain any '.properties' files");
            } else {
                contextLogger.info("postProcess: '.properties' file(s) are " + propertiesFiles);
            }

            List<ImageStack> outputFiles = new ArrayList<>();
            String defaultFilename = null;
            File propertyFileDir;

            for (File propertiesFile : propertiesFiles) {
            	
                Properties properties = new Properties();
                properties.load(new FileReader(propertiesFile));

                propertyFileDir = propertiesFile.getParentFile();
                String stackFilename = properties.getProperty("alignment.stack.filename");
                File file = new File(propertyFileDir, stackFilename);
                if (!file.exists()) {
                    throw new MissingGridResultException(outputDir.getAbsolutePath(), "Alignment stack file does not exist: " + file.getAbsolutePath());
                }

                String canonicalPath = file.getCanonicalPath();
                if ("true".equals(properties.getProperty("default"))) {
                    if (defaultFilename == null) {
                        defaultFilename = canonicalPath;
                    } 
                    else {
                        contextLogger.warn("default flag is true for both " + defaultFilename +
                                           " and " + canonicalPath + " (ignoring flag for second file)");
                    }
                }
                
                String channels = properties.getProperty("alignment.image.channels");
                if (channels==null) {
                	logger.warn("Alignment output does not contain 'alignment.image.channels' property, cannot continue processing.");
                	continue;
                }
                
                String refchan = properties.getProperty("alignment.image.refchan");
                if (refchan==null) {
                	logger.warn("Alignment output does not contain 'alignment.image.refchan' property, cannot continue processing.");
                	continue;
                }

                String channelSpec = null;
            	int numChannels = Integer.parseInt(channels);
            	int refChannel = Integer.parseInt(refchan);
            	channelSpec = ChanSpecUtils.createChanSpec(numChannels, refChannel);
                
                ImageStack outputFile = new ImageStack();
                outputFile.setFilepath(canonicalPath);
                outputFile.setChannelSpec(channelSpec);
            	
                outputFiles.add(outputFile);
            }

            if (outputFiles.isEmpty()) {
                throw new MissingGridResultException(outputDir.getAbsolutePath(), "No outputs defined for alignment: " + outputDir);
            }

            data.putItem("ALIGNED_IMAGES", outputFiles);

            if (defaultFilename == null) {
                contextLogger.warn("No default output defined for alignment: " + outputDir);
                defaultFilename = outputFiles.get(0).getFilepath();
            }

            data.putItem("ALIGNED_FILENAME", defaultFilename);

        }
        catch (IOException e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Error getting alignment outputs: " + outputDir, e);
        }
    }

    /**
     * Finds all .properties files within the specified directory and all of its sub-directories.
     * Separated here as a protected method to support unit testing.
     *
     * @param  outputDir  root directory to start search.
     *
     * @return list of .properties files.
     */
    protected Collection<File> getPropertiesFiles(File outputDir) {

        // Most aligners simply place the .properties files in the root result directory but
        // the brainalignPolarityPair_ds_dpx_1024px_INT_v2.sh aligner places properties files
        // in the following sub-directories:
        //
        //   Neurons/20x/NeuronAligned20xScale.properties
        //   Neurons/63x/NeuronAligned63xScale.properties
        //   Brains/20x/Aligned20xScale.properties
        //   Brains/63x/Aligned63xScale.properties

        final String[] extensions = { "properties" };
        return FileUtils.listFiles(outputDir, extensions, true);
    }

    private String sanitizeGender(String gender) {
		if (gender==null) return null;
		String genderLc = gender.toLowerCase();
		if (genderLc.startsWith("f")) {
			return "f";
		}
		else if (genderLc.startsWith("m")) {
			return "m";
		}
		else if (genderLc.startsWith("x")) {
			return "x";
		}
		else {
			logger.warn("Invalid value for sample gender: "+gender);
			return null;
		}
	}
}
