package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader

import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_SAMPLE

boolean DEBUG = false

final JacsUtils f = new JacsUtils(null, false)

Set<String> subjectKeys = new HashSet<String>();
for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
    subjectKeys.add(dataSet.getOwnerKey());
}
//subjectKeys.add("user:asoy,")
//subjectKeys.add("group:flylight")
//subjectKeys.add("user:korffw")
//subjectKeys.add("group:leetlab")
//subjectKeys.add("user:wolfft")
//subjectKeys.add("user:nerna")
//subjectKeys.add("user:taes")
//subjectKeys.add("group:heberleinlab")

println "Found users with data sets: "+subjectKeys
for(String subjectKey : subjectKeys) {
    println "Processing "+subjectKey;

    final EJBEntityLoader entityLoader = new EJBEntityLoader(f.e)
    for(Entity sample : f.e.getUserEntitiesByTypeName(subjectKey, TYPE_SAMPLE)) {
    
        cf = new ConsensusFinder()
        cf.findConsensus(sample, entityLoader)
        final String opticalRes = cf.consensus
        println sample.name+" "+opticalRes
        if (opticalRes!=null) {
            EntityVistationBuilder.create(entityLoader).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                    .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                    .run(new EntityVisitor() {
                public void visit(Entity result) throws Exception {
                    entityLoader.populateChildren(result)
                    Entity default3d = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
                    if (default3d != null) {
                        if (default3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)==null) {
                            default3d.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes)
                            if (!DEBUG) {
                                f.e.saveOrUpdateEntity(default3d.ownerKey, default3d)
                            }
                        }
                    }
                }
            });
        }
        
        sample.setEntityData(null)
    }
    
    println "Done "+subjectKey
}


class ConsensusFinder {

    String consensus;
    public void findConsensus(sample, entityLoader) {
            EntityVistationBuilder.create(entityLoader).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                    .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                    .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                    .run(new EntityVisitor() {
                public void visit(Entity lsm) throws Exception {
                    String opticalRes = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
                    if (consensus==null) {
                        if (opticalRes!=null) {
                            consensus = opticalRes
                        }
                    }
                    else if (!consensus.equals(opticalRes)) {
                        println "No consensus for optical res: "+sample.name
                    }
                }
            });
    }
}