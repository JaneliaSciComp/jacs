package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Extracts metadata from the entity model to be used for the neuron separator. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSeparationParametersService extends AbstractEntityService {

    protected static final String ARCHIVE_PREFIX = "/archive";
    
    protected FileNode resultFileNode;
    protected List<String> archivedFiles = new ArrayList<String>();
    protected List<String> targetFiles = new ArrayList<String>();
    
    public void execute() throws Exception {

        this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);

        String inputFilename = (String)processData.getItem("INPUT_FILENAME");
        if (inputFilename==null || "".equals(inputFilename)) {
            throw new IllegalArgumentException("Input parameter INPUT_FILENAME may not be empty");
        }
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

    	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
    	if (rootEntityId == null || "".equals(rootEntityId)) {
    		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
    	}

    	Entity rootEntity = entityBean.getEntityById(rootEntityId);
    	if (rootEntity == null) {
    		throw new IllegalArgumentException("Root entity not found with id="+sampleEntityId);
    	}

        String alignedConsolidatedLabelFilepath = (String)processData.getItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH");
        String previousResultFilename = null;
        
        if (StringUtils.isEmpty(alignedConsolidatedLabelFilepath)) {
            Entity prevResult = findPrevResult(rootEntity, sampleEntity);
            
            if (prevResult!=null) {
                data.putItem("PREVIOUS_SEPARATION_ID", prevResult.getId().toString());
                previousResultFilename = getPrevResultFilename(prevResult);
            }
        }
        
        // First, make sure we fetch the companion file
        if (previousResultFilename!=null) {
        	checkForArchival(previousResultFilename.replaceFirst("\\.nsp$", ".pbd"));
        }
        
        inputFilename = checkForArchival(inputFilename);
        alignedConsolidatedLabelFilepath = checkForArchival(alignedConsolidatedLabelFilepath);
        previousResultFilename = checkForArchival(previousResultFilename);
        
        if (inputFilename==null) inputFilename = "";
        data.putItem("INPUT_FILENAME", inputFilename);
        
        if (alignedConsolidatedLabelFilepath==null) alignedConsolidatedLabelFilepath = "";
        data.putItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH", alignedConsolidatedLabelFilepath);
        
        if (previousResultFilename==null) previousResultFilename = "";
        data.putItem("PREVIOUS_RESULT_FILENAME", previousResultFilename);

        if (!archivedFiles.isEmpty()) {
            logger.info("Putting true in COPY_FROM_ARCHIVE");
            processData.putItem("COPY_FROM_ARCHIVE", Boolean.TRUE);
            logger.info("Putting "+archivedFiles.size()+" objects in SOURCE_FILE_PATHS");
            processData.putItem("SOURCE_FILE_PATHS", Task.csvStringFromCollection(archivedFiles));
            logger.info("Putting "+targetFiles.size()+" objects in TARGET_FILE_PATHS");
            processData.putItem("TARGET_FILE_PATHS", Task.csvStringFromCollection(targetFiles));
        }
        else {
            logger.info("Putting false in COPY_FROM_ARCHIVE");
            processData.putItem("COPY_FROM_ARCHIVE", Boolean.FALSE);
            logger.info("Putting null in SOURCE_FILE_PATHS");
            processData.putItem("SOURCE_FILE_PATHS", null);
            logger.info("Putting null in TARGET_FILE_PATHS");
            processData.putItem("TARGET_FILE_PATHS", null);
        }
    }
    
    protected Entity findPrevResult(Entity rootEntity, Entity sampleEntity) throws Exception {

        populateChildren(rootEntity);
        Entity prevSeparation = EntityUtils.getLatestChildOfType(rootEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        if (prevSeparation != null) {
            logger.info("Found previous separation in the current result entity");
            return prevSeparation;
        }
        else {
            logger.info("Checking sample for previous separations: "+sampleEntity.getId());
            
            populateChildren(sampleEntity);
            List<Entity> runs = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN);
            Collections.reverse(runs);
            
            for(Entity run : runs) {
                populateChildren(run);
                    
                Entity lastResult = null;
                boolean currentResultFound = false;
                for(Entity result : EntityUtils.getChildrenOfType(run, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                    lastResult = result;
                    if (result.getId().equals(rootEntity.getId())) {
                        currentResultFound = true;
                        break;
                    }
                }
                
                logger.info("Check pipeline run "+run.getId()+" containsCurrentResult?="+currentResultFound+" resultFound?="+(lastResult!=null));
                    
                if (!currentResultFound && lastResult!=null) {
                    populateChildren(lastResult);
                    List<Entity> separations = EntityUtils.getChildrenOfType(lastResult, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                    Collections.reverse(separations);
                    
                    for(Entity separation : separations) {
                        logger.debug("Found previous separation in the previous pipeline run");
                        return separation;
                    }
                }
            }
        }
        
        return null;
    }
    
    public String getPrevResultFilename(Entity separation) throws Exception {

        logger.info("Getting previous result from separation with id="+separation.getId());
        
        populateChildren(separation);
        Entity supportingFiles = EntityUtils.getSupportingData(separation);
        
        if (supportingFiles==null) {
            logger.warn("Separation has no supporting files: "+separation.getId());
            return null;
        }
        
        populateChildren(supportingFiles);
        
        Entity prevResultFile = NeuronSeparatorHelper.getSeparationResult(supportingFiles);
        if (prevResultFile!=null) {
            String filepath = EntityUtils.getFilePath(prevResultFile);
            if (filepath!=null && !"".equals(filepath)) {
                return filepath;
            }
        }
        
        return null;
    }

    protected String checkForArchival(String filepath) throws Exception {
        if (filepath==null) return null;
        if (filepath.startsWith(ARCHIVE_PREFIX)) {
            archivedFiles.add(filepath);
            String newPath = new File(resultFileNode.getDirectoryPath()+"/tmp/", new File(filepath).getName()).getAbsolutePath();
            targetFiles.add(newPath);
            logger.info("Archived file "+filepath+" will be copied to "+newPath);
            return newPath;
        }
        return filepath;
    }
}
