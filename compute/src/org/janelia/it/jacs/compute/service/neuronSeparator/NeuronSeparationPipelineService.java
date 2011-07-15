package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class NeuronSeparationPipelineService implements IService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private Logger logger;
    private NeuronSeparatorPipelineTask task;
    private NeuronSeparatorResultNode parentNode;
    private AnnotationBeanRemote annotationBean;
    private ComputeBeanRemote computeBean;
    private Date createDate;
    private User user;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            computeBean = EJBFactory.getRemoteComputeBean();
            createDate = new Date();
            user = computeBean.getUserByName(task.getOwner());

            logger.info("Starting NeuronSeparationPipelineService with taskId="+task.getObjectId()+
                    " resultNodeId="+parentNode.getObjectId()+" resultDir="+parentNode.getDirectoryPath());

            String lsmFileList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList);
            String lsmEntityList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);
            String sampleEntityId = task.getParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId);

            if ( !(lsmFileList==null || lsmFileList.trim().length()==0) &&
                 !(lsmEntityList==null || lsmEntityList.trim().length()==0)) {
                throw new Exception("Only one of PARAM_inputLsmFilePathList or PARAM_inputLsmEntityIdList should be populated");
            }

            String fileList="";

            Entity lsm1 = null;
            Entity lsm2 = null;
            
            if (lsmFileList!=null && lsmFileList.length()>0) {
                fileList = lsmFileList;
            } 
            else {
                String[] lsmList = lsmEntityList.split(",");
                if (lsmList.length!=2) {
                    throw new Exception("Expected two files in lsmEntityList="+lsmEntityList);
                }
                lsm1 = annotationBean.getEntityById(lsmList[0].trim());
                lsm2 = annotationBean.getEntityById(lsmList[1].trim());
                fileList = lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) + " , " + lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            }
            
            if (lsm1 == null || lsm2 == null) {
            	// TODO: can either of these be null? Above logic suggests yes, but some code below depends on them being not null
            	throw new ServiceException("Must provide two LSM stack entities.");
            }

            Entity sample = annotationBean.getEntityById(sampleEntityId.trim());
            
            if (sample == null) {
            	throw new ServiceException("Must provide Sample entity.");
            }
            
            // TODO: this should be a separate process running on the grid
            String cmdLine = "cd "+parentNode.getDirectoryPath()+";export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:" +
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"genelib/mylib;"+
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib/sampsepNA -nr -pj "+
                    parentNode.getDirectoryPath()+" neuronSeparatorPipeline "+ addQuotesToCsvString(fileList);
            logger.info("NeuronSeparatorPipelineTask cmdLine="+cmdLine);
            
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine(cmdLine, true, TIMEOUT_SECONDS);
            if (0!=exitCode) {
                throw new ServiceException("The NeuronSeparationPipelineService consolidator step did not exit properly.");
            }

            createLsmMetadataFile(lsm1);
            createLsmMetadataFile(lsm2);
            
            Entity resultEntity = createResultEntity(lsm1, lsm2);
            addToParent(sample, resultEntity, 1);
            
            EntityType tif2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);
            EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
            EntityType tif3DLabel = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);

            createLsmFilePaths(lsm1, lsm2);
            
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
            try {
                EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("ERROR running the Neuron Separation Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the Neuron Separation NeuronSeparationPipelineService:" + e.getMessage(), e);
        }
    }

    private String addQuotesToCsvString(String csvString) {
        String[] clist=csvString.split(",");
        StringBuffer sb=new StringBuffer();
        for (int i=0;i<clist.length;i++) {
            sb.append("\"");
            sb.append(clist[i].trim());
            sb.append("\"");
            if (i<clist.length-1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private Entity createResultEntity(Entity lsm1, Entity lsm2) throws Exception {
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

    private void createLsmFilePaths(Entity lsm1, Entity lsm2) throws Exception {
        File lsmPathFile = new File(parentNode.getDirectoryPath()+"/"+"lsmFilePaths.txt");
        FileWriter fw = new FileWriter(lsmPathFile);
        fw.write(lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)+"\n");
        fw.write(lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)+"\n");
        fw.close();
    }
    
    private void addToParent(Entity parent, Entity entity) throws Exception {
    	addToParent(parent, entity, null);
    }
    
    private void addToParent(Entity parent, Entity entity, Integer index) throws Exception {
        EntityData ed = parent.addChildEntity(entity);
        ed.setOrderIndex(index);
        computeBean.genericSave(ed);
        logger.info("Added " + entity.getEntityType().getName() + "#" + entity.getId() + " as child of " + parent.getEntityType().getName() + "#" + entity.getId());
    }

    private void createLsmMetadataFile(Entity lsm) throws Exception {
        String lsmFilePath=lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        File lsmFile=new File(lsmFilePath);
        if (!lsmFile.exists()) {
            throw new Exception("Could not find lsm file="+lsmFile.getAbsolutePath());
        }
        File lsmDataFile=new File(parentNode.getDirectoryPath()+"/"+removeWhitespace(lsmFile.getName())+".metadata");
        String cmdLine = "cd " + parentNode.getDirectoryPath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "lsm_metadata_dump.pl " +
                lsmFilePath + " " + lsmDataFile.getAbsolutePath();
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