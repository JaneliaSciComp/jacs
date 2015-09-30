
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.ImagePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.RemoveJobEventHandler;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOracleListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Todd Safford
 */
abstract public class QuerySequenceChooserBaseTab implements QuerySequenceChooserTab {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserBaseTab");

    protected SortableTable _table;
    protected RemotingPaginator _paginator;
    protected RemotePagingPanel _pagingPanel;
    protected SearchOraclePanel _oraclePanel;
    protected String _searchString = null;

    protected static final int SUBJECT_DB_SIZE = 40;

    protected static final int DEFAULT_NUM_ROWS = 10;
    protected static final int ID_COLUMN = 0;
    protected static final int DELETE_COLUMN = 1;
    protected static final int IMAGE_COLUMN = 2;
    protected static final int NAME_COLUMN = 3;
    protected static final int QUERY_COLUMN = 4;
    protected static final int SUBJECT_COLUMN = 5;
    protected static final int LENGTH_COLUMN = 6;
    protected static final int NUM_HITS_COLUMN = 7;

    protected static final int THUMBNAIL_IMAGE_WIDTH = 25;
    protected static final int THUMBNAIL_IMAGE_HEIGHT = 25;
    protected static final int POPUP_IMAGE_WIDTH = 150;
    protected static final int POPUP_IMAGE_HEIGHT = 150;

    protected static final String TASKID_HEADING = "TaskId";
    protected static final String QUERY_HEADING = "Query";
    protected static final String SUBJECT_HEADING = "Subject";
    protected static final String NUM_HITS_HEADING = "# Hits";
    protected static final String LENGTH_HEADING = "Length";
    protected static final String NAME_HEADING = "Name";

    protected boolean loaded = false;
    protected JobSelectionListener _jobSelectionListener;
    protected JobSelectionListener _jobSelectionAndApplyListener;
    protected HashMap<String, RecruitableJobInfo> _jobs = new HashMap<String, RecruitableJobInfo>();
    protected TextColumn nameColumn = new TextColumn(NAME_HEADING, /*sortable*/ true, /*visible*/ false);
    protected ImageColumn deleteColumn = new ImageColumn("", /*visible*/ false);
    protected static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public QuerySequenceChooserBaseTab() {
        super();
    }

    public JobSelectionListener getRecruitableJobSelectionListener() {
        return _jobSelectionListener;
    }

    public void setRecruitableJobSelectionListener(JobSelectionListener jobSelectionListener) {
        _jobSelectionListener = jobSelectionListener;
    }

    public JobSelectionListener getRecruitableJobSelectionAndApplyListener() {
        return _jobSelectionAndApplyListener;
    }

    public void setRecruitableJobSelectionAndApplyListener(JobSelectionListener jobSelectionAndApplyListener) {
        _jobSelectionAndApplyListener = jobSelectionAndApplyListener;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setIsLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public HashMap<String, RecruitableJobInfo> getJobs() {
        return _jobs;
    }

    public void setJobs(HashMap<String, RecruitableJobInfo> jobs) {
        _jobs = jobs;
    }

    public static StatusServiceAsync getStatusService() {
        return _statusservice;
    }

    protected Widget getHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click once to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }

    protected Widget getSearchArea() {
        // Create the oracle and hook up callbacks that will repopulate the table
        _oraclePanel = new SearchOraclePanel("Query", new SearchOracleListener() {
            public void onRunSearch(String searchString) {
                _searchString = searchString;
                reloadData();
            }

            public void onShowAll() {
                _searchString = null;
                reloadData();
            }
        });
        _oraclePanel.addSuggestBoxStyleName("FRVPreviousSequenceSuggestBox");

        return _oraclePanel;
    }

    public void reloadData() {
        _oraclePanel.removeOracleSuggestions();
        populateSuggestOracle(); // retrieve the names for the suggestion box
        _pagingPanel.clear();
        _pagingPanel.first();
    }

    abstract void populateSuggestOracle();

