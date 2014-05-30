
package org.janelia.it.jacs.web.gwt.status.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.BlastResultExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHitWithSample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.OptionItem;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SelectOptionsLinks;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.CheckBoxColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.CheckBoxComparable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;

import java.util.*;

/**
 * @author Michael Press
 */
public class BlastHitsPanel extends TitledBox {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.panel.BlastHitsPanel");
    private HashMap _hitsById = new HashMap();
    private BlastJobInfo _job;
    private AlignmentListener _alignmentListener;
    private EntityListener _entityListener;

    private SortableTable seqWithSampleTable;
    private VerticalPanel _seqWithSamplePanel;
    private RemotingPaginator _seqWithSamplePaginator;
    private RemotePagingPanel _seqWithSamplePagingPanel;
    private SortableTable _seqWithoutSampleTable;
    private VerticalPanel _seqWithoutSamplePanel;
    private RemotingPaginator _seqWithoutSamplePaginator;
    private RemotePagingPanel _seqWithoutSamplePagingPanel;
    private SortableTable _blastHitsDataTableInUse;

    private LoadingLabel _loadingLabel;
    private HTML _errorLabel;
    private HTML _noDataLabel;

    private static final int CHECKBOX_COL = 0;
    private static final int ALIGNMENT_ID_COLUMN = 2;
    private static final int SEQ_ID_COLUMN = 3;
    private static final int SAMPLE_NAME_VISIBLE_SIZE = 15;
    private static final int SAMPLE_LOCATION_VISIBLE_SIZE = 30;
    private static final int QUERY_DEFLINE_VISIBLE_SIZE = 22;
    private static final int QUERY_DEFLINE_IDENTIFIER_MIN_VISIBLE_SIZE = 15;
    private static final int QUERY_DEFLINE_IDENTIFIER_MAX_VISIBLE_SIZE = 30;
    private static final int SUBJECT_DEFLINE_VISIBLE_SIZE = 40;
    private static final int DEFAULT_NUM_ROWS = 10;

    private static final String SEQWITHSAMPLETABLE_EVAL_HEADING = "Eval";
    private static final String SEQWITHSAMPLETABLE_ALIGNMENTID_HEADING = "Alignment ID";
    private static final String SEQWITHSAMPLETABLE_SEQUENCEID_HEADING = "Sequence ID";
    private static final String SEQWITHSAMPLETABLE_BITSCORE_HEADING = "Bit Score";
    private static final String SEQWITHSAMPLETABLE_ALIGNMENTLENGTH_HEADING = "Alignment Length";
    private static final String SEQWITHSAMPLETABLE_QUERY_HEADING = "Query";
    private static final String SEQWITHSAMPLETABLE_SUBJECTID_HEADING = "Subject";
    private static final String SEQWITHSAMPLETABLE_SAMPLES_HEADING = "Sample(s)";
    private static final String SEQWITHSAMPLETABLE_LOCATIONS_HEADING = "Location(s)";

    private static final String SEQTABLE_EVAL_HEADING = "Eval";
    private static final String SEQTABLE_ALIGNMENTID_HEADING = "Alignment ID";
    private static final String SEQTABLE_SEQUENCEID_HEADING = "Sequence ID";
    private static final String SEQTABLE_BITSCORE_HEADING = "Bit Score";
    private static final String SEQTABLE_ALIGNMENTLENGTH_HEADING = "Alignment Length";
    private static final String SEQTABLE_QUERY_HEADING = "Query";
    private static final String SEQTABLE_SUBJECTID_HEADING = "Subject";
    private static final String SEQTABLE_SUBJECTDESC_HEADING = "Subject Description";

    // Current state of the select all/none, so we can update newly retrieved rows
    // private boolean _selectAll = false;  THIS IS BEING CHANGED TO PAGE-ONLY SELECT STATE

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public BlastHitsPanel(String title, AlignmentListener alignmentListener, EntityListener entityListener) {
        super(title);
        setStyleName("blastHitsTitledBox");
        _alignmentListener = alignmentListener;
        _entityListener = entityListener;
    }

    protected void popuplateContentPanel() {
        _loadingLabel = new LoadingLabel("Loading sequences...", true);
        _errorLabel = HtmlUtils.getHtml("An error occurred retrieving the sequences for this job.", "error");
        _noDataLabel = HtmlUtils.getHtml("No matching sequences.", "text");

        add(_loadingLabel);
        add(_errorLabel);
        add(_noDataLabel);
        add(getSequenceWithSampleTable());
        add(getSequenceWithoutSampleTable());
    }

