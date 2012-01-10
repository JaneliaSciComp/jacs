
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:36:18 PM
 */
public class PeporfPanelBuilder extends BSEntityPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPanelBuilder");

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public PeporfPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        logger.info("PeporfPanelBuilder retrieve and populate data");
        super.retrieveAndPopulatePanelData();
        PeporfPanel peporfPanel = (PeporfPanel) getSubPanel();
        PeporfDataRetriever retriever = new PeporfDataRetriever(peporfPanel);
        retriever.retrieveData();
    }

}
