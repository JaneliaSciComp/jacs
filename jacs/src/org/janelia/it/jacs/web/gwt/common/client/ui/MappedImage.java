
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Image;

/**
 * Image widget that can be connected with image map.
 */
public class MappedImage extends Image {

    private ImageMap map;

    public MappedImage() {
        super();
    }

    public MappedImage(String url) {
        this();
        setUrl(url);
    }

    public MappedImage(String url, ImageMap map) {
        this(url);
        setMap(map);
    }

    public ImageMap getMap() {
        return map;
    }

    public void setMap(ImageMap map) {
        this.map = map;
        DOM.setElementAttribute(getElement(), "useMap", "#" + map.getName());
    }

}
