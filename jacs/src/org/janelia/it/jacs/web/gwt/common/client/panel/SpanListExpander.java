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

package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.HTMLPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.Span;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;

/**
 * This class creates a panel that's expandable.  It's holds the content of the panel
 * as a SpanList
 *
 * @author Tareq Nabeel
 */
public class SpanListExpander extends BaseContentExpander {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.SpanListExpander");

    private SpanList longSpanList;
    private SpanList shortSpanList;

    /**
     * Create a SpanListExpander given a SpanList and minimum pcData size before panel
     * switches to expanded panel
     *
     * @param spanList
     * @param minSize
     */
    public SpanListExpander(SpanList spanList, int minSize) {
        super(minSize, HTMLPanel.createUniqueId());
        this.longSpanList = spanList;
        createPanels();
    }

    /**
     *
     */
    protected void createShrinkContentAndSetExpandable() {
        SpanList[] spanListSplits = longSpanList.split(getMinSize());
        shortSpanList = spanListSplits[0];
        this.setExpandable(spanListSplits, longSpanList);
    }

    /**
     * Template method that sets the expanded content
     */
    protected void setExpandedContent() {
        setExpandedContent(longSpanList.toString());
    }

    /**
     * Template method that sets the shrink content
     */
    protected void setShrinkContent() {
        setShrinkContent(shortSpanList.toString());
    }

    /**
     * Template method adds the action links to the two long and short span lists
     */
    protected void addActionLinks() {
        addDotsToSpan(shortSpanList);
        addActionLinkToSpanList(shortSpanList);
        //addDotsToSpan(longSpanList);
        addActionLinkToSpanList(longSpanList);
    }

    /**
     * @param spanList
     */
    private void addDotsToSpan(SpanList spanList) {
        // Add "..." to last span of both long and short list
        Span lastSpan = spanList.getLastSpan();
        lastSpan.setHtml(lastSpan.getHtml() + "<br/>...");
    }

    /**
     * @param spanList
     */
    private void addActionLinkToSpanList(SpanList spanList) {
        Span actionSpan = new Span();
        actionSpan.setId(getActionLinkId());
        spanList.addSpan(actionSpan);
    }

    /**
     * @param spanListSplits
     * @param originalSpanList
     */
    private void setExpandable(SpanList[] spanListSplits, SpanList originalSpanList) {
        boolean splitOccurred = (spanListSplits[0].size() + spanListSplits[1].size()) > originalSpanList.size();
        setExpandable(splitOccurred);
    }
}
