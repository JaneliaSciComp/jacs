
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
public class FrvControlsPanelAnnotationTab extends FrvControlsPanelBaseTab {
    private VerticalPanel _verticalPanel;
    private TextBox searchBox = new TextBox();
    public static final String LABEL_NAME = "Annotation Filters";

    public FrvControlsPanelAnnotationTab(JobSelectionListener jobSelectionListener) {
        super(jobSelectionListener);
    }

    public void setJob(JobInfo job) {
        super.setJob(job);
        RecruitableJobInfo systemJob = (RecruitableJobInfo) job;
        searchBox = new TextBox();
        searchBox.setVisibleLength(40);
        String tmpAnnotString = systemJob.getAnnotationFilterString();
        if (null == tmpAnnotString || "".equals(tmpAnnotString)) {
            tmpAnnotString = "";
        }
        searchBox.setText(tmpAnnotString);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        buttonPanel.add(new ActionLink("clear", new ClickListener() {
            public void onClick(Widget sender) {
                searchBox.setText("");
            }
        }));
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", "text"));

        // Need to store/retrieve filters on the annotations.
        FlexTable _grid = new FlexTable();
        _grid.setWidget(0, 0, HtmlUtils.getHtml("Show annotations whose product have text:", "frvFilterTableHeader"));
        _grid.setWidget(0, 1, searchBox);
        _grid.setWidget(0, 2, buttonPanel);
        _verticalPanel.setWidth("600px");
        _verticalPanel.add(_grid);
    }

    public Widget getPanel() {
        _verticalPanel = new VerticalPanel();
        return _verticalPanel;
    }

    public String getTabLabel() {
        return LABEL_NAME;
    }

    /**
     * This method is intended to alert the panels that someone is going to submit a job and they should update any
     * changes they care about.
     *
     * @return boolean to state whether changes occurred
     */
    public boolean updateJobChanges() {
        // if both are useless, return false (no changes)
        String tmpText = searchBox.getText().trim();
        if ((null == _job.getAnnotationFilterString() || "".equals(_job.getAnnotationFilterString()))
                &&
                (null == tmpText || "".equals(tmpText))) {
            return false;
        }
        if (!_job.getAnnotationFilterString().equals(tmpText)) {
//             Window.alert(_job.getAnnotationFilterString()+" NOT equal to "+tmpText);
            _job.setAnnotationFilterString(tmpText);
            return true;
        }
        // Must be usable/comparable strings and equal
        return false;
    }

}