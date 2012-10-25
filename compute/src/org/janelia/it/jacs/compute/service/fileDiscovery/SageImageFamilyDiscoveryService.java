package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.util.*;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;

/**
 * Discovers images in SAGE which are part of a particular image family and not part of a data set, and creates 
 * or updates Samples within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageImageFamilyDiscoveryService extends SageDataSetDiscoveryService {

    @Override
    public void execute() throws Exception {

        String sageImageFamily = (String)processData.getItem("SAGE_IMAGE_FAMILY");
        if (sageImageFamily==null) {
    		throw new IllegalArgumentException("SAGE_IMAGE_FAMILY may not be null");
        }
        
		if ("system".equals(user.getUserLogin())) {
        	topLevelFolder = createOrVerifyRootEntity(PUBLIC_DATA_SET_FOLDER_NAME, true, false);
		}
		else {
        	topLevelFolder = createOrVerifyRootEntity(PRIVATE_DATA_SET_FOLDER_NAME, true, false);
		}
        
        logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
        
        processSageDataSet(sageImageFamily, null);	
    
        fixOrderIndices();
        fixTilingPatternFolderIndices();
        
        logger.info("Created "+numSamplesCreated+" samples, added "+numSamplesAdded+" samples to their corresponding data set folders.");
    }


    protected void fixTilingPatternFolderIndices() throws Exception {
		logger.info("Fixing order indicies for tiling pattern data sets in "+topLevelFolder.getName());
		populateChildren(topLevelFolder);

    	Map<String,EntityData> folders = new HashMap<String,EntityData>();
		for(EntityData ed : topLevelFolder.getEntityData()) {
			if (ed.getChildEntity()!=null) {
				folders.put(ed.getChildEntity().getName(), ed);
			}
		}

		int orderIndex = 0;
		for(TilingPattern pattern : TilingPattern.values()) {
			String dataSetName = "FlyLight "+pattern.getName();
			EntityData ed = folders.get(dataSetName);
			if (ed==null) continue;
			if (ed.getOrderIndex()==null || orderIndex!=ed.getOrderIndex()) {
				logger.info("  Updating link (id="+ed.getId()+") to "+ed.getChildEntity().getName()+" with order index "+orderIndex+" (was "+ed.getOrderIndex()+")");
				ed.setOrderIndex(orderIndex);
				entityBean.saveOrUpdateEntityData(ed);
			}
			orderIndex++;
		}
    }
    
    @Override
    protected Entity createOrUpdateSample(String sampleIdentifier, String dataSetIdentifier,  
    		List<ImageTileGroup> tileGroupList) throws Exception {

        List<String> tags = new ArrayList<String>();
        for(ImageTileGroup tileGroup : tileGroupList) {
        	tags.add(tileGroup.getTag());
        }
        
        TilingPattern tiling = TilingPattern.getTilingPattern(tags);
        
        if (tiling != null && tiling.isStitchable()) {
        	// This is a stitchable case
        	return createOrUpdateSample(sampleIdentifier, dataSetIdentifier, tiling, tileGroupList);
        }
        else {
        	// This is a disconnected case
        	if (TilingPattern.OPTIC_CENTRAL_BORDER_AND_OPTIC_TILE.equals(tiling)) {
        		// In this super special case, we have to break up the slide group into left and right pieces.

            	List<ImageTileGroup> tileGroupListLeft = new ArrayList<ImageTileGroup>();
            	List<ImageTileGroup> tileGroupListRight = new ArrayList<ImageTileGroup>();

            	String sampleNameLeft = null;
        		String sampleNameRight = null;
        		
        		TilingPattern tilingLeft = null;
        		TilingPattern tilingRight = null;
        		
            	for(ImageTileGroup tileGroup : tileGroupList) {
            		if (tileGroup.getTag().startsWith("Left")) {
            			tileGroupListLeft.add(tileGroup);
            			if (sampleNameLeft==null) {
            				sampleNameLeft = sampleIdentifier+"-"+tileGroup.getTag().replaceAll(" ", "_");	
            				tilingLeft = TilingPattern.OPTIC_TILE;
            			}
            			else {
            				sampleNameLeft = sampleIdentifier+"-Optic_Central_Border";
            				tilingLeft = TilingPattern.OPTIC_CENTRAL_BORDER;
            			}
            		}	
            		else if (tileGroup.getTag().startsWith("Right")) {
            			tileGroupListRight.add(tileGroup);
            			if (sampleNameRight==null) {
            				sampleNameRight = sampleIdentifier+"-"+tileGroup.getTag().replaceAll(" ", "_");	
            				tilingRight = TilingPattern.OPTIC_TILE;
            			}
            			else {
            				sampleNameRight = sampleIdentifier+"-Optic_Central_Border";
            				tilingRight = TilingPattern.OPTIC_CENTRAL_BORDER;
            			}
            		}	
            		else {
            			logger.warn("Tiling pattern for "+sampleIdentifier+" is "+tiling+", but it contains an invalid tile: "+tileGroup.getTag());
            		}
        		}

            	createOrUpdateSample(sampleNameLeft, dataSetIdentifier, tilingLeft, tileGroupListLeft);
            	createOrUpdateSample(sampleNameRight, dataSetIdentifier, tilingRight, tileGroupListRight);	
        	}
        	else {
        		// In non-stitchable cases we just create a Sample for each tile
            	for(ImageTileGroup tileGroup : tileGroupList) {

                    String sampleName = sampleIdentifier+"-"+tileGroup.getTag().replaceAll(" ", "_");
                    
                	List<ImageTileGroup> sampleTileGroupList = new ArrayList<ImageTileGroup>();
            		sampleTileGroupList.add(tileGroup);
            		
                    List<String> newTags = new ArrayList<String>();
                    newTags.add(tileGroup.getTag());
            		TilingPattern newTiling = TilingPattern.getTilingPattern(newTags);
                	
                	createOrUpdateSample(sampleName, dataSetIdentifier, newTiling, sampleTileGroupList);
            	}
        	}
        	
        	return null;
        }
    }
    
    private Entity createOrUpdateSample(String sampleName, String dataSetIdentifier,  TilingPattern tiling, 
    		List<ImageTileGroup> sampleTileGroupList) throws Exception {

        if (dataSetIdentifier==null) {
        	dataSetIdentifier = getDataSetIdentifierForTilingPattern(tiling);
        }
        logger.info("Subsetted sample "+sampleName+" has tiling pattern "+tiling.getName()
        		+" and data set "+dataSetIdentifier+" (stitchable="+tiling.isStitchable()+")");	
        
    	Entity sample = super.createOrUpdateSample(sampleName, dataSetIdentifier, sampleTileGroupList);
    	updateSampleTilingPattern(sample, tiling);
    	return sample;
    }
    
    private String getDataSetIdentifierForTilingPattern(TilingPattern tiling) throws ComputeException {

    	String dataSetName = "FlyLight "+tiling.getName();
    	Entity dataSet = annotationBean.getUserDataSetByName(user.getUserLogin(), dataSetName);
    	
    	if (dataSet == null) {
    		dataSet = annotationBean.createDataSet(user.getUserLogin(), dataSetName);
    		PipelineProcess process = getPipelineProcess(tiling);
    		dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, process.toString());
			entityBean.saveOrUpdateEntity(dataSet);
			logger.warn("Created new data set: "+dataSet.getName());
    	}
    	
    	return dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    }
    
    private PipelineProcess getPipelineProcess(TilingPattern tiling) {
    	switch (tiling) {
    	case WHOLE_BRAIN:
    		return PipelineProcess.FlyLightWholeBrain;
    	case OPTIC_SPAN:
    		return PipelineProcess.FlyLightWholeBrain;
    	case CENTRAL_BRAIN:
    		return PipelineProcess.FlyLightCentralBrain;
    	case OPTIC_CENTRAL_BORDER:
    		return PipelineProcess.FlyLightUnaligned;
    	case OPTIC_TILE:
    		return PipelineProcess.FlyLightOpticLobe;
    	case CENTRAL_TILE:
    		return PipelineProcess.FlyLightUnaligned;
    	case OTHER:
    		return PipelineProcess.FlyLightUnaligned;
    	case UNKNOWN:
    		return PipelineProcess.FlyLightUnaligned;
    	default:
    		throw new IllegalStateException("Unrecognized tiling pattern: "+tiling);
    	}
    }
    
    private void updateSampleTilingPattern(Entity sample, TilingPattern tiling) throws Exception {
		logger.info("    Setting properties: pattern="+tiling);
		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
		entityBean.saveOrUpdateEntity(sample);
    }
}
