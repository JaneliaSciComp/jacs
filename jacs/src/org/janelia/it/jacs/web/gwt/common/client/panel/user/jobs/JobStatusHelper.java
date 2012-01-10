
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusTimer;
import org.janelia.it.jacs.web.gwt.common.client.popup.PopperUpperPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobErrorsPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.HashMap;

/**
 * First page of the job results wizard; shows a table of all jobs.
 */
public class JobStatusHelper {
    private static Logger _logger = Logger.getLogger("JobResultsFormatHelper");

    public static final String JOB_NAME_HEADING = "Job Name";
    public static final String JOB_SUBMITTER_HEADING = "Submitter";
    public static final String JOB_TIMESTAMP_HEADING = "Submit Date";
    public static final String JOB_STATUS_HEADING = "Status";

    public static final int DB_MAX_SIZE = 50;

    private HashMap<String, JobStatusTimer> _statusTimers;

    public JobStatusHelper() {
        _statusTimers = new HashMap<String, JobStatusTimer>();
    }

    public void cancelAndRemoveJobResultMonitor(String jobId) {
        JobStatusTimer currentJobTimer = _statusTimers.remove(jobId);
        if (currentJobTimer != null) {
            _logger.debug("Cancel status timer for job " + jobId);
            currentJobTimer.cancel();
        }
    }

    public void clearJobMonitorTimers() {
        cancelJobStatusTimers();
        _statusTimers.clear();
    }

    /**
     * Determines the correct Widget to put in the Status cell of the table:<ul>
     * <p/>
     * <li>Link to results page, if job is completed</li>
     * <li>PopperUpper with detailed message if available</li>
     * <li>just the status otherwise</li>
     * </ul>
     *
     * @param job job to watch
     * @return the JobStatusWidget
     */
    public Widget createJobStatusWidget(JobInfo job, boolean ignoreMessages) {
        if (job.getStatus().equals(Event.COMPLETED_EVENT)) {
            // job is completed
            if (job.getNumTaskMessages() == null ||
                    job.getNumTaskMessages() == 0 || ignoreMessages) {
                // job completed with no errors
                return HtmlUtils.getHtml(job.getStatus(), "jobCompletedOKText");
            }
            else {
                return createCompletedWithErrorsWidget(job);
            }
        }
        else if (!Task.isDone(job.getStatus())) {
            // Running state
            return createRunningOrIncompleteWidget(job);
        }
        else { // Error state
            if (job.getStatusDescription() != null && job.getStatusDescription().trim().length() > 0) {
                // error state
                return new PopperUpperHTML(job.getStatus(), "jobErrorPopperUpperText", "jobErrorPopperUpperHover",
                        HtmlUtils.getHtml(job.getStatusDescription().replaceAll("\n", "<br/>"), "text"));
            }
            else {
                return HtmlUtils.getHtml(job.getStatus(), "jobError");
            }
        }
    }

    public boolean isJobSubmittedSuccessfully(JobInfo job) {
        return !job.getStatus().equals(Event.ERROR_EVENT) && !job.getStatus().equals(Event.DELETED_EVENT);
    }

    public void removeJobResultMonitor(String jobId) {
        _statusTimers.remove(jobId);
    }

    public void startJobResultMonitor(final JobInfo job, final JobStatusListener jobStatusListener) {
        _logger.debug("Starting status timer for job " + job.getJobId());
        JobStatusTimer currentJobTimer = _statusTimers.remove(job.getJobId());
        if (currentJobTimer != null) {
            // instead of reusing the timer we cancel this one and restart another one
            // because the tableRow stored in the jobstatus listener may not be valid any more
            _logger.debug("Cancel status timer for job " + job.getJobId());
            currentJobTimer.cancel();
        }
        // Crate a JobStatusTimer to monitor this job.  The timer will notify us of the state every 3 secs, and
        // we'll update the table (unless the user's sorting, in which case we'll wait till the sort is done)
        _statusTimers.put(job.getJobId(), new JobStatusTimer(job.getJobId(), jobStatusListener));
    }

    private void cancelJobStatusTimers() {
        for (JobStatusTimer timer : _statusTimers.values()) {
            timer.cancel();
        }
    }

    private Widget createCompletedWithErrorsWidget(JobInfo jobInfo) {
        VerticalPanel vertPanel = new VerticalPanel();
        vertPanel.add(HtmlUtils.getHtml(jobInfo.getStatus(), "jobCompletedWithErrorsText"));
        HorizontalPanel horPanel = new HorizontalPanel();
        horPanel.add(HtmlUtils.getHtml("with&nbsp;", "text"));
        String title = " Errors encountered during execution of Job: " + jobInfo.getJobname();
        horPanel.add(new PopperUpperPopup("errors", "jobWithErrorPopperUpperText", "jobErrorPopperUpperText", "jobErrorPopperUpperHover", "jobWithErrorPopperUpperClick", false, true,
                new JobErrorsPopupPanel(jobInfo, title)));
        vertPanel.add(horPanel);
        return vertPanel;
    }

