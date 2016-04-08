package org.janelia.it.jacs.compute.service.domain.sample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainGridService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.compute.util.JFSUtils;
import org.janelia.it.jacs.compute.util.ScalityEntity;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;
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
public class SyncSampleToScalityGridService extends AbstractDomainGridService {

    private static final String COMPLETION_TOKEN = "Synchronization script ran to completion";
    private static final String CONFIG_PREFIX = "scalityConfiguration.";

    protected static final boolean JFS_ALLOW_WRITES =
            SystemConfigurationProperties.getBoolean("JFS.AllowWrites");

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");

    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    private static final String JFS_CMD = 
            SystemConfigurationProperties.getString("JFS.CommandLineUtil");
        
    private Sample sample;
    private List<ScalityEntity> entitiesToMove = new ArrayList<>();
    
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
        
        Long sampleId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
        sample = domainDao.getDomainObject(ownerKey, Sample.class, sampleId);
        if (sample == null) {
            throw new IllegalArgumentException("Sample not found with id="+sampleId);
        }
        
        logger.info("Retrieved sample: "+sample.getName()+" (id="+sampleId+")");

        String fileTypesStr = data.getRequiredItemAsString("FILE_TYPES");
        List<String> fileTypes = Task.listOfStringsFromCsvString(fileTypesStr);
        
        processSample(sample, fileTypes);

