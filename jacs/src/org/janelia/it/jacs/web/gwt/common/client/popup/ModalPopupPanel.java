
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;

/**
 * Modal alert popup.  Fades out the rest of the page to imply its severity.
 */
abstract public class ModalPopupPanel extends BasePopupPanel {
    private boolean _doFade = true;

    public ModalPopupPanel(String title, boolean realizeNow) {
        this(title, realizeNow, /*fade*/ true);
    }

    public ModalPopupPanel(String title, boolean realizeNow, boolean fadeBackground) {
        super(title, realizeNow, /*autohide*/ false, /*modal*/ true);
        setDoFade(fadeBackground);
    }

    public void show() {
        if (doFade()) {
            RootPanel root = RootPanel.get(Constants.ROOT_PANEL_NAME);
            if (GWT.isScript()) // normal mode
                showFade(root);
            else                // hosted mode
                showImmediate(root);
        }
        else
            superShow();
    }

    private void showFade(RootPanel root) {
        Callback fadeFinished = new Callback() {
            public void execute() {
                superShow();
            }
        };
        SafeEffect.opacity(root, new EffectOption[]{
                new EffectOption("to", "0.2")
                , new EffectOption("duration", "0.00")
                , new EffectOption("afterFinish", fadeFinished)
        });
    }

    /**
     * Support GWT hosted mode
     */
    private void showImmediate(RootPanel root) {
        SafeEffect.fade(root);
        superShow();
    }

    public void hide() {
        if (doFade()) {
            RootPanel root = RootPanel.get(Constants.ROOT_PANEL_NAME);
            if (GWT.isScript()) // normal mode
                hideFade(root);
            else                // hosted mode
                hideImmediate(root);
        }
        else
            superHide();
    }

    public void hideFade(RootPanel root) {
        Callback fadeFinished = new Callback() {
            public void execute() {
                superHide();
            }
        };
        SafeEffect.opacity(root, new EffectOption[]{
                new EffectOption("to", "1.0")
                , new EffectOption("duration", "0.00")
                , new EffectOption("afterFinish", fadeFinished)
        });
    }

    /**
     * Support GWT hosted mode
     */
    private void hideImmediate(RootPanel root) {
        SafeEffect.appear(root);
        superHide();
    }

    /**
     * can't call this from inside an inner class
     */
    private void superShow() {
        super.show();
    }

    /**
     * can't call this from inside an inner class
     */
    private void superHide() {
        super.hide();
    }

    public boolean doFade() {
        return _doFade;
    }

    public void setDoFade(boolean doFade) {
        _doFade = doFade;
    }
}
