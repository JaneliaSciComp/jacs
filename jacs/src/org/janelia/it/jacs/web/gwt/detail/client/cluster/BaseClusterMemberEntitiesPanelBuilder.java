
package org.janelia.it.jacs.web.gwt.detail.client.cluster;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.detail.client.service.cluster.ClusterDetailService;
import org.janelia.it.jacs.web.gwt.detail.client.service.cluster.ClusterDetailServiceAsync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Responsible for controlling the sequence and timing of operations need to build a BSEntityPanel
 *
 * @author Tareq Nabeel
 */
public abstract class BaseClusterMemberEntitiesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.cluster.BaseClusterMemberEntitiesPanelBuilder");
    protected static final int DEFAULT_ROWS_PER_PAGE = 10;
    protected static final String[] DEFAULT_PAGE_LENGTH_OPTIONS = new String[]{"5", "10", "20"};
    private String rowsPerPagePreferenceKey;

    protected static ClusterDetailServiceAsync clusterService =
            (ClusterDetailServiceAsync) GWT.create(ClusterDetailService.class);

    static {
        ((ServiceDefTarget) clusterService).setServiceEntryPoint("clDetail.srv");
    }

    protected ClusterPanel clusterPanel;
    protected SortableTable dataTable;
    protected PagingPanel dataPagingPanel;
    protected boolean _haveData;

    public BaseClusterMemberEntitiesPanelBuilder(ClusterPanel clusterPanel, String rowsPerPagePreferenceKey) {
        this.clusterPanel = clusterPanel;
        _haveData = false;
        this.rowsPerPagePreferenceKey = rowsPerPagePreferenceKey;
    }

    public Panel createDataPanel() {
        return createDataPanel(DEFAULT_ROWS_PER_PAGE, DEFAULT_PAGE_LENGTH_OPTIONS);
    }

    public Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        dataTable = createDataTable();
        createDataPagingPanel(defaultNumVisibleRows, pageLengthOptions);
        return dataPagingPanel;
    }

    abstract public PagedDataRetriever createDataRetriever();

    public String[][] getSortOptions() {
        return null;
    }

    public AsyncCallback createDataCountCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable caught) {
                listener.onFailure(caught);
            }

            public void onSuccess(Object result) {
                listener.onSuccess(result); // Integer
            }
        };
    }

    public AsyncCallback createDataRetrievedCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {

            public void onFailure(Throwable throwable) {
                listener.onFailure(throwable);
            }

            public void onSuccess(Object result) {
                List dataList = (List) result;
                _haveData = true;
                if (dataList == null || dataList.size() == 0) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(formatDataListAsTableRowList(dataList));
                }
            }
        };
    }

    public void populateData() {
        if (!_haveData) {
            dataPagingPanel.first();
        }
    }

    protected void clearDataPanel() {
        _haveData = false;
        dataPagingPanel.clear();
    }

    protected SortableTable createDataTable() {
        return new SortableTable();
    }

    protected void createDataPagingPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        RemotingPaginator dataPaginator = new RemotingPaginator(dataTable,
                createDataRetriever(),
                getSortOptions(),
                rowsPerPagePreferenceKey);
        dataPagingPanel = new RemotePagingPanel(dataTable,
                pageLengthOptions,
                true,
                true,
                dataPaginator,
                false,
                PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                defaultNumVisibleRows,
                rowsPerPagePreferenceKey);
        dataPagingPanel.addAdvancedSortClickListener(new AdvancedSortableRemotePaginatorClickListener
                (dataTable, dataPaginator, (RemotePagingPanel) dataPagingPanel));
        DockPanel footer = dataPagingPanel.getTableFooterPanel();
        Widget exportMenu = createExportMenu();
        if (exportMenu != null) {
            footer.add(exportMenu, DockPanel.EAST);
            footer.setCellHorizontalAlignment(exportMenu, DockPanel.ALIGN_RIGHT);
            dataPagingPanel.showTableFooter();
        }
        dataPagingPanel.setStyleName("clusterMembersDataPanel");
    }

    abstract protected Widget createExportMenu();

    abstract protected TableRow formatDataAsTableRow(Object data);

    protected List formatDataListAsTableRowList(List dataList) {
        List tableRows = new ArrayList();
        for (Iterator dataItr = dataList.iterator(); dataItr.hasNext();) {
            TableRow row = formatDataAsTableRow(dataItr.next());
            tableRows.add(row);
        }
        return tableRows;
    }
}
