package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Add a verification movie to an alignment after the fact. Requires explicit knowledge of the alignment template.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddVerifyMoviePostService extends AbstractEntityGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String ALIGNER_SCRIPT_CMD = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ScriptPath");
    protected static final String BRAIN_ALIGNER_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.BrainAlignerDir");
    protected static final String CONFIG_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ConfigDir");
    protected static final String TEMPLATE_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.TemplateDir");
    protected static final String TOOLKITS_DIR = EXECUTABLE_DIR + SystemConfigurationProperties.getString("ConfiguredAligner.ToolkitsDir");
    protected static final String OUTPUT_FILE_NAME = "VerifyMovie.mp4";
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "movieConfiguration.";

    private File outputDir;
    private File outputFile;
    
    private Entity sampleEntity;
    private Entity alignment;
    private Entity defaultImage;
    private Entity supportingFiles;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        this.outputDir = new File(resultFileNode.getDirectoryPath());
    	this.outputFile = new File(outputFile, OUTPUT_FILE_NAME);
    	
        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (sampleEntityId == null || "".equals(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }
        
        logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        EntityVistationBuilder.create(entityLoader).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                .childrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT).last()
                .run(new EntityVisitor() {
            public void visit(Entity result) throws Exception {
                entityLoader.populateChildren(result);
                alignment = result;
                if (alignment!=null) {
                    logger.info("Found alignment: "+alignment.getName());
                    defaultImage = alignment.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (defaultImage!=null) {
                        logger.info("Found 3d image: "+defaultImage.getName());
                        supportingFiles = alignment.getChildByAttributeName(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);   
                        if (supportingFiles!=null) {
                            logger.info("Found supporting files: "+supportingFiles.getName());
                        }      
                    }
                }
            }
        });

        if (alignment==null) {
            throw new IllegalStateException("Sample "+sampleEntity.getId()+" has no alignment");
        }
        
        if (defaultImage==null) {
            throw new IllegalStateException("Alignment "+alignment.getId()+" has no default image");
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "movie";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        writeInstanceFiles();
        setJobIncrementStop(1);
        
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        
        String template20x = "wfb_ytx_template_dpx.v3draw";
        String template63x = "wfb_ysx_template_dpx_subsampled.v3draw";
        
        String objective = defaultImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        String chanSpec = defaultImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        String refChan = ChanSpecUtils.getReferenceChannelCSV(chanSpec);
        
        String template = objective.equals("20x") ? template20x : template63x;
        
        StringBuffer cmd = new StringBuffer();
        cmd.append("sh " + BRAIN_ALIGNER_DIR + "/createVerificationMovie.sh");
        cmd.append(" -c " + CONFIG_DIR + "/systemvars.apconf");
        cmd.append(" -k " + TOOLKITS_DIR);
        cmd.append(" -w " + resultFileNode.getDirectoryPath());
        cmd.append(" -s " + defaultImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        cmd.append(" -i " + TEMPLATE_DIR+"/"+template);
        cmd.append(" -r " + refChan);
        cmd.append(" -o " + outputFile.getAbsolutePath());

        script.append(cmd);
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 40;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

        if (!outputFile.exists()) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Missing output file: "+outputFile.getAbsolutePath());
        }

        String  alignmentDir = alignment.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        File finalFile = new File(alignmentDir, outputFile.getName());
        
        try {
            movePath(outputFile.getAbsolutePath(), finalFile.getAbsolutePath());
        }
        catch (Exception e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Error putting file into alignment location",e);
        }

        try {
            logger.info("Saving new image entity: "+finalFile.getAbsolutePath());
            Entity verifyEntity = createMovie(finalFile.getAbsolutePath(), finalFile.getName());
            entityHelper.addToParent(supportingFiles, verifyEntity, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            entityHelper.addToParent(defaultImage, verifyEntity, 0, EntityConstants.ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE);
        }
        catch (Exception e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Error creating verify movie entities",e);
        }
	}

    public Entity createMovie(String filepath, String name) throws ComputeException {
        Entity entity = new Entity();
        entity.setOwnerKey(ownerKey);
        entity.setEntityTypeName(EntityConstants.TYPE_MOVIE);
        Date createDate = new Date();
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setName(name);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, filepath);
        return entityBean.saveOrUpdateEntity(entity);
    }
    
    private void movePath(String sourcePath, String targetPath) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("cp "+sourcePath+" "+targetPath+"; ");
        logger.info("Running: "+script);
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, 600);
        if (0!=exitCode) {
            throw new ServiceException("Move failed with exitCode "+exitCode);
        }
    }
}
