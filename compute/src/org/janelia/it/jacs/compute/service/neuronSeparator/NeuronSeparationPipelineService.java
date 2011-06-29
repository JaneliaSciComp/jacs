package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.colorSeparator.ColorSeparatorResultNode;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class NeuronSeparationPipelineService implements IService {
    Logger logger;
    private NeuronSeparatorPipelineTask task;
    NeuronSeparatorResultNode parentNode;
    AnnotationBeanRemote annotationBean;
    ComputeBeanRemote computeBean;
    Date createDate;
    User user;
    Entity neuronSeparatorPipelineResultEntity;
    Entity lsm1;
    Entity lsm2;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            logger.debug("\n\nExecuting Neuron Separation...\n\n");
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
            annotationBean=EJBFactory.getRemoteAnnotationBean();
            computeBean=EJBFactory.getRemoteComputeBean();
            createDate=new Date();
            user=computeBean.getUserByName(task.getOwner());

            logger.info("Starting NeuronSeparationPipelineService with taskId="+task.getObjectId()+
                    " resultNodeId="+parentNode.getObjectId()+" resultDir="+parentNode.getDirectoryPath());

            String lsmFileList=task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList);
            String lsmEntityList=task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);

            if ( !(lsmFileList==null || lsmFileList.trim().length()==0) &&
                 !(lsmEntityList==null || lsmEntityList.trim().length()==0)) {
                throw new Exception("Only one of PARAM_inputLsmFilePathList or PARAM_inputLsmEntityIdList should be populated");
            }

            String fileList="";

            if (lsmFileList!=null && lsmFileList.length()>0) {
                fileList=lsmFileList;
            } else {
                String[] lsmList=lsmEntityList.split(",");
                if (lsmList.length!=2) {
                    throw new Exception("Expected two files in lsmEntityList="+lsmEntityList);
                }
                lsm1=annotationBean.getEntityById(lsmList[0].trim());
                lsm2=annotationBean.getEntityById(lsmList[1].trim());
                fileList=lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) + " , " + lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            }

            // todo this should be a separate process running on the grid
            String cmdLine = "cd "+parentNode.getDirectoryPath()+";export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:" +
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"genelib/mylib;"+
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib/sampsepNA -nr -pj "+
                    parentNode.getDirectoryPath()+" neuronSeparatorPipeline "+ addQuotesToCsvString(fileList);
            logger.info("NeuronSeparatorPipelineTask cmdLine="+cmdLine);
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine(cmdLine, true);
            if (0!=exitCode) {
                throw new ServiceException("The NeuronSeparationPipelineService consolidator step did not exit properly.");
            }
            createResultEntity();

            EntityType tif2D=annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_2D);
            EntityType tif3D=annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
            EntityType tif3DLabel=annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);

            createLsmFilePaths();

            File resultDir=new File(parentNode.getDirectoryPath());
            File[] resultFiles = resultDir.listFiles();
            for (File result : resultFiles) {
                if (result.getName().equals("ConsolidatedSignal.tif")) {
                    initEntity(tif3D, result.getAbsolutePath());
                } else if (result.getName().equals("ConsolidatedLabel.tif")) {
                    initEntity(tif3DLabel, result.getAbsolutePath());
                } else if (result.getName().startsWith("neuronSeparatorPipeline.PR.neuron") && result.getName().endsWith(".tif")) {
                    initEntity(tif2D, result.getAbsolutePath());
                } else if (result.getName().equals("lsmFilePaths.txt")) {
                  // do nothing - ignore this file
                } else {
                    throw new Exception("Do not recognize result file for Neuron Separator Pipeline="+result.getAbsolutePath());
                }
                logger.info("Added child entity as file="+result.getAbsolutePath());
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

    String addQuotesToCsvString(String csvString) {
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

    void createResultEntity() throws Exception {
        neuronSeparatorPipelineResultEntity = new Entity();
        EntityType neuSepType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        neuronSeparatorPipelineResultEntity.setUser(user);
        neuronSeparatorPipelineResultEntity.setEntityType(neuSepType);
        neuronSeparatorPipelineResultEntity.setCreationDate(createDate);
        neuronSeparatorPipelineResultEntity.setUpdatedDate(createDate);
        neuronSeparatorPipelineResultEntity=annotationBean.saveOrUpdateEntity(neuronSeparatorPipelineResultEntity);
        neuronSeparatorPipelineResultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, parentNode.getDirectoryPath());
        neuronSeparatorPipelineResultEntity=annotationBean.saveOrUpdateEntity(neuronSeparatorPipelineResultEntity);

        if (lsm1!=null) {
            logger.info("Adding lsm1 as child entity for neuronSeparatorPipelineResultEntity");
            addToParent(lsm1);
        }

        if (lsm2!=null) {
            logger.info("Adding lsm2 as child entity for neuronSeparatorPipelineResultEntity");
            addToParent(lsm2);
        }

        logger.info("Saved neuronSeparatorPipelineResultEntity id="+neuronSeparatorPipelineResultEntity.getId());
    }

    Entity initEntity(EntityType type, String filePath) throws Exception {
        Entity entity=new Entity();
        entity.setUser(user);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(type);
        entity=annotationBean.saveOrUpdateEntity(entity);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, filePath);
        entity=annotationBean.saveOrUpdateEntity(entity);
        addToParent(entity);
        return entity;
    }

    void addToParent(Entity entity) throws Exception {
        EntityData ed = neuronSeparatorPipelineResultEntity.addChildEntity(entity);
        //computeBean.genericSave(ed);
        neuronSeparatorPipelineResultEntity=annotationBean.saveOrUpdateEntity(neuronSeparatorPipelineResultEntity);
    }

    void createLsmFilePaths() throws Exception {
        File lsmPathFile=new File(parentNode.getDirectoryPath()+"/"+"lsmFilePaths.txt");
        FileWriter fw=new FileWriter(lsmPathFile);
        fw.write(lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)+"\n");
        fw.write(lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)+"\n");
        fw.close();
    }

}