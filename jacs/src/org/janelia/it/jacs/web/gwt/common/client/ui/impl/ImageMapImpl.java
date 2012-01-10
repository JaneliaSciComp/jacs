
package org.janelia.it.jacs.web.gwt.common.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Implements generic way to create "map" html element
 */
public class ImageMapImpl {

    public Element createImageMapElement() {
        Element elem = DOM.createElement("map");
        DOM.setElementAttribute(elem, "id", "");
        DOM.setElementAttribute(elem, "name", "");
        return elem;
    }

    public Element createImageMapElement(String name) {
        Element elem = DOM.createElement("map");
        DOM.setElementAttribute(elem, "id", name == null ? "" : name);
        DOM.setElementAttribute(elem, "name", name == null ? "" : name);
        return elem;
    }

}
