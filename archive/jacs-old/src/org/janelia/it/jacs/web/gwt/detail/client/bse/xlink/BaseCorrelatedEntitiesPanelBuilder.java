
package org.janelia.it.jacs.web.gwt.detail.client.bse.xlink;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for populating the entity details table
 */
abstract public class BaseCorrelatedEntitiesPanelBuilder {
    private BSEntityPanel parentPanel;
    private PagingPanel dataPagingPanel;
    private boolean _haveData;
    private String rowsPerPagePreferenceKey;

    protected static CRefEntityServiceAsync cRefService = (CRefEntityServiceAsync) GWT.create(CRefEntityService.class);

    static {
        ((ServiceDefTarget) cRefService).setServiceEntryPoint("cref.srv");
    }

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     *
     * @param parentPanel              parent base sequence entity panel
     * @param rowsPerPagePreferenceKey preference key which defines user rows-per-page
     */
    protected BaseCorrelatedEntitiesPanelBuilder(BSEntityPanel parentPanel, String rowsPerPagePreferenceKey) {
        this.parentPanel = parentPanel;
        this._haveData = false;
        this.rowsPerPagePreferenceKey = rowsPerPagePreferenceKey;
    }

    abstract public Widget createContent();

    public Widget createDataPanel() {
        SortableTable dataTable = createDataTable();
        RemotingPaginator dataPaginator = new RemotingPaginator(dataTable, createDataRetriever(), getSortOptions(), rowsPerPagePreferenceKey);
        dataPagingPanel = new RemotePagingPanel(dataTable,
                new String[]{"5", "10", "20"},
                true,
                true,
                dataPaginator,
                false,
                0,
                5,
                "BaseCorrelatedEntities");
        return dataPagingPanel;
    }

    public void populateData() {
        if (!_haveData) {
            dataPagingPanel.first();
        }
    }

    protected String getEntityAccessionNo() {
        return parentPanel.getAcc();
    }

    protected DetailPanel getMainDetailPanel() {
        return parentPanel.getParentPanel();
    }

    protected BSEntityPanel getParentPanel() {
        return parentPanel;
    }

    protected Widget createContent(String title, boolean showLinks) {
        TitledBox crossReferencedEntitiesBox = new TitledBox(title, showLinks);
        crossReferencedEntitiesBox.setStyleName("detailCrossLinkedEntitiesBox");
        createDataPanel();
        crossReferencedEntitiesBox.add(dataPagingPanel);
        return crossReferencedEntitiesBox;
    }

    protected AsyncCallback createDataCountCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable caught) {
                listener.onFailure(caught);
            }

            // On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                listener.onSuccess(result); // Integer
            }
        };
    }

    protected AsyncCallback createDataListCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {

            public void onFailure(Throwable throwable) {
                listener.onFailure(throwable);
            }

            public void onSuccess(Object result) {
                List orfList = (List) result;
                _haveData = true;
                if (orfList == null || orfList.size() == 0) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(formatDataListAsTableRowList(orfList));
                }
            }
        };
    }

    abstract protected PagedDataRetriever createDataRetriever();

    abstract protected SortableTable createDataTable();

    abstract protected String[][] getSortOptions();

    protected Widget createDNAOrientationColumnValue(int direction) {
        if (direction == -1)
            return ImageBundleFactory.getControlImageBundle().getArrowReverseImage().createImage();
        else
            return ImageBundleFactory.getControlImageBundle().getArrowForwardImage().createImage();
    }

    protected Widget createEntityLinkColumnValue(final String entityAcc) {
        return new Link(entityAcc, new ClickListener() {

            ActionLink prevBackLink = getParentPanel().getPreviousPanelLink();
            String pageTokenWhenCreated = History.getToken();
            // for now we do not handle history events ("back" and "forward")
            // however the clicks on the action links should add the corresponding history tokens
            BackActionLink backToCurrentDetailLink = new BackActionLink(
                    "back to " + getParentPanel().getDetailTypeLabel() + " Details",
                    new ClickListener() {
                        public void onClick(Widget widget) {
                            getMainDetailPanel().rebuildPanel(getParentPanel().getAcc(),
                                    pageTokenWhenCreated,
                                    prevBackLink);
                        }
                    }, pageTokenWhenCreated);

            public void onClick(Widget widget) {
                String currentPageTokenWhenClicked = History.getToken();
                String nextPageToken = null;
                if (currentPageTokenWhenClicked != null && currentPageTokenWhenClicked.length() > 0) {
                    nextPageToken = currentPageTokenWhenClicked.replaceAll(getParentPanel().getAcc(), entityAcc);
                }
                getMainDetailPanel().rebuildPanel(entityAcc,
                        nextPageToken,
                        backToCurrentDetailLink);
                getMainDetailPanel().setCurrentBackLink(backToCurrentDetailLink);
                getMainDetailPanel().setPreviousBackLink(prevBackLink);
            }
        });
    }

    protected Widget createTextColumnValue(String text, int maxColumnLength) {
        if (text == null || text.length() < maxColumnLength) {
            return HtmlUtils.getHtml(text, "text");
        }
        else {
            return new FulltextPopperUpperHTML(text, maxColumnLength);
        }
    }

    abstract protected TableRow formatDataAsTableRow(Object data);

    protected List formatDataListAsTableRowList(List dataList) {
        List<TableRow> tableRows = new ArrayList<TableRow>();
        for (Object aDataList : dataList) {
            TableRow row = formatDataAsTableRow(aDataList);
            tableRows.add(row);
        }
        return tableRows;
    }
}
