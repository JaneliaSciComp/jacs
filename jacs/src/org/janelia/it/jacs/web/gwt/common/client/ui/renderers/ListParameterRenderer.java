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

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;

/**
 * @author Michael Press
 */
public abstract class ListParameterRenderer extends ParameterRenderer {
    private ListBox _listBox;
    private MyChangeListener changeListener = new MyChangeListener();

    public ListParameterRenderer(ParameterVO param, String key, Task task) {
        super(param, key, task, false);
        realize();
    }

    protected Widget createPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        _listBox = new ListBox();
        String[] items = getListItems();
        for (int i = 0; items != null && i < items.length; i++) {
            _listBox.addItem(items[i]);
        }

        _listBox.addChangeListener(changeListener);
        panel.add(_listBox);
        return panel;
    }

    protected abstract String[] getListItems();

    protected abstract void setSelectedItems(String[] selectedItems);

    /**
     * Convenience method to get String values from a List
     *
     * @param values the items which will be rendered in the list
     * @return a string array of the items
     */
    protected String[] getListItems(List<String> values) {
        String[] vals = new String[values.size()];
        for (int i = 0; i < values.size(); i++)
            vals[i] = (String) values.get(i);

        return vals;
    }

    protected void setSelectedItem(int targetListItem) {
        _listBox.setSelectedIndex(targetListItem);
    }

    public class MyChangeListener implements ChangeListener {
        public void onChange(Widget widget) {
            String[] tmpItems = new String[0];
            tmpItems[0] = _listBox.getItemText(_listBox.getSelectedIndex());
            setSelectedItems(tmpItems);
        }
    }
}
