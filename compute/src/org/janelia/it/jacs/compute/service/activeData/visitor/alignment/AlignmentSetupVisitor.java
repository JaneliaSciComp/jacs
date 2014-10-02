package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Set;

/**
 * Created by murphys on 10/2/14.
 */
public class AlignmentSetupVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentSetupVisitor.class);

    @Override
    public Boolean call() throws Exception {
        AlignmentSampleScanner.SampleInfo sampleInfo=new AlignmentSampleScanner.SampleInfo();
        Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(entityId);

        sampleInfo.id=sampleEntity.getId();
        sampleInfo.owner=sampleEntity.getOwnerKey();
        sampleInfo.lineDescriptor=sampleEntity.getName();

        contextMap.put(AlignmentSampleScanner.SAMPLE_INFO, sampleInfo);

        return true;
    }

}
