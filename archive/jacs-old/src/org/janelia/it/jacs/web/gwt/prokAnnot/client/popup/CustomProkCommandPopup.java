
package org.janelia.it.jacs.web.gwt.prokAnnot.client.popup;

import java.util.ArrayList;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;
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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 1, 2009
 * Time: 2:49:19 PM
 */
public class CustomProkCommandPopup extends ModalPopupPanel {
    private static final String JCVI_BASE_DIR = SystemProps.getString("ProkAnnotation.BaseDir", null);
    private TextBox _customCommandTextBox;
    private String _localGenomeDirectoryName;
    private SybaseInfoPanel _sybaseInfoPanel = new SybaseInfoPanel();
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;

    public CustomProkCommandPopup(String localGenomeDirectoryName, JobSubmissionListener listener) {
        super("Custom Perl Script Command Line Action", false);
        _listener = listener;
        _localGenomeDirectoryName = localGenomeDirectoryName;
    }

    protected void populateContent() {
        _customCommandTextBox = new TextBox();
        RoundedButton _clearButton = new RoundedButton("Clear");
        RoundedButton _submitButton = new RoundedButton("Submit");
        _clearButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _customCommandTextBox.setText("");
            }
        });
        _submitButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                ProkaryoticAnnotationTask _currentTask = new ProkaryoticAnnotationTask();
                _currentTask.setJobName(_localGenomeDirectoryName);
                ArrayList<String> finalActionList = new ArrayList<String>();
                finalActionList.add(ProkaryoticAnnotationTask.CUSTOM_COMMANDS);
                _currentTask.setActionList(finalActionList);
                _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_targetDirectory, (JCVI_BASE_DIR + "/"+_localGenomeDirectoryName));
                submitJob(_currentTask);
            }
        });
        CenteredWidgetHorizontalPanel panel = new CenteredWidgetHorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));
        panel.setVisible(true);

        VerticalPanel _customCommandPanel = new VerticalPanel();
        _customCommandPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        _customCommandTextBox = new TextBox();
//        _customCommandTextBox.setMaxLength(120);
        _customCommandTextBox.setVisibleLength(80);
        _customCommandTextBox.setVisible(true);

        _customCommandPanel.add(_sybaseInfoPanel);
        _customCommandPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _customCommandPanel.add(HtmlUtils.getHtml("Paste custom perl script command: &nbsp;", "prompt"));
        _customCommandPanel.add(_customCommandTextBox);
        _customCommandPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _customCommandPanel.add(panel);
        _customCommandPanel.setVisible(true);
        add(_customCommandPanel);
    }

    private void submitJob(Task currentTask) {
        if (null == _customCommandTextBox.getText() || "".equals(_customCommandTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A command is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_customCommand, _customCommandTextBox.getText());
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