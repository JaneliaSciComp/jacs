package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Finds the 2 channel LSM for the given Sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitTwoChannelSampleService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();

            FileNode resultFileNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
            
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	Entity latestProcessing = sampleEntity.getLatestChildOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
        	Entity supportingFiles = EntityUtils.getSupportingData(latestProcessing);
        	
        	Entity merged = null;
        	for(Entity child : supportingFiles.getChildren()) {
        		if (child.getName().startsWith("merged-") && child.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
        			merged = child;
        			break;
        		}
        	}

        	if (merged==null) {
        		throw new IllegalArgumentException("Sample entity has no merged result");
        	}

        	String mergedFilepath = merged.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	processData.putItem("MERGED_FILE", mergedFilepath);
        	
        	String sampleFilepath = resultFileNode.getDirectoryPath()+"/TwoChanSample-"+sampleEntityId+".v3draw";
        	processData.putItem("SAMPLE_FILE", sampleFilepath);
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
//    private static int getNumberOfChannels(String metadataFilepath) throws Exception {
//    	File metadataFile = new File(metadataFilepath);
//		Scanner scanner = new Scanner(metadataFile);
//		while (scanner.hasNextLine()) {
//			
//			String match = scanner.findInLine("RECORDING_ENTRY_IMAGES_NUMBER_CHANNELS: ");
//			
//			if (match != null) {
//				int numChan = scanner.nextInt();
//				return numChan;
//			}
//			else {
//				scanner.nextLine();
//			}
//		}
//		
//		scanner.close();
//		return 0;
//    }
}
