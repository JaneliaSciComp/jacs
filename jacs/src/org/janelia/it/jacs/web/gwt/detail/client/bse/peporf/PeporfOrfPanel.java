
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
        if (baseTypeCode == EntityTypeGenomic.ENTITY_CODE_ORF) {
            tableBuilder = baseBuilder;
            String orfAcc = parentPanel.getAcc();
            rowIndex = tableBuilder.populateAccessionNo(orfAcc, PeporfPanel.ORF_DETAIL_TYPE, null);
        }
        else if (baseTypeCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
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
                    rowIndex = tableBuilder.populateAccessionNoAsTargetLink("Peptide details",
                            tableBuilder.getBaseEntity().getAccession(), "ORF", null);
                }
            }
        }
        if (rowIndex != null)
            tableBuilder.populateEntityDetails(rowIndex);
    }

}
