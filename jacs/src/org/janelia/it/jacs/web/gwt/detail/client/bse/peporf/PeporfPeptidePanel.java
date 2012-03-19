
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.model.genomics.ORF;
import org.janelia.it.jacs.model.genomics.Protein;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:54:45 PM
 */
public class PeporfPeptidePanel extends PeporfEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPeptidePanel");

    public PeporfPeptidePanel(PeporfPanel parentPanel) {
        super(parentPanel);
    }

    public void initialize() {
        super.initialize("Peptide Details", "orfPeptideDetailPanel");
    }

    public void display() {
        logger.debug("PeporfPeptidePanel display...");
        SequenceDetailsTableBuilder baseBuilder = parentPanel.getBaseEntityTableBuilder();
        BaseSequenceEntity bse = baseBuilder.getBaseEntity();
        int baseTypeCode = bse.getEntityType().getCode();
        RowIndex rowIndex = null;
        if (baseTypeCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN ||
                baseTypeCode == EntityTypeGenomic.ENTITY_CODE_PEPTIDE) {
            tableBuilder = baseBuilder;
            String pepAcc = parentPanel.getAcc();
            rowIndex = tableBuilder.populateAccessionNo(pepAcc, PeporfPanel.PEPTIDE_DETAIL_TYPE, null);
        }
        else if (baseTypeCode == EntityTypeGenomic.ENTITY_CODE_ORF) {
            ORF orf = (ORF) bse;
            String peptideAcc = orf.getProteinAcc();
            if (peptideAcc == null || peptideAcc.length() == 0) {
                setVisible(false);
            }
            else {
                tableBuilder = new SequenceDetailsTableBuilder(dataTable, parentPanel);
                Protein orfPeptide = orf.getProteinEntity();
                if (orfPeptide != null) {
                    // if we already have the peptide display it, otherwise we'll retrieve it later
                    tableBuilder.setBaseEntity(orfPeptide);
                    rowIndex = tableBuilder.populateAccessionNoAsTargetLink("Peptide details",
                            tableBuilder.getBaseEntity().getAccession(), "Peptide", null);
                }
            }
        }
        if (rowIndex != null)
            tableBuilder.populateEntityDetails(rowIndex);
    }

}
