package org.janelia.it.jacs.compute.service.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.vaa3d.CombinedFile;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Extracts the information necessary to run the intersection service.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitScreenSampleCrossParametersService implements IService {

    protected Logger logger;
    protected FileNode resultFileNode;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            
        	String sampleEntity1IdStr = (String)processData.getItem("SCREEN_SAMPLE_1_ID_LIST");
        	if (StringUtils.isEmpty(sampleEntity1IdStr)) {
        		throw new IllegalArgumentException("SCREEN_SAMPLE_1_ID_LIST may not be null");
        	}
        	
        	String sampleEntity2IdStr = (String)processData.getItem("SCREEN_SAMPLE_2_ID_LIST");
        	if (StringUtils.isEmpty(sampleEntity2IdStr)) {
        		throw new IllegalArgumentException("SCREEN_SAMPLE_2_ID_LIST may not be null");
        	}
        	
        	String[] sampleEntity1Ids = sampleEntity1IdStr.split(",");
        	String[] sampleEntity2Ids = sampleEntity2IdStr.split(",");
        	
        	if (sampleEntity1Ids.length != sampleEntity2Ids.length) {
        		throw new IllegalArgumentException("SCREEN_SAMPLE_1_ID_LIST must contain the same number of ids as SCREEN_SAMPLE_2_ID_LIST");
        	}
        	
        	List<CombinedFile> filePairs = new ArrayList<CombinedFile>();
        	
        	for(int i=0; i<sampleEntity1Ids.length; i++) {
        		long s1 = Long.parseLong(sampleEntity1Ids[i]);
        		long s2 = Long.parseLong(sampleEntity2Ids[i]);

            	Entity sampleEntity1 = EJBFactory.getLocalEntityBean().getEntityTree(s1);
            	if (sampleEntity1 == null) {
            		throw new IllegalArgumentException("Sample entity not found with id="+s1);
            	}
            	
            	Entity sampleEntity2 = EJBFactory.getLocalEntityBean().getEntityTree(s2);
            	if (sampleEntity2 == null) {
            		throw new IllegalArgumentException("Sample entity not found with id="+s2);
            	}	

            	String filepath1 = getHeatmapFilename(sampleEntity1);
            	String filepath2 = getHeatmapFilename(sampleEntity2);
            	String output = resultFileNode.getFilePath("intersection_"+i+".v3dpbd");
            	filePairs.add(new CombinedFile(filepath1, filepath2, output));
        	}
        	
        	processData.putItem("FILE_PAIRS", filePairs);
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private String getHeatmapFilename(Entity screenSample) throws Exception {
    	EntityData paEd = EntityUtils.findChildEntityDataWithName(screenSample, "Pattern Annotation");
    	if (paEd==null) {
    		throw new IllegalArgumentException("Screen sample (id="+screenSample+") does not have Pattern Annotation");
    	}
    	
    	Entity patternAnnotation = paEd.getChildEntity();
    	EntityData heatmapEd = EntityUtils.findChildEntityDataWithNameAndType(patternAnnotation, "Heatmap", EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
    	if (heatmapEd==null) {
    		throw new IllegalArgumentException("Screen sample (id="+screenSample+") does not have Heatmap");
    	}
    	
    	Entity heatmap = heatmapEd.getChildEntity();
    	return heatmap.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    }
}
