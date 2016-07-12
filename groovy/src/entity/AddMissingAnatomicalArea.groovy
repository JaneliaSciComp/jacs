package entity

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_SAMPLE

boolean DEBUG = false

final JacsUtils f = new JacsUtils(null, false)

Set<String> subjectKeys = new HashSet<String>();
for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
    subjectKeys.add(dataSet.getOwnerKey());
}
//subjectKeys.add("user:asoy")
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

        cf = new AreaConsensusFinder()
        cf.findConsensus(sample, entityLoader)
        final String anatomicalArea = cf.consensus
        if (anatomicalArea==null) anatomicalArea = "";
        println sample.name+" "+anatomicalArea
        
        EntityVistationBuilder.create(entityLoader).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                .run(new EntityVisitor() {
            public void visit(Entity result) throws Exception {
                setArea(result);
            }

            private void setArea(Entity entity) {
                String currArea = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if (currArea==null) {
                    entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, anatomicalArea)
                    println "  "+entity.id+" now has area: "+anatomicalArea;
                    if (!DEBUG) {
                        f.e.saveOrUpdateEntity(entity.ownerKey, entity)
                    }
                }
                else {
                    println "  "+entity.id+" already had area: "+currArea;
                }
            }
        });
        
        sample.setEntityData(null)
    }
    
    println "Done "+subjectKey
}


class AreaConsensusFinder {

    String consensus;
    public void findConsensus(sample, entityLoader) {
            EntityVistationBuilder.create(entityLoader).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                    .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                    .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                    .run(new EntityVisitor() {
                public void visit(Entity lsm) throws Exception {
                    String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
                    if (consensus==null) {
                        if (area!=null) {
                            consensus = area
                        }
                    }
                    else if (!consensus.equals(area)) {
                        println "No consensus for area: "+sample.name
                    }
                }
            });
    }
}