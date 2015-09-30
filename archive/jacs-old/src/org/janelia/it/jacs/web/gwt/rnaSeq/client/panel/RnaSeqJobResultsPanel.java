
package org.janelia.it.jacs.web.gwt.rnaSeq.client.panel;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RnaSeqJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.GeneralJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.JobNameWidget;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.JobStatusHelper;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.DateColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.AdvancedSortableRemotePaginatorClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * First page of the job results wizard; shows a table of all jobs.
 */
public class RnaSeqJobResultsPanel extends GeneralJobResultsPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.rnaSeq.client.panel.RnaSeqJobResultsPanel");
    public static final String TASK_RNA_SEQ_PIPELINE = "RnaSeqPipelineTask";

    public static final String NUM_HITS_HEADING = "# Hits";
    public static final String SUBJECT_HEADING = "Subject Sequences";
    public static final String RESULTS_PATH_HEADING = "Directory Path";

    private static final int DELETE_JOB_COLUMN = 0;
    private static final int JOB_NAME_COLUMN = 1;
    private static final int JOB_TIMESTAMP_COLUMN = 2;
    private static final int JOB_STATUS_COLUMN = 3;
    private static final int JOB_RESULTS_PATH_COLUMN = 4;
    private static final int ACTIONS_COLUMN = 5;

    public RnaSeqJobResultsPanel(JobSelectionListener jobSelectionListener,
                                 JobSelectionListener reRunJobListener, String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(TASK_RNA_SEQ_PIPELINE, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "RnaSeqResults");
    }

    protected Widget createTable(String[] rowsPerPageOptions) {
        // Add a to the panel - will be populated asynchronously by service call
        VerticalPanel tablePanel = new VerticalPanel();
        _table = new SortableTable();
        try {
            _table.addColumn(new ImageColumn("&nbsp;")); // job delete icon
            //_table.addColumn(new TextColumn("Job Id"));
            _table.addColumn(new TextColumn(JobStatusHelper.JOB_NAME_HEADING));
            _table.addColumn(new DateColumn(JobStatusHelper.JOB_TIMESTAMP_HEADING));
            _table.addColumn(new TextColumn(JobStatusHelper.JOB_STATUS_HEADING));
            _table.addColumn(new TextColumn(RESULTS_PATH_HEADING));
            _table.addColumn(new TextColumn(ACTIONS_HEADING, false));

            String[][] sortConstants = new String[][]{
                    {"", ""}, // SORT_BY_JOB_ID
                    {JobInfo.SORT_BY_JOB_NAME, JobStatusHelper.JOB_NAME_HEADING},
                    {JobInfo.SORT_BY_SUBMITTED, JobStatusHelper.JOB_TIMESTAMP_HEADING},
                    {JobInfo.SORT_BY_STATUS, JobStatusHelper.JOB_STATUS_HEADING}
            };
            JobStatusPaginator dataPaginator = new JobStatusPaginator(_table, new JobDataRetriever(), sortConstants);
            _pagingPanel = new RemotePagingPanel(_table, rowsPerPageOptions, /*scroll*/ false, /*show loading*/ true,
                    dataPaginator, /*footer*/ true, 0/**RemotePagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER**/,
                    _defaultRowsPerPage, "RnaSeqResults");
            _table.setDefaultSortColumns(new SortableColumn[]{
                    new SortableColumn(JOB_TIMESTAMP_COLUMN, JobStatusHelper.JOB_TIMESTAMP_HEADING, SortableColumn.SORT_DESC)
            });
            _pagingPanel.setNoDataMessage("No job results.");
            _pagingPanel.addAdvancedSortClickListener(new AdvancedSortableRemotePaginatorClickListener
                    (_table, dataPaginator, _pagingPanel));
            tablePanel.add(_pagingPanel);
        }
        catch (Exception e) {
            _logger.error("Problem creating the RnaSeq job result table. " + e.getMessage());
        }

        return _pagingPanel;
    }

    public void showDeletionColumn(boolean showColumn) {
        _table.getCol(DELETE_JOB_COLUMN).setVisible(showColumn);
    }

    public void showActionColumn(boolean showColumn) {
        _table.getCol(ACTIONS_COLUMN).setVisible(showColumn);
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */


    public class JobDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _statusservice.getNumTaskResultsForUser("RnaSeqPipelineTask", new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("RnaSeqJobResultsPanel.JobDataRetriever.getNumTaskResultsForUser().onFailure(): ", caught);
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("RnaSeqJobResultsPanel.JobDataRetriever.getNumTaskResultsForUser().onSuccess() got " + result);
                    listener.onSuccess(result); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _statusservice.getPagedRnaSeqPipelineTaskResultsForUser(TASK_RNA_SEQ_PIPELINE, startIndex, numRows, sortArgs, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("RnaSeqJobResultsPanel.JobDataRetriever.retrieveDataRows().onFailure(): ", caught);
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    RnaSeqJobInfo[] jobs = (RnaSeqJobInfo[]) result;
                    if (jobs == null || jobs.length == 0) {
                        _logger.debug("RnaSeqJobResultsPanel.JobDataRetriever.retrieveDataRows().onSuccess() got no data");
                        listener.onNoData();
                    }
                    else {
                        _logger.debug("RnaSeqJobResultsPanel.JobDataRetriever.retrieveDataRows().onSuccess() got data");
                        listener.onSuccess(formatData((RnaSeqJobInfo[]) result));
                    }
                }
            });
        }


        private List formatData(RnaSeqJobInfo[] jobs) {
            _logger.debug("RnaSeqJobResultsPanel.JobDataRetriever processing " + jobs.length + " jobs");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (final RnaSeqJobInfo job : jobs) {
                if (job == null) continue; // temporary until DAOs return real paged data

                TableRow tableRow = new TableRow();
                tableRow.setValue(DELETE_JOB_COLUMN, new TableCell("&nbsp;", createRemoveJobWidget(job, tableRow)));
                tableRow.setValue(JOB_NAME_COLUMN, new TableCell(job.getJobname(), JobNameWidget.getWidget(job, tableRow, JOB_NAME_COLUMN, _pagingPanel)));
                TableCell dateCell;
                if (null != job.getSubmitted()) {
                    dateCell = new TableCell(new FormattedDateTime(job.getSubmitted().getTime()));
                }
                else {
                    dateCell = new TableCell("NA");
                }
                tableRow.setValue(JOB_TIMESTAMP_COLUMN, dateCell);

                tableRow.setValue(JOB_STATUS_COLUMN, new TableCell(job.getStatus(), _jobStatusHelper.createJobStatusWidget(job, ignoreStatusMessages())));
                tableRow.setValue(JOB_RESULTS_PATH_COLUMN, new TableCell(job.getJobResultsDirectoryPath()));
                tableRow.setValue(ACTIONS_COLUMN, new TableCell("&nbsp;", getActionColumnWidget(job)));
                tableRows.add(tableRow);
                // If the job's not completed (and not too old), start a timer so we can update the GUI as the status changes
                if (!Task.isDone(job.getStatus()) &&
                        _jobStatusHelper.isJobSubmittedSuccessfully(job)) {

                    final TableRow currentTableRow = tableRow;
                    _jobStatusHelper.startJobResultMonitor(job, new JobStatusListener() {
                        private String _previousStatus = null;
                        private Integer _previousPercentComplete = null;

                        public void onJobRunning(JobInfo newJobInfo) {
                            _logger.debug("Job " + newJobInfo.getJobId() + " still running, status="+newJobInfo.getStatus()+
                                    ", description="+newJobInfo.getStatusDescription()+", time="+new Date().getTime());
                            updateDisplay(newJobInfo, false);
                        }

                        public void onJobFinished(JobInfo newJobInfo) {
                            _logger.info("Job " + newJobInfo.getJobId() +
                                    " completed, status = " + newJobInfo.getStatus());
                            _jobStatusHelper.removeJobResultMonitor(newJobInfo.getJobId());
                            updateDisplay(newJobInfo, true);
                            notifyJobCompletedListener(newJobInfo);
                        }

                        public void onCommunicationError() {
                            _logger.error("Communication error retrieving status for job " + job.getJobId());
                        }

                        private void updateDisplay(JobInfo newJobInfo, boolean isDone) {
                            if (newJobInfo == null) {
                                _logger.error("got invalid (null) task");
                                return;
                            }
                            // if the job is done update it no matter what the status is
                            if (!isDone && _previousStatus != null &&
                                    _previousStatus.equals(newJobInfo.getStatus()) &&
                                    _previousPercentComplete.equals(newJobInfo.getPercentComplete())) {

                                // nothing has changed yet
                                return;
                            }

                            _previousStatus = newJobInfo.getStatus(); // update the previous status

                            _previousPercentComplete = newJobInfo.getPercentComplete();

                            // Update the paging panel's data model with the new status and num hits
                            TableCell statusCell = currentTableRow.getTableCell(JOB_STATUS_COLUMN);

                            Widget statusWidget = _jobStatusHelper.createJobStatusWidget(newJobInfo, ignoreStatusMessages());

                            statusCell.setValue(newJobInfo.getStatus()); // for sorting

                            statusCell.setWidget(statusWidget); // for display

                            // If this job's row is currently visible in the table, tell the table to refresh it
                            if (statusCell.getRow() != TableCell.NOT_SET && statusCell.getCol() != TableCell.NOT_SET) {
                                _pagingPanel.getSortableTable().refreshCell(statusCell.getRow(), statusCell.getCol());
                            }
                            if (isDone) {
                                SafeEffect.highlight(statusWidget, new EffectOption[]{new EffectOption("duration", "4.0")});
                            }
                        }
                    });
                }
            }
            _haveData = true;
            return tableRows;
        }
    }

}