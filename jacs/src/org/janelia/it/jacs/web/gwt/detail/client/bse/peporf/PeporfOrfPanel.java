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

package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.EntityType;
import org.janelia.it.jacs.model.genomics.ORF;
import org.janelia.it.jacs.model.genomics.Protein;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:56:01 PM
 */
public class PeporfOrfPanel extends PeporfEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfOrfPanel");

    public PeporfOrfPanel(PeporfPanel parentPanel) {
        super(parentPanel);
    }

    public void initialize() {
        super.initialize("ORF Details", "peptideORFDetailPanel");
    }

    public void display() {
        logger.debug("PeporfOrfPanel display...");
        SequenceDetailsTableBuilder baseBuilder = parentPanel.getBaseEntityTableBuilder();
        BaseSequenceEntity bse = baseBuilder.getBaseEntity();
        int baseTypeCode = bse.getEntityType().getCode();
        RowIndex rowIndex = null;
        if (baseTypeCode == EntityType.ENTITY_CODE_ORF) {
            tableBuilder = baseBuilder;
            String orfAcc = parentPanel.getAcc();
            rowIndex = tableBuilder.populateCAMERAAccNo(orfAcc, PeporfPanel.ORF_DETAIL_TYPE, null);
        }
        else if (baseTypeCode == EntityType.ENTITY_CODE_PROTEIN) {
            Protein protein = (Protein) bse;
            String orfAcc = protein.getOrfAcc();
            ORF proteinORF = protein.getOrfEntity();
            if (orfAcc == null || orfAcc.length() == 0) {
                setVisible(false);
            }
            else {
                tableBuilder = new SequenceDetailsTableBuilder(dataTable, parentPanel);
                if (proteinORF != null) {
                    // if we already have the ORF display it, otherwise we'll retrieve it later
                    tableBuilder.setBaseEntity(proteinORF);
                    rowIndex = tableBuilder.populateCAMERAAccNoAsTargetLink("Peptide details",
                            tableBuilder.getBaseEntity().getCameraAcc(), "ORF", null);
                }
            }
        }
        if (rowIndex != null)
            tableBuilder.populateEntityDetails(rowIndex);
    }

}
