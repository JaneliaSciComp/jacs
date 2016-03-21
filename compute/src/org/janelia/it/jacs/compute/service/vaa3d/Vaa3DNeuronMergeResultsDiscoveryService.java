package org.janelia.it.jacs.compute.service.vaa3d;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.EntityHelperNG;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.neuron.NeuronMergeResultNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for neuron merge results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DNeuronMergeResultsDiscoveryService implements IService{

	protected Entity sampleEntity;
	protected EntityHelperNG entityHelper;
    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;
    protected Task task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, Vaa3DNeuronMergeResultsDiscoveryService.class);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            entityHelper = new EntityHelperNG(entityBean, computeBean, ownerKey, logger);
            createDate = new Date();
            task = ProcessDataHelper.getTask(processData);
            Entity separationResultEntity = entityBean.getEntityTree(Long.valueOf(task.getParameter(NeuronMergeTask.PARAM_separationEntityId)));
            // if the collection doesn't exist add it
            Entity tmpCuratedNeuronCollection = verifyOrCreateCuratedNeuronCollection(separationResultEntity);

            // Put the Curated Neuron in the collection
            Entity tmpCuratedNeuron =  createCuratedNeuronEntity(tmpCuratedNeuronCollection);

            addToParent(tmpCuratedNeuronCollection, tmpCuratedNeuron, tmpCuratedNeuronCollection.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            
            NeuronMergeResultNode tmpNode = (NeuronMergeResultNode)ProcessDataHelper.getResultFileNode(processData);
            Entity tmp2DMIP = entityHelper.create2dImage(tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_MIP));
            Entity tmp3DStack = entityHelper.create3dImage(tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_STACK));
            entityHelper.setDefault2dImage(tmp3DStack, tmp2DMIP);
            entityHelper.setDefault3dImage(tmpCuratedNeuron, tmp3DStack);
        }
        catch (Exception e) {
            throw new ServiceException("Unable to process the entities from the neuron merge step.",e);
        }
    }

    private Entity verifyOrCreateCuratedNeuronCollection(Entity separationResultEntity) throws Exception {
        if (null==separationResultEntity) {
            throw new ServiceException("Cannot add Curated Neurons to a null Separation Result");
        }
        List<Entity> tmpCuratedNeuronCollectionItems = EntityUtils.getChildrenOfType(separationResultEntity, EntityConstants.TYPE_CURATED_NEURON_COLLECTION);
        if (null!=tmpCuratedNeuronCollectionItems && 1==tmpCuratedNeuronCollectionItems.size()) {
            return tmpCuratedNeuronCollectionItems.get(0);
        }
        // if the collection doesn't exist create it
        Entity tmpCuratedNeuronCollection = createCuratedNeuronCollection(separationResultEntity);
        addToParent(separationResultEntity, tmpCuratedNeuronCollection, separationResultEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        return tmpCuratedNeuronCollection;
    }

    protected Integer getNextAvailableIndex(Entity parentEntity) {
        List<Entity> children = EntityUtils.getChildrenOfType(parentEntity, EntityConstants.TYPE_CURATED_NEURON);
        if (null==children) {return 1;}
        return children.size()+1;
    }
    
    protected Entity createCuratedNeuronEntity(Entity tmpCuratedNeuronCollection) throws Exception {
        String tmpIndex = getNextAvailableIndex(tmpCuratedNeuronCollection).toString();
        Entity curatedNeuronEntity = new Entity();
        curatedNeuronEntity.setOwnerKey(ownerKey);
        curatedNeuronEntity.setEntityTypeName(EntityConstants.TYPE_CURATED_NEURON);
        curatedNeuronEntity.setCreationDate(createDate);
        curatedNeuronEntity.setUpdatedDate(createDate);
        curatedNeuronEntity.setName("Curated Neuron " + tmpIndex);
        curatedNeuronEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, tmpIndex);
        curatedNeuronEntity = entityBean.saveOrUpdateEntity(curatedNeuronEntity);
        logger.info("Saved curated neuron entity as " + curatedNeuronEntity.getId());
        ArrayList<Long> tmpFragmentOidList = new ArrayList<Long>();
        for (String tmpFragmentOid : Task.listOfStringsFromCsvString(task.getParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList))) {
            tmpFragmentOidList.add(Long.valueOf(tmpFragmentOid));
        }
        entityBean.addChildren(ownerKey, curatedNeuronEntity.getId(), tmpFragmentOidList,EntityConstants.ATTRIBUTE_ENTITY);
        return curatedNeuronEntity;
    }
	
    protected Entity createCuratedNeuronCollection(Entity separationResultEntity) throws Exception {
        Entity curatedNeuronCollectionEntity = new Entity();
        curatedNeuronCollectionEntity.setOwnerKey(separationResultEntity.getOwnerKey());
        curatedNeuronCollectionEntity.setEntityTypeName(EntityConstants.TYPE_CURATED_NEURON_COLLECTION);
        curatedNeuronCollectionEntity.setCreationDate(createDate);
        curatedNeuronCollectionEntity.setUpdatedDate(createDate);
        curatedNeuronCollectionEntity.setName("Curated Neurons");
        curatedNeuronCollectionEntity = entityBean.saveOrUpdateEntity(curatedNeuronCollectionEntity);
        logger.info("Saved curated neuron collection as " + curatedNeuronCollectionEntity.getId());
        return curatedNeuronCollectionEntity;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, attrName);
        logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
                " as child of "+parent.getEntityTypeName()+"#"+parent.getId());
    }


}
