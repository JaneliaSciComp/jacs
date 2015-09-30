
package org.janelia.it.jacs.web.gwt.detail.client.bse.read;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder;

/**
 * Responsible for controlling the sequence and timing of operations need to build a ReadPanel
 *
 * @author Tareq Nabeel
 */
public class ReadPanelBuilder extends BSEntityPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanelBuilder");

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public ReadPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        super.retrieveAndPopulatePanelData();
    }

}
