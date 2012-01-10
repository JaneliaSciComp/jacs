
package org.janelia.it.jacs.web.gwt.blast.client.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.blast.client.panel.BlastOptionsPanel;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

import java.util.Set;

public class BlastWizardSubmitJobPage extends BlastWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardSubmitJobPage");

    public static final String BLAST_OPTIONS_LINK_HELP_PROP = "BlastOptions.HelpURL";
    public static final String HISTORY_TOKEN = "BlastWizardSubmitJobPage";

    private VerticalPanel _mainPanel;
    private FlexTable _dataPanelGrid;
    private TextBox _jobNameTextBox;
    private BlastOptionsPanel _blastOptions;
    private static BlastServiceAsync _blastservice = (BlastServiceAsync) GWT.create(BlastService.class);

    static {
        ((ServiceDefTarget) _blastservice).setServiceEntryPoint("blast.srv");
    }

    public BlastWizardSubmitJobPage(BlastData blastData, WizardController controller) {
        super(blastData, controller);
        init();
    }

    private void init() {
        _blastOptions = new BlastOptionsPanel(getData());

        TitledBox jobCriteriaTitledBox = getDataPanel("Job Criteria");
        _mainPanel = new VerticalPanel();
        _mainPanel.add(jobCriteriaTitledBox);
        _mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _mainPanel.add(getJobOptionsPanel());
    }

    private TitledBox getDataPanel(String title) {
        TitledBox box = new TitledBox(title, false);

        _dataPanelGrid = new FlexTable();
        int row = 0;

        // "Job Name:" prompt & text area
        _dataPanelGrid.setWidget(row, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        _dataPanelGrid.getCellFormatter().setStyleName(row, 0, "gridCell");

        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);

        //textBox.setFocus(true); // breaks IE
        _dataPanelGrid.setWidget(row, 1, _jobNameTextBox);
        _dataPanelGrid.getCellFormatter().setStyleName(row, 1, "gridCell");

        // "Reference Sequence:" prompt, value & change link
        ++row;
        _dataPanelGrid.setWidget(row, 0, HtmlUtils.getHtml("Query Sequence:", "prompt"));
        _dataPanelGrid.getCellFormatter().setStyleName(row, 0, "gridCell");
        _dataPanelGrid.setWidget(row, 1, new HTML("reference sequence goes here")); // real value filled in on preProcess()
        _dataPanelGrid.getCellFormatter().setStyleName(row, 1, "gridCell");

        // "Subject Sequence:" prompt, value and change link
        ++row;
        _dataPanelGrid.setWidget(row, 0, HtmlUtils.getHtml("Reference Dataset:", "prompt"));
        _dataPanelGrid.getCellFormatter().setStyleName(row, 0, "gridCellBottom");
        _dataPanelGrid.setWidget(row, 1, new HTML("subject sequence goes here")); // real value filled in on preProcess()
        _dataPanelGrid.getCellFormatter().setStyleName(row, 1, "gridCellBottom");

        box.add(_dataPanelGrid);

        return box;
    }

    private TitledBox getJobOptionsPanel() {
        LoadingLabel programsLoadingMsg = new LoadingLabel(/*visible*/ false);

        // Program: prompt
        HorizontalPanel programPanel = new HorizontalPanel();
        programPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        programPanel.add(HtmlUtils.getHtml("Program:", "prompt"));
        programPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        programPanel.add(_blastOptions.getProgramMenu());
        programPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        programPanel.add(programsLoadingMsg);

        TitledBox jobOptionsTitledBox = new TitledBox("Job Options");
        jobOptionsTitledBox.add(programPanel);
        jobOptionsTitledBox.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        jobOptionsTitledBox.add(_blastOptions);
        TitledBoxActionLinkUtils.addHelpActionLink(jobOptionsTitledBox, new HelpActionLink("help"), BLAST_OPTIONS_LINK_HELP_PROP);

        return jobOptionsTitledBox;
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_WIZARD_PROGRAM_OPTIONS_LABEL;
    }

    protected void preProcess(Integer priorPageNumber) {
        if (getData().getTaskIdFromParam() != null)
            populateBlastDataFromTaskId();
        else
            finishPreProcess();
    }

    protected void populateBlastDataFromTaskId() {
        try {
            String taskId = getData().getTaskIdFromParam().trim();
            _logger.error("BlastWizardSubmitJobPage populateBlastDataFromTaskId() calling _blastservice.populateBlastDataFromTaskId() taskId=" + taskId);
            _blastservice.getPrepopulatedBlastTask(taskId, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("BlastWizardSubmitJobPage preProcess() caught Exception=" + caught.getMessage(), caught);
                    String message = "There was an error retrieving blast results for taskId=" + getData().getTaskIdFromParam();
                    ErrorPopupPanel popup = new ErrorPopupPanel(message + ", restarting Blast Wizard");
                    new PopupCenteredLauncher(popup, 250).showPopup(null);
                    new Timer() {
                        public void run() {
                            getController().gotoPage(0);
                        }
                    }.schedule(1000);
                }

                public void onSuccess(Object result) {
                    BlastTaskVO blastTaskVO = (BlastTaskVO) result;
                    getData().setBlastTask(blastTaskVO.getBlastTask());
                    getData().setMostRecentlySelectedQuerySequenceType(blastTaskVO.getQueryType());
                    getData().setMostRecentlySelectedSubjectSequenceType(blastTaskVO.getSubjectType());

                    // Populate query node
                    UserDataNodeVO queryNodeVO = blastTaskVO.getQueryNodeVO();
                    getData().getQuerySequenceDataNodeMap().clear();
                    getData().getQuerySequenceDataNodeMap().put(queryNodeVO.getDatabaseObjectId(), queryNodeVO);
                    getData().setMostRecentlySpecifiedQuerySequenceName(queryNodeVO.getNodeName());

                    // Populate subject database node
                    getData().getSubjectSequenceDataNodeMap().clear();
                    Set<BlastableNodeVO> subjectNodes = blastTaskVO.getSubjectNodeVOs();
                    if (subjectNodes != null && subjectNodes.size() > 0) {
                        BlastableNodeVO subjectNodeVO = subjectNodes.iterator().next();
                        getData().getSubjectSequenceDataNodeMap().put(subjectNodeVO.getDatabaseObjectId(), subjectNodeVO);
                    }

                    finishPreProcess();
                }

            });
        }
        catch (Throwable e) {
            _logger.error("Error getting the blast programs. " + e.getMessage(), e);
        }
    }

    protected void finishPreProcess() {
        _blastOptions.getProgramMenu().clear();

        // Set the current values for reference and subject sequence on the GUI
        if (_blastOptions.prePopulated()) {
            setDataValue(getData().getMostRecentlySpecifiedQuerySequenceName(), 1, BlastWizardUserSequencePage.HISTORY_TOKEN, "gridCell", false);
            setDataValue(getPrePopulatedSubjectName(), 2, BlastWizardSubjectSequencePage.HISTORY_TOKEN, "gridCellBottom", false);
            _jobNameTextBox.setText(getData().getBlastTask().getJobName());
            getButtonManager().setBackButtonEnabled(false);
        }
        else {
            setDataValue(getData().getMostRecentlySpecifiedQuerySequenceName(), 1, BlastWizardUserSequencePage.HISTORY_TOKEN, "gridCell", true);
            setDataValue(getData().getSubjectSequenceNodesNamesString(), 2, BlastWizardSubjectSequencePage.HISTORY_TOKEN, "gridCellBottom", true);
        }

        _blastOptions.updateBlastPrograms(getData().getMostRecentlySelectedQuerySequenceType(),
                getData().getMostRecentlySelectedSubjectSequenceType(), new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                    }

                    public void onSuccess(Object result) { /* TODO: turn off program menu loading label */ }
                });
    }

    private String getPrePopulatedSubjectName() {
        String subjectName = "Reference Database";
        if (getData().getSubjectSequenceDataNodeMap() != null && getData().getSubjectSequenceDataNodeMap().size() > 0) {
            BlastableNodeVO subjNode = getData().getSubjectSequenceDataNodeMap().values().iterator().next();
            subjectName = subjNode.getNodeName();
        }

        return subjectName;
    }

    protected boolean isProgressionValid() {
        // If the job name doesn't exist then force the user to add one
        String text = _jobNameTextBox.getText();
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
            return false;
        }
        else {
            getData().getBlastTask().setJobName(text);
            return true;
        }
    }

    private void setDataValue(String value, int row, String pageName, String styleName, boolean permitChange) {
        try {
            // Reference sequence value
            Grid grid = new Grid(1, 2);
            grid.setWidget(0, 0, HtmlUtils.getHtml(value, "text"));
            if (permitChange)
                grid.setWidget(0, 1, new ActionLink("change", new ChangePageClickListener(pageName)));
            _dataPanelGrid.setWidget(row, 1, grid);
            _dataPanelGrid.getCellFormatter().setStyleName(row, 1, styleName);
        }
        catch (Throwable e) {
            _logger.error("Error setting the data value. " + e.getMessage(), e);
        }
    }

    public class ChangePageClickListener implements ClickListener {
        private String _pageName;

        public ChangePageClickListener(String pageName) {
            _pageName = pageName;
        }

        public void onClick(Widget widget) {
            _logger.debug("change link: going to page " + getController().findPageByName(_pageName) + "=" + _pageName);
            getController().gotoPageByName(_pageName);
        }
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }

    public void setupButtons() {
        super.setupButtons();

        if (getData().getBlastTask() != null)
            getButtonManager().setNextButtonEnabled(true);
        else
            getButtonManager().setNextButtonEnabled(true);

        if (_blastOptions.prePopulated())
            getButtonManager().setBackButtonEnabled(false);

        getButtonManager().setNextButtonText("Submit Job");
    }
}