    private Widget getSequenceWithSampleTable() {
        // Create a panel for the table so the hint will be centered underneath
        _seqWithSamplePanel = new VerticalPanel();
        _seqWithSamplePanel.setVisible(false);

        // Create the table with a Loading message
        seqWithSampleTable = new SortableTable();
        seqWithSampleTable.setCellStyle("tableCellNoPadding");
        seqWithSampleTable.addSelectionListener(new HitSelectedListener(seqWithSampleTable));

        seqWithSampleTable.addColumn(new CheckBoxColumn("&nbsp;"));
        seqWithSampleTable.addColumn(new NumericColumn(SEQWITHSAMPLETABLE_EVAL_HEADING));
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_ALIGNMENTID_HEADING, false, false)); // hidden
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_SEQUENCEID_HEADING, false, false)); // hidden
        seqWithSampleTable.addColumn(new NumericColumn("Score", SEQWITHSAMPLETABLE_BITSCORE_HEADING));
        seqWithSampleTable.addColumn(new NumericColumn("Len.", SEQWITHSAMPLETABLE_ALIGNMENTLENGTH_HEADING));
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_QUERY_HEADING));
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_SUBJECTID_HEADING));
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_SAMPLES_HEADING));
        seqWithSampleTable.addColumn(new TextColumn(SEQWITHSAMPLETABLE_LOCATIONS_HEADING, /*sortable*/ false));
        seqWithSampleTable.setHighlightSelect(true);

        String[][] sortConstants = new String[][]{
                {"", ""},
                {BlastHitWithSample.SORT_BY_EVAL, SEQWITHSAMPLETABLE_EVAL_HEADING},
                {"", ""},
                {"", ""},
                {BlastHitWithSample.SORT_BY_BIT_SCORE, SEQWITHSAMPLETABLE_BITSCORE_HEADING},
                {BlastHitWithSample.SORT_BY_ALIGNMENT_LEN, SEQWITHSAMPLETABLE_ALIGNMENTLENGTH_HEADING},
                {BlastHitWithSample.SORT_BY_QUERY_DEFLINE, SEQWITHSAMPLETABLE_QUERY_HEADING},
                {BlastHitWithSample.SORT_SUBJECT_ACC, SEQWITHSAMPLETABLE_SUBJECTID_HEADING},
                {BlastHitWithSample.SORT_BY_SAMPLE_NAME, SEQWITHSAMPLETABLE_SAMPLES_HEADING}
        };
        _seqWithSamplePaginator = new RemotingPaginator(seqWithSampleTable, new SequenceWithSampleDataRetriever(), sortConstants,
                "SeqWithSample");
        _seqWithSamplePagingPanel = new RemotePagingPanel(seqWithSampleTable,
                _seqWithSamplePaginator,
                true,
                RemotePagingPanel.ADVANCEDSORTLINK_IN_THE_FOOTER,
                PagingPanel.DEFAULT_VISIBLE_ROWS,
                "BlastHitsSeqWithSample");
        // set the default sort columns
        //seqWithSampleTable.setDefaultSortColumns(new SortableColumn[] {
        //        new SortableColumn(4, SEQWITHSAMPLETABLE_BITSCORE_HEADING, SortableColumn.SORT_DESC)
        //});
        seqWithSampleTable.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(1, SEQWITHSAMPLETABLE_EVAL_HEADING, SortableColumn.SORT_ASC)
        });
        decoratePanel(_seqWithSamplePagingPanel, _seqWithSamplePanel);
        _seqWithSamplePagingPanel.addAdvancedSortClickListener(new BlastHitsAdvancedSortClickListener(seqWithSampleTable,
                _seqWithSamplePaginator,
                _seqWithSamplePagingPanel));
        return _seqWithSamplePanel;
    }

    private Widget getSequenceWithoutSampleTable() {
        // Create a panel for the table so the hint will be centered underneath
        _seqWithoutSamplePanel = new VerticalPanel();
        _seqWithoutSamplePanel.setVisible(false);

        // Create the table with a Loading message
        _seqWithoutSampleTable = new SortableTable();
        _seqWithoutSampleTable.setCellStyle("tableCellNoPadding");
        _seqWithoutSampleTable.addSelectionListener(new HitSelectedListener(_seqWithoutSampleTable));
        _seqWithoutSampleTable.addColumn(new CheckBoxColumn("&nbsp;"));
        _seqWithoutSampleTable.addColumn(new NumericColumn(SEQTABLE_EVAL_HEADING));
        _seqWithoutSampleTable.addColumn(new TextColumn(SEQTABLE_ALIGNMENTID_HEADING, false, false)); //hidden
        _seqWithoutSampleTable.addColumn(new TextColumn(SEQTABLE_SEQUENCEID_HEADING, false, false));  //hidden
        _seqWithoutSampleTable.addColumn(new NumericColumn("Score", SEQTABLE_BITSCORE_HEADING));
        _seqWithoutSampleTable.addColumn(new NumericColumn("Len.", SEQTABLE_ALIGNMENTLENGTH_HEADING));
        _seqWithoutSampleTable.addColumn(new TextColumn(SEQTABLE_QUERY_HEADING)); // accession
        _seqWithoutSampleTable.addColumn(new TextColumn(SEQTABLE_SUBJECTID_HEADING));
        _seqWithoutSampleTable.addColumn(new TextColumn(SEQTABLE_SUBJECTDESC_HEADING));
        _seqWithoutSampleTable.setHighlightSelect(true);

        String[][] sortConstants = new String[][]{
                {"", ""},
                {BlastHit.SORT_BY_EVAL, SEQTABLE_EVAL_HEADING},
                {"", ""},
                {"", ""},
                {BlastHit.SORT_BY_BIT_SCORE, SEQTABLE_BITSCORE_HEADING},
                {BlastHit.SORT_BY_ALIGNMENT_LEN, SEQTABLE_ALIGNMENTLENGTH_HEADING},
                {BlastHit.SORT_BY_QUERY_DEFLINE, SEQTABLE_QUERY_HEADING},
                {BlastHit.SORT_SUBJECT_ACC, SEQTABLE_SEQUENCEID_HEADING},
                {BlastHit.SORT_SUBJECT_DEFLINE, SEQTABLE_SUBJECTDESC_HEADING},
        };
        _seqWithoutSamplePaginator = new RemotingPaginator(_seqWithoutSampleTable,
                new GenericSequenceHitDataRetriever(),
                sortConstants,
                "SeqWithoutSample");
        _seqWithoutSamplePagingPanel = new RemotePagingPanel(_seqWithoutSampleTable,
                _seqWithoutSamplePaginator,
                true,
                RemotePagingPanel.ADVANCEDSORTLINK_IN_THE_FOOTER,
                PagingPanel.DEFAULT_VISIBLE_ROWS,
                "BlastHitsSeqWithoutSample");
        _seqWithoutSampleTable.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(1, SEQTABLE_EVAL_HEADING, SortableColumn.SORT_ASC) // it initially sorts on eval
        });
        decoratePanel(_seqWithoutSamplePagingPanel, _seqWithoutSamplePanel);
        _seqWithoutSamplePagingPanel.addAdvancedSortClickListener(new BlastHitsAdvancedSortClickListener(_seqWithoutSampleTable,
                _seqWithoutSamplePaginator,
                _seqWithoutSamplePagingPanel));
        return _seqWithoutSamplePanel;
    }

    /**
     * Retrieves the next page of data and returns it (as List<TableRows>) to the RemotePaginator
     */
    public class SequenceWithSampleDataRetriever implements PagedDataRetriever {

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            if (_job == null) {
                listener.onNoData(); // don't know the item count at page init time
            }
            else {
                if (_job.getNumHits() == null) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(new Integer(_job.getNumHits().intValue()));
                }
            }
        }

        /**
         * Retrieves a page of hits and returns them (as displayable List<TableRows>) to to the RemotePaginator via callback
         */
        public void retrieveDataRows(int startIndex, int numRows, SortArgument[] sortArgs, final DataRetrievedListener listener) {
            _dataservice.getPagedBlastHitsByTaskId(_job.getJobId(), startIndex, numRows, sortArgs, new AsyncCallback() {

                public void onFailure(Throwable throwable) {
                    _logger.error("Failure retrieving blast result node for job " + _job.getJobId());
                    showError();
                }

                public void onSuccess(Object object) {
                    if (object == null) {
                        _logger.error("Got null BlastResultNode for task ID " + _job.getJobId());
                        showError();
                        return;
                    }
                    _logger.debug("Successfully retrieved blast hits");
                    listener.onSuccess(processData((List<BlastHit>) object));
                }
            });
        }

        protected List<TableRow> processData(List<BlastHit> hits) {
            if (_logger.isDebugEnabled()) _logger.debug(" " + hits.size() + " sequence with sample hits");

            // Temporary listener to push the first hit to the alignment panel after the first page load is complete
            _seqWithSamplePaginator.addDataRetrievedCallback(new FirstPageRetrievedListener(seqWithSampleTable, _seqWithSamplePaginator));

            List<TableRow> tableRows = new ArrayList();
            for (BlastHit hit : hits) {
                _hitsById.put(hit.getBlastHitId(), hit); // store the hit for retrieval on click
                tableRows.addAll(addHitToTable(hit)); // can create multiple rows
            }

            return tableRows;
        }
    }

    /**
     * Retrieves the next page of data and returns it (as List<TableRows>) to the RemotePaginator
     */
    public class GenericSequenceHitDataRetriever extends SequenceWithSampleDataRetriever {

        protected List processData(List hits) {
            if (_logger.isDebugEnabled()) _logger.debug(" " + hits.size() + " sequence hits");

            // Temporary listener to push the first hit to the alignment panel after the first page load is complete
            _seqWithoutSamplePaginator.addDataRetrievedCallback(new FirstPageRetrievedListener(_seqWithoutSampleTable, _seqWithoutSamplePaginator));

            List tableRows = new ArrayList();
            Iterator iter = hits.iterator();
            while (iter.hasNext()) {
                BlastHit hit = (BlastHit) iter.next();
                TableRow row = new TableRow();
                _hitsById.put(hit.getBlastHitId(), hit); // store the hit for retrieval on click

                int col = 0;
                CheckBox checkbox = new CheckBox();
                // checkbox.setValue(_selectAll);  SELECT STATE IS NOT CONTEXT DEPENDENT ANY LONGER

                row.setValue(col++, new TableCell(new CheckBoxComparable(checkbox), checkbox));
                row.setValue(col++, new TableCell(hit.getExpectScore(), getExpectWidget(hit)));
                row.setValue(col++, new TableCell(hit.getBlastHitId()));  // BlastHitId (hidden)
                row.setValue(col++, new TableCell(getEntityAcc(hit)));  //(hidden)
                row.setValue(col++, new TableCell(hit.getBitScoreFormatted()));
                row.setValue(col++, new TableCell(hit.getLengthAlignment()));
                row.setValue(col++, getQueryDeflineTableCell(hit.getQueryEntity()));

                if (hit.getSubjectEntity() != null) {
                    row.setValue(col++, new TableCell(getEntityAcc(hit)));
//                    row.setValue(col++, new TableCell(getEntityAcc(hit), getBseEntityLink(_seqWithoutSampleTable, hit)));
                }
                else {
                    row.setValue(col++, new TableCell("unknown")); // accession
                }

                row.setValue(col, getSubjectDeflineTableCell(hit.getSubjectEntity(), SUBJECT_DEFLINE_VISIBLE_SIZE));

                tableRows.add(row);
            }

            return tableRows;
        }
    }

    private PopperUpperHTML getExpectWidget(BlastHit hit) {
        PopperUpperHTML widget = new PopperUpperHTML(hit.getExpectScoreFormatted(), AlignmentDisplayPanel.queryToHtml(hit.getPairwiseAlignmentNarrow()));
        widget.setLauncher(new PopupBelowLauncher());
        return widget;
    }

    private class BlastHitsAdvancedSortClickListener extends AdvancedSortableRemotePaginatorClickListener {

        private BlastHitsAdvancedSortClickListener(SortableTable table,
                                                   RemotingPaginator paginator,
                                                   RemotePagingPanel pagingPanel) {
            super(table, paginator, pagingPanel);
        }

        public void sortBy(SortableColumn[] sortColumns) {
            getPaginator().clearData();
            // only set the sort columns in the table since the paginator will get them from there
            getPaginator().setSortColumns(sortColumns);
            // put the number of hits back then go and retrieve the first page
            getPagingPanel().getRemotePaginator().setTotalRowCount(_job.getNumHits().intValue());
            getPagingPanel().first();   // handles loading label visiblility
        }

    }

    /**
     * Create a paging panel to control the table
     */
    private void decoratePanel(PagingPanel pagingPanel, VerticalPanel panel) {
        // Add a footer to the paging panel
        pagingPanel.getDataPanel().add(getFooterForTable(pagingPanel));

        // Add the paging panel and hints to the supplied container panel
        panel.add(pagingPanel);
        panel.add(getHintsPanel());
    }

    private Panel getFooterForTable(PagingPanel pagingPanel) {
        DockPanel footer = pagingPanel.getTableFooterPanel();
        footer.add(getControls(pagingPanel), DockPanel.WEST);
        Widget exportMenu = getExportMenu(pagingPanel);
        footer.add(exportMenu, DockPanel.EAST);
        footer.setCellHorizontalAlignment(exportMenu, DockPanel.ALIGN_RIGHT);

        return footer;
    }

    private Panel getHintsPanel() {
        SimplePanel hintPanel = new SimplePanel();
        hintPanel.setStyleName("jobDetailsHitTableHint");
        hintPanel.add(HtmlUtils.getHtml(
                "&bull; Hover over an Eval to view the alignment" +
                        "&nbsp;&nbsp;" +
                        "&bull; Click on a row to see the alignment details" +
                        "<br/>" +
                        "&bull; BLAST results were computed using NCBI BLAST 2.2.15 XML output"
                , "hint"));
        return hintPanel;
    }

    private Widget getControls(PagingPanel pagingPanel) {
        HorizontalPanel panel = new HorizontalPanel();

        panel.add(getSelectControls(pagingPanel));
        //panel.add(HtmlUtils.getHtml("/", "controlSeparator"));
        //panel.add(getShowControls(table));
        DOM.setStyleAttribute(panel.getElement(), "display", "inline");

        return panel;
    }

    private Widget getSelectControls(PagingPanel pagingPanel) {
        OptionItem all = new OptionItem("all this page", new SelectItemsClickListener(pagingPanel, true));
        OptionItem clear = new OptionItem("clear this page", new SelectItemsClickListener(pagingPanel, false));
        return new SelectOptionsLinks("Select", Arrays.asList(all, clear));
    }

    private class SelectItemsClickListener implements ClickListener {
        private PagingPanel _pagingPanel;
        private boolean _setChecked;

        public SelectItemsClickListener(PagingPanel pagingPanel, boolean setChecked) {
            _pagingPanel = pagingPanel;
            _setChecked = setChecked;
        }

        public void onClick(Widget widget) {
            //_selectAll = _setChecked; NO SUCH STATE SUPPORTED ANY LONGER
            Iterator iter = _pagingPanel.getPaginator().createPageRows().iterator();
            while (iter.hasNext())
                ((CheckBox) ((TableRow) iter.next()).getTableCell(CHECKBOX_COL).getWidget()).setValue(_setChecked);
            _pagingPanel.getSortableTable().refreshColumn(CHECKBOX_COL);
        }
    }

    /**
     * extract the IDs from the selected rows
     *
     * @param data is a list of TableRow(s)
     * @return a list of selected IDs
     */
    private List getListOfSelectedIds(Collection data) {
        List selectedIDs = new ArrayList();
        if (data == null) {
            _logger.error("List for selected alignments was null");
            return selectedIDs;
        }
        _logger.debug("Total list size: " + data.size());
        Iterator iter = data.iterator();

        while (iter.hasNext()) {
            TableRow tableRow = (TableRow) iter.next();
            if (((CheckBox) tableRow.getTableCell(CHECKBOX_COL).getWidget()).getValue()) {
                selectedIDs.add(tableRow.getTableCell(ALIGNMENT_ID_COLUMN).getValue());
            }
        }
        _logger.debug("Found " + selectedIDs.size() + " selected row(s)");
        return selectedIDs;
    }

