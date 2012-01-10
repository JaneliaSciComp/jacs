
package org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles;

import com.google.gwt.core.client.GWT;

/**
 * @author Michael Press
 */
public class ImageBundleFactory {
    private static AnimatedImageBundle _animatedImageBundle = (AnimatedImageBundle) GWT.create(AnimatedImageBundle.class);
    private static CategoryImageBundle _categoryImageBundle = (CategoryImageBundle) GWT.create(CategoryImageBundle.class);
    private static ControlImageBundle _controlImageBundle = (ControlImageBundle) GWT.create(ControlImageBundle.class);

    public static AnimatedImageBundle getAnimatedImageBundle() {
        return _animatedImageBundle;
    }

    public static CategoryImageBundle getCategoryImageBundle() {
        return _categoryImageBundle;
    }

    public static ControlImageBundle getControlImageBundle() {
        return _controlImageBundle;
    }
}
