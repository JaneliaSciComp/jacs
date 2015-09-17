package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * A base class for result discovery services which can be re-run multiple times on the 
 * same result entity and directory in order to discover additional items over time. 
 *  
 * Input variables if adding files to an existing result:
 *   RESULT_ENTITY or RESULT_ENTITY_ID  
 * 
 * Input variables if discovering new result:
 *   ROOT_ENTITY_ID - the parent of the separation
 *   ROOT_FILE_NODE - the file node containing the files to be discovered
 *   RESULT_ENTITY_NAME - the name of the new result entity
 * 
 *  
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IncrementalResultDiscoveryService extends AbstractEntityService {

    private Map<String,Entity> resultItems = new HashMap<String,Entity>();
    
    protected FileDiscoveryHelper helper;
    protected Date createDate;
    protected FileNode resultFileNode;

    @Override
    public void execute() throws Exception {

        this.createDate = new Date();
        this.resultFileNode = (FileNode)data.getItem("ROOT_FILE_NODE");
        
        this.helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        helper.addFileExclusion("*.log");
        helper.addFileExclusion("*.oos");
        helper.addFileExclusion("sge_*");
        helper.addFileExclusion("temp");
        helper.addFileExclusion("tmp.*");
        helper.addFileExclusion("core.*");
        helper.addFileExclusion("*.sh");

        Entity resultEntity = (Entity)data.getItem("RESULT_ENTITY");
        if (resultEntity==null) {
            Long resultEntityId = data.getItemAsLong("RESULT_ENTITY_ID");
            if (resultEntityId!=null) {
                resultEntity = entityBean.getEntityTree(resultEntityId);
                if (resultEntity==null) {
                    logger.error("Result entity "+resultEntityId+" no longer exists. There is nothing to do.");
                    return;
                }
            }
        }
        
        if (resultEntity==null) {
            // Create a new result entity
            Long rootEntityId = data.getRequiredItemAsLong("ROOT_ENTITY_ID");
            String resultName = (String)processData.getItem("RESULT_ENTITY_NAME");
            Entity parentEntity = entityBean.getEntityTree(rootEntityId);
            resultEntity = createNewResultEntity(resultName);
            helper.addToParent(parentEntity, resultEntity, parentEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
        }
        else {
            // Find existing result items in the neuron separation
            logger.info("Finding existing result items...");
            findResultItems(resultEntity);
        }
        
        // Add additional files to the neuron separation
        discoverResultFiles(resultEntity);

        processData.putItem("RESULT_ENTITY", resultEntity);
        processData.putItem("RESULT_ENTITY_ID", resultEntity.getId().toString());
    }
    
    protected abstract Entity createNewResultEntity(String resultName) throws Exception;

    protected abstract void discoverResultFiles(Entity resultEntity) throws Exception;

    private void findResultItems(Entity entity) throws Exception {
        
        logger.trace("  findResultItems "+entity.getName()+" ("+entity.getId()+")");
        String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (filepath!=null) {
            logger.trace("  "+entity.getName()+": "+filepath);
            resultItems.put(filepath, entity);
        }
        
        populateChildren(entity);
        for(Entity child : entity.getChildren()) {
            findResultItems(child);
        }
    }
    
    protected Entity getOrCreateResultItem(Entity separation, File resultFile) throws Exception {
        
        logger.trace("Get or create "+resultFile.getAbsolutePath());
        Entity resultItem = resultItems.get(resultFile.getAbsolutePath());
        if (resultItem==null) {
            resultItem = helper.createResultItemForFile(resultFile);
            resultItems.put(resultFile.getAbsolutePath(), resultItem);
        }
        return resultItem;
    }

    protected void addToParentIfNecessary(Entity parent, Entity child, String entityAttrName) throws Exception {
        if (child==null) return;
        for(EntityData ed : parent.getOrderedEntityData()) {
            Entity existingChild = ed.getChildEntity();
            if (existingChild!=null) {
                if (ed.getEntityAttrName().equals(entityAttrName) && existingChild.getId().equals(child.getId())) {
                    return; // already an entity child
                }
            }
        }
        helper.addToParent(parent, child, parent.getMaxOrderIndex()+1, entityAttrName);
    }
}