        if (entitiesToMove.isEmpty()) {
            logger.info("No samples to process, aborting.");
            cancel();
            return;
        }
    }

    private void processSample(Sample sample, List<String> fileTypes) throws Exception {
        logger.info("Searching "+sample.getId()+" for files to move...");
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            processSample(sample, objectiveSample, fileTypes);
        }
    }
    
    private void processSample(Sample sample, ObjectiveSample objectiveSample, List<String> fileTypes) throws Exception {

        Set<String> types = new HashSet<>(fileTypes);
        
        if (types.remove("lsm")) {
            final DateTime cutoffDate = new DateTime().minus(Period.months(1));
            
            for(SampleTile tile : objectiveSample.getTiles()) {
                for(DomainObject domainObject : domainDao.getDomainObjects(ownerKey, tile.getLsmReferences())) {
                    LSMImage lsmImage = (LSMImage)domainObject;
                    Date creationDate = lsmImage.getCreationDate();
                    if (null!=creationDate) {
                        if (cutoffDate.isAfter(new DateTime(creationDate))) {

                            String filepath = DomainUtils.getFilepath(lsmImage, FileType.LosslessStack);
                            
                            if (filepath==null) {
                                logger.warn("LSM should be moved but has no filepath: "+lsmImage);
                                return;
                            }

                            File file = new File(filepath);
                            
                            if (isExcluded(file.getName())) {
                                logger.debug("Excluding file: "+file);
                                return;
                            }
                            
                            if (!file.exists()) {
                                logger.warn("Filepath does not exist: "+file);
                                return;
                            }

                            logger.info("Will synchronize file for LSM: "+lsmImage.getId());
                            ScalityEntity entity = new ScalityLsmEntity(JFSUtils.JFS_LSM_STORE, lsmImage.getId(), lsmImage.getName(), filepath, lsmImage);
                            entitiesToMove.add(entity);
                            
                        }
                        else {
                            logger.info("LSM is too recent to move: "+lsmImage.getId());
                        }
                    }
                    else {
                    	logger.error("LSM does not have a creation date, so it can't be moved: "+lsmImage.getId());
                    }
                }
            }
        }
        
        if (types.remove("pbd")) {
            

            for(SamplePipelineRun pipelineRun : objectiveSample.getPipelineRuns()) {
                for(PipelineResult result : pipelineRun.getResults()) {
                    String filepath = DomainUtils.getFilepath(result, FileType.LosslessStack);
                    if (filepath==null) {
                        logger.warn("Result should be moved but has no filepath: "+result.getId());
                        return;
                    }
                    if (!filepath.endsWith(".v3dpbd")) {
                        return;
                    }
                    if (!filepath.startsWith(JACS_DATA_DIR) && !filepath.startsWith(JACS_DATA_ARCHIVE_DIR)) {
                        logger.warn("Result has path outside of filestore: "+result.getId());
                        return;
                    }

                    File file = new File(filepath);
                    
                    if (isExcluded(file.getName())) {
                        logger.debug("Excluding file: "+file);
                        return;
                    }
                    
                    if (!file.exists()) {
                        logger.warn("Filepath does not exist: "+file);
                        return;
                    }

                    logger.info("Will synchronize file for result: "+result.getId());
                    ScalityEntity entity = new ScalityPbdEntity(JFSUtils.JFS_PBD_STORE, result.getId(), result.getName(), filepath, result);
                    entitiesToMove.add(entity);
                }
            }
        }
        
        if (!types.isEmpty()) {
            throw new IllegalArgumentException("Illegal file types: "+types);
        }
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
        for(ScalityEntity entity : entitiesToMove) {
            writeInstanceFile(entity, configIndex++);
        }
        setJobIncrementStop(configIndex-1);
        createShellScript(writer);
    }
    
    
    private void writeInstanceFile(ScalityEntity entity, int configIndex) throws Exception {
		String jfsPath = JFSUtils.getScalityPathFromEntity(entity);
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(entity.getFilepath() + "\n");
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
        
        // Exit on any errors
        script.append("set -o errexit\n");
        
        // Print hostname for debugging purposes
        script.append(Vaa3DHelper.getHostnameEcho());
        
        // Read parameters
        script.append("read FILE_PATH\n");
        script.append("read JFS_PATH\n");
        
        // Go to the working directory
        script.append(Vaa3DHelper.getScratchDirCreationScript("WORKING_DIR"));
        script.append("cd $WORKING_DIR\n");
        
        // If the file is an LSM, ensure that it is compressed before uploading
        script.append("FILE_STUB=`basename \"$FILE_PATH\"`\n");
        script.append("FILE_EXT=${FILE_STUB##*.}\n");
        script.append("if [[ \"$FILE_EXT\" = \"lsm\" ]]; then\n");
        script.append("  WORKING_FILE=$WORKING_DIR/$FILE_STUB.bz2\n");
        script.append("  "+Vaa3DHelper.getFormattedConvertScriptCommand("$FILE_PATH","$WORKING_FILE", "")).append("\n");
        script.append("  FILE_PATH=${WORKING_FILE}\n");
        script.append("  JFS_PATH=${JFS_PATH}.bz2\n");
        script.append("fi\n");
        
        // Use JFS to write the file to the Scality Ring
        script.append("echo \"Copy source: $FILE_PATH\"\n");
        script.append("echo \"Copy target: $JFS_PATH\"\n");
        script.append("CMD=\""+JFS_CMD + " -command write -path $JFS_PATH -file $FILE_PATH -checksum\"\n");
        script.append("echo \"Running: $CMD\"\n");
        script.append("$CMD\n");
        
        // Echo a completion token
        script.append("echo \""+COMPLETION_TOKEN+"\"\n");
        
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredSlots() {
    	// Two jobs per node
        return 8;
    }

    @Override
    protected String getAdditionalNativeSpecification() {
        return "-l scalityw=1 -l sandy=true";
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

    	// Ensure each script ran to completion. This ensures that nothing was killed prematurely. 
    	for(File outputFile : outputFiles) {
    		int index = getIndexExtension(outputFile);
    		try {
        		String stdout = org.apache.commons.io.FileUtils.readFileToString(outputFile);
				if (!stdout.contains(COMPLETION_TOKEN)) {
    				hasError[index-1] = true;
    			}
    		}
        	catch (IOException e) {
    			throw new MissingDataException("Error reading STDOUT file: "+outputFile.getName(),e);
        	}
        }
    	
    	// Ensure there is nothing in STDERR. JFS will write to STDERR if there are any problems with the upload.
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
        boolean sampleDirty = false;
    	int i=0;
    	for(ScalityEntity entity : entitiesToMove) {
    		if (hasError[i++]) {
    			logger.warn("Error synchronizing entity "+entity.getName()+" (id="+entity.getId()+")");
    		}
    		else {
                String oldFilepath = entity.getFilepath();
                String jfsPath = JFSUtils.getScalityPathFromEntity(entity);
                String webdavUrl = JFSUtils.getWebdavUrlForJFSPath(jfsPath);
                File file = new File(oldFilepath);
                Long bytes = null;

                if (entity instanceof ScalityLsmEntity) {
                    ScalityLsmEntity lsmEntity = (ScalityLsmEntity)entity;
                    LSMImage lsm = lsmEntity.getImage();
                    
                    // The LSM was compressed by this pipeline, so we need to add the correct extension everywhere
                    if (oldFilepath.endsWith(".lsm")) {
                        jfsPath += ".bz2";
                        webdavUrl += ".bz2";
                        bytes = file.length();
                        lsm.setName(entity.getName()+".bz2");
                    }
                    
                    // Update LSM
                    lsm.setFilepath(jfsPath);
                    lsm.getFiles().put(FileType.LosslessStack, jfsPath);
                    try {
                        domainDao.save(lsm.getOwnerKey(), lsm);
                    }
                    catch (Exception e) {
                        logger.error("Error updating LSM id="+lsm.getId(),e);
                        throw new MissingDataException("Could not update LSM, database may be in an inconsistent state!");
                    }

                    // Update SAGE if necessary
                    if (lsm.getSageId()!=null) {
                        try {
                            if (!jfsPath.endsWith(".lsm.bz2")) {
                                logger.warn("Expected lsm.bz2 extension on LSM: "+jfsPath);
                            }
                            Image image = sage.getImage(lsm.getSageId());
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
                            logger.error("Error updating SAGE image "+lsm.getSageId(),e);
                            throw new MissingDataException("Could not update SAGE, it may be in an inconsistent state!");
                        }
                    }
                    
                }
                else if (entity instanceof ScalityPbdEntity) {
                    ScalityPbdEntity pbdEntity = (ScalityPbdEntity)entity;
                    PipelineResult result = pbdEntity.getResult();

                    // Update result
                    result.getFiles().put(FileType.LosslessStack, jfsPath);
                    sampleDirty = true;
                }
                
                logger.info("Synchronized "+entity.getId()+" to "+jfsPath);
            
    			if (deleteSourceFiles) {
    				try {
    					FileUtils.forceDelete(file);
        			    logger.info("Deleted "+oldFilepath);
    				}
    				catch (IOException e) {
    					// Log "unable to delete file" message so that they can be parsed later and the files deleted
                        logger.info(e.getMessage());
    				}
                }
    		}	
    	}
    	
    	if (sampleDirty) {
    	    try {
    	        domainDao.save(sample.getOwnerKey(), sample);
    	    }
    	    catch (Exception e) {
                logger.error("Error updating entity id="+sample.getId(),e);
                throw new MissingDataException("Could not update sample, database may be in an inconsistent state!");
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
    
    private class ScalityLsmEntity extends ScalityEntity {
        
        private LSMImage image;

        public ScalityLsmEntity(String store, Long id, String name, String filepath, LSMImage image) {
            super(store, id, name, filepath);
            this.image = image;
        }

        public LSMImage getImage() {
            return image;
        }
    }
    
    private class ScalityPbdEntity extends ScalityEntity {
        
        private PipelineResult result;

        public ScalityPbdEntity(String store, Long id, String name, String filepath, PipelineResult result) {
            super(store, id, name, filepath);
            this.result = result;
        }

        public PipelineResult getResult() {
            return result;
        }
    }
}
