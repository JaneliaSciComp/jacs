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

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * @author Michael Press
 */
public class TextParameterRenderer extends ParameterRenderer {
    private TextBox _textBox;

    public TextParameterRenderer(TextParameterVO param, String key, Task task) {
        super(param, key, task);
        addKeyboardListener(new MyKeyboardListener());
    }

    protected TextParameterVO getTextParam() {
        return (TextParameterVO) getValueObject();
    }

    protected Widget createPanel() {
        _textBox = new TextBox();
        _textBox.setVisibleLength(10);
        _textBox.setText(getTextParam().getTextValue());

        return _textBox;
    }

    private class MyKeyboardListener implements KeyboardListener {
        public void onKeyDown(Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyPress(Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyUp(Widget sender, char keyCode, int modifiers) {
            try {
                String tmpValue = _textBox.getText();
                // todo Parameter validation probably needed here
                ((TextParameterVO) _param).setTextValue(tmpValue);
                setValueObject(_param);
            }
            catch (ParameterException e) {
                System.out.println("Error setting the text value.");
            }
        }
    }
}
