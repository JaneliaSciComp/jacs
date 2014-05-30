
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.LoginProtectedMenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.detail.client.peptide.PeptideTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for creating and populating a data panel with non redundant
 * cluster sequences
 *
 * @author Cristian Goina
 */
public class NRClusterSeqMembersPanelBuilder extends BaseClusterMemberEntitiesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.NRClusterSeqMembersPanelBuilder");

    private static final String NR_PROTEIN_ACC_HEADING = "NR Accession";

    private PeptideTableBuilder _tableBuilder;

    private class NRClusterSeqMembersRetriever implements PagedDataRetriever {
        private NRClusterSeqMembersRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(DataRetrievedListener listener) {
            listener.onSuccess(clusterPanel.getProteinCluster().getNumNonRedundantProteins());
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     DataRetrievedListener listener) {
            clusterService.getPagedNRSeqMembersFromCluster(clusterPanel.getProteinCluster().getClusterAcc(),
                    startIndex,
                    numRows,
                    sortArgs,
                    createDataRetrievedCallback(listener));
        }
    }

    public NRClusterSeqMembersPanelBuilder(ClusterPanel clusterPanel) {
        super(clusterPanel, "NRClusterSeqMembers");
    }

    protected SortableTable createDataTable() {
        // Custom columns for the standard peptide table
        List<TableColumn> customColumns = new ArrayList();
        customColumns.add(new TextColumn(NR_PROTEIN_ACC_HEADING, "Non-Redundant Protein Accession (or self)"));

        _tableBuilder = new PeptideTableBuilder(customColumns);
        return _tableBuilder.getSortableTable();
    }

    public PagedDataRetriever createDataRetriever() {
        return new NRClusterSeqMembersRetriever();
    }

    public String[][] getSortOptions() {
        return _tableBuilder.getSortOptions(new String[][]{
                {"nr_parent_acc", NR_PROTEIN_ACC_HEADING}
        });
    }

    protected Widget createExportMenu() {
        final MenuBarWithRightAlignedDropdowns menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        LoginProtectedMenuBarWithRightAlignedDropdowns dropDown =
                new LoginProtectedMenuBarWithRightAlignedDropdowns(true, "You must be logged in to export");

        dropDown.addItem("NR Sequences as FASTA", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(clusterPanel.getProteinCluster().getClusterAcc());
                ClusterProteinAnnotationExportTask exportTask = new ClusterProteinAnnotationExportTask(
                        null, true, ExportWriterConstants.EXPORT_TYPE_FASTA, accessionList, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });
        dropDown.addItem("NR Sequences as CSV", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(clusterPanel.getProteinCluster().getClusterAcc());
                ClusterProteinAnnotationExportTask exportTask = new ClusterProteinAnnotationExportTask(
                        null, true, ExportWriterConstants.EXPORT_TYPE_CSV, accessionList, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });


        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ProteinClusterMember peptide = (ProteinClusterMember) data;

        // Stock peptide table columns
        List<TableCell> cells = new ArrayList();
        cells.add(new TableCell(peptide.getProteinAcc(),
                clusterPanel.getTargetAccessionWidget(clusterPanel.getProteinCluster().getClusterAcc(), "Cluster Panel", peptide.getProteinAcc())));
        cells.add(new TableCell(peptide.getProteinFunction()));
        cells.add(new TableCell(peptide.getGeneSymbol()));
        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(peptide.getGoAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(peptide.getGoAnnotationDescription())));
        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(peptide.getEcAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(peptide.getEcAnnotationDescription())));
        cells.add(new TableCell(peptide.getLength())); //TODO: format with commas

        // Custom column values
        List<TableCell> customCells = new ArrayList();
        if (peptide.getNonRedundantParentAcc() != null)
            customCells.add(new TableCell(peptide.getNonRedundantParentAcc(),
                    clusterPanel.getTargetAccessionWidget(clusterPanel.getProteinCluster().getClusterAcc(),
                            "Cluster Panel", peptide.getNonRedundantParentAcc())));
        else
            customCells.add(new TableCell(peptide.getNonRedundantParentAcc(), HtmlUtils.getHtml("self", "text")));

        return _tableBuilder.createTableRow(cells, customCells);
    }
}
