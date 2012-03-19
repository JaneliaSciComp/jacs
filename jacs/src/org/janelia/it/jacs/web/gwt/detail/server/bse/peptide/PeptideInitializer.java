
package org.janelia.it.jacs.web.gwt.detail.server.bse.peptide;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Protein;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.shared.genomics.SequenceEntityFactory;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityInitializer;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl;

/**
 * This is the peptide initializer;
 */
public class PeptideInitializer extends BSEntityInitializer {

    public PeptideInitializer(BSEntityServiceImpl bseService) {
        super(bseService);
    }

    public boolean checkEntityTypeAccession(BaseSequenceEntity bse) {
        return AccessionIdentifierUtil.isPeptide(bse.getAccession()) && bse instanceof Protein;
    }

    public boolean recognizeAccessionNo(String accessionNo) {
        return AccessionIdentifierUtil.isPeptide(accessionNo);
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity retrieveBseEntity(String accession)
            throws DaoException {
        Protein protein = null;
        BaseSequenceEntity bsEntity = super.retrieveBseEntity(accession);
        if (bsEntity != null) {
            if (bsEntity instanceof Protein) {
                protein = (Protein) bsEntity;
            }
            else {
                protein = (Protein) SequenceEntityFactory.duplicateSequenceEntity(bsEntity);
            }
        }
        return protein;
    }

}
