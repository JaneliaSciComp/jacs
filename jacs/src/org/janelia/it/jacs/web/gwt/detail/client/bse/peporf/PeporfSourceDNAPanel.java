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

import com.google.gwt.user.client.ui.FlexTable;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:55:14 PM
 */
public class PeporfSourceDNAPanel extends PeporfEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfSourceDNAPanel");
    int sequenceBegin = 0;
    int sequenceEnd = 0;

    private class DnaSequenceDetailsTableBuilder extends SequenceDetailsTableBuilder {
        private DnaSequenceDetailsTableBuilder(FlexTable entityDetailsTable, BSEntityPanel parentPanel) {
            super(entityDetailsTable, parentPanel);
        }

        protected void populateSequenceLabelColumn(RowIndex rowIndex, String sequenceLabel) {
            populateSequenceLabelColumn(rowIndex, sequenceLabel, "ORF Range", "readDetailClearRangeTip");
        }
    }

    public PeporfSourceDNAPanel(PeporfPanel parentPanel) {
        super(parentPanel);
    }

    public void initialize() {
        super.initialize("Source DNA Details", "peptideDNADetailPanel");
    }

    public void display() {
        logger.debug("PeporfSourceDNAPanel display...");
        SequenceDetailsTableBuilder baseBuilder = parentPanel.getBaseEntityTableBuilder();
        BaseSequenceEntity bse = baseBuilder.getBaseEntity();
        int baseTypeCode = bse.getEntityType().getCode();
        if (baseTypeCode != EntityTypeGenomic.ENTITY_CODE_PROTEIN &&
                baseTypeCode != EntityTypeGenomic.ENTITY_CODE_ORF) {
            setVisible(false);
            return;
        }
        tableBuilder = new DnaSequenceDetailsTableBuilder(dataTable, parentPanel);
        String naAcc = null;
        Nucleotide naEntity = null;
        sequenceBegin = 0;
        sequenceEnd = 0;
        if (baseTypeCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
            Protein protein = (Protein) bse;
            if (protein.getDnaBegin() != null) {
                sequenceBegin = protein.getDnaBegin();
            }
            if (protein.getDnaEnd() != null) {
                sequenceEnd = protein.getDnaEnd();
            }
            naAcc = protein.getDnaAcc();
            naEntity = protein.getDnaEntity();
        }
        else { // ORF
            ORF orf = (ORF) bse;
            if (orf.getDnaBegin() != null) {
                sequenceBegin = orf.getDnaBegin();
            }
            if (orf.getDnaEnd() != null) {
                sequenceEnd = orf.getDnaEnd();
            }
            naAcc = orf.getDnaAcc();
            naEntity = orf.getDnaEntity();
        }
        if (naAcc == null || naAcc.length() == 0) {
            // DNA panel doesn't get created if there are no features
            // so check if the DNA panel exists
            setVisible(false);
        }
        else {
            if (naEntity != null) {
                // if we already have the DNA entity display it, otherwise we'll retrieve it later
                tableBuilder.setBaseEntity(naEntity);
                RowIndex rowIndex = tableBuilder.populateCAMERAAccNoAsTargetLink("Source DNA details",
                        tableBuilder.getBaseEntity().getCameraAcc(),
                        "Source DNA",
                        null);
                tableBuilder.populateEntityDetails(rowIndex);
            }
        }
    }

    protected int getSequenceBegin() {
        return sequenceBegin;
    }

    protected int getSequenceEnd() {
        return sequenceEnd;
    }

}
