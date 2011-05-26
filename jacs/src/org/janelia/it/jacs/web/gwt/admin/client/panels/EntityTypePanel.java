package org.janelia.it.jacs.web.gwt.admin.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.entity.RemoveEntityTypeListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.entity.RemoveEntityTypePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOracleListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntityTypePanel extends TitledPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.admin.client.panels.EntityTypePanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private static final int ENTITY_TYPE_DELETE_COLUMN      = 0;
    private static final int ENTITY_TYPE_ID_COLUMN          = 1;
    private static final int ENTITY_TYPE_NAME_COLUMN        = 2;
    private static final int ENTITY_TYPE_DESCRIPTION_COLUMN = 3;
    private static final int ENTITY_TYPE_STYLE_COLUMN       = 4;
    private static final int ENTITY_TYPE_SEQUENCE_COLUMN    = 5;
    private static final int ENTITY_TYPE_ICONURL_COLUMN     = 6;

    private static final String ENTITY_TYPE_DELETE_HEADING      = "Delete";
    private static final String ENTITY_TYPE_ID_HEADING          = "Id";
    private static final String ENTITY_TYPE_NAME_HEADING        = "Name";
    private static final String ENTITY_TYPE_DESCRIPTION_HEADING = "Description";
    private static final String ENTITY_TYPE_STYLE_HEADING       = "Style";
    private static final String ENTITY_TYPE_SEQUENCE_HEADING    = "Sequence";
    private static final String ENTITY_TYPE_ICONURL_HEADING     = "Icon";

    private static final int DEFAULT_NUM_ROWS = 15;
    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};

    private Panel entityTypePanel;
    private SearchOraclePanel oraclePanel;
    private String _searchString;
    private SortableTable entityTypeTable;
    private RemotePagingPanel entityTypePagingPanel;
    private RemotingPaginator entityTypePaginator;
    private boolean haveData = false;
    private HashMap<String, EntityType> entityTypeMap = new HashMap<String, EntityType>();

    public EntityTypePanel() {
        super();
        VerticalPanel previousSequencePanel = new VerticalPanel();
        setupOraclePanel();
        setupEntityTypeListPanel();

        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        previousSequencePanel.add(oraclePanel);
        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        previousSequencePanel.add(entityTypePanel);

        TitledPanel entityTypeSelectionPanel = new TitledPanel("Browse Entity Types");
        entityTypeSelectionPanel.add(previousSequencePanel);

        LoadingLabel loadingLabel = new LoadingLabel("Loading...", false);
        TitledPanel entityTypePanel = new TitledPanel("Entity Types");
        entityTypePanel.add(loadingLabel);
        VerticalPanel masterNodePanel = new VerticalPanel();
        entityTypePanel.add(masterNodePanel);

        add(entityTypeSelectionPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(entityTypePanel);
        // Prompt the first loading...
        if (!haveData) {
            populateOracle();
            entityTypePagingPanel.first();
        }
        realize();
    }

    protected void popuplateContentPanel() {
    }

    /**
     * After the panel is displayed, load the microbe data
     */
    protected void onRealize() {
    }

    private void setupOraclePanel() {
        // Create the oraclePanel and hook up callbacks that will repopulate the table
        oraclePanel = new SearchOraclePanel("Entity Type Name", new SearchOracleListener() {
            public void onRunSearch(String searchString) {
                _searchString = searchString;
                entityTypePagingPanel.clear();
                entityTypePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(ENTITY_TYPE_ID_COLUMN, ENTITY_TYPE_ID_HEADING, SortableColumn.SORT_ASC)
                });
                entityTypePagingPanel.first();
            }

            public void onShowAll() {
                _searchString = null;
                entityTypePagingPanel.clear();
                entityTypePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(ENTITY_TYPE_ID_COLUMN, ENTITY_TYPE_ID_HEADING, SortableColumn.SORT_ASC)
                });
                entityTypePagingPanel.first();
            }
        });
        oraclePanel.addSuggestBoxStyleName("AdvancedBlastPreviousSequenceSuggestBox");
    }

    private void populateOracle() {
        // Populate the oraclePanel with the node names
        _dataservice.getEntityTypeNames(new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("error retrieving Entity Types for suggest oracle: " + caught.getMessage());
            }

            public void onSuccess(Object result) {
                List<String> entityNames = (List<String>) result;
                oraclePanel.addAllOracleSuggestions(entityNames);
                haveData = true;
                _logger.debug("populating SuggestOracle with " + (entityNames == null ? 0 : entityNames.size()) + " entity types");
            }
        });
    }

    private void setupEntityTypeListPanel() {
        entityTypePanel = new VerticalPanel();

        entityTypeTable = new SortableTable();
        entityTypeTable.setStyleName("SequenceTable");
        entityTypeTable.addColumn(new ImageColumn("&nbsp;"));                       // delete icon
        entityTypeTable.addColumn(new NumericColumn(ENTITY_TYPE_ID_HEADING));       // must match column int above
        entityTypeTable.addColumn(new TextColumn(ENTITY_TYPE_NAME_HEADING));
        entityTypeTable.addColumn(new TextColumn(ENTITY_TYPE_DESCRIPTION_HEADING));
        entityTypeTable.addColumn(new TextColumn(ENTITY_TYPE_STYLE_HEADING));
        entityTypeTable.addColumn(new TextColumn(ENTITY_TYPE_SEQUENCE_HEADING));
        entityTypeTable.addColumn(new TextColumn(ENTITY_TYPE_ICONURL_HEADING));

        entityTypeTable.addSelectionListener(new UserTableListener(entityTypeTable));
        entityTypeTable.addDoubleClickSelectionListener(new UserDoubleClickListener(entityTypeTable));
        entityTypeTable.setHighlightSelect(true);

        // TODO: move to abstract RemotePagingPanelFactory
        String[][] sortConstants = new String[][]{
                {"", ""},
//                {UserDataVO.SORT_BY_USER_LOGIN, USER_LOGIN_HEADING},
//                {UserDataVO.SORT_BY_USER_ID, USER_ID_HEADING},
//                {UserDataVO.SORT_BY_FULLNAME, USER_FULLNAME_HEADING},
//                {UserDataVO.SORT_BY_EMAIL, USER_EMAIL_HEADING},
        };
        entityTypePaginator = new RemotingPaginator(entityTypeTable, new EntityTypeDataRetriever(), sortConstants, "EntityTypeBrowser");
        entityTypePagingPanel = new RemotePagingPanel(entityTypeTable, entityTypePaginator, DEFAULT_NUM_ROWS_OPTIONS, DEFAULT_NUM_ROWS,
                "EntityTypeBrowser");
        entityTypePaginator.setSortColumns(new SortableColumn[]{
                new SortableColumn(ENTITY_TYPE_NAME_COLUMN, ENTITY_TYPE_NAME_HEADING, SortableColumn.SORT_ASC)
        });
        entityTypePagingPanel.setNoDataMessage("No entity types found.");

        entityTypePanel.add(entityTypePagingPanel);
        entityTypePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        entityTypePanel.add(getTableSelectHint());
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class EntityTypeDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _dataservice.getNumEntityTypes(_searchString, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("EntityTypeDataRetriever.getNumUsers().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("EntityTypeDataRetriever.getNumUsers().onSuccess() got " + result);
                    listener.onSuccess(((List) result).size()); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _dataservice.getPagedEntityTypes(_searchString, startIndex, numRows, sortArgs, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("DataService.getPagedEntityTypes().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    List<EntityType> entityTypes = (List<EntityType>) result;
                    if (entityTypes == null || entityTypes.size()== 0) {
                        _logger.debug("DataService.getPagedEntityTypes().onSuccess() got no data");
                        listener.onNoData();
                    }
                    else {
                        // TODO: move to PagingController
                        _logger.debug("DataService.getPagedEntityTypes().onSuccess() got data");
                        listener.onSuccess(processData((List<EntityType>) result));
                    }
                }
            });
        }

        private List processData(List<EntityType> entityTypes) {
            _logger.debug("DataService processing " + entityTypes.size() + " entity types");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (EntityType entityType : entityTypes) {
                if (entityType == null) // temporary until DAOs return real paged data
                    continue;
                TableRow tableRow = new TableRow();
                String objectId = entityType.getId().toString();
                tableRow.setRowObject(objectId);
                _logger.debug("Adding name=" + entityType.getName() + " for entityId=" + objectId);
                entityTypeMap.put(objectId, entityType);

                tableRow.setValue(ENTITY_TYPE_DELETE_COLUMN,
                        new TableCell(null, createRemoveEntityTypeWidget(entityType, tableRow)));
                tableRow.setValue(ENTITY_TYPE_ID_COLUMN, new TableCell(entityType.getId()));
                tableRow.setValue(ENTITY_TYPE_NAME_COLUMN, new TableCell(entityType.getName()));
                tableRow.setValue(ENTITY_TYPE_DESCRIPTION_COLUMN, new TableCell(entityType.getDescription()));
                tableRow.setValue(ENTITY_TYPE_STYLE_COLUMN, new TableCell(entityType.getStyle()));
                tableRow.setValue(ENTITY_TYPE_SEQUENCE_COLUMN, new TableCell(entityType.getSequence()));
                tableRow.setValue(ENTITY_TYPE_ICONURL_COLUMN, new TableCell(entityType.getIconurl()));

                tableRows.add(tableRow);
            }
            haveData = true;
            return tableRows;
        }
    }

    /**
     * Creates the node remove widget
     */
    private Widget createRemoveEntityTypeWidget(EntityType entityType, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveEntityTypeEventHandler(entityType, row));
        return image;
    }

    private class RemoveEntityTypeEventHandler implements ClickListener, RemoveEntityTypeListener, PopupListener {
        private EntityType entityType;
        private TableRow row;
        boolean inProgress;
        RemoveEntityTypePopup popup;


        public RemoveEntityTypeEventHandler(EntityType entityType, TableRow row) {
            this.entityType = entityType;
            this.row = row;
            inProgress = false;
            popup = null;
        }

        public void onClick(Widget widget) {
            startPopup(widget);
        }

        public void onPopupClosed(PopupPanel popupPanel, boolean b) {
            inProgress = false;
        }

        public void removeEntityType(final String entityTypeName) {
            AsyncCallback removeUserCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove entityType" + entityTypeName + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    entityTypePagingPanel.removeRow(row);
                    _logger.debug("Remove user succeded");
                    SystemWebTracker.trackActivity("DeleteUserNode");
                    finishedPopup();
                }

            };
            _dataservice.markUserForDeletion(entityTypeName, removeUserCallback);
        }

        private void startPopup(Widget widget) {
            if (!inProgress) {
                inProgress = true;
                popup = new RemoveEntityTypePopup(entityType, this, false);
                popup.addPopupListener(this);
                PopupLauncher launcher = new PopupAboveLauncher(popup);
                launcher.showPopup(widget);
            }
        }

        private void finishedPopup() {
            if (popup != null) {
                popup.hide();
            }
            popup = null;
        }

    }

    public class UserTableListener implements SelectionListener {
        private SortableTable targetTable;

        public UserTableListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        // This callback method is called by SortableTable within its 'onSelect' method when a row has been selected
        public void onSelect(String rowString) {
            onPreviousSelect(rowString, targetTable);
        }

        // This callback method is called by SortableTable within its 'onSelect' method when the selected row has been unselected
        public void onUnSelect(String rowString) {
            _logger.info("User unselected row " + rowString);
//            blastData.getQuerySequenceDataNodeMap().clear();
//            notifyUnselectListener("");
//            previousSequenceSelected = false;
//            validQuerySequenceFormAndEnableApplyButton();

            // Clear any hover attribute
            entityTypeTable.clearHover();
        }
    }

    public class UserDoubleClickListener implements DoubleClickSelectionListener {
        private SortableTable targetTable;

        public UserDoubleClickListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            if (onPreviousSelect(rowString, targetTable)) {
                //notifyDoubleClickListener("");
            }
        }
    }

    private boolean onPreviousSelect(String rowString, SortableTable targetTable) {
        int row = Integer.valueOf(rowString);
        TableRow selectedRow = (targetTable.getTableRows().get(row - 1)); // getTableRows() is data rows only, so we use -1 offset
        String targetId = (String) selectedRow.getRowObject();
        if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
            return false;
        }
        String entityTypeName = entityTypeMap.get(targetId).getName();
        if (entityTypeName == null) {
            _logger.error("Could not find name in entityTypeMap, parsing from table instead");
//            userName=targetTable.getText(row, USER_LOGIN_COLUMN);
        }
        else {
            _logger.debug("On select, retrieved entity type name=" + entityTypeName + " for targetId=" + targetId);
        }

        entityTypeTable.clearHover();

        return true;
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }
}
