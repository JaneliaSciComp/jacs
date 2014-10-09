package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;
import org.janelia.it.jacs.compute.service.activeData.visitor.IdentityEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import java.util.*;

/**
 * Created by murphys on 10/3/14.
 */
public class AlignmentLsmVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentLsmVisitor.class);

    @Override
    public Boolean call() throws Exception {
        Entity sampleEntity = (Entity)contextMap.get(AlignmentSampleScanner.SAMPLE_ENTITY);
        AlignmentSampleScanner.SampleInfo sampleInfo = (AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);
        final Set<Long> visitedSet=new HashSet<>();

        String lineName=null;
        String mountingProtocol=null;

        // Get all LSMs
        final List<Entity> lsmList=new ArrayList<>();
        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(sampleEntity, new EntityVisitor() {
            public void visit(Entity v) throws Exception {
                if (v.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    lsmList.add(v);
                }
            }
        }, visitedSet);
        Collections.sort(lsmList, new Comparator<Entity>() {
            public int compare(Entity a, Entity b) {
                if (a.getId() < b.getId()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        // First look for 20x
        String lsm20xPath=null;
        for (Entity lsm : lsmList) {
            String objectiveString=lsm.getValueByAttributeName("Objective");
            if (objectiveString!=null) {
                if (objectiveString.equals("20x")) {
                    lsm20xPath=lsm.getValueByAttributeName("File Path");
                    lineName=lsm.getValueByAttributeName("Line");
                }
            }
            if (mountingProtocol==null) {
                mountingProtocol=lsm.getValueByAttributeName("Mounting Protocol");
            }
        }

        // Next, find 63x
        for (Entity lsm : lsmList) {
            String objectString=lsm.getValueByAttributeName("Objective");
            if (objectString!=null) {
                if (objectString.equals("63x")) {
                    List<AlignmentSampleScanner.Lsm63xInfo> lsm63xInfoList = sampleInfo.lsm63xInfoList;
                    if (lsm63xInfoList==null) {
                        lsm63xInfoList = new ArrayList<>();
                        sampleInfo.lsm63xInfoList=lsm63xInfoList;
                    }
                    AlignmentSampleScanner.Lsm63xInfo info63x = new AlignmentSampleScanner.Lsm63xInfo();
                    info63x.locationDescription=lsm.getValueByAttributeName("Anatomical Area");
                    info63x.path=lsm.getValueByAttributeName("File Path");
                    if (lineName==null) {
                        lineName=lsm.getValueByAttributeName("Line");
                    }
                    lsm63xInfoList.add(info63x);
                }
            }
        }

        if (lineName!=null) {
            sampleInfo.lineDescriptor=lineName;
        }

        if (mountingProtocol!=null) {
            sampleInfo.preparationType=mountingProtocol;
        }

        return true;
    }

}
