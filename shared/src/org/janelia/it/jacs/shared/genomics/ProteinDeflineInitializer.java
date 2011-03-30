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
