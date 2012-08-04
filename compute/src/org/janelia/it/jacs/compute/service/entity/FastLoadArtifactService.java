package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Create fast load artifacts for existing neuron separations.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadArtifactService implements IService {

    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
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
            else if (mode.equals(MODE_COMPLETE)) {
                doComplete();
            } 
            else {
                logger.error("Do not recognize mode type="+mode);
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
        
        List<String> inputDirs = new ArrayList<String>();
        for(Entity sample : entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE)) {
        	logger.info("Processing "+sample.getName());
        	entityBean.loadLazyEntity(sample, false);
        	for(Entity result : sample.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
        		String dir = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		inputDirs.add(dir);
        	}
        }
        
        List<List<String>> inputGroups = createGroups(inputDirs, GROUP_SIZE);
        processData.putItem("INPUT_PATH_LIST", inputGroups);
        
		logger.info("Processed "+inputDirs.size()+" entities into "+inputGroups.size()+" groups.");
    }

    private List<List<String>> createGroups(Collection<String> fullList, int groupSize) {
        List<List<String>> groupList = new ArrayList<List<String>>();
        List<String> currentGroup = null;
        for (String s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList<String>();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList<String>();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }
        
    private void doComplete() throws ComputeException {

    	// TODO: import some of the fast load artifacts as entities, if required
    }
}
