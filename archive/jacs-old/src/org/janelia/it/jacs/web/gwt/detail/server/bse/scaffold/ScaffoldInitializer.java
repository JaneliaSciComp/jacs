
package org.janelia.it.jacs.web.gwt.detail.server.bse.scaffold;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Scaffold;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.shared.genomics.SequenceEntityFactory;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityInitializer;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl;

/**
 * This is the scaffold initializer;
 */
public class ScaffoldInitializer extends BSEntityInitializer {

    public ScaffoldInitializer(BSEntityServiceImpl bseService) {
        super(bseService);
    }

    public boolean checkEntityTypeAccession(BaseSequenceEntity bse) {
        return AccessionIdentifierUtil.isScaffold(bse.getAccession()) && bse instanceof Scaffold;
    }

    public boolean recognizeAccessionNo(String accessionNo) {
        return AccessionIdentifierUtil.isScaffold(accessionNo);
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity retrieveBseEntity(String accession)
            throws DaoException {
        Scaffold scaffold = null;
        BaseSequenceEntity bsEntity = super.retrieveBseEntity(accession);
        if (bsEntity != null) {
            if (bsEntity instanceof Scaffold) {
                scaffold = (Scaffold) bsEntity;
            }
            else {
                scaffold = (Scaffold) SequenceEntityFactory.duplicateSequenceEntity(bsEntity);
            }
        }
        return scaffold;
    }

}
