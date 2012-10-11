package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected void createOrUpdateSamples(String sampleIdentifier, String dataSetIdentifier,  
    		String sampleChannelSpec, List<ImageTileGroup> tileGroupList) throws Exception {

        List<String> tags = new ArrayList<String>();
        for(ImageTileGroup filePair : tileGroupList) {
        	tags.add(filePair.getTag());
        }
        
        TilingPattern tiling = TilingPattern.getTilingPattern(tags);
        logger.info("Sample "+sampleIdentifier+" has tiling pattern: "+tiling.getName()+" (stitchable="+tiling.isStitchable()+")");	

        if (dataSetIdentifier==null) {

        	String dataSetName = "FlyLight "+tiling.getName();
        	Entity dataSet = annotationBean.getUserDataSetByName(user.getUserLogin(), dataSetName);
        	
        	if (dataSet == null) {
        		dataSet = annotationBean.createDataSet(user.getUserLogin(), dataSetName);
        		PipelineProcess process = getPipelineProcess(tiling);
        		dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, process.toString());
    			entityBean.saveOrUpdateEntity(dataSet);
    			logger.warn("Created new data set: "+dataSet.getName());
        	}
        	
        	dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        }
        
        if (tiling != null && tiling.isStitchable()) {
        	// This is a stitchable case
        	Entity sample = createOrUpdateSample(sampleIdentifier, dataSetIdentifier, sampleChannelSpec, tileGroupList);
        	updateSampleTilingPattern(sample, tiling);
        }
        else {
        	// In non-stitchable cases we just create a Sample for each tile
        	for(ImageTileGroup tileGroup : tileGroupList) {
        		String sampleName = sampleIdentifier+"-"+tileGroup.getTag().replaceAll(" ", "_");
        		List<ImageTileGroup> singleTileGroupList = new ArrayList<ImageTileGroup>();
        		singleTileGroupList.add(tileGroup);
            	Entity sample = createOrUpdateSample(sampleName, dataSetIdentifier, sampleChannelSpec, singleTileGroupList);
            	updateSampleTilingPattern(sample, tiling);
        	}
        }
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
    	if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN)==null && tiling!=null) {
    		logger.info("    Setting properties: pattern="+tiling);
    		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
    		entityBean.saveOrUpdateEntity(sample);
    	}
    }
}
