
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.FrvReadExportTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.frv.client.Frv;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public class FrvControlsPanelDataTab extends FrvControlsPanelBaseTab {
    //    private static final String RETRIEVE_DATA_URL = "/jacs/ExportSelected/get_data.htm";
    private FlexTable _grid;
    private ListBox colorizationBox;
    public static final String LABEL_NAME = "Data";
    //    private String  _seqExportType = "fasta";  // default
    private static final int QUERY_MAX_SIZE = 80;
    private static final int SUBJ_MAX_SIZE = 80;

    public FrvControlsPanelDataTab(JobSelectionListener jobSelectionListener) {
        super(jobSelectionListener);
        colorizationBox = new ListBox();
    }

    public void setJob(JobInfo job) {
        super.setJob(job);
        RecruitableJobInfo systemJob = (RecruitableJobInfo) job;
        if (null != systemJob) {
            String colorizationType = systemJob.getColorizationType();
            if (null == colorizationType) {
                colorizationType = RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE;
            }
            if (RecruitmentViewerFilterDataTask.COLORIZATION_MATE.equals(colorizationType)) {
                colorizationBox.setSelectedIndex(1);
            }
            else {
                colorizationBox.setSelectedIndex(0);
            }
        }

        _grid.setWidget(0, 1, getQueryWidget((systemJob == null) ? null : systemJob.getQueryName(), QUERY_MAX_SIZE));
        _grid.setWidget(1, 1, getSubjectDbWidget(systemJob));
        _grid.setWidget(2, 1, colorizationBox);
        if (systemJob != null)
            _grid.setWidget(0, 2, (createFrvControlsExportWidget()));
        _grid.setVisible(true);
    }

    public Widget getPanel() {
        colorizationBox = new ListBox();
        colorizationBox.addItem(RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE);
        colorizationBox.addItem(RecruitmentViewerFilterDataTask.COLORIZATION_MATE);

        _grid = new FlexTable();

        addPromptValuePair(_grid, 0, 0, "Query Sequence", "placeholder");
        addPromptValuePair(_grid, 1, 0, "Subject Sequence", "placeholder");
        addPromptValuePair(_grid, 2, 0, "Color Reads By", "placeholder");
        return _grid;
    }

    public String getTabLabel() {
        return LABEL_NAME;
    }

    protected Widget getSubjectDbWidget(RecruitableJobInfo job) {
        if (job == null || job.getSubjectName() == null)
            return HtmlUtils.getHtml("&nbsp;", "text");
        else
            return new FulltextPopperUpperHTML(job.getSubjectName(), SUBJ_MAX_SIZE);
    }

    private Widget createFrvControlsExportWidget() {
        MenuBar menu = new MenuBar();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBar(true);

        dropDown.addItem("Export All Reads as FASTA", true, new Command() {
            public void execute() {
                Frv.trackActivity("FRV.ExportAll.FASTA", _job);
                // Accession id list means nothing.  The bounds determine the reads involved
                ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                attributeList.add(new SortArgument("defline"));
                attributeList.add(new SortArgument("sequence"));
                FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                        _job.getJobId(), _job.getRefAxisBeginCoord(), _job.getRefAxisEndCoord(),
                        _job.getPercentIdentityMin(), _job.getPercentIdentityMax(),
                        ExportWriterConstants.EXPORT_TYPE_FASTA,
                        null, attributeList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        dropDown.addItem("Export All Reads as CSV", true, new Command() {
            public void execute() {
                Frv.trackActivity("FRV.ExportAll.CSV", _job);
                // Accession id list means nothing.  The bounds determine the reads involved
                ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                attributeList.add(new SortArgument("defline"));
                attributeList.add(new SortArgument("sequence"));
                FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                        _job.getJobId(), _job.getRefAxisBeginCoord(), _job.getRefAxisEndCoord(),
                        _job.getPercentIdentityMin(), _job.getPercentIdentityMax(),
                        ExportWriterConstants.EXPORT_TYPE_CSV,
                        null, attributeList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        dropDown.addItem("Export All Reads as Excel", true, new Command() {
            public void execute() {
                Frv.trackActivity("FRV.ExportAll.Excel", _job);
                // Accession id list means nothing.  The bounds determine the reads involved
                ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                attributeList.add(new SortArgument("defline"));
                attributeList.add(new SortArgument("sequence"));
                FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                        _job.getJobId(), _job.getRefAxisBeginCoord(), _job.getRefAxisEndCoord(),
                        _job.getPercentIdentityMin(), _job.getPercentIdentityMax(),
                        ExportWriterConstants.EXPORT_TYPE_EXCEL,
                        null, attributeList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

    /**
     * This method is intended to alert the panels that someone is going to submit a job and they should update any
     * changes they care about.
     *
     * @return boolean to state whether changes occurred
     */
    public boolean updateJobChanges() {
        boolean tmpChanges = false;
        String tmpColorType = colorizationBox.getValue(colorizationBox.getSelectedIndex());
        if (!tmpColorType.equals(_job.getColorizationType())) {
            tmpChanges = true;
            _job.setColorizationType(tmpColorType);
//            Window.alert("Setting the color type to: "+tmpColorType);
        }
        else {
//            Window.alert("Leaving the color type to: "+_job.getColorizationType());
        }
        return tmpChanges;
    }

}
