package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleProcessingParametersService extends AbstractEntityService {

    private FileNode sampleResultNode;
    private FileNode mergeResultNode;
    private FileNode stitchResultNode;
    
    public void execute() throws Exception {
        
    	sampleResultNode = (FileNode)processData.getItem("SAMPLE_RESULT_FILE_NODE");
    	if (sampleResultNode == null) {
    		throw new IllegalArgumentException("SAMPLE_RESULT_FILE_NODE may not be null");
    	}
    	
    	mergeResultNode = (FileNode)processData.getItem("MERGE_RESULT_FILE_NODE");
    	if (mergeResultNode == null) {
    		throw new IllegalArgumentException("MERGE_RESULT_FILE_NODE may not be null");
    	}

    	stitchResultNode = (FileNode)processData.getItem("STITCH_RESULT_FILE_NODE");
    	if (stitchResultNode == null) {
    		throw new IllegalArgumentException("STITCH_RESULT_FILE_NODE may not be null");
    	}
    	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        
    	logger.info("Running InitSampleProcessingParametersService for sampleId="+sampleEntityId);
    	
        List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();        

        List<Entity> tileEntities = null;
        if (sampleArea!=null) {
            logger.info("Processing tiles for area: "+sampleArea.getName());
            tileEntities = sampleArea.getTiles();
        }
        else {
            populateChildren(sampleEntity);
            Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
            if (supportingFiles == null) {
                throw new IllegalStateException("Sample does not have Supporting Files child: "+sampleEntityId);
            }
            supportingFiles = entityBean.getEntityTree(supportingFiles.getId());
            tileEntities = EntityUtils.getDescendantsOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE, true);
        }
        
    	boolean archived = populateMergedLsmPairs(tileEntities, mergedLsmPairs);

        if (mergedLsmPairs.isEmpty()) {
            throw new Exception("Sample (id="+sampleEntityId+") has no tiles");
        }
        
    	File stitchedFile = new File(stitchResultNode.getDirectoryPath(), "stitched-"+sampleEntity.getId()+".v3draw");
    	
    	List<String> stackFilenames = new ArrayList<String>();
    	stackFilenames.add(stitchedFile.getAbsolutePath());
    	for (MergedLsmPair mergedLsmPair : mergedLsmPairs) {
    		stackFilenames.add(mergedLsmPair.getMergedFilepath());
    	}
    	
    	String sampleProcessingResultsName = "Sample Processing Results";
        if (sampleArea!=null && !StringUtils.isEmpty(sampleArea.getName())) {
            sampleProcessingResultsName += " ("+sampleArea.getName()+")";
        }
    	
    	logger.info("Putting "+stitchedFile.getAbsolutePath()+" in STITCHED_FILENAME");
    	logger.info("Putting "+mergedLsmPairs.size()+" items in BULK_MERGE_PARAMETERS");
    	logger.info("Putting "+stackFilenames.size()+" items in STACK_FILENAMES");
    	logger.info("Putting "+archived+" in COPY_FROM_ARCHIVE");
    	logger.info("Putting "+sampleProcessingResultsName+" in SAMPLE_PROCESSING_RESULTS_NAME");
    	
    	processData.putItem("STITCHED_FILENAME", stitchedFile.getAbsolutePath());
    	processData.putItem("BULK_MERGE_PARAMETERS", mergedLsmPairs);
    	processData.putItem("STACK_FILENAMES", stackFilenames);
    	processData.putItem("COPY_FROM_ARCHIVE", archived);
    	processData.putItem("SAMPLE_PROCESSING_RESULTS_NAME", sampleProcessingResultsName);
    }
    
    private boolean populateMergedLsmPairs(List<Entity> tileEntities, List<MergedLsmPair> mergedLsmPairs) throws Exception {
        
        boolean archived = false;
        
        for(Entity tileEntity : tileEntities) {
            String lsmFilepath1 = null;
            String lsmFilepath2 = null;
            
            boolean gotFirst = false;
            for(EntityData ed : tileEntity.getOrderedEntityData()) {
                Entity lsmStack = ed.getChildEntity();
                if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    if (gotFirst) {
                        lsmFilepath2 = filepath;
                    }
                    else {
                        lsmFilepath1 = filepath;
                        gotFirst = true;
                    }   
                }
            }
            
            File mergedFile = null;
                
            File lsmFile1 = new File(lsmFilepath1);
            if (!lsmFile1.exists()||!lsmFile1.canRead()) {
                throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile1.getAbsolutePath());
            }
            
            File lsmFile2 = null;
            if (lsmFilepath2!=null) {
                lsmFile2 = new File(lsmFilepath2);  
                if (!lsmFile2.exists()||!lsmFile2.canRead()) {
                    throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile2.getAbsolutePath());
                }
                mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-"+tileEntity.getId()+".v3draw");
            }
            else {
                // lsmFilepath2 may be null
                mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-"+tileEntity.getId()+".v3draw");    
            }
            
            String lsmRealPath1 = lsmFile1.getCanonicalPath();
            String lsmRealPath2 = lsmFile2==null?null:lsmFile2.getCanonicalPath();
            
            if (lsmRealPath1.startsWith("/archive") || (lsmRealPath2!=null && lsmRealPath2.startsWith("/archive"))) {
                archived = true;
            }
            
            mergedLsmPairs.add(new MergedLsmPair(lsmRealPath1, lsmRealPath2, mergedFile.getAbsolutePath()));
        }

        return archived;
    }
}
