
package org.janelia.it.jacs.web.gwt.download.client.formatter;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 7, 2006
 * Time: 5:30:38 PM
 */
public class DataFileFormatter implements MetaDataDisplay {

    private static final String[] MILL_ABBREV = new String[]{"B", "KB", "MB", "GB"};
    private static final String DESCRIPTIVE_TEXT = "Description";
    private CommonFormatter common = new CommonFormatter();

    /**
     * Prompted-text panel getters.
     */
    public Panel getFile(DownloadableDataNode dataNode) {
        return common.getItalicPromptedPanel("File", dataNode.getText(), "fileNameInDownloadFileDetails_File");
    }

    public Panel getAttribute(DownloadableDataNode dataNode, String attributeName) {
        // NOTE: fileAttributeAttname is pattern to follow, to get a very specific
        //       per-attribute style applied.  In applying the style, all spaces will be
        //       replaced with underscores.
        String attributeValue = dataNode.getAttribute(attributeName);
        if (attributeValue == null)
            attributeValue = "";
        if (attributeValue.length() > 60) {
            return common.getExpandablePromptedPanel(
                    attributeName,
                    dataNode.getAttribute(attributeName),
                    "fileAttributeInDownloadFileDetails_" + spaceToUnderscore(attributeName));
        }
        else {
            return common.getPromptedPanel(
                    attributeName,
                    dataNode.getAttribute(attributeName),
                    "fileAttributeInDownloadFileDetails_" + spaceToUnderscore(attributeName));
        }

    }

    private String spaceToUnderscore(String attributeName) {
        return attributeName.replace(' ', '_');
    }

    /**
     * Given panel and list of data files, add one tree for each data file.
     *
     * @param panel     where to place trees.
     * @param dataNodes what to turn into trees.
     */
    public List addDataFileTrees(VerticalPanel panel, List dataNodes) {
        return addDataFileTrees(panel, dataNodes, null);
    }

    public List addDataFileTrees(VerticalPanel panel, List dataNodes, DownloadableDataNodeFilter filter) {
        return addDataFileTrees(panel, dataNodes, filter, null);
    }

    /**
     * Given panel and list of data files, add one tree for each data file.
     *
     * @param panel        where to place trees.
     * @param dataNodes    what to turn into trees.
     * @param filter       what subtrees to include/exclude.
     * @param treeListener to expansions/collapses.
     * @return list of root tree items.
     */
    public List addDataFileTrees(
            VerticalPanel panel,
            List dataNodes,
            DownloadableDataNodeFilter filter,
            TreeListener treeListener) {

        TreeListener treeMouseListener = new MetaDataMouseListener();

        List returnList = new ArrayList();

        for (int i = 0; i < dataNodes.size(); i++) {
            DownloadableDataNode nextNode = (DownloadableDataNode) dataNodes.get(i);

            // Filtering may cause the item to be null, and if so, no tree is
            // created.
            TreeItem item = makeTreeItem(nextNode, filter);
            if (item != null) {

                Tree tree = new Tree();
                item.setUserObject(nextNode);   // TODO consider making this an itentifier suitable for grabbing the file info from a Service.
                tree.addItem(item);
                returnList.add(item);
                panel.add(tree);
                tree.addTreeListener(treeMouseListener);

                if (treeListener != null)
                    tree.addTreeListener(treeListener);

            }
        }

        return returnList;
    }

    /**
     * Recursive method to populate a tree items with sub-treeitems, etc.
     */
    public TreeItem makeTreeItem(DownloadableDataNode node, DownloadableDataNodeFilter filter) {
        // Must traverse the children nodes.  If no child nodes are accepted by the
        // filter, then the node, itself, must be acceptable, in order for a tree
        // item to be returned at all.
        List childItemList = new ArrayList();
        for (int i = 0; node.getChildren() != null && i < node.getChildren().size(); i++) {
            TreeItem childItem = makeTreeItem((DownloadableDataNode) node.getChildren().get(i), filter);
            if (childItem != null)
                childItemList.add(childItem);
        }

        // Check: has child items that are accepted by filter?  Or is itSELF accepted by filter?
        if (childItemList.size() > 0 || filter == null || filter.isAcceptable(node)) {
            //TODO make this more scalable, and apply lazy instantiation.
            String size = abbreviateSize(node.getSize());
            boolean hasFile = isDownloadableFile(node);

            String labelText = hasFile ? getDescriptiveText(node) : node.getText().trim();
            String className = hasFile ? "DownloadFileItem" : "DownloadDirectoryItem";

            HorizontalPanel itemPanel = new HorizontalPanel();
            if (hasFile)
                itemPanel.add(ImageBundleFactory.getControlImageBundle().getDocumentImage().createImage());

            itemPanel.add(HtmlUtils.getHtml(labelText, className));

            if (hasFile)
                itemPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;(" + size + ")", "DownloadFileItemSize"));

            TreeItem item = new TreeItem();
            item.setWidget(itemPanel);

            // TODO consider making this an itentifier suitable for grabbing the file info from a Service.
            if (hasFile)
                item.setUserObject(node);
            else
                item.setUserObject(null);

            for (int i = 0; i < childItemList.size(); i++) {
                TreeItem nextChildItem = (TreeItem) childItemList.get(i);
                item.addItem(nextChildItem);
            }
            return item;
        }
        else {
//String site = "N/A";
//if (node.getSite() != null)
//site = node.getSite().getGeographicLocation();
//Window.alert("Filtered out node " + node.getText() + " at " + site);
            // Filtered out: no item created for this node.
            return null;
        }

    }

