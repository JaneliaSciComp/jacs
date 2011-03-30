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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Dec 1, 2006
 * Time: 4:28:44 PM
 * <p/>
 * Mutually exclusive states rather like radio buttons, but instead have a visual appearance
 * of links and non-link text.
 */
public class LinkGroup implements SelectionListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroup");

    private static final String UNSELECTED_STYLE = "smallTextLink";
    private static final String SELECTED_STYLE = "infoText";

    private HTML[] _linkWidgets;
    private LinkGroupSelectionModel _model;
    private String[] _linkStrings;

    /**
     * Construct with set of values to choose among, plust the initially chosen
     * value.
     *
     * @param linkLabels what to click
     * @param model      of selected value, which may be shared/centralized.
     */
    public LinkGroup(String[] linkLabels, LinkGroupSelectionModel model) {
        _model = model;
        _linkStrings = linkLabels;
        init();
    }

    /**
     * Hand back the managed items, for inclusion in GUIs.
     *
     * @return array of things created here.
     */
    public HTML[] getGroupMembers() {
        return _linkWidgets;
    }

    /**
     * When this is clicked, it will cause the highlighted/clickable links to change.  Assume this
     * notification is from the model, and do not update the model.  Changing the model's
     * selected value from here could cause an infinite loop.
     *
     * @param value whatever was clicked.
     */
    public void onSelect(String value) {
        update();
    }

    // Unused
    public void onUnSelect(String value) {

    }

    /**
     * Force selection via method call, rather than user action.
     *
     * @param selected what to become.
     */
    public void setSelectedValue(String selected) {
        _model.setSelectedValue(selected);
    }

    /**
     * Setup stage.
     */
    private void init() {
        makeWidgets();
        update();
    }

    /**
     * Makes all required HTML things.
     */
    private void makeWidgets() {
        MutexCoordinator coordinator = new MutexCoordinator();
        _linkWidgets = new HTML[_linkStrings.length];
        for (int i = 0; i < _linkStrings.length; i++) {
            _linkWidgets[i] = new HTML(_linkStrings[i]);
            _linkWidgets[i].addClickListener(coordinator);
        }
        update();
    }

    /**
     * Call this for any change to the button state.
     */
    private void update() {
        boolean valueInList = false;
        for (int i = 0; i < _linkStrings.length; i++) {
            if (_linkStrings[i].equals(_model.getSelectedValue())) {
                valueInList = true;
                _linkWidgets[i].setStyleName(SELECTED_STYLE);
            }
            else {
                _linkWidgets[i].setStyleName(UNSELECTED_STYLE);
            }
        }
        if (!valueInList)
            logMissingSelectedValue();

    }

    /**
     * Convenience method.
     */
    private void logMissingSelectedValue() {
        _logger.error(
                "Value " +
                        _model.getSelectedValue() +
                        " does not appear in the list of LinkGroup possible values, and yet it is selected"
        );

    }

    /**
     * This class will listen to clicks against any of the included texts, and
     * will appropriately ensure that the one clicked is no longer clickable,
     * and the others now are.
     */
    class MutexCoordinator implements ClickListener {
        public void onClick(Widget w) {
            boolean found = false;
            for (int i = 0; i < _linkWidgets.length && (!found); i++) {
                if (_linkWidgets[i] == w) {
                    // Test: was the clicked widget the same one that was already selected?
                    //    If so, do nothing, but if not, proceed.
                    if (!_linkStrings[i].equals(_model.getSelectedValue())) {
                        // NOW that the user clicked the widget, its corresponding
                        // value will become the selected one.
                        _model.setSelectedValue(_linkStrings[i]);

                        update();

                        found = true;
                    }
                }
            }
        }

    }
}
