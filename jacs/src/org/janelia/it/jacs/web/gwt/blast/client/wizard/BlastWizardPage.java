
package org.janelia.it.jacs.web.gwt.blast.client.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;

/**
 * @author Michael Press
 */
public abstract class BlastWizardPage extends WizardPage {
    private BlastData _data;

    public BlastWizardPage(BlastData data, WizardController controller) {
        super(controller);
        _data = data;
    }

    public BlastWizardPage(BlastData data, WizardController controller, boolean showButtons) {
        super(showButtons, controller);
        _data = data;
    }

    public BlastData getData() {
        return _data;
    }

    public void setData(BlastData blastData) {
        _data = blastData;
    }

    protected void hideThenShow(Panel oldPanel, Panel newPanel) {
        if (GWT.isScript()) // normal mode
            hideThenShowFade(oldPanel, newPanel);
        else               // hosted mode
            hideThenShowImmediate(oldPanel, newPanel);
    }

    protected void hideThenShowFade(final Panel oldPanel, final Panel newPanel) {

        // Last step - realize and fade in the new panel
        final Callback opacityFinished = new Callback() {
            public void execute() {
                newPanel.setVisible(true);
                SafeEffect.fade(newPanel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.2")
                });
            }
        };

        // After fadeout of old panel is complete, set the opacity on the new panel to 1% so it's basically not visible
        Callback fadeOutFinished = new Callback() {
            public void execute() {
                oldPanel.setVisible(false);
                SafeEffect.opacity(newPanel, new EffectOption[]{
                        new EffectOption("to", "0.01")
                        , new EffectOption("duration", "0")
                        , new EffectOption("afterFinish", opacityFinished)
                });
            }
        };

        // Fade out the old panel
        SafeEffect.fade(oldPanel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.1")
                , new EffectOption("afterFinish", fadeOutFinished)
        });

    }

    /**
     * Supports GWT hosted mode, where effects can't be serially chained
     *
     * @param oldPanel - old panel
     * @param newPanel - new panel
     */
    private void hideThenShowImmediate(Panel oldPanel, Panel newPanel) {
        oldPanel.setVisible(false);
        newPanel.setVisible(true);
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click once to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }

}
