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

package org.janelia.it.jacs.web.gwt.common.client.ui.list;

import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Nov 30, 2006
 * Time: 3:05:06 PM
 * <p/>
 * View port enforces the paging rules for some table.  Acts as a controller for controls
 * external to table itself.
 */
public class ViewPort {
    private SortableTable _sortableTable;
    private List _fullList;
    private int _numVisibleRows;
    private int _offset;
    private HTML _rangeIndicator;


    public ViewPort(SortableTable sortableTable) {
        this(sortableTable, null, null);
    }

    /**
     * Construct with managed table.
     *
     * @param sortableTable
     */
    public ViewPort(SortableTable sortableTable, List fullList) {
        this(sortableTable, fullList, null);
    }

    public ViewPort(SortableTable sortableTable, HTML rangeIndicator) {
        this(sortableTable, null, rangeIndicator);
    }

    /**
     * Construct with managed table and managed range indicator.
     *
     * @param sortableTable  what holds data.
     * @param fullList       list of all data.
     * @param rangeIndicator shows user what is presented.
     */
    public ViewPort(SortableTable sortableTable, List fullList, HTML rangeIndicator) {
        _sortableTable = sortableTable;
        _rangeIndicator = rangeIndicator;
        setItemList(fullList);
    }

    /**
     * Set number-of-rows, that will be visible in the "port" at a time.
     *
     * @param numRows how many to show?
     */
    public void setNumberOfVisibleRows(int numRows) {
        _numVisibleRows = numRows;
        _offset = 0;
        update();
    }

    /**
     * Set the list of all available rows.
     *
     * @param fullList what to setup.
     */
    public void setItemList(List fullList) {
        _fullList = fullList;
        if (_numVisibleRows == 0 && fullList != null)
            _numVisibleRows = fullList.size();
        _offset = 0;
        update();
    }

    public List<String> getItemList() {
        return _fullList;
    }

    /**
     * Add to list of available rows.
     *
     * @param item what to add.
     */
    public void addItem(Object item) {
        if (_fullList == null) {
            _fullList = new ArrayList();
        }
        _fullList.add(item);
        // Required to sort underlying collection, so that when the interval
        // of items to be displayed gets into the table, it will include the
        // item just added IN ITS PROPER PLACE.
        Collections.sort(_fullList);
        update();
    }

    /**
     * Take one item away from the list of available ones.
     *
     * @param item what to remove.
     */
    public void removeItem(Object item) {
        if (_fullList != null) {
            if (item != null && _fullList.contains(item)) {
                _fullList.remove(item);
                update();
            }
        }
    }

    /**
     * Positional setters/offset modifiers.
     */
    public void setPrevious() {
        if (_offset - _numVisibleRows >= 0) {
            _offset -= _numVisibleRows;
        }
        else {
            _offset = 0;
        }
        update();
    }

    public void setNext() {
        if (_offset + _numVisibleRows < _fullList.size()) {
            _offset += _numVisibleRows;
            update();
        }
    }

    public void setFirst() {
        _offset = 0;
        update();
    }

    public void setLast() {
        // Offset becomes the round-off to highest multiple of number-of-visible rows.
        _offset = _fullList.size() - (_fullList.size() % _numVisibleRows);
        update();
    }

    /**
     * For whatever reason, refresh what is seen.  Only update content texts.
     */
    public void update() {
        // Update the contents of the table which shows the current page of data.
        _sortableTable.clear();

        if (_fullList == null || _fullList.size() == 0) {
            _sortableTable.setValue(1, 0, null, HtmlUtils.getHtml("None", "text"));
        }
        else {
            for (int i = 0; (i < _numVisibleRows) && ((i + _offset) < _fullList.size()); i++) {
                Object insertableValue = _fullList.get(i + _offset);
                _sortableTable.setValue(i + 1, 0, insertableValue.toString()); // insert starting at 1
            }
        }
        _sortableTable.refresh();

        // Update the indicator of what rows are shown within the current page of data.
        if (_rangeIndicator == null)
            return;

        int lastValue = 0;
        int size = 0;
        if (_fullList != null) {
            size = _fullList.size();
            if (_offset + _numVisibleRows < size)
                lastValue = _offset + _numVisibleRows;
            else
                lastValue = size;
        }
        _rangeIndicator.setText((_offset + 1) + " - " + lastValue + " of " + size);
    }
}