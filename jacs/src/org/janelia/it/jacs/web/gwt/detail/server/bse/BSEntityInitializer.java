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
