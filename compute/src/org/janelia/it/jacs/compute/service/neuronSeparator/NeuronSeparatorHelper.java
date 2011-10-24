package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/18/11
 * Time: 10:19 AM
 */
public class NeuronSeparatorHelper {

	private static final Logger logger = Logger.getLogger(NeuronSeparatorHelper.class);
	
	// TODO: this path translation should be made into some sort of robust service
	
    private static final String jacsDataPathLinux = SystemConfigurationProperties.getString("JacsData.Dir.Linux","").trim();
    private static final String jacsDataPathMac = SystemConfigurationProperties.getString("JacsData.Dir.Mac","").trim();
    private static final String jacsDataPathRemote = SystemConfigurationProperties.getString("JacsData.Dir.Remote.Work.Server.Mac","").trim();
    
    private static final String flylightPathLinux = SystemConfigurationProperties.getString("FlyLight.Dir.Linux","").trim();
    private static final String flylightPathMac = SystemConfigurationProperties.getString("FlyLight.Dir.Mac","").trim();
    private static final String flylightPathRemote = SystemConfigurationProperties.getString("FlyLight.Dir.Remote.Work.Server.Mac","").trim();
    
    private static final String rubinlabPathLinux = SystemConfigurationProperties.getString("RubinLab.Dir.Linux","").trim();
    private static final String rubinlabPathMac = SystemConfigurationProperties.getString("RubinLab.Dir.Mac","").trim();
    private static final String rubinlabPathRemote = SystemConfigurationProperties.getString("RubinLab.Dir.Remote.Work.Server.Mac","").trim();
    
    public static String covertPathsToRemoteServer(String s) {
        return s.replaceAll(jacsDataPathLinux, jacsDataPathRemote).
        		 replaceAll(flylightPathLinux, flylightPathRemote).
        		 replaceAll(rubinlabPathLinux, rubinlabPathRemote);
    }

    public static String covertPathsToVolumeMounted(String s) {
        return s.replaceAll(jacsDataPathLinux, jacsDataPathMac).
		 		 replaceAll(flylightPathLinux, flylightPathMac).
		 		 replaceAll(rubinlabPathLinux, rubinlabPathMac);
    }
    
