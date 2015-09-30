
package org.janelia.it.jacs.web.gwt.frdata.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 17, 2010
 * Time: 1:32:57 PM
 */
public class CompleteRecruitmentPopup extends ModalPopupPanel {
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;

    public CompleteRecruitmentPopup(JobSubmissionListener listener) {
        super("Complete Recruitment of Sampling", false);
        this._listener = listener;
    }

    @Override
    protected void populateContent() {
        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                //GenomeProjectSamplingCompletionTask _currentTask
                //submitJob(_currentTask);
            }
        }));
        actionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        actionPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));

    }

    // todo Everything from here down can probably be put in a base class
    private void submitJob(Task currentTask) {
//        if (null==_sybaseInfoPanel.getUsername() || "".equals(_sybaseInfoPanel.getUsername())) {
//            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase username is required.")).showPopup(null);
//            return;
//        }
        _statusMessage.showSubmittingMessage();
        new SubmitJob(currentTask, new MyJobSubmissionListener()).runJob();
        hide();
    }

    private class MyJobSubmissionListener implements JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }

}
