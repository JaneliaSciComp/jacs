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

package org.janelia.it.jacs.web.gwt.common.client.ui.table;

import org.janelia.it.jacs.model.common.SortArgument;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: May 25, 2007
 * Time: 4:29:33 PM
 */
public class SortableColumn extends SortArgument {

    private int columnPosition;
    private String columnHeading;

    public SortableColumn() {
        super();
        columnPosition = -1;
    }

    public SortableColumn(int columnPosition, String columnHeading, int sortDirection) {
        super();
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
        setSortDirection(sortDirection);
    }

    public SortableColumn(int columnPosition, String columnHeading, String columnSortName) {
        super(columnSortName);
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
    }

    public SortableColumn(int columnPosition, String columnHeading, String columnSortName, int sortDirection) {
        super(columnSortName, sortDirection);
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
    }

    public String getColumnHeading() {
        return columnHeading;
    }

    public void setColumnHeading(String columnHeading) {
        this.columnHeading = columnHeading;
    }

    public int getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(int columnPosition) {
        this.columnPosition = columnPosition;
    }

}
