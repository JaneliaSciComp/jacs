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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.MultiSelectVO;

import java.util.List;

/**
 * I am hijacking this class to show a series of check-boxes for multi-selection
 *
 * @author Michael Press
 */
public class MultiSelectParameterRenderer extends ParameterRenderer {
    public MultiSelectParameterRenderer(MultiSelectVO param, String key, Task task) {
        super(param, key, task);
    }

    protected MultiSelectVO getMultiSelectValueObject() {
        return (MultiSelectVO) getValueObject();
    }

    protected void setSelectedItem(String selectedItem) {
        getMultiSelectValueObject().addActualChoice(selectedItem);
        setValueObject(getMultiSelectValueObject()); // updates task
    }

    protected void setUnselectedItem(String unselectedItem) {
        getMultiSelectValueObject().removeActualChoice(unselectedItem);
        setValueObject(getMultiSelectValueObject()); // updates task
    }

    @Override
    protected Widget createPanel() {
        VerticalPanel mainPanel = new VerticalPanel();
        FlexTable table = new FlexTable();
        table.setCellPadding(1);
        table.setCellSpacing(1);
        List<String> actuals = getMultiSelectValueObject().getActualUserChoices();
        int i = 0;
        int j = 0;
        for (String s : getMultiSelectValueObject().getPotentialChoices()) {
            // Two columns of checkboxes; thus, the 2 below.
            CheckBox tmpCheckBox = new CheckBox(s);
            tmpCheckBox.setValue(actuals.contains(s));
            tmpCheckBox.addValueChangeHandler(new MyValueChangeHandler());
            table.setWidget(i / 2, j % 2, tmpCheckBox);
            i++;
            j++;
        }
        mainPanel.add(table);
        return mainPanel;
    }

    public class MyValueChangeHandler implements ValueChangeHandler {
        @Override
        public void onValueChange(ValueChangeEvent valueChangeEvent) {
            Boolean tmpValue = (Boolean) valueChangeEvent.getValue();
            if (tmpValue) {
                setSelectedItem(((CheckBox) valueChangeEvent.getSource()).getText());
            }
            else {
                setUnselectedItem(((CheckBox) valueChangeEvent.getSource()).getText());
            }
        }
    }
}
