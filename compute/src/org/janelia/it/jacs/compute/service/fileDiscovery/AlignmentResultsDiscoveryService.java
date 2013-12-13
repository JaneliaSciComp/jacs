package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * File discovery service for alignment results. Reads .properties files and updates the discovered files
 * with alignment properties. Also sets the channel specification for any found 3d images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentResultsDiscoveryService extends SupportingFilesDiscoveryService {
    
    private static DecimalFormat dfScore = new DecimalFormat("#.######");
    
    @Override
    public void execute(IProcessData processData) throws ServiceException {
        processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_ALIGNMENT_RESULT);
        super.execute(processData);
    }

    @Override
    protected void processFolderForData(Entity alignmentResult) throws Exception {

        if (!alignmentResult.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
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
            
            if (resultItem.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE)) {
                logger.info("Got text file: "+resultItem.getName());    
                if (resultItem.getName().endsWith(".properties")) {
                    
                    logger.info("Got properties file: "+resultItem.getName());
                    File propertiesFile = new File(resultItem.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    Properties properties = new Properties();
                    properties.load(new FileReader(propertiesFile));
                    
                    String stackFilename = properties.getProperty("alignment.stack.filename");
                    
                    EntityData stackEntityEd = resultItemMap.get(stackFilename);
                    Entity stackEntity = stackEntityEd.getChildEntity();
                    
                    if (stackEntity==null) {
                        logger.warn("Could not find result item with filename: "+stackFilename);
                        continue;
                    }

                    String verifyFilename = properties.getProperty("alignment.verify.filename");
                    if (verifyFilename!=null) {
                        EntityData verifyEntityEd = resultItemMap.get(verifyFilename);
                        Entity verifyEntity = verifyEntityEd.getChildEntity();
                        helper.addToParent(stackEntity, verifyEntity, 0, EntityConstants.ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE);
                    }
                    
                    String alignmentSpace = properties.getProperty("alignment.space.name");
                    String opticalRes = properties.getProperty("alignment.resolution.voxels");
                    String pixelRes = properties.getProperty("alignment.image.size");
                    String boundingBox = properties.getProperty("alignment.bounding.box");
                    String objective = properties.getProperty("alignment.objective");
                    String scoreNcc = properties.getProperty("alignment.quality.score.ncc");
                    String scoreQi = properties.getProperty("alignment.quality.score.qi");
                    
                    String score1MinusQi = null;
                    if (!StringUtils.isEmpty(scoreQi)) {
                        List<String> inconsistencyScores = new ArrayList<String>();
                        for(String qiScore : Task.listOfStringsFromCsvString(scoreQi)) {
                            try {
                                double score = 1 - Double.parseDouble(qiScore);
                                inconsistencyScores.add(dfScore.format(score));
                            }
                            catch (NumberFormatException e) {
                                logger.error("Error parsing double: "+e);
                            }
                        }
                        score1MinusQi = Task.csvStringFromCollection(inconsistencyScores);
                    }
                    
                    
                    helper.setAlignmentSpace(stackEntity, alignmentSpace);
                    helper.setOpticalResolution(stackEntity, opticalRes);
                    helper.setPixelResolution(stackEntity, pixelRes);
                    helper.setBoundingBox(stackEntity, boundingBox);
                    helper.setObjective(stackEntity, objective);
                    helper.setNccScore(stackEntity, scoreNcc);
                    helper.setQiScore(stackEntity, score1MinusQi);
                    
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
                        helper.addImage(stackEntity, EntityConstants.ATTRIBUTE_ALIGNED_CONSOLIDATED_LABEL, alignedNeuronMask);   
                        entityBean.deleteEntityData(alignedNeuronMaskEd);
                        hasWarpedSeparation = true;
                    }
                }
            }
            else if (resultItem.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                logger.info("Setting channel specification for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelSpec);
                helper.setChannelSpec(resultItem, channelSpec);
                if (!StringUtils.isEmpty(channelColors)) {
                    logger.info("Setting channel colors for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelColors);
                    helper.setChannelColors(resultItem, channelColors);
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
