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

package org.janelia.it.jacs.web.gwt.admin.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataVO;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveUserListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveUserPopup;
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

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 25, 2006
 * Time: 4:03:06 PM
 */
public class UserBrowserPanel extends TitledPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.admin.client.panels.UserBrowserPanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private static final int USER_DELETE_COLUMN = 0;
    private static final int USER_LOGIN_COLUMN = 1;
    private static final int USER_ID_COLUMN = 2;
    private static final int USER_FULLNAME_COLUMN = 3;
    private static final int USER_EMAIL_COLUMN = 4;

    private static final String USER_LOGIN_HEADING = "User Login";
    private static final String USER_ID_HEADING = "User Id";
    private static final String USER_FULLNAME_HEADING = "Name";
    private static final String USER_EMAIL_HEADING = "Email";

    private static final int DEFAULT_NUM_ROWS = 15;
    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};

    private Panel userListPanel;
    private SearchOraclePanel oraclePanel;
    private String _searchString;
    private SortableTable userTable;
    private RemotePagingPanel userPagingPanel;
    private RemotingPaginator userPaginator;
    private boolean haveData = false;
    private HashMap<String, UserDataVO> userMap = new HashMap<String, UserDataVO>();

    public UserBrowserPanel() {
        super();
        VerticalPanel previousSequencePanel = new VerticalPanel();
        setupOraclePanel();
        setupUserListPanel();

        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        previousSequencePanel.add(oraclePanel);
        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        previousSequencePanel.add(userListPanel);

        TitledPanel userSelectionPanel = new TitledPanel("Browse Users");
        userSelectionPanel.add(previousSequencePanel);

        LoadingLabel loadingLabel = new LoadingLabel("Loading...", false);
        TitledPanel userNodePanel = new TitledPanel("User Nodes");
        userNodePanel.add(loadingLabel);
        VerticalPanel masterNodePanel = new VerticalPanel();
        userNodePanel.add(masterNodePanel);

        add(userSelectionPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(userNodePanel);
        // Prompt the first loading...
        if (!haveData) {
            populateOracle();
            userPagingPanel.first();
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
        oraclePanel = new SearchOraclePanel("User Login", new SearchOracleListener() {
            public void onRunSearch(String searchString) {
                _searchString = searchString;
                userPagingPanel.clear();
                userPaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_LOGIN_COLUMN, USER_LOGIN_HEADING, SortableColumn.SORT_ASC)
                });
                userPagingPanel.first();
            }

            public void onShowAll() {
                _searchString = null;
                userPagingPanel.clear();
                userPaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_LOGIN_COLUMN, USER_LOGIN_HEADING, SortableColumn.SORT_ASC)
                });
                userPagingPanel.first();
            }
        });
        oraclePanel.addSuggestBoxStyleName("AdvancedBlastPreviousSequenceSuggestBox");
    }

    private void populateOracle() {
        // Populate the oraclePanel with the node names
        _dataservice.getUserLogins(new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("error retrieving user logins for suggest oracle: " + caught.getMessage());
            }

            public void onSuccess(Object result) {
                List<String> names = (List<String>) result;
                oraclePanel.addAllOracleSuggestions(names);
                haveData = true;
                _logger.debug("populating SuggestOrcle with " + (names == null ? 0 : names.size()) + " node names");
            }
        });
    }

    private void setupUserListPanel() {
        userListPanel = new VerticalPanel();

        userTable = new SortableTable();
        userTable.setStyleName("SequenceTable");
        userTable.addColumn(new ImageColumn("&nbsp;"));                       // node delete icon
        userTable.addColumn(new TextColumn(USER_LOGIN_HEADING));      // must match column int above
        userTable.addColumn(new NumericColumn(USER_ID_HEADING));     // must match column int above
        userTable.addColumn(new TextColumn(USER_FULLNAME_HEADING));      // must match column int above
        userTable.addColumn(new TextColumn(USER_EMAIL_HEADING)); // must match column int above

        userTable.addSelectionListener(new UserTableListener(userTable));
        userTable.addDoubleClickSelectionListener(new UserDoubleClickListener(userTable));
        userTable.setHighlightSelect(true);

        // TODO: move to abstract RemotePagingPanelFactory
        String[][] sortConstants = new String[][]{
                {"", ""},
                {UserDataVO.SORT_BY_USER_LOGIN, USER_LOGIN_HEADING},
                {UserDataVO.SORT_BY_USER_ID, USER_ID_HEADING},
                {UserDataVO.SORT_BY_FULLNAME, USER_FULLNAME_HEADING},
                {UserDataVO.SORT_BY_EMAIL, USER_EMAIL_HEADING},
        };
        userPaginator = new RemotingPaginator(userTable, new UserDataRetriever(), sortConstants, "UserBrowser");
        userPagingPanel = new RemotePagingPanel(userTable, userPaginator, DEFAULT_NUM_ROWS_OPTIONS, DEFAULT_NUM_ROWS,
                "UserBrowser");
        userPaginator.setSortColumns(new SortableColumn[]{
                new SortableColumn(USER_LOGIN_COLUMN, USER_LOGIN_HEADING, SortableColumn.SORT_ASC)
        });
        userPagingPanel.setNoDataMessage("No users found.");

        userListPanel.add(userPagingPanel);
        userListPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        userListPanel.add(getTableSelectHint());
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class UserDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _dataservice.getNumUsers(_searchString, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("UserDataRetriever.getNumUsers().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("UserDataRetriever.getNumUsers().onSuccess() got " + result);
                    listener.onSuccess(result); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _dataservice.getPagedUsers(_searchString, startIndex, numRows, sortArgs, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("UserDataRetriever.retrieveDataRows().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    UserDataVO[] users = (UserDataVO[]) result;
                    if (users == null || users.length == 0) {
                        _logger.debug("UserDataRetriever.getPagedUsers().onSuccess() got no data");
                        listener.onNoData();
                    }
                    else {
                        // TODO: move to PagingController
                        _logger.debug("UserDataRetriever.getPagedUsers().onSuccess() got data");
                        listener.onSuccess(processData((UserDataVO[]) result));
                    }
                }
            });
        }

        private List processData(UserDataVO[] users) {
            _logger.debug("UserDataRetriever processing " + users.length + " users");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (UserDataVO user : users) {
                if (user == null) // temporary until DAOs return real paged data
                    continue;
                TableRow tableRow = new TableRow();
                String objectId = user.getUserId().toString();
                tableRow.setRowObject(objectId);
                _logger.debug("Adding name=" + user.getUserLogin() + " for userId=" + objectId);
                userMap.put(objectId, user);

                tableRow.setValue(USER_DELETE_COLUMN,
                        new TableCell(null, createRemoveNodeWidget(user, tableRow)));
                tableRow.setValue(USER_LOGIN_COLUMN,
                        new TableCell(user.getUserLogin()));
                tableRow.setValue(USER_ID_COLUMN, new TableCell(user.getUserId()));
                tableRow.setValue(USER_FULLNAME_COLUMN,
                        new TableCell(user.getFullname()));
                tableRow.setValue(USER_EMAIL_COLUMN,
                        new TableCell(user.getEmail()));

                tableRows.add(tableRow);
            }
            haveData = true;
            return tableRows;
        }
    }

    /**
     * Creates the node remove widget
     */
    private Widget createRemoveNodeWidget(UserDataVO user, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveUserEventHandler(user, row));
        return image;
    }

    private class RemoveUserEventHandler implements ClickListener, RemoveUserListener, PopupListener {
        private UserDataVO user;
        private TableRow row;
        boolean inProgress;
        RemoveUserPopup popup;


        public RemoveUserEventHandler(UserDataVO user, TableRow row) {
            this.user = user;
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

        public void removeUser(final String userId) {
            AsyncCallback removeUserCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove user" + userId + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    userPagingPanel.removeRow(row);
                    _logger.debug("Remove user succeded");
                    SystemWebTracker.trackActivity("DeleteUserNode");
                    finishedPopup();
                }

            };
            _dataservice.markUserForDeletion(userId, removeUserCallback);
        }

        private void startPopup(Widget widget) {
            if (!inProgress) {
                inProgress = true;
                popup = new RemoveUserPopup(user, this, false);
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
            userTable.clearHover();
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
        String userName = userMap.get(targetId).getUserLogin();
        if (userName == null) {
            _logger.error("Could not find user name in userMap, parsing from table instead");
//            userName=targetTable.getText(row, USER_LOGIN_COLUMN);
        }
        else {
            _logger.debug("On select, retrieved userName=" + userName + " for targetId=" + targetId);
        }

        userTable.clearHover();

        return true;
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }
}
