
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.PulldownPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;
import org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserPopup;

/**
 * @author Michael Press
 */
abstract class FrvControlsPanelBaseTab implements FrvControlsPanelTab {
    protected RecruitableJobInfo _job;
    private QuerySequenceChooserPopup _queryChooserPopup;
    private JobSelectionListener _jobSelectionListener;
    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }

    public FrvControlsPanelBaseTab(JobSelectionListener listener) {
        setRecruitableJobSelectionListener(listener);
    }

    public void setRecruitableJobSelectionListener(JobSelectionListener listener) {
        _jobSelectionListener = listener;
    }

    public JobSelectionListener getRecruitableJobSelectionListener() {
        return _jobSelectionListener;
    }

    public JobInfo getJob() {
        return _job;
    }

    public void setJob(JobInfo job) {
        _job = (RecruitableJobInfo) job;
    }

    /*
     * Creates the query selector widget, which is a PulldownPopup (looks like a pulldown but clicking it opens a
     * popup to select the query).  The popup is opened 100 pixels above the pulldown so it has most of the vertical
     * page real estate to show the query selection table.
     */
    protected Widget getQueryWidget(String queryName, int displaySize) {
        if (_queryChooserPopup == null)
            _queryChooserPopup = new QuerySequenceChooserPopup(_jobSelectionListener);

        PulldownPopup widget = new PulldownPopup(_queryChooserPopup);
        widget.setLauncher(new PopupAtRelativePixelLauncher(-100, 0));
        if (queryName != null)
            widget.setText(queryName);

        return widget;
    }

    protected String getConcatenatedSubjectNames(JobInfo job) {
        StringBuffer buf = new StringBuffer();
        buf.append("&bull;&nbsp;").append(job.getSubjectName()).append("<br/>");
        return buf.toString();
    }

    protected void addPromptValuePair(HTMLTable table, int row, int col, String prompt, String itemValue) {
        TableUtils.addTextRow(table, new RowIndex(row), col, prompt, itemValue);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }

}
