package org.janelia.it.jacs.compute.process_result_validation.content_checker;

import java.util.Map;

/**
 * Implement this to make the root validatable, from which entire hierarchy can be obtained.
 * Created by fosterl on 6/24/14.
 */
public interface ValidatableCollectionFactory {
    Map<String,PrototypeValidatable> getValidatables();
}
