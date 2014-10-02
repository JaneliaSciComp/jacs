package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClient;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataServerSimpleLocal;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;

/**
 * Created by murphys on 10/2/14.
 */
public class AlignmentCompletionVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentCompletionVisitor.class);

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=(AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        if (sampleInfo!=null) {
            ActiveDataClient activeData = (ActiveDataClient) ActiveDataServerSimpleLocal.getInstance();
            activeData.addEntityEvent(signature, entityId, AlignmentSampleScanner.SAMPLE_INFO, sampleInfo);
        }
        return true;
    }

}
