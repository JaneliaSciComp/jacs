package org.janelia.it.jacs.compute.service.domain.sample;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * Find the latest sample processing entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseSampleProcessingResultService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        
        SampleProcessingResult latestSp = null;
        
        for(SamplePipelineRun pipelineRun : objectiveSample.getPipelineRuns()) {
            for(SampleProcessingResult sp : pipelineRun.getSampleProcessingResults()) {
                String spArea = sp.getAnatomicalArea();
                if (spArea==null) spArea = "";
                if (sampleArea!=null && !sampleArea.getName().equals(spArea)) {
                    contextLogger.debug("Can't use "+sp.getId()+" because "+sampleArea.getName()+"!="+spArea);
                    continue;
                }
                latestSp = sp;
            }
        }
        
        if (latestSp!=null) {
            String stitchedFilepath = DomainUtils.getDefault3dImageFilePath(latestSp);
            if (stitchedFilepath!=null) {
                sampleArea.setStitchedFilepath(stitchedFilepath);
                run.addResult(latestSp);
                
                contextLogger.info("Reusing sample processing result "+latestSp.getId()+" for "+sampleArea.getName()+" area in new pipeline run "+run.getId());
                
                processData.putItem("RESULT_ENTITY_ID", latestSp.getId().toString());
                contextLogger.info("Putting '"+latestSp.getId()+"' in RESULT_ENTITY_ID");
                
                processData.putItem("RUN_PROCESSING", Boolean.FALSE);    
                contextLogger.info("Putting '"+Boolean.FALSE+"' in RUN_PROCESSING");
            }
            else {
                contextLogger.warn("Sample processing result has no default 3d image path: "+latestSp.getId());
            }
        }
        else {
            contextLogger.info("No existing sample processing available for reuse for sample: "+sample.getId());
        }
        
        sampleHelper.saveSample(sample);
    }
}
