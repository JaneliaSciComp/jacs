
package org.janelia.it.jacs.web.gwt.frdata.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.frdata.client.popup.CompleteRecruitmentPopup;
import org.janelia.it.jacs.web.gwt.frdata.client.popup.CreateBlastDatabasePopup;
import org.janelia.it.jacs.web.gwt.frdata.client.popup.RecruitAgainstSamplesPopup;

/**
 * @author Todd Safford
 */
public class FRDataPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frdata.client.FRDataPanel");

    private RoundedButton _createBlastDatabase;
    private RoundedButton _recruitAgainstSamples;
    private RoundedButton _completeRecruitmentAgainstSampling;
    private TitledBox _mainPanel;
    private JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public FRDataPanel(String title, String taskId, JobSubmissionListener listener) {
        _listener = listener;
        init(title, taskId);
    }

    private void init(String title, String taskId) {
        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        initWidget(_mainPanel);

        // Prepopulate if a taskId was supplied
        if (StringUtils.hasValue(taskId)) {
            setJob(taskId);
        } // will populateContentPanel() after loading data
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _createBlastDatabase = new RoundedButton("Provide FASTA file of Reads", new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(new CreateBlastDatabasePopup(_listener), 250).showPopup(_createBlastDatabase);
            }
        });
        _recruitAgainstSamples = new RoundedButton("Recruit Against Genomes via Sampling", new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(new RecruitAgainstSamplesPopup(_listener), 250).showPopup(_recruitAgainstSamples);
            }
        });
        _completeRecruitmentAgainstSampling = new RoundedButton("Complete Recruitment of Sampling", new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(new CompleteRecruitmentPopup(_listener), 250).showPopup(_completeRecruitmentAgainstSampling);
            }
        });
        _completeRecruitmentAgainstSampling.setEnabled(false);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "smallSpacer"));
        contentPanel.add(_createBlastDatabase);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "smallSpacer"));
        contentPanel.add(_recruitAgainstSamples);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "smallSpacer"));
        contentPanel.add(_completeRecruitmentAgainstSampling);
        _mainPanel.add(contentPanel);
    }

    /**
     * Notification that a file was uploaded as part of executing a BLAST job.  Put the node (that was created
     * for the file contents) in the BlastData as the query sequence node as if it was a previous sequence (so we
     * don't try to upload it again).
     */
    private class UploadListener implements AsyncCallback {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(Object result) {
            _logger.info("User file was was uploaded, adding info to BlastData");
            UserDataNodeVO node = (UserDataNodeVO) result;
        }
    }

    public void setJob(String taskId) {
//        _blastData.setTaskIdFromParam(taskId);
//        populateBlastDataFromTaskId();
    }

}