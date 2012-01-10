
package org.janelia.it.jacs.web.gwt.detail.server.bse;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;

/**
 * This is the default base sequence initializer;
 */
public class DefaultBSEInitializer extends BSEntityInitializer {

    public DefaultBSEInitializer(BSEntityServiceImpl bseService) {
        super(bseService);
    }

    public boolean checkEntityTypeAccession(BaseSequenceEntity bse) {
        return bse != null;
    }

    public boolean recognizeAccessionNo(String accessionNo) {
        return true;
    }

}
