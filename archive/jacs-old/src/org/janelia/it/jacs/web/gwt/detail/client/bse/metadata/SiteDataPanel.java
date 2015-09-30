
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabPanel;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;

import java.util.*;

/**
 * This class is responsible for managing the Site TitleBox contents in ReadDetail.
 *
 * @author Tareq Nabeel
 */
public class SiteDataPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteDataPanel");

    private SiteManager siteManager;
    private TitledBox siteMetaDataTitleBox;
    private LoadingLabel siteDataLoadingLabel;
    private List currentSites;
    private TabPanel sitesTabPanel;
    private ListBox sitesListBox;

    private static final String ADD_SCROLL = "add scroll";
    private static final String REMOVE_SCROLL = "remove scroll";
    private static final String ADD_TABS = "add tabs";
    private static final String REMOVE_TABS = "remove tabs";
    public static final String DEFAULT_SITE_SCROLL_OR_NOSCROLL_SELECTION = ADD_SCROLL;
    public static final String DEFAULT_SITE_TAB_OR_LIST_SELECTION = REMOVE_TABS;
    private String siteScrollOrNoScrollSelection = DEFAULT_SITE_SCROLL_OR_NOSCROLL_SELECTION;
    private String siteTabOrListSelection = DEFAULT_SITE_TAB_OR_LIST_SELECTION;
    private ActionLink addRemoveSiteScrollLink = new ActionLink(getSiteScrollOrNoScrollSelection(), new SiteScrollLinkListener());
    private ActionLink addRemoveSiteTabLink = new ActionLink(getSiteTabOrListSelection(), new SiteTabLinkListener());

    /**
     * This only creates the Site Metadata panel (and loading message) but does not fill it
     *
     * @param siteManager SiteManager instance
     */
    protected SiteDataPanel(SiteManager siteManager) {
        try {
            this.siteManager = siteManager;
            siteMetaDataTitleBox = new TitledBox("Site Metadata", true);
            siteMetaDataTitleBox.setStyleName("readDetailSiteDataBox");
            siteMetaDataTitleBox.setActionLinkBackgroundStyleName("tertiaryTitledBoxActionLinkBackground");
            createAndAddLoadingLabels();
        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel constructor caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Adds the loading message to the Site Metadata box
     */
    protected void createAndAddLoadingLabels() {
        siteDataLoadingLabel = new LoadingLabel("Loading site data...", true);
        siteMetaDataTitleBox.add(siteDataLoadingLabel);
    }

    /**
     * This method fills the contents of the Site Metadata panel.  There are several triggers for this method call:
     * a) Called when map creation/recreation is complete (MapCompleteListener onBusyDone)
     * i) Map is created when ReadDetail panel is displayed
     * ii) Map is recreated when a different Sample is chosen
     * b) Called when user toggles "add scroll / remove scroll"  option
     * c) Called when user toggles "add tab / remove tab" option
     *
     * @param previousSiteIndex needed for (b) and (c) to restore user's previous site selection
     */
    protected void recreateSiteDataPanel(int previousSiteIndex) {
        try {
            removeActionLinks();
            getSiteMetaDataTitleBox().clearContent();

            if (currentSites == null || currentSites.size() == 0) {
                MessageUtil.setNotFoundErrorMessage(getSiteMetaDataTitleBox(), "Site", "Sample " + siteManager.getCurrentSample().getSampleName());
                return;
            }

            //logger.debug("SiteManager recreateSiteDataPanel previousSiteIndex=" + previousSiteIndex);
            if (currentSites.size() == 1) {
                // No need to create
                getSiteMetaDataTitleBox().add(populateSiteTable((BioMaterial) currentSites.get(0)));
                showMapMarkerPopup(0);
                // addRemoveSiteTabLink is only relevant if there are multiple sites
                getSiteMetaDataTitleBox().addActionLink(addRemoveSiteScrollLink);
            }
            else {
                if (showingSiteTabs()) {
                    createSitesTabPanel();
                }
                else {
                    createSitesListPanel();
                }
                // Add the listener after all tabs/items have been added to avoid unnecessary map refreshes
                addSiteChangeListener();
                // previousSiteIndex could be -1 if Site data panel was being created for the first time
                if (previousSiteIndex == -1) {
                    selectSiteOption(0);
                }
                else {
                    selectSiteOption(previousSiteIndex);   // to restore user's previous selection
                }
                addActionLinks();
            }
            //logger.debug("SiteManager recreateSiteDataPanel after recreate getSiteIndex()=" + getSiteIndex());
        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel recreateSiteDataPanel caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Gets called when user clicks on a Map marker and when user selects different sites in a list box or tab panel
     *
     * @param index the index of the site to select
     */
    protected void selectSiteOption(int index) {
        try {
            if (currentSites.size() == 1) {
                showMapMarkerPopup(0);
                return;
            }
            //logger.debug("SiteManager selectSiteOption index=" + index);
            if (showingSiteTabs()) {
                getSitesTabPanel().selectTab(index);
            }
            else {
                //logger.debug("SiteManager calling sitesListBox.setSelectedIndex getSiteIndex()=" + getSiteIndex() + " going to index " + index);
                sitesListBox.setSelectedIndex(index); // does not trigger SiteChangeListener onChange event for some reason
                refreshSiteTable();
            }
        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel selectSiteOption caught exception " + e.getMessage());
            throw e;
        }
    }


    /**
     * This method is called when Map has been displayed.  We need to decide whether or not to switch to listbox
     * because of TabPanel's issues with horizontal scrolling when tab number is large.  We don't want to
     * make this decision in recreateSiteDataPanel because it would be too annoying to override user selection for tabs
     * when user clicks on addSroll for example.
     */
    protected void displayInitialSiteDataPanel() {
        if (showingSiteTabs() && currentSites != null && currentSites.size() > 4) {
            toggleSiteTabLink();      // toggleSiteTabLink will call recreateSiteDataPanel();
        }
        else {
            recreateSiteDataPanel(-1);
        }
    }

    /**
     * This method gets called if currentSites.size()>1 and user has chosen tabs.  It creates a tab panel, each tab containing a FlexTable containing
     * information for one site. If user has chosen scrolling, then the Flextable is wrapped with a scrolling panel.
     */
    private void createSitesTabPanel() {
        sitesTabPanel = new RoundedTabPanel();
        getSiteMetaDataTitleBox().add(sitesTabPanel);
        for (Iterator iterator = currentSites.iterator(); iterator.hasNext();) {
            BioMaterial site = (BioMaterial) iterator.next();
            sitesTabPanel.add(populateSiteTable(site), truncateSiteId(site.getMaterialAcc()));
        }
    }

    /**
     * This method gets called if currentSites.size()>1 and user has chosen against tabs.  It creates a FlexTable with the a listbox listing each site id
     * and site information for the current selected site in the listbox.  If user has chosen scrolling, then the Flextable is wrapped with a scrolling panel.
     */
    private void createSitesListPanel() {
        createSitesListBox();
        Widget listPanel = populateSiteTable((BioMaterial) currentSites.get(sitesListBox.getSelectedIndex()));
        getSiteMetaDataTitleBox().add(listPanel);
    }

    /**
     * Creates the listbox containing all the site ids for the currentSample
     */
    private void createSitesListBox() {
        sitesListBox = new ListBox();
        sitesListBox.setStyleName("readDetailSiteListBox");   // addStyleName doesn't work for some reason
        for (Iterator iterator = currentSites.iterator(); iterator.hasNext();) {
            BioMaterial site = (BioMaterial) iterator.next();
            sitesListBox.addItem(site.getMaterialAcc());
        }
        sitesListBox.setVisibleItemCount(1);
    }

    /**
     * Creates and populates a FlexTable with site data.  If user has chosen scrolling, then the Flextable is wrapped with a scrolling panel.
     *
     * @param site The Site to fill the FlexTable with
     * @return a FlexTable or a ScrollPanel wrapping the FlexTable
     */
    private Widget populateSiteTable(BioMaterial site) {
        try {
            RowIndex rowIndex = new RowIndex(0);
            HTMLTable siteDataTable = new FlexTable();
            if (showingSiteTabs() || currentSites.size() == 1) {
                TableUtil.addTextRow(siteDataTable, rowIndex, "site id", site.getMaterialAcc());
            }
            else {
                TableUtil.addWidgetRow(siteDataTable, rowIndex, "site id", sitesListBox);
                // Switch the label to align at cell bottom
                TableUtils.addCellStyle(siteDataTable, rowIndex.getCurrentRow() - 1, 0, "gridCellBottomForReal");
            }
            TableUtil.addTextRow(siteDataTable, rowIndex, "project", site.getProject());
//            TableUtil.addTextRow(siteDataTable, rowIndex, "Leg", site.getLeg());
//            TableUtil.addDateRow(siteDataTable, rowIndex, "Data Time", site.getDataTimestamp());
            if (site.getCollectionSite() != null) {
                TableUtil.addTextRow(siteDataTable, rowIndex, "location", site.getCollectionSite().getLocation());
                TableUtil.addTextRow(siteDataTable, rowIndex, "region", site.getCollectionSite().getRegion());
                TableUtil.addTextRow(siteDataTable, rowIndex, "comment", site.getCollectionSite().getComment());
                TableUtil.addTextRow(siteDataTable, rowIndex, "country", ((GeoPoint) site.getCollectionSite()).getCountry());
                TableUtil.addTextRow(siteDataTable, rowIndex, "latitude", ((GeoPoint) site.getCollectionSite()).getFormattedLatitude());
                TableUtil.addTextRow(siteDataTable, rowIndex, "longitude", ((GeoPoint) site.getCollectionSite()).getFormattedLongitude());
                TableUtil.addTextRow(siteDataTable, rowIndex, "habitat", site.getCollectionSite().getSiteDescription());
/*
                if (((GeoPoint) site.getCollectionSite()).getDepth()!=null)
                    TableUtil.addTextRow(siteDataTable, rowIndex, "sample depth", ((GeoPoint) site.getCollectionSite()).getDepth());
*/
            }
            if (site.getCollectionHost() != null) {
                TableUtil.addTextRow(siteDataTable, rowIndex, "host organism", site.getCollectionHost().getOrganism());
                TableUtil.addTextRow(siteDataTable, rowIndex, "host details", site.getCollectionHost().getHostDetails());
            }

            if (site.getCollectionStartTime() != null) {
                String startTime = (new FormattedDate(site.getCollectionStartTime().getTime())).toString();
                String stopTime = null;
                if (site.getCollectionStopTime() != null)
                    stopTime = (new FormattedDate(site.getCollectionStopTime().getTime())).toString();

                if (stopTime == null || startTime.equals(stopTime))
                    TableUtil.addTextRow(siteDataTable, rowIndex, "date collected", startTime);
                else
                    TableUtil.addTextRow(siteDataTable, rowIndex, "date collected", startTime.concat(" to ").concat(stopTime));
            }

            List obsKeyList = new ArrayList();
            Iterator obsIter = site.getObservations().keySet().iterator();
            while (obsIter.hasNext())
                obsKeyList.add(obsIter.next());
            Collections.sort(obsKeyList);
            obsIter = obsKeyList.iterator();
            String obsKey;
            while (obsIter.hasNext()) {
                obsKey = (String) obsIter.next();
                TableUtil.addTextRow(siteDataTable, rowIndex, obsKey.toLowerCase(), site.getObservationAsString(obsKey));
            }
            if (showingSiteScroll()) {
                ScrollPanel scrollPanel = new ScrollPanel(siteDataTable);
                scrollPanel.setStyleName("readDetailSiteTabScrollPanel");
                //logger.debug("SiteManager createSitePanel() returning scrollable tab with rowCount=" + siteDataTable.getRowCount());
                return scrollPanel;
            }
            else {
                //logger.debug("SiteManager createSitePanel() returning tab with rowCount=" + siteDataTable.getRowCount());
                return siteDataTable;
            }

        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel populateSiteTable caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Gets called when user selects a different site in the drop-down.  Relevant when user has chosen "remove tabs".
     */
    private void refreshSiteTable() {
        Widget siteDataWidget = populateSiteTable((BioMaterial) currentSites.get(sitesListBox.getSelectedIndex()));
        // for unknown reasons the table goes away
        getSiteMetaDataTitleBox().clearContent();
        getSiteMetaDataTitleBox().add(siteDataWidget);
        showMapMarkerPopup(sitesListBox.getSelectedIndex());
    }

    /**
     * Captures the site that the user has chosen
     */
    private class SiteChangeListener implements TabListener, ChangeListener {
        public boolean onBeforeTabSelected(SourcesTabEvents srcTabEvents, int tabIndex) {
            return true;
        }

        public void onTabSelected(SourcesTabEvents srcTabEvents, int tabIndex) {
            try {
                //logger.debug("SiteManager SiteChangeListener onTabSelected tabIndex " + tabIndex);
                showMapMarkerPopup(tabIndex);
            }
            catch (RuntimeException e) {
                logger.error("SiteDataPanel SiteChangeListener onTabSelected caught exception " + e.getMessage());
                throw e;
            }
        }

        public void onChange(Widget sender) {
            //logger.debug("SiteManager SiteChangeListener onChange sitesListBox.getSelectedIndex()=" + sitesListBox.getSelectedIndex());
            refreshSiteTable();
            // Don't call sitesListBox.setFocus from refreshSiteTable as that could have been triggered by user click on map
            sitesListBox.setFocus(true);
        }
    }

    /**
     * Displays the info window on the Google Map marker
     *
     * @param index the index of the site to pop-up the info window for
     */
    private void showMapMarkerPopup(int index) {
        try {
            Marker gMarker = siteManager.getPointManager().getMarker(index);
            String markerHtml = siteManager.getPointManager().getMarkerHtml(index);
            gMarker.showMapBlowup(new InfoWindowContent(markerHtml));
        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel showMapMarkerPopup caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Listener for the "add scroll / remove scroll" link on the Site MetaData Title box
     */
    private class SiteScrollLinkListener implements ClickListener {
        public void onClick(Widget widget) {
            toggleSiteScrollLink();
        }
    }

    /**
     * Listener for the "add tab / remove tab" link on the Site MetaData Title box
     */
    private class SiteTabLinkListener implements ClickListener {
        public void onClick(Widget widget) {
            toggleSiteTabLink();
        }
    }

    /**
     * Gets called when user clicks on "add scroll / remove scroll" link on the Site MetaData Title box.  Also, gets called
     * when currentSites.size()>4 because of bugs in GWT TabPanel (can't handle horizontal scrolling on IE)
     */
    private void toggleSiteScrollLink() {
        // If scroll bars exist in the tabs currently (i.e. showingSiteScroll()==true)
        // then set the link to say "Add Scroll".
        // refreshSiteMetaData() would then check showingSiteScroll() which would return false for "Add Scroll"
        // and that will cause scrolls to go away
        int previousSiteIndex = getSiteIndex();
        if (showingSiteScroll()) {
            setSiteScrollOrNoScrollSelection(ADD_SCROLL);
        }
        else {
            setSiteScrollOrNoScrollSelection(REMOVE_SCROLL);
        }
        recreateSiteDataPanel(previousSiteIndex);
    }

    /**
     * Gets called when user clicks on "add tab / remove tab" link on the Site MetaData Title box.
     */
    private void toggleSiteTabLink() {
        int previousSiteIndex = getSiteIndex();
        if (showingSiteTabs()) {
            setSiteTabOrListSelection(ADD_TABS);
        }
        else {
            setSiteTabOrListSelection(REMOVE_TABS);
        }
        recreateSiteDataPanel(previousSiteIndex);
    }

    /**
     * Used by ReadPagingPanel to maintain user selections between ReadDetail viewing on JobDetail page
     *
     * @return siteScrollOrNoScrollSelection
     */
    public String getSiteScrollOrNoScrollSelection() {
        return siteScrollOrNoScrollSelection;
    }

    /**
     * Used by ReadPagingPanel to maintain user selections between ReadDetail viewing on JobDetail page
     *
     * @param siteScrollOrNoScrollSelection "add scroll" or "remove scroll"
     */
    public void setSiteScrollOrNoScrollSelection(String siteScrollOrNoScrollSelection) {
        this.siteScrollOrNoScrollSelection = siteScrollOrNoScrollSelection;
        addRemoveSiteScrollLink.setText(siteScrollOrNoScrollSelection);
    }

    /**
     * Used by ReadPagingPanel to maintain user selections between ReadDetail viewing on JobDetail page
     *
     * @return siteTabOrListSelection
     */
    public String getSiteTabOrListSelection() {
        return siteTabOrListSelection;
    }

    /**
     * Used by ReadPagingPanel to maintain user selections between ReadDetail viewing on JobDetail page
     *
     * @param siteTabOrListSelection "add tab" or "remove tab"
     */
    public void setSiteTabOrListSelection(String siteTabOrListSelection) {
        this.siteTabOrListSelection = siteTabOrListSelection;
        addRemoveSiteTabLink.setText(siteTabOrListSelection);
    }

    /**
     * Adds listener so user site selection is tracked
     */
    private void addSiteChangeListener() {
        if (showingSiteTabs()) {
            sitesTabPanel.addTabListener(new SiteChangeListener());
        }
        else {
            sitesListBox.addChangeListener(new SiteChangeListener());
        }
    }

    /**
     * Truncates SCUMS_SITE_ARCTIC_CHUKCHISEA to ARCTIC_CHUKCHISEA to remove redundant information in the tab labels.
     *
     * @param siteId the actual SiteId
     * @return the truncated SiteId
     */
    public String truncateSiteId(String siteId) {
        //SCUMS_SITE_ARCTIC_CHUKCHISEA would be truncated to ARCTIC_CHUKCHISEA
        int indFrom = siteId.indexOf('_', siteId.indexOf('_') + 1) + 1;
        return siteId.substring(indFrom);
        //return siteId;
    }

    /**
     * @return true if we're scrolling on the Site Metadata box; false otherwise
     */
    private boolean showingSiteScroll() {
        // Link would give the option of "remove scroll" only if we're currently scrolling
        return (addRemoveSiteScrollLink != null && addRemoveSiteScrollLink.getText().equals(REMOVE_SCROLL));
    }

    /**
     * @return true if we're tabbing on the Site Metadata box; false otherwise
     */
    protected boolean showingSiteTabs() {
        // Link would give the option of "remove scroll" only if we're currently scrolling
        return (addRemoveSiteTabLink != null && addRemoveSiteTabLink.getText().equals(REMOVE_TABS));
    }

    /**
     * Adds the action links to Site Metadata box.
     */
    protected void addActionLinks() {
        getSiteMetaDataTitleBox().addActionLink(addRemoveSiteScrollLink);
        getSiteMetaDataTitleBox().addActionLink(addRemoveSiteTabLink);
    }

    /**
     * Removes the action links to Site Metadata box.
     */
    protected void removeActionLinks() {
        getSiteMetaDataTitleBox().removeActionLinks();
    }


    /**
     * @return the currentSample's sites as an ordered list
     */
    protected List getCurrentSites() {
        return currentSites;
    }

    /**
     * Set the currentSample's sites as an ordered list
     *
     * @param currentSites the currentSample's sites as an unordered set
     */
    protected void setCurrentSites(Set currentSites) {
        this.currentSites = getOrderedSites(currentSites);
    }

    /**
     * Orders the sites using Site's compareTo implementation
     *
     * @param sites the sites to order
     * @return sites as an ordered list
     */
    private List getOrderedSites(Set sites) {
        try {
            List sitesList = copySetItemsToList(sites);
            if (sitesList == null || sitesList.size() == 0)
                return sitesList;

            Collections.sort(sitesList);
            return sitesList;
        }
        catch (RuntimeException e) {
            logger.error("SiteDataPanel getOrderedSites caught exception " + e.getMessage());
            throw e;
        }
    }

    private List copySetItemsToList(Set items) {
        if (items == null) {
            return null;
        }
        List copy = new ArrayList();
        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            copy.add(iterator.next());
        }
        return copy;
    }

    private int getSiteIndex() {
        if (showingSiteTabs()) {
            return getSiteTabIndex();
        }
        else {
            return getSiteListIndex();
        }
    }

    private int getSiteListIndex() {
        return (sitesListBox == null) ? -1 : sitesListBox.getSelectedIndex();
    }

    private int getSiteTabIndex() {
        return (sitesTabPanel == null || sitesTabPanel.getTabBar() == null) ? -1 : sitesTabPanel.getTabBar().getSelectedTab();
    }

    public TitledBox getSiteMetaDataTitleBox() {
        return siteMetaDataTitleBox;
    }

    protected LoadingLabel getSiteDataLoadingLabel() {
        return siteDataLoadingLabel;
    }

    protected TabPanel getSitesTabPanel() {
        return this.sitesTabPanel;
    }
}
