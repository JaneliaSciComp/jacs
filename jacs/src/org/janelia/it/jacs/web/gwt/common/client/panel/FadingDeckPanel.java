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

import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;

/**
 * @author Michael Press
 */
public class FadingDeckPanel extends org.janelia.it.jacs.web.gwt.common.google.user.client.ui.DeckPanel {
    /**
     * Overrides parent implementation to fade out the old panel and fade in the new one
     */
    public void showWidget(int index) {
        checkIndex(index);

        Widget oldWidget = getVisibleWidgetAsWidget();
        Widget newWidget = getWidget(index);

        // Swap widgets
        setVisibleWidget(newWidget);

        // Fade out the old widget and fade in the new one
        if (oldWidget == null)
            newWidget.setVisible(true);
        else
            fadeTabBodies(oldWidget, newWidget);
    }

    protected void fadeTabBodies(final Widget oldWidget, final Widget newWidget) {

        // Step 2 - fade in the new widget
        final Callback showNewWidgetFinished = new Callback() {
            public void execute() {
                newWidget.setVisible(true);
                SafeEffect.fade(newWidget, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.3")
                });
                oldWidget.setVisible(false);
            }
        };

        Callback fadeOutFinished = new Callback() {
            public void execute() {
                // Step 2 - set new widget opacity to 1% so it can be faded in
                SafeEffect.opacity(newWidget, new EffectOption[]{
                        new EffectOption("to", "0.01")
                        , new EffectOption("duration", "0")
                        , new EffectOption("afterFinish", showNewWidgetFinished)
                });
            }
        };

        // Step 1 - partially fade out the old widget
        SafeEffect.fade(oldWidget, new EffectOption[]{
                new EffectOption("to", "0.25")
                , new EffectOption("duration", "0.1")
                , new EffectOption("afterFinish", fadeOutFinished)
        });
    }
}
