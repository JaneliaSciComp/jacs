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

package org.janelia.it.jacs.web.gwt.search.client.panel.accession;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class AccessionSummarySearchPanel extends CategorySummarySearchPanel {

    private Image accessionIcon;

    public AccessionSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new AccessionSearchDataBuilder(searchId, searchQuery);
    }

    public void populatePanel() {
        // create the summary panel widgets
        createAccessionIcon();
        addItem(accessionIcon);
        Panel dataPanel = getDataBuilder().createDataPanel();
        addItem(dataPanel);
        // populate the summary panel
        getDataBuilder().populateDataPanel();
    }

    protected void init() {
        _canvas = new HorizontalPanel();
        _canvas.setStyleName("AccessionSummarySearchPanel");
        _canvas.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        _canvas.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        initWidget(_canvas);
    }

    private void createAccessionIcon() {
        SearchIconPanelFactory searchIconFactory = new SearchIconPanelFactory();
        accessionIcon = searchIconFactory.createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconSmall(), Constants.SEARCH_ALL);
    }

}
