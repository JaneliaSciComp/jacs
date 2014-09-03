package org.janelia.it.jacs.compute.service.align;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
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

    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            this.scriptFile = data.getRequiredItemAsString("ALIGNMENT_SCRIPT_NAME");

            entityLoader.populateChildren(sampleEntity);
            Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
            if (supportingData!=null) {
                this.mountingProtocol = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL, alignedArea);
                this.tissueOrientation = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_TISSUE_ORIENTATION, alignedArea);
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
            cmd.append(" -j ").append(getInputParameter(input2));
            if (input2.getInputSeparationFilename()!=null) {
                cmd.append(" -f ").append(input2.getInputSeparationFilename());
            }
        }
        return cmd.toString();
    }

    protected String getInputParameter(AlignmentInputFile input) {
        return StringUtils.emptyIfNull(input.getInputFilename()) + "," +
               StringUtils.emptyIfNull(input.getNumChannels()) + "," +
               StringUtils.emptyIfNull(input.getRefChannelOneIndexed()) + "," +
               StringUtils.emptyIfNull(input.getOpticalResolution()) + "," +
               StringUtils.emptyIfNull(input.getPixelResolution());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 128;
    }

    @Override
    public void postProcess() throws MissingDataException {

        final File outputDir = new File(resultFileNode.getDirectoryPath());

        if (! outputDir.exists()) {
            throw new MissingDataException("Output directory missing for alignment: " + outputDir);
        }

        try {
            final Collection<File> propertiesFiles = getPropertiesFiles(outputDir);

            if (propertiesFiles.size() == 0) {
                throw new MissingDataException("alignment output directory " + outputDir.getAbsolutePath() +
                        " does not contain any '.properties' files");
            } else {
                contextLogger.info("postProcess: '.properties' file(s) are " + propertiesFiles);
            }

            List<String> filenames = new ArrayList<>();
            String defaultFilename = null;
            String canonicalPath;
            File propertyFileDir;

            for (File propertiesFile : propertiesFiles) {
                Properties properties = new Properties();
                properties.load(new FileReader(propertiesFile));

                propertyFileDir = propertiesFile.getParentFile();
                String filename = properties.getProperty("alignment.stack.filename");
                File file = new File(propertyFileDir, filename);
                if (!file.exists()) {
                    throw new MissingDataException("Alignment stack file does not exist: " + file.getAbsolutePath());
                }

                canonicalPath = file.getCanonicalPath();
                filenames.add(canonicalPath);
                if ("true".equals(properties.getProperty("default"))) {
                    if (defaultFilename == null) {
                        defaultFilename = canonicalPath;
                    } else {
                        contextLogger.warn("default flag is true for both " + defaultFilename +
                                           " and " + canonicalPath + " (ignoring flag for second file)");
                    }
                }
            }

            if (filenames.isEmpty()) {
                throw new MissingDataException("No outputs defined for alignment: " + outputDir);
            }

            data.putItem("ALIGNED_FILENAMES", filenames);

            if (defaultFilename == null) {
                contextLogger.warn("No default output defined for alignment: " + outputDir);
                defaultFilename = filenames.get(0);
            }

            data.putItem("ALIGNED_FILENAME", defaultFilename);

        }
        catch (IOException e) {
            throw new MissingDataException("Error getting alignment outputs: " + outputDir, e);
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

}
