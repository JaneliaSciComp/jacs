
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.DetailServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.service.cluster.ClusterDetailService;
import org.janelia.it.jacs.web.gwt.detail.client.service.cluster.ClusterDetailServiceAsync;

/**
 * Responsible for controlling the sequence and timing of operations need to build a BSEntityPanel
 *
 * @author Tareq Nabeel
 */
public class ClusterPanelBuilder extends DetailPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.ClusterPanelBuilder");

    private static ClusterDetailServiceAsync clEntityService = (ClusterDetailServiceAsync) GWT.create(ClusterDetailService.class);

    static {
        ((ServiceDefTarget) clEntityService).setServiceEntryPoint("clDetail.srv");
    }

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public ClusterPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Gets called on successful completion of retrieveData
     */
    protected void populatePanel() {
        try {
            logger.debug("ClusterPanelBuilder populatePanel...");
            // start displaying the cluster data
            getSubPanel().displayData();
            // retrieve other panel specific data
            retrieveAndPopulatePanelData();
        }
        catch (RuntimeException e) {
            logger.error("ClusterPanelBuilder populatePanel caught exception " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        logger.debug("retrieve and populate specific entity data");
        ClusterPanel clusterPanel = (ClusterPanel) getSubPanel();
        if (clusterPanel.getNrSeqMembersPanelBuilder() != null) {
            clusterPanel.getNrSeqMembersPanelBuilder().populateData();
        }
        if (clusterPanel.getSeqMembersPanelBuilder() != null) {
            clusterPanel.getSeqMembersPanelBuilder().populateData();
        }
        if (clusterPanel.getCoreClustersPanelBuilder() != null) {
            clusterPanel.getCoreClustersPanelBuilder().populateData();
        }
        clusterPanel.getClusterAnnotationsPanelBuilder().populateData();
    }

    protected DetailServiceAsync getDetailService() {
        return clEntityService;
    }

}
