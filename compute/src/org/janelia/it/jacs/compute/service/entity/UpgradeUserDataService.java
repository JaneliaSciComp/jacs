package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
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

        createWorkspaceType();
        createWorkspaceIfNecessary();
        groupSearchResultsIfNecessary();
    }

    private void createWorkspaceType() throws Exception {
    	
    	if (entityBean.getEntityTypeByName(EntityConstants.TYPE_WORKSPACE)!=null) {
    		return;
    	}
    	
		logger.info("Creating Workspace entity type.");
		entityBean.createNewEntityType(EntityConstants.TYPE_WORKSPACE);
		entityBean.createNewEntityAttr(EntityConstants.TYPE_WORKSPACE, EntityConstants.ATTRIBUTE_ENTITY);
    }
    
    private void createWorkspaceIfNecessary() throws Exception {

    	for(Entity workspace : entityBean.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_WORKSPACE)) {
    		if (workspace.getOwnerKey().equals(ownerKey)) {
        		logger.info("User "+ownerKey+" already has at least one workspace, skipping creation step.");
        		return;
    		}
    	}
    	
        List<Entity> roots = annotationBean.getCommonRootEntities(ownerKey);
        Collections.sort(roots, new EntityRootComparator());

        Entity newRoot = entityBean.createEntity(ownerKey, EntityConstants.TYPE_WORKSPACE, EntityConstants.NAME_DEFAULT_WORKSPACE);
        
        int index = 0;
        for(Entity root : roots) {
        	entityBean.addEntityToParent(newRoot, root, index++, EntityConstants.ATTRIBUTE_ENTITY, false);
        }
        
        log.info("Created workspace (id="+newRoot.getId()+") for "+ownerKey+" with "+(index+1)+" top-level roots");
    }

    private void groupSearchResultsIfNecessary() throws Exception {

    	if (ownerKey.startsWith("group:")) return;
    	
    	Entity workspace = entityBean.getDefaultWorkspace(ownerKey);
        populateChildren(workspace);
        
        int count = 0;
    	for(EntityData ed : workspace.getOrderedEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child==null) continue;
    		if (child.getName().startsWith("Search Results #")) {
    			count++;
    		}
    	}
    	
    	if (count<1) {
    		log.info("No top-level search result folders found for "+ownerKey+", skipping grouping step.");
    		return;
    	}
    	
        Entity topLevelFolder = entityHelper.createOrVerifyRootEntity(EntityConstants.NAME_SEARCH_RESULTS, true, false);
        if (topLevelFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(topLevelFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            entityBean.saveOrUpdateEntity(topLevelFolder);
        }
        
        populateChildren(topLevelFolder);
        
        if (!topLevelFolder.getChildren().isEmpty()) {
        	log.info("User "+ownerKey+"'s search results folder already has children, skipping grouping step.");
        	return;
        }
        
        List<EntityData> toMove = new ArrayList<EntityData>();
        
    	for(EntityData ed : workspace.getOrderedEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child==null) continue;
    		if (child.getName().startsWith("Search Results #")) {
    			
    			// The search result folders are no longer common roots
    			EntityData commonRootEd = child.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
    			if (commonRootEd!=null) {
    				child.getEntityData().remove(commonRootEd);
    				entityBean.deleteEntityData(commonRootEd);
    			}
    			
    			// Remove them from the workspace as well, and prepare to add them to the search results folder
    			workspace.getEntityData().remove(ed);
    			toMove.add(ed);
    		}
    	}
    	
    	log.info("Moving "+toMove.size()+" result folders into search results folder");
    	
    	// Move search result folders into 
    	int index = 0;
    	for(EntityData ed : toMove) {
    		ed.setOrderIndex(index++);
			ed.setParentEntity(topLevelFolder);
    		topLevelFolder.getEntityData().add(ed);
    		entityBean.saveOrUpdateEntityData(ed);
    	}

    	// Renumber the remaining workspace children
    	index = 0;
    	for(EntityData ed : workspace.getOrderedEntityData()) {
    		if (ed.getChildEntity()==null) continue;
    		ed.setOrderIndex(index++);
    		entityBean.saveOrUpdateEntityData(ed);
    	}
    	
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
