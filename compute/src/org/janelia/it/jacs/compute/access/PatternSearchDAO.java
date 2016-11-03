package org.janelia.it.jacs.compute.access;

import org.janelia.it.jacs.shared.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/30/12
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */

public class PatternSearchDAO {
    private static Logger LOG = LoggerFactory.getLogger(PatternSearchDAO.class);
    static int state = PatternAnnotationDataManager.STATE_UNDEFINED;
    static Map<String, PatternAnnotationDataManager> managerMap = new HashMap<>();

    static {
        try {
            state = PatternAnnotationDataManager.STATE_LOADING;
            LOG.info("Setup RelativePatternAnnotationDataManager");
            RelativePatternAnnotationDataManager relativeManager = new RelativePatternAnnotationDataManager();
            relativeManager.setup();
            managerMap.put(relativeManager.getDataManagerType(), relativeManager);

            LOG.info("Setup FuhuiPatternAnnotationDataManager");
            FuhuiPatternAnnotationDataManager fuhuiManager = new FuhuiPatternAnnotationDataManager();
            fuhuiManager.setup();
            managerMap.put(fuhuiManager.getDataManagerType(), fuhuiManager);
            state = PatternAnnotationDataManager.STATE_READY;
        } catch (Exception ex) {
            LOG.error("PatternSearchDAO load exception", ex);
            state = PatternAnnotationDataManager.STATE_ERROR;
        }
    }

    public static synchronized List<DataDescriptor> getDataDescriptors(String type) {
        if (state == PatternAnnotationDataManager.STATE_READY) {
            PatternAnnotationDataManager manager = managerMap.get(type);
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
            List<DataDescriptor> descriptorList = getDataDescriptors(type);
            PatternAnnotationDataManager manager = managerMap.get(type);
            FilterResult filterResult;
            try {
                filterResult = manager.getFilteredResults(filterMap);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                return null;
            }
            return filterResult;
        } else {
            return null;
        }
    }

}
