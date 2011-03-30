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
 * Specifies that a TableColumn will have image content;  this is important because the SortableTable detects
 * Widgets with empty HTML output and inserts blank content so the table cell borders don't get messed up on
 * some browsers.  Images also have empty HTML output, so use of this TableColumn notifies SortableTable not
 * to replace its content with generic blank content.
 *
 * @author Michael Press
 */
public class ImageColumn extends BaseTableColumn {
    public ImageColumn(String displayName) {
        this(displayName, /*isVisible*/ true);
    }

    public ImageColumn(String displayName, boolean isVisible) {
        this(displayName, /*isSortable*/ false, isVisible);
    }

    public ImageColumn(String displayName, boolean isSortable, boolean isVisible) {
        super(displayName, isSortable, isVisible);
    }

    public boolean hasImageContent() {
        return true;
    }
}