
package org.janelia.it.jacs.web.gwt.frdata.client.popup;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.CreateRecruitmentBlastDatabaseTask;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 17, 2010
 * Time: 1:32:33 PM
 */
public class CreateBlastDatabasePopup extends ModalPopupPanel {
    private LoadingLabel _statusMessage = new LoadingLabel();
    private TextBox sampleNameTextBox;
    private JobSubmissionListener _listener;
    private UploadUserSequencePanel _sequencePanel;

    public CreateBlastDatabasePopup(JobSubmissionListener selectionListener) {
        super("Create Sample Blast Database", false);
        _sequencePanel = new UploadUserSequencePanel(new SelectionListener() {
            // todo Fix this!
            @Override
            public void onSelect(String value) {
                if (_sequencePanel.validateAndPersistSequenceSelection()) {
                    BlastData blastData = _sequencePanel.getBlastData();
                    Window.alert("User selected this fasta for their db: " + blastData.getUserReferenceFASTA());
                }
            }

            @Override
            public void onUnSelect(String value) {
                // Do nothing
            }
        }, SequenceType.NUCLEOTIDE, 10);
        this._listener = selectionListener;
    }

    @Override
    protected void populateContent() {
        HorizontalPanel sampleNamePanel = new HorizontalPanel();
        sampleNameTextBox = new TextBox();
        sampleNamePanel.add(HtmlUtils.getHtml("Sample Defline Identifier (value of /sample_name):", "prompt"));
        sampleNamePanel.add(sampleNameTextBox);
        add(sampleNamePanel);
        add(_sequencePanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                CreateRecruitmentBlastDatabaseTask _currentTask = new CreateRecruitmentBlastDatabaseTask();
                String filePath;
                String sequenceName = _sequencePanel.getSequenceName().trim();
                String sampleName = sampleNameTextBox.getText().trim();
                if (null==sampleName || "".equals(sampleName)) {
                    new PopupCenteredLauncher(new ErrorPopupPanel("A sample identifier must be provided."), 250).showPopup(sampleNameTextBox);
                    return;
                }
                if (_sequencePanel.isNetworkFileSelected()) {
                    filePath = _sequencePanel.getNetworkPath();
                    // todo What about the other sequence entry types? pasted and uploaded?  Fix this!
                    _currentTask.setParameter(CreateRecruitmentBlastDatabaseTask.PARAM_FASTA_FILE_PATH, filePath);
                }
                // todo Add a parse of each initial defline and see if /sample_name or /source is found.  If not
                // challenge the user to defline the source attribute or string for each fasta provided.
                _currentTask.setJobName(sequenceName);
                _currentTask.setParameter(CreateRecruitmentBlastDatabaseTask.PARAM_BLAST_DB_NAME, sequenceName);
                _currentTask.setParameter(CreateRecruitmentBlastDatabaseTask.PARAM_BLAST_DB_DESCRIPTION, sequenceName);
                _currentTask.setParameter(CreateRecruitmentBlastDatabaseTask.PARAM_SAMPLE_NAME, sampleName);
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
            //_listener.onSelect(jobId);
            // todo Take care of this JobInfo
        }
    }

}
