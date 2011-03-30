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

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HideShowActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ToggleActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Creates a portlet-style panel with title bar and colored background.
 *
 * @author mpress
 */

public class TitledPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel");

    public static final String DEFAULT_TITLE_STYLE_NAME = "portletTitle";
    public static final String DEFAULT_EMPTY_TITLE_STYLE_NAME = "portletNoTitle";
    public static final String DEFAULT_CONTENT_STYLE_NAME = "portlet";
    public static final String DEFAULT_CONTENT_WRAPPER_STYLE_NAME = "portletContentsWrapper";
    public static final String DEFAULT_TOP_ROUND_STYLE_NAME = "portletTopRounding";
    //public static final String DEFAULT_EMPTY_TITLE_TOP_ROUND_STYLE_NAME = "portletNoTitleTopRounding";
    public static final String DEFAULT_BOTTOM_ROUND_STYLE_NAME = "portletBottomRounding";

    private String _title = null;
    private Panel _titlePanel = null;
    private DockPanel _titleHtmlPanel = null;  //TODO: can get this dynamically?
    private VerticalPanel _contentPanel = new VerticalPanel();
    private VerticalPanel _contentsPanelWrapper = null;
    private RoundedPanel2 _bottomRoundPanel = null;
    private boolean _showActionLinks = true;
    private boolean _realized = false;
    private HideShowActionLink _actionLink = null;

    public TitledPanel() {
        this(null);
    }

    public TitledPanel(String title) {
        super();
        init(title, true, true);
    }

    public TitledPanel(String title, boolean showActionLinks) {
        super();
        init(title, showActionLinks, true);
    }

    public TitledPanel(String title, boolean showActionLinks, boolean realizeNow) {
        super();
        init(title, showActionLinks, realizeNow);
    }

    protected void init(String title, boolean showActionLinks, boolean realizeNow) {
        _title = title;
        setSpacing(0);
        setStyleName("titledPanel"); //TODO: allow user override
        setShowActionLinks(showActionLinks);
        if (realizeNow)
            realize();
    }

    private boolean hasTitle() {
        return _title != null && !_title.equals("");
    }

    /**
     * Creates and shows the panel (if not already shown).  Must be called if realizeNow set to false in constructor,
     * otherwise not necessary to call.
     */
    public void realize() {
        if (!_realized) {
            _realized = true;
            _logger.debug("realizing TiledPanel - " + getTitle());

            // Create the panel contents
            if (hasTitle())
                super.add(createTitlePanel());
            super.add(createContentPanel());
            popuplateContentPanel();

            // Let the subclass run any post-realize code
            onRealize();
        }
    }

    /**
     * Hook for subclass to populate content after the panel is realized (such as retrieving list contents).  This
     * allows the panel to show up very quickly onscreen and give the user feedback that data's being retrieved.  This
     * default implementation is empty.
     */
    protected void onRealize() {
    }

    /**
     * Hook for subclasses to populate the Content panel
     */
    protected void popuplateContentPanel() {
    }

    /**
     * Adds Widgets (to the content panel)
     */
    public void add(Widget w) {
        getContentPanel().add(w);
    }

    /**
     * Creates title panel
     *
     * @return title panel
     */
    //TODO: dynamically take action links
    protected Panel createTitlePanel() {
        // Create a panel for the title html content
        _titleHtmlPanel = new DockPanel();
        _titleHtmlPanel.setHeight("1%");
        setTitleStyleName(getTitleStyleName());

        // Add title
        Widget titleHtml = new HTML(_title);
        _titleHtmlPanel.add(titleHtml, DockPanel.WEST);
        _titleHtmlPanel.setCellHorizontalAlignment(titleHtml, HasHorizontalAlignment.ALIGN_LEFT);

        // Add right-aligned actions
        if (getShowActionLinks()) {
            _actionLink = new HideShowActionLink(_contentPanel, null, ToggleActionLink.PRIMARY_STATE);
            _titleHtmlPanel.add(_actionLink, DockPanel.EAST);
            _titleHtmlPanel.setCellHorizontalAlignment(_actionLink, HasHorizontalAlignment.ALIGN_RIGHT);
        }

        // Put the title content into a top-rounded panel
        //TODO: figure out way to get CSS color dynamically
        RoundedPanel2 topRounding = new RoundedPanel2(_titleHtmlPanel, RoundedPanel2.TOP, getRoundingBorder());
        topRounding.setCornerStyleName(getTopRoundingBorderStyleName());
        setTitlePanel(topRounding);
        return _titlePanel;
    }

    protected String getTitleStyleName() {
        return DEFAULT_TITLE_STYLE_NAME;
    }

    /**
     * Same action as the hide link - fades out the content and shrinks the panel
     */
    public void hideContent() {
        _actionLink.toggleToSecondaryState(); // hide
    }

    /**
     * Same action as the show link - fades out the content and shrinks the panel
     */
    public void showContent() {
        _actionLink.toggleToPrimaryState(); // show
    }

    protected void setTitleStyleName(String styleName) {
        _titleHtmlPanel.setStyleName(styleName);
    }

    protected Panel createContentPanel() {
        _contentPanel.setSpacing(0);
        _contentPanel.setWidth("100%");
        //_contentPanel.setStyleName(DEFAULT_CONTENT_STYLE_NAME);

        // use spacers instead of CSS padding because IE adds spacing to each element in the vertical panel
        _contentsPanelWrapper = new VerticalPanel();
        //_contentsPanelWrapper.setStyleName(DEFAULT_CONTENT_WRAPPER_STYLE_NAME);
        _contentsPanelWrapper.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _contentsPanelWrapper.add(_contentPanel);
        _contentsPanelWrapper.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        setContentStyleName(DEFAULT_CONTENT_STYLE_NAME);  // covers both _contentsPanel and _contentsPanelWrapper

        //TODO: find better way to deal with no title
        int rounding = RoundedPanel2.ALL;
        if (hasTitle())
            rounding = RoundedPanel2.BOTTOM; /* title will round the top */
        _bottomRoundPanel = new RoundedPanel2(_contentsPanelWrapper, rounding, getRoundingBorder());
        _bottomRoundPanel.setCornerStyleName(DEFAULT_BOTTOM_ROUND_STYLE_NAME);

        return _bottomRoundPanel;
    }

    protected String getRoundingBorder() {
        return "#5C9EBF";
    }

    protected String getTopRoundingBorderStyleName() {
        return DEFAULT_TOP_ROUND_STYLE_NAME;
    }

    public String getContentStyleName() {
        return _contentPanel.getStyleName();
    }

    public void setContentStyleName(String styleName) {
        _contentPanel.setStyleName(styleName);
        _contentsPanelWrapper.setStyleName(styleName + "ContentsWrapper");  // hack
    }

    public void setBottomRoundCornerStyleName(String styleName) {
        _bottomRoundPanel.setCornerStyleName(styleName);
    }

    public String getBottomRoundStyleName() {
        return _bottomRoundPanel.getStyleName();
    }

    protected void setTitlePanel(Panel panel) {
        _titlePanel = panel;
    }

    public Panel getTitlePanel() {
        return _titlePanel;
    }

    public Panel getContentPanel() {
        return _contentPanel;
    }

    protected void setContentPanel(VerticalPanel panel) {
        _contentPanel = panel;
    }

    public boolean getShowActionLinks() {
        return _showActionLinks;
    }

    public void setShowActionLinks(boolean showActionLinks) {
        _showActionLinks = showActionLinks;
    }

    public boolean isRealized() {
        return _realized;
    }
}