//    /**
//     * formats the list of IDs as a comma separated list
//     * @param listOfIDs is a list of String
//     * @return a comma separated string of IDs
//     */
//    private String formatSelectedIDsAsString(List listOfIDs) {
//        if(listOfIDs == null) {
//            return null;
//        }
//        String idListAsCSL="";
//        int numIDs=0;
//        Iterator iter = listOfIDs.iterator();
//        while (iter.hasNext()) {
//            String id = (String)iter.next();
//            idListAsCSL+= (numIDs>0?",":"") + id;
//            numIDs++;
//        }
//        return idListAsCSL;
//    }

    private Widget getExportMenu(final PagingPanel pagingPanel) {
        final MenuBar menu = new MenuBar();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBar(true);

        dropDown.addItem("Matching Query Sequences as FASTA", true, new BlastResultsExportCommand() {
            public void execute() {
                _logger.debug("Execute called for Matching FASTA (Query)");
                List listOfSelectedIds = getListOfSelectedIds(pagingPanel.getPaginator().getData());
                if (listOfSelectedIds.size() > 0) {
                    BlastHitsExportConfirmationPopup confirmationDlg =
                            new BlastHitsExportConfirmationPopup(listOfSelectedIds, this, false);
                    new PopupCenteredLauncher(confirmationDlg).showPopup(menu);
                }
                else {
                    exportResults(null);
                }
            }

            public void exportResults(List selectedIDsList) {
                _logger.debug("Export query sequences for " +
                        (selectedIDsList == null ? "all results" : selectedIDsList.toString()));
//                SystemWebTracker.trackActivity("Status.ExportJob.Query.FASTA", new String[]{_job.getJobId()});
                SortableColumn[] sortArgs = pagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                // todo Fix the problem with sortArgs!!!
                BlastResultExportTask blastResultExportTask = new BlastResultExportTask(
                        _job.getJobId(), BlastResultExportTask.SEQUENCES_QUERY,
                        ExportWriterConstants.EXPORT_TYPE_FASTA, selectedIDsList, new ArrayList<SortArgument>());
                new AsyncExportTaskController(blastResultExportTask).start();
            }
        });

        dropDown.addItem("Matching Subject Sequences as FASTA", true, new BlastResultsExportCommand() {
            public void execute() {
                _logger.debug("Execute called for All FASTA (Subject)");
                List listOfSelectedIds = getListOfSelectedIds(pagingPanel.getPaginator().getData());
                if (listOfSelectedIds.size() > 0) {
                    BlastHitsExportConfirmationPopup confirmationDlg =
                            new BlastHitsExportConfirmationPopup(listOfSelectedIds, this, false);
                    new PopupCenteredLauncher(confirmationDlg).showPopup(menu);
                }
                else {
                    exportResults(null);
                }
            }

            public void exportResults(List selectedIDsList) {
                _logger.debug("Export subject sequences for " +
                        (selectedIDsList == null ? "all results" : selectedIDsList.toString()));
//                SystemWebTracker.trackActivity("Status.ExportJob.Subject.FASTA", new String[]{_job.getJobId()});
                SortableColumn[] sortArgs = pagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                // todo Fix the problem with sortArgs!!!
                BlastResultExportTask blastResultExportTask = new BlastResultExportTask(
                        _job.getJobId(), BlastResultExportTask.SEQUENCES_SUBJECT,
                        ExportWriterConstants.EXPORT_TYPE_FASTA, selectedIDsList, new ArrayList<SortArgument>());
                new AsyncExportTaskController(blastResultExportTask).start();
            }
        });

        dropDown.addItem("BLAST Results with Metadata as CSV", true, new BlastResultsExportCommand() {
            public void execute() {
                _logger.debug("Execute called for All CSV");
                List listOfSelectedIds = getListOfSelectedIds(pagingPanel.getPaginator().getData());
                if (listOfSelectedIds.size() > 0) {
                    BlastHitsExportConfirmationPopup confirmationDlg =
                            new BlastHitsExportConfirmationPopup(listOfSelectedIds, this, false);
                    new PopupCenteredLauncher(confirmationDlg).showPopup(menu);
                }
                else {
                    exportResults(null);
                }
            }

            public void exportResults(List selectedIDsList) {
                _logger.debug("Export results as CSV for " +
                        (selectedIDsList == null ? "all results" : selectedIDsList.toString()));
//                SystemWebTracker.trackActivity("Status.ExportJob.CSV", new String[]{_job.getJobId()});
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                BlastResultExportTask blastResultExportTask = new BlastResultExportTask(
                        _job.getJobId(), BlastResultExportTask.SEQUENCES_SELECTED,
                        ExportWriterConstants.EXPORT_TYPE_CSV, selectedIDsList, sortList);
                new AsyncExportTaskController(blastResultExportTask).start();
            }
        });

        dropDown.addItem("BLAST Results as NCBI XML", true, new BlastResultsExportCommand() {
            public void execute() {
                _logger.debug("Export (selected) results as NCBI XML");
                List listOfSelectedIds = getListOfSelectedIds(pagingPanel.getPaginator().getData());
                if (listOfSelectedIds.size() > 0) {
                    BlastHitsExportConfirmationPopup confirmationDlg =
                            new BlastHitsExportConfirmationPopup(listOfSelectedIds, this, false);
                    new PopupCenteredLauncher(confirmationDlg).showPopup(menu);
                }
                else {
                    exportResults(null);
                }
            }

            public void exportResults(List selectedIDsList) {
                _logger.debug("Export results as XML for " +
                        (selectedIDsList == null ? "all results" : selectedIDsList.toString()));
//                SystemWebTracker.trackActivity("Status.ExportJob.XML", new String[]{_job.getJobId()});
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                BlastResultExportTask blastResultExportTask = new BlastResultExportTask(
                        _job.getJobId(), BlastResultExportTask.SEQUENCES_SELECTED,
                        ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML, selectedIDsList, sortList);
                new AsyncExportTaskController(blastResultExportTask).start();
            }
        });

        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

    public class HitSelectedListener implements SelectionListener {
        SortableTable _table;

        public HitSelectedListener(SortableTable table) {
            _table = table;
        }

        public void onSelect(String row) {
            if (_alignmentListener == null)
                return;

            TableCell cell = _table.getValue(Integer.valueOf(row).intValue(), ALIGNMENT_ID_COLUMN);
            String id = (String) cell.getValue();
            if (_logger.isDebugEnabled()) _logger.debug("sequence " + id + " in row " + row + " was selected");

            _table.clearHover(); // Have to remove the highlight style since the table will never get a mouse out event
            _alignmentListener.onAlignmentSelected((BlastHit) _hitsById.get(id), _job.getProgram());
        }

        public void onUnSelect(String row) {
        }
    }

    /**
     * Called when the user selected a job to display.
     */
    public void setJob(BlastJobInfo job) {
        if (_logger.isDebugEnabled()) _logger.debug("BlastHitsPanel.setJob() " + job.getJobId());

        _job = job;
        reset();
        _hitsById.clear();

        if (job.isSubjectWithSample()) {
            _blastHitsDataTableInUse = seqWithSampleTable;
            showData(_seqWithSamplePanel);
            _seqWithSamplePagingPanel.getRemotePaginator().setTotalRowCount(_job.getNumHits().intValue());
            _seqWithSamplePagingPanel.first();
        }
        else {
            _blastHitsDataTableInUse = _seqWithoutSampleTable;
            showData(_seqWithoutSamplePanel);
            _seqWithoutSamplePagingPanel.getRemotePaginator().setTotalRowCount(_job.getNumHits().intValue());
            _seqWithoutSamplePagingPanel.first();
        }
    }

    private String getEntityAcc(BlastHit hit) {
        String accession = "";
        if (hit.getSubjectEntity() != null && hit.getSubjectEntity().getAccession() != null)
            accession = String.valueOf(hit.getSubjectEntity().getAccession());
        return accession;
    }

    public class FirstPageRetrievedListener implements DataRetrievedListener {
        private SortableTable _table;
        private RemotingPaginator _paginator;

        public FirstPageRetrievedListener(SortableTable table, RemotingPaginator paginator) {
            _table = table;
            _paginator = paginator;
        }

        public void onSuccess(Object data) {
            removeListener();
            String id = (String) _table.getValue(1, ALIGNMENT_ID_COLUMN).getValue();
            _table.selectRow(1);
            _alignmentListener.onAlignmentSelected((BlastHit) _hitsById.get(id), _job.getProgram());
        }

        public void onFailure(Throwable throwable) {
            removeListener();
        }

        public void onNoData() {
            removeListener();
        }

        private void removeListener() {
            _paginator.removeDataRetrievedCallback(this);
        }
    }

    /**
     * Creates one or more TableRows - 1 per hit per site (i.e. a hit from N samples will result in N rows)
     */
    private List<TableRow> addHitToTable(BlastHit hit) {
        List<TableRow> rows = new ArrayList();
        TableRow row = new TableRow();

        int col = 0;
        // Checkbox
        CheckBox checkbox = new CheckBox();
        checkbox.setValue(false); // checkbox.setValue(_selectAll);  NO LONGER SUPPORTED
        row.setValue(col++, new TableCell(new CheckBoxComparable(checkbox), checkbox));

        // Hit info
        row.setValue(col++, new TableCell(hit.getExpectScore(), getExpectWidget(hit)));
        row.setValue(col++, new TableCell(hit.getBlastHitId())); // hidden
        row.setValue(col++, new TableCell(getEntityAcc(hit)));     // hidden
        row.setValue(col++, new TableCell(hit.getBitScoreFormatted()));
        row.setValue(col++, new TableCell(hit.getLengthAlignment()));
        row.setValue(col++, getQueryDeflineTableCell(hit.getQueryEntity()));
        row.setValue(col++, new TableCell(getEntityAcc(hit)));
        //row.setValue(col++, new TableCell(hit.getSubjectEntity().getAccession(), getBseEntityLink(seqWithSampleTable, hit)));

        // Sample and site info
        if (hit instanceof BlastHitWithSample) {
            row.setValue(col++, getSampleNameTableCell((BlastHitWithSample) hit));
            row.setValue(col++, getSampleLocationTableCell((BlastHitWithSample) hit));
        }
        else {
            row.setValue(col++, new TableCell("&nbsp;"));
            row.setValue(col++, new TableCell("&nbsp;"));
        }

        rows.add(row);

        return rows;
    }

    private TableCell getSampleNameTableCell(BlastHitWithSample hit) {
        if (hit.getSample() == null) {
            return new TableCell("unknown");
        }
        // we can no longer support multiple samples for one hit
        String sampleName = hit.getSample().getSampleName();
        return new TableCell(sampleName, new FulltextPopperUpperHTML(sampleName, SAMPLE_NAME_VISIBLE_SIZE));
    }

    private TableCell getSampleLocationTableCell(BlastHitWithSample hit) {
        List sites = getSites(hit.getSample());
        if (sites == null || sites.size() == 0) {
            return new TableCell("unknown");
        }
        // Simple case if only 1 site
        if (sites.size() == 1) {
            String sampleLocation = ((Site) sites.iterator().next()).getSampleLocation();
            return new TableCell(sampleLocation, new FulltextPopperUpperHTML(sampleLocation, SAMPLE_LOCATION_VISIBLE_SIZE));
        }
        // Multiple sites
        StringBuffer popup = new StringBuffer();
        StringBuffer locations = new StringBuffer();
        for (int i = 0; i < sites.size(); i++) {
            String sampleLocation = ((Site) sites.get(i)).getSampleLocation();
            locations.append(locations.length() == 0 ? "" : "; ").append(sampleLocation);
            popup.append("&bull;&nbsp;").append(sampleLocation).append("<br>");
        }
        HTML popupHtml = HtmlUtils.getHtml(popup.toString(), "infoText");
        // TODO: make a flavor of FullTextPopperUpperHTML(String longTextToAbbreviate, HTML popupContents)
        return new TableCell(locations.toString(),
                new PopperUpperHTML(FulltextPopperUpperHTML.abbreviateText(locations.toString(), SAMPLE_LOCATION_VISIBLE_SIZE), popupHtml));
    }

    /**
     * Accrue all the sites for all the samples in a set, and sort them alphabetically
     */
    private List getSites(Sample sample) {
        List sites = new ArrayList();
        if (sample == null) {
            return sites;
        }
        Set sampleSites = sample.getSites();
        if (sampleSites != null && sampleSites.size() > 0) {
            sites.addAll(sampleSites);
        }
        Collections.sort(sites, new SortSiteBySampleLocationComparator());
        return sites;
    }

    public class SortSiteBySampleLocationComparator implements Comparator {
        public int compare(Object object1, Object object2) {
            String loc1 = ((Site) object1).getSampleLocation();
            String loc2 = ((Site) object2).getSampleLocation();
            if (loc1 == null && loc2 == null) return 0;
            if (loc2 == null) return 1;
            if (loc1 == null) return -1;
            return loc1.compareTo(loc2);
        }
    }

    /**
     * We need to put an intelligent summary of the defline in the narrow Query column.  We'll use the entity
     * description, rather than the full defline, because the full defline has been prepended with the internal
     * accession, while description is the original string the user entered.  If the entire defline is < 25 chars,
     * we'll display the whole thing.   Often the defline begins with an identifier; if it does, we'll put the full
     * identifier (up to 25 chars) in the column, with a popperUpper with the full defline. Otherwise, we'll just show
     * the first 22 chars (somewhat arbitrarily) with a PopperUpper showing the whole defline.
     */
    private TableCell getQueryDeflineTableCell(BaseSequenceEntity entity) {
        if (entity == null || entity.getDescription() == null)
            return new TableCell("unknown");

        String defline = entity.getDefline();
        String deflineFormatted = entity.getDeflineFormatted();

        // Short defline - show it all
        if (defline.length() < QUERY_DEFLINE_VISIBLE_SIZE) {
            // Identifier < 25 chars, show the identifier with a popup containing the whole defline
            return new TableCell(defline);
        }
        else if (defline.indexOf(" ") > QUERY_DEFLINE_IDENTIFIER_MIN_VISIBLE_SIZE &&
                defline.indexOf(" ") < QUERY_DEFLINE_IDENTIFIER_MAX_VISIBLE_SIZE) {
            // Otherwise random text, so show the first N characters and a popup with the whole defline
            return new TableCell(defline, new FulltextPopperUpperHTML(defline.substring(0, defline.indexOf(" ")), deflineFormatted));
        }
        else {
            return new TableCell(defline,
                    new FulltextPopperUpperHTML(FulltextPopperUpperHTML.abbreviateText(defline, QUERY_DEFLINE_VISIBLE_SIZE), deflineFormatted));
        }
    }

    /**
     * For subject entity, always show the defline (NOT description, as is the case for query sequences)
     */
    private TableCell getSubjectDeflineTableCell(BaseSequenceEntity entity, int size) {
        if (entity == null || entity.getDefline() == null)
            return new TableCell("unknown");

        //PopperUpperHTML popperUpper = new FulltextPopperUpperHTML(entity.getDefline(), size);
        //popperUpper.getPopup().addStyleName("jobDetailsDeflinePopup"); // restrict width
        //return new TableCell(entity.getDefline().substring(0, size), popperUpper);
        //return new TableCell(entity.getDefline(), new FulltextPopperUpperHTML(entity.getDefline(), size));
        return new TableCell(entity.getDefline(), new FulltextPopperUpperHTML(entity.getDeflineFormatted(), size));
    }

    private Widget getBseEntityLink(final SortableTable table, final BlastHit hit) {
        final String entityAcc = getEntityAcc(hit);

        if (entityAcc == null || entityAcc.length() == 0)
            return HtmlUtils.getHtml("unknown", "error");

        Widget widget = new Link(hit.getSubjectEntity().getAccession(), new ClickListener() {
            public void onClick(Widget widget) {
                if (table != null)
                    table.clearHover(); // Have to remove the highlight style since the table will never get a mouse out event
                _entityListener.onEntitySelected(entityAcc, null);
            }
        });
        return widget;
    }

    public void reset() {
        showLoading();
        _blastHitsDataTableInUse = null;
        _seqWithSamplePagingPanel.clear();
        _seqWithoutSamplePagingPanel.clear();
        // _selectAll = false;  NO LONGER MAINTAINED
    }

    /**
     * "Loading" state - clears the hit tables and shows the loading message
     */
    public void showLoading() {
        _loadingLabel.setVisible(true);
        _errorLabel.setVisible(false);
        _noDataLabel.setVisible(false);
        _seqWithSamplePanel.setVisible(false);
        _seqWithoutSamplePanel.setVisible(false);
    }

    /**
     * "Error" state - clears the hit tables and shows the error message
     */
    public void showError() {
        _errorLabel.setVisible(true);
        _loadingLabel.setVisible(false);
        _noDataLabel.setVisible(false);
        _seqWithSamplePanel.setVisible(false);
        _seqWithoutSamplePanel.setVisible(false);
    }

    /**
     * "Data" state - shows the specified panel
     */
    public void showData(Panel panel) {
        _errorLabel.setVisible(false);
        _loadingLabel.setVisible(false);
        _noDataLabel.setVisible(false);
        _seqWithSamplePanel.setVisible(false);
        _seqWithoutSamplePanel.setVisible(false);
        panel.setVisible(true);
    }

    /**
     * "No Data" state - shows the specified panel
     */
    public void showNoData() {
        _noDataLabel.setVisible(true);

        _errorLabel.setVisible(false);
        _loadingLabel.setVisible(false);
        _seqWithSamplePanel.setVisible(false);
        _seqWithoutSamplePanel.setVisible(false);
    }

}
