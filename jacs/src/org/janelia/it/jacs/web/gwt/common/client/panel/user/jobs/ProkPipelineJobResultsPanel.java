
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
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
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 28, 2008
 * Time: 11:38:01 AM
 */
public class ProkPipelineJobResultsPanel extends GeneralJobResultsPanel {
    private static Logger _logger = Logger.getLogger("ProkPipelineJobResultsPanel");

    private static final int DELETE_JOB_COLUMN = 0;
    private static final int JOB_NAME_COLUMN = 1;
    private static final int JOB_SUBMITTER_COLUMN = 2;
    private static final int JOB_TIMESTAMP_COLUMN = 3;
    private static final int JOB_STATUS_COLUMN = 4;
    private static final int ACTIONS_COLUMN = 5;


    public ProkPipelineJobResultsPanel(String taskType, JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                       String[] rowsPerPageOptions, int defaultRowsPerPage) {
        // Should use...
        // select t from Task t where t.taskDeleted=false and t.expirationDate=null and
        // t.taskName in ('prokAnnotationTask','prokAnnotationLoadGenomeDataTask','prokAnnotationServiceLoadGenomeDataTask')
        // order by t.objectId desc
        super(taskType, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "ProkPipelineJobResults", true);
    }

    protected Widget createTable(String[] rowsPerPageOptions) {
        // Add a to the panel - will be populated asynchronously by service call
        VerticalPanel tablePanel = new VerticalPanel();
        _table = new SortableTable();
        _table.addColumn(new ImageColumn("&nbsp;")); // job delete icon
        //_table.addColumn(new TextColumn("Job Id"));
        _table.addColumn(new TextColumn(JobStatusHelper.JOB_NAME_HEADING));
        _table.addColumn(new TextColumn(JobStatusHelper.JOB_SUBMITTER_HEADING));
        _table.addColumn(new DateColumn(JobStatusHelper.JOB_TIMESTAMP_HEADING));
        _table.addColumn(new TextColumn(JobStatusHelper.JOB_STATUS_HEADING));
        _table.addColumn(new TextColumn(ACTIONS_HEADING, false));

        String[][] sortConstants = new String[][]{
                {"", ""}, // SORT_BY_JOB_ID
                {JobInfo.SORT_BY_JOB_NAME, JobStatusHelper.JOB_NAME_HEADING},
                {JobInfo.SORT_BY_JOB_NAME, JobStatusHelper.JOB_SUBMITTER_HEADING},
                {JobInfo.SORT_BY_SUBMITTED, JobStatusHelper.JOB_TIMESTAMP_HEADING},
                {JobInfo.SORT_BY_STATUS, JobStatusHelper.JOB_STATUS_HEADING}
        };
        JobStatusPaginator dataPaginator = new JobStatusPaginator(_table, new JobDataRetriever(), sortConstants);
        //_pagingPanel = new RemotePagingPanel(_table, dataPaginator, true, RemotePagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER);
        _pagingPanel = new RemotePagingPanel(_table, rowsPerPageOptions, /*scroll*/ false, /*show loading*/ true,
                dataPaginator, /*footer*/ true, 0/**RemotePagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER**/,
                _defaultRowsPerPage, _rowsPerPagePreferenceKey);
        _table.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(JOB_TIMESTAMP_COLUMN, JobStatusHelper.JOB_TIMESTAMP_HEADING, SortableColumn.SORT_DESC)
        });
        _pagingPanel.setNoDataMessage("No job results.");
        _pagingPanel.addAdvancedSortClickListener(new AdvancedSortableRemotePaginatorClickListener
                (_table, dataPaginator, _pagingPanel));
        tablePanel.add(_pagingPanel);

        return _pagingPanel;
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class JobDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            if (!_collaborationMode) {
                _statusservice.getNumTaskResultsForUser(_taskClassNameToWatch, new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _logger.error("JobDataRetriever.getNumTaskResultsForUser().onFailure(): ", caught);
                        listener.onFailure(caught);
                    }

                    // On success, populate the table with the DataNodes received
                    public void onSuccess(Object result) {
                        _logger.debug("JobDataRetriever.getNumTaskResultsForUser().onSuccess() got " + result);
                        listener.onSuccess(result); // Integer
                    }
                });
            }
            else {
                _statusservice.getNumTaskResults(_taskClassNameToWatch, new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _logger.error("JobDataRetriever.getNumTaskResultsForUser().onFailure(): ", caught);
                        listener.onFailure(caught);
                    }

                    // On success, populate the table with the DataNodes received
                    public void onSuccess(Object result) {
                        _logger.debug("JobDataRetriever.getNumTaskResultsForUser().onSuccess() got " + result);
                        listener.onSuccess(result); // Integer
                    }
                });
            }
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            if (!_collaborationMode) {
                _statusservice.getPagedTaskResultsForUser(_taskClassNameToWatch, startIndex, numRows, sortArgs, new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _logger.error("JobDataRetriever.retrieveDataRows().onFailure(): ", caught);
                        listener.onFailure(caught);
                    }

                    // On success, populate the table with the DataNodes received
                    public void onSuccess(Object result) {
                        JobInfo[] jobs = (JobInfo[]) result;
                        if (jobs == null || jobs.length == 0) {
                            _logger.debug("JobDataRetriever.retrieveDataRows().onSuccess() got no data");
                            listener.onNoData();
                        }
                        else {
                            _logger.debug("JobDataRetriever.retrieveDataRows().onSuccess() got data");
                            listener.onSuccess(formatData(jobs));
                        }
                    }
                });
            }
            else {
                _statusservice.getPagedTaskResults(_taskClassNameToWatch, startIndex, numRows, sortArgs, new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _logger.error("JobDataRetriever.retrieveDataRows().onFailure(): ", caught);
                        listener.onFailure(caught);
                    }

                    // On success, populate the table with the DataNodes received
                    public void onSuccess(Object result) {
                        JobInfo[] jobs = (JobInfo[]) result;
                        if (jobs == null || jobs.length == 0) {
                            _logger.debug("JobDataRetriever.retrieveDataRows().onSuccess() got no data");
                            listener.onNoData();
                        }
                        else {
                            _logger.debug("JobDataRetriever.retrieveDataRows().onSuccess() got data");
                            listener.onSuccess(formatData(jobs));
                        }
                    }
                });
            }
        }

        protected List formatData(JobInfo[] jobs) {
            _logger.debug("JobDataRetriever processing " + jobs.length + " jobs");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (JobInfo job : jobs) {
                if (job == null) // temporary until DAOs return real paged data
                    continue;
                TableRow tableRow = new TableRow();
                tableRow.setValue(DELETE_JOB_COLUMN, new TableCell("&nbsp;", createRemoveJobWidget(job, tableRow)));
                tableRow.setValue(JOB_NAME_COLUMN, new TableCell(job.getJobname(),
                        JobNameWidget.getWidget(job, tableRow, JOB_NAME_COLUMN, _pagingPanel)));
                tableRow.setValue(JOB_SUBMITTER_COLUMN, new TableCell(job.getUsername()));
                tableRow.setValue(JOB_TIMESTAMP_COLUMN, new TableCell(new FormattedDateTime(job.getSubmitted().getTime())));
                tableRow.setValue(JOB_STATUS_COLUMN, new TableCell(job.getStatus(), _jobStatusHelper.createJobStatusWidget(job, ignoreStatusMessages())));
                tableRow.setValue(ACTIONS_COLUMN, new TableCell("&nbsp;", getActionColumnWidget(job)));
                tableRows.add(tableRow);
                // If the job's not completed start a timer so we can update the GUI as the status changes
                if (!Task.isDone(job.getStatus()) && _jobStatusHelper.isJobSubmittedSuccessfully(job)) {
                    final JobInfo currentJobInfo = job;
                    final TableRow currentTableRow = tableRow;
                    _jobStatusHelper.startJobResultMonitor(currentJobInfo, new JobStatusListener() {
                        private String _previousStatus = null;

                        public void onJobRunning(JobInfo newJobInfo) {
                            _logger.debug("Job " + newJobInfo.getJobId() + " still running...");
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
                            _logger.error("Communication error retrieving status for job " + currentJobInfo.getJobId());
                        }

                        private void updateDisplay(JobInfo newJobInfo, boolean isDone) {
                            if (newJobInfo == null) {
                                _logger.error("got invalid (null) task");
                                return;
                            }
                            // if the job is done update it no matter what the status is
                            if (!isDone && _previousStatus != null && _previousStatus.equals(newJobInfo.getStatus())) {
                                // nothing has changed yet
                                return;
                            }
                            _previousStatus = newJobInfo.getStatus(); // update the previous status
                            // Update the paging panel's data model with the new status and num hits
                            TableCell statusCell = currentTableRow.getTableCell(JOB_STATUS_COLUMN);
                            Widget statusWidget = _jobStatusHelper.createJobStatusWidget(newJobInfo, ignoreStatusMessages());
                            statusCell.setValue(newJobInfo.getStatus()); // for sorting
                            statusCell.setWidget(statusWidget); // for display

                            // If done with hits, show the action menus
                            TableCell actionsCell = currentTableRow.getTableCell(ACTIONS_COLUMN);
                            if (isDone) {
                                _logger.debug("job is done");
                                actionsCell.setWidget(getActionColumnWidget(newJobInfo)); // for display (no sorting)
                            }
                            // If this job's row is currently visible in the table, tell the table to refresh it
                            if (statusCell.getRow() != TableCell.NOT_SET && statusCell.getCol() != TableCell.NOT_SET) {
                                _pagingPanel.getSortableTable().refreshCell(statusCell.getRow(), statusCell.getCol());
                                _pagingPanel.getSortableTable().refreshCell(actionsCell.getRow(), actionsCell.getCol());
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


    protected Widget getJobMenu(final JobInfo job) {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

        MenuItem paramItem = new MenuItem("Show Parameters", true, new Command() {
            public void execute() {
                _paramPopup = new org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup(
                        job.getJobname(),
                        new FormattedDateTime(job.getSubmitted().getTime()).toString(),
                        job.getParamMap(), false);
                _paramPopup.setPopupTitle("Job Parameters");
                new PopupCenteredLauncher(_paramPopup).showPopup(menu);
            }
        });
        MenuItem exportAllItem = new MenuItem("Export Archive of All Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "archive");
                Window.open(url, "_self", "");
            }
        });

        dropDown.addItem(exportAllItem);
        dropDown.addItem(paramItem);

        MenuItem jobItem = new MenuItem("Job&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        jobItem.setStyleName("tableTopLevelMenuItem");
        menu.addItem(jobItem);

        // Check the status
        menu.setVisible(job.getStatus().equals(Event.COMPLETED_EVENT));
        return menu;
    }

    private String getDownloadURL(JobInfo job, String fileTag) {
        return "/jacs/fileDelivery.htm?nodeTaskId=" + job.getJobId() + "&fileTag=" + fileTag;
    }
}