package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Create fast load artifacts for existing neuron separations.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadArtifactService implements IService {

	private static final String centralDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
	
	public static final String PARAM_separationId = "separation id";
	
    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final String MODE_CREATE_SINGLE_INPUT_LIST = "CREATE_SINGLE_INPUT_LIST";
    public static final String MODE_COMPLETE = "COMPLETE";
    public static final int GROUP_SIZE = 200;
	
    protected Logger logger;
    protected Task task;
    protected String username;
    protected AnnotationBeanLocal annotationBean;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    
    private String mode = MODE_UNDEFINED;
    protected IProcessData processData;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            username = task.getOwner();
            mode = processData.getString("MODE");
            this.processData = processData;

            if (mode.equals(MODE_CREATE_INPUT_LIST)) {
                doCreateInputList();
            }
            else if (mode.equals(MODE_CREATE_SINGLE_INPUT_LIST)) {
                doCreateSingleInputList();
            }
            else if (mode.equals(MODE_COMPLETE)) {
                doComplete();
            } 
            else {
                logger.error("Unrecognized mode: "+mode);
            }
    	}
        catch (Exception e) {
			logger.info("Encountered an exception. Before dying, we...");
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running MCFODataCompressService", e);
        }
    }
    
    private void doCreateInputList() throws ComputeException {

        logger.info("Finding neuron separations...");
        
        List<Entity> entities = new ArrayList<Entity>();
        for(Entity result : entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
        	logger.info("Processing neuron separation, id="+result.getId());
    		String dir = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		if (!fastLoadDirExists(dir)) {
    			if (!dir.contains(centralDir)) {
    				logger.info("Ignoring entity with path which does not contain the FileStore.CentralDir: "+result.getId());
    			}
    			else {
    				entities.add(result);		
    			}
    		}
        }
        
        List<List> inputGroups = createGroups(entities, GROUP_SIZE);
        processData.putItem("ENTITY_LIST", inputGroups);
		logger.info("Processed "+entities.size()+" entities into "+inputGroups.size()+" groups.");
    }

    private void doCreateSingleInputList() throws ComputeException {
    	
    	String sepId = processData.getString("SEPARATION_ID");
        logger.info("Getting neuron separation "+sepId);

    	if (sepId == null) {
    		throw new IllegalArgumentException("SEPARATION_ID may not be null");
    	}
        
        Entity result = entityBean.getEntityById(sepId);
        
        List<Entity> entities = new ArrayList<Entity>();

        if (!result.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
        	throw new IllegalArgumentException("Entity with id="+sepId+" is not a neuron separation result");
        }
        
		String dir = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

		if (fastLoadDirExists(dir)) {
			logger.info("The fastLoad directory already exists under "+dir);
			return;
		}
		
		if (!dir.contains(centralDir)) {
			throw new ComputeException("Entity path does not contain the FileStore.CentralDir: "+result.getId());
		}
		else {
			entities.add(result);
			processData.putItem("ENTITY_LIST", entities);
		}
    }

    private List<List> createGroups(Collection fullList, int groupSize) {
        List<List> groupList = new ArrayList<List>();
        List currentGroup = null;
        for (Object s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }

    private boolean fastLoadDirExists(String dir) {
    	File fastLoadDir = new File(dir,"fastLoad");
    	return fastLoadDir.exists();
    }
    
    private void doComplete() throws ComputeException {

    	// TODO: import some of the fast load artifacts as entities, if required
    }
}
