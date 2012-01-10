
package org.janelia.it.jacs.web.gwt.common.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Implements IE specific way to create "map" html element
 */
public class ImageMapImplIE6 extends ImageMapImpl {

    public Element createImageMapElement() {
        return DOM.createElement("<map id='' name=''></map>");
    }

    public Element createImageMapElement(String name) {
        return DOM.createElement("<map " +
                "id=" + "'" + (name == null ? "" : name) + "'" + " " +
                "name=" + "'" + (name == null ? "" : name) + "'" + " " +
                ">" +
                "</map>");
    }

}