    /**
     * Creates a Widget that shows the job status for non-complete jobs.  If the job is older than a day, just
     * mark it "incomplete".  If it's more recent, show the status and start a timer to update the status.
     *
     * @param job the job to watch or ignore
     * @return the widget to execute
     */
    private Widget createRunningOrIncompleteWidget(JobInfo job) {
        Widget widget;

        if (!isJobSubmittedSuccessfully(job)) {
            widget = new PopperUpperHTML("submit failed", "jobErrorPopperUpperText", "jobErrorPopperUpperHover",
                    HtmlUtils.getHtml("Submission of this job failed.", "text"));
            cancelAndRemoveJobResultMonitor(job.getJobId());
        }
//        else if (job.isWaitingOnTheGrid()) {
//            widget = new WaitingJobWidget(job);
//        }
        else {
            widget = new RunningJobWidget(job);
        }

        return widget;
    }

    private class RunningJobWidget extends Composite {
        protected JobInfo _job;
        protected HorizontalPanel _panel;

        public RunningJobWidget(JobInfo job) {
            _job = job;
            init();
            initWidget(_panel);
        }

        private void init() {
            _panel = new HorizontalPanel();
//            if (_job.getPercentComplete() != null) {
//                _panel.add(HtmlUtils.getHtml(_job.getStatus() + " (" + _job.getPercentComplete() + "%) &nbsp;&nbsp;", "jobRunning", "nowrap"));
//            }
//            else {
//                _panel.add(HtmlUtils.getHtml(_job.getStatus() + "%) &nbsp;&nbsp;", "jobRunning", "nowrap"));
//            }

            String statusString = "";
            if (_job.getStatus().equals(Event.RUNNING_EVENT) && _job.getStatusDescription() != null && _job.getStatusDescription().length()>0) {
                statusString = _job.getStatusDescription();
            } else {
                statusString = _job.getStatus();
            }

            _panel.add(HtmlUtils.getHtml(statusString + "&nbsp;&nbsp;", "jobRunning", "nowrap"));
            _panel.add(ImageBundleFactory.getAnimatedImageBundle().getBusyAnimatedIcon().createImage());
            DOM.setStyleAttribute(_panel.getElement(), "display", "inline");
        }

        protected void onAttach() {
            super.onAttach();
            JobStatusTimer jobStatusTimer = _statusTimers.get(_job.getJobId());
            if (jobStatusTimer != null)
                jobStatusTimer.restart();
        }

        protected void onDetach() {
            JobStatusTimer jobStatusTimer = _statusTimers.get(_job.getJobId());
            if (jobStatusTimer != null)
                jobStatusTimer.cancel();
            super.onDetach();
        }

    }

//    private class WaitingJobWidget extends Composite
//    {
//
//        protected JobInfo _job;
//        protected HorizontalPanel _panel;
//
//        public WaitingJobWidget(JobInfo job) {
//           _job = job;
//            init();
//            initWidget(_panel);
//        }
//
//
//        private void init()
//        {
//            _panel = new HorizontalPanel();
//            Integer order = _job.getJobOrder();
//            if (order == null || order < 1) {
//                _panel.add(HtmlUtils.getHtml("waiting &nbsp;&nbsp;", "jobRunning", "nowrap"));
//            }
//            else if (order == 1) {
//                _panel.add(HtmlUtils.getHtml("running &nbsp;&nbsp;", "jobRunning", "nowrap"));
//            }
//            else  {
//                _panel.add(HtmlUtils.getHtml("# " + order + " in the queue &nbsp;&nbsp;", "jobRunning", "nowrap"));
//            }
//            _panel.add(ImageBundleFactory.getAnimatedImageBundle().getBusyAnimatedIcon().createImage());
//            DOM.setStyleAttribute(_panel.getElement(), "display", "inline");
//        }
//
//         protected void onAttach() {
//            super.onAttach();
//            JobStatusTimer jobStatusTimer = _statusTimers.get(_job.getJobId());
//            if(jobStatusTimer != null)
//                jobStatusTimer.restart();
//        }
//
//        protected void onDetach() {
//            JobStatusTimer jobStatusTimer = _statusTimers.get(_job.getJobId());
//            if(jobStatusTimer != null)
//                jobStatusTimer.cancel();
//            super.onDetach();
//        }
//
//    }

}
