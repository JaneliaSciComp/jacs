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

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * @author Michael Press
 */
abstract public class ParameterRenderer extends Renderer {
    ParameterVO _param;
    String _key;
    Task _task;

    public ParameterRenderer(ParameterVO param, String key, Task task) {
        this(param, key, task, true);
    }

    public ParameterRenderer(ParameterVO param, String key, Task task, boolean realizeNow) {
        _param = param;
        _key = key;
        _task = task;
        if (realizeNow) {
            realize();
        }
    }

    public ParameterVO getValueObject() {
        return _param;
    }

    public void setValueObject(ParameterVO param) {
        _param = param;
//        Window.alert("Setting "+_key + " to " + param.getStringValue());
        _task.setParameter(_key, param.getStringValue());
    }

    abstract protected Widget createPanel();

    protected void realize() {
        add(createPanel());
    }
}
