package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Moves the given Sample's files to the Scality object store via a Fuse mount and updates the object model to add a Scality Id attribute to each file entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncSampleToScalityFuseGridService extends AbstractEntityGridService {
    
	protected static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    protected static final String CONFIG_PREFIX = "scalityConfiguration.";

    protected static final String TIMING_PREFIX="Timing: ";
    protected static final String ARCHIVE_SYNC_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ArchiveSync.Timing.ScriptPath");
    protected static final String REMOVE_COMMAND = "rm -rf"; 

    protected static final String[] NON_SCALITY_PREFIXES = { "/tier2" };
    protected static final String SCALITY_ROOT_PATH = 
            SystemConfigurationProperties.getString("Root.Scality");
    
    protected int configIndex = 1;
    protected Entity sampleEntity;
    protected List<Entity> entitiesToMove = new ArrayList<Entity>();
    private boolean deleteSourceFiles = false;

    @Override
    protected void init() throws Exception {

        Long sampleEntityId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
        sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }
        
        logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        String fileTypes = data.getRequiredItemAsString("FILE_TYPES");
        List<String> types = Task.listOfStringsFromCsvString(fileTypes);
        
        if (types.remove("lsms")) {
        	logger.info("Searching for LSMs to move...");
	        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
	                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
	                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
	                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
	                .run(new EntityVisitor() {
	            public void visit(Entity lsm) throws Exception {
	            	logger.info("Will move "+lsm.getName());
	        		entitiesToMove.add(lsm);
	            }
	        });
        }
        
        if (!types.isEmpty()) {
        	logger.warn("Unrecognized file types specified in FILE_TYPES: "+types);
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "scality";
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

    protected void writeInstanceFile(Entity entity, int configIndex) throws Exception {
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		if (filepath==null) {
			logger.warn("Entity should be moved to Scality but has no filepath: "+entity.getId());
			return;
		}
		
		int fsi = filepath.indexOf("filestore");
		String relPath = filepath.substring(fsi);
		String newPath = SCALITY_ROOT_PATH+relPath;
		
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(filepath + "\n");
        	fw.write(newPath + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    protected void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read SOURCE_FILE\n");
        script.append("read TARGET_FILE\n");
        script.append("timing=`"+ARCHIVE_SYNC_CMD + " cp \"$SOURCE_FILE\" \"$TARGET_FILE\"`\n");
        script.append("echo \""+TIMING_PREFIX+"$timing\"");
        if (deleteSourceFiles) {
            script.append(REMOVE_COMMAND + " \"$SOURCE_FILE\"\n");    
        }
        writer.write(script.toString());
    }
    
    @Override
    protected String getNativeSpecificationOverride() {
    	return "-q 'new.q@h02*'";
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	boolean[] hasError = new boolean[entitiesToMove.size()];
    	
        File outputDir = new File(resultFileNode.getDirectoryPath(), "sge_output");
    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, getGridServicePrefixName());
    	
    	if (outputFiles.length != entitiesToMove.size()) {
    		throw new MissingDataException("Number of entities to move ("+entitiesToMove.size()+") does not match number of output files ("+outputFiles.length+")");
    	}

    	for(File outputFile : outputFiles) {
    		String name = outputFile.getName();
    		String ext = name.substring(name.lastIndexOf('.'));
    		int index = Integer.parseInt(ext);
    		try {
    			String timingCsv = null;
    			Scanner in = new Scanner(new FileReader(outputFile));
    			while (in.hasNext()) {
    				String line = in.nextLine();
    				if (line.startsWith(TIMING_PREFIX)) {
    					timingCsv = line.substring(TIMING_PREFIX.length());
    					logger.info("Got timing: ["+timingCsv+"]");
    					break;
    				}
    			}
    			
    			if (timingCsv!=null) {
					String[] timingValues = timingCsv.split(",");
					if (timingValues.length==3) {
						logger.info("Scality PUT Rate: "+timingValues[2]+" Gbps");
					}
					else {
						hasError[index] = true;
						logger.warn("Could not parse timing: "+timingCsv);
					}
    			}
    		}
    		catch (FileNotFoundException e) {
    			throw new MissingDataException("Missing file "+outputFile.getName(),e);
    		}
    	}

        File errorDir = new File(resultFileNode.getDirectoryPath(), "sge_error");
    	File[] errorFiles = FileUtil.getFilesWithPrefixes(errorDir, getGridServicePrefixName());

    	for(File errorFile : errorFiles) {
    		int index = getIndex(errorFile);
    		if (errorFile.length()>0) {
    			logger.warn("Not empty error file: "+errorFile.getAbsolutePath());
				hasError[index] = true;
    		}
    	}
    	
    	int i=0;
    	for(Entity entity : entitiesToMove) {
    		if (!hasError[i++]) {
    			logger.warn("Successfully moved entity "+entity.getName()+" (id="+entity.getId()+")");
    			// TODO: update model
    			//String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
    			//entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_URL, scalityUrl);
    		}
    		else {
    			logger.warn("Error moving entity "+entity.getName()+" (id="+entity.getId()+")");
    		}
    	}
	}
    
    private int getIndex(File file) {
		String name = file.getName();
		String ext = name.substring(name.lastIndexOf('.'));
		return Integer.parseInt(ext);
    }
}
