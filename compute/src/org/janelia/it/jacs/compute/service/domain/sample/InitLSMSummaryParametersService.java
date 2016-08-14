package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;

/**
 * Extracts stuff about the LSMs from the entity model and loads it into
 * simplified objects for use by LSM summary services.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitLSMSummaryParametersService extends InitSampleProcessingParametersService {

	private Sample sample;
	private ObjectiveSample objectiveSample;

	public void execute() throws Exception {

	    SampleHelperNG sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);

        contextLogger.info("Running InitLSMSummaryParametersService for sample " + sample.getName());

        // Create dummy area to hold all the LSM information
        AnatomicalArea sampleArea = new AnatomicalArea(sample.getId(), objectiveSample.getObjective(), "");
		
		List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();
		populateMergedLsmPairs(objectiveSample.getTiles(), mergedLsmPairs);
	    sampleArea.setMergedLsmPairs(mergedLsmPairs);

        contextLogger.info("Putting " + sampleArea + " into SAMPLE_AREA");
        processData.putItem("SAMPLE_AREA", sampleArea);
	}

}
