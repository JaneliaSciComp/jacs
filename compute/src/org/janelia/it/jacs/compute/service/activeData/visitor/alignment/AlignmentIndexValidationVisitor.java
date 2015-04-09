package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClient;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataServerSimpleLocal;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by murphys on 1/7/15.
 */
public class AlignmentIndexValidationVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentIndexValidationVisitor.class);

    public static final String SAMPLE_INDEX_VALIDATION = "SAMPLE_INDEX_VALIDATION";

    public static int validationCount=0;

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=(AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        if (sampleInfo!=null) {
            Boolean validAlignmentSpace = false;
            List<AlignmentSampleScanner.AlignmentResult> arList = sampleInfo.alignmentResultList;
            if (arList!=null) {
                for (AlignmentSampleScanner.AlignmentResult ar : sampleInfo.alignmentResultList) {
                    if (ar!=null && ar.spaceDescriptor!=null && ar.spaceDescriptor.equals("Yoshi 63x Subsampled Alignment Space")) {
                        validAlignmentSpace = true;
                        logger.info("AlignmentValidationVisitor: incrementing validationCount to "+validationCount);
                        validationCount++;
                    }
                }
                contextMap.put(SAMPLE_INDEX_VALIDATION, validAlignmentSpace);
            }
        }
        return true;
    }

}
