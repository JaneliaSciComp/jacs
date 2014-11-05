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

/**
 * Created by murphys on 10/2/14.
 */
public class AlignmentCompletionVisitor extends ActiveVisitor {

    public static final String SAMPLE_INFO_FILENAME = "sampleInfo.txt";

    Logger logger = Logger.getLogger(AlignmentCompletionVisitor.class);

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=(AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        if (sampleInfo!=null) {
            ActiveDataClient activeData = (ActiveDataClient) ActiveDataServerSimpleLocal.getInstance();
            activeData.addEntityEvent(signature, entityId, AlignmentSampleScanner.SAMPLE_INFO, sampleInfo);
            addSampleInfoToAlignmentResource(sampleInfo);
        }
        return true;
    }

    public void addSampleInfoToAlignmentResource(AlignmentSampleScanner.SampleInfo sampleInfo) throws Exception {
        File alignmentResourceDir=new File(AlignmentSampleScanner.ALIGNMENT_RESOURCE_DIR);
        String sampleOffsetPath= FileNode.getTreePathForId(sampleInfo.id);
        File sampleDir=new File(alignmentResourceDir.getAbsoluteFile()+"/"+"samples"+"/"+sampleOffsetPath);
        FileUtil.ensureDirExists(sampleDir.getAbsolutePath());
        File sampleInfoFile=new File(sampleDir, SAMPLE_INFO_FILENAME);
        logger.info("Writing "+sampleInfoFile.getAbsolutePath());
        BufferedWriter bw=new BufferedWriter(new FileWriter(sampleInfoFile));
        bw.write(sampleInfo.toString());
        bw.close();
    }

}
