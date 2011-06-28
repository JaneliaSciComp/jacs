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

package org.janelia.it.jacs.web.gwt.neuronSeparator.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

/**
 * @author Todd Safford
 *         todo All of these generic panels need to have a common base.  All the listeners could be shared
 */
public class NeuronSeparatorPanel extends Composite {
    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private TextBox _inputPathTextBox;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;
    private FileChooserPanel _primerFileChooserPanel;
    final FormPanel _uploadPrimerFastaForm = new FormPanel();
    public String _uploadedPrimerFilePath;
    public String _uploadedPrimerFileSequenceType;

    private FileChooserPanel _ampliconFileChooserPanel;
    final FormPanel _uploadAmpliconFastaForm = new FormPanel();
    public String _uploadedAmpliconFilePath;
    public String _uploadedAmpliconFileSequenceType;

    private boolean projectCodeRequired;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private NeuronSeparatorTaskOptionsPanel _optionsPanel;
    private ProjectCodePanel _projectCodePanel;
    private NeuronSeparatorPipelineTask _neusepPipelineTask = new NeuronSeparatorPipelineTask();
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public NeuronSeparatorPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    private void init(String title) {
        _mainPanel = new TitledBox(title);
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "") +
                SystemProps.getString("Wiki.NeuronSeparatorHelp", "")));
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _optionsPanel = new NeuronSeparatorTaskOptionsPanel();
        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
        _optionsPanel.displayParams(_neusepPipelineTask);


        Grid grid = new Grid((projectCodeRequired) ? 5 : 4, 2);
        grid.setCellSpacing(3);
        int tmpIndex = 0;
        grid.setWidget(tmpIndex, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getJobNameWidget());

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Path To Input File</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getInputPathWidget());

        grid.getCellFormatter().setVerticalAlignment(3, 0, VerticalPanel.ALIGN_TOP);

        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Program:</span>"));
        grid.setWidget(tmpIndex, 1, HtmlUtils.getHtml(NeuronSeparatorPipelineTask.DISPLAY_NAME, "prompt"));

        grid.setWidget(++tmpIndex, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(tmpIndex, 1, _finalOutputPanel);

        if (projectCodeRequired) {
            _projectCodePanel = new ProjectCodePanel();
            grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
            grid.setWidget(tmpIndex, 1, _projectCodePanel);
        }

        createButtons();
        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(_optionsPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getPrimerStatusMessage());

        _mainPanel.add(contentPanel);
        _submitButton.setEnabled(false);
    }

    private Widget getPrimerStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    private void createButtons() {
        _clearButton = new RoundedButton("Clear", new ClickListener() {
            public void onClick(Widget sender) {
                clear();
            }
        });

        _submitButton = new RoundedButton("Submit Job", new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        _submitButton.setEnabled(false);
    }

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private Widget getInputPathWidget() {
        _inputPathTextBox = new TextBox();
        _inputPathTextBox.setMaxLength(64);
        _inputPathTextBox.setVisibleLength(64);
        return _jobNameTextBox;
    }

    private void updateJobNameWidget() {
        _jobNameTextBox.setText("My Neuron Separator Pipeline job " + new FormattedDate().toString());
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void clear() {
        // Clear the Query Sequence PulldownPopup and any selections in the popup
        //_uploadPrimerFastaForm.clear();

        _neusepPipelineTask = new NeuronSeparatorPipelineTask();
        _optionsPanel.displayParams(_neusepPipelineTask);
        _primerFileChooserPanel.clear();
        _ampliconFileChooserPanel.clear();
        _submitButton.setEnabled(false);
        // Update all the GUI widgets with the new values
        updateAll();
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
            return;
        }
        if (projectCodeRequired) {
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
        }
        // submit the job
        _neusepPipelineTask.setJobName(_jobNameTextBox.getText());
        if (projectCodeRequired) {
            _neusepPipelineTask.setParameter(Task.PARAM_project, _projectCodePanel.getProjectCode());
        }
        _neusepPipelineTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList, _uploadedPrimerFilePath);
        if (_finalOutputPanel.overrideFinalOutputPath()) {
            _neusepPipelineTask.setParameter(Task.PARAM_finalOutputDirectory, _finalOutputPanel.getFinalOutputDestination());
        }
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();
        new SubmitJob(_neusepPipelineTask, new JobSubmissionListener()).runJob();
    }

    private void updateAll() {
        updateJobNameWidget();
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _submitButton.setEnabled(true);
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }

}