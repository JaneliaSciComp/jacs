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
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Moves the given Sample's files to the Scality object store and updates the object model to add a Scality Id attribute to each file entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MoveSampleFilesToScalityGridService extends AbstractEntityGridService {
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "scalityConfiguration.";

    private int configIndex = 1;
    private Entity sampleEntity;
    private List<Entity> entitiesToMove = new ArrayList<Entity>();

    @Override
    protected void init() throws Exception {

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

        String fileTypes = data.getRequiredItemAsString("FILE_TYPES");
        List<String> types = Task.listOfStringsFromCsvString(fileTypes);
        
        if (types.contains("lsms")) {
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
    }

    @Override
    protected String getGridServicePrefixName() {
        return "scality";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles();
        setJobIncrementStop(configIndex-1);
    	createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {
    	int configIndex = 0;
    	for(Entity entity : entitiesToMove) {
    		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		if (filepath==null) {
    			logger.warn("Entity should be moved to Scality but has no filepath: "+entity.getId());
    			continue;
    		}
    		String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
    		writeInstanceFile(filepath, scalityUrl, configIndex);
    	}
    	configIndex++;
    }

    private void writeInstanceFile(String filepath, String scalityUrl, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);FileWriter fw = new FileWriter(configFile);
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
        script.append("curl -D - -X PUT $SCALITY_URL --data-binary @$FILE_PATH\n");
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredSlots() {
    	return 1;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {


        File outputDir = new File(resultFileNode.getDirectoryPath(), "sge_output");
    	File[] outputFiles = FileUtil.getFilesWithPrefixes(outputDir, getGridServicePrefixName());
    	
    	if (outputFiles.length != entitiesToMove.size()) {
    		throw new MissingDataException("Number of entities to move ("+entitiesToMove.size()+") does not match number of output files ("+outputFiles.length+")");
    	}
    	
    	boolean[] hasError = new boolean[entitiesToMove.size()];

    	for(File outputFile : outputFiles) {
    		String name = outputFile.getName();
    		String ext = name.substring(name.lastIndexOf('.'));
    		int index = Integer.parseInt(ext);
    		try {
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
    			}
    		}
    		catch (FileNotFoundException e) {
    			throw new MissingDataException("Missing file "+outputFile.getName(),e);
    		}
    	}
    	
    	int i=0;
    	for(Entity entity : entitiesToMove) {
    		if (!hasError[i++]) {
    			String scalityUrl = ScalityDAO.getUrl(""+entity.getId());
    			entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_URL, scalityUrl);
    		}	
    	}
	}
}
