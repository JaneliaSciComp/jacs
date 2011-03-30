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

package org.janelia.it.jacs.web.gwt.download.client.samples;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Press
 */
public class SamplesMapLegendPanel extends Composite {
    private static final int PROJECT_NAME_MAX_LENGTH = 35;

    private VerticalPanel _contentPanel;
    private Map<String, Panel> _items;

    public SamplesMapLegendPanel() {
        init();
    }

    private void init() {
        _items = new HashMap<String, Panel>();
        _contentPanel = new VerticalPanel();

        ScrollPanel mainPanel = new ScrollPanel();
        mainPanel.setStyleName("SamplesLegendPanel");
        mainPanel.add(_contentPanel);

        initWidget(mainPanel);
    }

    public void add(String projectName, int markerColor) {
        Image marker = new Image(GoogleMap.getMarkerDirectory() + markerColor + ".png");
        marker.setStyleName("SampleMapLegendMarker");

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(marker);
        panel.add(new FulltextPopperUpperHTML(projectName, PROJECT_NAME_MAX_LENGTH));

        _contentPanel.add(panel);
        _items.put(projectName, panel);
    }

    public void highlightItem(String projectName, boolean highlight) {
        Panel panel = _items.get(projectName);
        if (panel == null)
            return;

        if (highlight) {
            unhighlightAll(); // necessary since Google Maps isn't giving us a mouse out event
            panel.addStyleName("SampleMapLegendItemHighlight");
        }
        else
            panel.removeStyleName("SampleMapLegendItemHighlight");
    }

    private void unhighlightAll() {
        for (Panel panel : _items.values())
            panel.removeStyleName("SampleMapLegendItemHighlight");
    }

    public void clear() {
        _contentPanel.clear();
    }
}
