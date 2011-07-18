package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

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

            String[]lsmFilePaths = NeuronSeparatorHelper.getLSMFilePaths(task);
            createLsmMetadataFile(lsmFilePaths[0]);
            createLsmMetadataFile(lsmFilePaths[1]);

            Entity resultEntity = createResultEntity();
            addToParent(sample, resultEntity, 1);

            EntityType tif2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);
            EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
            EntityType tif3DLabel = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);

            createLsmFilePaths(lsmFilePaths[0], lsmFilePaths[1]);

            File resultDir = new File(parentNode.getDirectoryPath());
            for (File resultFile : resultDir.listFiles()) {
            	String filename = resultFile.getName();

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
                else if (filename.equals("lsmFilePaths.txt")) {
                    // do nothing - ignore this file
                }
                else if (filename.equals("neuronSeparatorPipeline.neu")) {
                    // ignore
                }
                else {
                    throw new Exception("Unrecognized result file: "+resultFile.getAbsolutePath());
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

        // TODO: maybe the LSM inputs should be saved as attributes of the result entity, but not as generic entity children
        // for now you can go up a level to the Sample and then the LSM Stack Pair

//        if (lsm1!=null) {
//            addToParent(resultEntity, lsm1);
//        }
//
//        if (lsm2!=null) {
//            addToParent(resultEntity, lsm2);
//        }

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

    // todo This dependency should be managed and the job should run on the grid as part of the grid job script
    private void createLsmFilePaths(String lsm1Path, String lsm2Path) throws Exception {
        File lsmPathFile = new File(parentNode.getDirectoryPath()+"/"+"lsmFilePaths.txt");
        FileWriter fw = new FileWriter(lsmPathFile);
        fw.write(lsm1Path+"\n");
        fw.write(lsm2Path+"\n");
        fw.close();
    }

    private void addToParent(Entity parent, Entity entity, Integer index) throws Exception {
        EntityData ed = parent.addChildEntity(entity);
        ed.setOrderIndex(index);
        EJBFactory.getLocalComputeBean().genericSave(ed);
        logger.info("Added " + entity.getEntityType().getName() + "#" + entity.getId() + " as child of " + parent.getEntityType().getName() + "#" + entity.getId());
    }

    // todo This dependency should be managed and the job should run on the grid as part of the grid job script
    private void createLsmMetadataFile(String lsmPath) throws Exception {
        File lsmFile=new File(lsmPath);
        if (!lsmFile.exists()) {
            throw new Exception("Could not find lsm file="+lsmFile.getAbsolutePath());
        }
        File lsmDataFile=new File(parentNode.getDirectoryPath()+"/"+removeWhitespace(lsmFile.getName())+".metadata");
        String cmdLine = "cd " + parentNode.getDirectoryPath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "lsm_metadata_dump.pl " +
                lsmPath + " " + lsmDataFile.getAbsolutePath();
        logger.info("createLsmMetadataFile cmdLine=" + cmdLine);

        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(cmdLine, true, 120 /* seconds */);
        if (0 != exitCode) {
            throw new Exception("The NeuronSeparationPipelineService createLsmMetadata step did not exit properly.");
        }
    }

    private String removeWhitespace(String s) {
        return s.replaceAll("\\s+","");
    }

}