package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.janelia.it.jacs.compute.service.domain.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * File discovery service for alignment results. Reads .properties files and updates the discovered files
 * with alignment properties. Also sets the channel specification for any found 3d images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentResultsDiscoveryService extends AbstractDomainService {

	private static DecimalFormat dfScore = new DecimalFormat("0.0000");

    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);

        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();
        
        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);
        contextLogger.info("Collected "+filepaths.size()+" files in "+rootPath);

        Map<String,File> fileMap = new HashMap<>();
        for(String filepath : filepaths) {
            File file = new File(filepath);
            String filename = file.getName();
            if (filename.endsWith(".properties")) {
                fileMap.put(filename, file);
            }
        }
        
        List<Long> alignmentIds = new ArrayList<>(); 
        
        for(String filepath : filepaths) {
            File file = new File(filepath);
            String filename = file.getName();
        
            if (filename.endsWith(".properties")) {
                
                logger.info("Processing alignment result: " + filename);
                Properties properties = new Properties();
                properties.load(new FileReader(file));
                String stackFilename = properties.getProperty("alignment.stack.filename");
                
                File stackFile = fileMap.get(stackFilename);

                if (stackFile==null) {
                    logger.warn("Could not find item with filename: " + stackFilename);
                    continue;
                }
                
                SampleAlignmentResult alignment = sampleHelper.addNewAlignmentResult(run, resultName);
                alignment.setFilepath(rootPath);
                
                logger.info("Created new alignment result: "+alignment.getId());
                alignmentIds.add(alignment.getId());

                // TODO: there should be a better way of determining the area
                if (stackFilename.contains("VNC")) {
                    alignment.setAnatomicalArea("VNC");    
                }
                else {
                    alignment.setAnatomicalArea("Brain");
                }
                
                String verifyFilename = properties.getProperty("alignment.verify.filename");
                if (verifyFilename!=null) {
                    File verifyFile = fileMap.get(verifyFilename);
                    if (verifyFile!=null) {
                        DomainUtils.setFilepath(alignment, FileType.AlignmentVerificationMovie, verifyFile.getAbsolutePath());
                    }
                    else {
                        logger.warn("Result referenced by alignment.verify.filename="+verifyFilename+" does not exist. Cannot continue processing.");
                        continue;
                    }
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

                String neuronMasksFilename = properties.getProperty("neuron.masks.filename");
                if (neuronMasksFilename!=null) {
                    File neuronMasksFile = fileMap.get(neuronMasksFilename);
                    DomainUtils.setFilepath(alignment, FileType.AlignedCondolidatedLabel, neuronMasksFile.getAbsolutePath());
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
                
            	alignment.setChannelSpec(channelSpec);
            	alignment.setAlignmentSpace(alignmentSpace);
            	alignment.setOpticalResolution(opticalRes);
            	alignment.setImageSize(pixelRes);
            	alignment.setBoundingBox(boundingBox);
            	alignment.setObjective(objective);
            	
                // Parse everything else into Doubles to use a consistent decimal format
            	Map<AlignmentScoreType,String> scores = new HashMap<AlignmentScoreType,String>();
            	
				if (!StringUtils.isEmpty(scoreNcc)) {
					String formattedScoreNcc = dfScore.format(Double.parseDouble(scoreNcc));
					scores.put(AlignmentScoreType.NormalizedCrossCorrelation,formattedScoreNcc);
				}

				if (!StringUtils.isEmpty(scoreJbaQm)) { 
					String formattedScoreJbaQm = dfScore.format(Double.parseDouble(scoreJbaQm));
		            scores.put(AlignmentScoreType.ModelViolation,formattedScoreJbaQm);
				}

                if (!StringUtils.isEmpty(overlapCoeff)) {
                    String formattedOverlapCoeff = dfScore.format(Double.parseDouble(overlapCoeff));
                    scores.put(AlignmentScoreType.OverlapCoefficient,formattedOverlapCoeff);
                }

                if (!StringUtils.isEmpty(objectPearsonCoeff)) {
                    String formattedObjectPearsonCoeff = dfScore.format(Double.parseDouble(objectPearsonCoeff));
                    scores.put(AlignmentScoreType.ObjectPearsonCoefficient,formattedObjectPearsonCoeff);
                }

                // Derive all Qi and inconsistency (1-Qi) scores
                processQiScoreCsv(scores, scoresQiCsv);
                if (!scores.isEmpty()) alignment.setScores(scores);
                
            }
        }  
        
        sampleHelper.saveSample(sample);

        contextLogger.info("Putting "+alignmentIds+" in ALIGNMENT_ID");
        data.putItem("ALIGNMENT_ID", alignmentIds);
    }
    
	private void processQiScoreCsv(Map<AlignmentScoreType,String> scores, String scoresQiCsv) throws Exception {

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
        
        scores.put(AlignmentScoreType.Qi,getFormattedWeightedAverage(qiScores));
        scores.put(AlignmentScoreType.QiByRegion,getFormattedCSV(qiScores));
        scores.put(AlignmentScoreType.Inconsistency,getFormattedWeightedAverage(inconsistencyScores));
        scores.put(AlignmentScoreType.InconsistencyByRegion,getFormattedCSV(inconsistencyScores));
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
