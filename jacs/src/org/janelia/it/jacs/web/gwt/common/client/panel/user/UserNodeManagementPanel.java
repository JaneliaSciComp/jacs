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

package org.janelia.it.jacs.web.gwt.common.client.panel.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.node.UserNodeManagementNewNodePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.*;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 14, 2010
 * Time: 2:21:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserNodeManagementPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.user.UserNodeManagementPanel");
    VerticalPanel userNodeListPanel;
    HorizontalPanel nodeManagementPanel;
    TitledBox userNodeTitleBox;
    SortableTable userNodeTable;
    RemotingPaginator userNodePaginator;
    RemotePagingPanel userNodePagingPanel;
    HorizontalPanel buttonPanel;
    RoundedButton refreshButton;
    RoundedButton uploadNewNodeButton;
    UserNodeManagementNewNodePopup newNodePopup;
    String nodeClassName;
    private HashMap<String, String> nodeNameMap = new HashMap<String, String>();
    private HashMap<String, UserDataNodeVO> nodeIdMap = new HashMap<String, UserDataNodeVO>();
    private boolean haveData = false;
    int verticalPopupOffset;
    int horizontalPopupOffset;

    private String[] numRowOptions;
    private int defaultNumRows;
    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};
    private static int DEFAULT_START_NUM_ROWS = 10;

    UserDataNodeVO currentlySelectedNode;

    private static final int USER_NODE_DELETE_NODE_COLUMN = 0;
    private static final int USER_NODE_NAME_COLUMN = 1;
    private static final int USER_NODE_ADDED_COLUMN = 2;
    private static final int USER_NODE_TYPE_COLUMN = 3;
    private static final int USER_NODE_LENGTH_COLUMN = 4;
    private static final int USER_NODE_STATUS_COLUMN = 5;
    private static final int USER_NODE_ACTION_COLUMN = 6;

    private static final String USER_NODE_NAME_HEADING = "Node Name";
    private static final String USER_NODE_ADDED_HEADING = "Created";
    private static final String USER_NODE_TYPE_HEADING = "Type";
    private static final String USER_NODE_LENGTH_HEADING = "File size";
    private static final String USER_NODE_STATUS_HEADING = "Status";
    private static final String USER_NODE_ACTION_HEADING = "Action";

    private static final int NAME_MAX_SIZE = 100;

    List<NodeSelectedAction> nodeSelectedActionList;

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public interface NodeSelectedAction {
        public void nodeSelected(UserDataNodeVO node);
    }

    public UserNodeManagementPanel(String nodeClassName, UserNodeManagementNewNodePopup newNodePopup, int verticalPopupOffset, int horizontalPopupOffset) {
        super();
        nodeSelectedActionList = new ArrayList<NodeSelectedAction>();
        this.nodeClassName = nodeClassName;
        this.newNodePopup = newNodePopup;
        this.verticalPopupOffset=verticalPopupOffset;
        this.horizontalPopupOffset=horizontalPopupOffset;
        init();
    }

    private String getRootClassName(String classNameString) {
        int lastPeriodPosition = classNameString.lastIndexOf(".");
        if (lastPeriodPosition > 0) {
            return classNameString.substring(lastPeriodPosition + 1);
        }
        else {
            return classNameString;
        }
    }

    private void init() {
        nodeManagementPanel = new HorizontalPanel();
        userNodeTitleBox = new TitledBox(getRootClassName(nodeClassName) + " Management Panel");
        userNodeTitleBox.setWidth("300px"); // min width when contents hidden
        userNodeTitleBox.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        buttonPanel = createButtonPanel();
        userNodeTitleBox.add(buttonPanel);
        userNodeTitleBox.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        setupNodeListPanel();
        nodeManagementPanel.add(userNodeTitleBox);

        setVisible(true);
        nodeManagementPanel.setVisible(true);
        this.add(nodeManagementPanel);
    }

    HorizontalPanel createButtonPanel() {
        buttonPanel = new HorizontalPanel();

        refreshButton = new RoundedButton("Refresh", new ClickListener() {
            public void onClick(Widget widget) {
                _logger.debug("UserNodeManagementPanel: calling userNodeTable.refresh()");
                setupNodeListPanel();
            }
        });
        refreshButton.setWidth("130px");
        buttonPanel.add(refreshButton);

        buttonPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        uploadNewNodeButton = new RoundedButton("Upload New Node", new ClickListener() {
            public void onClick(Widget widget) {
                _logger.debug("UserNodeManagementPanel: upload button clicked - calling newNodePopup.show()");
                PopupLauncher popupLauncher = new PopupAtAbsolutePixelLauncher(newNodePopup, verticalPopupOffset, horizontalPopupOffset);
                popupLauncher.showPopup(newNodePopup);
            }
        });
        uploadNewNodeButton.setWidth("130px");
        buttonPanel.add(uploadNewNodeButton);

        return buttonPanel;
    }

    public void registerNodeSelectedAction(NodeSelectedAction action) {
        nodeSelectedActionList.add(action);
    }

    private void setupNodeListPanel() {
        if (userNodeListPanel != null) {
            userNodeTitleBox.remove(userNodeListPanel);
        }
        userNodeListPanel = new VerticalPanel();

        userNodeTable = new SortableTable();
        userNodeTable.setStyleName("SequenceTable");
        userNodeTable.addColumn(new ImageColumn("&nbsp;"));                       // node delete icon
        userNodeTable.addColumn(new TextColumn(USER_NODE_NAME_HEADING));      // must match column int above
        userNodeTable.addColumn(new TextColumn(USER_NODE_ADDED_HEADING));     // must match column int above
        userNodeTable.addColumn(new TextColumn(USER_NODE_TYPE_HEADING));      // must match column int above
        userNodeTable.addColumn(new NumericColumn(USER_NODE_LENGTH_HEADING)); // must match column int above
        userNodeTable.addColumn(new TextColumn(USER_NODE_STATUS_HEADING));    // must match column int above
        userNodeTable.addColumn(new TextColumn(USER_NODE_ACTION_HEADING));    // must match column int above

        userNodeTable.addSelectionListener(new UserNodeTableListener(userNodeTable));
        userNodeTable.addDoubleClickSelectionListener(new UserNodeDoubleClickListener(userNodeTable));
        userNodeTable.setHighlightSelect(true);

        // TODO: move to abstract RemotePagingPanelFactory
        String[][] sortConstants = new String[][]{
                {"", ""},
                {UserDataNodeVO.SORT_BY_NAME, USER_NODE_NAME_HEADING},
                {UserDataNodeVO.SORT_BY_DATE_CREATED, USER_NODE_ADDED_HEADING},
                {UserDataNodeVO.SORT_BY_TYPE, USER_NODE_TYPE_HEADING},
                {UserDataNodeVO.SORT_BY_LENGTH, USER_NODE_LENGTH_HEADING},
        };
        userNodePaginator = new RemotingPaginator(userNodeTable, new NodeDataRetriever(), sortConstants,
                "UploadUserNode");
        defaultNumRows = DEFAULT_START_NUM_ROWS;
        numRowOptions = DEFAULT_NUM_ROWS_OPTIONS;
        userNodePagingPanel = new RemotePagingPanel(userNodeTable, userNodePaginator, numRowOptions,
                defaultNumRows, "UploadUserNode");
        userNodePaginator.setSortColumns(new SortableColumn[]{
                new SortableColumn(USER_NODE_ADDED_COLUMN, USER_NODE_ADDED_HEADING, SortableColumn.SORT_DESC)
        });
        userNodePagingPanel.setNoDataMessage("No user nodes found.");

        userNodeListPanel.add(userNodePagingPanel);
        userNodeListPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        userNodeListPanel.add(getTableSelectHint());
        userNodeListPanel.setVisible(true);
        userNodeTitleBox.add(userNodeListPanel);

        // Without this command, the table does not get initialized in the proper order
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                userNodePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_NODE_ADDED_COLUMN, USER_NODE_ADDED_HEADING, SortableColumn.SORT_DESC)
                });
                userNodePagingPanel.first();
            }
        });
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class NodeDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _dataservice.getNumNodesForUserByName(nodeClassName, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("NodeDataRetriever.getNumNodesForUserByName().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("NodeDataRetriever.getNumNodesForUserByName().onSuccess() got " + result);
                    listener.onSuccess(result); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _dataservice.getPagedNodesForUserByName(nodeClassName, startIndex, numRows,
                    sortArgs, new AsyncCallback() {
                        public void onFailure(Throwable caught) {
                            _logger.error("NodeDataRetriever.retrieveDataRows().onFailure(): " + caught.getMessage());
                            listener.onFailure(caught);
                        }

                        // On success, populate the table with the DataNodes received
                        public void onSuccess(Object result) {
                            UserDataNodeVO[] nodes = (UserDataNodeVO[]) result;
                            if (nodes == null || nodes.length == 0) {
                                _logger.debug("NodeDataRetriever.retrieveDataRows().onSuccess() got no data");
                                listener.onNoData();
                            }
                            else {
                                // TODO: move to PagingController
                                _logger.debug("NodeDataRetriever.retrieveDataRows().onSuccess() got data");
                                listener.onSuccess(processData((UserDataNodeVO[]) result));
                            }
                        }
                    });
        }

        private List processData(UserDataNodeVO[] nodes) {
            _logger.debug("NodeDataRetriever processing " + nodes.length + " nodes");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (UserDataNodeVO node : nodes) {
                if (node == null) // temporary until DAOs return real paged data
                    continue;
                node.setDataType(nodeClassName);
                TableRow tableRow = new TableRow();
                String objectId = node.getDatabaseObjectId();
                tableRow.setRowObject(objectId);
                _logger.debug("Adding name=" + node.getNodeName() + " for nodeId=" + objectId);
                nodeNameMap.put(objectId, determineUsefulNodeName(node));
                nodeIdMap.put(node.getDatabaseObjectId(), node);

                tableRow.setValue(USER_NODE_DELETE_NODE_COLUMN,
                        new TableCell(null, createRemoveNodeWidget(node, tableRow)));
                tableRow.setValue(USER_NODE_NAME_COLUMN,
                        new TableCell(determineUsefulNodeName(node), getNodeNameWidget(node, tableRow)));
                tableRow.setValue(USER_NODE_ADDED_COLUMN,
                        new TableCell(new FormattedDateTime(node.getDateCreated().getTime())));
                tableRow.setValue(USER_NODE_TYPE_COLUMN,
                        new TableCell(getBaseName(node.getSequenceType())));
                tableRow.setValue(USER_NODE_LENGTH_COLUMN,
                        new TableCell(node.getLength()));
                tableRow.setValue(USER_NODE_STATUS_COLUMN,
                        new TableCell(node.getParentTaskStatus()));
                tableRow.setValue(USER_NODE_ACTION_COLUMN,
                        new TableCell(null, createUserNodeInfoWidget(node, userNodePagingPanel)));

                tableRows.add(tableRow);
            }
            haveData = true;
            return tableRows;
        }
    }

    public class UserNodeTableListener implements SelectionListener {
        private SortableTable targetTable;

        public UserNodeTableListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        // This callback method is called by SortableTable within its 'onSelect' method when a row has been selected
        public void onSelect(String rowString) {
            onNodeSelect(rowString, targetTable);
        }

        // This callback method is called by SortableTable within its 'onSelect' method when the selected row has been unselected
        public void onUnSelect(String rowString) {
            _logger.info("User unselected row " + rowString);
//            blastData.getQuerySequenceDataNodeMap().clear();
//            notifyUnselectListener("");
//            previousSequenceSelected = false;
//            validQuerySequenceFormAndEnableApplyButton();
//
//            // Clear any hover attribute
//            userSequenceTable.clearHover();
        }
    }

    public class UserNodeDoubleClickListener implements DoubleClickSelectionListener {
        private SortableTable targetTable;

        public UserNodeDoubleClickListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            if (onNodeSelect(rowString, targetTable)) {
                //notifyDoubleClickListener("");
            }
        }
    }

    private boolean onNodeSelect(String rowString, SortableTable targetTable) {
        int row = Integer.valueOf(rowString);
        TableRow selectedRow = targetTable.getTableRows().get(row - 1); // getTableRows() is data rows only, so we use -1 offset
        String targetId = (String) selectedRow.getRowObject();
        if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
            return false;
        }
        currentlySelectedNode = nodeIdMap.get(targetId);
        for (NodeSelectedAction action : nodeSelectedActionList) {
            action.nodeSelected(currentlySelectedNode);
        }
        userNodeTable.clearHover();
        return true;
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }

    public static String determineUsefulNodeName(UserDataNodeVO node) {
        if (node.getNodeName() != null &&
                node.getNodeName().startsWith("upload_") &&
                node.getDescription() != null &&
                node.getDescription().length() > 0) {
            return node.getDescription();
        }
        else {
            return node.getNodeName();
        }
    }

    private Widget createRemoveNodeWidget(UserDataNodeVO node, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveNodeEventHandler(node, row));
        return image;
    }

    private class RemoveNodeEventHandler implements ClickListener, RemoveNodeListener, PopupListener {
        private UserDataNodeVO node;
        private TableRow row;
        boolean inProgress;
        RemoveNodePopup popup;


        public RemoveNodeEventHandler(UserDataNodeVO node, TableRow row) {
            this.node = node;
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

        public void removeNode(final String nodeId) {
            AsyncCallback removeNodeCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove node" + nodeId + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    userNodePagingPanel.removeRow(row);
                    _logger.debug("Remove node succeded");
                    SystemWebTracker.trackActivity("DeleteQueryNode");
                    finishedPopup();
                }

            };
            _dataservice.deleteNode(nodeId, removeNodeCallback);
        }

        private void startPopup(Widget widget) {
            if (!inProgress) {
                inProgress = true;
                popup = new RemoveNodePopup(node.getNodeName(), node.getDatabaseObjectId(), this, false);
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

    private Widget getNodeNameWidget(UserDataNodeVO node, TableRow row) {
        Grid grid = new Grid(1, 3);
        grid.setCellSpacing(0);
        grid.setCellPadding(0);

        grid.setWidget(0, 0, new FulltextPopperUpperHTML(determineUsefulNodeName(node), NAME_MAX_SIZE));
        grid.setWidget(0, 1, HtmlUtils.getHtml("&nbsp;", "text"));
        grid.setWidget(0, 2, new ActionLink("edit", new EditUserNodeNameHandler(node, row)));

        return grid;
    }

    private class EditUserNodeNameHandler implements ClickListener, EditUserNodeNameListener {

        private UserDataNodeVO node;
        private TableRow row;

        public EditUserNodeNameHandler(UserDataNodeVO node, TableRow row) {
            this.node = node;
            this.row = row;
        }

        public void onClick(Widget widget) {
            new PopupAboveLauncher(new EditUserNodeNamePopup(node, this, false)).showPopup(widget);
        }

        public void replaceUserNodeName(final String nodeId, final String nodeName) {
            AsyncCallback renameNodeCallback = new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("Renaming user node " + nodeId + " from " + node.getNodeName() + " to " +
                            nodeName + " failed", caught);
                }

                //  On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("EditUserNodeNameHandler: onSuccess()");
                    String newNodeName = (String) result;
                    if (newNodeName != null) {
                        if (!newNodeName.equals(nodeName)) {
                            // the user provided a name longer
                            // than the maximum length allowed for the name column
                            Window.alert("The given name was too long therefore it was truncated to:\n" +
                                    "\"" + newNodeName + "\"");
                        }
                        _logger.debug("Renamed node " + node.getDatabaseObjectId() + " from " + node.getNodeName() +
                                " to " + newNodeName);
                        node.setNodeName(newNodeName);
                        // Here we assume that editing the node name should implicitly select it as the selected node
                        nodeNameMap.put(node.getDatabaseObjectId(), newNodeName);
                        nodeIdMap.put(node.getDatabaseObjectId(), node);
                        updateDisplay(node, row);
                    }
                    else {
                        _logger.debug("EditUserNodeNameHandler: newNodeName is null");
                    }
                }
            };
            _dataservice.replaceNodeName(nodeId, nodeName, renameNodeCallback);
        }

        /**
         * Updates the edit node name widget
         *
         * @param node
         * @return
         */
        private void updateDisplay(UserDataNodeVO node, TableRow row) {
            if (node == null)
                _logger.error("updateDisplay() node is null");
            TableCell nodeNameCell = row.getTableCell(USER_NODE_NAME_COLUMN);
            if (node != null)
                nodeNameCell.setValue(determineUsefulNodeName(node));
            Widget widget = getNodeNameWidget(node, row);
            if (widget == null)
                _logger.error("widget from getNodeNameWidget() is null");
            nodeNameCell.setWidget(widget);
            userNodePagingPanel.getSortableTable().refreshCell(nodeNameCell.getRow(), nodeNameCell.getCol());
        }

    }

    public UserDataNodeVO getCurrentlySelectedNode() {
        return currentlySelectedNode;
    }

    private Widget createUserNodeInfoWidget(final UserDataNodeVO node, final Widget widget) {
        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar menu = new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar();
        menu.setAutoOpen(false);

        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar dropDown = new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar(true);

        dropDown.addItem("Show Node details", true, new Command() {
            public void execute() {
                _logger.debug("Execute called for detailed info for node=" + node.getDatabaseObjectId());
                NodeDetailsPopup nodeDetailsPopup = new NodeDetailsPopup(node, true);
                PopupLauncher launcher = new PopupAtRelativePixelLauncher(nodeDetailsPopup, 0, 0);
                launcher.showPopup(widget);
            }
        });

        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem nodeDetailsMenuItem =
                new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem("Details&nbsp;" +
                        ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                        /* asHTML*/ true, dropDown);
        nodeDetailsMenuItem.setStyleName("topLevelMenuItem");
        menu.addItem(nodeDetailsMenuItem);

        return menu;
    }

    public class NodeDetailsPopup extends ModalPopupPanel {
        private UserDataNodeVO node;

        public NodeDetailsPopup(UserDataNodeVO node, boolean realizeNow) {
            super("Node Details", realizeNow);
            this.node = node;
        }

        protected ButtonSet createButtons() {
            RoundedButton[] tmpButtons = new RoundedButton[2];
            tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
                public void onClick(Widget widget) {
                    hide();
                }
            });
            tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
                public void onClick(Widget widget) {
                    hide();
                }
            });
            return new ButtonSet(tmpButtons);
        }

        /**
         * For subclasses to supply dialog content
         */
        protected void populateContent() {
            add(HtmlUtils.getHtml("Delete sequence \"" + node.getNodeName() + "\" ?", "text"));
            add(HtmlUtils.getHtml("&nbsp;", "text"));
        }

    }

    private String getBaseName(String className) {
        String[] comps=className.split("\\.");
        if (comps==null || comps.length==0 || comps.length==1) {
            return className;
        } else {
            return comps[comps.length-1];
        }
    }

}
