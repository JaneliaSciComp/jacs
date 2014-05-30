
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.DeckPanel;

/**
 * @author Michael Press
 */
public class FadingDeckPanel extends DeckPanel {
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
