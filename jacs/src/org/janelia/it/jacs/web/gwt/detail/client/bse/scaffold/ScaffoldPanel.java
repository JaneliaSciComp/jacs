
package org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.genomics.PeptideDetail;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabPanel;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteManager;

/**
 * The class encapsulates the data and operations needed to render ScaffoldPanel
 *
 * @author Tareq Nabeel
 * @author Christian Goina
 */
public class ScaffoldPanel extends BSEntityPanel {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldPanel");

    private static final String DETAIL_TYPE = "Scaffold";

    private TabPanel featuresTabPanel;
    private SiteManager scaffoldReadSiteManager;
    private SiteManager scaffoldPeptideSiteManager;
    private TitledBox currentMetadataTitledBox;
    private TitledBox scaffoldMetadataTitledBox;
    private TitledBox scaffoldPeptideMetadataTitledBox;
    private TitledBox scaffoldReadMetadataTitledBox;
    private ScaffoldReadsPanelBuilder scaffoldReadsPanelBuilder;
    private ScaffoldPeptidesPanelBuilder scaffoldPeptidesPanelBuilder;

    private static BSEntityServiceAsync bsEntityService = (BSEntityServiceAsync) GWT.create(BSEntityService.class);

    static {
        ((ServiceDefTarget) bsEntityService).setServiceEntryPoint("bsDetail.srv");
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }

    protected void createDetailSpecificPanels() {
        try {
            addSpacer(this);
            createScaffoldFeaturesTabPanel();
            // Streamline the requests or it will take longer
            new CreateScaffoldReadsPanelTimer().schedule(500);
            new CreateScaffoldPeptidesPanelTimer().schedule(700);
            new AddFeatureTabPanelListenerTimer().schedule(1500);
            new CreateScaffoldMetadataTimer().schedule(2000);
        }
        catch (Exception e) {
            throw new RuntimeException("createDetailSpecificPanels caught " + e.getMessage());
        }
    }

    private class CreateScaffoldReadsPanelTimer extends Timer {
        public void run() {
            createScaffoldReadsPanel();
        }
    }

    private class CreateScaffoldPeptidesPanelTimer extends Timer {
        public void run() {
            createScaffoldPeptidesPanel();
        }
    }

    private class AddFeatureTabPanelListenerTimer extends Timer {
        public void run() {
            addFeatureTabPanelListener();
        }
    }

    private class CreateScaffoldMetadataTimer extends Timer {
        public void run() {
            displayScaffoldMetadata();
        }
    }

    private class ScaffoldReadSelectedListener implements EntityListener {
        public void onEntitySelected(String entityId, Object entityData) {
            Sample sample = extractReadSample((ScaffoldReadAlignment) entityData);
            displayMetadata(entityId, sample, entityData, scaffoldReadMetadataTitledBox, scaffoldReadSiteManager);
            featuresTabPanel.selectTab(0);
        }
    }

    private class ScaffoldPeptideSelectedListener implements EntityListener {
        public void onEntitySelected(String entityId, Object entityData) {
            Sample sample = extractPeptideSample((PeptideDetail) entityData);
            displayMetadata(entityId, sample, entityData, scaffoldPeptideMetadataTitledBox, scaffoldPeptideSiteManager);
        }
    }

    private Sample extractReadSample(ScaffoldReadAlignment scaffoldReadAlignment) {
        Sample sample = null;
        if (scaffoldReadAlignment != null) {
            Read scaffoldRead = scaffoldReadAlignment.getRead();
            if (scaffoldRead != null) {
                sample = scaffoldRead.getSample();
            }
        }
        else {
            throw new IllegalArgumentException("Invalid entity data");
        }
        return sample;
    }

    private Sample extractPeptideSample(PeptideDetail peptideDetail) {
        Sample sample = null;
        if (peptideDetail != null) {
            sample = peptideDetail.getSample();
        }
        else {
            throw new IllegalArgumentException("Invalid entity data");
        }

        return sample;
    }

    private void displayMetadata(String entityId, Sample sample, Object container, TitledBox titledBox, SiteManager siteManager) {
        titledBox.setTitle(entityId + " Metadata");
        logger.info("displayMetadata entityId=" + entityId + " sample null=" + (sample == null ? "true" : "false"));
        if (sample == null) {
            bsEntityService.getEntitySampleByAcc(entityId, new GetSamplesCallback(entityId, container, siteManager));
        }
        else {
            siteManager.getSampleDataPanel().displaySampleData(sample);
        }
    }

    /**
     */
    private class GetSamplesCallback implements AsyncCallback {
        private SiteManager siteManager;
        private String entityAcc;
        private Object container;

        public GetSamplesCallback(String entityAcc, Object container, SiteManager siteManager) {
            this.siteManager = siteManager;
            this.entityAcc = entityAcc;
            this.container = container;
        }

        public void onFailure(Throwable throwable) {
            logger.error("ScaffoldPanel GetSamplesCallback failed: " + throwable.getMessage(), throwable);
            siteManager.setServiceErrorMessage("Entity Acc: ", entityAcc);
        }

        public void onSuccess(Object result) {
            logger.debug("ScaffoldPanel GetSamplesCallback succeeded ");
            Sample sample = (Sample) result;
            if (sample != null) {
                logger.debug("ScaffoldPanel GetSamplesCallback received " + sample.getSampleName());
                siteManager.getSampleDataPanel().displaySampleData(sample);
                if (container instanceof PeptideDetail) {
                    PeptideDetail peptideDetail = (PeptideDetail) container;
                    peptideDetail.setSample(sample);
                }
            }
            else {
                siteManager.setNotFoundErrorMessage("Entity Acc: ", entityAcc);
            }
        }
    }

