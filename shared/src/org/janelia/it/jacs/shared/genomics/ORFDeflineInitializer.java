
package org.janelia.it.jacs.shared.genomics;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.ORF;

import java.util.Map;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
public class ORFDeflineInitializer extends SequenceEntityDeflineInitializer {

    public ORFDeflineInitializer() {
        super();
    }

    public void initialize(BaseSequenceEntity entity, Map deflineMap) {
        ORF orf = (ORF) entity;
        setORFAttributes(orf, deflineMap);
    }

    /**
     * sets ORF alignment attributes
     *
     * @param orf        the entity of interest
     * @param deflineMap the entity's defLine
     */
    private void setORFAttributes(ORF orf, Map deflineMap) {
        String translationStart = (String) deflineMap.get("translation_start");
        if (translationStart == null || translationStart.length() == 0) {
            translationStart = (String) deflineMap.get("begin");
        }
        if (translationStart != null && translationStart.length() > 0) {
            orf.setDnaBegin(Integer.valueOf(translationStart));
        }
        String translationEnd = (String) deflineMap.get("translation_end");
        if (translationEnd == null || translationEnd.length() == 0) {
            translationEnd = (String) deflineMap.get("end");
        }
        if (translationEnd != null && translationEnd.length() > 0) {
            orf.setDnaEnd(Integer.valueOf(translationEnd));
        }
        String translationTable = (String) deflineMap.get("ttable");
        if (translationTable != null && translationTable.length() > 0) {
            orf.setTranslationTable(translationTable);
        }
        String orientation = (String) deflineMap.get("orientation");
        if (orientation != null) {
            if (orientation.equals("reverse")) {
                orf.setDnaOrientation(-1);
            }
            else if (orientation.equals("forward")) {
                orf.setDnaOrientation(1);
            }
            else {
                orf.setDnaOrientation(Integer.valueOf(orientation));
            }
        }
        String dnaAccNo = (String) deflineMap.get("read_id");
        if (dnaAccNo == null || dnaAccNo.length() == 0) {
            dnaAccNo = (String) deflineMap.get("source_dna_id");
        }
        if (dnaAccNo != null && dnaAccNo.length() > 0) {
            orf.setDnaAcc(dnaAccNo);
        }
        String peptideAcc = (String) deflineMap.get("pep_id");
        if (peptideAcc == null || peptideAcc.length() == 0) {
            peptideAcc = (String) deflineMap.get("peptide_id");
        }
        if (peptideAcc != null && peptideAcc.length() > 0) {
            orf.setProteinAcc(peptideAcc);
        }
    }

}
