
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.processors.recruitment.ProjectData;
import org.janelia.it.jacs.shared.processors.recruitment.SampleData;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SmallLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.LinkClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 25, 2007
 * Time: 4:46:37 PM
 */
public class FrvControlsPanelSampleTab extends FrvControlsPanelBaseTab {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.panels.tabs.FrvControlsPanelSampleTab");
    private VerticalPanel _verticalPanel = new VerticalPanel();
    private static final int NUM_COLS_DISPLAYED = 4;
    private static final int SAMPLE_DATA_NAME_MAX_LENGTH = 32;
    public static final String LABEL_NAME = "Sample Filters";
    private HashMap<String, String> sampleDescToSampleNameHash = new HashMap<String, String>();
    private HashMap<ProjectData, ArrayList<SampleData>> mapProjectToSortedSampleList = new HashMap<ProjectData, ArrayList<SampleData>>();
    private HashMap<String, CheckBox> sampleDescToCheckBoxHash = new HashMap<String, CheckBox>();
    private static final String ALL_DISPLAY = "select all";
    private static final String NONE_DISPLAY = "select none";

    public FrvControlsPanelSampleTab(JobSelectionListener jobSelectionListener) {
        super(jobSelectionListener);
        _verticalPanel.setWidth("850px");
        _verticalPanel.add(HtmlUtils.getHtml("<br>", "text"));
        _recruitmentService.getRVSampleData(new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Failed to get RV list of sample data");
            }

            public void onSuccess(Object result) {
                mapProjectToSortedSampleList = (HashMap<ProjectData, ArrayList<SampleData>>) result;
                for (Object o : mapProjectToSortedSampleList.values()) {
                    List tmpSampleList = (List) o;
                    if (null == tmpSampleList) {
                        continue;
                    }
                    for (Object aTmpSampleList : tmpSampleList) {
                        SampleData tmpData = (SampleData) aTmpSampleList;
                        CheckBox tmpCheckBox = new CheckBox();
                        sampleDescToCheckBoxHash.put(tmpData.getDescription(), tmpCheckBox);
                        sampleDescToSampleNameHash.put(tmpData.getDescription(), tmpData.getName());
                    }
                }
                buildSamplePanel();
                setJob(_job);
            }
        });
    }


    private void buildSamplePanel() {
        // Sort the projects by their descriptions
        ArrayList<ProjectData> projectList = new ArrayList<ProjectData>();
        for (ProjectData o : mapProjectToSortedSampleList.keySet()) {
            projectList.add(o);
        }
        Collections.sort(projectList);
        ScrollPanel scrollPanel = new ScrollPanel();
        scrollPanel.setHeight("200px");
        VerticalPanel internalPanel = new VerticalPanel();
        scrollPanel.add(internalPanel);
        for (Object aProjectList : projectList) {
            ProjectData tmpProjectData = (ProjectData) aProjectList;
            String tmpProjectAccession = tmpProjectData.getProjectName();
            List tmpSampleList = mapProjectToSortedSampleList.get(tmpProjectData);
            // Assumes at least one sample item for a project
            SampleData tmpData = (SampleData) tmpSampleList.get(0);
            // Create a grid for the legend items
            HorizontalPanel labelPanel = new HorizontalPanel();
            labelPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            labelPanel.add(HtmlUtils.getHtml(">>&nbsp;", "greaterGreater"));
            labelPanel.add(HtmlUtils.getHtml(tmpData.getProjectName() + "&nbsp;", "frvFilterTableHeader"));
            ActionLink allLink = new ActionLink(ALL_DISPLAY, new SelectionClickListener(tmpProjectData, true));
            ActionLink noneLink = new ActionLink(NONE_DISPLAY, new SelectionClickListener(tmpProjectData, false));
            labelPanel.add(allLink);
            labelPanel.add(noneLink);
            internalPanel.add(labelPanel);
            FlexTable _grid;
            _grid = new FlexTable();
            _grid.setCellPadding(0);
            _grid.setCellSpacing(0);
            internalPanel.add(_grid);
            int numPerCol = (int) Math.ceil(((double) tmpSampleList.size() / (double) NUM_COLS_DISPLAYED));
            for (int i = 0, col = 0; i < tmpSampleList.size(); i++) {
                SampleData item = (SampleData) tmpSampleList.get(i);
                // if the sample belongs to this project, then add it
                if (null != tmpProjectAccession && tmpProjectAccession.equalsIgnoreCase(item.getProjectAccession())) {
                    int row = i % numPerCol;
                    if (row == 0) {
                        col += 3; // skip to next set of 3 cols (checkbox/link/spacer)
                    }
                    SmallLink link = new SmallLink(
                            FulltextPopperUpperHTML.abbreviateText(item.getDescription(), SAMPLE_DATA_NAME_MAX_LENGTH),
                            new LinkClickListener(item.getName(), _grid));
                    CheckBox tmpCheckBox = sampleDescToCheckBoxHash.get(item.getDescription());
                    _grid.setWidget(row, col, tmpCheckBox);
                    _grid.setWidget(row, col + 1, link);
                    _grid.setWidget(row, col + 2, HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;", "smallText"));
                }
            }
            internalPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;", "smallText"));
        }
        _verticalPanel.add(scrollPanel);
    }

    public void setJob(JobInfo job) {
        super.setJob(job);
        if (null == job) return;
        RecruitableJobInfo systemJob = (RecruitableJobInfo) job;
        String sampleString = systemJob.getSamplesRecruited();
        for (Object o : mapProjectToSortedSampleList.values()) {
            List tmpSampleList = (List) o;
            for (Object aTmpSampleList : tmpSampleList) {
                SampleData item = (SampleData) aTmpSampleList;
                CheckBox tmpCheckBox = sampleDescToCheckBoxHash.get(item.getDescription());
                // Set the checkbox to the sample state
                if (null != tmpCheckBox) {
                    tmpCheckBox.setValue(sampleString.indexOf(sampleDescToSampleNameHash.get(item.getDescription())) >= 0);
                }
            }
        }
    }

    /**
     * This method is intended to alert the panels that someone is going to submit a job and they should update any
     * changes they care about.
     *
     * @return boolean to state whether changes occurred
     */
    public boolean updateJobChanges() {
        StringBuffer buf = new StringBuffer();
        for (Object o : sampleDescToCheckBoxHash.keySet()) {
            String tmpDesc = (String) o;
            CheckBox tmpCheckBox = sampleDescToCheckBoxHash.get(tmpDesc);
            // If checked, grab the sample name for the comma list
            if (tmpCheckBox.getValue()) {
                buf.append(sampleDescToSampleNameHash.get(tmpDesc)).append(",");
            }
        }
        String tmpSampleStr = buf.toString();
        if (tmpSampleStr.endsWith(",")) {
            tmpSampleStr = tmpSampleStr.substring(0, tmpSampleStr.length() - 1);
        }
        if (!_job.getSamplesRecruited().equals(tmpSampleStr)) {
//            Window.alert(_job.getSamplesRecruited()+"\n NOT equal to "+tmpSampleStr);
            _job.setSamplesRecruited(buf.toString());
            return true;
        }
//        Window.alert("The samples are equal.");
        return false;
    }

    public Widget getPanel() {
        return _verticalPanel;
    }

    public String getTabLabel() {
        return LABEL_NAME;
    }

    private class SelectionClickListener implements ClickListener {
        private ProjectData projectData;
        private boolean check = false;

        private SelectionClickListener(ProjectData projectData, boolean check) {
            this.projectData = projectData;
            this.check = check;
        }

        public void onClick(Widget sender) {
            List projectSamples = mapProjectToSortedSampleList.get(projectData);

            // iterate through the samples for the project, get their checkboxes and set
            for (Object projectSample : projectSamples) {
                SampleData tmpSampleData = (SampleData) projectSample;
                (sampleDescToCheckBoxHash.get(tmpSampleData.getDescription())).setValue(check);
            }
        }
    }
}
