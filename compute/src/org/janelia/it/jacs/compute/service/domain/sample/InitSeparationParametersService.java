package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasImageStack;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

import com.google.common.collect.Lists;

/**
 * Extracts metadata from the entity model to be used for the neuron separator. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSeparationParametersService extends AbstractDomainService {

    protected static final String ARCHIVE_PREFIX = "/archive";

    private Sample sample;
    private ObjectiveSample objectiveSample;
    private PipelineResult result;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        Long resultId = data.getRequiredItemAsLong("ROOT_ENTITY_ID");
        List<PipelineResult> results = run.getResultsById(null, resultId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Could not find result "+resultId+" in sample "+sample.getId());
        }
        this.result = results.get(0); // We can take any instance, since they're all the same
        
        if (result instanceof HasImageStack) {
            HasImageStack hasImageStack = (HasImageStack)result;
            String chanSpec = hasImageStack.getChannelSpec();
            final String signalChannels = ChanSpecUtils.getSignalChannelIndexes(chanSpec);
            if (signalChannels!=null) {
                contextLogger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
                processData.putItem("SIGNAL_CHANNELS", signalChannels);
            }        
            final String referenceChannels = ChanSpecUtils.getReferenceChannelIndexes(chanSpec);
            if (referenceChannels!=null) {
                contextLogger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
                processData.putItem("REFERENCE_CHANNEL", referenceChannels);
            }        
        }
        else {
            throw new IllegalStateException("Result cannot be separated because it does not implement the HasImageStack interface");
        }
        
        String inputFilename = DomainUtils.getDefault3dImageFilePath(result);
        if (inputFilename==null) {
            throw new IllegalStateException("Result has no default 3d image: "+result.getId()+" in sample "+sample.getId());
        }
        data.putItem("INPUT_FILENAME", inputFilename);
        
        if (result instanceof SampleAlignmentResult) {
            SampleAlignmentResult alignment = (SampleAlignmentResult)result;
            String alignedConsolidatedLabelFilepath = DomainUtils.getFilepath(alignment, FileType.AlignedCondolidatedLabel);
            if (alignedConsolidatedLabelFilepath!=null) {
                data.putItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH", alignedConsolidatedLabelFilepath);
            }
            else {
                throw new IllegalStateException("Separation of aligned stack without warped neurons is currently prohibited");
            }
        }

        NeuronSeparation prevResult = findPrevResult();
        if (prevResult!=null) {
            String previousResultFilename = DomainUtils.getFilepath(prevResult, FileType.NeuronSeparatorResult);
            if (previousResultFilename!=null) {
                data.putItem("PREVIOUS_RESULT_FILENAME", previousResultFilename);
            }
        }
    }
    
    protected NeuronSeparation findPrevResult() throws Exception {

        NeuronSeparation prevSeparation = result.getLatestSeparationResult();
        if (prevSeparation != null) {
            logger.info("Found previous separation in the current result entity");
            return prevSeparation;
        }
        else {
            logger.info("Checking sample for previous separations: "+sample.getId());
            
            for(SamplePipelineRun run : Lists.reverse(objectiveSample.getPipelineRuns())) {
                
                PipelineResult lastResult = null;
                for(PipelineResult prevResult : run.getResults()) {
                    // Find another result with the same name but different id
                    if (prevResult.getName().equals(result.getName()) && !prevResult.getId().equals(result.getId())) {
                        lastResult = prevResult;
                        break;
                    }
                }
                
                logger.info("Check pipeline run "+run.getId()+" resultFound?="+(lastResult!=null));                    
                if (lastResult!=null) {
                    return lastResult.getLatestSeparationResult();
                }
            }
        }
        
        return null;
    }
}
