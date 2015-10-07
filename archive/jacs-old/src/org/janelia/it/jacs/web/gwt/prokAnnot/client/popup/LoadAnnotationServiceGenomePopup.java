
package org.janelia.it.jacs.web.gwt.prokAnnot.client.popup;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationServiceLoadGenomeDataTask;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.panel.SybaseInfoPanel;

import com.google.gwt.user.client.ui.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 1, 2009
 * Time: 2:49:19 PM
 */
public class LoadAnnotationServiceGenomePopup extends ModalPopupPanel {
    private static final String JCVI_BASE_DIR = SystemProps.getString("ProkAnnotation.BaseDir", null);
    private TextBox _jcviDirectoryTextBox;
    private TextBox _dateStringTextBox;
    private String _localGenomeDirectoryName;
    private SybaseInfoPanel _sybaseInfoPanel;
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;

    public LoadAnnotationServiceGenomePopup(String localGenomeDirectoryName, JobSubmissionListener listener) {
        super("Load Annotation Service Data", false);
        _listener = listener;
        _localGenomeDirectoryName = localGenomeDirectoryName;
    }

    protected void populateContent() {
        _jcviDirectoryTextBox = new TextBox();
        _jcviDirectoryTextBox.setMaxLength(40);
        _jcviDirectoryTextBox.setVisibleLength(40);
        _jcviDirectoryTextBox.setVisible(true);
        _dateStringTextBox = new TextBox();
        _dateStringTextBox.setMaxLength(40);
        _dateStringTextBox.setVisibleLength(40);
        _dateStringTextBox.setVisible(true);
        RoundedButton _clearButton = new RoundedButton("Clear");
        RoundedButton _getFilesButton = new RoundedButton("Get Files");
        _clearButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _jcviDirectoryTextBox.setText("");
                _dateStringTextBox.setText("");
            }
        });
        _getFilesButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                ProkaryoticAnnotationServiceLoadGenomeDataTask _currentTask = new ProkaryoticAnnotationServiceLoadGenomeDataTask();
                submitJob(_currentTask);
            }
        });
        CenteredWidgetHorizontalPanel panel = new CenteredWidgetHorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_getFilesButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));
        panel.setVisible(true);

        HorizontalPanel tmpDirPanel = new HorizontalPanel();
        tmpDirPanel.add(HtmlUtils.getHtml("Organism Directory:", "prompt"));
        tmpDirPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        tmpDirPanel.add(HtmlUtils.getHtml(_localGenomeDirectoryName, "prompt"));

        VerticalPanel _loadJCVIPanel = new VerticalPanel();
        _loadJCVIPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        _sybaseInfoPanel = new SybaseInfoPanel();
        _loadJCVIPanel.add(tmpDirPanel);
        _loadJCVIPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _loadJCVIPanel.add(_sybaseInfoPanel);
        _loadJCVIPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _loadJCVIPanel.add(HtmlUtils.getHtml("This information helps us to find the files locally. &nbsp;", "prompt"));
        _loadJCVIPanel.add(HtmlUtils.getHtml("Paste JCVI directory name (Usually, name of user who requested data): &nbsp;", "prompt"));
        _loadJCVIPanel.add(_jcviDirectoryTextBox);
        _loadJCVIPanel.add(HtmlUtils.getHtml("Paste data submission date string (Example, Jul_27_2009): &nbsp;", "prompt"));
        _loadJCVIPanel.add(_dateStringTextBox);
        _loadJCVIPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _loadJCVIPanel.add(panel);
        _loadJCVIPanel.setVisible(true);
        add(_loadJCVIPanel);
    }

    private void submitJob(Task currentTask) {
        if (null == _jcviDirectoryTextBox.getText() || "".equals(_jcviDirectoryTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A JCVI directory is required.")).showPopup(null);
            return;
        }

        if (null == _dateStringTextBox.getText() || "".equals(_dateStringTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A date string is required.")).showPopup(null);
            return;
        }

        if (null == _sybaseInfoPanel.getUsername() || "".equals(_sybaseInfoPanel.getUsername())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase username is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_username, _sybaseInfoPanel.getUsername());
            Preferences.setSubjectPreference(new SubjectPreference("sbLogin", "ProkPipeline", _sybaseInfoPanel.getUsername()));
        }

        if (null == _sybaseInfoPanel.getSybasePassword() || "".equals(_sybaseInfoPanel.getSybasePassword())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase password is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_sybasePassword, _sybaseInfoPanel.getSybasePassword());
            Preferences.setSubjectPreference(new SubjectPreference("sbPass", "ProkPipeline", _sybaseInfoPanel.getSybasePassword()));
        }

        currentTask.setJobName(_localGenomeDirectoryName);
        currentTask.setParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_targetDirectory, (JCVI_BASE_DIR + "/" + _localGenomeDirectoryName));
        currentTask.setParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_sourceDirectory, (_jcviDirectoryTextBox.getText()));
        currentTask.setParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_dateString, (_dateStringTextBox.getText()));

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