    /**
     * Convenience for descriptive text.
     */
    public static String getDescriptiveText(DownloadableDataNode dataNode) {
        return dataNode.getAttribute(DESCRIPTIVE_TEXT);
    }

    /**
     * Displays data about file in a handy panel.
     *
     * @param panel what to show.
     */
    public void showFileMetaData(final VerticalPanel panel, int x, int y) {

        // Avoid fielding requests meant to go elsewhere.
        if (x == -1 || y == -1) {
            return;
        }
        // Construct a popup with no title, realize-now=true, and autohide=true
        final boolean autohideProperty = false;  // True does not currently work.
        final BasePopupPanel popup = new BasePopupPanel(null, true, autohideProperty) {

            public void populateContent() {
                add(panel);
            }

        };
        panel.add(closeLink(popup));
        popup.show();
        popup.setPopupPosition(x, y);

    }

    /**
     * Displays data about file in a handy panel, and implements the MetaDataDisplay
     * interface.
     *
     * @param dataNode what to show.
     * @param x        position of mouse cursor
     * @param y        position of mouse cursor
     */
    public void showFileMetaData(final DownloadableDataNode dataNode, int x, int y) {
        // no longer used
    }

    /**
     * Add result of calling this method, to any popup you wish the user to be able to close.
     *
     * @param toClose what to close.
     * @return something to close it.
     */
    public Panel closeLink(final BasePopupPanel toClose) {
        ClickListener dismissListener = new ClickListener() {
            public void onClick(Widget w) {
                toClose.hide();
            }
        };
        FlowPanel fpanel = new FlowPanel();
        ActionLink link = new ActionLink("close this window", dismissListener);
        fpanel.add(new HTMLPanel("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"));
        fpanel.add(link);
        return fpanel;
    }

    /**
     * Creates a link to a downloadable PDF.
     *
     * @param node represents a data file on disk.
     */
    public Widget createPaperDownloadLink(DownloadableDataNode node) {
        ExternalLink link = new ExternalLink("Download Paper", node.getLocation());

        String id = HTMLPanel.createUniqueId();
        HTMLPanel panel = new HTMLPanel("<span class='greaterGreater'>&gt;&gt;&nbsp;</span><span id='" + id + "'></span>");
        panel.setStyleName("DownloadPublicationLinkWrapper");
        DOM.setStyleAttribute(panel.getElement(), "display", "inline");
        DOM.setStyleAttribute(link.getElement(), "display", "inline");
        panel.add(link, id);

        return panel;
    }

    /**
     * Helper to make the size follow a fixed format.
     */
    public static String abbreviateSize(long size) {
        String suffixMill = "B";
        double returnSize = size;
        for (int i = 0; i < 4; i++) {
            // Test: is this mantissa a presentable size?
            if (returnSize < 1000) {
                suffixMill = MILL_ABBREV[i];
                break;
            }

            // Iteratively reduce the mantissa by 1000.
            returnSize /= 1000;
        }

        String returnSizeStr = "" + returnSize;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 2; i++) {
            char theChar = returnSizeStr.charAt(i);
            buf.append(theChar);
            // Include one character after the period, if encountered.
            if (theChar == '.') {
                buf.append(returnSizeStr.charAt(i + 1));
            }
        }
        if (returnSize > 100) {
            buf.append("0");
        }

        return buf.toString() + " " + suffixMill;
    }

    private boolean isDownloadableFile(DownloadableDataNode node) {
        if (node == null)
            return false;
        if (node.getChildren() == null)
            return true;
        if (node.getChildren().size() == 0 && node.getSize() > 0)
            return true;
        return false;
    }

    private boolean notEmpty(DownloadableDataNode node) {
        boolean returnValue = false;
        if (node != null) {
            String fileLocation = node.getLocation();
            if (fileLocation != null && fileLocation.length() > 0) {
                if (!fileLocation.endsWith("/")) {
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    /**
     * A Click Listener to replace the current window with contents of some URL.
     * Essentially to make the web browser "go to" the new link.
     */
    public class GotoLinkClickListener implements ClickListener {
        private String location;

        public GotoLinkClickListener(String location) {
            this.location = location;
        }

        public void onClick(Widget w) {
            Window.open(location, "_self", "");
        }
    }


}
