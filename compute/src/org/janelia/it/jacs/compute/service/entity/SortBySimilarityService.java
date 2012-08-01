package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Sort a folder of images by similarity score to a target image.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SortBySimilarityService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
        	
        	String mode = (String)processData.getItem("MODE");
        	if (mode == null || "".equals(mode)) {
        		throw new IllegalArgumentException("MODE may not be null");
        	}

        	String folderId = (String)processData.getItem("FOLDER_ID");
        	if (folderId == null) {
        		throw new IllegalArgumentException("FOLDER_ID may not be null");
        	}
        	
        	Entity folder = entityBean.getEntityById(folderId);
    		entityBean.loadLazyEntity(folder, false);
        	
        	if ("SETUP".equals(mode)) {

        		FileNode resultFileNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
            	if (resultFileNode == null) {
            		throw new IllegalArgumentException("RESULT_FILE_NODE may not be null");
            	}
            	
            	String targetStackId = (String)processData.getItem("TARGET_STACK_ID");
            	if (targetStackId == null) {
            		throw new IllegalArgumentException("TARGET_STACK_ID may not be null");
            	}

            	Entity targetStack = entityBean.getEntityById(targetStackId);
            	
            	
            	String targetStackFilepath = targetStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            	String inputListFilepath = resultFileNode.getFilePath("inputList.txt");
            	String outputListFilepath = resultFileNode.getFilePath("outputList.txt");
            	
            	File inputListFile = new File(inputListFilepath);
            	FileWriter writer = new FileWriter(inputListFile);
            	
            	for(Entity child : folder.getChildren()) {
            		String filepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            		writer.write(filepath+"\n");
            	}
            	
            	writer.close();

            	logger.info("Putting '"+targetStackFilepath+"' in TARGET_STACK_FILEPATH");
            	processData.putItem("TARGET_STACK_FILEPATH", targetStackFilepath);

            	logger.info("Putting '"+inputListFilepath+"' in INPUT_LIST_FILEPATH");
            	processData.putItem("INPUT_LIST_FILEPATH", inputListFilepath);
            	
            	logger.info("Putting '"+outputListFilepath+"' in OUTPUT_LIST_FILEPATH");
            	processData.putItem("OUTPUT_LIST_FILEPATH", outputListFilepath);
        	}
        	else if ("RESORT".equals(mode)) {

            	String outputListFilepath = (String)processData.getItem("OUTPUT_LIST_FILEPATH");
            	if (outputListFilepath == null) {
            		throw new IllegalArgumentException("OUTPUT_LIST_FILEPATH may not be null");
            	}
            	
        		final Map<String, Double> scores = new HashMap<String, Double>();
        		int s = 0;
        		Scanner scanner = new Scanner(new File(outputListFilepath));
                while (scanner.hasNextLine()){
                    String[] parts = scanner.nextLine().split(" ");
                    if (parts.length==2) {
	                    String scoreStr = parts[0];
	                    String filepath = parts[1];
	                    scores.put(filepath, Double.parseDouble(scoreStr));
	                    s++;
                    }
                }
                
        		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(folder);
        		Collections.sort(eds, new Comparator<EntityData>() {
					@Override
					public int compare(EntityData o1, EntityData o2) {
						String fp1 = o1.getChildEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
						String fp2 = o2.getChildEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
						Double s1 = scores.get(fp1);
						Double s2 = scores.get(fp2);
						if (s1==null&&s2==null) return 0;
						if (s1==null) return 1;
						if (s2==null) return -1;
						if (s1<0&&s1<0) return s2.compareTo(s1);
						if (s1<0) return -1;
						if (s2<0) return 1;
						return s1.compareTo(s2);
					}
				});

                logger.info("Got "+s+" scores for "+eds.size()+" images");
                
        		int index = 0;
        		int numUpdated = 0;
            	for(EntityData ed : eds) {
            		if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
            			ed.setOrderIndex(index);
        				entityBean.saveOrUpdateEntityData(ed);
        				numUpdated++;
            		}
            		index++;
            	}
            	
                logger.info("Updated "+numUpdated+" order indicies");
        	}
        	else {
        		throw new IllegalArgumentException("Unknown mode: "+mode);
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
