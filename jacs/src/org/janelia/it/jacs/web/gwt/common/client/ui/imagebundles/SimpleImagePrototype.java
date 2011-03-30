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
