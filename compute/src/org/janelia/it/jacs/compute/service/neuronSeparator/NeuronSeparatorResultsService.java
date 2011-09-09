package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Process the results of a Neuron Separation task and create the required entities, symbolic links, and other 
 * auxiliary files.
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparatorResultsService implements IService {

    private static final String REMOTE_SERVER = SystemConfigurationProperties.getString("Remote.Work.Server.Mac");
    private static final String REMOTE_LINKING_SCRIPT = "symlink.mac.sh";
    
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
            String sampleEntityId = task.getParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId);
            String symbolicLinkName = task.getParameter(NeuronSeparatorPipelineTask.PARAM_symbolLinkName);
            Entity sample = annotationBean.getEntityTree(new Long(sampleEntityId.trim()));

            boolean evidenceOfSuccessfulCompletion=false;

            if (sample == null) {
                throw new ServiceException("Must provide Sample entity.");
            }

        	logger.info("Processing results for sample "+sample.getName());
        	
            // Create the other files that are necessary

            String cmdLine = NeuronSeparatorHelper.getPostNeuronSeparationCommands(task, parentNode, sample, " ; ");

            if (cmdLine!=null && cmdLine.length()>0) {

                StringBuffer stdout = new StringBuffer();
                StringBuffer stderr = new StringBuffer();
                SystemCall call = new SystemCall(logger, stdout, stderr);
                int exitCode = call.emulateCommandLine(cmdLine.toString(), true, 60);

                File outFile = new File(parentNode.getDirectoryPath(), "stdout");
                if (stdout.length() > 0) FileUtils.writeStringToFile(outFile, stdout.toString(), true);

                File errFile = new File(parentNode.getDirectoryPath(), "stderr");
                if (stderr.length() > 0) FileUtils.writeStringToFile(errFile, stderr.toString(), true);

                if (0 != exitCode) {
                    throw new ServiceException("NeuronSeparatorResultsService failed with exitCode " + exitCode + " for resultDir=" + parentNode.getDirectoryPath());
                }

            }
            
            // Create the result entity and populate with the output files
        	
            Entity resultEntity = createResultEntity();
            
            addToParent(sample, resultEntity, sample.getMaxOrderIndex()+1);

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
                else if (filename.startsWith("Signal_") && filename.endsWith(".tif")) {
                    addResultItem(resultEntity, tif3D, resultFile);
                }
                else if (filename.equals("ConsolidatedLabel.tif")) {
                    evidenceOfSuccessfulCompletion=true;
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
                else {
                    // ignore other files
                }
            }

            File symbolicLink = new File(symbolicLinkName);

            logger.info("Preparing to verify or create symbolicLink="+symbolicLink);
            
            // Create the user-space symbolic link to the result directory
            if (symbolicLink.exists()) {
            	try {
            		symbolicLink.delete();	
                	logger.info("  Deleted existing link to result directory: "+symbolicLink.getAbsolutePath());
            	}
            	catch (Exception e) {
                	logger.info("  Could not delete existing link to result directory: "+symbolicLink.getAbsolutePath(),e);
            	}
            }

        	String target = NeuronSeparatorHelper.covertPathsToVolumeMounted(resultDir.getAbsolutePath());
        	String link = NeuronSeparatorHelper.covertPathsToRemoteServer(symbolicLink.getAbsolutePath());

            if (evidenceOfSuccessfulCompletion) {

                logger.info("Creating symbolic link with target=" + target + " link=" + link);

                try {
                    createLink(target, link);
                    logger.info("  Created symbolic link for results at: " + symbolicLink.getAbsolutePath());
                } catch (Exception e) {
                    logger.info("  Could not create symbolic link for results at " + symbolicLink.getAbsolutePath()
                            + " because: " + e.getMessage());
                }

            } else {
                logger.info("Skipping generation of link due to lack of evidence of pipeline success");
            }

            // TODO: migrate the annotations from the previous result

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    /**
     * Create a symbolic link using the REMOTE_SERVER. This is useful for creating OS X symbolic links which 
     * function as aliases.
     * @param target
     * @param symbolicLink
     * @throws Exception
     */
    private void createLink(String target, String symbolicLink) throws Exception {
    	if (REMOTE_SERVER == null || "".equals(REMOTE_SERVER)) {
    		throw new Exception("Cannot create symbolic link: no REMOTE_SERVER defined in system configuration.");
    	}
    	
    	String script = "ln -s "+escapeFilepathForCommandLine(target)+ " "+escapeFilepathForCommandLine(symbolicLink);

    	File scriptFile = new File(parentNode.getDirectoryPath(), REMOTE_LINKING_SCRIPT);
    	FileUtils.writeStringToFile(scriptFile, script);

        String cmdLine = "ssh "+REMOTE_SERVER+" sh "+NeuronSeparatorHelper.covertPathsToRemoteServer(scriptFile.getAbsolutePath());
        
        StringBuffer stdout = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        SystemCall call = new SystemCall(logger, stdout, stderr);
        int exitCode = call.emulateCommandLine(cmdLine, true, 20);
        
    	File outFile = new File(parentNode.getDirectoryPath(), "stdout");
    	if (stdout.length() > 0) FileUtils.writeStringToFile(outFile, stdout.toString(), true);

        File errFile = new File(parentNode.getDirectoryPath(), "stderr");
        if (stderr.length() > 0) FileUtils.writeStringToFile(errFile, stderr.toString(), true);
        
        if (0!=exitCode) {
        	throw new Exception("Could not create symlink");
        }
    }
    
    /**
     * Escapes special characters in a file path for use in a shell command. Does not escape /'s or $'s. 
     */
    private String escapeFilepathForCommandLine(String path) {
    	return path.replaceAll("(\\W)", "\\\\$1").replaceAll("\\\\/", "/").replaceAll("\\\\\\$", "\\$");
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
        computeBean.genericSave(ed);
        logger.info("Added " + entity.getEntityType().getName() + "#" + entity.getId() + " as child of " + parent.getEntityType().getName() + "#" + entity.getId());
    }

}