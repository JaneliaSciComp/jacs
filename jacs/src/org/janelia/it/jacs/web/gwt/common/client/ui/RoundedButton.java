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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Press
 */
public class RoundedButton extends Composite {
    HTML _panel; // clickable
    RoundedPanel2 _roundedButton;
    private HoverStyleSetter _styleManager;// internal for changing styles on hover and detecting clicks

    public static final String BUTTON_STYLE = "buttonPanel";
    public static final String ROUNDING_STYLE = "buttonRounding";
    public static final String BUTTON_HOVER_STYLE = "buttonPanelHover";
    public static final String ROUNDING_HOVER_STYLE = "buttonRoundingHover";
    private static final String VISIBLE_OPACITY = "1.0";
    private static final String FADED_OPACITY = "0.25";
    public static final String BORDER_COLOR = "cornflowerblue"; // temp until can be set via CSS
    boolean _hoverAdded = false;
    Set<RoundedButton> linkedButtonSet = new HashSet<RoundedButton>();

    public RoundedButton(String text) {
        super();
        init(text, null);
    }

    public RoundedButton(String text, ClickListener clickListener) {
        super();
        init(text, clickListener);
    }

    protected void init(String text, ClickListener clickListener) {
        // Create a panel for the button text, and surround with a rounded panel
        _panel = new HTML(text);
        _panel.setStyleName(getLabelStyleName());
        _roundedButton = createRoundedPanel(_panel);
        _roundedButton.setCornerStyleName(ROUNDING_STYLE);

        // Delegate to HoverStyleSetter to manage the styles on hover and notify the clickListener on click
        _styleManager = new HoverStyleSetter(_panel, getLabelStyleName(), getLabelHoverStyleName(),
                _roundedButton, ROUNDING_STYLE, ROUNDING_HOVER_STYLE, clickListener);

        // Set the style and hover effect by default
        setEnabled(true);

        // Composites are required to notify GWT with the Widget that's being wrapped
        initWidget(_roundedButton);

        // Last, automatically set the debug ID (optimized out for production)
        ensureDebugId(text + "Button");
    }

    /**
     * Hook for subclasses to change the rounded panel
     */
    protected RoundedPanel2 createRoundedPanel(Widget contents) {
        return new RoundedPanel2(contents, RoundedPanel2.ALL, BORDER_COLOR);
    }

    /**
     * Allows subclass to override hover style name
     */
    protected String getLabelStyleName() {
        return BUTTON_STYLE;
    }

    /**
     * Allows subclass to override hover style name
     */
    protected String getLabelHoverStyleName() {
        return BUTTON_HOVER_STYLE;
    }

    public String getText() {
        return _panel.getText();
    }

    /**
     * Synonymous with setClickListener() since we support only 1 click listener
     */
    public void addClickListener(ClickListener clickListener) {
        setClickListener(clickListener);
    }

    /**
     * Sets the clickListener on the button
     */
    public void setClickListener(ClickListener clickListener) {
        _styleManager.setClickListener(clickListener);
        setHoverEffects();
    }

    public void setText(String text) {
        _panel.setText(text);
    }

    private void setHoverEffects() {
        if (!_hoverAdded) {
            _panel.addMouseListener(_styleManager);
            _hoverAdded = true;
        }
    }

    private void unsetHoverEffects() {
        if (_hoverAdded) {
            _panel.removeMouseListener(_styleManager);
            _hoverAdded = false;
        }
    }

    /**
     * For consistency with GWT style
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        }
        else {
            disable();
        }
        for (Object aLinkedButtonSet : linkedButtonSet) {
            RoundedButton rb = (RoundedButton) aLinkedButtonSet;
            rb.setEnabled(enabled);
        }
    }

    private void disable() {
        unsetHoverEffects();
        setOpacity(_roundedButton, FADED_OPACITY);
        if (BrowserDetector.isIE())  // Also have to explicitly set opacity on the inner panel
            setOpacity(_panel, FADED_OPACITY);
    }

    private void enable() {
        setHoverEffects();
        setOpacity(_roundedButton, VISIBLE_OPACITY);
        if (BrowserDetector.isIE())  // Also have to explicitly set opacity on the inner panel
            setOpacity(_panel, VISIBLE_OPACITY);
    }

    /**
     * @param fraction 0.0 thru 1.0
     */
    private void setOpacity(Widget panel, String fraction) {
        SafeEffect.opacity(panel, new EffectOption[]{
                new EffectOption("to", fraction)
                , new EffectOption("duration", "0")
        });
    }

    /**
     * Programmatic push of the button
     */
    public void execute() {
        _styleManager.getClickListener().onClick(this);
    }

    public ClickListener getRemoteClickListener() {
        return new ClickListener() {
            public void onClick(Widget widget) {
                _styleManager.getClickListener().onClick(widget);
            }
        };
    }

    public void addLinkedButton(RoundedButton linkedButton) {
        linkedButtonSet.add(linkedButton);
    }

    public void removeLinkedButton(RoundedButton linkedButton) {
        linkedButtonSet.remove(linkedButton);
    }

    public void onEnsureDebugId(String id) {
        // Override normal UIObject setting main Element's ID, and instead set it on the inner panel with the button text
        _panel.ensureDebugId(id);
    }
}
