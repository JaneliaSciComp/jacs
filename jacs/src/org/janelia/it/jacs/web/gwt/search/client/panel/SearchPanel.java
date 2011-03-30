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
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SearchPanel extends Composite {
    Panel _canvas;
    RoundedPanel2 _canvasWrapper;

    public static final String SEARCH_STYLE = "SearchPanel";
    public static final String ROUNDING_STYLE = "SearchRounding";
    public static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    public SearchPanel() {
        super();
        _canvas = new HorizontalPanel();
        _canvas.setStyleName(SEARCH_STYLE);
        _canvasWrapper = new RoundedPanel2(_canvas, RoundedPanel2.ALL, BORDER_COLOR);
        _canvasWrapper.setCornerStyleName(ROUNDING_STYLE);
        initWidget(_canvasWrapper);
    }

    public void add(Widget w) {
        _canvas.add(w);
    }

}