    protected Widget getTable() {
        String[][] sortConstants = new String[][]{
                {"", ""}, // hidden ID
                {"", ""}, // delete column
                {"", ""}, // image column
                {JobInfo.SORT_BY_JOB_NAME, NAME_HEADING}, // name column
                {RecruitableJobInfo.QUERY_SORT, QUERY_HEADING},
                {"", ""}, // hidden subject column
                {RecruitableJobInfo.LENGTH_SORT, LENGTH_HEADING},
                {RecruitableJobInfo.HITS_SORT, NUM_HITS_HEADING}
        };

        _table = new SortableTable();
        _table.setWidth("100%");
        _table.addColumn(new TextColumn(TASKID_HEADING, false, false)); // hidden
        _table.addColumn(deleteColumn); // delete column
        _table.addColumn(new ImageColumn("&nbsp;")); // preview image column
        _table.addColumn(nameColumn); // hide for system data
        _table.addColumn(new TextColumn(QUERY_HEADING));
        _table.addColumn(new TextColumn(SUBJECT_HEADING, true, false)); // hidden until something useful to display
        _table.addColumn(new NumericColumn(LENGTH_HEADING, "Microbe Genome Length"));
        _table.addColumn(new NumericColumn(NUM_HITS_HEADING, "Number of matching sequences"));
        _table.setHighlightSelect(true);
        _table.addSelectionListener(new RowSelectedListener());
        _table.addDoubleClickSelectionListener(new RowSelectedWithDoubleClickListener());
        _table.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(QUERY_COLUMN, QUERY_HEADING, SortableColumn.SORT_ASC)
        });

        _paginator = new RemotingPaginator(_table, new RecruitementNodeRetriever(), sortConstants,
                "QuerySequenceChooser");
        _pagingPanel = new RemotePagingPanel(_table, _paginator, DEFAULT_NUM_ROWS, "QuerySequenceChooser");
        _pagingPanel.setStyleName("querySequenceChooserPopup");
        _pagingPanel.setNoDataMessage("No query sequences found.");

        return _pagingPanel;
    }

    /**
     * When a table row is selected, notify the panel that houses this tab
     */
    public class RowSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            _logger.debug("row " + value + " selected");
            if (getRecruitableJobSelectionListener() != null)
                getRecruitableJobSelectionListener().onSelect(getSelectedJob());
        }

        public void onUnSelect(String value) {
            if (getRecruitableJobSelectionListener() != null)
                getRecruitableJobSelectionListener().onUnSelect();
        }
    }

    public class RowSelectedWithDoubleClickListener implements DoubleClickSelectionListener {
        public void onSelect(String value) {
            _logger.debug("row " + value + " selected with double click");
            if (getRecruitableJobSelectionAndApplyListener() != null)
                getRecruitableJobSelectionAndApplyListener().onSelect(getSelectedJob());
        }
    }

    protected RecruitableJobInfo getSelectedJob() {
        String jobId = _table.getSelectedRow().getRowObject().getTableCell(0).getValue().toString();
        return getJobs().get(jobId);
    }

    /**
     * External notification that the data should be loaded
     */
    public void realize() {
        _logger.debug("QuerySequenceChooserSystemTab.realize()");

        if (isLoaded())
            _table.clearHover();  // data's already retrieved, but we need to clear any residual hover artifacts
        else
            new LoadQuerySequencesTimer().schedule(1); // start the data retrieval
        _table.clearSelect();
    }

    public class LoadQuerySequencesTimer extends Timer {
        public void run() {
            _logger.debug("QuerySequenceChooserSystemTab loading data");

            populateSuggestOracle(); // retrieve the names for the suggestion box
            _pagingPanel.first();    // retrieve the first page of results
            setIsLoaded(true);
        }
    }

    abstract void getNumTaskResults(DataRetrievedListener listener);

    abstract void getPagedTaskResults(String searchString, int startIndex, int numRows, SortArgument[] sortArgs, final DataRetrievedListener listener);

    protected List processDataBase(RecruitableJobInfo[] jobs) {
        List<TableRow> tableRows = new ArrayList<TableRow>();
        if (jobs != null) {
            for (RecruitableJobInfo job : jobs) {
                TableRow tableRow = new TableRow();
                tableRow.setValue(ID_COLUMN, new TableCell(job.getJobId()));        // hidden
                tableRow.setValue(DELETE_COLUMN, new TableCell("&nbsp;", createRemoveJobWidget(job, tableRow)));
                tableRow.setValue(IMAGE_COLUMN, new TableCell("", getImage(job))); // visible but not sortable
                tableRow.setValue(NAME_COLUMN, new TableCell(job.getJobname()));
                tableRow.setValue(QUERY_COLUMN, new TableCell(job.getQueryName()));
                tableRow.setValue(SUBJECT_COLUMN,
                        new TableCell(job.getSubjectName(), new FulltextPopperUpperHTML(job.getSubjectName(), SUBJECT_DB_SIZE)));
                tableRow.setValue(LENGTH_COLUMN, new TableCell(job.getGenomeLengthFormatted()));
                tableRow.setValue(NUM_HITS_COLUMN, new TableCell(job.getNumHitsFormatted()));

                tableRows.add(tableRow);
                getJobs().put(job.getJobId(), job);
            }
        }

        return tableRows;
    }

    /**
     * Creates the job remove widget
     *
     * @param job job to create the widget for
     * @param row row in the table the job is associated with
     * @return returns a job removal widget
     */
    private Widget createRemoveJobWidget(JobInfo job, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveJobEventHandler(job, row, _pagingPanel));
        return image;
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class RecruitementNodeRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            getNumTaskResults(listener);
        }

        public void retrieveDataRows(int startIndex, int numRows, SortArgument[] sortArgs, final DataRetrievedListener listener) {
            getPagedTaskResults(_searchString, startIndex, numRows, sortArgs, listener);
        }

        protected List getFakeData() {
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (int i = 0; i < 10; i++) {
                TableRow tableRow = new TableRow();
                tableRow.setValue(ID_COLUMN, new TableCell(i));
                tableRow.setValue(IMAGE_COLUMN, new TableCell("", getImage(null))); // visible but not sortable
                tableRow.setValue(QUERY_COLUMN, new TableCell("query name " + i));
                tableRow.setValue(SUBJECT_COLUMN,
                        new TableCell("abbreviated subject name " + i,
                                new FulltextPopperUpperHTML("this is the very long version of the subject name " + i, 20)));
                tableRow.setValue(NUM_HITS_COLUMN, new TableCell("" + i));
                tableRows.add(tableRow);

                getJobs().put(Integer.toString(i), new RecruitableJobInfo());
            }

            return tableRows;
        }

        protected List processData(RecruitableJobInfo[] jobs) {
            return processDataBase(jobs);
        }
    }

    protected Widget getImage(final RecruitableJobInfo job) {
        final Image image = new Image();
        image.setWidth(THUMBNAIL_IMAGE_WIDTH + "px");
        image.setHeight(THUMBNAIL_IMAGE_HEIGHT + "px");
        if (job != null) {
            ImageMouseHandler tmpMouseHandler = new ImageMouseHandler(FrvImagePanel.getTileUrl(0, 0, 0, job));
            image.addMouseOverHandler(tmpMouseHandler);
            image.addMouseOutHandler(tmpMouseHandler);
        }
        return image;
    }

    // TODO: make ImagePopperUpper
    public class ImageMouseHandler implements MouseOverHandler, MouseOutHandler {
        private String _url;
        private PopupLauncher _launcher;

        public ImageMouseHandler(String url) {
            _url = url;
        }

        @Override
        public void onMouseOut(MouseOutEvent mouseOutEvent) {
            Window.alert("Heard the mouse out.");
            _launcher.hidePopup();
        }

        @Override
        public void onMouseOver(MouseOverEvent mouseOverEvent) {
            Image popupImage = new Image();
            popupImage.setWidth(POPUP_IMAGE_WIDTH + "px");
            popupImage.setHeight(POPUP_IMAGE_HEIGHT + "px");
            popupImage.setUrl(_url);

            //TODO: make an ImagePopperUpper
            Window.alert("Heard the mouse over");
            _launcher = new PopupAboveLauncher(new ImagePopup(popupImage));
            _launcher.showPopup(_table);
        }
    }

    abstract public String getTabLabel();
}
