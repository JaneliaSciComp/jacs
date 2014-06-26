package org.janelia.it.jacs.compute.process_result_validation.content_checker;

import java.util.HashMap;
import java.util.Map;

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
    private int maxCount = Integer.MAX_VALUE;
    private Map<Relationship,PrototypeValidatable> children;

    /**
     * Maintenance chore: make sure this gets updated with all required state if any changes are made.
     * @return one just like this.
     */
    @Override
    public Object clone() {
        PrototypeValidatable v = new PrototypeValidatable();
        v.setValidationType(validationType);
        v.setValidationTypeCategory(validationTypeCategory);
        v.setParent(parent);
        v.setMaxCount(maxCount);
        v.setChildren(children);
        return v;
    }

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
    public Map<Relationship,PrototypeValidatable> getChildren() {
        return children;
    }

    public void addChild( Relationship relationship, PrototypeValidatable child ) {
        if ( children == null ) {
            children = new HashMap<>();
        }
        if ( children.containsKey( relationship ) ) {
            throw new IllegalStateException("Key must be unique.");
        }
        children.put(relationship, child );
    }

    public void setChildren(Map<Relationship,PrototypeValidatable> children) {
        if ( children == null ) {
            this.children = children;
        }
        else {
            throw new IllegalStateException("Children already exist.  Please use add");
        }
    }

    public void addChildren(Map<Relationship,PrototypeValidatable> children) {
        if ( children == null ) {
            setChildren(children);
        }
        else {
            this.children.putAll( children );
        }
    }

    @Override
    public boolean equals(Object other) {
        if ( other == null || ! (other instanceof PrototypeValidatable)) {
            return false;
        }
        else {
            PrototypeValidatable otherPV = (PrototypeValidatable)other;
            return otherPV.getValidationType() == validationType  &&
                   otherPV.getValidationTypeCategory().equals( validationTypeCategory );
        }
    }

    @Override
    public int hashCode() {
        return validationTypeCategory.hashCode() + (validationType.hashCode() * 32);
    }

    /** Max count is the maximum cardinality.  Could be default of unlimited, or 1, for instance. */
    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    /** Describes child relationship. */
    public static class Relationship {
        private String name;
        private int uniqueId;

        /**
         * Construct with all data. Used as a key for finding children.
         *
         * @param name null if any name will do.  Otherwise name of entity.
         * @param count unique sequence member under the parent.
         */
        public Relationship( String name, int count ) {
            this.name = name;
            this.uniqueId = count;
        }

        public static Relationship createNonNamedRelationship( int seqId ) {
            return new Relationship(null, seqId);
        }

        public String getName() {
            return name;
        }

        /** Name may not be specified at all.  May be null. */
        public void setName(String name) {
            this.name = name;
        }

        public boolean equals( Object o ) {
            if ( o == null  ||  !( o instanceof Relationship) ) {
                return false;
            }
            return ((Relationship)o).getUniqueId() == uniqueId;
        }

        public int hashCode() {
            return uniqueId;
        }

        public int getUniqueId() {
            return uniqueId;
        }

        /** This should be unique under one given parent. */
        public void setUniqueId(int uniqueId) {
            this.uniqueId = uniqueId;
        }
    }
}
