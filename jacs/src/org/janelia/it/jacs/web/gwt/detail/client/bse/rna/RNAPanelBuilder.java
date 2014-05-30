
package org.janelia.it.jacs.web.gwt.detail.client.bse.rna;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder;

/**
 * Responsible for controlling the sequence and timing of operations needed to build an RNAPanel
 *
 * @author Cristian Goina
 * @autor Tareq Nabeel
 */
public class RNAPanelBuilder extends BSEntityPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.orf.RNAPanelBuilder");

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public RNAPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        logger.info("RNAPanelBuilder retrieve and populate data");
        RNAPanel rnaPanel = (RNAPanel) getSubPanel();
        RNADnaDataRetriever rnaDnaDataRetriever = new RNADnaDataRetriever(rnaPanel, getBaseEntityService());
        rnaDnaDataRetriever.retrieveData();
    }

}
