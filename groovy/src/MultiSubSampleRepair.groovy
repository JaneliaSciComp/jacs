import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;

class MultiSubSampleRepairScript {
    
    private static final String OWNER = "nerna"
    private static final String OWNER_KEY = "user:"+OWNER
	private static final boolean WRITE_DATABASE = true
    private static final Long[] sampleIds = [
1889492539698839650L,
1897487654707003399L,
1889492066711371874L,
2105940400685449314L,
2105940736745668706L,
1919112998442500194L,
1914890066660950023L,
1919113035801165922L,
1889492377689653346L,
1903200884729315426L,
1897487114166075399L,
1897487781723111431L,
1889492252019916898L,
1889492583147634786L,
1897487153194074119L,
1897487622616383495L,
1903200888336416866L,
1897487602508890119L,
1903200503941038178L,
1889492143169339490L,
1889492454869041250L,
1897487156637597703L,
1897487341631569927L,
1919112972144214114L,
1897487181312688135L,
1897487753717743623L,
1897487595139497991L,
1903200519552237666L,
1889492060621242466L,
1889492172005179490L,
1897487749586354183L,
1903200896083296354L,
1903200901397479522L,
1889492474796179554L,
1919113084148908130L,
1889492408454873186L,
1903200892954345570L,
1906807685857149026L,
1897487124194656263L,
1897487313387126791L,
1914890076588867591L,
1919113039471181922L,
1889492337298505826L,
1889492413005693026L,
1897487524557750279L,
1897749871704670215L,
1903200907252727906L,
1903200920204738658L,
1889492159158026338L,
1897487106217869319L,
1897487367464288263L,
1897487376368795655L,
1903200483095347298L,
1903200528909729890L,
1914890032733224967L,
1919113077358329954L,
1903200515655729250L,
1903200532206452834L,
1906807681436352610L,
1889492200811659362L,
1897487358471700487L,
1897487547634810887L,
1897487681483440135L,
1889492588218548322L,
1919112995070279778L,
1897487345708433415L,
1919112965752094818L,
1897487134130962439L]
    
    private JacsUtils f
    
    public MultiSubSampleRepairScript() {
        this.f = new JacsUtils(OWNER_KEY, WRITE_DATABASE)
    }
    
    public void run() {
        for(Long sampleId : sampleIds) {
            repairSample(sampleId)
        }
    }
 
    private void repairSample(Long sampleId) {
        
        println "----------------------------------------------------------------------------"
        println "Repairing ${sampleId}"
        
        Entity sample = f.e.getEntityById(null, sampleId)
        f.loadChildren(sample)
        
        List<Entity> subsamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        
        if (subsamples.size()>2) {
            println "Repairing: more than 2 subsamples for ${sampleId}";   
            repairTooManySubsamples(subsamples)
        }
    }
    
    private void repairTooManySubsamples(List<Entity> subsamples) {
        
        def latestObjectiveSamples = [:]
        subsamples.each {
            latestObjectiveSamples[it.getValueByAttributeName("Objective")] = it
        }
        
        subsamples.each {
            if (latestObjectiveSamples.values().contains(it)) {
                // This is the latest, leave it alone
                println "Leaving subsample ${it.name} (${it.id})"
            }
            else {
                forwardAndRemove(it, latestObjectiveSamples[it.getValueByAttributeName("Objective")])
            }
        }
    }
    
    /**
     * Take all the references and annotations on the reject and attempt to move them to the successor. Then delete the reject with extreme prejudice.
     */
    private void forwardAndRemove(Entity reject, Entity successor) {
        
        def numAnnotated = f.a.getNumDescendantsAnnotated(reject.id)
        def parentEds = f.e.getParentEntityDatas(null, reject.id)
        
        if (numAnnotated>0) {
            println "Migrating annotations to ${successor.name} (${successor.id})"
            def annotations = getAnnotations(reject)
            if (annotations.size()<numAnnotated) {
                println "All annotations not found (${annotations.size()} != ${numAnnotated})";
                throw new Exception("Could not find all annotations for "+reject.id)
            }
            else {
                def annot_set = []
                annotations.each {
                    if (!annot_set.contains(it.name)) {
                        annot_set.add(it.name)
                        println "  Migrating annotation "+it.name
                        if (WRITE_DATABASE) {
                            f.e.setOrUpdateValue(it.ownerKey, it.id, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, successor.id.toString());
                        }
                    }
                }
            }
        }

        if (parentEds.size()>1) {
            println "Migrating references to ${successor.name} (${successor.id})"
            for(EntityData ed: parentEds) {
                if (!ed.parentEntity.entityTypeName.equals("Sample")) {
                    println "  Migrating reference from ${ed.parentEntity.name}"
                    if (WRITE_DATABASE) {
                        ed.setChildEntity(successor);
                        f.e.saveOrUpdateEntityData(ed.ownerKey, ed);
                    }
                }
            }
        }
        
        println "Removing reject ${reject.name} (${reject.id})"
        if (WRITE_DATABASE) {
            f.e.deleteEntityTreeById(reject.ownerKey, reject.id, true)
        }
        
    }
    
    private List<Entity> getAnnotations(Entity entity) {
        
        def annotations = []
        def ea = f.a.getAnnotationsForEntity(null, entity.id)
        annotations.addAll(ea)
        
        f.loadChildren(entity)
        entity.getOrderedChildren().each {
            annotations.addAll(getAnnotations(it))
        }
        
        return annotations
    }
    
    
    
}
    
MultiSubSampleRepairScript script = new MultiSubSampleRepairScript()
script.run()
System.exit(0)