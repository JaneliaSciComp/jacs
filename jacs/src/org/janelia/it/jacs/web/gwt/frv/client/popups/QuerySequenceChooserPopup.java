
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;

import java.util.HashMap;
import java.util.Set;

/**
 * Can't make this modal or else user can't move the Overview panel out of the way.
 *
 * @author Michael Press
 */
public class QuerySequenceChooserPopup extends BasePopupPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserPopup");
    public static final Integer TAB_SYSTEM = 0;
    public static final Integer TAB_USER = 1;
    public static final Integer TAB_SEQUENCE = 2;

    private RecruitableJobInfo _job;
    private JobSelectionListener _jobSelectionListener;
    private QuerySequenceChooserSystemTab _systemTab;
    private QuerySequenceChooserLoadDataTab _loadDataTab;
    private QuerySequenceChooserUserTab _userTab;
    private RoundedTabPanel tabPanel = new RoundedTabPanel();
    private RoundedButton _applyButton;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);
    private static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }

    /**
     * @param jobSelectionListener callback when a job is selected and Apply button is hit
     */
    public QuerySequenceChooserPopup(JobSelectionListener jobSelectionListener) {
        super("Select a Query Sequence", /*realize now*/false, /*autohide*/false, /*modal*/ false);
        _jobSelectionListener = jobSelectionListener;
    }

    protected void populateContent() {
        // Create the tabs
        _systemTab = new QuerySequenceChooserSystemTab(new RowSelectedListener(), new RowDoubleClickSelectedListener());
        _userTab = new QuerySequenceChooserUserTab(new RowSelectedListener(), new RowDoubleClickSelectedListener());
        _loadDataTab = new QuerySequenceChooserLoadDataTab(new SequenceSelectionListener());

        tabPanel.add(_systemTab.getPanel(), _systemTab.getTabLabel());
        tabPanel.add(_userTab.getPanel(), _userTab.getTabLabel());
        tabPanel.add(_loadDataTab.getPanel(), _loadDataTab.getTabLabel());
        tabPanel.selectTab(0);
        tabPanel.setWidth("100%");

        // Create a listener to notify the tab when it's visible so it can load data.  We'll create the
        // listener AFTER the selectTab() above so the first tab won't be called until we explicitly do
        // it when the popup is shown
        final HashMap<Integer, Object> tabs = new HashMap<Integer, Object>();
        tabs.put(TAB_SYSTEM, _systemTab);
        tabs.put(TAB_USER, _userTab);
        tabs.put(TAB_SEQUENCE, _loadDataTab);
        tabPanel.addTabListener(new TabListener() {
            public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
                _applyButton.setEnabled(false);
                ((QuerySequenceChooserTab) tabs.get(new Integer(tabIndex))).realize();
            }

            public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
                return true; // allows the tab to be selected
            }
        });

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(tabPanel);
    }

    public class SequenceSelectionListener implements SelectionListener {

        public void onSelect(String value) {
            _applyButton.setEnabled(true);
        }

        public void onUnSelect(String value) {
            _applyButton.setEnabled(false);
        }
    }

    public class RowSelectedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            // This is a little weak
            if (!TAB_SEQUENCE.equals(tabPanel.getTabBar().getSelectedTab())) {
                _job = (RecruitableJobInfo) job;
            }
            _applyButton.setEnabled(true);
        }

        public void onUnSelect() {
            // This is a little weak
            if (!TAB_SEQUENCE.equals(tabPanel.getTabBar().getSelectedTab())) {
                _job = null;
            }
            _applyButton.setEnabled(false);
        }
    }

    /**
     * Same as RowSelectedListener except that it programatically executes the Apply button
     */
    public class RowDoubleClickSelectedListener extends RowSelectedListener {
        //public void onSelect(RecruitableJobInfo job) { _job = job; _applyButton.execute(); }
        public void onSelect(JobInfo job) {
            _job = (RecruitableJobInfo) job;
            _applyButton.execute();
        }
    }

    /**
     * When the popup comes up, instruct the paging panel to retrieve and load the first page of data
     */
    public void show() {
        super.show();
        _systemTab.realize(); // default tab
        _userTab.reloadData();
    }

    protected ButtonSet createButtons() {
        // Apply button = when a new node is selected, close the popup and notify the listener 
        _applyButton = new RoundedButton("Apply", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
                if (TAB_SEQUENCE.equals(tabPanel.getTabBar().getSelectedTab())) {
                    //Window.alert("Apply hit for user-provided sequence.");
                    boolean canProceed = _loadDataTab.validateAndPersistSequenceSelection();
                    if (!canProceed) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("Unable to blast and recruit your data at this time.")).showPopup(_applyButton);
                        return;
                    }
                    BlastData blastData = _loadDataTab.getBlastData();
                    if (null != blastData) {
                        // if the query node id exists, then use it
                        Set priorIdSet = blastData.getQuerySequenceDataNodeMap().keySet();
                        if (priorIdSet != null && priorIdSet.size() > 0) {
                            String queryNodeId = (String) priorIdSet.iterator().next();
                            blastData.getQuerySequenceDataNodeMap().clear(); // clear because we have id already
                            try {
                                _recruitmentService.runUserBlastRecruitment(queryNodeId, new AsyncCallback() {
                                    public void onFailure(Throwable caught) {
                                        _logger.error("Failed in attempt to submit the user Blast-FRV job. Exception:" + caught.getMessage());
                                        new PopupCenteredLauncher(new ErrorPopupPanel("Unable to blast and recruit your data at this time.")).showPopup(_applyButton);
                                    }

                                    public void onSuccess(Object result) {
                                        showSubmitPopup();
                                    }
                                });
                            }
                            catch (Throwable t) {
                                _logger.error("Error calling setupBlastJob() with query node id=" + queryNodeId);
                            }
                        }
                        // else, take the sequence, ensure FASTA, save to drive
                        else {
                            _dataservice.saveUserDefinedFastaNode(blastData.getMostRecentlySpecifiedQuerySequenceName(),
                                    blastData.getUserReferenceFASTA(),
                                    Node.VISIBILITY_PRIVATE,
                                    new AsyncCallback() {

                                        public void onFailure(Throwable throwable) {
                                            _logger.error("Failed in attempt to save the user data node. Exception:" + throwable.getMessage());
                                        }

                                        public void onSuccess(Object object) {
                                            try {
                                                _logger.debug("Successfully called the service to save the user data node. NodeId=" + object.toString());
                                                UserDataNodeVO newVO = (UserDataNodeVO) object;
                                                //Window.alert("Saved FASTA to node: "+newVO.getDatabaseObjectId());
                                                _recruitmentService.runUserBlastRecruitment(newVO.getDatabaseObjectId(), new AsyncCallback() {
                                                    public void onFailure(Throwable caught) {
                                                        _logger.error("Failed in attempt to submit the user Blast-FRV job. Exception:" + caught.getMessage());
                                                        new PopupCenteredLauncher(new ErrorPopupPanel("Unable to blast and recruit your data at this time.")).showPopup(_applyButton);
                                                    }

                                                    public void onSuccess(Object result) {
                                                        showSubmitPopup();
                                                    }
                                                });
                                            }
                                            catch (Throwable e) {
                                                _logger.error("Error trying to postprocess the job submission page. " + e.getMessage());
                                            }
                                        }

                                    });
                        }
                    }
                    return;
                }
                if (_jobSelectionListener != null && null != _job) {
                    _jobSelectionListener.onSelect(_job);
                }
            }

            private void showSubmitPopup() {
                new PopupCenteredLauncher(new FrvUserPipelinePopup()).showPopup(null);
            }

        });
        _applyButton.setEnabled(false);

        RoundedButton closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
            }
        });

        return new ButtonSet(new RoundedButton[]{_applyButton, closeButton});
    }

    /**
     * Hook for inner classes to hide the popup
     */
    protected void hidePopup() {
        hide();
    }

}
