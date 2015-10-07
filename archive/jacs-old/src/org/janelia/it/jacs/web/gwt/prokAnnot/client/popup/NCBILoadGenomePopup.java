
package org.janelia.it.jacs.web.gwt.prokAnnot.client.popup;

import java.util.ArrayList;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationBulkLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.FileChooserPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
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
public class NCBILoadGenomePopup extends ModalPopupPanel {
    private static final String NCBI_FTP_BASE_DIR = SystemProps.getString("ProkAnnotation.NcbiBaseDir", null);
    private static final String JCVI_BASE_DIR = SystemProps.getString("ProkAnnotation.BaseDir", null);
    private boolean _useBulkMode;
    private FileChooserPanel _fileChooserPanel;
    private RoundedButton _getFilesButton;
    private TextBox _ftpDirectoryTextBox;
    private String _localGenomeDirectoryName;
    private SybaseInfoPanel _sybaseInfoPanel;
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;

    public NCBILoadGenomePopup(String localGenomeDirectoryName, boolean useBulkLoad, JobSubmissionListener listener) {
        super("Load Data From NCBI", false);
        _useBulkMode = useBulkLoad;
        _listener = listener;
        _localGenomeDirectoryName = localGenomeDirectoryName;
    }

    protected void populateContent() {
        _ftpDirectoryTextBox = new TextBox();
        _ftpDirectoryTextBox = new TextBox();
        _ftpDirectoryTextBox.setMaxLength(120);
        _ftpDirectoryTextBox.setVisibleLength(80);
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.txt);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _getFilesButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _getFilesButton.setEnabled(false);
            }
        }, types);
        RoundedButton _clearButton = new RoundedButton("Clear");
        _getFilesButton = new RoundedButton("Load Data");
        _clearButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _ftpDirectoryTextBox.setText("");
                _fileChooserPanel.clear();
            }
        });
        _getFilesButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Task _currentTask;
                if (!_useBulkMode) {
                    _currentTask = new ProkaryoticAnnotationLoadGenomeDataTask();
                    _currentTask.setJobName(_localGenomeDirectoryName);
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_ftpSourceDirectory, NCBI_FTP_BASE_DIR + _ftpDirectoryTextBox.getText());
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_targetDirectory, (JCVI_BASE_DIR + "/" + _localGenomeDirectoryName));
                }
                else {
                    _currentTask = new ProkaryoticAnnotationBulkLoadGenomeDataTask();
                    _currentTask.setJobName("Bulk NCBI Genome Load");
                    _currentTask.setParameter(ProkaryoticAnnotationBulkLoadGenomeDataTask.PARAM_genomeListFile, _fileChooserPanel.getUploadedFileName());
                }
                submitJob(_currentTask);
            }
        });
        CenteredWidgetHorizontalPanel buttonPanel = new CenteredWidgetHorizontalPanel();
        buttonPanel.add(_clearButton);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        buttonPanel.add(_getFilesButton);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        buttonPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));
        buttonPanel.setVisible(true);

        VerticalPanel _singleModePanel = new VerticalPanel();
        HorizontalPanel tmpDirPanel = new HorizontalPanel();
        tmpDirPanel.add(HtmlUtils.getHtml("Organism Directory:", "prompt"));
        tmpDirPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        tmpDirPanel.add(HtmlUtils.getHtml(_localGenomeDirectoryName, "prompt"));
        HorizontalPanel tmpPanel = new HorizontalPanel();
        tmpPanel.add(HtmlUtils.getHtml("Paste directory name from&nbsp;", "prompt"));
        tmpPanel.add(new ExternalLink("NCBI", "ftp://ftp.ncbi.nih.gov/genbank/genomes/Bacteria/"));
        _singleModePanel.add(tmpDirPanel);
        _singleModePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _singleModePanel.add(tmpPanel);
        _singleModePanel.add(_ftpDirectoryTextBox);
        _singleModePanel.setVisible(true);

        VerticalPanel _bulkModePanel = new VerticalPanel();
        _bulkModePanel.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
        HorizontalPanel tmpBulkPanel = new HorizontalPanel();
        tmpBulkPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        tmpBulkPanel.add(HtmlUtils.getHtml("Choose genome list file", "prompt"));
        tmpBulkPanel.add(_fileChooserPanel);
        _bulkModePanel.add(HtmlUtils.getHtml("A genome list file is a file in the format of: <br>(organism directory &lt;tab&gt; NCBI ftp directory location)", "prompt"));
        _bulkModePanel.add(tmpBulkPanel);

        VerticalPanel _loadNCBIPanel = new VerticalPanel();
        _loadNCBIPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        _sybaseInfoPanel = new SybaseInfoPanel();
        _loadNCBIPanel.add(_sybaseInfoPanel);
        _loadNCBIPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        if (!_useBulkMode) {
            _loadNCBIPanel.add(_singleModePanel);
        }
        else {
            _loadNCBIPanel.add(_bulkModePanel);
        }
        _loadNCBIPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _loadNCBIPanel.add(buttonPanel);
        _loadNCBIPanel.setVisible(true);
        add(_loadNCBIPanel);
    }

    private void submitJob(Task currentTask) {
        if ((null == _ftpDirectoryTextBox.getText() || "".equals(_ftpDirectoryTextBox.getText())) && !_useBulkMode) {
            new PopupCenteredLauncher(new ErrorPopupPanel("An ftp directory is required.")).showPopup(null);
            return;
        }

        if ((null == _fileChooserPanel.getUploadedFileName() || "".equals(_fileChooserPanel.getUploadedFileName())) && _useBulkMode) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A genome list file is required.")).showPopup(null);
            return;
        }

        if (null == _sybaseInfoPanel.getUsername() || "".equals(_sybaseInfoPanel.getUsername())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase username is required.")).showPopup(null);
            return;
        }

        if (null == _sybaseInfoPanel.getSybasePassword() || "".equals(_sybaseInfoPanel.getSybasePassword())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase password is required.")).showPopup(null);
            return;
        }
        currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_username, _sybaseInfoPanel.getUsername());
        currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_sybasePassword, _sybaseInfoPanel.getSybasePassword());
        Preferences.setSubjectPreference(new SubjectPreference("sbLogin", "ProkPipeline", _sybaseInfoPanel.getUsername()));
        Preferences.setSubjectPreference(new SubjectPreference("sbPass", "ProkPipeline", _sybaseInfoPanel.getSybasePassword()));

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