    private void createScaffoldReadsPanel() {
        VerticalPanel scaffoldReadsPanel = new VerticalPanel();
        featuresTabPanel.add(scaffoldReadsPanel, " Reads");
        addSmallSpacer(scaffoldReadsPanel);
        scaffoldReadsPanel.add(HtmlUtils.getHtml("Click a row to see the associated metadata in the panel below", "text"));
        addSmallSpacer(scaffoldReadsPanel);
        createReadsMetadataPanel();
        scaffoldReadsPanelBuilder = new ScaffoldReadsPanelBuilder(this, new ScaffoldReadSelectedListener());
        scaffoldReadsPanel.add(scaffoldReadsPanelBuilder.createContent());
    }

    private void createScaffoldPeptidesPanel() {
        VerticalPanel scaffoldPeptidesPanel = new VerticalPanel();
        featuresTabPanel.add(scaffoldPeptidesPanel, " Peptides");
        addSmallSpacer(scaffoldPeptidesPanel);
        scaffoldPeptidesPanel.add(HtmlUtils.getHtml("Click a row to see the associated metadata in the panel below", "text"));
        addSmallSpacer(scaffoldPeptidesPanel);
        createPeptidesMetadataPanel();
        scaffoldPeptidesPanelBuilder = new ScaffoldPeptidesPanelBuilder(this, new ScaffoldPeptideSelectedListener());
        scaffoldPeptidesPanel.add(scaffoldPeptidesPanelBuilder.createContent());
    }

    private void createReadsMetadataPanel() {
        scaffoldReadSiteManager = new SiteManager();
        Panel scaffoldReadMetadataPanel = scaffoldReadSiteManager.createFullSampleDataPanel();
        scaffoldReadMetadataTitledBox = new TitledBox("", true);
        scaffoldReadMetadataTitledBox.add(scaffoldReadMetadataPanel);
    }

    private void createPeptidesMetadataPanel() {
        scaffoldPeptideSiteManager = new SiteManager();
        Panel scaffoldPeptideMetadataPanel = scaffoldPeptideSiteManager.createFullSampleDataPanel();
        scaffoldPeptideMetadataTitledBox = new TitledBox("", true);
        scaffoldPeptideMetadataTitledBox.add(scaffoldPeptideMetadataPanel);
    }

    private void createScaffoldMetadataPanel() {
        // If we do have reads or peptides then their samples metadata must overwrite
        if (!hasSample() || hasReads() || hasPeptides()) {
            return;
        }
        scaffoldMetadataTitledBox = new TitledBox(getAcc() + " Metadata", true);
        createSampleSiteMapPanel(scaffoldMetadataTitledBox);
        super.retrieveAndBuildSampleSiteMapData();
    }

    private void createScaffoldFeaturesTabPanel() {
        TitledBox titledBox = new TitledBox("Scaffold Features");
        featuresTabPanel = new RoundedTabPanel();
        titledBox.add(featuresTabPanel);
        add(titledBox);
        addSpacer(this);
    }

    private void displayReadMetadata() {
        removeCurrentMetadataTitledBox();
        currentMetadataTitledBox = scaffoldReadMetadataTitledBox;
        addCurrentMetadataTitledBox(scaffoldReadSiteManager);
    }

    private void displayPeptideMetadata() {
        removeCurrentMetadataTitledBox();
        currentMetadataTitledBox = scaffoldPeptideMetadataTitledBox;
        addCurrentMetadataTitledBox(scaffoldPeptideSiteManager);
    }

    private void displayScaffoldMetadata() {
        // If we do have reads or peptides then their samples metadata must overwrite
        if (!hasSample() || hasReads() || hasPeptides()) {
            return;
        }
        removeCurrentMetadataTitledBox();
        if (scaffoldMetadataTitledBox == null) {
            createScaffoldMetadataPanel();
        }
        currentMetadataTitledBox = scaffoldMetadataTitledBox;
        addCurrentMetadataTitledBox(getSiteManager());
    }


    /**
     * Retrieves the sample and site map data
     */
    protected void retrieveAndBuildSampleSiteMapData() {
        // Overiding parent because we don't want retrieve this if we have reads/peptides
    }

    private void addFeatureTabPanelListener() {
        featuresTabPanel.addTabListener(new TabListener() {
            public boolean onBeforeTabSelected(SourcesTabEvents tabEvents, int i) {
                return true; // don't do anything
            }

            public void onTabSelected(SourcesTabEvents tabEvents, int i) {
                if (readTabSelected(i)) {
                    displayReadMetadata();
                }
                else if (peptideTabSelected(i)) {
                    displayPeptideMetadata();
                }
                else {
                    displayScaffoldMetadata();
                }
            }

            private boolean readTabSelected(int index) {
                if (hasReads()) {
                    return index == 0;
                }
                else {
                    return false;
                }
            }

            private boolean peptideTabSelected(int index) {
                if (hasReads() && hasPeptides()) {
                    return index == 1;
                }
                else if (!hasReads() && hasPeptides()) {
                    return index == 0;
                }
                else {
                    return false;
                }
            }

        });
        featuresTabPanel.selectTab(0);
    }

    private void removeCurrentMetadataTitledBox() {
        if (currentMetadataTitledBox != null) {
            remove(currentMetadataTitledBox);
        }
    }

    private void addCurrentMetadataTitledBox(SiteManager siteManager) {
        add(currentMetadataTitledBox);
        siteManager.getMapPanel().getGoogleMapBox().checkResizeMap();
    }

    private boolean hasReads() {
        return (scaffoldReadsPanelBuilder != null && scaffoldReadsPanelBuilder.hasData());
    }

    private boolean hasPeptides() {
        return (scaffoldPeptidesPanelBuilder != null && scaffoldPeptidesPanelBuilder.hasData());
    }


}
