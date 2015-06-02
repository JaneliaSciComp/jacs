package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Moves the given Sample's files to the Scality object store via the Sproxyd REST API and updates the object model to add a Scality BPID attribute to each file entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncSampleToScalityGridService extends AbstractEntityGridService {

    private static final String CONFIG_PREFIX = "scalityConfiguration.";
    private static final String TIMING_PREFIX="Timing: ";

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");

    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    private static final String ARCHIVE_SYNC_CMD = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ArchiveSyncSproxyd.Timing.ScriptPath");
    
    private static final String REMOVE_COMMAND = "rm -rf"; 
    
    private Entity sampleEntity;
    private List<Entity> entitiesToMove = new ArrayList<Entity>();
    private boolean deleteSourceFiles = false;

    @Override
    protected String getGridServicePrefixName() {
        return "scality";
    }

    @Override
    protected void init() throws Exception {

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
            logger.info("Searching for LSM files to move...");
            EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                    .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                    .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                    .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                    .run(new EntityVisitor() {
                public void visit(Entity entity) throws Exception {
                    addToEntitiesToMove(entity);
                }
            });
        }
        
        if (types.remove("pbd")) {
            logger.info("Searching for PBD files to move...");
            EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
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

        String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        
        if (filepath==null) {
            logger.warn("Entity should be moved to Scality but has no filepath: "+entity.getId());
            return;
        }

        File file = new File(filepath);
        if (!file.exists()) {
            logger.warn("Entity has filepath which does not exist: "+entity.getId());
            return;
        }

        logger.info("Will synchronized file for entity: "+entity.getId());
        entitiesToMove.add(entity);
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
		String scalityUrl = ScalityDAO.getUrlFromEntityId(entity.getId());
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(filepath + "\n");
        	fw.write(scalityUrl + "\n");
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
        script.append("read SCALITY_URL\n");
        script.append("cd ").append(resultFileNode.getDirectoryPath()).append("\n");
        script.append(Vaa3DHelper.getHostnameEcho());
        script.append("echo \"Copy source: $FILE_PATH\"\n");
        script.append("echo \"Copy target: $SCALITY_URL\"\n");
        script.append("timing=`"+ARCHIVE_SYNC_CMD + " PUT \"$FILE_PATH\" \"$SCALITY_URL\"`\n");
        script.append("echo \"$timing\"\n");
        if (deleteSourceFiles) {
            script.append(REMOVE_COMMAND + " \"$FILE_PATH\"\n");    
        }
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
	public void postProcess() throws MissingDataException {

        logger.debug("Processing "+resultFileNode.getDirectoryPath());

        File outputDir = new File(resultFileNode.getDirectoryPath(), "sge_output");
    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, getGridServicePrefixName());
    	
    	if (outputFiles.length != entitiesToMove.size()) {
    		throw new MissingDataException("Number of entities to move ("+entitiesToMove.size()+") does not match number of output files ("+outputFiles.length+")");
    	}
    	
    	boolean[] hasError = new boolean[entitiesToMove.size()];
    	String[] timings = new String[entitiesToMove.size()];

    	for(File outputFile : outputFiles) {
    		int index = getIndexExtension(outputFile);
    		Scanner in = null;
    		try {
    			String timingCsv = null;
    			in = new Scanner(new FileReader(outputFile));
    			while (in.hasNext()) {
    				String line = in.nextLine();
    				if (line.startsWith("Result: success")) {
						hasError[index-1] = false;
    				}
                    else if (line.startsWith("Result: failure")) {
                        hasError[index-1] = true;
                        logger.error("Error uploading "+entitiesToMove.get(index).getId()+" to Scality. Details can be found at "+outputFile.getAbsolutePath());
                    }
    				else if (line.startsWith(TIMING_PREFIX)) {
    					timingCsv = line.substring(TIMING_PREFIX.length());
    				}
    			}
    			if (timingCsv!=null) {
    				timings[index-1] = timingCsv;
    			}
    		}
    		catch (FileNotFoundException e) {
    			throw new MissingDataException("Missing file "+outputFile.getName(),e);
    		}
    		finally {
    		    if (in!=null) in.close();
    		}
        }

        File errorDir = new File(resultFileNode.getDirectoryPath(), "sge_error");
        File[] errorFiles = FileUtil.getFilesWithPrefixes(errorDir, getGridServicePrefixName());

        for (File errorFile : errorFiles) {
            int index = getIndexExtension(errorFile);
            if (errorFile.length() > 0) {
                logger.warn("Not empty error file: " + errorFile.getAbsolutePath());
                hasError[index-1] = true;
            }
        }

        int i=0;
        StringBuilder sb = new StringBuilder();
        for(Entity entity : entitiesToMove) {
            String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            File file = new File(filepath);
            if (!hasError[i]) {
                String timingCsv = timings[i];
                sb.append("\nScalityBenchmark,PUT,Sproxyd,"+file.getName()+","+timingCsv);
            }
            else {
                sb.append("\nScalityBenchmark,PUT,Sproxyd,"+file.getName()+",Error");
            }
            i++;
        }
        logger.info("Timings:"+sb);
        
    	i=0;
    	for(Entity entity : entitiesToMove) {
    		if (!hasError[i++]) {
                EntityData filepathEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	        String scalityUrl = ScalityDAO.getUrlFromEntityId(entity.getId());
    			logger.info("Successfully synchronized entity "+entity.getName()+" to "+scalityUrl);
                if (deleteSourceFiles) {
                    logger.info("  and deleted "+filepathEd.getValue());
                }
                try {
                    String bpid = ScalityDAO.getBPIDFromEntityId(entity.getId());
        			entityBean.setOrUpdateValue(entity.getId(), EntityConstants.ATTRIBUTE_SCALITY_BPID, bpid);
        			if (deleteSourceFiles) {
        			    entityBean.deleteEntityData(filepathEd);
        			}
                }
                catch (ComputeException e) {
                    logger.error("Error updating entity id="+entity.getId(),e);
                }
    			
    		}	
    		else {
    			logger.warn("Error synchronizing entity "+entity.getName()+" (id="+entity.getId()+")");
    		}
    	}
	}

    private int getIndexExtension(File file) {
        String name = file.getName();
        String ext = name.substring(name.lastIndexOf('.')+1);
        return Integer.parseInt(ext);
    }
}
