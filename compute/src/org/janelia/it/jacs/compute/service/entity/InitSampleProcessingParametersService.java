package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleProcessingParametersService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected FileDiscoveryHelper helper;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            helper = new FileDiscoveryHelper(user);
            
        	FileNode sampleResultNode = (FileNode)processData.getItem("SAMPLE_RESULT_FILE_NODE");
        	if (sampleResultNode == null) {
        		throw new IllegalArgumentException("SAMPLE_RESULT_FILE_NODE may not be null");
        	}
        	
        	FileNode mergeResultNode = (FileNode)processData.getItem("MERGE_RESULT_FILE_NODE");
        	if (mergeResultNode == null) {
        		throw new IllegalArgumentException("MERGE_RESULT_FILE_NODE may not be null");
        	}

        	FileNode stitchResultNode = (FileNode)processData.getItem("STITCH_RESULT_FILE_NODE");
        	if (stitchResultNode == null) {
        		throw new IllegalArgumentException("STITCH_RESULT_FILE_NODE may not be null");
        	}
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();
        	
        	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
        	if (supportingFiles == null) {
        		throw new IllegalStateException("Sample does not have Supporting Files child: "+sampleEntityId);
        	}
        	
        	List<Entity> lsmStackPairs = supportingFiles.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR, true);
        	
        	if (lsmStackPairs.isEmpty()) {
        		for(Entity lsmStack : sampleEntity.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK, true)) {
        			String lsmFilepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                	File lsmFile = new File(lsmFilepath);
                	if (!lsmFile.exists()||!lsmFile.canRead()) {
                		throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile.getAbsolutePath());
                	}
                	File mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-"+lsmStack.getId()+".v3draw");
                	mergedLsmPairs.add(new MergedLsmPair(lsmFilepath, null, mergedFile.getAbsolutePath()));
        		}
        	}
        	else {
            	for(Entity lsmPairEntity : lsmStackPairs) {
            		String lsmFilepath1 = null;
            		String lsmFilepath2 = null;
            		
                	boolean gotFirst = false;
                	for(EntityData ed : lsmPairEntity.getOrderedEntityData()) {
                		Entity lsmStack = ed.getChildEntity();
                		if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                			String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                			if (gotFirst) {
                				lsmFilepath2 = filepath;
                			}
                			else {
                				lsmFilepath1 = filepath;
                            	gotFirst = true;
                			}	
                		}
                	}
                	
                	File mergedFile = null;
                		
                	File lsmFile1 = new File(lsmFilepath1);
                	if (!lsmFile1.exists()||!lsmFile1.canRead()) {
                		throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile1.getAbsolutePath());
                	}
                	
                	
                	if (lsmFilepath2!=null) {
                		File lsmFile2 = new File(lsmFilepath2);	
                    	if (!lsmFile2.exists()||!lsmFile2.canRead()) {
                    		throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile2.getAbsolutePath());
                    	}
                    	mergedFile = new File(mergeResultNode.getDirectoryPath(), "merged-"+lsmPairEntity.getId()+".v3draw");
                	}
                	else {
                		// lsmFilepath2 may be null
                    	mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-"+lsmPairEntity.getId()+".v3draw");	
                	}
                	
                	mergedLsmPairs.add(new MergedLsmPair(lsmFilepath1, lsmFilepath2, mergedFile.getAbsolutePath()));
            	}

        	}

        	if (mergedLsmPairs.isEmpty()) {
        		throw new Exception("Sample (id="+sampleEntityId+") has no LSM pairs");
        	}

        	File stitchedFile = new File(stitchResultNode.getDirectoryPath(), "stitched-"+sampleEntity.getId()+".v3draw");
        	
        	List<String> stackFilenames = new ArrayList<String>();
        	stackFilenames.add(stitchedFile.getAbsolutePath());
        	for (MergedLsmPair mergedLsmPair : mergedLsmPairs) {
        		stackFilenames.add(mergedLsmPair.getMergedFilepath());
        	}
        	
        	processData.putItem("STITCHED_FILENAME", stitchedFile.getAbsolutePath());
        	processData.putItem("BULK_MERGE_PARAMETERS", mergedLsmPairs);
        	processData.putItem("STACK_FILENAMES", stackFilenames);
        	processData.putItem("NUM_PAIRS", new Long(mergedLsmPairs.size()));
        	processData.putItem("RUN_MERGE", Boolean.TRUE);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
