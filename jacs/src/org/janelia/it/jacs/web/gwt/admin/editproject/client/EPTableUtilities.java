
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;

/**
 * Created by IntelliJ IDEA.
 * User: gcao
 * Date: Jun 11, 2007
 * Time: 11:24:46 AM
 */
public class EPTableUtilities {

    public static void addPromptWidgetPair(HTMLTable table, int row, int col, String prompt, Widget tableWidget) {
        TableUtils.addHTMLRow(table, new RowIndex(row), col, prompt, tableWidget);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }

    public static void addTextTextPair(HTMLTable table, int row, int col, String name, String value) {
        TableUtils.addHTMLRow(table, new RowIndex(row), col, name, value);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }

    public static void addWidgetWidgetPair(HTMLTable table, int row, int col, Widget widget1, Widget widget2) {
        table.setWidget(row, col, widget1);
        table.setWidget(row, col + 1, widget2);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }


    /**
     * derived from: BrowseProjectsPage.java:
     * Have to fill up the blank space with something other than the external link or else the double
     * underline will expand past the text
     */
    public static Widget getStyleLinkCell(Widget externalLink) {
        DockPanel panel = new DockPanel();

        String id = HTMLPanel.createUniqueId();
        HTMLPanel linkPanel = new HTMLPanel("<span>[ </span><span id='" + id + "'></span> &nbsp]");
        linkPanel.add(externalLink, id);
        DOM.setStyleAttribute(linkPanel.getElement(), "display", "inline");
        DOM.setStyleAttribute(externalLink.getElement(), "display", "inline");

        panel.add(linkPanel, DockPanel.WEST);
        panel.add(new HTML("&nbsp;"), DockPanel.CENTER); // fill the rest of the row with blank space
        return panel;

    }

    /**
     * copied from: BrowseProjectsPage.java:
     * Have to fill up the blank space with something other than the external link or else the double
     * underline will expand past the text
     */
    public static Widget getLinkCell(Widget externalLink) {
        DockPanel panel = new DockPanel();

        String id = HTMLPanel.createUniqueId();
        HTMLPanel linkPanel = new HTMLPanel("<span class='greaterGreater'>&gt;&gt;&nbsp;</span><span id='" + id + "'></span>");
        linkPanel.add(externalLink, id);
        DOM.setStyleAttribute(linkPanel.getElement(), "display", "inline");
        DOM.setStyleAttribute(externalLink.getElement(), "display", "inline");

        panel.add(linkPanel, DockPanel.WEST);
        panel.add(new HTML("&nbsp;"), DockPanel.CENTER); // fill the rest of the row with blank space
        return panel;
    }


}
