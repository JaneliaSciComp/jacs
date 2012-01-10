
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.*;

/**
 * @author mpress
 *         <p/>
 *         Toggles images for a GWT Image when the mouse enters/exists.
 */
public class HoverImageSetter implements EventListener, MouseListener {
    Image _image = null;
    ClickListener _clickListener = null;
    private AbstractImagePrototype _imagePrototype;
    private AbstractImagePrototype _hoverImagePrototype;

    /**
     * @param image               - the image that has been added to a Panel
     * @param imagePrototype      - specification of the base image state
     * @param hoverImagePrototype - specification of the image's hover state
     */
    public HoverImageSetter(Image image, AbstractImagePrototype imagePrototype, AbstractImagePrototype hoverImagePrototype) {
        this(image, imagePrototype, hoverImagePrototype, null);
    }

    /**
     * @param image               - the image that has been added to a Panel
     * @param imagePrototype      - specification of the base image state
     * @param hoverImagePrototype - specification of the image's hover state
     */
    public HoverImageSetter(Image image, AbstractImagePrototype imagePrototype, AbstractImagePrototype hoverImagePrototype,
                            ClickListener clickListener) {
        _image = image;
        _imagePrototype = imagePrototype;
        _hoverImagePrototype = hoverImagePrototype;
        _clickListener = clickListener;
    }

    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONMOUSEOVER)
            onMouseEnter(_image);
        else if (DOM.eventGetType(event) == Event.ONMOUSEOUT)
            onMouseLeave(_image);
        else if (DOM.eventGetType(event) == Event.ONMOUSEUP)
            onMouseLeave(_image);
        else if (DOM.eventGetType(event) == Event.ONMOUSEUP)
            onMouseUp(_image, -1, -1);
        else if (DOM.eventGetType(event) == Event.ONMOUSEDOWN)
            onMouseDown(_image, -1, -1);
    }

    public void onMouseEnter(Widget image) {
        _hoverImagePrototype.applyTo(_image);
    }

    public void onMouseLeave(Widget sender) {
        _imagePrototype.applyTo(_image);
    }

    public void onMouseDown(Widget sender, int x, int y) {
        onMouseLeave(sender);           // restore style to normal
        if (_clickListener != null) // process click event
            _clickListener.onClick(sender);
    }

    public void onMouseMove(Widget sender, int x, int y) {
    }

    public void onMouseUp(Widget sender, int x, int y) {
    }
}
