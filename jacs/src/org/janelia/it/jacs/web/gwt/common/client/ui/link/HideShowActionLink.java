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

package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class HideShowActionLink extends ToggleActionLink {
    private Panel _panel;
    private Panel _wrapper;
    public static final boolean USE_EFFECTS = true;
    public static final boolean IMMEDIATE = false;
    private String widgetWidth;

    /**
     * @param panel   Panel to fade
     * @param wrapper Outer panel to shrink (if null then panel is shrunk as well as faded)
     * @param state   intial state, one of PRIMARY_STATE or SECONDARY_STATE
     */
    public HideShowActionLink(Panel panel, Panel wrapper, int state) {
        this(panel, wrapper, state, "hide", "show", null, null);
    }

    /**
     * @param panel          Panel to fade
     * @param wrapper        Outer panel to shrink (if null then panel is shrunk as well as faded)
     * @param state          intial state, one of PRIMARY_STATE or SECONDARY_STATE
     * @param primaryLabel   label to show in first state
     * @param secondaryLabel label to show in secondary state
     * @param primaryImage   image to use in first state; overrides default show image
     * @param secondaryImage image to use in secondary state; overrides default hide image
     */
    public HideShowActionLink(Panel panel, Panel wrapper, int state, String primaryLabel, String secondaryLabel,
                              Image primaryImage, Image secondaryImage) {
        super(primaryLabel, secondaryLabel, primaryImage, secondaryImage);
        setShowBrackets(false);
        _panel = panel;
        _wrapper = wrapper;
        init(state);
    }

    private void init(int state) {
        createImages();
        if (state == SECONDARY_STATE) {
            hidePanelImmediately();
            super.toggleToSecondaryState(); // change the action link label
        }
    }

    private void createImages() {
        setPrimaryImage(ImageBundleFactory.getControlImageBundle().getHidePanelImage().createImage());
        setSecondaryImage(ImageBundleFactory.getControlImageBundle().getShowPanelImage().createImage());
    }

    /**
     * Hides the panel contents using scale and fade effect
     */
    public void toggleToSecondaryState() {
        hidePanel();
        super.toggleToSecondaryState(); // change the action link label
    }

    /**
     * Shows the panel contents using scale and fade effect
     */
    public void toggleToPrimaryState() {
        showPanel();
        super.toggleToPrimaryState(); // change the action link label
    }

    /**
     * Hides the panel immediately (no effect)
     */
    private void showPanel() {
        final Callback slideFinished = new Callback() {
            public void execute() {
                SafeEffect.fade(_panel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", 0.5)
                });
            }
        };

        _panel.setVisible(true);
        Widget widget = _wrapper != null ? _wrapper : _panel;
        widget.setWidth(widgetWidth);

        SafeEffect.slideDown(widget, new EffectOption[]{
                new EffectOption("restoreAfterFinish", "true")
                , new EffectOption("to", "1.0")
                , new EffectOption("duration", 0.1)
                , new EffectOption("afterFinish", slideFinished)
        });
    }

    private void hidePanel() {
        final Callback slideFinished = new Callback() {
            public void execute() {
                _panel.setVisible(false);
            }
        };

        Callback fadeOutFinished = new Callback() {
            public void execute() {
                Widget widget = _wrapper != null ? _wrapper : _panel;
                SafeEffect.slideUp(widget, new EffectOption[]{
                        new EffectOption("duration", 0.05)
                        , new EffectOption("afterFinish", slideFinished)
                });
            }
        };

        SafeEffect.fade(_panel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", 0.8)
                , new EffectOption("afterFinish", fadeOutFinished)
        });
    }

    private void hidePanelImmediately() {
        _panel.setVisible(false);
        SafeEffect.fade(_panel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", 0.0)
        });
        Widget widget = _wrapper != null ? _wrapper : _panel;
        SafeEffect.slideUp(widget, new EffectOption[]{
                new EffectOption("duration", 0.0)
        });
    }
}
