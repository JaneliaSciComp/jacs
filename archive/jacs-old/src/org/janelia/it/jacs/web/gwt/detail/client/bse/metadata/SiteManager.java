
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;
import org.janelia.it.jacs.web.gwt.download.client.DownloadBox;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.DownloadSampleFileClickListener;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class manages Site locations and their corresponding sites and samples.  It also
 * manages the Google map markers that correspond to the site locations
 *
 * @author Tareq Nabeel
 */
public class SiteManager {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteManager");

    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    private PointManager pointManager;
    private DownloadBox sampleDataDownloadsPanel;
    private SampleDataPanel sampleDataPanel;
    private SiteDataPanel siteDataPanel;
    private MapPanel mapPanel;
    private List downloadableSampleAccessions = new ArrayList();
    private HorizontalPanel sampleDataAndMapPanel;
    private boolean delayAddingMapBox = false;
    private VerticalPanel sampleDownloadsAndMapBoxes;

    /**
     * This only creates the Sample, Site, and Map panels (and loading messages)
     */
    public SiteManager() {
        try {
            siteDataPanel = new SiteDataPanel(this);
            sampleDataPanel = new SampleDataPanel(this);
            sampleDataDownloadsPanel = new DownloadBox("Sample Data Download",
                    null, /* actionLink */
                    false, /* showActionLink */
                    false /* showContent */);
            mapPanel = new MapPanel(this);
            pointManager = new PointManager(this);
        }
        catch (RuntimeException e) {
            logger.error("SiteManager constructor caught exception " + e.getMessage());
            throw e;
        }
    }

    public void setDelayAddingMapBox(boolean delay) {
        delayAddingMapBox = delay;
    }

    public Panel createFullSampleDataPanel() {
        sampleDataAndMapPanel = new HorizontalPanel();
        sampleDataAndMapPanel.setStyleName("detailSampleDataAndMapPanel");
        VerticalPanel siteAndSampleBoxes = new VerticalPanel();
        siteAndSampleBoxes.add(sampleDataPanel.getSampleMetaDataTitleBox());
        siteAndSampleBoxes.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        siteAndSampleBoxes.add(siteDataPanel.getSiteMetaDataTitleBox());
        sampleDownloadsAndMapBoxes = new VerticalPanel();
        sampleDownloadsAndMapBoxes.add(sampleDataDownloadsPanel);
        sampleDownloadsAndMapBoxes.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        if (!delayAddingMapBox)
            sampleDownloadsAndMapBoxes.add(mapPanel.getGoogleMapBox());
        sampleDataAndMapPanel.add(siteAndSampleBoxes);
        sampleDataAndMapPanel.add(sampleDownloadsAndMapBoxes);
        return sampleDataAndMapPanel;
    }

    public HorizontalPanel getSampleDataAndMapPanel() {
        return sampleDataAndMapPanel;
    }

    public void addMapBox() {
        if (sampleDownloadsAndMapBoxes != null) {
            sampleDownloadsAndMapBoxes.add(mapPanel.getGoogleMapBox());
            mapPanel.getGoogleMapBox().reset();
            Set markers = pointManager.createSiteMarkers(siteDataPanel.getCurrentSites());
            mapPanel.recreateGoogleMap(markers);
        }
    }

