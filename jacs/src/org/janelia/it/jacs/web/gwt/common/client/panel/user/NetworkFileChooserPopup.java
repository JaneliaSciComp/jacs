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
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 18, 2010
 * Time: 1:14:20 PM
 */
public class NetworkFileChooserPopup extends ModalPopupPanel {
    private static final String EXPAND_DISPLAY = "expand all";
    private static final String COLLAPSE_DISPLAY = "collapse all";
    private TextBox _directoryPathTextBox;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public NetworkFileChooserPopup() {
        super("Network File Chooser", false);
    }

    @Override
    protected void populateContent() {
        _directoryPathTextBox = new TextBox();
        VerticalPanel mainPanel = new VerticalPanel();
        HorizontalPanel addressPanel = new HorizontalPanel();
        addressPanel.add(HtmlUtils.getHtml("Address:", "prompt"));
        addressPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        addressPanel.add(_directoryPathTextBox);
        mainPanel.add(addressPanel);
        HorizontalPanel expansionControlPanel = new HorizontalPanel();
        ActionLink expandLink = new ActionLink(EXPAND_DISPLAY);
        ActionLink collapseLink = new ActionLink(COLLAPSE_DISPLAY);
        expansionControlPanel.add(expandLink);
        expansionControlPanel.add(collapseLink);
        //LocalCollapseExpandListener localCollapseExpandListener = new LocalCollapseExpandListener();
        mainPanel.add(expansionControlPanel);
        mainPanel.add(new TextArea());
//        List rootItems = addDataFileTrees(mainPanel, new ArrayList(), new MyTreeListener());
        List rootItems = new ArrayList();
        rootItems.add("home");
        rootItems.add("ccbuild");
        ExpansionClickListener expander = new ExpansionClickListener(rootItems);
        expandLink.addClickListener(expander);
        collapseLink.addClickListener(expander);
        add(mainPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
            }
        }));
        actionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        actionPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));
        add(actionPanel);

        show();
    }

    /**
     * Listener to two expansion click listeners, that fully contract or fully expand a tree.
     */
    public class ExpansionClickListener implements ClickListener {
        private static final int EXPAND_ALL = 0;
        private static final int EXPAND_NONE = 1;
        private static final int EXPAND_LOCAL = 2;

        private List _treeItems;
        private int _expandAllState = EXPAND_NONE;

        public ExpansionClickListener(List treeItems) {
            _treeItems = treeItems;
        }

        public void setExpandStateNeutral() {
            _expandAllState = EXPAND_LOCAL;
        }

        public boolean isFullyExpanded() {
            return _expandAllState == EXPAND_ALL;
        }

        public boolean isFullyCollapsed() {
            return _expandAllState == EXPAND_NONE;
        }

        public void onClick(Widget w) {
            if (w.toString().indexOf(EXPAND_DISPLAY) != -1) {
                setExpansionState(true);
            }
            else if (w.toString().indexOf(COLLAPSE_DISPLAY) != -1) {
                setExpansionState(false);
            }

        }

        public void refresh() {
            if (_expandAllState == EXPAND_ALL) {
                setExpansionState(true);
            }
            else if (_expandAllState == EXPAND_NONE) {
                setExpansionState(false);
            }
            // Do nothing for local expansion.
        }

        public void setExpansionState(boolean expandState) {
            if (expandState) {
                _expandAllState = EXPAND_ALL;
            }
            else {
                _expandAllState = EXPAND_NONE;
            }

            for (Object _treeItem : _treeItems) {
                TreeItem item = (TreeItem) _treeItem;
                recursiveStateChange(item, expandState);
            }
        }

        private void recursiveStateChange(TreeItem item, boolean newState) {
            item.setState(newState);
            for (int i = 0; i < item.getChildCount(); i++) {
                recursiveStateChange(item.getChild(i), newState);
            }
        }
    }

    /**
     * Listener here, will detect when the state of any item has changed, and inform the
     * expansion click listener, that no absolute (but rather a local) expansion state is
     * in effect.
     */
    public class LocalCollapseExpandListener implements TreeListener {
        private ExpansionClickListener _expander;

        public void setExpander(ExpansionClickListener expander) {
            _expander = expander;
        }

        public void onTreeItemSelected(TreeItem item) {
        }

        public void onTreeItemStateChanged(TreeItem item) {
            _expander.setExpandStateNeutral();
        }
    }

    /**
     * Given panel and list of data files, add one tree for each data file.
     * <p/>
     * //     * @param panel        where to place trees.
     * //     * @param dataNodes    what to turn into trees.
     * //     * @param treeListener to expansions/collapses.
     * //     * @return list of root tree items.
     */
//    public List addDataFileTrees(
//            VerticalPanel panel,
//            List dataNodes,
//            TreeListener treeListener) {
//
//        TreeListener treeMouseListener = new MetaDataMouseListener();
//
//        List returnList = new ArrayList();
//
//        for (Object dataNode : dataNodes) {
////            DownloadableDataNode nextNode = (DownloadableDataNode) dataNode;
//
//            // Filtering may cause the item to be null, and if so, no tree is
//            // created.
//            TreeItem item = makeTreeItem(nextNode);
//            if (item != null) {
//                Tree tree = new Tree();
//                item.setUserObject(nextNode);   // TODO consider making this an itentifier suitable for grabbing the file info from a Service.
//                tree.addItem(item);
//                returnList.add(item);
//                panel.add(tree);
//                tree.addTreeListener(treeMouseListener);
//
//                if (treeListener != null)
//                    tree.addTreeListener(treeListener);
//
//            }
//        }
//
//        return returnList;
//    }
//
//    /**
//     * Recursive method to populate a tree items with sub-treeitems, etc.
//     */
//    public TreeItem makeTreeItem(DownloadableDataNode node) {
//        // Must traverse the children nodes.  If no child nodes are accepted by the
//        // filter, then the node, itself, must be acceptable, in order for a tree
//        // item to be returned at all.
//        List childItemList = new ArrayList();
//        for (int i = 0; node.getChildren() != null && i < node.getChildren().size(); i++) {
//            TreeItem childItem = makeTreeItem((DownloadableDataNode) node.getChildren().get(i));
//            if (childItem != null)
//                childItemList.add(childItem);
//        }
//
//        // Check: has child items that are accepted by filter?  Or is itSELF accepted by filter?
//        if (childItemList.size() > 0 ) {
//            //TODO make this more scalable, and apply lazy instantiation.
//            String labelText = node.getText().trim();
//            HorizontalPanel itemPanel = new HorizontalPanel();
//            itemPanel.add(ImageBundleFactory.getControlImageBundle().getDocumentImage().createImage());
//
//            itemPanel.add(HtmlUtils.getHtml(labelText, "Name"));
//
//            TreeItem item = new TreeItem();
//            item.setWidget(itemPanel);
//
//            item.setUserObject(node);
//
//            for (Object aChildItemList : childItemList) {
//                TreeItem nextChildItem = (TreeItem) aChildItemList;
//                item.addItem(nextChildItem);
//            }
//            return item;
//        }
//        else {
//            return null;
//        }
//
//    }

    private class MyTreeListener implements TreeListener {

        private MyTreeListener() {
        }

        public void onTreeItemSelected(TreeItem treeItem) {
//            DownloadableDataNode dataNode =
//		(DownloadableDataNode)treeItem.getUserObject();
        }

        public void onTreeItemStateChanged(TreeItem treeItem) {
        }

    }

}
