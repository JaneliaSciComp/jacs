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

package org.janelia.it.jacs.web.gwt.frv.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.model.tasks.LegendItem;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SmallLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.LinkClickListener;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;

/**
 * @author Michael Press
 */
public class FrvHistogramPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.home.client.Home");

    private RecruitableJobInfo _job;
    private HorizontalPanel _panel;
    private FlexTable _grid;

    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }

    private static final int SITE_NAME_MAX_LENGTH = 32;
    private static final int NUM_LEGEND_COLS = 4;

    public FrvHistogramPanel() {
        // Create a panel
        _panel = new HorizontalPanel();
        _panel.setStyleName("frvLegendPanel");
        _panel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        _panel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

        // Create a grid for the legend items
        _grid = new FlexTable();
        _grid.setCellPadding(0);
        _grid.setCellSpacing(0);
        _panel.add(_grid);

        initWidget(_panel);
    }

    public void setJob(RecruitableJobInfo job) {
        _job = job;
        populateLegend();
    }

    private void populateLegend() {
        if (_job == null)
            return;

        _grid.clear();
        _logger.debug("populating legend...");

        _recruitmentService.getLegend(_job.getRecruitmentResultsFileNodeId(), new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("Unable to retrieve legend for node " + _job.getRecruitmentResultsFileNodeId());
                _grid.setWidget(0, 0, HtmlUtils.getHtml("An error occurred retrieving legend information.", "error"));
            }

            public void onSuccess(Object object) {
                LegendItem[] legendItems = (LegendItem[]) object;
                int numPerCol = (int) Math.ceil(((double) legendItems.length / (double) NUM_LEGEND_COLS));
                for (int i = 0, col = 0; i < legendItems.length; i++) {
                    int row = i % numPerCol;
                    if (row == 0)
                        col += 3; // skip to next set of 3 cols (color/name/spacer)

                    // Dynamically set the color
                    LegendItem item = legendItems[i];
                    HTML colorBlock = new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"); //sets the width
                    colorBlock.setStyleName("frvLegendColorBlock");
                    DOM.setStyleAttribute(colorBlock.getElement(), "backgroundColor",
                            "rgb(" + item.getRed() + "," + item.getGreen() + "," + item.getBlue() + ")");

                    SmallLink link = new SmallLink(
                            FulltextPopperUpperHTML.abbreviateText(item.getDisplayValue(), SITE_NAME_MAX_LENGTH),
                            new LinkClickListener(item.getId(), _grid));

                    _grid.setWidget(row, col, colorBlock);
                    _grid.setWidget(row, col + 1, link);
                    _grid.setWidget(row, col + 2, HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;", "smallText"));
                }
            }
        });
    }

    public Panel getPanel() {
        return _panel;
    }
}