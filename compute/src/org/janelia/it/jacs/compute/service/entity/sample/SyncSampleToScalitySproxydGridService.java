package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
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
 * Moves the given Sample's files to the Scality object store via the Sproxyd REST API and updates the object model to add a Scality Id attribute to each file entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncSampleToScalitySproxydGridService extends AbstractEntityGridService {

    protected static final String CONFIG_PREFIX = "scalityConfiguration.";
    
    protected static final String ARCHIVE_SYNC_CMD = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ArchiveSyncSproxyd.Timing.ScriptPath");
    protected static final String REMOVE_COMMAND = "rm -rf"; 
    
    protected static final String TIMING_PREFIX="Timing: ";

    protected static final String[] NON_SCALITY_PREFIXES = { "/tier2" };
    protected static final String SCALITY_ROOT_PATH = 
            SystemConfigurationProperties.getString("Root.Scality.Dir");

    protected static final String ITERATION = "1";
    
    protected int configIndex = 1;
    protected Entity sampleEntity;
    protected List<Entity> entitiesToMove = new ArrayList<Entity>();
    protected boolean deleteSourceFiles = false;

    @Override
    protected String getGridServicePrefixName() {
        return "scality";
    }

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
                    // TODO: REMOVE LATER
                    // For benchmarking purposes we want big files
                    if ("3".equals(lsm.getValueByAttributeName("Num Channels"))) {
                        logger.info("Will move "+lsm.getName());
                        entitiesToMove.add(lsm);
                    }
                }
            });
        }
        
        // TODO: REMOVE LATER
        // Move just one file per sample to make benchmarking easier
//        Entity first = entitiesToMove.get(0);
//        entitiesToMove.clear();
//        entitiesToMove.add(first);
        
        if (!types.isEmpty()) {
            logger.warn("Unrecognized file types specified in FILE_TYPES: "+types);
        }
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

		for(String prefix : NON_SCALITY_PREFIXES) {
			if (filepath.startsWith(prefix)) {
				break;
			}
			else if (filepath.startsWith("/groups/flylight/flylight")) {
		        // TODO: REMOVE LATER
				// hack for working with outdated val-db
				File file = new File(filepath);
				String name = file.getName();
				name = name.substring(0,name.lastIndexOf('.'));
				filepath = filepath.replaceFirst("/groups/flylight", "/tier2")+".bz2";
				break;
			}
		}
		
		String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
		
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

    protected void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read FILE_PATH\n");
        script.append("read SCALITY_URL\n");
        script.append("cd ").append(resultFileNode.getDirectoryPath()).append("\n");
        script.append("hostname > hostname\n");
        script.append("echo \"Copying $FILE_PATH to $SCALITY_URL\"\n");
        script.append("timing=`"+ARCHIVE_SYNC_CMD + " PUT \"$FILE_PATH\" \"$SCALITY_URL\"`\n");
        script.append("echo \"$timing\"");
        if (deleteSourceFiles) {
            script.append(REMOVE_COMMAND + " \"$SOURCE_FILE\"\n");    
        }
        writer.write(script.toString());
    }

    @Override
    protected String getNativeSpecificationOverride() {
        return "-q 'test.q@h02*' -pe batch 16";
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
    		try {
    			String timingCsv = null;
    			Scanner in = new Scanner(new FileReader(outputFile));
    			while (in.hasNext()) {
    				String line = in.nextLine();
    				if ("HTTP/1.1 100 Continue".equals(line)) {
    					continue;
    				}
    				if (line.startsWith("HTTP/1.1 ")) {
    					String responseCodeStr = line.substring(9).trim();
    					if (!"200".equals(responseCodeStr)) {
    						hasError[index] = true;
    						logger.error("Error uploading "+entitiesToMove.get(index).getId()+" to Scality. Details can be found at "+outputFile.getAbsolutePath());
    					}
    				}
    				if (line.startsWith(TIMING_PREFIX)) {
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
    	}

        File errorDir = new File(resultFileNode.getDirectoryPath(), "sge_error");
    	File[] errorFiles = FileUtil.getFilesWithPrefixes(errorDir, getGridServicePrefixName());

    	for(File errorFile : errorFiles) {
    		int index = getIndexExtension(errorFile);
    		if (errorFile.length()>0) {
    			logger.warn("Not empty error file: "+errorFile.getAbsolutePath());
				hasError[index-1] = true;
    		}
    	}
    	
    	int i=0;
    	for(Entity entity : entitiesToMove) {
    		if (!hasError[i++]) {
    			logger.debug("Successfully moved entity "+entity.getName()+" (id="+entity.getId()+")");
    			// TODO: update model
//    			String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
//    			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_URL, scalityUrl);
    		}	
    		else {
    			logger.warn("Error moving entity "+entity.getName()+" (id="+entity.getId()+")");
    		}
    	}
    	
    	i=0;
    	StringBuilder sb = new StringBuilder();
    	for(Entity entity : entitiesToMove) {
			String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			File file = new File(filepath);
    		if (!hasError[i]) {
    			String timingCsv = timings[i];
    			sb.append("\nScalityBenchmark"+ITERATION+",PUT,Sproxyd,"+file.getName()+","+timingCsv);
        	}
    		else {
    			sb.append("\nScalityBenchmark"+ITERATION+",PUT,Sproxyd,"+file.getName()+",WriteError");
    		}
    		i++;
    	}
		logger.info("Timings:"+sb);
	}

    protected int getIndexExtension(File file) {
        String name = file.getName();
        String ext = name.substring(name.lastIndexOf('.')+1);
        return Integer.parseInt(ext);
    }
}
