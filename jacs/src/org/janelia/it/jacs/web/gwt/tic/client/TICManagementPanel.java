package org.janelia.it.jacs.web.gwt.tic.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.tic.BatchTicTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 1/9/12
 * Time: 2:24 PM
 */
public class TICManagementPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.tic.client.TICManagementPanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private BatchTicTask _ticTask;
    private TextBox _jobNameTextBox;

    private CheckBox _applyCalibrationCheckBox;
    private CheckBox _illuminationCorrectionCheckBox;
    private CheckBox _fqbatchCheckBox;

    private TextBox _inputFileOrDirTextBox;
    private RoundedButton _validateInputFileOrDirButton;

    private TextBox _transformationFileTextBox;
    private RoundedButton _validateTransformationFileButton;

    private TextBox _avgReadNoiseTextBox;

    private TextBox _avgDarkTextBox;

    private TextBox _borderCropPixelsTextBox;

    private TextBox _intensityCorrectionMatrixFileTextBox;
    private RoundedButton _validateIntensityCorrectionFileButton;

    private TextBox _microscopeSettingsTextBox;

    private TextBox _positionXMinTextBox;
    private TextBox _positionXMaxTextBox;
    private TextBox _positionYMinTextBox;
    private TextBox _positionYMaxTextBox;
    private TextBox _positionZMinTextBox;
    private TextBox _positionZMaxTextBox;
    private TextBox _sigmaXMinTextBox;
    private TextBox _sigmaXMaxTextBox;
    private TextBox _sigmaYMinTextBox;
    private TextBox _sigmaYMaxTextBox;
    private TextBox _sigmaZMinTextBox;
    private TextBox _sigmaZMaxTextBox;

    private RoundedButton _validateMicroscopeSettingsFileButton;
    private CheckBox _spotsOnlyCheckBox;

    private String _validationFilePath;
    private boolean haveData = false;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private Widget statusMessage;

    public TICManagementPanel(org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        super();
        _listener = listener;
        HorizontalPanel stepPanel = new HorizontalPanel();
        _applyCalibrationCheckBox = new CheckBox("Apply Calibration Step");
        _applyCalibrationCheckBox.setValue(Boolean.TRUE);
        _illuminationCorrectionCheckBox = new CheckBox("Illumination Correction Step");
        _illuminationCorrectionCheckBox.setValue(Boolean.TRUE);
        _fqbatchCheckBox = new CheckBox("FISH QUANT V4 Step");
        _fqbatchCheckBox.setValue(Boolean.TRUE);
        stepPanel.add(_applyCalibrationCheckBox);
        stepPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        stepPanel.add(_illuminationCorrectionCheckBox);
        stepPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        stepPanel.add(_fqbatchCheckBox);
        _spotsOnlyCheckBox = new CheckBox("Only persist spots data");
        _spotsOnlyCheckBox.setValue(Boolean.TRUE);

        HorizontalPanel _spotsPanel = new HorizontalPanel();
        _spotsPanel.add(_spotsOnlyCheckBox);

        HorizontalPanel jobPanel = new HorizontalPanel();
        _jobNameTextBox = new TextBox();
        jobPanel.add(HtmlUtils.getHtml("Job Name:", "prompt"));
        jobPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        jobPanel.add(_jobNameTextBox);

        VerticalPanel settingsPanel = new VerticalPanel();
        createButtons();
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(jobPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(stepPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(_spotsPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        setUpInputFileOrDirectoryPanel();
        setUpBorderCropPanel();
        setUpTransformationFilePanel();
        setUpIntensityCorrectionPanel();
        setUpAvgReadNoisePanel();
        setUpAvgDarkPanel();
        setUpMicroscopeSettingsPanel();

        Grid centerGrid = new Grid(7,2);
        centerGrid.setWidget(0,0,HtmlUtils.getHtml("Input File or Directory Path: ", "prompt"));
        centerGrid.setWidget(0,1,_inputFileOrDirTextBox);
        centerGrid.setWidget(1,0,HtmlUtils.getHtml("Border Pixels to Crop: ", "prompt"));
        centerGrid.setWidget(1,1,_borderCropPixelsTextBox);
        centerGrid.setWidget(2,0,HtmlUtils.getHtml("Transformation File Path: ", "prompt"));
        centerGrid.setWidget(2,1,_transformationFileTextBox);
        centerGrid.setWidget(3,0,HtmlUtils.getHtml("Intensity Correction File Path: ", "prompt"));
        centerGrid.setWidget(3,1,_intensityCorrectionMatrixFileTextBox);
        centerGrid.setWidget(4,0,HtmlUtils.getHtml("Average Read Noise Settings: ", "prompt"));
        centerGrid.setWidget(4,1,_avgReadNoiseTextBox);
        centerGrid.setWidget(5,0,HtmlUtils.getHtml("Averge Dark Settings: ", "prompt"));
        centerGrid.setWidget(5,1,_avgDarkTextBox);
        centerGrid.setWidget(6,0,HtmlUtils.getHtml("Microscope Settings File Path: ", "prompt"));
        centerGrid.setWidget(6,1,_microscopeSettingsTextBox);

        settingsPanel.add(centerGrid);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(setUpThresholdPanel());
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(getSubmitButtonPanel());
        settingsPanel.add(getStatusMessage());

        TitledPanel userSelectionPanel = new TitledPanel("Set Parameters ");
        userSelectionPanel.add(settingsPanel);

        add(userSelectionPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
    }


    protected void popuplateContentPanel() {
    }

    private void setUpInputFileOrDirectoryPanel() {
        _inputFileOrDirTextBox = new TextBox();
        _inputFileOrDirTextBox.setVisibleLength(90);
        _inputFileOrDirTextBox.setText("");
        _validateInputFileOrDirButton = getValidationButton(_inputFileOrDirTextBox.getText());
//        sourcePanel.add(_validateInputFileOrDirButton);
    }

    private void setUpBorderCropPanel() {
        _borderCropPixelsTextBox = new TextBox();
        _borderCropPixelsTextBox.setVisibleLength(10);
        _borderCropPixelsTextBox.setText("");
    }

    private void setUpTransformationFilePanel() {
        _transformationFileTextBox = new TextBox();
        _transformationFileTextBox.setVisibleLength(90);
        _transformationFileTextBox.setText("");
        _validateTransformationFileButton = getValidationButton(_transformationFileTextBox.getText());
    }

    private void setUpIntensityCorrectionPanel() {
        _intensityCorrectionMatrixFileTextBox = new TextBox();
        _intensityCorrectionMatrixFileTextBox.setVisibleLength(90);
        _intensityCorrectionMatrixFileTextBox.setText("");
        _validateIntensityCorrectionFileButton = getValidationButton(_intensityCorrectionMatrixFileTextBox.getText());
    }

    private void setUpMicroscopeSettingsPanel() {
        _microscopeSettingsTextBox = new TextBox();
        _microscopeSettingsTextBox.setVisibleLength(90);
        _microscopeSettingsTextBox.setText("");
        _validateMicroscopeSettingsFileButton = getValidationButton(_microscopeSettingsTextBox.getText());
    }

    private void setUpAvgReadNoisePanel() {
        _avgReadNoiseTextBox = new TextBox();
        _avgReadNoiseTextBox.setVisibleLength(90);
        _avgReadNoiseTextBox.setText("107.089,107.39,107.395,107.41,107.711,107.739,107.609,107.958,107.932");
    }

    private void setUpAvgDarkPanel() {
        _avgDarkTextBox = new TextBox();
        _avgDarkTextBox.setVisibleLength(90);
        _avgDarkTextBox.setText("0,0,0,0,0,0,0,0,0");
    }

    private HorizontalPanel setUpThresholdPanel() {
        _positionXMinTextBox = new TextBox();
        _positionXMinTextBox.setVisibleLength(10);
        _positionXMinTextBox.setText("-120");
        _positionXMaxTextBox = new TextBox();
        _positionXMaxTextBox.setVisibleLength(10);
        _positionXMaxTextBox.setText("22000");

        _positionYMinTextBox = new TextBox();
        _positionYMinTextBox.setVisibleLength(10);
        _positionYMinTextBox.setText("-120");
        _positionYMaxTextBox = new TextBox();
        _positionYMaxTextBox.setVisibleLength(10);
        _positionYMaxTextBox.setText("22000");

        _positionZMinTextBox = new TextBox();
        _positionZMinTextBox.setVisibleLength(10);
        _positionZMinTextBox.setText("-500");
        _positionZMaxTextBox = new TextBox();
        _positionZMaxTextBox.setVisibleLength(10);
        _positionZMaxTextBox.setText("5000");

        _sigmaXMinTextBox = new TextBox();
        _sigmaXMinTextBox.setVisibleLength(10);
        _sigmaXMinTextBox.setText("30");
        _sigmaXMaxTextBox = new TextBox();
        _sigmaXMaxTextBox.setVisibleLength(10);
        _sigmaXMaxTextBox.setText("600");

        _sigmaYMinTextBox = new TextBox();
        _sigmaYMinTextBox.setVisibleLength(10);
        _sigmaYMinTextBox.setText("30");
        _sigmaYMaxTextBox = new TextBox();
        _sigmaYMaxTextBox.setVisibleLength(10);
        _sigmaYMaxTextBox.setText("600");

        _sigmaZMinTextBox = new TextBox();
        _sigmaZMinTextBox.setVisibleLength(10);
        _sigmaZMinTextBox.setText("50");
        _sigmaZMaxTextBox = new TextBox();
        _sigmaZMaxTextBox.setVisibleLength(10);
        _sigmaZMaxTextBox.setText("800");

        _validateIntensityCorrectionFileButton = getValidationButton(_intensityCorrectionMatrixFileTextBox.getText());

        HorizontalPanel thresholdPanel = new HorizontalPanel();
        Grid thrPosGrid = new Grid(5,3);
        thrPosGrid.setWidget(0,0,HtmlUtils.getHtml("Thresholds","prompt"));
        thrPosGrid.setWidget(1,0,HtmlUtils.getHtml("Position","prompt"));
        thrPosGrid.setWidget(1,1,HtmlUtils.getHtml("Min","prompt"));
        thrPosGrid.setWidget(1,2,HtmlUtils.getHtml("Max","prompt"));
        thrPosGrid.setWidget(2,0,HtmlUtils.getHtml("X","prompt"));
        thrPosGrid.setWidget(2,1,_positionXMinTextBox);
        thrPosGrid.setWidget(2,2,_positionXMaxTextBox);
        thrPosGrid.setWidget(3,0,HtmlUtils.getHtml("Y","prompt"));
        thrPosGrid.setWidget(3,1,_positionYMinTextBox);
        thrPosGrid.setWidget(3,2,_positionYMaxTextBox);
        thrPosGrid.setWidget(4,0,HtmlUtils.getHtml("Z","prompt"));
        thrPosGrid.setWidget(4,1,_positionZMinTextBox);
        thrPosGrid.setWidget(4,2,_positionZMaxTextBox);

        Grid thrSigGrid = new Grid(5,3);
        thrSigGrid.setWidget(0,0,HtmlUtils.getHtml("&nbsp","prompt"));
        thrSigGrid.setWidget(1,0,HtmlUtils.getHtml("Sigma","prompt"));
        thrSigGrid.setWidget(1,1,HtmlUtils.getHtml("Min","prompt"));
        thrSigGrid.setWidget(1,2,HtmlUtils.getHtml("Max","prompt"));
        thrSigGrid.setWidget(2,0,HtmlUtils.getHtml("X","prompt"));
        thrSigGrid.setWidget(2,1,_sigmaXMinTextBox);
        thrSigGrid.setWidget(2,2,_sigmaXMaxTextBox);
        thrSigGrid.setWidget(3,0,HtmlUtils.getHtml("Y","prompt"));
        thrSigGrid.setWidget(3,1,_sigmaYMinTextBox);
        thrSigGrid.setWidget(3,2,_sigmaYMaxTextBox);
        thrSigGrid.setWidget(4,0,HtmlUtils.getHtml("Z","prompt"));
        thrSigGrid.setWidget(4,1,_sigmaZMinTextBox);
        thrSigGrid.setWidget(4,2,_sigmaZMaxTextBox);

        thresholdPanel.add(thrPosGrid);
        thresholdPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        thresholdPanel.add(thrSigGrid);
        return thresholdPanel;
    }

    private RoundedButton getValidationButton(String testFilePath) {
        _validationFilePath = testFilePath;
        return new RoundedButton("Validate Target", new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                _dataservice.validateFilePath(_validationFilePath, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem validating the path."), 250).showPopup(_clearButton);
                    }

                    public void onSuccess(Object o) {
                        new PopupCenteredLauncher(new InfoPopupPanel("The file exists and is reachable."), 250).showPopup(_clearButton);
                    }
                });
            }
        });
    }

    public Widget getStatusMessage() {
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
//        _submitButton.setEnabled(false);
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void submitJob() {
        // Validate Information.  If any of the validations fail then leave
        boolean inputFileGood       = checkValue("Input File Or Directory",_inputFileOrDirTextBox.getText());
        if (!inputFileGood) { return; }

        boolean intensityFileGood   = checkValue("Intensity Correction File", _intensityCorrectionMatrixFileTextBox.getText());
        if (!intensityFileGood) { return; }

        boolean microscopeFileGood  = checkValue("Microscope Settings File", _microscopeSettingsTextBox.getText());
        if (!microscopeFileGood) { return; }

        boolean transformFileGood   = checkValue("Transformation File", _transformationFileTextBox.getText());
        if (!transformFileGood) { return; }

        // submit the job
        _ticTask = new BatchTicTask();
//        String nodeId = "Test";//naaResultsTable.getValue(naaResultsTable.getSelectedRow().getRowIndex(), 0).toString();
        _ticTask.setJobName("TIC Job");
        _ticTask.setParameter(BatchTicTask.PARAM_inputFile, _inputFileOrDirTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_borderValue, _borderCropPixelsTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_transformationFile, _transformationFileTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_intensityCorrectionFactorFile, _intensityCorrectionMatrixFileTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_microscopeSettingsFile, _microscopeSettingsTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_runApplyCalibrationToFrame, _applyCalibrationCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_runIlluminationCorrection, _illuminationCorrectionCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_runFQBatch, _fqbatchCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_spotDataOnly, _spotsOnlyCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_avgReadNoise, _avgReadNoiseTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_avgDark, _avgDarkTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionXMin, _positionXMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionXMax, _positionXMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionYMin, _positionYMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionYMax, _positionYMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionZMin, _positionZMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionZMax, _positionZMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaXMin, _sigmaXMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaXMax, _sigmaXMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaYMin, _sigmaYMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaYMax, _sigmaYMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaZMin, _sigmaZMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaZMax, _sigmaZMaxTextBox.getText());
//        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();

        new SubmitJob(_ticTask, new JobSubmissionListener()).runJob();
    }

    private boolean checkValue(String valueName, String text) {
        if (null==text || "".equals(text)) {
            new PopupCenteredLauncher(new ErrorPopupPanel(valueName+" cannot be empty."), 250).showPopup(_submitButton);
            return false;
        }
        String testTxt = text.trim();
        if (testTxt.contains(" ")) {
            new PopupCenteredLauncher(new ErrorPopupPanel(valueName+" cannot contain spaces.  Please fix."), 250).showPopup(_submitButton);
            return false;
        }
        return true;
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
//            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
//            _submitButton.setEnabled(true);
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }


}
