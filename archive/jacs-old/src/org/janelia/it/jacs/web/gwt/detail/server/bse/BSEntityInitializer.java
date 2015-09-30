
package org.janelia.it.jacs.web.gwt.detail.server.bse;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

/**
 * This class is used to retrieve the appropriate sequence entity type based on entity's accession
 * and if needed do some additional data retrieval and initialization
 */
abstract public class BSEntityInitializer {

    private FeatureDAO featureDAO;
    protected BSEntityServiceImpl bseService;

    public BSEntityInitializer(BSEntityServiceImpl bseService) {
        this.bseService = bseService;
    }

    public FeatureDAO getFeatureDAO() {
        return featureDAO;
    }

    public void setFeatureDAO(FeatureDAO dao) {
        featureDAO = dao;
    }

    abstract public boolean checkEntityTypeAccession(BaseSequenceEntity bse);

    abstract public boolean recognizeAccessionNo(String accessionNo);

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity retrieveBseEntity(String accession)
            throws DaoException {
        return featureDAO.findBseByAcc(accession);
    }

}
