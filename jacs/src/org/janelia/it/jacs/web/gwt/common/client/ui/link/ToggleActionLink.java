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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Press
 */
abstract public class ToggleActionLink extends ActionLink {
    private String _primaryLabel;
    private String _secondaryLabel;
    private Image _primaryImage;
    private Image _secondaryImage;
    private List _primaryStateListeners;
    private List _secondaryStateListeners;
    private Side _secondaryImageSide = ActionLink.Side.LEFT;  // images on the left by default

    private boolean _isPrimaryState = true;

    public static final int PRIMARY_STATE = 1;
    public static final int SECONDARY_STATE = 2;

    public ToggleActionLink(String primaryLabel, String secondaryLabel) {
        this(primaryLabel, secondaryLabel, /*image1*/ null, /*image2*/ null);
    }

    public ToggleActionLink(String primaryLabel, String secondaryLabel, Image primaryImage, Image secondaryImage) {
        super(primaryLabel);
        addClickListener(new ToggleActionLinkClickListener());
        setPrimaryImage(primaryImage);
        setSecondaryImage(secondaryImage);
        _primaryLabel = primaryLabel;
        _secondaryLabel = secondaryLabel;
    }

    public void toggleToPrimaryState() {
        _isPrimaryState = true;

        setText(_primaryLabel);
        if (_primaryImage != null) {
            super.setImage(null, ActionLink.Side.RIGHT);
            super.setImage(_primaryImage, ActionLink.Side.LEFT);
        }

        notifyStateListeners();
    }

    public void toggleToSecondaryState() {
        _isPrimaryState = false;

        setText(_secondaryLabel);
        if (_secondaryImage != null) {
            super.setImage(null, ActionLink.Side.LEFT);
            super.setImage(_secondaryImage, _secondaryImageSide);
        }

        notifyStateListeners();
    }

    protected void notifyStateListeners() {
        if (isPrimaryState() && _primaryStateListeners != null)
            for (int i = 0; i < _primaryStateListeners.size(); i++)
                ((StateChangeListener) _primaryStateListeners.get(i)).onStateChange();
        else if (!isPrimaryState() && _secondaryStateListeners != null)
            for (int i = 0; i < _secondaryStateListeners.size(); i++)
                ((StateChangeListener) _secondaryStateListeners.get(i)).onStateChange();
    }

    public void addPrimaryStateChangeListener(StateChangeListener listener) {
        if (_primaryStateListeners == null)
            _primaryStateListeners = new ArrayList();
        _primaryStateListeners.add(listener);
    }

    public void addSecondaryStateChangeListener(StateChangeListener listener) {
        if (_secondaryStateListeners == null)
            _secondaryStateListeners = new ArrayList();
        _secondaryStateListeners.add(listener);
    }

    public boolean isPrimaryState() {
        return _isPrimaryState;
    }

    public void toggleState() {
        if (isPrimaryState())
            toggleToSecondaryState();
        else
            toggleToPrimaryState();
    }

    public class ToggleActionLinkClickListener implements ClickListener {
        public void onClick(Widget sender) {
            toggleState();
        }
    }

    public String getPrimaryLabel() {
        return _primaryLabel;
    }

    public String getSecondaryLabel() {
        return _secondaryLabel;
    }

    /**
     * Synonymous with setPrimaryImage (overrides parent implementation to avoid cofusion)
     *
     * @param image
     */
    public void setImage(Image image) {
        setPrimaryImage(image);
    }

    public void setPrimaryImage(Image image) {
        _primaryImage = image;
        if (isPrimaryState())
            super.setImage(_primaryImage);
    }

    public void setSecondaryImage(Image image) {
        setSecondaryImage(image, ActionLink.Side.LEFT);
    }

    public void setSecondaryImage(Image image, ActionLink.Side side) {
        _secondaryImage = image;
        _secondaryImageSide = side;
        if (!isPrimaryState())
            super.setImage(_primaryImage, _secondaryImageSide);
    }
}
