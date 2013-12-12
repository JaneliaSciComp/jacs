package org.janelia.it.jacs.compute.service.fly;

import java.sql.SQLException;
import java.util.*;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.FlyBoyDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service checks the split lines loaded by the SplitLinesLoadingService against the FlyBoy database. 
 * Any line that is not in FlyBoy is deleted. Any robot id that is missing is updated.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitLinesVerificationService extends AbstractEntityService {
	
	private static final boolean DEBUG = false;
	
    protected Date createDate;
    
    
    protected EntityType folderType;
    
    protected Map<String, String> robotIds = new HashMap<String,String>();
	
    public void execute() throws Exception {
	
        createDate = new Date();
        
    	// Get data from FlyBoy
    	
    	FlyBoyDAO flyboyDAO = new FlyBoyDAO(logger);
    	ResultSetIterator iterator = flyboyDAO.getGMRStockLines();
    	Map<String,Integer> stockNameToRobotId = new HashMap<String,Integer>();

    	try {
    		while (iterator.hasNext()) {
        		Map<String,Object> row = iterator.next();
        		String stockName = (String)row.get("Stock_Name");
        		Integer robotId = (Integer)row.get("RobotID");
        		stockNameToRobotId.put(stockName, robotId);
        	}
    	}
    	catch (RuntimeException e) {
    		if (e.getCause() instanceof SQLException) {
    			throw new DaoException(e);
    		}
    		throw e;
    	}
        finally {
        	if (iterator!=null) iterator.close();
        }
        
        // Verify split lines
        int numSplitLinesDeleted = 0;
        int numSplitLinesLeft = 0;
        
        List<Long> toDelete = new ArrayList<Long>();
        
        for(Entity flyline : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_FLY_LINE)) {
        	Integer robotId = stockNameToRobotId.get(flyline.getName());
        	String splitPart = flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
    		if (splitPart!=null) {
            	if (robotId == null) {
            		// Split line doesn't exist in FlyBoy, so delete it 
            		logger.info("Will delete "+flyline.getName()+" because it was not found in FlyBoy");	
            		toDelete.add(flyline.getId());
            		numSplitLinesDeleted++;
            	}
            	else {
            		// Split line exists, but let's update the robot id
        			setFlylineRobotId(flyline, robotId.toString());
        			numSplitLinesLeft++;
            	}            	
    		}
    		else {
    			if (robotId == null) {
    				// Normal line does not exist in FlyBoy. This shouldn't happen, so produce a warning
            		logger.warn("Non-split fly line does not exist in FlyBoy: "+flyline.getName());
    			}
    			else {
    				// Normal line exists, but let's update the robot id
    				setFlylineRobotId(flyline, robotId.toString());
    			}
    		}
        }
        
        // Delete unwanted lines
        if (!DEBUG) {
            for (Long entityId : toDelete) {
            	entityBean.deleteEntityTree(ownerKey, entityId);
            }
        }

    	logger.info("Deleted "+numSplitLinesDeleted+" split lines, kept "+numSplitLinesLeft);
    }


    private void setFlylineRobotId(Entity flyline, String robotId) throws Exception {
		EntityData currRobotIdEd = flyline.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
		if (currRobotIdEd == null) {
			if (!StringUtils.isEmpty(robotId)) {
				logger.info("FlyLine "+flyline.getName()+" is now associated with robot id "+robotId);
				flyline.setValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID, robotId);
				if (!DEBUG) {
					entityBean.saveOrUpdateEntity(flyline);	
				}
			}
		}
    }
}
