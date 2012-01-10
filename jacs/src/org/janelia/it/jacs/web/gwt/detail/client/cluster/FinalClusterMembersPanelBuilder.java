
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinCluster;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;

/**
 * Responsible for creating and populating a data panel with core cluster members of a final cluster
 *
 * @author Cristian Goina
 */
public class FinalClusterMembersPanelBuilder extends BaseClusterMemberEntitiesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.FinalClusterMembersPanelBuilder");

    private static final int CORE_CLUSTER_ACC_COLUMN = 0;
    private static final int NUM_PROTEINS_COLUMN = 1;
    private static final int NUM_NR_PROTEINS_COLUMN = 2;

    private static final String CORE_CLUSTER_ACC_HEADING = "Core Acc";
    private static final String NUM_PROTEINS__HEADING = " Proteins";
    private static final String NUM_NR_PROTEINS__HEADING = "Num. NR Proteins";

    private class FinalClusterMembersRetriever implements PagedDataRetriever {
        private FinalClusterMembersRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(DataRetrievedListener listener) {
            listener.onSuccess(clusterPanel.getProteinCluster().getNumClusterMembers());
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     DataRetrievedListener listener) {
            clusterService.getPagedCoreClustersFromFinalCluster(clusterPanel.getProteinCluster().getClusterAcc(),
                    startIndex,
                    numRows,
                    sortArgs,
                    createDataRetrievedCallback(listener));
        }
    }

    public FinalClusterMembersPanelBuilder(ClusterPanel clusterPanel) {
        super(clusterPanel, "FinalClusterMembers");
    }

    protected SortableTable createDataTable() {
        SortableTable sortableTable = super.createDataTable();
        sortableTable.addColumn(new TextColumn(CORE_CLUSTER_ACC_HEADING));
        sortableTable.addColumn(new NumericColumn(NUM_PROTEINS__HEADING));
        sortableTable.addColumn(new NumericColumn(NUM_NR_PROTEINS__HEADING));
        return sortableTable;
    }

    public PagedDataRetriever createDataRetriever() {
        return new FinalClusterMembersRetriever();
    }

    public String[][] getSortOptions() {

        return new String[][]{
                {"coreClusterAcc", CORE_CLUSTER_ACC_HEADING},
                {"numProteins", NUM_PROTEINS__HEADING},
                {"numNonRedundant", NUM_NR_PROTEINS__HEADING}
        };
    }

    protected Widget createExportMenu() {
        return null; // don't create an export for this table
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ProteinCluster clusterMember = (ProteinCluster) data;
        TableRow row = new TableRow();
        row.setValue(CORE_CLUSTER_ACC_COLUMN,
                new TableCell(clusterMember.getClusterAcc(),
                        clusterPanel.getTargetAccessionWidget(clusterPanel.getProteinCluster().getClusterAcc(),
                                "Cluster Panel",
                                clusterMember.getClusterAcc())));
        row.setValue(NUM_PROTEINS_COLUMN, new TableCell(clusterMember.getNumProteins()));
        row.setValue(NUM_NR_PROTEINS_COLUMN, new TableCell(clusterMember.getNumNonRedundantProteins()));
        return row;
    }

}
