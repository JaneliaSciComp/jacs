/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.status.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.export.FileNodeExportTask;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.BlastJobParamHelper;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtAbsolutePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.PreferenceService;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.PreferenceServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.StateChangeListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.status.client.StatusConstants;

import java.util.Map;

/**
 * @author Michael Press
 */
public class JobSummaryPanel extends TitledBox {
    private LoadingLabel _loadingLabel;
    private FlexTable _grid;

    private static final int DB_MAX_SIZE = 80;

//    private static final String RETRIEVE_DATA_URL = "/jacs/ExportSelected/get_data.htm";

    private static PreferenceServiceAsync _preferenceService = (PreferenceServiceAsync) GWT.create(PreferenceService.class);

    static {
        ((ServiceDefTarget) _preferenceService).setServiceEntryPoint("preference.srv");
    }

    public JobSummaryPanel(String title) {
        super(title, /*show action links*/true, /*show content*/true); // start with content shown by default
    }

    protected void init() {
        super.init();

        // Get the preference for whether the Job Summary panel should be open or closed (open by default); if pref is to hide, then hide it
        UserPreference state = Preferences.getUserPreference(StatusConstants.JOB_SUMMARY_STATE_PREF, StatusConstants.STATUS_PREF_CATEGORY, StatusConstants.SHOW);
        if (StatusConstants.HIDE.equals(state.getValue()))
            hideContent();

        // When the user hides or shows the panel, update the preference for next time
        getHideShowActionLink().addPrimaryStateChangeListener(new StateChangeListener() {
            public void onStateChange() {
                Preferences.setUserPreference(
                        new UserPreference(StatusConstants.JOB_SUMMARY_STATE_PREF, StatusConstants.STATUS_PREF_CATEGORY, StatusConstants.SHOW));
            }
        });
        getHideShowActionLink().addSecondaryStateChangeListener(new StateChangeListener() {
            public void onStateChange() {
                Preferences.setUserPreference(
                        new UserPreference(StatusConstants.JOB_SUMMARY_STATE_PREF, StatusConstants.STATUS_PREF_CATEGORY, StatusConstants.HIDE));
            }
        });
    }

    protected void popuplateContentPanel() {
        _loadingLabel = new LoadingLabel("Loading job information...", true);
        add(_loadingLabel);

        _grid = createGrid();
        _grid.setVisible(false);
        add(_grid);
    }

    private FlexTable createGrid() {
        FlexTable grid = new FlexTable();

        addPromptValuePair(grid, 0, 0, "Job ID", "placeholder");
        addPromptValuePair(grid, 0, 2, "Submitted", "placeholder");
        addPromptValuePair(grid, 0, 4, "Query Sequence", "placeholder");
        addPromptValuePair(grid, 0, 6, "Job parameters", "placeholder");

        addPromptValuePair(grid, 1, 0, "Job Name", "placeholder");
        addPromptValuePair(grid, 1, 2, "Program", "placeholder");
        addPromptValuePair(grid, 1, 4, "Subject Sequence", "placeholder");

        return grid;
    }

    /**
     * Called when the page has a concrete job to display.  Updates the values.
     *
     * @param job new job to use
     */
    public void setJob(BlastJobInfo job) {
        if (job == null)
            _loadingLabel.setText("Error: no job specified.");
        else {
            _grid.setText(0, 1, job.getJobId());
            _grid.setText(0, 3, new FormattedDateTime(job.getSubmitted().getTime()).toString());

            // query sequence link - downloads the query sequence
            Map<String, String> blastPopupParamMap = BlastJobParamHelper.createBlastPopupParamMap(job);
            final FileNodeExportTask exportTask = new FileNodeExportTask(blastPopupParamMap.get("query Id"),
                    ExportWriterConstants.EXPORT_TYPE_CURRENT, null, null);
            ExternalLink xl = new ExternalLink(job.getQueryName(), new ClickListener() {
                public void onClick(Widget sender) {
                    new AsyncExportTaskController(exportTask).start();
                }
            });

            _grid.setWidget(0, 5, xl);

            org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup parameterPopup = new JobParameterPopup(
                    job.getJobname(),
                    new FormattedDateTime(job.getSubmitted().getTime()).toString(),
                    blastPopupParamMap, false);
            parameterPopup.setPopupTitle("Job Parameters - " + job.getProgram());
            _grid.setWidget(0, 7, getParameterWidget(parameterPopup));

            _grid.setText(1, 1, job.getJobname());
            _grid.setText(1, 3, job.getProgram());
            _grid.setWidget(1, 5, getSubjectDbWidget(job));

            _loadingLabel.setVisible(false);
            _grid.setVisible(true);
        }
    }

    private Widget getParameterWidget(final JobParameterPopup popup) {
        return new ActionLink("view", new ClickListener() {
            public void onClick(Widget widget) {
                new PopupAtAbsolutePixelLauncher(popup, 100, 100).showPopup(_grid);
            }
        });
    }

    private Widget getSubjectDbWidget(BlastJobInfo job) {
        if (job.getSubjectName() != null)
            return new FulltextPopperUpperHTML(job.getSubjectName(), getConcatenatedSubjectNames(job));
        else // 1 subject db
            return new FulltextPopperUpperHTML(job.getAllSubjectNames().get(0), DB_MAX_SIZE);
    }

    private String getConcatenatedSubjectNames(BlastJobInfo job) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < job.getAllSubjectNames().size(); i++)
            buf.append("&bull;&nbsp;").append(job.getAllSubjectNames().get(i)).append("<br/>");
        return buf.toString();
    }

    private void addPromptValuePair(HTMLTable table, int row, int col, String prompt, String itemValue) {
        TableUtils.addTextRow(table, new RowIndex(row), col, prompt, itemValue);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }
}
