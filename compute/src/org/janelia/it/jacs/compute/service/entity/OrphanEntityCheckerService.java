package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Find annotations with missing targets or ontology terms.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OrphanEntityCheckerService extends AbstractDomainService {

    public transient static final String PARAM_deleteOrphanEntityTrees = "remove orphan entity trees";
    
    private boolean deleteOrphanEntityTrees = false;
    
    public void execute() throws Exception {

        deleteOrphanEntityTrees = Boolean.parseBoolean(task.getParameter(PARAM_deleteOrphanEntityTrees));

        logger.info("Finding orphan entities for "+ownerKey);
        logger.info("    deleteOrphanEntityTrees="+deleteOrphanEntityTrees);
    
        List<Long> entityIds = entityBean.getOrphanEntityIds(ownerKey);
        logger.info("Found "+entityIds.size()+" orphan entities");
        
        int numDeleted = 0;
        
        if (deleteOrphanEntityTrees) {
            for(Long id : entityIds) {
                entityBean.deleteEntityTreeById(ownerKey, id);
                numDeleted++;
            }
        }
        else {
            logger.info("Orphan entities:");
            for(Entity entity : entityBean.getEntitiesById(entityIds)) {
                logger.info(entity.getEntityTypeName()+" - "+entity.getName()+" (id="+entity.getId()+")");
            }
        }
        
        logger.info("Done with orphan entity deletion. Deleted "+numDeleted+" entity trees.");
    }
}
