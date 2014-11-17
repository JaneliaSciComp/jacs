package org.janelia.it.jacs.model.domain.ontology;

public class Interval extends OntologyTerm {

    private Long lowerBound;
    private Long upperBound;

    public Interval() {
    }

    public Interval(Long lowerBound, Long upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (lowerBound.compareTo(upperBound)>=0) {
            throw new IllegalArgumentException("Lower bound must be less than upper bound");
        }
    }

    public boolean allowsChildren() {
        return true;
    }

    public String getTypeName() {
        return "Interval";
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Long lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Long getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Long upperBound) {
        this.upperBound = upperBound;
    }
}
