
package org.janelia.it.jacs.shared.genomics;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Protein;

import java.util.Map;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
public class ProteinDeflineInitializer extends SequenceEntityDeflineInitializer {

    public ProteinDeflineInitializer() {
        super();
    }

    public void initialize(BaseSequenceEntity entity, Map deflineMap) {
        Protein protein = (Protein) entity;
        setProteinAttributes(protein, deflineMap);
    }

    /**
     * sets Protein alignment attributes
     *
     * @param protein    the entity of interest
     * @param deflineMap the entity's defLine
     */
    private void setProteinAttributes(Protein protein, Map deflineMap) {
        String orfAccNo = (String) deflineMap.get("orf_id");
        if (orfAccNo != null && orfAccNo.length() > 0) {
            protein.setOrfAcc(orfAccNo);
        }
        String dnaAccNo = (String) deflineMap.get("read_id");
        if (dnaAccNo == null || dnaAccNo.length() == 0) {
            dnaAccNo = (String) deflineMap.get("source_dna_id");
        }
        if (dnaAccNo != null && dnaAccNo.length() > 0) {
            protein.setDnaAcc(dnaAccNo);
        }
    }

}
