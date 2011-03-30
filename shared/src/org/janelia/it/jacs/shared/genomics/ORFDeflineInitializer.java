/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
