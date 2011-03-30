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

package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.EditJobNameListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 24, 2008
 * Time: 11:12:26 AM
 */
public class JobNameWidget {
    public static final int JOBNAME_MAX_SIZE = 80;

    public static Widget getWidget(JobInfo job, TableRow row, int jobNameColumn, PagingPanel pagingPanel) {
        Grid grid = new Grid(1, 3);
        grid.setCellSpacing(0);
        grid.setCellPadding(0);

        grid.setWidget(0, 0, new FulltextPopperUpperHTML(job.getJobname(), JOBNAME_MAX_SIZE));
        grid.setWidget(0, 1, HtmlUtils.getHtml("&nbsp;", "text"));
        grid.setWidget(0, 2, new ActionLink("edit", new EditJobNameListener(job, row, jobNameColumn, pagingPanel)));
        return grid;
    }
}
