
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinClusterAnnotationMember;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
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
public class AnnotatedClusterSeqMembersPanelBuilder extends BaseClusterMemberEntitiesPanelBuilder {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.AnnotatedClusterSeqMembersPanelBuilder");

    // Custom cols
    private static final String NR_PROTEIN_ACC_HEADING = "NR Accession";
    private static final String EVIDENCE_HEADING = "Evidence";

    private static final int EVIDENCE_COLUMN_LENGTH = 20;

    private PeptideTableBuilder _tableBuilder;

    private class AnnotatedClusterSeqMembersRetriever implements PagedDataRetriever {
        private AnnotatedClusterSeqMembersRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(DataRetrievedListener listener) {
            clusterService.getNumMatchingRepsFromClusterWithAnnotation(clusterPanel.getProteinCluster().getClusterAcc(),
                    annotationID,
                    createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     DataRetrievedListener listener) {
            clusterService.getPagedMatchingRepsFromClusterWithAnnotation(clusterPanel.getProteinCluster().getClusterAcc(),
                    annotationID,
                    startIndex,
                    numRows,
                    sortArgs,
                    createDataRetrievedCallback(listener));
        }
    }

    private String annotationID;

    public AnnotatedClusterSeqMembersPanelBuilder(ClusterPanel clusterPanel) {
        super(clusterPanel, "AnnotatedClusterSeqMembers");
    }

    protected SortableTable createDataTable() {
        // Custom columns for the standard peptide table
        List<TableColumn> customColumns = new ArrayList();
        TextColumn nrCol = new TextColumn(NR_PROTEIN_ACC_HEADING, "Non-Redundant Protein Accession (or self)");
        customColumns.add(nrCol);
        customColumns.add(new TextColumn(EVIDENCE_HEADING));

        _tableBuilder = new PeptideTableBuilder(customColumns);
        SortableTable st = _tableBuilder.getSortableTable();
        // default sort is by non-redundatn protein ID to force all
        // non-redundant proteins in the list to the top of the table
        st.setDefaultSortColumns(new SortableColumn[]{new SortableColumn(1, NR_PROTEIN_ACC_HEADING, SortArgument.SORT_DESC)});
        return st;
    }

    public PagedDataRetriever createDataRetriever() {
        return new AnnotatedClusterSeqMembersRetriever();
    }

    public String[][] getSortOptions() {
        return _tableBuilder.getSortOptions(new String[][]{
                {"nr_parent_acc", NR_PROTEIN_ACC_HEADING},
                {"evidence", EVIDENCE_HEADING}}
        );
    }

    public void populateData() {
        if (annotationID == null) {
            dataPagingPanel.displayErrorMessage("No annotation ID has been provided");
        }
        else {
            super.populateData();
        }
    }

    protected Widget createExportMenu() {
        final MenuBarWithRightAlignedDropdowns menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        LoginProtectedMenuBarWithRightAlignedDropdowns dropDown =
                new LoginProtectedMenuBarWithRightAlignedDropdowns(true, "You must be logged in to export");

        dropDown.addItem("Annotated Sequences as FASTA", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(clusterPanel.getProteinCluster().getClusterAcc());
                ClusterProteinAnnotationExportTask exportTask = new ClusterProteinAnnotationExportTask(
                        annotationID, false, ExportWriterConstants.EXPORT_TYPE_FASTA, accessionList, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });
        dropDown.addItem("Annotated Sequences as CSV", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(clusterPanel.getProteinCluster().getClusterAcc());
                ClusterProteinAnnotationExportTask exportTask = new ClusterProteinAnnotationExportTask(
                        annotationID, false, ExportWriterConstants.EXPORT_TYPE_CSV, accessionList, sortList);
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

        // Custom columns - NR Acc, Defline and Evidence
        List<TableCell> customCells = new ArrayList();
        if (peptide.getNonRedundantParentAcc() != null)
            customCells.add(new TableCell(peptide.getNonRedundantParentAcc(),
                    clusterPanel.getTargetAccessionWidget(clusterPanel.getProteinCluster().getClusterAcc(),
                            "Cluster Panel", peptide.getNonRedundantParentAcc())));
        else
            customCells.add(new TableCell(peptide.getNonRedundantParentAcc(), HtmlUtils.getHtml("self", "text")));
        // Evidence
        String evidence = ((ProteinClusterAnnotationMember) peptide).getEvidence();
        String evidenceExternalLink = ((ProteinClusterAnnotationMember) peptide).getExternalEvidenceLink();
        if (evidenceExternalLink != null) {
            customCells.add(new TableCell(evidence, new ExternalLink(evidence, evidenceExternalLink)));
        }
        else {
            customCells.add(new TableCell(((ProteinClusterAnnotationMember) peptide).getEvidence(),
                    new FulltextPopperUpperHTML(((ProteinClusterAnnotationMember) peptide).getEvidence(), EVIDENCE_COLUMN_LENGTH)));
        }
        return _tableBuilder.createTableRow(cells, customCells);
    }

    void setAnnotationID(String annotationID) {
        String prevAnnotationID = this.annotationID;
        this.annotationID = annotationID;
        if (prevAnnotationID != null && !prevAnnotationID.equals(annotationID)) {
            clearDataPanel();
        }
        populateData();
    }

    void displayNoData(String message) {
        dataPagingPanel.displayNoDataMessage(message);
    }

}
