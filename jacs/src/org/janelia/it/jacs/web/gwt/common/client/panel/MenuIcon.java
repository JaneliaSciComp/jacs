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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 3:17:27 PM
 */
public class MenuIcon extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.MenuIcon");

    private static final String HEADER_STYLENAME = "MenuIconTitle";
    private static final String FOOTER_STYLENAME = "MenuIconFooter";
    private static final String ICON_STYLENAME = "MenuIconImage";
    private static final String DEFAULTICON_WIDTH = "62px";
    private static final String DEFAULTICON_HEIGHT = "50px";

    public static final String PANEL_HOVER = "MenuIconPanelHover";
    public static final String PANEL_SELECTED = "MenuIconPanelSelected";
    public static final String PANEL_UNSELECTED = "MenuIconPanelUnselected";

    private ClickListener _clickListener;
    private MouseListener _mouseListener;
    private List _selectionListeners;
    private Image _iconImage;
    private HTML _headerHtml;
    private HTML _footerHtml;
    private boolean _selected = false;
    private String iconStyleName = ICON_STYLENAME;
    private String iconWidth = DEFAULTICON_WIDTH;
    private String iconHeight = DEFAULTICON_HEIGHT;
    private String headerStyleName = HEADER_STYLENAME;
    private String footerStyleName = FOOTER_STYLENAME;
    private String _debugId;

    /**
     * Displays a light background on non-selected icons when the mouse hovers
     */
    private class IconMouseWrapper extends MouseListenerAdapter {
        public IconMouseWrapper() {
        }

        public void onMouseEnter(Widget widget) {
            if (!_selected) {
                removeStyleName(PANEL_UNSELECTED);
                addStyleName(PANEL_HOVER);
            }
        }

        public void onMouseLeave(Widget widget) {
            if (!_selected) {
                removeStyleName(PANEL_HOVER);
                addStyleName(PANEL_UNSELECTED);
            }
        }
    }

    private class IconClickWrapper implements ClickListener {
        ClickListener _clickListener;

        public IconClickWrapper(ClickListener clickListener) {
            this._clickListener = clickListener;
        }

        public void onClick(Widget w) {
            _selected = !_selected;
            if (_selected) {
                removeStyleName(PANEL_UNSELECTED);
                removeStyleName(PANEL_HOVER);
                addStyleName(PANEL_SELECTED);
                fireSelectEvent();
            }
            else {
                removeStyleName(PANEL_SELECTED);
                addStyleName(PANEL_HOVER);
                fireUnselectEvent();
            }
            if (_clickListener != null) {
                _clickListener.onClick(w);
            }
        }
    }

    public MenuIcon(String iconImageURL, String headerHtml, String tooltipText, String debugId) {
        this(new Image(iconImageURL), headerHtml, tooltipText, debugId);
    }

    public MenuIcon(Image iconImage, String headerHtml, String tooltipText, String debugId) {
        this._iconImage = iconImage;
        _selectionListeners = new ArrayList();
        _debugId = debugId;
        init(headerHtml, tooltipText);
    }

    public void addClickListener(ClickListener clickListener) {
        if (clickListener != null) {
            // handles initial case
            _iconImage.removeClickListener(_clickListener);
        }
        this._clickListener = new IconClickWrapper(clickListener);
        _iconImage.addClickListener(_clickListener);
    }

    public void addMouseListener(MouseListener mouseListener) {
        if (mouseListener != null) {
            // handles initial case
            _iconImage.removeMouseListener(_mouseListener);
        }
        this._mouseListener = new IconMouseWrapper();
        _iconImage.addMouseListener(_mouseListener);
    }

    public String getHeader() {
        return _headerHtml.getText();
    }

    public String getHeaderStyleName() {
        return headerStyleName;
    }

    public void setHeaderStyleName(String headerStyleName) {
        if (headerStyleName == null) {
            this.headerStyleName = HEADER_STYLENAME;
        }
        else {
            this.headerStyleName = headerStyleName;
        }
        if (_headerHtml != null) {
            _headerHtml.setStyleName(this.headerStyleName);
        }
    }

    public String getFooterStyleName() {
        return footerStyleName;
    }

    public void setFooterStyleName(String footerStyleName) {
        if (footerStyleName == null) {
            this.footerStyleName = FOOTER_STYLENAME;
        }
        else {
            this.footerStyleName = footerStyleName;
        }
        if (_footerHtml != null) {
            _footerHtml.setStyleName(this.footerStyleName);
        }
    }

    public void setFooter(String footer) {
        _footerHtml.setHTML(footer);
        _footerHtml.setStyleName(getFooterStyleName());
        if (!_footerHtml.isVisible()) {
            _footerHtml.setVisible(true);
        }
        highlightFooter();
    }

    public String getIconStyleName() {
        return iconStyleName;
    }

    public void setIconStyleName(String iconStyleName) {
        if (iconStyleName == null) {
            this.iconStyleName = ICON_STYLENAME;
        }
        else {
            this.iconStyleName = iconStyleName;
        }
        if (_iconImage != null) {
            _iconImage.setStyleName(this.iconStyleName);
        }
    }

    public String getIconHeight() {
        return iconHeight;
    }

    public String getIconWidth() {
        return iconWidth;
    }

    public void setIconSize(String width, String height) {
        if (width == null) {
            this.iconWidth = DEFAULTICON_WIDTH;
        }
        else {
            this.iconWidth = width;
        }
        if (height == null) {
            this.iconHeight = DEFAULTICON_HEIGHT;
        }
        else {
            this.iconHeight = height;
        }
        if (_iconImage != null) {
            _iconImage.setSize(iconWidth, iconHeight);
        }
    }

    public boolean isSelected() {
        return _selected;
    }

    public void clearFooter() {
        _footerHtml.setHTML("&nbsp;");
        _footerHtml.setVisible(false);
    }

    public void unSelect() {
        if (_selected) {
            _selected = false;
            this.removeStyleName(PANEL_SELECTED);
            this.addStyleName(PANEL_UNSELECTED);
        }
    }

    public void select() {
        if (!_selected) {
            _selected = true;
            this.removeStyleName(PANEL_UNSELECTED);
            this.addStyleName(PANEL_SELECTED);
        }
    }

    public void addSelectionListener(SelectionListener selectionListener) {
        _selectionListeners.add(selectionListener);
    }

    public void removeSelectionListener(SelectionListener selectionListener) {
        _selectionListeners.remove(selectionListener);
    }

    private void fireSelectEvent() {
        String eventSource = getHeader();
        for (Iterator selListenerItr = _selectionListeners.iterator(); selListenerItr.hasNext();) {
            SelectionListener selectionListener = (SelectionListener) selListenerItr.next();
            selectionListener.onSelect(eventSource);
        }
    }

    private void fireUnselectEvent() {
        String eventSource = getHeader();
        for (Iterator selListenerItr = _selectionListeners.iterator(); selListenerItr.hasNext();) {
            SelectionListener selectionListener = (SelectionListener) selListenerItr.next();
            selectionListener.onUnSelect(eventSource);
        }
    }

    private void highlightFooter() {
        new Timer() {
            public void run() {
                SafeEffect.highlight(_footerHtml, new EffectOption[]{new EffectOption("duration", "4.0")});
            }
        }.schedule(300);
    }

    private void init(String headerHtml, String tooltipText) {
        this.addStyleName(PANEL_UNSELECTED);
        addClickListener(null /* custom click listener */);
        addMouseListener(null /* custom mouse listener */);

        _headerHtml = HtmlUtils.getHtml(headerHtml, getHeaderStyleName());
        _footerHtml = HtmlUtils.getHtml("&nbsp;", getFooterStyleName()); // placeholder so highlight colors look right

        _iconImage.addStyleName(getIconStyleName());
        _iconImage.setTitle(tooltipText);
        if (_debugId != null)
            _iconImage.ensureDebugId(_debugId + "MenuIcon");

        add(_headerHtml);
        add(_iconImage);
        add(new CenteredWidgetHorizontalPanel(_footerHtml));
    }

}
