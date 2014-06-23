package org.janelia.it.jacs.compute.process_result_validation.content_checker;

import java.util.List;

/**
 * This has all type information required to drive validation of a specific item.  There should not be any specifics
 * for identity, path, etc. Also, is part of a larger tree of things to be examined.
 *
 * Created by fosterl on 6/18/14.
 */
public class PrototypeValidatable {
    private ValidationType validationType;
    private String validationTypeCategory;
    private PrototypeValidatable parent;
    private List<PrototypeValidatable> children;

    public ValidationType getValidationType() {
        return validationType;
    }
    public void setValidationType( ValidationType validationType ) {
        this.validationType = validationType;
    }

    /**
     * Category can be something like mask file, chan file, specific attribute name.  This is specific to the
     * validatable type, but might be repeated by other instances in different samples.  Not a unique name
     * for the instance.
     * @return name applied, to further categorize validation type.
     */
    public String getValidationTypeCategory() {
        return validationTypeCategory;
    }

    public void setValidationTypeCategory(String itemType) {
        this.validationTypeCategory = itemType;
    }

    /** This can return null. */
    public PrototypeValidatable getParent() {
        return parent;
    }

    public void setParent(PrototypeValidatable parent) {
        this.parent = parent;
    }

    /** This can return null. */
    public List<PrototypeValidatable> getChildren() {
        return children;
    }

    public void setChildren(List<PrototypeValidatable> children) {
        this.children = children;
    }
}
