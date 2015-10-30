
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.PerfStats;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;

import java.util.Iterator;

/**
 * This class is responsible for managing the Sample TitleBox contents in ReadDetail.
 *
 * @author Tareq Nabeel
 */
public class SampleDataPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SampleDataPanel");

    private SiteManager siteManager;
    private TitledBox sampleMetaDataTitleBox;
    private LoadingLabel sampleDataLoadingLabel;
    private Sample currentSample;
    private String acc;
    private boolean samplesPopulatedFlag = false;

    private static BSEntityServiceAsync bsEntityService = (BSEntityServiceAsync) GWT.create(BSEntityService.class);

    static {
        ((ServiceDefTarget) bsEntityService).setServiceEntryPoint("bsDetail.srv");
    }

    /**
     * This only creates the Sample Metadata panel (and loading message) but does not fill it
     *
     * @param siteManager SiteManager instance
     */
    protected SampleDataPanel(SiteManager siteManager) {
        try {
            this.siteManager = siteManager;
            sampleMetaDataTitleBox = new TitledBox("Sample Metadata", true);
            sampleMetaDataTitleBox.setStyleName("readDetailSampleDataBox");
            sampleMetaDataTitleBox.setActionLinkBackgroundStyleName("tertiaryTitledBoxActionLinkBackground");
            createAndAddLoadingLabels();
        }
        catch (Exception e) {
            logger.error("SampleDataPanel constructor caught exception " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isSamplesPopulated() {
        return samplesPopulatedFlag;
    }

    /**
     * @param sample The sample to display information for
     */
    public void displaySampleData(Sample sample) {
        try {
            getSampleDataLoadingLabel().setVisible(false);
            if (currentSample == null || !currentSample.getSampleAcc().equals(sample.getSampleAcc())) {
                setCurrentSample(sample);
                createSamplePanel();
            }
        }
        catch (Exception e) {
            logger.error("SampleDataPanel displayData caught exception " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds the loading message to the Sample Metadata box
     */
    private void createAndAddLoadingLabels() {
        sampleDataLoadingLabel = new LoadingLabel("Loading sample data...", true);
        sampleMetaDataTitleBox.add(sampleDataLoadingLabel);
    }

    /**
     * Retrieves sample data
     *
     * @param acc the accession of the entity for which to retrieve samples for
     */
    protected void fetchAndDisplayDataByEntityAcc(String acc) {
        try {
            PerfStats.start("GetSamplesByReadCallback");
            logger.debug("Calling bsEntityService.getSamplesByReadAcc");
            this.acc = acc;
            bsEntityService.getEntitySampleByAcc(acc, new GetSamplesCallback());
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("SampleDataPanel getSamples(Read read) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves sample data by sample accession
     */
    protected void fetchAndDisplayDataBySampleAcc(String sampleAcc) {
        try {
            PerfStats.start("GetSamplesBySampleAccession");
            logger.debug("Calling bsEntityService.getSamplesBySampleAcc");
            this.acc = sampleAcc;
            bsEntityService.getSampleBySampleAcc(sampleAcc, new GetSamplesCallback());
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("SampleDataPanel getSamples(Read read) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves sample data by sample name
     */
    protected void fetchAndDisplayDataBySampleName(String sampleName) {
        try {
            PerfStats.start("GetSamplesByReadCallback");
            logger.debug("Calling bsEntityService.getSamplesBySampleName");
            this.acc = sampleName;
            bsEntityService.getEntitySampleByName(acc, new GetSamplesCallback());
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("SampleDataPanel getSamples(Read read) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method retrieves the Sample model instances for the Library Id of the retrieved Read instance.
     * Its onSuccess call displays Sample data
     */
    private class GetSamplesCallback implements AsyncCallback {
        public void onFailure(Throwable throwable) {
            logger.error("GetSamplesCallback failed: " + throwable.getMessage(), throwable);
            PerfStats.end("GetSamplesCallback");
            getSampleDataLoadingLabel().setVisible(false);
            siteManager.setServiceErrorMessage("Entity Acc: ", String.valueOf(acc));
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("GetSamplesCallback succeeded ");
                PerfStats.end("GetSamplesCallback");
                Sample sample = (Sample) result;
                if (sample != null) {
                    samplesPopulatedFlag = true;
                    logger.debug("GetSamplesCallback received " + sample.getSampleName());
                    //siteManager.getMapPanel().getGoogleMapBox().setVisible(false);
                    displaySampleData(sample);
                    //siteManager.getMapPanel().getGoogleMapBox().setVisible(true);
                }
                else {
                    siteManager.setNotFoundErrorMessage("Entity Acc: ", String.valueOf(acc));
                }
            }
            catch (RuntimeException e) {
                logger.error("SampleDataPanel GetSamplesCallback onSuccess caught exception:" + e.getMessage(), e);
                throw e;
            }
        }
    }

    private void setCurrentSample(Sample sample) {
        currentSample = sample;
        siteManager.setCurrentSample(currentSample);
    }

    /**
     * This method creates a Sample TabPanel (readSamplesSet.size() > 1) or Sample table.  Both the Sample TabPanel and Table
     * creation would cause re-creation of Site and Map panels.
     */
    private void createSamplePanel() {
        try {
            getSampleMetaDataTitleBox().clearContent();
            Widget samplePanel = createSampleTable(currentSample);
            samplePanel.setStyleName("readDetailSampleDataPanel");
            getSampleMetaDataTitleBox().add(samplePanel);
            siteManager.retrieveDownloadableDataBySampleAcc(currentSample.getSampleAcc());
        }
        catch (Exception e) {
            logger.error("SampleDataPanel createSamplePanel caught exception " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This method creates a Sample FlexTable (readSamplesSet.size() == 1).  It causes re-creation of Site and Map panels.
     *
     * @return an instance of FlexTable representing one sample
     */
    private Widget createSampleTable(Sample sample) {
        HTMLTable sampleDataTable = new FlexTable();
        populateSampleDataTable(sampleDataTable, sample);
        siteManager.recreateSiteMapPanels();
        return sampleDataTable;
    }

    /**
     * Populates a FlexTable representing one sample
     *
     * @param sampleDataTable the FlexTable to fill
     * @param sample          the sample to fill the FlexTable with
     * @return a FlexTable representing one sample
     */
    private HTMLTable populateSampleDataTable(HTMLTable sampleDataTable, Sample sample) {
        try {
            RowIndex rowIndex = new RowIndex(0);
            TableUtil.addTextRow(sampleDataTable, rowIndex, "Sample", sample.getTitle());
            TableUtil.addTextRow(sampleDataTable, rowIndex, "Accession", sample.getSampleAcc());
            Iterator bmIter = sample.getBioMaterials().iterator();
            while (bmIter.hasNext()) {
                TableUtil.addTextRow(sampleDataTable, rowIndex, "Site Id", ((BioMaterial) bmIter.next()).getMaterialAcc());
            }

            String filterSize = null;
            if (sample.getFilterMax() == sample.getFilterMin()) {
                filterSize = String.valueOf(sample.getFilterMax());
            }
            else {
                filterSize = sample.getFilterMin() + " - " + sample.getFilterMax() + " \u00B5m";
            }
            if (filterSize != "null")
                TableUtil.addTextRow(sampleDataTable, rowIndex, "Filter Size", filterSize);
            return sampleDataTable;
        }
        catch (RuntimeException e) {
            logger.error("SampleDataPanel populateSampleDataTable caught exception " + e.getMessage());
            throw e;
        }
    }

    public TitledBox getSampleMetaDataTitleBox() {
        return sampleMetaDataTitleBox;
    }

    protected LoadingLabel getSampleDataLoadingLabel() {
        return sampleDataLoadingLabel;
    }

    protected Sample getCurrentSample() {
        return currentSample;
    }

}
