package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * File discovery service for alignment results. Reads .properties files and updates the discovered files
 * with alignment properties. Also sets the channel specification for any found 3d images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentResultsDiscoveryService extends SupportingFilesDiscoveryService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_ALIGNMENT_RESULT);
        super.execute(processData);
    }

    @Override
    protected void processFolderForData(Entity alignmentResult) throws Exception {

        if (!alignmentResult.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
            throw new IllegalStateException("Expected Alignment Result as input");
        }

        super.processFolderForData(alignmentResult);

        String channelColors = (String)processData.getItem("CHANNEL_COLORS");
        
        String channelSpec = (String)processData.getItem("CHANNEL_SPEC");
        if (StringUtils.isEmpty(channelSpec)) {
            throw new IllegalArgumentException("CHANNEL_SPEC may not be null");
        }
        
        boolean hasConsensusAlignmentSpace = true;
        String defaultAlignmentSpace = null;
        String consensusAlignmentSpace = null;
        Entity supportingFiles = EntityUtils.getSupportingData(alignmentResult);
        entityLoader.populateChildren(supportingFiles);
        
        Map<String,EntityData> resultItemMap = new HashMap<String,EntityData>();
        for(EntityData resultItemEd : supportingFiles.getEntityData()) {
            Entity resultItem = resultItemEd.getChildEntity();
            if (resultItem!=null) {
                resultItemMap.put(resultItem.getName(), resultItemEd);
            }
        }
        
        boolean hasWarpedSeparation = false;
        
        for(Entity resultItem : supportingFiles.getChildren()) {
            
            if (resultItem.getEntityType().getName().equals(EntityConstants.TYPE_TEXT_FILE)) {
                logger.info("Got text file: "+resultItem.getName());    
                if (resultItem.getName().endsWith(".properties")) {
                    
                    logger.info("Got properties file: "+resultItem.getName());
                    File propertiesFile = new File(resultItem.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    Properties properties = new Properties();
                    properties.load(new FileReader(propertiesFile));
                    
                    String filename = properties.getProperty("alignment.stack.filename");
                    EntityData entityEd = resultItemMap.get(filename);
                    Entity entity = entityEd.getChildEntity();
                    
                    if (entity==null) {
                        logger.warn("Could not find result item with filename: "+filename);
                        continue;
                    }
                    
                    String alignmentSpace = properties.getProperty("alignment.space.name");
                    String opticalRes = properties.getProperty("alignment.resolution.voxels");
                    String pixelRes = properties.getProperty("alignment.image.size");
                    String boundingBox = properties.getProperty("alignment.bounding.box");
                    String objective = properties.getProperty("alignment.objective");
                    
                    helper.setAlignmentSpace(entity, alignmentSpace);
                    helper.setOpticalResolution(entity, opticalRes);
                    helper.setPixelResolution(entity, pixelRes);
                    helper.setBoundingBox(entity, boundingBox);
                    helper.setObjective(entity, objective);
                    
                    if ("true".equals(properties.getProperty("default"))) {
                        defaultAlignmentSpace = alignmentSpace;
                    }
                    
                    if (consensusAlignmentSpace==null) {
                        consensusAlignmentSpace = alignmentSpace;
                    }
                    else if (!consensusAlignmentSpace.equals(alignmentSpace)) {
                        hasConsensusAlignmentSpace = false;
                    }
                    
                    String neuronMasksFilename = properties.getProperty("neuron.masks.filename");
                    if (neuronMasksFilename!=null) {
                        EntityData alignedNeuronMaskEd = resultItemMap.get(neuronMasksFilename);
                        Entity alignedNeuronMask = alignedNeuronMaskEd.getChildEntity();
                        helper.addImage(entity, EntityConstants.ATTRIBUTE_ALIGNED_CONSOLIDATED_LABEL, alignedNeuronMask);   
                        supportingFiles.getEntityData().remove(alignedNeuronMaskEd);
                        entityBean.deleteEntityData(alignedNeuronMaskEd);
                        hasWarpedSeparation = true;
                    }
                }
            }
            else if (resultItem.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                logger.info("Setting channel specification for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelSpec);
                helper.setChannelSpec(resultItem, channelSpec);
                if (!StringUtils.isEmpty(channelColors)) {
                    logger.info("Setting channel colors for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelColors);
                    helper.setChannelSpec(resultItem, channelColors);
                }
            }
        }   
        
        if (hasConsensusAlignmentSpace && consensusAlignmentSpace!=null) {
            helper.setAlignmentSpace(alignmentResult, consensusAlignmentSpace);
        }
        else {
            logger.warn("No consensus for alignment space, using default: "+defaultAlignmentSpace);
            helper.setAlignmentSpace(alignmentResult, defaultAlignmentSpace);
        }
        
        logger.info("Putting "+hasWarpedSeparation+" in PREWARPED_SEPARATION");
        processData.putItem("PREWARPED_SEPARATION", new Boolean(hasWarpedSeparation));
    }
}
