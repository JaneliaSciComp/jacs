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
    private AnnotationBeanLocal annotationBean;
    private ComputeBeanLocal computeBean;
    private Date createDate;
    private User user;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            NeuronSeparatorPipelineTask task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            createDate = new Date();
            user = computeBean.getUserByName(task.getOwner());

            // Create the other files that are necessary
//            File outFile = new File(parentNode.getDirectoryPath(), STDOUT_FILE);
//            File errFile = new File(parentNode.getDirectoryPath(), STDERR_FILE);
//            String cmdLine = NeuronSeparatorHelper.getPostNeuronSeparationCommands(task, parentNode, sample, " ; ") +
//            	" 1>"+outFile.getAbsolutePath()+" 2>"+errFile.getAbsolutePath();
//
//            if (cmdLine!=null && cmdLine.length()>0) {
//                SystemCall call = new SystemCall();
//                int exitCode = call.emulateCommandLine(cmdLine.toString(), true, 60);
//                if (0 != exitCode) {
//                    throw new ServiceException("NeuronSeparatorResultsService failed with exitCode " + exitCode + " for resultDir=" + parentNode.getDirectoryPath());
//                }
//            }
            
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
            NeuronSeparatorHelper.addToParent(userParentRoot, resultEntity, userParentRoot.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
            
            Entity filesFolder = createSupportingFilesFolder();
            NeuronSeparatorHelper.addToParent(resultEntity, filesFolder, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
            
            Entity fragmentsFolder = createFragmentCollection();
            NeuronSeparatorHelper.addToParent(resultEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_NEURON_FRAGMENTS);
            
            EntityType tif2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);
            EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
            EntityType tif3DLabel = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);
            
            File resultDir = new File(parentNode.getDirectoryPath());
            for (File resultFile : resultDir.listFiles()) {
            	String filename = resultFile.getName();

            	if (resultFile.isDirectory()) continue;
            	
                if (filename.equals("ConsolidatedSignal.tif")) {
                    addResultItem(filesFolder, tif3D, resultFile);
                }
                else if (filename.startsWith("Signal_") && filename.endsWith(".tif")) {
                    addResultItem(filesFolder, tif3D, resultFile);
                }
                else if (filename.equals("ConsolidatedLabel.tif")) {
                    addResultItem(filesFolder, tif3DLabel, resultFile);
                }
                else if (filename.equals("Reference.tif")) {
                    addResultItem(filesFolder, tif3D, resultFile);
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
	
    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = annotationBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
    
	private Entity createFolderEntity(String name) throws Exception {
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setUser(user);
        fragmentsEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName(name);

        fragmentsEntity = annotationBean.saveOrUpdateEntity(fragmentsEntity);
        logger.info("Saved folder '"+name+"' as "+fragmentsEntity.getId());
        
        return fragmentsEntity;
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
        Entity entity = new Entity();
        entity.setUser(user);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(type);
        entity.setName(file.getName());
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        entity = annotationBean.saveOrUpdateEntity(entity);
        logger.info("Saved "+type.getName()+" as "+entity.getId());
        NeuronSeparatorHelper.addToParent(resultEntity, entity, null, EntityConstants.ATTRIBUTE_ENTITY);
        return entity;
    }

}