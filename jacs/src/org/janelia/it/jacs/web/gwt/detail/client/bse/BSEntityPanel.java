
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteManager;

/**
 * The class contains the data and operations needed to render BSEntityPanel
 *
 * @author Tareq Nabeel
 */
public class BSEntityPanel extends DetailSubPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel");
    private static final String DETAIL_TYPE = "GenericService Entity";

    // The maximum sequence length for UI to retrieve and display sequence
    private static final int SEQUENCE_DISPLAY_CUTOFF = 20000;

    // Index of character to break at
    public static final int SEQUENCE_CHARS_PER_LINE = 105;

    protected SequenceDetailsTableBuilder sequenceDetailsTableBuilder;
    private LoadingLabel sequenceLoadingLabel;
    protected Panel sampleDataAndMapPanel;
    protected SiteManager siteManager;
    // previous site manager preferences
    private String siteMgrTabOrListSelection;
    private String siteMgrSrollOrNoScrollSelection;
    private boolean displayDeflineFlag;

    /**
     * Go through DetailSubPanelFactory for getting instance
     */
    protected BSEntityPanel() {
        displayDeflineFlag = false;
    }

    public Object getEntity() {
        return getBaseEntityTableBuilder().getBaseEntity();
    }

    public void setEntity(Object obj) {
        logger.debug("BSEntityPanel setEntity " + (obj == null ? "null" : obj.toString()));
        if (getBaseEntityTableBuilder() == null)
            logger.error("BSEntityPanel getBaseEntityTableBuilder() returns null");
        getBaseEntityTableBuilder().setBaseEntity((BaseSequenceEntity) obj);
    }

    public boolean isSequenceTooBigToDisplay(BaseSequenceEntity baseEntity) {
        if (baseEntity == null) {
            logger.debug("baseEntity is null");
            return false;
        }
        if (baseEntity.getSequenceLength() == null) {
            logger.debug("baseEntity getSeqLength is null acc=" + baseEntity.getAccession());
        }
        else {
            logger.debug("baseEntity getSeqLength is NOT null and =" + baseEntity.getSequenceLength());
        }
        return baseEntity.getSequenceLength() > SEQUENCE_DISPLAY_CUTOFF;
    }

    public boolean hasSample() {
        BaseSequenceEntity baseEntity = getBaseEntityTableBuilder().getBaseEntity();
        String sampleAcc = null;
        if (baseEntity != null) {
            sampleAcc = baseEntity.getSampleAcc();
        }
        return (sampleAcc != null && sampleAcc.length() > 0) || isEntitySampleAvailable();
    }

    public boolean isDisplayDeflineFlag() {
        return displayDeflineFlag;
    }

    public boolean isEntitySampleAvailable() {
        BaseSequenceEntity baseEntity = getBaseEntityTableBuilder().getBaseEntity();
        return baseEntity != null && baseEntity.getSample() != null;
    }

    /**
     * Adds entity data to the panel using the entity model instance retrieved through
     * the service call. This method is called after successful detailservice callback.
     */
    public void displayData() {
        try {
            logger.debug("BSEntityPanel displayData...");
            RowIndex rowIndex = getBaseEntityTableBuilder().populateAccessionNo(getAcc(), getDetailTypeLabel(), null);
            logger.debug("BSEntityPanel populateEntityDetails...");
            getBaseEntityTableBuilder().populateEntityDetails(rowIndex);
        }
        catch (RuntimeException e) {
            logger.error("BSEntityPanel displayData caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

    public void displayEntityDefline() {
        getBaseEntityTableBuilder().populateDefLine(null);
    }

    public void displayEntitySequence(boolean asLinkOnlyFlag) {
        getSequenceLoadingLabel().setVisible(false);
        getBaseEntityTableBuilder().populateSequenceData(null, asLinkOnlyFlag);
    }

    /**
     * Used to set error messages when entity retrieval fails
     */
    public void setServiceErrorMessage() {
        super.setServiceErrorMessage();
        if (siteManager != null) {
            siteManager.setServiceErrorMessage(getEntityKeyLabel() + ": ", getAcc());
        }
    }

    /**
     * Can be used to store any user preferences for example before sub panel is blow away
     */
    public void preInit() {
        try {
            if (siteManager != null) {
                // save site manager's preferences
                siteMgrTabOrListSelection = siteManager.getSiteDataPanel().getSiteTabOrListSelection();
                siteMgrSrollOrNoScrollSelection = siteManager.getSiteDataPanel().getSiteScrollOrNoScrollSelection();
            }
        }
        catch (RuntimeException e) {
            logger.error("preInit caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Can be used to restore any user preferences
     */
    public void postInit() {
        try {
            // Set the user's preference so they don't get overwritten on viewing each read
            if (siteManager != null) {
                // restore site manager's preferences
                siteManager.getSiteDataPanel().setSiteTabOrListSelection(siteMgrTabOrListSelection);
                siteManager.getSiteDataPanel().setSiteScrollOrNoScrollSelection(siteMgrSrollOrNoScrollSelection);
            }
        }
        catch (RuntimeException e) {
            logger.error("postInit caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Use to post error messages when entity cannot be found in database
     */
    public void setNotFoundErrorMessage() {
        super.setNotFoundErrorMessage();
        if (siteManager != null) {
            siteManager.setNotFoundErrorMessage(getEntityKeyLabel() + ": ", getAcc());
        }
    }

    public SequenceDetailsTableBuilder getBaseEntityTableBuilder() {
        return sequenceDetailsTableBuilder;
    }

    public void setBaseEntityTableBuilder(SequenceDetailsTableBuilder sequenceDetailsTableBuilder) {
        this.sequenceDetailsTableBuilder = sequenceDetailsTableBuilder;
    }

    protected int getClearRangeBegin() {
        return 0;
    }

    protected int getClearRangeEnd() {
        return 0;
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }

    protected boolean isSequenceTooBigToDisplay() {
        return getBaseEntityTableBuilder().getBaseEntity().getSequenceLength().intValue() > SEQUENCE_DISPLAY_CUTOFF;
    }

    protected void setSequenceUIData(SequenceUIData sequenceUIData) {
        getBaseEntityTableBuilder().setBaseEntitySequenceData(sequenceUIData);
    }

    protected LoadingLabel getSequenceLoadingLabel() {
        return sequenceLoadingLabel;
    }

    public void setSequenceLoadingLabel(LoadingLabel sequenceLoadingLabel) {
        this.sequenceLoadingLabel = sequenceLoadingLabel;
    }

    protected void createBaseEntityTableBuilder() {
        sequenceDetailsTableBuilder = new SequenceDetailsTableBuilder(getMainDataTable(), this);
    }

    /**
     * precondition: the method is invoked only once the entity is available
     */
    protected void createDetailSpecificPanels() {
        if (hasSample()) {
            // if the entity has a sample associated with it create the sample and site panels
            addSpacer(this);
            createSampleSiteMapPanel(this);
        }
    }

    /**
     * Creates sample, site, and map panels.
     */
    public void createSampleSiteMapPanel(Panel parentPanel) {
        try {
            logger.debug("BaseEntityPanel createSampleSiteMapPanel....");
            siteManager = new SiteManager();
            sampleDataAndMapPanel = siteManager.createFullSampleDataPanel();
            parentPanel.add(sampleDataAndMapPanel);
        }
        catch (RuntimeException e) {
            logger.error("ReadPanel createSampleSiteMapPanel caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * This method creates the skeleton of the BSEntityPanel with loading messages
     * before data is retrieved from server asynchronously
     */

    protected void createSkeleton() {
        try {
            logger.debug("BSEntityPanel createSkeleton...");
            super.createSkeleton();
            logger.debug("BSEntityPanel setting loading label...");
            sequenceLoadingLabel = new LoadingLabel("Loading sequence data...", true);
            logger.debug("BSEntityPanel setting loading label...");
            getMainTitledBox().add(sequenceLoadingLabel);
            logger.debug("BSEntityPanel creating bse table builder...");
            createBaseEntityTableBuilder();
        }
        catch (RuntimeException e) {
            logger.error("BSEntityPanel createSkeleton caught exception " + e.getMessage());
            throw e;
        }
    }

    protected void displayIntellectualPropertyNotice() {
        BaseSequenceEntity baseEntity = getBaseEntityTableBuilder().getBaseEntity();
        if (baseEntity != null && baseEntity.getSample() != null) {
            displayIntellectualPropertyNotice(baseEntity.getSample().getIntellectualPropertyNotice());
        }
    }

    protected void displayIntellectualPropertyNotice(String ipNotice) {
        getBaseEntityTableBuilder().populateIntellectualPropertyNotice(ipNotice);
    }

    /**
     * Retrieves the sample and site map data
     */
    protected void retrieveAndBuildSampleSiteMapData() {
        logger.debug("BaseEntityPanel calling siteManger.retrieveAndDisplayByReadAcc with readAcc=" + getAcc());
        siteManager.retrieveAndDisplayDataByBSEntityAcc(getAcc());
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    protected void setSiteManager(SiteManager siteManager) {
        this.siteManager = siteManager;
    }

}
