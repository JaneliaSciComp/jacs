
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 28, 2007
 * Time: 10:14:14 PM
 */
public class FeatureAnnotation implements Serializable, IsSerializable {
    private String accession;
    private String annotationID;
    private String annotationType;
    private String description;
    private String evidence;
    private String evidenceCode;
    private Float evidencePct;
    private String assignedBy;

    public FeatureAnnotation() {
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getAnnotationID() {
        return annotationID;
    }

    public void setAnnotationID(String annotationID) {
        this.annotationID = annotationID;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode(String evidenceCode) {
        this.evidenceCode = evidenceCode;
    }

    public Float getEvidencePct() {
        return evidencePct;
    }

    public void setEvidencePct(Float evidencePct) {
        this.evidencePct = evidencePct;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

}
