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
public class AlignmentResourceVisitor extends ActiveVisitor {


    Logger logger = Logger.getLogger(AlignmentResourceVisitor.class);

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=(AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        if (sampleInfo!=null) {
            ActiveDataClient activeData = (ActiveDataClient) ActiveDataServerSimpleLocal.getInstance();
            // First copy added to event map gets appended to the unified file containing all entries - ultimately this needs to go away and just use the resource tree copy
            activeData.addEntityEvent(signature, entityId, AlignmentSampleScanner.SAMPLE_INFO, sampleInfo);
            // Second copy goes to sample resource file tree, its proper location
            addSampleInfoToAlignmentResource(sampleInfo);
        }
        return true;
    }

    public void addSampleInfoToAlignmentResource(AlignmentSampleScanner.SampleInfo sampleInfo) throws Exception {
        File alignmentResourceDir=new File(AlignmentSampleScanner.ALIGNMENT_RESOURCE_DIR);
        String sampleInfoFilepath=sampleInfo.getResourcePath(alignmentResourceDir.getAbsolutePath());
        File sampleInfoFile=new File(sampleInfoFilepath);
        FileUtil.ensureDirExists(sampleInfoFile.getParentFile().getAbsolutePath());
        logger.info("Writing "+sampleInfoFile.getAbsolutePath());
        BufferedWriter bw=new BufferedWriter(new FileWriter(sampleInfoFile));
        bw.write(sampleInfo.toString());
        bw.close();
    }

}
