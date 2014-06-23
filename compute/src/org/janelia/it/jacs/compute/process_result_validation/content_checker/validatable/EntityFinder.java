package org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.content_checker.FinderException;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.List;

/**
 * Implement this in whatever way is most efficient, to find the entity of the given type.
 * Created by fosterl on 6/18/14.
 */
public interface EntityFinder {
    List<Entity> getChildrenOfType(Entity parentEntity, String type) throws FinderException;
}
