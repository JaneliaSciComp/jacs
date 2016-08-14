package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.maskSearch.MaskSearchTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.maskSearch.MaskSearchResultNode;

/**
 * TODO: UNDOCUMENTED
 */
public class Vaa3DMaskSearchResultsDiscoveryService implements IService{

	protected EntityHelper entityHelper;
    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;
    protected Task task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, Vaa3DMaskSearchResultsDiscoveryService.class);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger);
            createDate = new Date();
            task = ProcessDataHelper.getTask(processData);

            MaskSearchResultNode tmpNode = (MaskSearchResultNode)ProcessDataHelper.getResultFileNode(processData);
            Entity parentFolder = entityHelper.createOrVerifyRootEntity(task.getParameter(MaskSearchTask.PARAM_resultsFolderName), true, true);
            parentFolder.setValueByAttributeName(EntityConstants.ATTRIBUTE_SEARCH_TASK_ID, task.getObjectId().toString());
            Entity inputMaskEntity = createMaskInputEntity(task.getParameter(MaskSearchTask.PARAM_inputFilePath));
            Entity textResultsEntity = createMaskSearchResultEntity(tmpNode.getFilePathByTag(MaskSearchResultNode.TAG_RESULTS));
            addToParent(parentFolder, textResultsEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            addToParent(parentFolder, inputMaskEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);

            Scanner scanner = new Scanner(new File(tmpNode.getDirectoryPath()+File.separator+"searchResults.txt"));
            ArrayList<String> sampleNames = new ArrayList<String>();

            while(scanner.hasNextLine()){
                String tmpLine = scanner.nextLine();
                tmpLine = tmpLine.substring(tmpLine.lastIndexOf("/")+1);
                tmpLine = tmpLine.substring(0,tmpLine.indexOf("."));
                sampleNames.add(tmpLine);
            }

            ArrayList<Long> searchResultSampleIds = new ArrayList<Long>();
            for (String tmpName : sampleNames) {
                List<Entity> tmpSet = entityBean.getEntitiesByNameAndTypeName(subject.getKey(), tmpName, EntityConstants.TYPE_SCREEN_SAMPLE);
                // Assume one hit and take it.
                if (null!=tmpSet && tmpSet.size()>0) {
                    searchResultSampleIds.add(tmpSet.iterator().next().getId());
                }
            }
            entityBean.addChildren(subject.getKey(), parentFolder.getId(), searchResultSampleIds, EntityConstants.ATTRIBUTE_ENTITY);

            Entity tmp2DMIP = entityHelper.create2dImage(tmpNode.getFilePathByTag(MaskSearchResultNode.TAG_MIP));
            Entity tmp3DStack = entityHelper.create3dImage(task.getParameter(MaskSearchTask.PARAM_inputFilePath));
            entityHelper.setDefault2dImage(tmp3DStack, tmp2DMIP);
            entityHelper.setDefault3dImage(inputMaskEntity, tmp3DStack);
        }
        catch (Exception e) {
            throw new ServiceException("Unable to process the entities from the mask search step.",e);
        }
    }

    private Entity createMaskInputEntity(String inputFilePath) throws Exception {
        File tmpFile = new File(inputFilePath);
        Entity inputEntity = new Entity();
        inputEntity.setName(tmpFile.getName());
        inputEntity.setOwnerKey(ownerKey);
        inputEntity.setEntityTypeName(EntityConstants.TYPE_IMAGE_3D);
        inputEntity.setCreationDate(createDate);
        inputEntity.setUpdatedDate(createDate);
        inputEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, inputFilePath);
        inputEntity = entityBean.saveOrUpdateEntity(inputEntity);
        logger.info("Saved search results entity as " + inputEntity.getId());
        return inputEntity;
    }

    protected Entity createMaskSearchResultEntity(String resultsFilePath) throws Exception {
        if (null==resultsFilePath || "".equals(resultsFilePath)) return null;
        File tmpFile = new File(resultsFilePath);
        Entity resultsEntity = new Entity();
        resultsEntity.setName(tmpFile.getName());
        resultsEntity.setOwnerKey(ownerKey);
        resultsEntity.setEntityTypeName(EntityConstants.TYPE_TEXT_FILE);
        resultsEntity.setCreationDate(createDate);
        resultsEntity.setUpdatedDate(createDate);
        resultsEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, resultsFilePath);
        resultsEntity = entityBean.saveOrUpdateEntity(resultsEntity);
        logger.info("Saved search results entity as " + resultsEntity.getId());
        return resultsEntity;
    }
	
    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, attrName);
        logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
                " as child of "+parent.getEntityTypeName()+"#"+parent.getId());
    }


}
