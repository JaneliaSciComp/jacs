
package org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.xlink.CRefEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.xlink.CRefEntityServiceAsync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 14, 2008
 * Time: 12:29:42 PM
 */
public abstract class ScaffoldFeaturesPanelBuilder {

    private BSEntityPanel parentPanel;
    private EntityListener entitySelectedListener;
    private RemotePagingPanel dataPagingPanel;

    private boolean hasData;
    private RemotingPaginator dataPaginator;
    private SortableTable dataTable;

    protected abstract TableRow formatDataAsTableRow(Object data);

    protected abstract SortableTable createDataTable();

    protected abstract PagedDataRetriever createDataRetriever();

    protected abstract SelectionListener createSelectionListener();

    protected abstract String[][] getSortOptions();

    protected static CRefEntityServiceAsync cRefService = (CRefEntityServiceAsync) GWT.create(CRefEntityService.class);

    static {
        ((ServiceDefTarget) cRefService).setServiceEntryPoint("cref.srv");
    }

    protected ScaffoldFeaturesPanelBuilder(BSEntityPanel parentPanel, EntityListener entitySelectedListener) {
        this.parentPanel = parentPanel;
        this.entitySelectedListener = entitySelectedListener;
    }

    public Widget createContent() {
        createDataPanel();
        populateData();
        return dataPagingPanel;
    }

    private void createDataPanel() {
        dataTable = createDataTable();
        dataTable.addSelectionListener(createSelectionListener());
        dataTable.setHighlightSelect(true);
        dataPaginator = new RemotingPaginator(dataTable, createDataRetriever(), getSortOptions(), "ScaffoldFeatures");
        dataPagingPanel = new RemotePagingPanel(dataTable,
                new String[]{"5", "10", "25"},
                true,
                true,
                dataPaginator,
                false,
                0,
                5,
                "ScaffoldFeatures");
    }

    private DetailPanel getMainDetailPanel() {
        return parentPanel.getParentPanel();
    }

    private BSEntityPanel getParentPanel() {
        return parentPanel;
    }

    public void populateData() {
        if (!hasData) {
            dataPagingPanel.first();
        }
    }

    protected String getEntityAccessionNo() {
        return parentPanel.getAcc();
    }

    protected AsyncCallback createDataCountCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable caught) {
                hasData = false;
                listener.onFailure(caught);
            }

            // On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                Integer count = (Integer) result;
                if (count != null && count > 0) {
                    hasData = true;
                }
                listener.onSuccess(result); // Integer
            }
        };
    }

    protected Widget createDNAOrientationColumnValue(int direction) {
        if (direction == -1)
            return ImageBundleFactory.getControlImageBundle().getArrowReverseImage().createImage();
        else
            return ImageBundleFactory.getControlImageBundle().getArrowForwardImage().createImage();
    }

    protected Widget createEntityLinkColumnValue(final String entityAcc) {
        Widget entityAccessLink = new Link(entityAcc, new ClickListener() {

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
        return entityAccessLink;
    }

    protected Widget createTextColumnValue(String text, int maxColumnLength) {
        if (text == null || text.length() < maxColumnLength) {
            return HtmlUtils.getHtml(text, "text");
        }
        else {
            return new FulltextPopperUpperHTML(text, maxColumnLength);
        }
    }

    private class DataTableReadyListener implements DataRetrievedListener {
        private DataTableReadyListener() {
        }

        public void onSuccess(Object data) {
            // this callback is useful only for one notification
            // so once we processed it the listener will remove itself
            // from the notification list
            removeListener();
            dataTable.selectRow(1);
        }

        public void onFailure(Throwable throwable) {
            removeListener();
        }

        public void onNoData() {
            removeListener();
        }

        private void removeListener() {
            dataPaginator.removeDataRetrievedCallback(this);
        }

    }

    protected AsyncCallback createDataListCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                listener.onFailure(throwable);
            }

            public void onSuccess(Object result) {
                List orfList = (List) result;
                if (orfList == null || orfList.size() == 0) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(formatDataListAsTableRowList(orfList));
                }
            }
        };
    }

    private List formatDataListAsTableRowList(List dataList) {
        // we set up a data retrieve listener so that we could get a notification
        // when the sortable table has finished refreshing its data
        dataPaginator.addDataRetrievedCallback(new DataTableReadyListener());
        List tableRows = new ArrayList();
        for (Iterator dataItr = dataList.iterator(); dataItr.hasNext();) {
            TableRow row = formatDataAsTableRow(dataItr.next());
            tableRows.add(row);
        }
        return tableRows;
    }

    protected EntityListener getEntitySelectedListener() {
        return entitySelectedListener;
    }

    protected SortableTable getDataTable() {
        return dataTable;
    }

    public boolean hasData() {
        return hasData;
    }

}
