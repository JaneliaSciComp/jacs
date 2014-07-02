package org.janelia.it.jacs.model.domain;

public enum AlignmentScoreType {
    Inconsistency("Alignment Inconsistency Score"),
    InconsistencyByRegion("Alignment Inconsistency Scores"),
    NormalizedCrossCorrelation("Normalized Cross Correlation Score"),
    ModelViolation("Model Violation Score");
    private final String label;
    private AlignmentScoreType(String label) {
        this.label = label;
    }
    public String getLabel() {
        return label;
    }
}
