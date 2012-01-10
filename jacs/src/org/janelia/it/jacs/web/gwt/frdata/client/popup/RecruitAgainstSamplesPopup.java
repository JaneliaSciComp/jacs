
package org.janelia.it.jacs.web.gwt.frdata.client.popup;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectRecruitmentSamplingTask;
import org.janelia.it.jacs.web.gwt.advancedblast.client.popups.AdvancedBlastReferenceDatasetChooserPopup;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.FileChooserPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 17, 2010
 * Time: 1:29:12 PM
 */
public class RecruitAgainstSamplesPopup extends ModalPopupPanel {
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;
    private RoundedButton _addDatabaseButton;
    private RoundedButton _removeDatabaseButton;
    private ListBox dbListBox;
    private TextBox jobNameTextBox;
    private AdvancedBlastReferenceDatasetChooserPopup blastDBPopup;
    private String _sampleInfoFilePath;
    private ProjectCodePanel _projectCodePanel;

    public RecruitAgainstSamplesPopup(JobSubmissionListener listener) {
        super("Recruit Against Genomes via Sampling", false);
        this._listener = listener;
    }

    @Override
    protected void populateContent() {
        HorizontalPanel jobNamePanel = new HorizontalPanel();
        jobNameTextBox = new TextBox();
        _projectCodePanel = new ProjectCodePanel();
        jobNamePanel.add(HtmlUtils.getHtml("Job Name:", "prompt"));
        jobNamePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        jobNamePanel.add(jobNameTextBox);
        VerticalPanel mainPanel = new VerticalPanel();
        blastDBPopup = new AdvancedBlastReferenceDatasetChooserPopup(new SelectionListener() {
            @Override
            public void onSelect(String value) {
                if (null!=value) {
                    BlastableNodeVO tmpNodeVO = blastDBPopup.getBlastNodeForId(value);
                    dbListBox.addItem(tmpNodeVO.getNodeName(),value);
                }
            }

            @Override
            public void onUnSelect(String value) {
                // Do nothing
            }
        }, new BlastData());
        _addDatabaseButton = new RoundedButton("Add Blast Database");
        _addDatabaseButton.addClickListener(new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(blastDBPopup, 250).showPopup(_addDatabaseButton);
            }
        });
        _removeDatabaseButton = new RoundedButton("Remove Selected Database");
        _removeDatabaseButton.addClickListener(new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                if (dbListBox.getSelectedIndex() >= 0) {
                    dbListBox.removeItem(dbListBox.getSelectedIndex());
                }
            }
        });
        dbListBox = new ListBox();
        dbListBox.setWidth("200px");
        dbListBox.setVisibleItemCount(5);
        VerticalPanel buttonPanel = new VerticalPanel();
        buttonPanel.add(_addDatabaseButton);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        buttonPanel.add(_removeDatabaseButton);
        ArrayList<FileChooserPanel.FILE_TYPE> sampleInfoList = new ArrayList<FileChooserPanel.FILE_TYPE>();
        sampleInfoList.add(FileChooserPanel.FILE_TYPE.info);

        Grid grid = new Grid(1, 2);
        grid.setCellSpacing(3);
        grid.setWidget(0,0, dbListBox);
        grid.setWidget(0,1, buttonPanel);

        mainPanel.add(jobNamePanel);
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(HtmlUtils.getHtml("Databases Selected", "prompt"));
        mainPanel.add(grid);
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(HtmlUtils.getHtml("Sample Info", "prompt"));
        mainPanel.add(new FileChooserPanel(new SelectionListener() {
            @Override
            public void onSelect(String value) {
                _sampleInfoFilePath = value;
            }

            @Override
            public void onUnSelect(String value) {
                _sampleInfoFilePath = null;
            }}, sampleInfoList));
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(_projectCodePanel);
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                GenomeProjectRecruitmentSamplingTask _currentTask = new GenomeProjectRecruitmentSamplingTask();
                StringBuffer sbuf = new StringBuffer();
                for (int i = 0; i < dbListBox.getItemCount(); i++) {
                    if (null != dbListBox.getValue(i) && !"".equals(dbListBox.getValue(i))) {
                        sbuf.append(dbListBox.getValue(i)).append(",");
                    }
                }
                String finalList = sbuf.toString();
                if (finalList.endsWith(",")) {
                    finalList = finalList.substring(0, finalList.length() - 1);
                }
                _currentTask.setParameter(GenomeProjectRecruitmentSamplingTask.BLASTABLE_DATABASE_NODES, finalList);
                if (null!=jobNameTextBox.getText() && !"".equals(jobNameTextBox.getText())) {
                    _currentTask.setJobName(jobNameTextBox.getText());
                }
                _currentTask.setParameter(Task.PARAM_project, _projectCodePanel.getProjectCode());
                // Submit the job
                submitJob(_currentTask);
            }
        }));
        actionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        actionPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));

        add(mainPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(actionPanel);
    }

    // todo Everything from here down can probably be put in a base class
    private void submitJob(Task currentTask) {
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