    /**
     * This gets called once by ReadPanel.  It retrievs the samples from the database.  Population of sample
     * data and subsequent auto selection of a Sample tab causes recreateSiteMapPanels() to be called which
     * renders map and site data
     */
    public void retrieveAndDisplayDataByBSEntityAcc(String acc) {
        try {
            sampleDataPanel.fetchAndDisplayDataByEntityAcc(acc);
        }
        catch (RuntimeException e) {
            logger.error("SiteManager fetchAndDisplayDataByEntityAcc caught exception " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * This gets called once to retrieve the samples from the database using a sample accession.  Population of sample
     * data and subsequent auto selection of a Sample tab causes recreateSiteMapPanels() to be called which
     * renders map and site data
     */
    public void retrieveAndDisplayDataBySampleAcc(String sampleAcc) {
        try {
            sampleDataPanel.fetchAndDisplayDataBySampleAcc(sampleAcc);
        }
        catch (RuntimeException e) {
            logger.error("SiteManager fetchAndDisplayDataBySampleName caught exception", e);
            throw e;
        }
    }

    /**
     * This gets called once to retrieve the samples from the database using a sample name.  Population of sample
     * data and subsequent auto selection of a Sample tab causes recreateSiteMapPanels() to be called which
     * renders map and site data
     */
    public void retrieveAndDisplayDataBySampleName(String sampleName) {
        try {
            sampleDataPanel.fetchAndDisplayDataBySampleName(sampleName);
        }
        catch (RuntimeException e) {
            logger.error("SiteManager fetchAndDisplayDataBySampleName caught exception", e);
            throw e;
        }
    }

    /**
     * This gets called on Sample data tab selection.  Map creation completion triggers site data display
     */
    protected void recreateSiteMapPanels() {
        try {
            siteDataPanel.getSiteMetaDataTitleBox().clearContent();
            // siteDataPanel.getSiteDataLoadingLabel().setVisible(true);  just doesn't work
            siteDataPanel.createAndAddLoadingLabels();
            mapPanel.getGoogleMapBox().reset();
            Set markers = pointManager.createSiteMarkers(siteDataPanel.getCurrentSites());
            mapPanel.recreateGoogleMap(markers);
            //recreateSiteDataPanel(); Call this after map is complete to take advantage of Site Tab/Change listeners events
        }
        catch (RuntimeException e) {
            logger.error("SiteManager recreateSiteMapPanels caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve and populate the downloadable files for the given sample accession
     *
     * @param sampleAcc
     */
    protected void retrieveDownloadableDataBySampleAcc(final String sampleAcc) {
        if (downloadableSampleAccessions.contains(sampleAcc)) {
            return; // We were adding the sample sample twice
        }
        downloadService.getDownloadableFilesBySampleAcc(sampleAcc, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                logger.error("Error retrieving downloadable files for " + sampleAcc, throwable);
                MessageUtil.addServiceErrorMessage(sampleDataDownloadsPanel, "Data files", sampleAcc);
            }

            public void onSuccess(Object result) {
                List downloadableFiles = (List) result;
                for (Iterator dataFileItr = downloadableFiles.iterator(); dataFileItr.hasNext();) {
                    DownloadableDataNode dataFile = (DownloadableDataNode) dataFileItr.next();
                    sampleDataDownloadsPanel.add(new Link(DataFileFormatter.getDescriptiveText(dataFile),
                            new DownloadSampleFileClickListener(sampleAcc, dataFile)));
                    downloadableSampleAccessions.add(sampleAcc);
                }
            }
        });
    }

    /**
     * Gets called from MapPanel when user clicks on a map marker
     *
     * @param index of the site tab index or site list index
     */
    protected void selectSiteOption(int index) {
        siteDataPanel.selectSiteOption(index);
    }

    /**
     * Gets called when map creation is done for the first time
     */
    protected void displayInitialSiteDataPanel() {
        siteDataPanel.displayInitialSiteDataPanel();
    }

    /**
     * The list of sites for the currently displayed sample.  This is an ordered list.
     * sample.getSites() is unordered.
     *
     * @return list of sites for the currently displayed sample
     */
    protected List getCurrentSites() {
        return siteDataPanel.getCurrentSites();
    }

    /**
     * Gets called by SampleDataPanel when user changes sample selection, for example
     *
     * @param sample the current sample
     */
    protected void setCurrentSample(Sample sample) {
        siteDataPanel.setCurrentSites(sample.getBioMaterials());
    }

    /**
     * @return the currently displayed sample
     */
    public Sample getCurrentSample() {
        return sampleDataPanel.getCurrentSample();
    }

    public SiteDataPanel getSiteDataPanel() {
        return siteDataPanel;
    }

    public SampleDataPanel getSampleDataPanel() {
        return sampleDataPanel;
    }

    public DownloadBox getSampleDataDownloadsPanel() {
        return sampleDataDownloadsPanel;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public PointManager getPointManager() {
        return pointManager;
    }

    /**
     * Adds data retrieval error message to Sample, Site, and Map panels
     *
     * @param entityLabel The entity label (e.g. "Read"/"Library") for which we're retrieving data
     * @param entityId    The entity value (e.g. "Read"/"Library") for which we're retrieving data
     */
    protected void addServiceErrorMessage(String entityLabel, String entityId) {
        MessageUtil.setServiceErrorMessage(sampleDataPanel.getSampleMetaDataTitleBox(), "Sample", entityLabel + entityId);
        MessageUtil.setServiceErrorMessage(siteDataPanel.getSiteMetaDataTitleBox(), "Site", entityLabel + entityId);
        mapPanel.setMapError();
    }

    /**
     * Adds data retrieval error message to Sample, Site, and Map panels after clearing the panels
     *
     * @param entityLabel The entity label (e.g. "Read"/"Library") for which we're retrieving data
     * @param entityId    The entity value (e.g. "Read"/"Library") for which we're retrieving data
     */
    public void setServiceErrorMessage(String entityLabel, String entityId) {
        sampleDataPanel.getSampleMetaDataTitleBox().clearContent();
        siteDataPanel.getSiteMetaDataTitleBox().clearContent();
        mapPanel.setMapError();
        addServiceErrorMessage(entityLabel, entityId);
    }

    /**
     * Adds entity not found error message to Sample, Site, and Map panels
     *
     * @param entityLabel The entity label (e.g. "Read"/"Library") for which we're retrieving data
     * @param entityId    The entity value (e.g. "Read"/"Library") for which we're retrieving data
     */
    protected void addNotFoundErrorMessage(String entityLabel, String entityId) {
        MessageUtil.addNotFoundErrorMessage(sampleDataPanel.getSampleMetaDataTitleBox(), "Sample", entityLabel + entityId);
        MessageUtil.addNotFoundErrorMessage(siteDataPanel.getSiteMetaDataTitleBox(), "Site", entityLabel + entityId);
        mapPanel.setMapNoDataMessage();
    }

    /**
     * Adds entity not found error message to Sample, Site, and Map panels after clearing the panels
     *
     * @param entityLabel The entity label (e.g. "Read"/"Library") for which we're retrieving data
     * @param entityId    The entity value (e.g. "Read"/"Library") for which we're retrieving data
     */
    public void setNotFoundErrorMessage(String entityLabel, String entityId) {
        sampleDataPanel.getSampleMetaDataTitleBox().clearContent();
        siteDataPanel.getSiteMetaDataTitleBox().clearContent();
        addNotFoundErrorMessage(entityLabel, entityId);
    }

}
