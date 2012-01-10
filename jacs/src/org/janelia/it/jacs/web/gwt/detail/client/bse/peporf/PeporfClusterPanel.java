
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 11, 2008
 * Time: 1:50:15 PM
 */
public class PeporfClusterPanel extends VerticalPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfClusterPanel");

    private PeporfPanel parentPanel;
    private TitledBox clusterInfoBox;

    PeporfClusterPanel(PeporfPanel parentPanel) {
        super();
        this.parentPanel = parentPanel;
    }

    public void initialize() {
        clusterInfoBox = new TitledBox("Cluster Information", false);
        clusterInfoBox.setStyleName("peptideAnnotationBox");
        add(clusterInfoBox);
    }

    public void display() {
        logger.debug("PeporfClusterPanel dislay...");
    }

    void populateClusterInfoPanel(ProteinClusterMember proteinClusterInfo, String errorMessage, Throwable error) {
        if (error != null) {
            clusterInfoBox.add(HtmlUtils.getHtml((errorMessage != null ? errorMessage : "Error:") +
                    " " + error.toString(),
                    "error"));
        }
        else {
            if (proteinClusterInfo == null) {
                clusterInfoBox.add(HtmlUtils.getHtml("Cluster information not yet available", "text"));
            }
            else {
                FlexTable clusterInfoTable = new FlexTable();
                RowIndex currentRow = new RowIndex(0);
                Widget clusterMembershipWidget = formatClusterMembershipInfo(proteinClusterInfo);
                TableUtil.addWidgetRow(clusterInfoTable,
                        currentRow,
                        "Core Cluster Membership",
                        clusterMembershipWidget);
                if (proteinClusterInfo.getFinalClusterAcc() != null) {
                    Widget finalClusterAccessionLink = parentPanel.getTargetAccessionWidget(parentPanel.getAcc(),
                            "Peptide Details",
                            proteinClusterInfo.getFinalClusterAcc());
                    TableUtil.addWidgetRow(clusterInfoTable,
                            currentRow,
                            "Final Cluster Acc",
                            finalClusterAccessionLink);
                }
                if (proteinClusterInfo.getNonRedundantParentAcc() != null) {
                    TableUtil.addWidgetRow(clusterInfoTable,
                            currentRow,
                            "Non Redundant Protein Accession",
                            parentPanel.getTargetAccessionWidget(parentPanel.getAcc(),
                                    "Peptide Details",
                                    proteinClusterInfo.getNonRedundantParentAcc()));
                }
                clusterInfoBox.add(clusterInfoTable);
            }
        }
    }

    private Widget formatClusterMembershipInfo(ProteinClusterMember proteinClusterInfo) {
        Link coreClusterAccessionLink = (Link) parentPanel.getTargetAccessionWidget(parentPanel.getAcc(),
                "Peptide Details",
                proteinClusterInfo.getCoreClusterAcc());
        HorizontalPanel clusterMembershipWidget;
        String nrInfo;
        if (proteinClusterInfo.getClusterQuality().equals("singleton")) {
            clusterMembershipWidget = new HorizontalPanel();
            clusterMembershipWidget.add(HtmlUtils.getHtml("This sequence is assigned to a singleton cluster",
                    "text"));
        }
        else {
            if (proteinClusterInfo.getClusterQuality().equals("spurious")) {
                nrInfo = "This sequence is a member of a spurios cluster &nbsp;";
            }
            else {
                if (proteinClusterInfo.getNonRedundantParentAcc() == null) {
                    nrInfo = "This sequence is a non redundant member of &nbsp;";
                }
                else {
                    nrInfo = "This sequence is a member of &nbsp;";
                }
            }
            clusterMembershipWidget = new HorizontalPanel();
            clusterMembershipWidget.add(HtmlUtils.getHtml(nrInfo, "text"));
            clusterMembershipWidget.add(coreClusterAccessionLink);
        }
        return clusterMembershipWidget;
    }

}
