package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;

import javax.inject.Named;
import java.util.concurrent.CompletionStage;

@Named("flylightSamplePipelineService")
public class FlylightSamplePipelineComputation extends AbstractServiceComputation {

    @Override
    public CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        // TODO
        return null;
    }
}
