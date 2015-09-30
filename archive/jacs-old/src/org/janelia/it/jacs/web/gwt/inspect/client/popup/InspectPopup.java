
package org.janelia.it.jacs.web.gwt.inspect.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.inspect.InspectTask;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 17, 2010
 * Time: 1:32:33 PM
 */
public class InspectPopup extends ModalPopupPanel {
    private LoadingLabel _statusMessage = new LoadingLabel();
    private TextBox _pathToArchiveTextBox, _jobName;
    private JobSubmissionListener _listener;
    private ProjectCodePanel _projectCodeWidget;

    public InspectPopup(JobSubmissionListener selectionListener) {
        super("Run Inspect", false);
        this._listener = selectionListener;
    }

    @Override
    protected void populateContent() {
        _projectCodeWidget = new ProjectCodePanel();
        _pathToArchiveTextBox = new TextBox();
        _jobName = new TextBox();
        Grid grid = new Grid(3, 2);
        grid.setCellSpacing(3);
        grid.setWidget(0,0, HtmlUtils.getHtml("Job Name:", "prompt"));
        grid.setWidget(0,1, _jobName);
        grid.setWidget(1,0, HtmlUtils.getHtml("Project code:", "prompt"));
        grid.setWidget(1,1, _projectCodeWidget);
        grid.setWidget(2,0, HtmlUtils.getHtml("Path to the archive:", "prompt"));
        grid.setWidget(2,1, _pathToArchiveTextBox);

        add(grid);
        add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                InspectTask _currentTask = new InspectTask();
                String archivePath = _pathToArchiveTextBox.getText().trim();
                if (null==archivePath || "".equals(archivePath)) {
                    new PopupCenteredLauncher(new ErrorPopupPanel("A full path to the archive must be provided."), 250).showPopup(_pathToArchiveTextBox);
                    return;
                }
                else {
                    _currentTask.setParameter(InspectTask.PARAM_archiveFilePath, archivePath);
                }
                // challenge the user to defline the source attribute or string for each fasta provided.
                _currentTask.setJobName(_jobName.getText());
                _currentTask.setParameter(Task.PARAM_project, _projectCodeWidget.getProjectCode());
                submitJob(_currentTask);
            }
        }));
        actionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        actionPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));
        add(actionPanel);
    }

    // todo Everything from here down can probably be put in a base class
    private void submitJob(Task currentTask) {
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