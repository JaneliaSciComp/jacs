package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
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

    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    protected FileNode resultFileNode;
    protected List<String> archivedFiles = new ArrayList<String>();
    protected List<String> targetFiles = new ArrayList<String>();
    
    public void execute() throws Exception {

        this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        
    	String resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
    	if (resultEntityName == null || "".equals(resultEntityName)) {
    		throw new IllegalArgumentException("RESULT_ENTITY_NAME may not be null");
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
                previousResultFilename = getPrevResultFilename(prevResult);
            }
        }
        
        alignedConsolidatedLabelFilepath = checkForArchival(alignedConsolidatedLabelFilepath);
        previousResultFilename = checkForArchival(previousResultFilename);
        
        if (alignedConsolidatedLabelFilepath==null) alignedConsolidatedLabelFilepath = "";
        logger.info("Putting '"+alignedConsolidatedLabelFilepath+"' in ALIGNED_CONSOLIDATED_LABEL_FILEPATH");
        processData.putItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH", alignedConsolidatedLabelFilepath);
        
        if (previousResultFilename==null) previousResultFilename = "";
        logger.info("Putting '"+previousResultFilename+"' in PREVIOUS_RESULT_FILENAME");
        processData.putItem("PREVIOUS_RESULT_FILENAME", previousResultFilename);

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
                boolean resultFound = false;
                for(Entity result : EntityUtils.getChildrenOfType(run, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                    lastResult = result;
                    if (result.getId().equals(rootEntity.getId())) {
                        resultFound = true;
                        break;
                    }
                }
                
                logger.info("Check pipeline run "+run.getId()+" containsCurrentResult?="+resultFound+" resultFound?="+(lastResult!=null));
                    
                if (!resultFound && lastResult!=null) {
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
    
    protected String getPrevResultFilename(Entity separation) throws Exception {

        logger.info("Getting previous result from separation with id="+separation.getId());
        
        populateChildren(separation);
        Entity supportingFiles = EntityUtils.getSupportingData(separation);
        populateChildren(supportingFiles);
        
        Entity prevResultFile = null;
        for(Entity file : supportingFiles.getChildren()) {
            if (file.getName().equals("SeparationResultUnmapped.nsp") || file.getName().equals("SeparationResult.nsp")) {
                prevResultFile = file;
                break;
            }
        }
        
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
        if (filepath.startsWith(JACS_DATA_ARCHIVE_DIR)) {
            archivedFiles.add(filepath);
            String newPath = new File(resultFileNode.getDirectoryPath(), new File(filepath).getName()).getAbsolutePath();
            targetFiles.add(newPath);
            return newPath;
        }
        return filepath;
    }
}
