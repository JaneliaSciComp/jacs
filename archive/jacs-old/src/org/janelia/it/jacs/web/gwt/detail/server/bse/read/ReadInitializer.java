
package org.janelia.it.jacs.web.gwt.detail.server.bse.read;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityInitializer;
import org.janelia.it.jacs.web.gwt.detail.server.bse.BSEntityServiceImpl;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
public class ReadInitializer extends BSEntityInitializer {

    public ReadInitializer(BSEntityServiceImpl bseService) {
        super(bseService);
    }

    public boolean checkEntityTypeAccession(BaseSequenceEntity bse) {
        return AccessionIdentifierUtil.isRead(bse.getAccession()) && bse instanceof Read;
    }

    public boolean recognizeAccessionNo(String accessionNo) {
        return AccessionIdentifierUtil.isRead(accessionNo);
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity retrieveBseEntity(String accession)
            throws DaoException {
        return getFeatureDAO().getReadWithLibraryByAccesion(accession);
    }

}
