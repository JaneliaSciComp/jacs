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
