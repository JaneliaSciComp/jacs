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

package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * @author Michael Press
 */
abstract public class BaseTableColumn implements TableColumn {
    private String _displayName;
    private boolean _isSortable = true;
    private String _popupText = null;
    private boolean _isVisible = true;
    protected String _style = "tableColumnGeneral";

    public BaseTableColumn(String displayName) {
        _displayName = displayName;
    }

    public BaseTableColumn(String displayName, String popupText) {
        _displayName = displayName;
        _popupText = popupText;
    }

    public BaseTableColumn(String displayName, boolean isSortable) {
        _displayName = displayName;
        _isSortable = isSortable;
    }

    public BaseTableColumn(String displayName, boolean isSortable, boolean isVisible) {
        _displayName = displayName;
        _isSortable = isSortable;
        _isVisible = isVisible;
    }

    public BaseTableColumn(String displayName, String popupText, boolean isSortable, boolean isVisible) {
        _displayName = displayName;
        _popupText = popupText;
        _isSortable = isSortable;
        _isVisible = isVisible;
    }


    public String getDisplayName() {
        return _displayName;
    }

    public boolean isSortable() {
        return _isSortable;
    }

    public void setSortable(boolean sortable) {
        _isSortable = sortable;
    }

    /**
     * Default implementation - returns null, so nothing will pop up.
     */
    public String getPopupText() {
        return _popupText;
    }

    public void setPopupText(String popupText) {
        _popupText = popupText;
    }

    public boolean isVisible() {
        return _isVisible;
    }

    public void setVisible(boolean visible) {
        _isVisible = visible;
    }

    public String getTextStyleName() {
        return _style;
    }

    public void setStyle(String style) {
        _style = style;
    }

    public boolean hasImageContent() {
        return false;
    }

    public void setDisplayName(String displayName) {
        _displayName = displayName;
    }
}
