
package org.janelia.it.jacs.web.gwt.blast.client.wizard;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.blast.client.submit.SubmitBlastJob;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusTimer;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

public class SubmitJobWaitPage extends BlastWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.wizard.SubmitJobWaitPage");

    public static final String HISTORY_TOKEN = "SubmitJobWaitPage";

    VerticalPanel _mainPanel;
    private VerticalPanel jobStatusPanel;
    private HTMLPanel _resultsMessagePanel;

    public SubmitJobWaitPage(BlastData blastData, WizardController controller) {
        super(blastData, controller, false); // no buttons
        init();
    }

    private void init() {
        _mainPanel = new VerticalPanel();
        _mainPanel.setWidth("100%");

        // Main message.
        HorizontalPanel widgets = new HorizontalPanel();
        widgets.add(ImageBundleFactory.getAnimatedImageBundle().getBusyAnimatedIcon().createImage());
        // nasty hack to stop the text from wrapping.  Don't know why whitespace:nowrap doesn't work.
        widgets.add(HtmlUtils.getHtml("Your&nbsp;query&nbsp;sequence&nbsp;is&nbsp;being&nbsp;aligned&nbsp;to&nbsp;the&nbsp;reference&nbsp;database.", "JobWaitMessage"));

        CenteredWidgetHorizontalPanel centerPanel = new CenteredWidgetHorizontalPanel(widgets);
        centerPanel.setStyleName("JobWaitMessageArea");

        // Secondary messages toward bottom
        jobStatusPanel = new VerticalPanel();
        jobStatusPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        jobStatusPanel.add(HtmlUtils.getHtml("Your job number is being retrieved....", "JobWaitMessageSecondary"));
        jobStatusPanel.add(HtmlUtils.getHtml("You can either wait here for the job to complete, or use the menus above.", "JobWaitMessageTertiary"));
        jobStatusPanel.setWidth("100%");
        updateResultsMessage(false);

        _logger.debug("_mainPanel.getWidgetIndex(secondaryMessageArea)=" + _mainPanel.getWidgetIndex(jobStatusPanel));

        _mainPanel.add(centerPanel);
        _mainPanel.add(jobStatusPanel);
        _mainPanel.add(new HTML("&nbsp;")); // spacer
    }

    private void updateResultsMessage(boolean displayLink) {
        if (_resultsMessagePanel != null) {
            jobStatusPanel.remove(_resultsMessagePanel);
        }
        String id = HTMLPanel.createUniqueId();
        _resultsMessagePanel = new HTMLPanel("Your job results will be available on the&nbsp;<span id='" + id + "'></span>&nbsp;page");
        jobStatusPanel.add(_resultsMessagePanel);
        _resultsMessagePanel.setStyleName("JobWaitMessageTertiary");
        HTML resultsHtml = new HTML("Job Results");
        if (!displayLink) {
            resultsHtml.setStyleName("JobWaitMessageTertiary");
        }
        else {
            resultsHtml.addClickListener(new ClickListener() {
                public void onClick(Widget widget) {
                    Window.open("/jacs/gwt/Status/Status.htm", "_self", "");
                }
            });
            resultsHtml.setStyleName("textLink");
        }
        _resultsMessagePanel.add(resultsHtml, id);
        DOM.setStyleAttribute(resultsHtml.getElement(), "display", "inline");
        DOM.setStyleAttribute(_resultsMessagePanel.getElement(), "display", "inline");
    }

    protected void preProcess(Integer priorPageNumber) {
        new SubmitBlastJob(getData(), new JobSubmissionListener(), /*UploadListener*/null).runJob();
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable throwable) {
            postError("An error occurred submitting your job.", "JobWaitMessageError", throwable);
        }

        public void onSuccess(String jobId) {
            getData().setJobNumber(jobId);
            updateResultsMessage(true);
            postSuccess("Your job number is " + jobId + ".", "JobWaitMessageSecondary");
            createJobStatusTimer(jobId);
        }
    }

    /**
     * This method uses the JobStatusTimer to monitor the job.
     *
     * @param jobNumber - grid job number
     */
    private void createJobStatusTimer(final String jobNumber) {
        new JobStatusTimer(jobNumber, new JobStatusListener() {
            public void onJobRunning(org.janelia.it.jacs.shared.tasks.JobInfo ignore) {
                // ignore, timer still running
            }

            // timer cancels itself
            public void onJobFinished(org.janelia.it.jacs.shared.tasks.JobInfo newJobInfo) {
                _logger.debug("Job " + newJobInfo.getJobId() + " completed, status = " + newJobInfo.getStatus());
                Window.open("/jacs/gwt/Status/Status.htm", "_self", "");
            }

            // timer cancels itself
            public void onCommunicationError() {
                postJobStatusError(jobNumber, null);
            }
        });
    }

    /**
     * This method posts a message to the UI and logs it as well
     *
     * @param msg       message
     * @param styleName style to use
     */
    private void postSuccess(String msg, String styleName) {
        _logger.debug(msg);
        updateStatusPage(msg, styleName);
    }

    /**
     * This method posts a message with the given styleName to the UI
     *
     * @param msg       message
     * @param styleName style to use
     */
    protected void updateStatusPage(String msg, String styleName) {
        jobStatusPanel.remove(0);
        HTML jobStatusWidget = HtmlUtils.getHtml(msg, styleName);
        jobStatusPanel.insert(jobStatusWidget, 0);
    }

    private void postJobStatusError(String jobNumber, Throwable e) {
        postError("An error occurred during status check for job number " + jobNumber, "JobWaitMessageError", e);
    }

    /**
     * This method posts an error message to the UI and logs it and the exception as well
     *
     * @param msg       message
     * @param styleName style to use
     * @param throwable problem updating status
     */
    private void postError(String msg, String styleName, Throwable throwable) {
        _logger.error(msg, throwable);
        updateStatusPage(msg, styleName);
    }


    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_WIZARD_SUBMITTED_LABEL;
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }
}
