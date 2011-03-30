/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.RemoveJobEventHandler;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.DateColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * First page of the job results wizard; showsubs a table of all jobs.
 */
public class GeneralJobResultsPanel extends SimplePanel {
    private static Logger _logger = Logger.getLogger("GeneralJobResultsPanel");
    protected RemotePagingPanel _pagingPanel;
    protected SortableTable _table;
    protected JobParameterPopup _paramPopup;
    protected JobStatusHelper _jobStatusHelper;
    protected JobSelectionListener _jobSelectionListener;
    protected JobSelectionListener _jobCompletedListener;
    protected JobSelectionListener _reRunJobListener;
    // NOTE: collaboration mode is very unsafe.  Might want to think twice about this in a Production external setting!
    protected boolean _collaborationMode = false;
    protected boolean _haveData = false;
    protected String _taskClassNameToWatch;
    protected String _rowsPerPagePreferenceKey;
    protected int _defaultRowsPerPage;

    private static final int DELETE_JOB_COLUMN = 0;
    private static final int JOB_NAME_COLUMN = 1;
    private static final int JOB_TIMESTAMP_COLUMN = 2;
    private static final int JOB_STATUS_COLUMN = 3;
    private static final int ACTIONS_COLUMN = 4;

    protected static final String ACTIONS_HEADING = "Actions";

    protected static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public GeneralJobResultsPanel() {
    }

    public GeneralJobResultsPanel(String taskClassNameToWatch, JobSelectionListener jobSelectionListener,
                                  JobSelectionListener reRunJobListener, String[] rowsPerPageOptions, int defaultRowsPerPage, String rowsPerPagePreferenceKey, boolean collaborationMode) {
        _taskClassNameToWatch = taskClassNameToWatch;
        _jobSelectionListener = jobSelectionListener;
        _reRunJobListener = reRunJobListener;
        _rowsPerPagePreferenceKey = rowsPerPagePreferenceKey;
        _defaultRowsPerPage = defaultRowsPerPage;
        init(rowsPerPageOptions);
        _collaborationMode = collaborationMode;
    }

    public GeneralJobResultsPanel(String taskClassNameToWatch, JobSelectionListener jobSelectionListener,
                                  JobSelectionListener reRunJobListener, String[] rowsPerPageOptions, int defaultRowsPerPage, String rowsPerPagePreferenceKey) {
        this(taskClassNameToWatch, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage, rowsPerPagePreferenceKey, false);
    }

    protected void init(String[] rowsPerPageOptions) {
        _jobStatusHelper = new JobStatusHelper();
        add(createTable(rowsPerPageOptions));
    }

    public Widget getMainPanel() {
        return this;
    }