	public static String getNeuronSeparationCommands(NeuronSeparatorPipelineTask task, 
			NeuronSeparatorResultNode parentNode, String mylibDir, String commandDelim) throws ServiceException {

        StringBuilder cmdLine = new StringBuilder();
        String fileList;
        if (null!=task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath)&&
            !"".equals(task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath))) {
            fileList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath);
        }
        else {
            fileList = NeuronSeparatorHelper.getFileListString(task);
        }

        cmdLine.append("cd ").append(parentNode.getDirectoryPath()).append(";export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:").
                append(SystemConfigurationProperties.getString("Executables.ModuleBase")).
                append("singleNeuronTools/genelib/").append(mylibDir).append(";").
                append(SystemConfigurationProperties.getString("Executables.ModuleBase")).
                append("singleNeuronTools/genelib/").append(mylibDir).append("/sampsepNALoadRaw -nr -pj ").
                append(parentNode.getDirectoryPath()).append(" neuronSeparatorPipeline ").
                append(NeuronSeparatorHelper.addQuotesToCsvString(fileList)).append(commandDelim);
        
        return cmdLine.toString();
	}

    public static String getPostNeuronSeparationCommands(NeuronSeparatorPipelineTask task,
                                                         NeuronSeparatorResultNode parentNode, Entity sample, String commandDelim) throws ServiceException {

        StringBuffer cmdLine = new StringBuffer();

        String inputLsmEntityIdList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);
        String lsmFilePathsFilename = parentNode.getDirectoryPath() + "/" + "lsmFilePaths.txt";

        // In this case, we assume a tiled input in which two lsm files are input
        logger.info("starting getPostNeuronSeparationCommands()");
        if (inputLsmEntityIdList != null && inputLsmEntityIdList.length() > 0) {
            logger.info("Found inputLsmEntityIdList to process - creating lsmFilePaths");

            String[] lsmFilePaths = NeuronSeparatorHelper.getFilePaths(task);

            cmdLine.append("echo '" + lsmFilePaths[0] + "' > " + lsmFilePathsFilename).append(commandDelim);
            cmdLine.append("echo '" + lsmFilePaths[1] + "' >> " + lsmFilePathsFilename).append(commandDelim);
            cmdLine.append(NeuronSeparatorHelper.getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[0])).append(commandDelim);
            cmdLine.append(NeuronSeparatorHelper.getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[1])).append(commandDelim);

        } 
        else {
            logger.info("Assuming raw input - checking for lsm child entities");
            // In this case, we expect a raw input, suggestive of a stitched sample. We will look for lsm files as part of the
            // sample entity tree, and process these.
            Entity supportingData = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SUPPORTING_FILES).getChildEntity();
            if (supportingData == null) {
            	logger.warn("  Sample "+sample.getId()+" has no supporting data!");
            }
            else {
                for (Entity childEntity : supportingData.getChildren()) {
                    logger.info("  Considering entity type="+childEntity.getEntityType().getName());
                    if (childEntity.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                        logger.info("  Creating cmd entries for file="+childEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        String appendString = "' > ";
                        if (cmdLine.length()>0) {
                            appendString = "' >> ";
                        }
                        cmdLine.append("echo '" + childEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH) + appendString + lsmFilePathsFilename).append(commandDelim);
                        cmdLine.append(NeuronSeparatorHelper.getScriptToCreateLsmMetadataFile(parentNode, childEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH))).append(commandDelim);
                    } else {
                        logger.info("  Disregarding the child entity");
                    }
                }
            }
        }

        return cmdLine.toString();
    }
	
    public static String getFileListString(NeuronSeparatorPipelineTask task) throws ServiceException {
        String[] filePaths = getFilePaths(task);
        if (filePaths.length==0) {
            throw new ServiceException("Must have non-zero files as input to neuron separator");
        } else if (filePaths.length==1) {
            return filePaths[0];
        } else {
            StringBuffer sb=new StringBuffer();
            sb.append(filePaths[0]);
            for (int i=1;i<filePaths.length;i++) {
                sb.append(" , " + filePaths[i]);
            }
            return sb.toString();
        }
    }

    public static String[] getFilePaths(NeuronSeparatorPipelineTask task) throws ServiceException {
        AnnotationBeanLocal annotationBean = EJBFactory.getLocalAnnotationBean();
        String lsmEntityIdList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);
        String stitchedEntityId = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputStitchedStackId);

        if (lsmEntityIdList!=null && lsmEntityIdList.trim().length()>0) {
            Entity lsm1;
            Entity lsm2;
            String[] lsmList = lsmEntityIdList.split(",");
            if (lsmList.length!=2) {
                throw new ServiceException("Expected two files in lsmEntityList="+lsmEntityIdList);
            }
            lsm1 = annotationBean.getEntityById(lsmList[0].trim());
            lsm2 = annotationBean.getEntityById(lsmList[1].trim());
            if (lsm1 == null || lsm2 == null) {
                throw new ServiceException("Must provide two LSM stack entities.");
            }
            String[] returnList = new String[2];
            returnList[0] = lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            returnList[1] = lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            return returnList;
        } else if (stitchedEntityId!=null && stitchedEntityId.trim().length()>0) {
            Entity stitchedStack = annotationBean.getEntityById(stitchedEntityId);
            if (stitchedStack==null) {
                throw new ServiceException("Must provide valid stitched stack entity id");
            }
            String[] returnList = new String[1];
            returnList[0] = stitchedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            return returnList;
        } else {
            throw new ServiceException("PARAM_inputLsmEntityIdList or PARAM_inputStitchedStackId must be populated");
        }
    }

    public static String getScriptToCreateLsmMetadataFile(NeuronSeparatorResultNode parentNode, String lsmPath) throws ServiceException {

        File lsmFile = new File(lsmPath);
        if (!lsmFile.exists()) {
            throw new ServiceException("Could not find LSM file "+lsmFile.getAbsolutePath());
        }
        File lsmDataFile=new File(parentNode.getDirectoryPath()+"/"+createLsmMetadataFilename(lsmFile)+".metadata");
        String cmdLine = "cd " + parentNode.getDirectoryPath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "singleNeuronTools/lsm_metadata_dump.pl " +
                addQuotes(lsmPath) + " " + addQuotes(lsmDataFile.getAbsolutePath());

        return cmdLine;
    }

    public static String addQuotes(String s) {
    	return "\""+s+"\"";
    }

    public static String addQuotesToCsvString(String csvString) {
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
    
    public static String createLsmMetadataFilename(File lsmFile) {
        return lsmFile.getName().replaceAll("\\s+","_");
    }
    
    public static void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
    
}
