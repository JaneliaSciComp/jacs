package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

/**
 * @author Todd Safford
 */
public class NeuronSeparatorResultsService implements IService {

    private Logger logger;
    private NeuronSeparatorResultNode parentNode;
    private AnnotationBeanRemote annotationBean;
    private Date createDate;
    private User user;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            NeuronSeparatorPipelineTask task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            createDate = new Date();
            user = computeBean.getUserByName(task.getOwner());
            String sampleEntityId = task.getParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId);
            Entity sample = annotationBean.getEntityById(sampleEntityId.trim());

            if (sample == null) {
                throw new ServiceException("Must provide Sample entity.");
            }
            
            // Create the result entity and populate with the output files
            
            Entity resultEntity = createResultEntity();
            addToParent(sample, resultEntity, 1);

            EntityType tif2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);
            EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
            EntityType tif3DLabel = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);

            File resultDir = new File(parentNode.getDirectoryPath());
            for (File resultFile : resultDir.listFiles()) {
            	String filename = resultFile.getName();

            	if (resultFile.isDirectory()) continue;
            	
                if (filename.equals("ConsolidatedSignal.tif")) {
                    addResultItem(resultEntity, tif3D, resultFile);
                }
                else if (filename.equals("ConsolidatedLabel.tif")) {
                    addResultItem(resultEntity, tif3DLabel, resultFile);
                }
                else if (filename.equals("Reference.tif")) {
                    addResultItem(resultEntity, tif3D, resultFile);
                }
                else if (filename.startsWith("neuronSeparatorPipeline.PR.neuron") && filename.endsWith(".tif")) {
                	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));

                	Integer index = null;
                	try {
                		index = Integer.parseInt(mipNum);
                	}
                	catch (NumberFormatException e) {
                		logger.warn("Error parsing number from MIP filename: "+mipNum);
                	}

                    addResultItem(resultEntity, tif2D, resultFile, index);
                }
                else if (filename.equals("neuronSeparatorPipeline.neu")) {
                    // ignore
                }
                else if (filename.endsWith(".metadata")) {
                    // ignore metadata files
                }
                else if (filename.endsWith(".txt")) {
                    // ignore text files
                }
                else if (filename.endsWith(".oos") || filename.endsWith(".log")) {
                    // ignore log files
                }
                else {
                    logger.warn("Unrecognized result file: "+resultFile.getAbsolutePath());
                }
            }
            
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private Entity createResultEntity() throws Exception {
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName("Neuron Separation");

        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, parentNode.getDirectoryPath());
        resultEntity = annotationBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved pipeline result entity as "+resultEntity.getId());

        return resultEntity;
    }

    private Entity addResultItem(Entity resultEntity, EntityType type, File file) throws Exception {
    	return addResultItem(resultEntity, type, file, null);
    }

    private Entity addResultItem(Entity resultEntity, EntityType type, File file, Integer index) throws Exception {
        Entity entity = new Entity();
        entity.setUser(user);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(type);
        entity.setName(file.getName());
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        entity = annotationBean.saveOrUpdateEntity(entity);
        logger.info("Saved "+type.getName()+" as "+entity.getId());
        addToParent(resultEntity, entity, index);
        return entity;
    }

    private void addToParent(Entity parent, Entity entity, Integer index) throws Exception {
        EntityData ed = parent.addChildEntity(entity);
        ed.setOrderIndex(index);
        EJBFactory.getLocalComputeBean().genericSave(ed);
        logger.info("Added " + entity.getEntityType().getName() + "#" + entity.getId() + " as child of " + parent.getEntityType().getName() + "#" + entity.getId());
    }

}