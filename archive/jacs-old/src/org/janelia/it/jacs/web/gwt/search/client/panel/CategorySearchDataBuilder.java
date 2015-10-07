
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.MultiValueSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.SearchEntityListener;
import org.janelia.it.jacs.web.gwt.search.client.service.SearchService;
import org.janelia.it.jacs.web.gwt.search.client.service.SearchServiceAsync;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
abstract public class CategorySearchDataBuilder {

    private static Logger logger =
            Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder");

    // constants for column positions
    private static int INDEXED_DOC_ID_COLUMN = 0;
    private static int INTERNAL_ACCESSION_COLUMN = 1;
    private static int EXTERNAL_ID_COLUMN = 2;
    //private static int HEADLINE_COLUMN = 3;
    // constants for column headings
    private static String INDEXED_DOC_ID_HEADING = "Indexed Document ID";
    private static String INTERNAL_ACCESSION_HEADING = "Accession";
    private static String EXTERNAL_ID_HEADING = "External ID";
    protected static String HEADLINE_HEADING = "Search Match"; // default

    protected final boolean SORTABLE = true;
    protected final boolean UNSORTABLE = false;

    protected static int HEADLINE_COLUMN_LENGTH = 35;
    protected static int DEFLINE_COLUMN_LENGTH = 50;

    protected CategorySummarySearchPanel parentPanel;

    protected static SearchServiceAsync _searchService = (SearchServiceAsync) GWT.create(SearchService.class);

    static {
        ((ServiceDefTarget) _searchService).setServiceEntryPoint("search.oas");
    }

    protected String searchId;
    protected String searchQuery;
    protected PagingPanel dataPagingPanel;
    protected SortableTable dataTable;
    protected boolean _haveData;
    private SearchEntityListener _entityListener;
    private SelectionListener _selectionListener;

    protected CategorySearchDataBuilder(String searchId, String searchQuery) {
        this.searchId = searchId;
        this.searchQuery = searchQuery;
        dataTable = createDataTable();
    }

    public String getSearchId() {
        return searchId;
    }

    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void addDataRetrievedCallback(DataRetrievedListener listener) {
        if (dataPagingPanel != null) {
            ((RemotingPaginator) dataPagingPanel.getPaginator()).addDataRetrievedCallback(listener);
        }
        else {
            throw new NullPointerException("Data paging panel has not been created yet");
        }
    }

    public void setEntityListener(SearchEntityListener entityListener) {
        _entityListener = entityListener;
    }

    public void setSelectionListener(final MultiValueSelectionListener selectionListener) {
        if (_selectionListener != null) {
            // remove the previous listener
            dataTable.removeSelectionListener(_selectionListener);
        }
        if (selectionListener == null) {
            _selectionListener = null;
        }
        else {
            _selectionListener = new SelectionListener() {
                private MultiValueSelectionListener _toNotify = selectionListener;

                public void onSelect(String row) {
                    String[] values = getSelectedValues(row);
                    if (logger.isDebugEnabled())
                        logger.debug("Selected accession=" + values[0] + ", name=" + values[1]);
                    dataTable.clearHover();  // remove the highlight since the table will not get any mouseout event
                    _toNotify.onSelect(values);
                }

                public void onUnSelect(String row) {
                    String[] values = getSelectedValues(row);
                    dataTable.clearHover();  // remove the highlight since the table will not get any mouseout event
                    _toNotify.onUnSelect(values);
                }
            };
            dataTable.addSelectionListener(_selectionListener);
        }
    }

    public void selectItem(int itemIndex) {
        dataTable.selectRow(itemIndex);
    }

    protected class SearchResultDataRetriever implements PagedDataRetriever {

        private String searchCategory;

        public SearchResultDataRetriever(String searchCategory) {
            this.searchCategory = searchCategory;
        }

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            List<String> categories = new ArrayList<String>();
            categories.add(searchCategory);
            _searchService.getNumSearchResultsForSearchId(searchId, categories,
                    createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            List<String> categories = new ArrayList<String>();
            categories.add(searchCategory);
            _searchService.getPagedSearchResultsForSearchId(searchId, categories, startIndex, numRows, sortArgs,
                    createDataListCallback(listener));
        }

    }

    protected class CategoryResultDataRetriever implements PagedDataRetriever {

        public CategoryResultDataRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _searchService.getNumCategoryResults(searchId, getPanelSearchCategory(),
                    createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            if (logger.isDebugEnabled()) {
                logger.debug("CategoryResultDataRetriever: sortArgs=");
                if (sortArgs == null) {
                    logger.debug("no sortArgs");
                }
                else {
                    for (int i = 0; i < sortArgs.length; i++) {
                        logger.debug("sortArg " + i + "=" + sortArgs[i].getSortArgumentName() + " " + sortArgs[i].getSortDirection());
                    }
                }
            }
            _searchService.getPagedCategoryResults(searchId, getPanelSearchCategory(), startIndex, numRows, sortArgs,
                    createDataListCallback(listener));
        }

    }

    protected Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        addDataTableHeadings(dataTable);
        dataTable.setHighlightSelect(true);

