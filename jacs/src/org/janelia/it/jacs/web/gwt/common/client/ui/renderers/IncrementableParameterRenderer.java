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

package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
abstract public class IncrementableParameterRenderer extends ParameterRenderer {
    protected IncrementableParameterRenderer(ParameterVO param, String key, Task task) {
        super(param, key, task);
    }

    protected Panel getIncrementButtons(IncrementListener incrCallback, IncrementListener decrCallback) {
        VerticalPanel buttons = new VerticalPanel();

        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();

        Image up = imageBundle.getArrowUpButtonImage().createImage();
        up.addMouseListener(new HoverImageSetter(up, imageBundle.getArrowUpButtonImage(), imageBundle.getArrowUpButtonHoverImage(),
                new IncrementClickListenerConverterListener(incrCallback)));

        Image down = imageBundle.getArrowDownButtonImage().createImage();
        down.addMouseListener(new HoverImageSetter(down, imageBundle.getArrowDownButtonImage(), imageBundle.getArrowDownButtonHoverImage(),
                new IncrementClickListenerConverterListener(decrCallback)));

        buttons.add(up);
        buttons.add(down);

        return buttons;
    }

    // Converts a ClickListener's onClick(Widget) callback to an IncrementListener's onClick() callback
    public class IncrementClickListenerConverterListener implements ClickListener {
        private IncrementListener _incrListener;

        public IncrementClickListenerConverterListener(IncrementListener incrListener) {
            _incrListener = incrListener;
        }

        public void onClick(Widget widget) {
            if (_incrListener != null)
                _incrListener.onClick();
        }
    }

}
