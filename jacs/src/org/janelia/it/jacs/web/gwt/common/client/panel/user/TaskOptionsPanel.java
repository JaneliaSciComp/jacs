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

package org.janelia.it.jacs.web.gwt.common.client.panel.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.renderers.ParameterRenderer;
import org.janelia.it.jacs.web.gwt.common.client.ui.renderers.ParameterRendererFactory;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 18, 2008
 * Time: 12:47:46 PM
 */
public class TaskOptionsPanel extends VerticalPanel {
    // For regular options
    private FlexTable _grid;
    private FlexTable _grid2;
    // For advanced options
    private FlexTable _advancedGrid1;
    private FlexTable _advancedGrid2;
    private SecondaryTitledBox advancedOptionsBox = new SecondaryTitledBox("Advanced Options", true, false);

    public TaskOptionsPanel() {
        super();
        init();
    }

    private void init() {
        // 2 grids for params
        SecondaryTitledBox basicOptionsBox = new SecondaryTitledBox("Basic Options", false);
        basicOptionsBox.setWidth("100%");
        this.add(basicOptionsBox);

        HorizontalPanel basicPanel = new HorizontalPanel();
        basicPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        basicPanel.setWidth("100%");
        _grid = new FlexTable();
        _grid2 = new FlexTable();
        basicPanel.add(_grid);
        basicPanel.add(HtmlUtils.getHtml("&nbsp;", "BlastOptionsColumnSpacer"));
        basicPanel.add(_grid2);
        basicOptionsBox.add(basicPanel);

        this.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        HorizontalPanel advancedPanel = new HorizontalPanel();
        advancedPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        advancedPanel.setWidth("100%");

        advancedOptionsBox.setWidth("100%");

        _advancedGrid1 = new FlexTable();
        _advancedGrid2 = new FlexTable();
        advancedPanel.add(_advancedGrid1);
        advancedPanel.add(HtmlUtils.getHtml("&nbsp;", "BlastOptionsColumnSpacer"));
        advancedPanel.add(_advancedGrid2);
        advancedOptionsBox.add(advancedPanel);
        this.add(advancedOptionsBox);
    }

    /**
     * Adds the params to the grids - half to grid1 (column 1), half to grid2 (column 2)
     *
     * @param tmpTask task in question
     */
    public void displayParams(Task tmpTask) {
        //_logger.debug("Calling display params");
        // Hide the params panel until it's updated
        _grid.setVisible(false);
        _grid2.setVisible(false);
        _advancedGrid1.setVisible(false);
        _advancedGrid2.setVisible(false);
        clearParams();
        boolean showAdvanced = true;
        // Add in the new params
        try {
            int basicCounter = 0, advancedCounter = 0;
            Set<String> keySet = tmpTask.getParameterKeySet();
            Iterator<String> sortIter = keySet.iterator();
            ArrayList<String> keyList = new ArrayList<String>();
            while (sortIter.hasNext()) {
                keyList.add(sortIter.next());
            }
            Collections.sort(keyList);
            for (Object aKeyList : keyList) {
                FlexTable tmpTable;
                String tmpKey = (String) aKeyList;
                //_logger.debug("Checking key: " + tmpKey);
                ParameterVO tmpParam = tmpTask.getParameterVO(tmpKey);
                // if null or subj or query, ignore the param
                // ignore believeDefline because the merge/sort xml parsing depends on accession in the <Iteration_query-def> element
                if (!displayParameter(tmpKey, tmpParam)) {
                    continue;
                }
                // if required param, add to the basic options
                if (tmpTask.isParameterRequired(tmpKey)) {
                    if (basicCounter % 2 == 0) {
                        tmpTable = _grid;
                    }
                    else {
                        tmpTable = _grid2;
                    }
                    basicCounter++;
                }
                // else, add to the advanced options
                else {
                    if (advancedCounter % 2 == 0) {
                        tmpTable = _advancedGrid1;
                    }
                    else {
                        tmpTable = _advancedGrid2;
                    }
                    advancedCounter++;
                }
                addGridRow(tmpTable, tmpKey, ParameterRendererFactory.getParameterRenderer(tmpKey, tmpParam, tmpTask));

            }
            if (0 == advancedCounter) {
                showAdvanced = false;
            }
        }
        catch (Throwable e) {
            // Adding the try catch should always fire fof the fade in at least...
            //_logger.error("got exception looping through required params: "+e.getMessage(),e);
        }

        // Fade in the new params
        fadeIn(_grid);
        fadeIn(_grid2);
        advancedOptionsBox.setVisible(showAdvanced);
        if (showAdvanced) {
            fadeIn(_advancedGrid1);
            fadeIn(_advancedGrid2);
        }
    }

    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return (null != tmpParam && null != parameterKeyName && !"".equals(parameterKeyName));
    }

    private void addGridRow(FlexTable grid, String paramName, ParameterRenderer renderer) {
        try {
            int row = grid.getRowCount();

            // Create prompt and tooltip (launches after 1/2 sec delay)
            HTML prompt = HtmlUtils.getHtml(paramName + ":", "prompt");
//            ParameterInfoPopup popup = new ParameterInfoPopup(paramName, param, false);
//            PopupLauncher launcher = new PopupLauncher(popup, 250); /* 1/4 sec delay */
//            prompt.addMouseListener(new HoverStyleSetter(prompt, "prompt", "promptHover", (HoverListener) launcher));

            grid.setWidget(row, 0, prompt);
            //grid.getCellFormatter().setStyleName(row, 0, "gridCell");
//            grid.getCellFormatter().addStyleName(row, 0, "BlastOptionPrompt");

            //grid.getCellFormatter().setStyleName(row, 1, "gridCell");
            grid.setWidget(row, 1, renderer);
        }
        catch (Throwable e) {
            //_logger.error("Error in addGridRow on param= "+paramName+"\n"+e.getMessage(),e);
        }
    }

    private void clearParams() {
        try {
            clearTable(_grid);
            clearTable(_grid2);
            clearTable(_advancedGrid1);
            clearTable(_advancedGrid2);
        }
        catch (Throwable e) {
            //_logger.error("error clearing the params.",e);
        }
    }

    private void clearTable(FlexTable table) {
        for (int i = table.getRowCount() - 1; i >= 0; i--) {
            table.removeRow(i);
        }
    }

    private void fadeIn(Widget widget) {
        if (GWT.isScript()) // normal mode
            fadeInTimer(widget);
        else               // hosted mode
            fadeInImmediate(widget);
    }

    private void fadeInTimer(final Widget widget) {
        Callback opacityFinished = new Callback() {
            public void execute() {
                // Show the grid (at 1%) and fade back in
                widget.setVisible(true);
                SafeEffect.fade(widget, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "1.0")
                });
            }
        };

        // Set the opacity of the GWT widgets to 1%
        SafeEffect.opacity(widget, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0")
                , new EffectOption("afterFinish", opacityFinished)
        });
    }

    public void fadeInImmediate(Widget widget) {
        widget.setVisible(true);
    }

}