        RemotingPaginator dataPaginator = new RemotingPaginator(dataTable, createDataRetriever(), getSortOptions(),
                getPanelSearchCategory() + "RowNumPref");
        dataPagingPanel = new RemotePagingPanel(dataTable,
                pageLengthOptions,
                true,
                true,
                dataPaginator,
                false,
                PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                defaultNumVisibleRows,
                "CategorySearch" + getPanelSearchCategory());
        dataPagingPanel.addAdvancedSortClickListener(new AdvancedSortableRemotePaginatorClickListener
                (dataTable, dataPaginator, (RemotePagingPanel) dataPagingPanel));
        dataPagingPanel.ensureDebugId("CategorySearchDataTable");

        SimplePanel dataTableWrapper = new SimplePanel();
        dataTableWrapper.setStyleName("SearchDataPanel");
        dataTableWrapper.add(dataPagingPanel);

        VerticalPanel dataPanel = new VerticalPanel();
        dataPanel.add(HtmlUtils.getHtml(createDataPanelTitle(), "SearchDataPanelTitle"));
        dataPanel.add(dataTableWrapper);

        return dataPanel;
    }

    abstract protected String createDataPanelTitle();

    protected AsyncCallback createDataCountCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable caught) {
                logger.error("Exception in DataCountCallback: " + caught.getMessage(), caught);
                listener.onFailure(caught);
            }

            // On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                listener.onSuccess(result); // Integer
            }
        };
    }

    public void setParentPanel(CategorySummarySearchPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    public Panel createDataPanel() {
        return createDataPanel(5, new String[]{"3", "5", "10"});
    }

    public void populateDataPanel() {
        if (!_haveData) {
            dataPagingPanel.first();
        }
    }

    protected SortableTable createDataTable() {
        return new SortableTable();
    }

    protected void addDataTableHeadings(SortableTable table) {
        table.addColumn(new NumericColumn(INDEXED_DOC_ID_HEADING, null, false, false));
        table.addColumn(new TextColumn(INTERNAL_ACCESSION_HEADING, true));
        table.addColumn(new TextColumn(EXTERNAL_ID_HEADING));
        table.addColumn(new TextColumn(HEADLINE_HEADING));
    }

    protected PagedDataRetriever createDataRetriever() {
        return new CategorySearchDataBuilder.SearchResultDataRetriever(getPanelSearchCategory());
    }

    abstract protected String getPanelSearchCategory();

    protected String[][] getSortOptions() {
        return new String[][]{
                {"accession", INTERNAL_ACCESSION_HEADING},
                {"docname", EXTERNAL_ID_HEADING},
                {"headline", HEADLINE_HEADING}
        };
    }

    protected String[] getSelectedValues(String rowStr) {
        throw new UnsupportedOperationException("getSelectedAccession not implemented for " + this);
    }

    protected AsyncCallback createDataListCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {

            public void onFailure(Throwable throwable) {
                logger.error("Exeption in DataListCallback: " + throwable.getMessage(), throwable);
                listener.onFailure(throwable);
            }

            public void onSuccess(Object result) {
                List resultList = (List) result;
                _haveData = true;
                if (resultList == null || resultList.size() == 0) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(formatDataListAsTableRowList(resultList));
                }
            }
        };
    }

    protected TableRow formatDataAsTableRow(Object data) {
        SearchHit searchHit = (SearchHit) data;
        TableRow row = new TableRow();
        row.setValue(INDEXED_DOC_ID_COLUMN, new TableCell(searchHit.getDocumentId()));
        row.setValue(INTERNAL_ACCESSION_COLUMN, new TableCell(searchHit.getAccession(),
                getAccessionLink(searchHit.getAccession(), searchHit.getAccession())));
        row.setValue(EXTERNAL_ID_COLUMN, new TableCell(searchHit.getDocumentName()));
/*
        row.setValue(HEADLINE_COLUMN,new TableCell(searchHit.getHeadline(),
                new FulltextPopperUpperHTML(searchHit.getHeadline(),
                        HEADLINE_COLUMN_LENGTH)));
*/
        return row;
    }

    protected List<TableRow> formatDataListAsTableRowList(List dataList) {
        List<TableRow> tableRows = new ArrayList<TableRow>();
        for (Object aDataList : dataList) {
            TableRow row = formatDataAsTableRow(aDataList);
            tableRows.add(row);
        }
        return tableRows;
    }

    protected Widget getAccessionLink(final String accession) {
        return getAccessionLink(accession, accession);
    }

    protected Widget getAccessionLink(final String accession, final String entityKey) {
        return new Link(accession, new ClickListener() {
            public void onClick(Widget widget) {
                if (dataTable != null) {
                    dataTable.clearHover(); // Have to remove the highlight style since the table will never get a mouse out event
                }
                if (_entityListener != null) {
                    _entityListener.onEntitySelected(entityKey, getPanelSearchCategory(), null);
                }
            }
        });
    }

    public void getSearchResultCharts() {
        _searchService.getSearchResultChartsForSearchId(searchId, getPanelSearchCategory(), new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                // only log the error for now
                logger.error(throwable);
                parentPanel.populateResultCharts(null);
            }

            public void onSuccess(Object o) {
                List<ImageModel> resultChartImages = (List<ImageModel>) o;
                parentPanel.populateResultCharts(resultChartImages);
            }
        });
    }
}
