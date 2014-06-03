package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import com.google.common.collect.ComparisonChain;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {
    
    private static final Logger log = Logger.getLogger(UpgradeUserDataService.class);
    
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

        createWorkspaceIfNecessary();
    }

    private void createWorkspaceIfNecessary() throws Exception {

    	if (!entityBean.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_WORKSPACE).isEmpty()) {
    		logger.info("User "+ownerKey+" already has at least one workspace");
    		return;
    	}
    	
        List<Entity> roots = annotationBean.getCommonRootEntities(ownerKey);
        Collections.sort(roots, new EntityRootComparator());

        Entity newRoot = entityBean.createEntity(ownerKey, EntityConstants.TYPE_WORKSPACE, EntityConstants.NAME_DEFAULT_WORKSPACE);
        
        int index = 0;
        for(Entity root : roots) {
        	entityBean.addEntityToParent(ownerKey, newRoot.getId(), root.getId(), index++, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        log.info("Created workspace (id="+newRoot.getId()+") for "+ownerKey+" with "+(index+1)+" top-level roots");
    }
    
    public class EntityRootComparator implements Comparator<Entity> {
        
        public int compare(Entity o1, Entity o2) {
            return ComparisonChain.start()
                .compareTrueFirst(o1.getOwnerKey().equals(ownerKey), o2.getOwnerKey().equals(ownerKey))
                .compare(o1.getOwnerKey(), o2.getOwnerKey())
                .compareTrueFirst(EntityUtils.isProtected(o1), EntityUtils.isProtected(o2))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_DATA_SETS), o2.getName().equals(EntityConstants.NAME_DATA_SETS))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SHARED_DATA), o2.getName().equals(EntityConstants.NAME_SHARED_DATA))
                .compare(o1.getId(), o2.getId()).result();
        }
    };
}
