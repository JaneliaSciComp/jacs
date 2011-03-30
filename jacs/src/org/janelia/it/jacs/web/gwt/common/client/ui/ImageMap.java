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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.impl.ImageMapImpl;

/**
 * Represents "map" html element
 */
public class ImageMap extends ComplexPanel {

    private static ImageMapImpl impl;

    static {
        impl = (ImageMapImpl) GWT.create(ImageMapImpl.class);
    }

    public ImageMap() {
        super();
    }

    public ImageMap(String name) {
        this();
        setName(name);
    }

    public String getName() {
        return DOM.getElementAttribute(getElement(), "name");
    }

    public void setName(String name) {
        setElement(impl.createImageMapElement());
        DOM.setElementAttribute(getElement(), "name", name == null ? "" : name);
        DOM.setElementAttribute(getElement(), "id", name == null ? "" : name);
    }

    public ImageMapArea addArea(String shape, String coords) {
        ImageMapArea area = new ImageMapArea(shape, coords);
        super.add(area, getElement());
        return area;
    }

    public ImageMapArea addArea(String shape, String coords,
                                String targetHystoryToken) {
        ImageMapArea area = new ImageMapArea(shape, coords, targetHystoryToken);
        super.add(area, getElement());
        return area;
    }

    public boolean removeArea(ImageMapArea area) {
        return super.remove(area);
    }

    public void add(Widget w) {
        if (!(w instanceof ImageMapArea)) {
            throw new IllegalArgumentException("Unsupported argument type");
        }

        super.add(w);
    }

    public boolean remove(Widget w) {
        if (!(w instanceof ImageMapArea)) {
            throw new IllegalArgumentException("Unsupported argument type");
        }

        return super.remove(w);
    }

}
