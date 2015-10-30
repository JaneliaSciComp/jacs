
package org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;

/**
 * The class encapsulates the data and operations needed to render NCI Panels
 *
 * @author Tareq Nabeel
 */
public class NCBIBasePanel extends BSEntityPanel {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBIBasePanel");

    private static final String DETAIL_TYPE = "NCBI";

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }
}
