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

package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.List;

/**
 * @author Michael Press
 */
public class QuerySequenceChooserSystemTab extends QuerySequenceChooserBaseTab {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserSystemTab");

    private static final String TAB_LABEL = "Public Data";

    public QuerySequenceChooserSystemTab(JobSelectionListener jobSelectionListener, JobSelectionListener jobSelectedAndAppliedListener) {
        super();
        setRecruitableJobSelectionListener(jobSelectionListener);
        setRecruitableJobSelectionAndApplyListener(jobSelectedAndAppliedListener);
    }

    public Widget getPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        contentPanel.add(getSearchArea());
        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        contentPanel.add(getTable());
        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        contentPanel.add(getHint());

        return contentPanel;
    }

    protected void populateSuggestOracle() {
        getStatusService().getSystemTaskQueryNames(new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Error retrieving system task query names: " + ((caught == null) ? "" : caught.getMessage()));
            }

            public void onSuccess(Object result) {
                List<String> names = (List<String>) result;
                _oraclePanel.addAllOracleSuggestions(names);
                //Window.alert("QueryChooserSystemTab retrieved " + (names==null?0:names.size()) + " query names for suggest box");
            }
        });
    }

    void getNumTaskResults(final DataRetrievedListener listener) {
        getStatusService().getNumRVSystemTaskResults(_searchString, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                //TODO: show some kind of error
                _logger.error(throwable.getMessage());
            }

            public void onSuccess(Object object) {
                listener.onSuccess(object);
            }
        });
        //    listener.onSuccess(new Integer(10));
    }

    void getPagedTaskResults(String searchString, int startIndex, int numRows, SortArgument[] sortArgs, final DataRetrievedListener listener) {
        getStatusService().getPagedRVSystemTaskResults(_searchString, startIndex, numRows, sortArgs, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                listener.onFailure(throwable);
            }

            public void onSuccess(Object object) {
                RecruitableJobInfo[] jobs = (org.janelia.it.jacs.shared.tasks.RecruitableJobInfo[]) object;
                if (jobs == null || jobs.length == 0) {
                    _logger.debug("Got 0 system jobs");
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(processDataBase(jobs));
                }
            }
        });
        //listener.onSuccess(getFakeData());
    }

    public String getTabLabel() {
        return TAB_LABEL;
    }
}
