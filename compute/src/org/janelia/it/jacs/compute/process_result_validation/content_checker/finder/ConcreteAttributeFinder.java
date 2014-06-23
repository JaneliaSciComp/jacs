package org.janelia.it.jacs.compute.process_result_validation.content_checker.finder;

import org.janelia.it.jacs.compute.process_result_validation.content_checker.FinderException;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable.EntityFinder;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.List;

/**
 * Use proprietary database schema to find the attribute of the type given.
 * Created by fosterl on 6/19/14.
 */
public class ConcreteAttributeFinder implements EntityFinder {
    @Override
    public List<Entity> getChildrenOfType(Entity parentEntity, String type) throws FinderException {
        try {
            return null;
        } catch ( Exception ex ) {
            throw new FinderException( "Failed to attribute under " + parentEntity + " with name " + type, ex );
        }

    }
}
