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

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.ui.Span;

/**
 * @author Michael Press
 */
public abstract class RadioParameterRenderer extends ParameterRenderer {
    private RadioButton[] _buttons;
    private String buttonGroupName = "";
    private MyClickListener clickListener = new MyClickListener();

    public RadioParameterRenderer(ParameterVO param, String buttonGroupName, Task task) {
        super(param, buttonGroupName, task, false);
        this.buttonGroupName = buttonGroupName;
        realize();
    }

    protected Widget createPanel() {
        HorizontalPanel panel = new HorizontalPanel();

        String[] labels = getButtonLabels();
        _buttons = new RadioButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            Span label = new Span(labels[i] + "&nbsp;", "text");
            _buttons[i] = new RadioButton(getButtonGroupName(), label.toString(), true);
            _buttons[i].setStyleName("text");
            _buttons[i].addClickListener(clickListener);
            panel.add(_buttons[i]);
            if (labels[i].equalsIgnoreCase(getValueObject().getStringValue())) {
                _buttons[i].setValue(true);
            }
        }

        return panel;
    }

    protected String getButtonGroupName() {
        return buttonGroupName + "ButtonGroup";
    }

    protected abstract String[] getButtonLabels();

    protected abstract void setSelectedParameterValue(String selectedParameterValue);

    private class MyClickListener implements ClickListener {
        public void onClick(Widget widget) {
            Element widgetElement = widget.getElement();
            // Find what was clicked and set the parameter accordingly.
            // The assumption is that the value of the button is an acceptable setting for the parameter
            for (RadioButton button : _buttons) {
                if (button.getElement().equals(widgetElement)) {
                    setSelectedParameterValue(button.getText());
                    return;
                }
            }
        }
    }
}
