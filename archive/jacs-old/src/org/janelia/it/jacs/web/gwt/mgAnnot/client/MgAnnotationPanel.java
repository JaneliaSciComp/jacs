
package org.janelia.it.jacs.web.gwt.mgAnnot.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;

import static org.janelia.it.jacs.web.gwt.common.client.Constants.UPLOADED_FILE_NODE_KEY;


/**
 * Created by IntelliJ IDEA.
 * User: jgoll
 * Date: Jul 14, 2009
 * Time: 8:23:38 AM
 * layouts metagenomics annotation pipeline submission panel
 */

public class MgAnnotationPanel extends Composite {

    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.mgAnnot.client.MgAnnotationPanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    private static final String MG_ORF_ACTION = "Metagenomics Orf-Calling";
    private static final String MG_ANNOTATION_ACTION = "Metagenomics Annotation";

    private FileChooserPanel _fileChooserPanel;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    private String _uploadedFilePath;

    private ProjectCodePanel _projectCodePanel;

    private TextBox _jobNameTextBox;

    private CheckBox _clearRangeCheckBox;
    private ListBox _pipelineListBox;
    private Grid grid;

    private RoundedButton _submitButton;

    final Label label = new Label("Enter the text in Suggestion box");
    private LoadingLabel _statusMessage;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private boolean projectCodeRequired;

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    //constructor
    public MgAnnotationPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    //sets up panel
    private void init(String title) {
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        //creates new common title box
        TitledBox _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        initWidget(_mainPanel);

        initComponents();
        //create panel
        Panel panel = this.getContentPanel();

        //add to main panel
        _mainPanel.add(panel);
    }

    //create gwt components
    private void initComponents() {
        //common panels/modules
        _projectCodePanel = new ProjectCodePanel();
        _fileChooserPanel = getUploadPanel();

        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        _jobNameTextBox.setText("My Mg Annotation Pipeline job " + new FormattedDate().toString());

        _pipelineListBox = new ListBox();
        _pipelineListBox.setVisible(false);
        _pipelineListBox.setMultipleSelect(false);

        _clearRangeCheckBox = new CheckBox("Process only clear range residues");
        _clearRangeCheckBox.setVisible(false);
    }

    //layouts the form panel
    private Panel getContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        grid = new Grid(5, 3);
        grid.setCellSpacing(7);

