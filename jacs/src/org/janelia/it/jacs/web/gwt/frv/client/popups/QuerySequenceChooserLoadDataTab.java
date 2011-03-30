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

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 18, 2008
 * Time: 11:20:29 AM
 */
public class QuerySequenceChooserLoadDataTab implements QuerySequenceChooserTab {

    private static final String TAB_LABEL = "Blast and Recruit My Sequence";
    private UploadUserSequencePanel uploadPanel;

    public QuerySequenceChooserLoadDataTab(SelectionListener selectionListener) {
        uploadPanel = new UploadUserSequencePanel(selectionListener, SequenceType.NUCLEOTIDE, 10);
    }

    public Widget getPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        contentPanel.add(uploadPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        return contentPanel;
    }

    public String getTabLabel() {
        return TAB_LABEL;
    }

    public void setRecruitableJobSelectionListener(JobSelectionListener listener) {
        // Not valid in this context
        //Window.alert("Calling setRecruitableJobSelectionListener");
    }

    public void realize() {
        // Nothing to do
    }

    public BlastData getBlastData() {
        return uploadPanel.getBlastData();
    }

    public boolean validateAndPersistSequenceSelection() {
        return uploadPanel.validateAndPersistSequenceSelection();
    }
}
