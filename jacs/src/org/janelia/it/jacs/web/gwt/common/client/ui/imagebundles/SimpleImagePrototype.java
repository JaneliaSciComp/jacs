
package org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

/**
 * Simple implementation of AbstractImagePrototype
 *
 * @author Michael Press
 */
public class SimpleImagePrototype extends AbstractImagePrototype {
    private String _url;
    private Image _image;

    public SimpleImagePrototype(String url) {
        _url = url;
    }

    public void applyTo(Image image) {
        image.setUrl(_url);
    }

    public Image createImage() {
        _image = new Image(_url);
        return _image;
    }

    /**
     * Not tested
     */
    public String getHTML() {
        if (_image != null)
            return _image.toString();
        return null;
    }
}
