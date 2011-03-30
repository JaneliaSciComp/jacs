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

package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.common.client.ui.MultiValueSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.search.client.SearchEntityListener;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class CategorySearchResultTablePanel extends Composite {

    protected static final String CANVAS_STYLENAME = "CategorySearchResultTablePanel";
    protected static final String CANVASCORNER_STYLENAME = "SearchRounding";
    protected static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    private CategorySearchDataBuilder dataBuilder;

    public CategorySearchResultTablePanel(String category, String searchId, String searchQuery) {
        super();
        dataBuilder = new SearchResultTableBuilderFactory().createResultTableBuilder(category, searchId, searchQuery);
        HorizontalPanel canvas = new HorizontalPanel();
        canvas.setStyleName(CANVAS_STYLENAME);
        RoundedPanel2 canvasWrapper = new RoundedPanel2(canvas, RoundedPanel2.ALL, BORDER_COLOR);
        canvasWrapper.setCornerStyleName(CANVASCORNER_STYLENAME);
        initWidget(canvasWrapper);
        Panel dataPanel = dataBuilder.createDataPanel(10, new String[]{"10", "20", "50"});
        canvas.add(dataPanel);
    }

    public void populatePanel() {
        dataBuilder.populateDataPanel();
    }

    public void addDataRetrievedCallback(DataRetrievedListener listener) {
        dataBuilder.addDataRetrievedCallback(listener);
    }

    public void setEntityListener(SearchEntityListener entityListener) {
        dataBuilder.setEntityListener(entityListener);
    }

    public void setSelectionListener(MultiValueSelectionListener selectionListener) {
        dataBuilder.setSelectionListener(selectionListener);
    }

    public void selectItem(int itemIndex) {
        dataBuilder.selectItem(itemIndex);
    }

}
