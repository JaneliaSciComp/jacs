
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ClusterAnnotation;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.EnzymeCommissionExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.GeneOntologyExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TablePopulateListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortableTableClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;

import java.util.List;

/**
 * Responsible for creating and populating a data panel with non redundant
 * cluster sequences
 *
 * @author Cristian Goina
 */
public class ClusterAnnotationsPanelBuilder extends BaseClusterMemberEntitiesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.ClusterAnnotationsPanelBuilder");

    private static final int ANNOTATION_ID_COLUMN = 0;
    private static final int ANNOTATION_CATEGORY_COLUMN = 1;
    private static final int ANNOTATION_DESCRIPTION_COLUMN = 2;
    private static final int PCT_ASSIGNED_COLUMN = 3;

    private static final String ANNOTATION_CATEGORY_HEADING = "Category";
    private static final String ANNOTATION_ID_HEADING = "ID";
    private static final String ANNOTATION_DESCRIPTION_HEADING = "Description";
    private static final String PCT_ASSIGNED_HEADING = "% Assigned";

    private static final int MAX_ANNOTATION_DESC_LENGTH = 100;

    private class AnnotationSelectedListener implements SelectionListener {
        public void onSelect(String rowNum) {
            TableCell cell = dataTable.getValue(Integer.valueOf(rowNum).intValue(), ANNOTATION_ID_COLUMN);
            String id = (String) cell.getValue();

            SimplePanel panel = new SimplePanel();
            panel.addStyleName("dependentTableTitleSpacer");

            Widget accession = getAnnotationIDLink(id, null);
            accession.addStyleName("tableRowPostSelect"); // highlight to match selected table row
            accession.addStyleName("dependentTableTitleHighlight"); // add some margin around the highlight
            accession.addStyleName("dependentTableAccession"); // have to push down the accession a bit

            HorizontalPanel subtitlePanel = new HorizontalPanel();
            subtitlePanel.add(HtmlUtils.getHtml("Matching sequences for&nbsp;", "dependentTableTitle"));

            HTML selectedHtml = HtmlUtils.getHtml("selected annotation&nbsp;", "dependentTableTitle");
            selectedHtml.addStyleName("tableRowPostSelect"); // highlight to match selected table row
            selectedHtml.addStyleName("dependentTableTitleHighlight"); // add some margin around the highlight
            subtitlePanel.add(selectedHtml);

            subtitlePanel.add(accession);
            subtitlePanel.add(HtmlUtils.getHtml(":", "dependentTableTitle"));

            panel.add(subtitlePanel);
            matchingSeqHintPanel.setWidget(panel);

            annotatedSeqPanelBuilder.setAnnotationID(id);
        }

        public void onUnSelect(String rowNum) {
            // nothing to do on unselect
        }
    }

    private String annotationType;
    private AnnotatedClusterSeqMembersPanelBuilder annotatedSeqPanelBuilder;
    private SimplePanel matchingSeqHintPanel;

    public ClusterAnnotationsPanelBuilder(ClusterPanel clusterPanel, String annotationType) {
        super(clusterPanel, "ClusterAnnotations");
        this.annotationType = annotationType;
    }

    public Panel createDataPanel() {
        VerticalPanel annotationsPanel = new VerticalPanel();
        annotationsPanel.add(HtmlUtils.getHtml("Click on a row to see the list of matching sequences", "hint"));
        annotationsPanel.add(super.createDataPanel());
        matchingSeqHintPanel = new SimplePanel();
        annotationsPanel.add(matchingSeqHintPanel);
        annotatedSeqPanelBuilder = new AnnotatedClusterSeqMembersPanelBuilder(clusterPanel);
        annotationsPanel.add(annotatedSeqPanelBuilder.createDataPanel());
        return annotationsPanel;
    }

    public PagedDataRetriever createDataRetriever() {
        throw new UnsupportedOperationException("Data retriever is not supported");
    }

    /**
     * This method is never used since the annotation table is fairly small and all the sorting is
     * done by the table itself
     */
    public String[][] getSortOptions() {
        throw new UnsupportedOperationException("Sort options are not defined");
    }

    public void populateData() {
        if (!_haveData) {
            retrievePanelData(dataTable.getSortColumns());
        }
    }

    protected void createDataPagingPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        dataPagingPanel = new PagingPanel(dataTable,
                pageLengthOptions,
                defaultNumVisibleRows,
                true,
                true,
                null,
                false,
                PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                "ClusterAnnotationsData");
        dataPagingPanel.addAdvancedSortClickListener(new AdvancedSortableTableClickListener(dataTable,
                dataTable.getAllSortableColumns()));
        dataPagingPanel.setStyleName("clusterMembersDataPanel");
    }

    protected SortableTable createDataTable() {
        final SortableTable sortableTable = super.createDataTable();
        sortableTable.setHighlightSelect(true);
        sortableTable.addPopulateListener(new TablePopulateListener() {
            public void onBusy(Widget widget) {
            }

            public void onBusyDone(Widget widget) {
                if (sortableTable.getNumDataRows() > 0) {
                    // select the first row from the annotations table
                    sortableTable.selectRow(1);
                }
            }
        });
        sortableTable.addSelectionListener(new AnnotationSelectedListener());

        sortableTable.addColumn(new TextColumn(ANNOTATION_ID_HEADING));
        sortableTable.addColumn(new TextColumn(ANNOTATION_CATEGORY_HEADING));
        sortableTable.addColumn(new TextColumn(ANNOTATION_DESCRIPTION_HEADING));
        sortableTable.addColumn(new TextColumn(PCT_ASSIGNED_HEADING));

        return sortableTable;
    }

    protected Widget createExportMenu() {
        return null;  // don't create an export menu for the annotations table
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ClusterAnnotation clusterAnnotation = (ClusterAnnotation) data;
        TableRow row = new TableRow();

        String id = clusterAnnotation.getAnnotationID();
        String desc = AnnotationUtil.arrowizeAnnotationDescription(clusterAnnotation.getDescription());
        row.setValue(ANNOTATION_ID_COLUMN, new TableCell(id, getAnnotationIDLink(id, desc)));
        row.setValue(ANNOTATION_CATEGORY_COLUMN, new TableCell(clusterAnnotation.getAnnotationType()));
        row.setValue(ANNOTATION_DESCRIPTION_COLUMN, new TableCell(clusterAnnotation.getDescription(),
                new FulltextPopperUpperHTML(desc, MAX_ANNOTATION_DESC_LENGTH)));
        row.setValue(PCT_ASSIGNED_COLUMN, new TableCell(clusterAnnotation.getEvidencePct() + "%"));

        return row;
    }

    private void retrievePanelData(SortArgument[] sortArgs) {
        AsyncCallback annotationsCallback = new AsyncCallback() {
            public void onFailure(Throwable caught) {
                dataPagingPanel.displayErrorMessage("Error retrieving " + annotationType +
                        "cluster annotations");
            }

            public void onSuccess(Object result) {
                List annotationsList = (List) result;
                List annotationsRows = formatDataListAsTableRowList(annotationsList);
                _haveData = true;
                dataPagingPanel.getPaginator().setData(annotationsRows);
                if (annotationsRows != null && annotationsRows.size() > 0) {
                    dataPagingPanel.first();
                }
                else {
                    annotatedSeqPanelBuilder.displayNoData("No " + annotationType + " annotation found");
                }
            }

        };
        clusterService.getClusterAnnotations(clusterPanel.getProteinCluster().getClusterAcc(),
                sortArgs,
                annotationsCallback);
    }

    private Widget getAnnotationIDLink(String id, String desc) {
        if (id.startsWith("EC:"))
            return new EnzymeCommissionExternalLink(id, desc);
        else if (id.startsWith("GO"))
            return new GeneOntologyExternalLink(id, desc);
        else
            return HtmlUtils.getHtml(id, "text");
    }
}
