package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

/**
 * File discovery service for alignment results. Reads .properties files and updates the discovered files
 * with alignment properties. Also sets the channel specification for any found 3d images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentResultsDiscoveryService extends SupportingFilesDiscoveryService {

	private static DecimalFormat dfScore = new DecimalFormat("0.0000");
    
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
        
        boolean hasConsensusAlignmentSpace = true;
        String defaultAlignmentSpace = null;
        String consensusAlignmentSpace = null;
        Entity supportingFiles = EntityUtils.getSupportingData(alignmentResult);
        entityLoader.populateChildren(supportingFiles);
        
        Map<String,EntityData> resultItemMap = new HashMap<>();
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
                    
                    logger.info("Got properties file: " + resultItem.getName());
                    File propertiesFile = new File(resultItem.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    
                    Properties properties = new Properties();
                    properties.load(new FileReader(propertiesFile));
                    
                    String stackFilename = properties.getProperty("alignment.stack.filename");
                    
                    EntityData stackEntityEd = resultItemMap.get(stackFilename);
                    Entity stackEntity = stackEntityEd.getChildEntity();
                    
                    if (stackEntity==null) {
                        logger.warn("Could not find result item with filename: " + stackFilename);
                        continue;
                    }

                    String verifyFilename = properties.getProperty("alignment.verify.filename");
                    if (verifyFilename!=null) {
                        EntityData verifyEntityEd = resultItemMap.get(verifyFilename);
                        Entity verifyEntity = verifyEntityEd.getChildEntity();
                        helper.addToParent(stackEntity, verifyEntity, 0, EntityConstants.ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE);
                    }

                    String channels = properties.getProperty("alignment.image.channels");
                    if (channels==null) {
                    	logger.warn("Alignment output does not contain 'alignment.image.channels' property, cannot continue processing.");
                    	continue;
                    }
                    
                    String refchan = properties.getProperty("alignment.image.refchan");
                    if (refchan==null) {
                    	logger.warn("Alignment output does not contain 'alignment.image.refchan' property, cannot continue processing.");
                    	continue;
                    }
                    
                    String alignmentSpace = properties.getProperty("alignment.space.name");
                    String opticalRes = properties.getProperty("alignment.resolution.voxels");
                    String pixelRes = properties.getProperty("alignment.image.size");
                    String boundingBox = properties.getProperty("alignment.bounding.box");
                    String objective = properties.getProperty("alignment.objective");
                    String scoreNcc = properties.getProperty("alignment.quality.score.ncc");
                    String scoreJbaQm = properties.getProperty("alignment.quality.score.jbaqm");
                    String scoresQiCsv = properties.getProperty("alignment.quality.score.qi"); // The three comma-delimited scores from QiScore.csv 
                    String overlapCoeff  = properties.getProperty("alignment.overlap.coefficient");
                    String objectPearsonCoeff = properties.getProperty("alignment.object.pearson.coefficient");

                    String channelSpec;
                	int numChannels = Integer.parseInt(channels);
                	int refChannel = Integer.parseInt(refchan);
                	channelSpec = ChanSpecUtils.createChanSpec(numChannels, refChannel);
                    
                    helper.setChannelSpec(stackEntity, channelSpec);
                    helper.setAlignmentSpace(stackEntity, alignmentSpace);
                    helper.setOpticalResolution(stackEntity, opticalRes);
                    helper.setPixelResolution(stackEntity, pixelRes);
                    helper.setBoundingBox(stackEntity, boundingBox);
                    helper.setObjective(stackEntity, objective);

                    // Parse everything else into Doubles to use a consistent decimal format

    				if (!StringUtils.isEmpty(scoreNcc)) {
    					String formattedScoreNcc = dfScore.format(Double.parseDouble(scoreNcc));
    					helper.setNccScore(stackEntity, formattedScoreNcc);
    				}

    				if (!StringUtils.isEmpty(scoreJbaQm)) { 
    					String formattedScoreJbaQm = dfScore.format(Double.parseDouble(scoreJbaQm));
    					helper.setModelViolationScore(stackEntity, formattedScoreJbaQm);
    				}

                    if (!StringUtils.isEmpty(overlapCoeff)) {
                        String formattedOverlapCoeff = dfScore.format(Double.parseDouble(overlapCoeff));
                        helper.setOverlapCoeff(stackEntity, formattedOverlapCoeff);
                    }

                    if (!StringUtils.isEmpty(objectPearsonCoeff)) {
                        String formattedObjectPearsonCoeff = dfScore.format(Double.parseDouble(objectPearsonCoeff));
                        helper.setObjectPearsonCoeff(stackEntity, formattedObjectPearsonCoeff);
                    }

                    // Derive all Qi and inconsistency (1-Qi) scores
                    processQiScoreCsv(stackEntity, scoresQiCsv);
                    
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
        }   
        
        if (hasConsensusAlignmentSpace && consensusAlignmentSpace!=null) {
            helper.setAlignmentSpace(alignmentResult, consensusAlignmentSpace);
        }
        else {
            logger.info("No consensus for alignment space, using default: "+defaultAlignmentSpace);
            helper.setAlignmentSpace(alignmentResult, defaultAlignmentSpace);
        }
        
        logger.info("Putting "+hasWarpedSeparation+" in PREWARPED_SEPARATION");
        processData.putItem("PREWARPED_SEPARATION", new Boolean(hasWarpedSeparation));
    }
    
	private void processQiScoreCsv(Entity alignedImage, String scoresQiCsv) throws Exception {

    	if (StringUtils.isEmpty(scoresQiCsv)) return;
    		
    	List<Double> qiScores = new ArrayList<>();
    	List<Double> inconsistencyScores = new ArrayList<>();
        for(String scoreQi : Task.listOfStringsFromCsvString(scoresQiCsv)) {
            try {
                Double d_scoresQi = Double.parseDouble(scoreQi);
                qiScores.add(d_scoresQi);
                inconsistencyScores.add(1-d_scoresQi);
            }
            catch (NumberFormatException e) {
                logger.error("Error parsing double: "+e);
            }
        }

        helper.setQiScore(alignedImage, getFormattedWeightedAverage(qiScores));
        helper.setQiScores(alignedImage, getFormattedCSV(qiScores));
        helper.setInconsistencyScore(alignedImage, getFormattedWeightedAverage(inconsistencyScores));
        helper.setInconsistencyScores(alignedImage, getFormattedCSV(inconsistencyScores));
    }
    
    /**
     * Format the given doubles with the default format and create a comma-separated list with the formatted values.
     * @param scores list of doubles
     * @return formatted csv string
     */
    private String getFormattedCSV(List<Double> scores) {
    	StringBuilder sb = new StringBuilder();
    	for(Double score : scores) {
    		if (sb.length()>0) sb.append(",");
    		sb.append(dfScore.format(score));
    	}
    	return sb.toString();
    }

    /**
     * @see AlignmentResultsDiscoveryService#getJBAWeightedAverage(double, double, double)
     * @param scores Three individual Qi or Inconsistency (1-Qi) scores
     * @return Combined Qi
     */
    private String getFormattedWeightedAverage(List<Double> scores) {
    	return dfScore.format(getJBAWeightedAverage(scores));
    }
    
    /**
     * @see AlignmentResultsDiscoveryService#getJBAWeightedAverage(double, double, double)
     * @param scores Three individual Qi or Inconsistency (1-Qi) scores
     * @return Combined Qi
     */
    private Double getJBAWeightedAverage(List<Double> scores) {
    	if (scores.size()!=3) {
    		logger.info("Expected three scores for computing weighted average, but got "+scores.size());
    		return null;
    	}
    	return getJBAWeightedAverage(scores.get(0), scores.get(1), scores.get(2));
    }
    
    /**
     * Qi is the percentage of landmarks that are matched. Qi scores range from 0 to 1, with 1 being the best possible score in that all landmarks were matched. 
     * Note that JBA currently provides three Qi scores. The landmark matches yielded by a run of JBA are subdivided into three areas:
     * <ol>
     * <li>Left optic lobe (144 possible landmarks)</li>
     * <li>Central brain (231 possible landmarks)</li>
     * <li>Right optic lobe (125 possible landmarks)</li>
     * </ol>
     * 
     * Each area will have its own Qi score. These three scores are combined to provide a Qi for the whole brain using the following formula:
     * 
     *     Q = Qi(Left optic lobe) * 0.288 + Qi(Central brain) * 0.462 + Qi(Right optic lobe) * 0.25
     * 
     * Note that this method works for either Qi or Inconsistency (1-Qi) scores, since the constant weights sum to 1.
     *  
     * @param s1 Score for left optic lobe
     * @param s2 Score for central brain
     * @param s3 Score for right optic lobe
     * @return Combined Qi as calculated by the above formula
     */
    private double getJBAWeightedAverage(double s1, double s2, double s3) {
    	return s1 * 0.288 + s2 * 0.462 + s3 * 0.25;
    }
}
