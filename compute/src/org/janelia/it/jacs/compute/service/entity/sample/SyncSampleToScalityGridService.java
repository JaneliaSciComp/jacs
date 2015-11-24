package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.compute.util.JFSUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Moves the given Sample's files to the Scality object store via JFS and updates the object model 
 * to add a JFS Path attribute to each file entity.
 * 
 * This service can currently move all PBDs in the sample, or all LSMs. In the case of LSMs, it only moves
 * LSMS that were processed more than 30 days ago. It also compresses the LSMs, and updates SAGE to point 
 * to JFS instead of the file system.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncSampleToScalityGridService extends AbstractEntityGridService {

    private static final String CONFIG_PREFIX = "scalityConfiguration.";

    protected static final boolean JFS_ALLOW_WRITES =
            SystemConfigurationProperties.getBoolean("JFS.AllowWrites");

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");

    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    private static final String JFS_CMD = 
            SystemConfigurationProperties.getString("JFS.CommandLineUtil");
        
    private Entity sampleEntity;
    private Set<Long> seenEntityIds = new HashSet<>();
    private List<Entity> entitiesToMove = new ArrayList<>();
    private boolean deleteSourceFiles = false;
    private Set<Pattern> exclusions = new HashSet<>();

    @Override
    protected String getGridServicePrefixName() {
        return "scality";
    }

    @Override
    protected void init() throws Exception {

        if (!JFS_ALLOW_WRITES) {
            logger.info("JFS writes are disallowed by configuration. JFS.AllowWrites is set to false in jacs.properties.");
            cancel();
            return;
        }
        
        String excludeFiles = data.getItemAsString("EXCLUDE_FILES");
        if (!StringUtils.isEmpty(excludeFiles)) {
            for(String filePattern : excludeFiles.split("\\s*,\\s*")) {
            	Pattern p = Pattern.compile(filePattern.replaceAll("\\*", "(.*?)"));
            	exclusions.add(p);
            }
        }
        
        this.deleteSourceFiles = data.getItemAsBoolean("DELETE_SOURCE_FILES");
        
        Long sampleEntityId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
        sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }
        
        logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        String fileTypesStr = data.getRequiredItemAsString("FILE_TYPES");
        List<String> fileTypes = Task.listOfStringsFromCsvString(fileTypesStr);
        
        processSample(sampleEntity, fileTypes);

        if (entitiesToMove.isEmpty()) {
            logger.info("No entities to process, aborting.");
            cancel();
            return;
        }
    }
	
    private void processSample(Entity sample, List<String> fileTypes) throws Exception {

        Set<String> types = new HashSet<>(fileTypes);
        
        entityLoader.populateChildren(sample);
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample");
        if (!childSamples.isEmpty()) {
            for(Entity childSample : childSamples) {
                processSample(childSample, fileTypes);
            }
            return;
        }

        if (types.remove("lsm")) {
            logger.info("Searching "+sample.getId()+" for LSM files to move...");
            final DateTime cutoffDate = new DateTime().minus(Period.months(1));
            EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                    .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                    .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                    .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                    .run(new EntityVisitor() {
                public void visit(Entity entity) throws Exception {
                    String completionDateStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE);
                    if (null!=completionDateStr) {
                        DateTime completionDate = new DateTime(ISO8601Utils.parse(completionDateStr));
                        if (cutoffDate.isAfter(completionDate)) {
                            addToEntitiesToMove(entity);
                        }
                        else {
                        	logger.info("LSM is too recent to move: "+entity.getId());
                        }
                    }
                    else {
                    	logger.info("LSM has not been completed, so it can't be moved: "+entity.getId());
                    }
                }
            });
        }
        
        if (types.remove("pbd")) {
            logger.info("Searching "+sample.getId()+" for PBD files to move...");
            EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                    .childrenOfAttr(EntityConstants.ATTRIBUTE_RESULT)
                    .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                    .childrenOfType(EntityConstants.TYPE_IMAGE_3D)
                    .descendants()
                    .run(new EntityVisitor() {
                public void visit(Entity entity) throws Exception {
                    String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (filepath==null) {
                        return;
                    }
                    if (!filepath.endsWith(".v3dpbd")) {
                        return;
                    }
                    if (!filepath.startsWith(JACS_DATA_DIR) && !filepath.startsWith(JACS_DATA_ARCHIVE_DIR)) {
                        logger.warn("Entity has path outside of filestore: "+entity.getId());
                        return;
                    }
                    addToEntitiesToMove(entity);
                }
            });   
        }
        
        if (!types.isEmpty()) {
            throw new IllegalArgumentException("Illegal file types: "+types);
        }
    }

    private void addToEntitiesToMove(Entity entity) {

    	if (seenEntityIds.contains(entity.getId())) {
    		return;
    	}
    	seenEntityIds.add(entity.getId());
    	
        String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        
        if (filepath==null) {
            logger.warn("Entity should be moved but has no filepath: "+entity.getId());
            return;
        }

        File file = new File(filepath);
        
        if (isExcluded(file.getName())) {
            logger.debug("Excluding file: "+entity.getId());
            return;
        }
        
        if (!file.exists()) {
            logger.warn("Entity has filepath which does not exist: "+entity.getId());
            return;
        }

        logger.info("Will synchronized file for entity: "+entity.getId());
        entitiesToMove.add(entity);
    }
    
	private boolean isExcluded(String filename) {		
		for(Pattern p : exclusions) {
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				return true;
			}
		}
		return false;
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;
        for(Entity entity : entitiesToMove) {
            writeInstanceFile(entity, configIndex++);
        }
        setJobIncrementStop(configIndex-1);
        createShellScript(writer);
    }
    
    private void writeInstanceFile(Entity entity, int configIndex) throws Exception {
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		String jfsPath = JFSUtils.getScalityPathFromEntity(entity);
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(filepath + "\n");
        	fw.write(jfsPath + "\n");
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
        script.append("read FILE_PATH\n");
        script.append("read JFS_PATH\n");
        script.append("WORKING_DIR=").append(resultFileNode.getDirectoryPath()).append("\n");
        script.append("cd $WORKING_DIR\n");
        script.append(Vaa3DHelper.getHostnameEcho());
        script.append("FILE_STUB=`basename $FILE_PATH`\n");
        script.append("FILE_EXT=${FILE_STUB##*.}\n");
        script.append("if [[ \"$FILE_EXT\" = \"lsm\" ]]; then\n");
        script.append("  WORKING_FILE=$WORKING_DIR/$FILE_STUB.bz2\n");
        script.append("  "+Vaa3DHelper.getFormattedConvertScriptCommand("$FILE_PATH","$WORKING_FILE", "")).append("\n");
        script.append("  FILE_PATH=${WORKING_FILE}\n");
        script.append("  JFS_PATH=${JFS_PATH}.bz2\n");
        script.append("fi\n");
        script.append("echo \"Copy source: $FILE_PATH\"\n");
        script.append("echo \"Copy target: $JFS_PATH\"\n");
        script.append("CMD=\""+JFS_CMD + " -command write -path $JFS_PATH -file $FILE_PATH -checksum\"\n");
        script.append("echo \"Running: $CMD\"\n");
        script.append("$CMD\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredSlots() {
        return 16;
    }

    @Override
    protected String getAdditionalNativeSpecification() {
        return "-l scalityw=1";
    }

    @Override
    protected boolean isShortPipelineJob() {
        return true;
    }

    @Override
	public void postProcess() throws MissingDataException {

    	SageDAO sage = new SageDAO(logger);
        CvTerm propertyFilesize = getCvTermByName(sage, "light_imagery", "file_size");
        
        logger.debug("Processing "+resultFileNode.getDirectoryPath());

        File outputDir = new File(resultFileNode.getDirectoryPath(), "sge_output");
    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, getGridServicePrefixName());
    	
    	if (outputFiles.length != entitiesToMove.size()) {
    		throw new MissingDataException("Number of entities to move ("+entitiesToMove.size()+") does not match number of output files ("+outputFiles.length+")");
    	}
    	
    	// Find errors
    	
    	boolean[] hasError = new boolean[entitiesToMove.size()];
        File errorDir = new File(resultFileNode.getDirectoryPath(), "sge_error");
        File[] errorFiles = FileUtil.getFilesWithPrefixes(errorDir, getGridServicePrefixName());
        for (File errorFile : errorFiles) {
            int index = getIndexExtension(errorFile);
            if (errorFile.length() > 0) {
            	logger.error("Errors encountered when synchronizing " + errorFile.getAbsolutePath()+":");
            	try {
            		String stderr = org.apache.commons.io.FileUtils.readFileToString(errorFile);
            		logger.error(stderr);
            	}
            	catch (IOException e) {
            		logger.error("Error reading STDERR file",e);
            	}
                hasError[index-1] = true;
            }
        }
        
        // Update all entities that were transferred correctly
        
    	int i=0;
    	for(Entity entity : entitiesToMove) {
    		if (hasError[i++]) {
    			logger.warn("Error synchronizing entity "+entity.getName()+" (id="+entity.getId()+")");
    		}
    		else {
                String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                String jfsPath = JFSUtils.getScalityPathFromEntity(entity);
                String webdavUrl = JFSUtils.getWebdavUrlForJFSPath(jfsPath);
                File file = new File(filepath);
                Long bytes = null;
                
                logger.info("Synchronized "+entity.getId()+" to "+jfsPath);
                
                try {
                    if (filepath.endsWith(".lsm")) {
                    	
                    	// The LSM was compressed by this pipeline, so we need to add the correct extension everywhere
	                    jfsPath += ".bz2";
	                    webdavUrl += ".bz2";
	                    bytes = file.length();
	                    
	                    // Update the entity name too
	        			entity.setName(entity.getName()+".bz2");
	                    entityBean.saveOrUpdateEntity(entity);
                    }

        			entityBean.setOrUpdateValue(entity.getId(), EntityConstants.ATTRIBUTE_JFS_PATH, jfsPath);
                    
    			    entityHelper.removeEntityDataForAttributeName(entity, EntityConstants.ATTRIBUTE_FILE_PATH);
    			    int numUpdated = entityBean.bulkUpdateEntityDataValue(filepath, jfsPath);
                    if (numUpdated>0) {
                    	logger.info("Updated "+numUpdated+" entity data values to "+jfsPath);
                    }

        			if (deleteSourceFiles) {
        				try {
        					FileUtils.forceDelete(file);
	        			    logger.info("Deleted "+filepath);
        				}
        				catch (IOException e) {
        					// Log "unable to delete file" message so that they can be parsed later and the files deleted
	                        logger.info(e.getMessage());
        				}
                    }
                }
                catch (Exception e) {
                	logger.error("Error updating entity id="+entity.getId(),e);
                    throw new MissingDataException("Could not update entities, database may be in an inconsistent state!");
                }

    			// Update SAGE if necessary
                String sageIdStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
    			if (sageIdStr!=null) {
	                try {
        				if (!jfsPath.endsWith(".lsm.bz2")) {
        					logger.warn("Expected lsm.bz2 extension on LSM: "+jfsPath);
        				}
        				
        		        Image image = sage.getImage(new Integer(sageIdStr));
        		        image.setPath(null);
	        		    image.setJfsPath(jfsPath);
        		        image.setUrl(webdavUrl);
        		        sage.saveImage(image);
        		        logger.info("Updated SAGE image "+image.getId());
        		        if (bytes!=null) {
        		        	sage.setImageProperty(image, propertyFilesize, bytes.toString());
        		        	logger.info("Updated bytes to "+bytes+" for image "+image.getId());
        		        }
	                }
	                catch (Exception e) {
	                	logger.error("Error updating SAGE image "+sageIdStr,e);
	                    throw new MissingDataException("Could not update SAGE, it may be in an inconsistent state!");
	                }
    			}
    		}	
    	}
	}

    private int getIndexExtension(File file) {
        String name = file.getName();
        String ext = name.substring(name.lastIndexOf('.')+1);
        return Integer.parseInt(ext);
    }
    
    private CvTerm getCvTermByName(SageDAO sage, String cvName, String termName) {
    	try {
	        CvTerm term = sage.getCvTermByName(cvName, termName);
	        if (term==null) {
	            throw new IllegalStateException("No such term: "+termName+" in CV "+cvName);
	        }
	        return term;
    	}
    	catch (Exception e) {
            throw new IllegalStateException("Error getting term: "+termName+" in CV "+cvName,e);
    	}
    }
}
