
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