        grid.setWidget(0, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(0, 1, _jobNameTextBox);

        grid.setWidget(1, 0, new HTMLPanel("<span class='prompt'>Input File:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(1, 1, _fileChooserPanel);

        grid.setWidget(2, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(2, 1, _projectCodePanel);

        grid.setWidget(3, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(3, 1, _finalOutputPanel);

        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getStatusMessage());
        return contentPanel;
    }

    //adjusts action list to match orf-calling actions
    private void addOrfCallingActions() {
        _pipelineListBox = new ListBox();
        _pipelineListBox.addItem(MG_ORF_ACTION);
        //_pipelineListBox.addItem(MG_ORF_AND_ANNOTATION_ACTION);
        _pipelineListBox.setVisible(true);


        grid.setWidget(3, 0, new HTMLPanel("<span class='prompt'>Action:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(3, 1, _pipelineListBox);

        grid.setWidget(4, 0, new HTMLPanel("<span class='prompt'>Clear Range:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(4, 1, _clearRangeCheckBox);
        _clearRangeCheckBox.setVisible(true);
    }

    //adjusts action list to match annotations actions
    private void addAnnotationActions() {
        _pipelineListBox = new ListBox();
        _pipelineListBox.addItem(MG_ANNOTATION_ACTION);
        _pipelineListBox.setVisible(true);

        grid.setWidget(3, 0, new HTMLPanel("<span class='prompt'>Action:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(3, 1, _pipelineListBox);

        // remove orf-calling parameter
        _clearRangeCheckBox.setVisible(false);
        grid.clearCell(4, 0);
        grid.clearCell(4, 1);
    }


    private Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    //method is exexuted whne the user hits the submit button
    private void submitJob() {

        //validate project code
        if (projectCodeRequired) {
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
        }

        //create file node from input file (push from temporary file location to filestore) and submit jobs
        _dataservice.saveUserDefinedFastaNode(_uploadedFilePath,
                UPLOADED_FILE_NODE_KEY, Node.VISIBILITY_PRIVATE, new AsyncCallback() {

                    public void onFailure(Throwable throwable) {
                        _logger.error("Failed in attempt to save the user data node. Exception:" + throwable.getMessage());
                        new PopupCenteredLauncher(new ErrorPopupPanel("Failed in attempt to save the user data node. Exception:" + throwable.getMessage())).showPopup(null);
                    }

                    //submit job
                    public void onSuccess(Object object) {
                        if (object == null) {
                            new PopupCenteredLauncher(new ErrorPopupPanel("File node object generation failed.")).showPopup(null);
                        }
                        else {
                            _logger.debug("Successfully called the service to save the user data node.");
                            UserDataNodeVO _fileNode = (UserDataNodeVO) object;


                            int selectedActionIndex = _pipelineListBox.getSelectedIndex();

                            //create generic task object
                            Task newTask = null;

                            //generate orf-caller task
                            if (_pipelineListBox.getItemText(selectedActionIndex).equals(MG_ORF_ACTION)) {
                                newTask = new MetaGenoOrfCallerTask();

                                //set clear range
                                newTask.setParameter(MetaGenoOrfCallerTask.PARAM_useClearRange, String.valueOf(_clearRangeCheckBox.getValue()));
                                newTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, _fileNode.getDatabaseObjectId());
                                newTask.setJobName(_jobNameTextBox.getText());

                            }

                            //generate annotation task
                            else if (_pipelineListBox.getItemText(selectedActionIndex).equals(MG_ANNOTATION_ACTION)) {
                                newTask = new MetaGenoAnnotationTask();
                                newTask.setParameter(MetaGenoAnnotationTask.PARAM_input_node_id, _fileNode.getDatabaseObjectId());
                                newTask.setJobName(_jobNameTextBox.getText());
                            }

                            if (null != newTask && projectCodeRequired) {
                                String projectCode = _projectCodePanel.getProjectCode();
                                if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                                    new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                                    return;
                                }
                                newTask.setParameter(Task.PARAM_project, projectCode);
                            }

                            if (_finalOutputPanel.overrideFinalOutputPath()) {
                                if (null != newTask) {
                                    newTask.setParameter(Task.PARAM_finalOutputDirectory, _finalOutputPanel.getFinalOutputDestination());
                                }
                            }
                            //submit job
                            new SubmitJob(newTask, new JobSubmissionListener()).runJob();
                            _submitButton.setEnabled(false);
                            _statusMessage.showSubmittingMessage();
                        }
                    }
                });
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

    //returns the file upload panel; actions are added to the UI depending on the file type
    private FileChooserPanel getUploadPanel() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.mpfa);
        types.add(FileChooserPanel.FILE_TYPE.seq);
        types.add(FileChooserPanel.FILE_TYPE.ffn);
        types.add(FileChooserPanel.FILE_TYPE.fa);
        types.add(FileChooserPanel.FILE_TYPE.faa);
        types.add(FileChooserPanel.FILE_TYPE.fna);
        types.add(FileChooserPanel.FILE_TYPE.fsa);
        types.add(FileChooserPanel.FILE_TYPE.fasta);
        types.add(FileChooserPanel.FILE_TYPE.frg);

        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {

                //adjust action options depending on sequence type
                if (_fileChooserPanel.getSequenceType().equals("nucleotide")) {
                    addOrfCallingActions();
                }
                else if (_fileChooserPanel.getSequenceType().equals("peptide")) {
                    addAnnotationActions();
                }

                //store file path to create file node in submitJob()
                _uploadedFilePath = value;
                _submitButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _uploadedFilePath = null;
                _submitButton.setEnabled(false);
            }
        }, types);
        return _fileChooserPanel;
    }

    //sets up the cancel and submit buttons
    private Widget getSubmitButtonPanel() {
        //create buttons
        _submitButton = new RoundedButton("Submit Job");
        _submitButton.setEnabled(false);

        RoundedButton _clearButton = new RoundedButton("Clear");

        //add listeners
        _submitButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        _clearButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                clear();
            }
        });

        //create horizontal panel
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    //re-opens the window
    private void clear() {
        Window.open(Window.Location.getHref(), "_self", "");
    }

}