package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker;

/**
 * Created by fosterl on 6/18/14.
 */
public enum ValidationType {
    Entity, Attribute, File;

    public int specialHashCode() {
        switch (this) {
            case Entity:
                return 10;
            case Attribute:
                return 20;
            case File:
                return 30;
            default:
                throw new IllegalStateException("Hashcode impl is broken.  Please fix.");
        }
    }
}
