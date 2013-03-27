package org.janelia.it.jacs.compute.service.vaa3d;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.cellCounting.CellCountingResultNode;
import org.janelia.it.jacs.model.user_data.neuron.NeuronMergeResultNode;

import java.util.Date;

/**
 * File discovery service for cell counting results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DCellCounterResultsDiscoveryService implements IService{

	protected Entity sampleEntity;
	protected FileDiscoveryHelper entityHelper;
    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;
    protected Task task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, Vaa3DCellCounterResultsDiscoveryService.class);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            entityHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
            createDate = new Date();
            task = ProcessDataHelper.getTask(processData);

            // Put the Markup stack in the collection
//            Entity resultFolder = entityHelper.createFolderForFile()
//            Entity tmpCuratedNeuron = createCellCountingResult(tmpCuratedNeuronCollection);
//            entityHelper.addToParent(tmpCuratedNeuronCollection, tmpCuratedNeuron, tmpCuratedNeuronCollection.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
            
            CellCountingResultNode tmpNode = (CellCountingResultNode)ProcessDataHelper.getResultFileNode(processData);
            Entity tmp2DMIP = entityHelper.create2dImage(tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_MIP));
            Entity tmp3DStack = entityHelper.create3dImage(tmpNode.getFilePathByTag(NeuronMergeResultNode.TAG_STACK));
            entityHelper.setDefault2dImage(tmp3DStack, tmp2DMIP);
//            entityHelper.setDefault3dImage(tmpCuratedNeuron, tmp3DStack);
        }
        catch (Exception e) {
            throw new ServiceException("Unable to process the entities from the neuron merge step.",e);
        }
    }

    protected Entity createCellCountingResult(Entity separationResultEntity) throws Exception {
        Entity curatedNeuronCollectionEntity = new Entity();
        curatedNeuronCollectionEntity.setOwnerKey(separationResultEntity.getOwnerKey());
        curatedNeuronCollectionEntity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_CURATED_NEURON_COLLECTION));
        curatedNeuronCollectionEntity.setCreationDate(createDate);
        curatedNeuronCollectionEntity.setUpdatedDate(createDate);
        curatedNeuronCollectionEntity.setName("Curated Neurons");
        curatedNeuronCollectionEntity = entityBean.saveOrUpdateEntity(curatedNeuronCollectionEntity);
        logger.info("Saved curated neuron collection as " + curatedNeuronCollectionEntity.getId());
        return curatedNeuronCollectionEntity;
    }

}
