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

package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: gcao
 * Date: Jul 2, 2007
 * Time: 9:41:17 AM
 */
public class EditPublication extends BaseEntryPoint {


    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.EDIT_PUBLICATION_SECTION_LABEL), Constants.ROOT_PANEL_NAME);

        VerticalPanel rows = new VerticalPanel();
        rows.setWidth("100%");

        // Edit Project panel
        EditPublicationPanel editPublicationPanel = new EditPublicationPanel();
        editPublicationPanel.setWidth("100%");
        rows.add(editPublicationPanel);

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(rows);
        show();

    }


}
