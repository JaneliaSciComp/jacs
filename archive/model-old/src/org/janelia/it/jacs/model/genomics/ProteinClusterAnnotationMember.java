
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 20, 2007
 * Time: 9:00:55 AM
 */
public class ProteinClusterAnnotationMember extends ProteinClusterMember implements Serializable, IsSerializable {

    private String evidence;
    private String externalEvidenceLink;

    public ProteinClusterAnnotationMember() {

    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getExternalEvidenceLink() {
        return externalEvidenceLink;
    }

    public void setExternalEvidenceLink(String externalEvidenceLink) {
        this.externalEvidenceLink = externalEvidenceLink;
    }
}
