package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Process the results of a Neuron Separation task and create the required entities, symbolic links, and other 
 * auxiliary files.
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronMergeSeparatorResultsService implements IService {

//    private static final String STDOUT_FILE = "postsep.out";
//    private static final String STDERR_FILE = "postsep.err";
    
    private Logger logger;
    private NeuronSeparatorResultNode parentNode;
    private NeuronSeparatorPipelineTask task;
    private AnnotationBeanLocal annotationBean;
    private Date createDate;
    private User user;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            createDate = new Date();
            user = computeBean.getUserByName(task.getOwner());

            // Create the result entity and populate with the output files
            List<Entity> userCommonRoots = EJBFactory.getLocalAnnotationBean().getCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER);
            Entity userParentRoot=null;
            for(final Entity commonRoot : userCommonRoots) {
                if (task.getOwner().equals(commonRoot.getUser().getUserLogin())) {
                    userParentRoot = commonRoot;
                    break;
                }
            }
            if (null==userParentRoot) {
                throw new ServiceException("NeuronMergeSeparatorResultsService: Can't find a common root for user "+task.getOwner());
            }
            Entity resultEntity = createResultEntity();
            NeuronSeparatorHelper.addToParent(userParentRoot, resultEntity, userParentRoot.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            
            Entity inputFolder = createInputFilesFolder();
            NeuronSeparatorHelper.addToParent(resultEntity, inputFolder, 0, EntityConstants.ATTRIBUTE_ENTITY);
            
            Entity fragmentsFolder = createFragmentCollection();
            NeuronSeparatorHelper.addToParent(resultEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_NEURON_FRAGMENTS);
            
            EntityType tif2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);

            File resultDir = new File(parentNode.getDirectoryPath());
            for (File resultFile : resultDir.listFiles()) {
            	String filename = resultFile.getName();

            	if (resultFile.isDirectory()) continue;
            	
                if (filename.startsWith("neuronSeparatorPipeline.PR.neuron") && filename.endsWith(".tif")) {
                	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));

                	Integer index = null;
                	try {
                		index = Integer.parseInt(mipNum);
                	}
                	catch (NumberFormatException e) {
                		logger.warn("Error parsing number from MIP filename: "+mipNum);
                	}

                	Entity fragmentEntity = createFragmentEntity(tif2D, resultFile, index);
                	NeuronSeparatorHelper.addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);
                }
                else {
                    // ignore other files
                }
            }

            // TODO: migrate the annotations from the previous result

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private Entity createInputFilesFolder() throws Exception {
        Entity inputFolder = new Entity();
        inputFolder.setUser(user);
        inputFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
        inputFolder.setCreationDate(createDate);
        inputFolder.setUpdatedDate(createDate);
        inputFolder.setName("Input Files");
        inputFolder = annotationBean.saveOrUpdateEntity(inputFolder);
        logger.info("Saved supporting files folder as "+inputFolder.getId());

        List<String> inputLsmFiles = Task.listOfStringsFromCsvString(task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList));
        Entity lsmEntity1 = createLsmStackFromFile(new File(inputLsmFiles.get(0)));
        Entity lsmEntity2 = createLsmStackFromFile(new File(inputLsmFiles.get(1)));
        NeuronSeparatorHelper.addToParent(inputFolder, lsmEntity1, inputFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        NeuronSeparatorHelper.addToParent(inputFolder, lsmEntity2, inputFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        return inputFolder;
    }

    private Entity createFragmentEntity(EntityType tif2D, File file, Integer index) throws Exception {
		
        Entity fragmentEntity = new Entity();
        fragmentEntity.setUser(user);
        fragmentEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT));
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        
        fragmentEntity = annotationBean.saveOrUpdateEntity(fragmentEntity);
        logger.info("Saved fragment entity as "+fragmentEntity.getId());

        Entity fileEntity = new Entity();
        fileEntity.setUser(user);
        fileEntity.setCreationDate(createDate);
        fileEntity.setUpdatedDate(createDate);
        fileEntity.setEntityType(tif2D);
        fileEntity.setName(file.getName());
        fileEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        fileEntity = annotationBean.saveOrUpdateEntity(fileEntity);
        NeuronSeparatorHelper.addToParent(fragmentEntity, fileEntity, null, EntityConstants.ATTRIBUTE_ENTITY);

        logger.info("Saved fragment MIP as "+fileEntity.getId());
        
        return fragmentEntity;
    }
	
	private Entity createFragmentCollection() throws Exception {
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setUser(user);
        fragmentsEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION));
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName("Neuron Fragments");

        fragmentsEntity = annotationBean.saveOrUpdateEntity(fragmentsEntity);
        logger.info("Saved fragment collection as "+fragmentsEntity.getId());
        
        return fragmentsEntity;
    }
	
	private Entity createResultEntity() throws Exception {
        List<String> tmplsmFiles = Task.listOfStringsFromCsvString(task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList));
        File tmpInputPath = new File(tmplsmFiles.get(0));
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName("Neuron Separation of "+tmpInputPath.getParentFile().getName());

        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, parentNode.getDirectoryPath());
        resultEntity = annotationBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved pipeline result entity as "+resultEntity.getId());

        return resultEntity;
    }

    private Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as " + lsmStack.getId());
        return lsmStack;
    }

}