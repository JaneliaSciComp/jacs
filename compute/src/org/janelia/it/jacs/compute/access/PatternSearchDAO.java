package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.annotation.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/30/12
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */

public class PatternSearchDAO {
    static Logger logger=Logger.getLogger(PatternSearchDAO.class);
    static int state=PatternAnnotationDataManager.STATE_UNDEFINED;
    static Map<String, PatternAnnotationDataManager> managerMap=new HashMap<String, PatternAnnotationDataManager>();

    static {
        try {
            state = PatternAnnotationDataManager.STATE_LOADING;
            logger.info("Starting RelativePatternAnnotationDataManager load");
            RelativePatternAnnotationDataManager relativeManager = new RelativePatternAnnotationDataManager();
            relativeManager.setup();
            managerMap.put(relativeManager.getDataManagerType(), relativeManager);
            logger.info("Starting FuhuiPatternAnnotationDataManager load");
            FuhuiPatternAnnotationDataManager fuhuiManager = new FuhuiPatternAnnotationDataManager();
            fuhuiManager.setup();
            managerMap.put(fuhuiManager.getDataManagerType(), fuhuiManager);
            state = PatternAnnotationDataManager.STATE_READY;
        }
        catch (Exception ex) {
            logger.error("PatternSearchDAO: "+ex.getMessage());
            ex.printStackTrace();
            state = PatternAnnotationDataManager.STATE_ERROR;
        }
    }

    public static synchronized List<DataDescriptor> getDataDescriptors(String type) {
        if (state==PatternAnnotationDataManager.STATE_READY) {
            PatternAnnotationDataManager manager=managerMap.get(type);
            return manager.getDataDescriptors();
        } else {
            return null;
        }
    }

    public static synchronized int getState() {
        return state;
    }

    public static synchronized List<String> getCompartmentList(String type) {
        if (state==PatternAnnotationDataManager.STATE_READY) {
            PatternAnnotationDataManager manager=managerMap.get(type);
            return manager.getCompartmentListInstance();
        } else {
            return null;
        }
    }

    public static synchronized FilterResult getFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) {
        if (state==PatternAnnotationDataManager.STATE_READY) {
            List<DataDescriptor> descriptorList=getDataDescriptors(type);
            PatternAnnotationDataManager manager=managerMap.get(type);
            FilterResult filterResult;
            try {
                filterResult=manager.getFilteredResults(filterMap);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                return null;
            }
            return filterResult;
        } else {
            return null;
        }
    }

}
