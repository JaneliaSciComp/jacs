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
