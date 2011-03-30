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

package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;

import java.io.Serializable;
import java.util.List;

/**
 * @author Michael Press
 */
public interface Paginator extends Serializable, IsSerializable {
    public void next();

    public void previous();

    public void first();

    public void last();

    public boolean hasData();

    public boolean hasNext();

    public boolean hasPrevious();

    public void initRowsPerPage(int rows);

    public void setRowsPerPage(int rows);

    public void modifyRowsPerPage(int rows);

    public int getRowsPerPage();

    public int getCurrentOffset();

    public int getLastRow();

    public int getTotalRowCount();

    public void removeRow(TableRow row);

    public void refresh();

    public void clear();

    public LoadingLabel getPagingInProgressLabel();

    public void setPagingInProgressLabel(LoadingLabel pagingInProgressLabel);

    public List<TableRow> getData(); // Collection<TableRows>

    public void setData(List<TableRow> data); // Collection<TableRows>

    public List createPageRows();
}
