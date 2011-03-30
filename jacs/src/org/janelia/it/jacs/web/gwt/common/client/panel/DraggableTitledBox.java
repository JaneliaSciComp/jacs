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

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * Extension of TitledBox that is draggable by the title area; this DraggableTitledBox must be a child (i.e. added to)
 * a DraggableAreaPanel.
 *
 * @author Michael Press
 */
public class DraggableTitledBox extends TitledBox {
    private Image _dragImage;
    private HorizontalPanel _contentsPanel;

    /**
     * @param title          text to appear in the title area
     * @param dragController the dragController that will (duh) control dragging this titled box
     */
    public DraggableTitledBox(String title, DragController dragController) {
        super(title);
        setDragController(dragController);
    }

    public void setTitle(String title) {
        setTitleLabel(new Label(title));
        getTitleLabel().setStyleName("titledBoxLabelDraggable");

        _contentsPanel = new HorizontalPanel();
        _contentsPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _contentsPanel.add(getDragImage());
        _contentsPanel.add(getTitleLabel());

        SimplePanel wrapper = new SimplePanel();
        wrapper.setStyleName("titledBoxLabel");
        wrapper.add(_contentsPanel);

        setTitle(wrapper);
    }

    private Widget getDragImage() {
        _dragImage = ImageBundleFactory.getControlImageBundle().getDraggableImage().createImage();
        _dragImage.setStyleName("draggableImage");

        return _dragImage;
    }

    /**
     * Informs this title box of the drag controller that will control its drag behavior.  This method
     * informs the drag controller of the proxies that can be used to drag the entire titled box - the
     * title area and the drag image on the title area.
     */
    private void setDragController(DragController dragController) {
        dragController.makeDraggable(this, getTitleLabel());
        dragController.makeDraggable(this, _dragImage);
    }
}
