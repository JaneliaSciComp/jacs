
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