    /**
     * Creates the job remove widget
     *
     * @param job job to remove
     * @param row target row
     * @return widget which activates the deletion action
     */
    protected Widget createRemoveJobWidget(JobInfo job, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveJobEventHandler(job, row, _pagingPanel));
        return image;
    }

    protected Widget createTable(String[] rowsPerPageOptions) {
        // Add a to the panel - will be populated asynchronously by service call
        VerticalPanel tablePanel = new VerticalPanel();
        _table = new SortableTable();
        _table.addColumn(new ImageColumn("&nbsp;")); // job delete icon
        //_table.addColumn(new TextColumn("Job Id"));
        _table.addColumn(new TextColumn(JobStatusHelper.JOB_NAME_HEADING));
        _table.addColumn(new DateColumn(JobStatusHelper.JOB_TIMESTAMP_HEADING));
        _table.addColumn(new TextColumn(JobStatusHelper.JOB_STATUS_HEADING));
        _table.addColumn(new TextColumn(ACTIONS_HEADING, false));

        String[][] sortConstants = new String[][]{
                {"", ""}, // SORT_BY_JOB_ID
                {JobInfo.SORT_BY_JOB_NAME, JobStatusHelper.JOB_NAME_HEADING},
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
     * Method to control whether grid Error messages are ignored by the GUI
     *
     * @return returns a boolean whether to ignore error messages in the job status widget
     */
    protected boolean ignoreStatusMessages() {
        return false;
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class JobDataRetriever implements PagedDataRetriever {
        public JobDataRetriever() {
        }

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


    protected Widget getActionColumnWidget(JobInfo job) {
        Grid grid = new Grid(1, 5);
        grid.setCellSpacing(0);
        grid.setCellPadding(0);

        Widget resultMenu = getResultsMenu();
        grid.setWidget(0, 0, resultMenu);
        if (null != resultMenu) {
            grid.setWidget(0, 1, HtmlUtils.getHtml("/", "linkSeparator"));
        }
        grid.setWidget(0, 2, getJobMenu(job));
        Widget exportMenu = getExportMenu(job);
        if (null != exportMenu) {
            grid.setWidget(0, 3, HtmlUtils.getHtml("/", "linkSeparator"));
        }
        grid.setWidget(0, 4, exportMenu);

        return grid;
    }


    protected Widget getResultsMenu() {
        return null;
        //return HtmlUtils.getHtml("Results", "disabledTextLink");
    }

    protected Widget getExportMenu(final JobInfo job) {
        return getJobExportMenu(job);
    }

    protected Widget getJobExportMenu(JobInfo job) {
        return null;
    }

    protected Widget getDisabledExportMenu() {
        HTML html = new HTML();
        html.setHTML("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownDisabledImage().getHTML());
        html.setStyleName("disabledTopLevelMenuItem");
        return html;
    }

    protected Widget getJobMenu(final JobInfo job) {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

        MenuItem rerunItem = new MenuItem("Re-run This Job With New Parameters", true, new Command() {
            public void execute() {
                _logger.debug("Re-running job=" + job.getJobId() + " with new parameters selected from drop-down");
                if (_reRunJobListener != null)
                    _reRunJobListener.onSelect(job);
            }
        });
        dropDown.addItem(rerunItem);

        MenuItem paramItem = new MenuItem("Show Parameters", true, new Command() {
            public void execute() {
                _logger.debug("Displaying parameters for job=" + job.getJobname() + " with parameter popup");
                // Subclasses may scrub the parameters
                Map<String, String> popupParamMap = job.getParamMap();
                _paramPopup = new JobParameterPopup(
                        job.getJobname(),
                        new FormattedDateTime(job.getSubmitted().getTime()).toString(),
                        popupParamMap, false);
                new PopupCenteredLauncher(_paramPopup).showPopup(menu);
            }
        });
        dropDown.addItem(paramItem);

        MenuItem rerun = new MenuItem("Job&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        rerun.setStyleName("tableTopLevelMenuItem");
        menu.addItem(rerun);

        return menu;
    }

    protected class JobStatusPaginator extends RemotingPaginator {
        public JobStatusPaginator() {
        }

        public JobStatusPaginator(SortableTable sortableTable,
                                  PagedDataRetriever dataRetriever,
                                  String[][] sortConstants) {
            super(sortableTable, dataRetriever, sortConstants, _rowsPerPagePreferenceKey);
        }

        /**
         * This method is overriden only in order to take care of timers cancellation and removal when
         * data from the model is being removed
         */
        public void clear() {
            super.clear();
            _jobStatusHelper.clearJobMonitorTimers();
        }
    }

    public void first() {
        _logger.debug("JobResultPage.preProcess(): " + _haveData);
        if (!_haveData)
            _pagingPanel.first();
    }

    public void refresh() {
        _pagingPanel.clear();
        _pagingPanel.first();
    }

    public void setJobCompletedListener(JobSelectionListener jobCompletedListener) {
        _jobCompletedListener = jobCompletedListener;
    }

    protected void notifyJobCompletedListener(JobInfo job) {
        if (_jobCompletedListener != null)
            _jobCompletedListener.onSelect(job);
    }
}