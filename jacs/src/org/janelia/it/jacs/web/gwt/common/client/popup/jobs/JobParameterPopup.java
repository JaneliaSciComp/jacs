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

package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 19, 2007
 * Time: 2:21:00 PM
 */
public class JobParameterPopup extends BasePopupPanel {
    private String _jobName;
    private String _dateString;
    private Map<String, String> _paramMap;

    public JobParameterPopup(String jobName, String dateString, Map<String, String> parameterMap, boolean realizeNow) {
        super("Job Parameters", realizeNow);
        _paramMap = parameterMap;
        _jobName = jobName;
        _dateString = dateString;
    }

    protected void populateContent() {
        Grid grid = new Grid(1, 2);  // blank first row for spacing
        addGridRow(grid, "Job Name", _jobName);
        addGridRow(grid, "Submit Date", _dateString);
        Iterator iter = _paramMap.keySet().iterator();
        ArrayList<String> keyList = new ArrayList<String>();
        while (iter.hasNext()) {
            keyList.add((String) iter.next());
        }
        Collections.sort(keyList);
        for (String aKeyList : keyList) {
            String value = (_paramMap.get(aKeyList)).trim();
            addGridRow(grid, aKeyList, value);
        }
        add(grid);
    }

    private void addGridRow(Grid grid, String key, String value) {
        int row = grid.getRowCount();
        grid.resizeRows(row + 1);
        if (!(key.equals("") && value.equals(""))) {
            grid.setWidget(row, 0, HtmlUtils.getHtml(key + ":", "prompt"));
            grid.setWidget(row, 1, HtmlUtils.getHtml(value, "text"));
        }
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[1];
        tmpButtons[0] = new RoundedButton("OK", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    public String getJobName() {
        return _jobName;
    }

}
