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

package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import org.janelia.it.jacs.web.gwt.common.client.popup.BaseInfoPopup;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * @author Michael Press
 */
public class ProjectInfoPopup extends BaseInfoPopup {
    private Project _project;

    public ProjectInfoPopup(String title, Project project, boolean realizeNow) {
        super(title, /*realize now*/ false);
        _project = project;

        if (realizeNow)
            realize();
    }

    protected void populateContent() {
        if (_project == null)
            return;

        clear();

        int row = 0;
        FlexTable grid = new FlexTable();
        grid.setStyleName("ProjectInfoPopup");
        grid.setCellPadding(0);
        grid.setCellSpacing(0);

        addRow(row++, grid, "Project", _project.getProjectName());
        if (_project.getPrincipalInvestigators() != null)
            addRow(row++, grid, "Principal&nbsp;Investigator", _project.getPrincipalInvestigators());
        if (_project.getFundedBy() != null)
            addRow(row++, grid, "Funded&nbsp;By", _project.getFundedBy());
        if (_project.getOrganization() != null)
            addRow(row++, grid, "Organization", _project.getOrganization());
        if (_project.getInstitutionalAffiliation() != null)
            addRow(row, grid, "Affiliation", _project.getInstitutionalAffiliation());
        if (_project.getDescription() != null) {
            String desc = _project.getDescription();
            if (_project.getDescription().length() > 1000) //TODO: constant
                desc = desc.substring(0, 1000) + "...";

            grid.setWidget(row, 0, HtmlUtils.getHtml("Description:", "prompt"));
            grid.getCellFormatter().setVerticalAlignment(row, 0, HasVerticalAlignment.ALIGN_TOP);

            HTML html = HtmlUtils.getHtml(desc, "text");
            grid.setWidget(row, 1, html);
            grid.getCellFormatter().setVerticalAlignment(row, 1, HasVerticalAlignment.ALIGN_TOP);
        }

        add(grid);
    }
}