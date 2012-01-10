
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.ProteinCluster;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;
import org.janelia.it.jacs.web.gwt.download.client.DownloadBox;
import org.janelia.it.jacs.web.gwt.download.client.DownloadClusterPopup;

/**
 * The class contains the data and operations needed to render `ClusterPanel
 *
 * @author Cristian Goina based on Tareq Nabeel's BSEntityPanel
 */
public class ClusterPanel extends DetailSubPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.ClusterPanel");
    private static final String DETAIL_TYPE = "Cluster";
    private static final int DOWNLOAD_BOX_ROW = 0;
    private static final int DOWNLOAD_BOX_COL = 2;
    private static final int DOWNLOAD_BOX_HEIGHT = 4;

    private ProteinCluster proteinCluster;
    private TitledBox nrSeqMembersBox;
    private NRClusterSeqMembersPanelBuilder nrSeqMembersPanelBuilder;
    private TitledBox seqMembersBox;
    private ClusterSeqMembersPanelBuilder seqMembersPanelBuilder;
    private TitledBox coreClustersBox;
    private FinalClusterMembersPanelBuilder coreClustersPanelBuilder;
    private TitledBox annotationsBox;
    private ClusterAnnotationsPanelBuilder clusterAnnotationsPanelBuilder;
    private DownloadBox clusterDownloadsBox;

    /**
     * Go through DetailSubPanelFactory for getting instance
     */
    public ClusterPanel() {
    }

    public Object getEntity() {
        return proteinCluster;
    }

    public void setEntity(Object obj) {
        logger.debug("ClusterPanel setEntity " +
                (obj == null ? "null" : obj.toString()));
        proteinCluster = (ProteinCluster) obj;
    }

    /**
     * Adds entity data to the panel using the entity model instance retrieved through
     * the service call. This method is called after successful detailservice callback.
     */
    public void displayData() {
        try {
            logger.debug("ClusterPanel displayData...");
            populateClusterDetails();
        }
        catch (RuntimeException e) {
            logger.error("ClusterPanel displayData caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }

    /**
     * precondition: the method is invoked only once the entity is available
     */
    protected void createDetailSpecificPanels() {
        addSpacer(this);
        // create annotations panel
        createAnnotationsPanel();
        addSpacer(this);
        if (proteinCluster.getNumClusterMembers() != null && proteinCluster.getNumClusterMembers().intValue() > 0) {
            createClusterMembersPanel();
        }
        else {
            // create representative sequences panel
            createNRSeqMembersPanel();
            addSpacer(this);
            // create member sequences panel
            createSeqMembersPanel();
        }
    }

    ClusterAnnotationsPanelBuilder getClusterAnnotationsPanelBuilder() {
        return clusterAnnotationsPanelBuilder;
    }

    FinalClusterMembersPanelBuilder getCoreClustersPanelBuilder() {
        return coreClustersPanelBuilder;
    }

    ClusterSeqMembersPanelBuilder getSeqMembersPanelBuilder() {
        return seqMembersPanelBuilder;
    }

    NRClusterSeqMembersPanelBuilder getNrSeqMembersPanelBuilder() {
        return nrSeqMembersPanelBuilder;
    }

    /**
     * This is a getter for the enclosed entity that doesn't require casting
     *
     * @return the panel's protein cluster entity
     */
    ProteinCluster getProteinCluster() {
        return proteinCluster;
    }

    private void addClusterDownloadsBox() {
        clusterDownloadsBox = new DownloadBox("Downloads",
                null, /* actionLink */
                false, /* showActionLink */
                false /* showContent */);
        clusterDownloadsBox.add(new Link("Download Non-Redundant Sequences (Multi-FASTA)",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        new PopupAboveLauncher(new DownloadClusterPopup(proteinCluster.getClusterAcc(),
                                true,
                                "Non-Redundant Sequences",
                                false)).showPopup(sender);
                    }
                }));
        clusterDownloadsBox.add(new Link("Download Member Sequences (Multi-FASTA)",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        new PopupAboveLauncher(new DownloadClusterPopup(proteinCluster.getClusterAcc(),
                                false,
                                "Member Sequences",
                                false)).showPopup(sender);
                    }
                }));
        getMainDataTable().setWidget(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL, clusterDownloadsBox);
        getMainDataTable().getFlexCellFormatter().setRowSpan(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL,
                DOWNLOAD_BOX_HEIGHT);
        getMainDataTable().getFlexCellFormatter().setHorizontalAlignment(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL,
                HorizontalPanel.ALIGN_RIGHT);
    }

    private void createClusterMembersPanel() {
        coreClustersBox = new TitledBox("Core Clusters", true);
        add(coreClustersBox);
        coreClustersPanelBuilder = new FinalClusterMembersPanelBuilder(this);
        coreClustersBox.add(coreClustersPanelBuilder.createDataPanel());
    }

    private void createNRSeqMembersPanel() {
        nrSeqMembersBox = new TitledBox("Non-Redundant Sequences", true);
        add(nrSeqMembersBox);
        nrSeqMembersPanelBuilder = new NRClusterSeqMembersPanelBuilder(this);
        nrSeqMembersBox.add(nrSeqMembersPanelBuilder.createDataPanel());
    }

    private void createSeqMembersPanel() {
        seqMembersBox = new TitledBox("Member Sequences", true);
        add(seqMembersBox);
        seqMembersPanelBuilder = new ClusterSeqMembersPanelBuilder(this);
        seqMembersBox.add(seqMembersPanelBuilder.createDataPanel());
    }

    private void createAnnotationsPanel() {
        annotationsBox = new TitledBox("Protein Annotations", true);
        add(annotationsBox);
        clusterAnnotationsPanelBuilder = new ClusterAnnotationsPanelBuilder(this, "Protein");
        annotationsBox.add(clusterAnnotationsPanelBuilder.createDataPanel());
    }

    private void populateClusterDetails() {
        RowIndex rowIndex = new RowIndex(0);
        // add the cluster ID
        TableUtil.addTextRow(getMainDataTable(), rowIndex, "Cluster ID", proteinCluster.getClusterAcc());
        // add the parent cluster ID
        if (proteinCluster.getParentClusterAcc() != null) {
            TableUtil.addWidgetRow(getMainDataTable(),
                    rowIndex,
                    "Final Cluster ID",
                    getTargetAccessionWidget(proteinCluster.getClusterAcc(),
                            "Final Cluster Details",
                            proteinCluster.getParentClusterAcc()));

        }
        // cluster quality
        TableUtil.addTextRow(getMainDataTable(), rowIndex, "Cluster quality", proteinCluster.getClusterQuality());
        String numberOfMemberSeqs = null;
        if (proteinCluster.getNumProteins() == null) {
            numberOfMemberSeqs = "n/a";
        }
        else {
            numberOfMemberSeqs = proteinCluster.getNumProteins().toString();
        }
        TableUtil.addTextRow(getMainDataTable(), rowIndex, "Member sequences", numberOfMemberSeqs);
        String numberOfNRMemberSeqs = null;
        if (proteinCluster.getNumProteins() == null) {
            numberOfNRMemberSeqs = "n/a";
        }
        else {
            numberOfNRMemberSeqs = proteinCluster.getNumNonRedundantProteins().toString();
        }
        TableUtil.addTextRow(getMainDataTable(), rowIndex, "Non redundant member sequences", numberOfNRMemberSeqs);
        if (proteinCluster.getNumClusterMembers() != null && proteinCluster.getNumClusterMembers().intValue() > 0) {
            TableUtil.addTextRow(getMainDataTable(),
                    rowIndex, "Number of core clusters", proteinCluster.getNumClusterMembers().toString());
        }
        addClusterDownloadsBox();
    }

}
