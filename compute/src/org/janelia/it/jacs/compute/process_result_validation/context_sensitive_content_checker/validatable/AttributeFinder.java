package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.FinderException;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created by fosterl on 6/19/14.
 */
public interface AttributeFinder {
    String getAttribute( Entity entity, String attributeName ) throws FinderException;
}
