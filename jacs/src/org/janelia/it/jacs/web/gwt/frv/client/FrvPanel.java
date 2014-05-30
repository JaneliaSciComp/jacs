
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.frv.client.panels.FrvControlsPanel;

public class FrvPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.FrvPanel");

    // todo This was a DraggableAreaPanel!!!!!!!
    private HorizontalPanel _contentsPanel;
    private FrvControlsPanel _frvControlsPanel;
//    private FrvOverviewPanel _frvOverviewPanel;
//    private FrvImagePanel _frvImagePanel;

    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public FrvPanel() {
        setWidth("100%");
    }

    public void setDefaultJob() {
        String defTaskId = SystemProps.getString("RecruitmentViewer.DefaultTaskId", null);
        if (defTaskId == null)
            _logger.error("Got null default FRV task ID from properties, starting with no default");
        else
            getDefaultTask(defTaskId);
    }

    public void getDefaultTask(String taskId) {
        _statusservice.getRecruitmentTaskById(taskId, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("Failed to retrieve default FRV task, starting with no default");
            }

            public void onSuccess(Object object) {
                if (object == null) {
                    _logger.error("Got null default FRV task, starting with no default");
                    setJob(null);
                }
                else {
                    _logger.info("Got default FRV task");
                    setJob((RecruitableJobInfo) object);
                }
            }
        });
    }

    public void setUserTask(String userPipelineTaskId) {
        _statusservice.getRecruitmentFilterTaskByUserPipelineId(userPipelineTaskId, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("Failed to retrieve user FRV task, starting with default");
                navFailureStartup();
            }

            public void onSuccess(Object object) {
                if (object == null) {
                    // This should only happen for very old data when pipeline child tasks didn't know their parents
                    _logger.error("Got null user FRV task, starting with no default");
                    navFailureStartup();
                }
                else {
                    _logger.info("Got user FRV task");
                    setJob((RecruitableJobInfo) object);
                }
            }

            private void navFailureStartup() {
                setDefaultJob();
//                new PopupCenteredLauncher(new InfoPopupPanel("Could not navigate to your data.<br>Please use the \"Query Sequence\" selector<br>and \"My Previous Work\" tab instead."), 500).showPopup(_frvOverviewPanel);
            }
        });
    }

    public void setJob(RecruitableJobInfo job) {
        setJob(job, null);
    }

    public void setJob(RecruitableJobInfo job, ActionLink link) {
        realize(link);
        // Notify each panel to show its info for the new job
        _frvControlsPanel.setJob(job);
//        _frvOverviewPanel.setJob(job);
//        _frvImagePanel.setJob(job);
    }

    private void realize(ActionLink link) {
        // Invisible area for dragging;  allows Overview to be dragged around entire screen contents
        if (_contentsPanel == null) {
            _contentsPanel = new HorizontalPanel();
            add(_contentsPanel);
        }

        // Controls and data panel
        if (_frvControlsPanel == null) {
            _frvControlsPanel = new FrvControlsPanel("Controls", new JobChangedListener());
            if (link != null)
                _frvControlsPanel.addActionLink(link);
            //_frvControlsPanel.setWidth("100%");
            _contentsPanel.add(_frvControlsPanel);
        }

        // Image panel
//        if (_frvImagePanel == null) {
//            _frvImagePanel = new FrvImagePanel("Fragment Recruitment Viewer");
//            _frvImagePanel.setWidth("900px");
//            //_frvImagePanel.setWidth("100%");
//            _contentsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
//            _contentsPanel.add(_frvImagePanel);
//        }
//
//        // Draggable overview panel;  is notified by FrvImagePanel of move events to update zoom window, and notifies
//        // FrvImagePanel of user move requests so map can be updated
//        if (_frvOverviewPanel == null) {
//            _frvOverviewPanel = new FrvOverviewPanel("Overview", _frvImagePanel.getMapMoveRequestListener(), _contentsPanel.getDragController());
//            _frvOverviewPanel.setVisible(false);
//            _contentsPanel.add(_frvOverviewPanel);
//            // todo Comment back in the map listener
//            //_frvImagePanel.setMapListener(_frvOverviewPanel);
//        }
    }

    /**
     * This is a hook for internal panels to notify this main panel that the job has been changed by the
     * user using the panel's controls.  We'll just re-init each panel with the new job
     */
    public class JobChangedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            setJob((RecruitableJobInfo) job);
        }

        /**
         * not applicable
         */
        public void onUnSelect() {
        }
    }
}
