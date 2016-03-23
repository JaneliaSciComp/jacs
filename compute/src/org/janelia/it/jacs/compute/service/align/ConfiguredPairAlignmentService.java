package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.entity.cv.Objective;

/**
 * A configured aligner which takes additional parameters to align the 63x image to a whole brain 20x image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredPairAlignmentService extends ConfiguredAlignmentService {
        
    @Override
    protected void populateInputs(List<AnatomicalArea> sampleAreas) throws Exception {
        
    	alignedAreas.addAll(sampleAreas);

    	// Ignore sample areas, and get the sample pair (20x/63x)

        contextLogger.info("initParameters: found " + sample.getObjectiveSamples().size() + " objective samples for entity " + sample);

        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            
            String objective = objectiveSample.getObjective();
            if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                contextLogger.info("Found 20x objective for: "+sample.getName());
                SampleProcessingResult result = getLatestResultOfType(objectiveSample, BRAIN_AREA);
                if (result==null) {
                    // In some cases there is no "Brain" area, let's try to find anything we can use
                    result = getLatestResultOfType(objectiveSample, null);
                }
                input2 = buildInputFromResult("second input (20x stack)", result, objectiveSample, objective);
            }
            else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                contextLogger.info("Found 63x objective for: "+sample.getName());
                SampleProcessingResult result = getLatestResultOfType(objectiveSample, BRAIN_AREA);
                if (result==null) {
                    // In some cases there is no "Brain" area, let's try to find anything we can use
                    result = getLatestResultOfType(objectiveSample, null);
                }
                input1 = buildInputFromResult("first input (63x stack)", result, objectiveSample, objective);
            }
        }

        if (input1==null || input2==null) {
        	runAligner = false;
        }
    }

    @Override
    protected void setLegacyConsensusValues() throws Exception {
    	// Do nothing, since we build our consensus values while populating the inputs
    }

    private AlignmentInputFile buildInputFromResult(String inputType, SampleProcessingResult result, ObjectiveSample objectiveSample, String objective) throws Exception {

        if (result==null) return null;
        AlignmentInputFile inputFile = new AlignmentInputFile();
        inputFile.setPropertiesFromEntity(result);
        inputFile.setSampleId(sample.getId());
        inputFile.setObjective(objective);        
        if (warpNeurons) {
            inputFile.setInputSeparationFilename(getConsolidatedLabel(result));
        }
        logInputFound(inputType, inputFile);
        
        return inputFile;
    }
}
