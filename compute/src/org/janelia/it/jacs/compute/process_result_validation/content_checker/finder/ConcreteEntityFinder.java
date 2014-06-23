package org.janelia.it.jacs.compute.process_result_validation.content_checker.finder;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.EntityFinder;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses JACS information/database, to find the entit(ies) of the type specified.
 * Created by fosterl on 6/19/14.
 */
public class ConcreteEntityFinder implements EntityFinder {
    private Logger logger = Logger.getLogger(ConcreteEntityFinder.class);
    @Override
    public List<Entity> getChildrenOfType(Entity parentEntity, String type) {
        List<Entity> rtnList = new ArrayList<>();
        try {
        } catch ( Exception ex ) {
            logger.error("Failed to find entities for " + parentEntity, ex);
        }
        return rtnList;
    }
}
