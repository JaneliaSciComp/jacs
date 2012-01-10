
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.Image;

/**
 * @author Michael Press
 */
public class ImagePopup extends BasePopupPanel {
    private Image _image;

    public ImagePopup(Image image) {
        super(null, false); // defer realizing until needed
        _image = image;
    }


    protected void populateContent() {
        add(_image);
    }
}
