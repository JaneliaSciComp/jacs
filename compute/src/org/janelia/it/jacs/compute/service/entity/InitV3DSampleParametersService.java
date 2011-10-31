package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Extracts and outputs the two filepaths from an LsmPair entity. The parameter must be included in the ProcessData:
 *   SAMPLE_ENTITY_ID
 *   RESULT_FILE_NODE
 * 
 * Output is produced in ProcessData as:
 *   BULK_MERGE_PARAMETERS
 *   STITCHED_FILENAME
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitV3DSampleParametersService implements IService {

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
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
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
            	
            	File mergedFile = new File(sampleResultNode.getDirectoryPath(), "merged-"+lsmPairEntity.getId()+".v3draw");
            	mergedLsmPairs.add(new MergedLsmPair(lsmFilepath1, lsmFilepath2, mergedFile.getAbsolutePath()));
        	}

        	File mergedFile = new File(sampleResultNode.getDirectoryPath(), "stitched-"+sampleEntity.getId()+".v3draw");
        	processData.putItem("BULK_MERGE_PARAMETERS", mergedLsmPairs);
        	processData.putItem("STITCHED_FILENAME", mergedFile.getAbsolutePath());
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
