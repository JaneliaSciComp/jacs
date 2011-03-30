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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * A fancy pulldown replacement - displays currently-selected text like a regular pulldown, but shows a different
 * icon to indicate that it's a special pulldown, and clicking on the pulldown image or text pops up a regular
 * popup.  Also supports an initial unselected state, in which "-- Select One --" is shown and the backgroudn is
 * yellow; once <code>setText(String)</code> is called, the yellow background is removed.
 *
 * @author Michael Press
 */
public class PulldownPopup extends Composite implements ClickListener {
    private HorizontalPanel _panel;
    private BasePopupPanel _popup;
    private BasePopupPanel _hoverPopup;
    private HTML _textBox;
    private PopupLauncher _launcher;
    private PopupLauncher _hoverLauncher;

    public static final String DEFAULT_TEXT = "-- Select One --";
    private static final String PANEL_STYLE_NAME = "pulldownPopupWidget";
    private static final String TEXT_STYLE_NAME = "pulldownPopupText";
    private static final String TEXT_HOVER_STYLE_NAME = "pulldownPopupTextHover";
    private static final String NORMAL_STYLE_NAME = "pulldownPopupWidgetNormal";
    private static final String WARNING_STYLE_NAME = "pulldownPopupWidgetWarning";

    public PulldownPopup(BasePopupPanel popup) {
        _popup = popup;
        init();
    }

    public PulldownPopup(BasePopupPanel popup, BasePopupPanel hoverPopup) {
        _popup = popup;
        _hoverPopup = hoverPopup;
        init();
    }

    private void init() {
        _textBox = new HTML();
        _textBox.setStyleName(TEXT_STYLE_NAME);
        _textBox.setWordWrap(false);
        _textBox.addClickListener(this);
        _textBox.addMouseListener(new HoverStyleSetter(_textBox, TEXT_STYLE_NAME, TEXT_HOVER_STYLE_NAME, (HoverListener) null));

        Image pulldownImage = ImageBundleFactory.getControlImageBundle().getPulldownImage2().createImage();
        pulldownImage.addClickListener(this);

        // Add a mouse listener to the text box and the image to switch to the hover image on hover
        MouseListener mouseListener = new HoverMouseListener(pulldownImage);
        _textBox.addMouseListener(mouseListener);
        pulldownImage.addMouseListener(mouseListener);

        _panel = new HorizontalPanel();
        _panel.setStyleName(PANEL_STYLE_NAME);
        _panel.add(_textBox);
        _panel.add(pulldownImage);

        // start in default mode
        setDefaultText(DEFAULT_TEXT);

        // Required for Composite
        initWidget(_panel);
    }

    public void setDefaultText() {
        setDefaultText(DEFAULT_TEXT);
    }

    public void setDefaultText(String defaultText) {
        _panel.removeStyleName(NORMAL_STYLE_NAME);
        _panel.addStyleName(WARNING_STYLE_NAME);
        _textBox.setText(defaultText);
    }

    /**
     * If text is not null, sets the text in the pulldown, and restores the style to non-warning.
     */
    public void setText(String text) {
        if (text != null) {
            _panel.removeStyleName(WARNING_STYLE_NAME);
            _panel.addStyleName(NORMAL_STYLE_NAME);
            _textBox.setText(text);
        }
    }

    /**
     * Implementation of ClickListener interface - show the popup
     */
    public void onClick(Widget widget) {
        getLauncher().showPopup(_panel);
    }

    public PopupLauncher getLauncher() {
        if (_launcher == null)
            _launcher = new PulldownPopupLauncher(_popup); // default to below
        return _launcher;
    }

    private class PulldownPopupLauncher extends PopupBelowLauncher {
        private PulldownPopupLauncher(PopupPanel popup) {
            super(popup);
        }

        protected int getPopupLeftPosition(UIObject sender) {
            return super.getPopupLeftPosition(sender) + 5; // shift to right a bit
        }

        protected int getPopupTopPosition(UIObject sender) {
            return super.getPopupTopPosition(sender) + 1;  // shift down a bit
        }
    }

    public PopupLauncher getHoverLauncher() {
        if (_hoverLauncher == null)
            _hoverLauncher = new PulldownPopupHoverLauncher(_hoverPopup, 250);
        return _hoverLauncher;
    }

    public void setLauncher(PopupLauncher launcher) {
        if (launcher != null) {
            _launcher = launcher;
            _launcher.setPopup(_popup);
        }
    }

    public void setHoverPopup(BasePopupPanel hoverPopup) {
        _hoverPopup = hoverPopup;
        if (_hoverLauncher != null)
            _hoverLauncher.setPopup(_hoverPopup);
    }

    public class HoverMouseListener extends HoverImageSetter {
        public HoverMouseListener(Image image) {
            super(image,
                    ImageBundleFactory.getControlImageBundle().getPulldownImage2(),
                    ImageBundleFactory.getControlImageBundle().getPulldownHoverImage2());
        }

        public void onMouseEnter(Widget widget) {
            super.onMouseEnter(widget); // changes the icon to hover version
            if (_hoverPopup != null)
                getHoverLauncher().showPopup(_textBox); // launches the hover popup
        }

        public void onMouseLeave(Widget widget) {
            super.onMouseLeave(widget); // resets the icon
            if (_hoverPopup != null)
                getHoverLauncher().hidePopup();
        }
    }

    /**
     * Need to adjust regular PopupAboveLauncher popup location slightly
     */
    public class PulldownPopupHoverLauncher extends PopupAboveLauncher {
        public PulldownPopupHoverLauncher(BasePopupPanel popup) {
            super(popup);
        }

        public PulldownPopupHoverLauncher(BasePopupPanel popup, int msDelay) {
            super(popup, msDelay);
        }

        protected int getPopupTopPosition(UIObject sender) {
            return super.getPopupTopPosition(sender) - 2;
        }

        protected int getPopupLeftPosition(UIObject sender) {
            return super.getPopupLeftPosition(sender) + 3;
        }
    }
}
