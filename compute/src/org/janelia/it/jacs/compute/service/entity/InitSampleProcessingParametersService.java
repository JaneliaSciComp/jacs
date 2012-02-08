package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.v3d.MergedLsmPair;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts stuff about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleProcessingParametersService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();
        	
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
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	List<String> tags = new ArrayList<String>();
        	List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();
        	
        	for(Entity lsmPairEntity : sampleEntity.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR)) {
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
            	
            	File lsmFile1 = new File(lsmFilepath1);
            	File lsmFile2 = new File(lsmFilepath2);
            	
            	if (!lsmFile1.exists()||!lsmFile1.canRead()) {
            		throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile1.getAbsolutePath());
            	}

            	if (!lsmFile2.exists()||!lsmFile2.canRead()) {
            		throw new FileNotFoundException("LSM file does not exist or is not readable: "+lsmFile2.getAbsolutePath());
            	}

            	
            	File mergedFile = new File(mergeResultNode.getDirectoryPath(), "merged-"+lsmPairEntity.getId()+".v3draw");
            	mergedLsmPairs.add(new MergedLsmPair(lsmFilepath1, lsmFilepath2, mergedFile.getAbsolutePath()));
            	tags.add(lsmPairEntity.getName());
        	}

        	if (mergedLsmPairs.isEmpty()) {
        		throw new Exception("Sample (id="+sampleEntityId+") has no LSM pairs");
        	}

        	File stitchedFile = new File(stitchResultNode.getDirectoryPath(), "stitched-"+sampleEntity.getId()+".v3draw");
        	processData.putItem("STITCHED_FILENAME", stitchedFile.getAbsolutePath());
        	processData.putItem("BULK_MERGE_PARAMETERS", mergedLsmPairs);
        	processData.putItem("NUM_PAIRS", new Long(mergedLsmPairs.size()));
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
