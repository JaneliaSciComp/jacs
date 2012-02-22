package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/18/11
 * Time: 10:19 AM
 */
public class NeuronSeparatorHelper {

	private static final Logger logger = Logger.getLogger(NeuronSeparatorHelper.class);

//    protected static final String SEPARATOR_BASE_CMD = "export LD_LIBRARY_PATH="+
//    		SystemConfigurationProperties.getString("Separator.LDLibraryPath")+":$LD_LIBRARY_PATH\n"+
//            SystemConfigurationProperties.getString("Executables.ModuleBase") +
//            SystemConfigurationProperties.getString("Separator.CMD");

    protected static final String SEPARATOR_SCRIPT = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("Separator.ScriptPath");
    
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

    /**
     * @deprecated Use the method with ProcessData instead
     */
	public static String getNeuronSeparationCommands(NeuronSeparatorPipelineTask task, 
			FileNode parentNode, String mylibDir, String commandDelim) throws ServiceException {
        String fileList;
        if (null!=task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath)&&
            !"".equals(task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath))) {
            fileList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath);
        }
        else {
            fileList = NeuronSeparatorHelper.getFileListString((NeuronSeparatorPipelineTask)task);
        }
		return getNeuronSeparationCommands(fileList, parentNode, mylibDir, commandDelim);
	}

	public static String getNeuronSeparationCommands(Task task, IProcessData processData,
			FileNode parentNode, String mylibDir, String commandDelim) throws ServiceException {

		if (task!=null&&task instanceof NeuronSeparatorPipelineTask) {
			return getNeuronSeparationCommands((NeuronSeparatorPipelineTask)task, parentNode, mylibDir, commandDelim);
		}
		else {
			String inputFilename = (String)processData.getItem("INPUT_FILENAME");
	        if (inputFilename==null) {
	        	throw new ServiceException("Input parameter INPUT_FILENAME may not be null");
	        }
			return getNeuronSeparationCommands(inputFilename, parentNode, mylibDir, commandDelim);
		}
	}
	
	private static String getNeuronSeparationCommands(String inputFilename,
			FileNode parentNode, String mylibDir, String commandDelim) throws ServiceException {
        
		// TODO: mylibDir no longer does anything... maybe we should inject it into the command string if we intend
		// to run on multiple architectures. 
        StringBuilder cmdLine = new StringBuilder();
        cmdLine.append("cd ").append(parentNode.getDirectoryPath()).append(commandDelim);

        boolean deleteInputWhenDone = false;
    	File inputFile = new File(inputFilename);
        if (inputFilename.endsWith(".zip")) {
        	inputFile = new File(parentNode.getDirectoryPath(), inputFile.getName().replaceFirst("\\.zip$", ""));
        	cmdLine.append("unzip "+inputFilename).append(commandDelim);
        	deleteInputWhenDone = true;
        }
        else if (inputFilename.endsWith(".v3dpbd")) {
        	inputFile = new File(parentNode.getDirectoryPath(), inputFile.getName().replaceAll("v3dpbd", "v3draw"));
        	cmdLine.append(Vaa3DHelper.getHeadlessGridCommandPrefix());
        	cmdLine.append("\n");
        	cmdLine.append(Vaa3DHelper.getFormattedConvertCommand(inputFilename, inputFile.getAbsolutePath(), ""));
        	cmdLine.append("\n");
        	cmdLine.append(Vaa3DHelper.getHeadlessGridCommandSuffix());
        	deleteInputWhenDone = true;
        }

//      cmdLine.append(SEPARATOR_BASE_CMD).append(" ");
//      cmdLine.append("-nr -pj ");
        
        cmdLine.append("sh ").append(SEPARATOR_SCRIPT).append(" ").append(parentNode.getDirectoryPath()).
    			append(" neuronSeparatorPipeline ").append(inputFile.getAbsolutePath()).append(commandDelim);
        
        // Remove the uncompressed file, if necessary.
        if (deleteInputWhenDone) {
        	cmdLine.append("rm ").append(inputFile.getAbsolutePath()).append(commandDelim);
        }
        
        // A little hack to clear core dumps that can be ignored. If the last line in the output has "Kill" in it, 
        // that means we're in the cleanup stage, and core dumps are not important.
        cmdLine.append("lastOut=`tail -n 1 ").append(parentNode.getDirectoryPath()).append("/sge_output/neuSepOutput.1`\n");
        cmdLine.append("if echo \"$lastOut\" |grep -q Kill; then\n");
        cmdLine.append("    echo \"Removing useless core dump\"\n");
        cmdLine.append("    rm ").append(parentNode.getDirectoryPath()).append("/core.*\n");
        cmdLine.append("fi\n");
        
        return cmdLine.toString();
	}

	/**
	 * @deprecated Use the separate CreateLsmMetadataService for this
	 */
    public static String getPostNeuronSeparationCommands(NeuronSeparatorPipelineTask task,
    		FileNode parentNode, Entity sample, String commandDelim) throws ServiceException {
    
        StringBuffer cmdLine = new StringBuffer();

        String inputLsmEntityIdList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);
        String lsmFilePathsFilename = parentNode.getDirectoryPath() + "/" + "lsmFilePaths.txt";

        // In this case, we assume a tiled input in which two lsm files are input
        logger.info("starting getPostNeuronSeparationCommands");
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
	
    private static String getFileListString(NeuronSeparatorPipelineTask task) throws ServiceException {
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

    private static String[] getFilePaths(NeuronSeparatorPipelineTask task) throws ServiceException {
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

    private static String getScriptToCreateLsmMetadataFile(FileNode parentNode, String lsmPath) throws ServiceException {

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

    private static String addQuotes(String s) {
    	return "\""+s+"\"";
    }
    
    private static String createLsmMetadataFilename(File lsmFile) {
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
