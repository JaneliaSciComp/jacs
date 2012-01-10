
package org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import org.janelia.it.jacs.web.gwt.common.client.Constants;

/**
 * ImageBundle-style singleton for animated GIFs.  ImageBundle doesn't work for animated gifs, but this class
 * mimics ImageBundle-style image retrieval for code consistency.  The images are located in webroot/images, unlike
 * real ImageBundle images.
 *
 * @author Michael Press
 */
public class AnimatedImageBundle {
    public AbstractImagePrototype getBusyAnimatedIcon() {
        return new SimpleImagePrototype(Constants.IMAGES_DIRECTORY + "/busy.gif");
    }

    //public AbstractImagePrototype busyAnimated2()
    //{
    //    return new SimpleImagePrototype(Constants.IMAGES_DIRECTORY + "/common/icon_animated_busy.gif");
    //}
}