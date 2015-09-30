
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TertiaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.list.MultiSelectListTables;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabPanel;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DownloadableDataNodeFilter;
import org.janelia.it.jacs.web.gwt.download.client.formatter.GeographicLocationsFilter;
import org.janelia.it.jacs.web.gwt.download.client.formatter.PublicationFormatter;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PublicationPanelHelper {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.PublicationPanelHelper");

    public Widget createPublicationPanel(Publication publication, Project project) {
        _logger.debug("Create publication panel for " + publication.getAccessionNumber());
        RoundedTabPanel publicationPanel = new RoundedTabPanel();
        populatePublicationTabPanels(publicationPanel, publication, project);
        return publicationPanel;
    }

    public Widget populatePublicationTabPanels(TabPanel parentPanel,
                                               Publication publication,
                                               Project project) {
        PublicationFormatter formatter = new PublicationFormatter();
        return populatePublicationTabPanels(parentPanel, publication, project, formatter);
    }

    public Widget populatePublicationTabPanels(TabPanel parentPanel,
                                               Publication publication,
                                               Project project,
                                               PublicationFormatter formatter) {
        parentPanel.add(getDetailsPanelForPublication(formatter,
                publication,
                project,
                parentPanel),
                "Details");
        if (publication.getDataFiles() != null && publication.getDataFiles().size() > 0) {
            parentPanel.add(getDownloadsPanelForPublication(formatter, publication), "Downloads");
        }
        parentPanel.selectTab(0);
        return parentPanel;
    }

    /**
     * Setup a vertical panel to show the details of the model.
     *
     * @param formatter object to display fully-formatted paper fields.
     * @return widget to show that.
     */
    Widget getDetailsPanelForPublication(PublicationFormatter formatter,
                                         Publication pub,
                                         Project project,
                                         TabPanel parentPanel) {
        String html = new StringBuffer()
                .append("<span id='publicationTitle'></span>")
                .append("<span class='MoreInfoLinksBox' id='publicationLinks'></span>")
                .append("<span id='publicationDescription'></span>")
                .toString();

        HTMLPanel panel = new HTMLPanel(html);
        panel.add(formatter.getTitle(pub), "publicationTitle");
        panel.add(getPublicationLinksPanel(pub, project, parentPanel),
                "publicationLinks");
        panel.add(HtmlUtils.getHtml(pub.getDescription(), "text"),
                "publicationDescription");
        return panel;
    }

    /**
     * Setup a vertical panel to show the downloadables from the model.
     *
     * @param formatter object to display fully-formatted paper fields.
     * @return widget to show that.
     */
    Widget getDownloadsPanelForPublication(PublicationFormatter formatter,
                                           Publication pub) {
        VerticalPanel publicationPanel = new VerticalPanel();

        Panel pubTitle = formatter.getTitle(pub);
        publicationPanel.add(pubTitle);

        // special  spacer since download sections have negative margine
        publicationPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        DownloadBox downloadBox = new DownloadBox("Publication Downloads");
        downloadBox.setWidth("100%");
        downloadBox.addFileAsExternalLink(pub.getSubjectDocument(),
                "Publication",
                new DownloadPubListener(pub));
        downloadBox.addFiles(pub.getRolledUpDataArchives());
        publicationPanel.add(downloadBox);

        publicationPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        GeographicLocationsFilter filter = new GeographicLocationsFilter();

        DownloadBox filterPanel =
                new SecondaryDownloadBox("Filters",
                        null, /* list of files */
                        true, /*actionlink*/
                        false /*show content*/);

        filterPanel.setWidth("100%");

        List allGeographicLocations = getAllGeographicLocations(pub.getDataFiles());
        LocationAddRemoveListener nonGenericCallback = null;

        if (allGeographicLocations != null && allGeographicLocations.size() > 0) {
            // Create the double list thing
            MultiSelectListTables list =
                    new MultiSelectListTables("Available Geographic Areas",
                            "Selected Geographic Areas");
            list.setAvailableList(allGeographicLocations);

            // Set up a listener to be notified of select and unselect events
            nonGenericCallback =
                    new LocationAddRemoveListener(list.getSelectedListBox(),
                            filter,
                            null,
                            pub,
                            publicationPanel,
                            formatter);
            list.addSelectionListener(nonGenericCallback); // dataPanel set below

            filterPanel.add(list);
        }
        else {
            Label noFiltersLabel = new Label("No filters available");
            noFiltersLabel.setStyleName("text");
            filterPanel.add(noFiltersLabel);
        }

        VerticalPanel dataPanelContents = swapDataPanel(pub, publicationPanel, formatter, filter, null, filterPanel);
        if (nonGenericCallback != null) {
            nonGenericCallback.setDataPanelContents(dataPanelContents);
        }

        return publicationPanel;
    }

    private Panel getPublicationLinksPanel(Publication pub,
                                           Project project,
                                           final TabPanel parentPanel) {
        TertiaryTitledBox infoBox = new TertiaryTitledBox("More Information", false);
        FlexTable grid = new FlexTable();
        int row = 0;
        if (pub.getSubjectDocument() != null &&
                pub.getSubjectDocument().getLocation() != null) {
            grid.setWidget(row++, 0,
                    TableUtils.getLinkCell(getDownloadPublicationLink("Download this publication", pub)));
        }
        if (pub.getDataFiles() != null && pub.getDataFiles().size() > 0) {
            grid.setWidget(row++, 0,
                    TableUtils.getLinkCell(getDownloadDataLink("Download publication data", parentPanel)));
        }
        if (project != null) {
            grid.setWidget(row++, 0,
                    TableUtils.getLinkCell(new Link("Browse the project", getProjectUrlBySymbol(project.getProjectSymbol()))));
            grid.setWidget(row++, 0,
                    TableUtils.getLinkCell(new Link("Browse project samples", getSamplesUrlByProjectSymbol(project.getProjectSymbol()))));
            if (project.getWebsite() != null) {
                grid.setWidget(row, 0,
                        TableUtils.getLinkCell(new ExternalLink("View the project website", project.getWebsite())));
            }
        }
        infoBox.add(grid);
        return infoBox;
    }

    private class DownloadPubListener implements ClickListener {

        private Publication pub;

        private DownloadPubListener(Publication pub) {
            this.pub = pub;
        }

        public void onClick(Widget widget) {
            SystemWebTracker.trackActivity("DownloadPublication",
                    new String[]{
                            pub.getAccessionNumber()
                    });
        }

    }

    private class DownloadPubDataListener implements TreeListener {

        private Publication pub;

        private DownloadPubDataListener(Publication pub) {
            this.pub = pub;
        }

        public void onTreeItemSelected(TreeItem treeItem) {
            DownloadableDataNode dataNode =
                    (DownloadableDataNode) treeItem.getUserObject();
            SystemWebTracker.trackActivity("DownloadPublicationData",
                    new String[]{
                            pub.getAccessionNumber(),
                            dataNode.getLocation()
                    });
        }

        public void onTreeItemStateChanged(TreeItem treeItem) {
        }

    }

    private Widget getDownloadPublicationLink(String linkText, final Publication pub) {
        ExternalLink downloadPubLink =
                new ExternalLink(linkText, pub.getSubjectDocument().getLocation());
        downloadPubLink.addClickListener(new DownloadPubListener(pub));
        return downloadPubLink;
    }

    private Widget getDownloadDataLink(String linkText, final TabPanel panel) {
        HTML html = HtmlUtils.getHtml(linkText, "textLink");
        html.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                panel.selectTab(1);
            }
        });
        return html;
    }

    String getSamplesUrlByProjectSymbol(String projectSymbol) {
        return "/jacs/gwt/ProjectSamplesPage/ProjectSamplesPage.oa?projectSymbol=" + projectSymbol;
    }

    /**
     * Retrieve the project's page URL
     *
     * @param projectSymbol
     * @return
     */
    String getProjectUrlBySymbol(String projectSymbol) {
        return "/jacs/gwt/BrowseProjectsPage/BrowseProjectsPage.oa?projectSymbol=" + projectSymbol;
    }

    /**
     * Have to fill up the blank space with something other than the external link or else the double
     * underline will expand past the text
     */


    /**
     * Given the data files, sift through and find the list of ALL geographic
     * locations associated with them. Recurses through child nodes, as well.
     *
     * @param dataFiles data file trees.
     * @return list of all locations--strings.
     */
    private List getAllGeographicLocations(List dataFiles) {
        if (dataFiles == null || dataFiles.size() == 0)
            return null;

        HashSet uniqueLocations = new HashSet();
        for (int i = 0; i < dataFiles.size(); i++) {
            DownloadableDataNode nextNode = (DownloadableDataNode) dataFiles.get(i);
            if (nextNode != null && nextNode.getSite() != null && nextNode.getSite().getGeographicLocation() != null)
                uniqueLocations.add(nextNode.getSite().getGeographicLocation());

            if (nextNode != null && nextNode.getChildren() != null)
                uniqueLocations.addAll(getAllGeographicLocations(nextNode.getChildren()));
        }

        return new ArrayList(uniqueLocations);
    }

    private VerticalPanel swapDataPanel(
            Publication pub,
            VerticalPanel publicationPanel,
            PublicationFormatter formatter,
            DownloadableDataNodeFilter filter,
            VerticalPanel dataPanelContents,
            Panel filterPanel) {

        // First time, create and show the Download box
        if (dataPanelContents == null) {
            // Create separate Panel for download box contents so it can be swapped out when filters change
            dataPanelContents = new VerticalPanel();
            formatter.getDataFiles(dataPanelContents, pub, filter, new DownloadPubDataListener(pub));

            DownloadBox downloadBox = new DownloadBox("Publication Data Downloads", null, /*actionlink*/ false, /*show content*/true);
            downloadBox.setWidth("100%");
            downloadBox.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
            downloadBox.add(filterPanel);
            downloadBox.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
            downloadBox.add(dataPanelContents);

            publicationPanel.add(downloadBox);
        }
        else { // subsequent times, fade the contents out and fade in the new contents
            VerticalPanel newContentsPanel = new VerticalPanel();
            formatter.getDataFiles(newContentsPanel, pub, filter, new DownloadPubDataListener(pub));

            fadeDataPanels(dataPanelContents, newContentsPanel);
            dataPanelContents = newContentsPanel;
        }

        return dataPanelContents;
    }

    private void fadeDataPanels(final Panel oldPanel, final Panel newPanel) {
        final Panel downloadBox = (Panel) oldPanel.getParent();

        // Step 3 - fade in the new panel
        final Callback visiblityFinished = new Callback() {
            public void execute() {
                // Add the (transparent) panel to the parent and fade it in
                downloadBox.add(newPanel);
                SafeEffect.fade(newPanel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.2")
                });
                oldPanel.removeFromParent();
            }
        };

        // Step 2 - set the new panel to slightly transparent
        Callback fadeOutFinished = new Callback() {
            public void execute() {
                SafeEffect.fade(newPanel, new EffectOption[]{
                        new EffectOption("to", "0.01")
                        , new EffectOption("duration", "0")
                        , new EffectOption("afterFinish", visiblityFinished)
                });
            }
        };

        // Step 1 - fade out the current panel contents
        SafeEffect.fade(oldPanel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.2")
                , new EffectOption("afterFinish", fadeOutFinished)
        });
    }

    /**
     * Given a table that has selections for filtering, change the set of things against
     * which TO filter.
     *
     * @param filter has "opt in" list.
     * @param table  has selections for opt-in by user.
     */
    private void resetFilter(GeographicLocationsFilter filter, SortableTable table) {
        List newFilterList = new ArrayList();
        for (int i = 1; i < table.getRowCount(); i++) {
            String value = table.getText(i, 0);
            if (value != null) {
                newFilterList.add(value);
            }
        }
        filter.setLocations(newFilterList);
    }

    /**
     * Listener to table-swapping listener.  This handles non-generic behavior for that listener.
     */
    private class LocationAddRemoveListener implements SelectionListener {
        private SortableTable _receivingTable;
        private VerticalPanel _dataPanel;
        private GeographicLocationsFilter _filter;
        private VerticalPanel _publicationPanel;
        private Publication _pub;
        private PublicationFormatter _formatter;

        public LocationAddRemoveListener(
                SortableTable receivingTable,
                GeographicLocationsFilter filter,
                VerticalPanel dataPanel,
                Publication pub,
                VerticalPanel publicationPanel,
                PublicationFormatter formatter) {

            _receivingTable = receivingTable;
            _filter = filter;
            _dataPanel = dataPanel;
            _publicationPanel = publicationPanel;
            _pub = pub;
            _formatter = formatter;
        }

        /**
         * Exposure of setter to support delayed dependency injectiong (after construction).
         *
         * @param dataPanel
         */
        public void setDataPanelContents(VerticalPanel dataPanel) {
            _dataPanel = dataPanel;
        }

        public void onSelect(String value) {
            // Reset the filter.
            resetFilter(_filter, _receivingTable);
            // Swap the old data panel for the newly-filtered one.
            _dataPanel = swapDataPanel(_pub, _publicationPanel, _formatter, _filter, _dataPanel, null);
        }

        public void onUnSelect(String value) {
            // Reset the filter.
            resetFilter(_filter, _receivingTable);
            // Swap the old data panel for the newly-filtered one.
            _dataPanel = swapDataPanel(_pub, _publicationPanel, _formatter, _filter, _dataPanel, null);
        }
    }
}
