
package org.janelia.it.jacs.web.gwt.detail.client.bse.rna;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.genomics.NonCodingRNA;
import org.janelia.it.jacs.model.genomics.Nucleotide;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceUIData;

/**
 * The class encapsulates the data and operations needed to render PeptidePanel
 *
 * @author Tareq Nabeel
 */
public class RNAPanel extends BSEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.ncRNA.RNAPanel");

    private static final String DETAIL_TYPE = "RNA";
    private static final int METADATA_TAB_INDEX = 0;

    private SequenceDetailsTableBuilder ncRNAEntityTableBuilder;
    private VerticalPanel ncRNAFeaturesPanel;
    private VerticalPanel metaDataPanel;
    private HorizontalPanel naPanel;
    private TitledBox naDetailBox;
    private FlexTable naDataTable;
    private SequenceDetailsTableBuilder naSeqDetailsTableBuilder;
    private LoadingLabel naSequenceLoadingLabel;

    private class DnaSequenceDetailsTableBuilder extends SequenceDetailsTableBuilder {
        private DnaSequenceDetailsTableBuilder(FlexTable entityDetailsTable, BSEntityPanel parentPanel) {
            super(entityDetailsTable, parentPanel);
        }

        protected void populateSequenceLabelColumn(RowIndex rowIndex, String sequenceLabel) {
            populateSequenceLabelColumn(rowIndex, sequenceLabel, "RNA Range", "readDetailClearRangeTip");
        }
    }

    public void displayData() {
        try {
            logger.debug("RNAPanel displayData...");
            RowIndex rowIndex = getBaseEntityTableBuilder().populateAccessionNo(getAcc(), null, null);
            getBaseEntityTableBuilder().populateEntityDetails(rowIndex);
            NonCodingRNA ncRNA = (NonCodingRNA) getBaseEntityTableBuilder().getBaseEntity();

            String naAcc = ncRNA.getDnaAcc();
            if (naAcc != null && naAcc.length() > 0) {
                naSeqDetailsTableBuilder = new DnaSequenceDetailsTableBuilder(naDataTable, this);
                Nucleotide naEntity = ncRNA.getDnaEntity();
                if (naEntity != null) {
                    // if we already have the DNA entity display it
                    // otherwise we'll retrieve it later
                    setNAEntity(naEntity);
                    displayNAEntity();
                }
            }
            else {
                naPanel.setVisible(false);
            }
        }
        catch (RuntimeException e) {
            logger.error("RNAPanel displayData caught exception", e);
            throw e;
        }
    }

    public SequenceDetailsTableBuilder getBaseEntityTableBuilder() {
        return ncRNAEntityTableBuilder;
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "NcRNA" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }

    protected void createBaseEntityTableBuilder() {
        ncRNAEntityTableBuilder = new SequenceDetailsTableBuilder(getMainDataTable(), this);
    }

    protected void createDetailSpecificPanels() {
        addSpacer(this);
        if (hasSample()) {
            // if the NcRNA comes from a READ create a TAB panel
            createTabPanel();
            // prepare to fill the metadata panel
            createSampleSiteMapPanel(metaDataPanel);
        }
        else {
            // if the NcRNA does not come directly from a read create a simple vertical panel
            ncRNAFeaturesPanel = new VerticalPanel();
            ncRNAFeaturesPanel.setStyleName("rnaDetailFeaturesPanel");
            add(ncRNAFeaturesPanel);
        }
        createFeaturesPanel();
    }

    TitledBox getNADetailBox() {
        return naDetailBox;
    }

    LoadingLabel getNASequenceLoadingLabel() {
        return naSequenceLoadingLabel;
    }

    Nucleotide getNAEntity() {
        return (Nucleotide) naSeqDetailsTableBuilder.getBaseEntity();
    }

    void setNAEntity(Nucleotide naEntity) {
        naSeqDetailsTableBuilder.setBaseEntity(naEntity);
    }

    void setNASequenceUIData(SequenceUIData naSequenceUIData) {
        naSeqDetailsTableBuilder.setBaseEntitySequenceData(naSequenceUIData);
    }

    void displayNAEntity() {
        RowIndex rowIndex = naSeqDetailsTableBuilder.populateAccessionNoAsTargetLink("Source DNA details",
                naSeqDetailsTableBuilder.getBaseEntity().getAccession(),
                "Source DNA",
                null);
        naSeqDetailsTableBuilder.populateEntityDetails(rowIndex);
    }

    void displayNASequence(boolean asLinkOnlyFlag) {
        naSequenceLoadingLabel.setVisible(false);
        naSeqDetailsTableBuilder.populateSequenceData(null, asLinkOnlyFlag);
    }

    private void createDNADetailPanel() {
        logger.debug("RNAPanel createDNADetailPanel....");
        naPanel = new HorizontalPanel();
        naDetailBox = new TitledBox("Source DNA Details", true);
        naDetailBox.setActionLinkBackgroundStyleName("tertiaryTitledBoxActionLinkBackground");
        naDetailBox.setStyleName("orfDNADetailPanel");
        naDataTable = new FlexTable();
        naDetailBox.add(naDataTable);
        naPanel.add(naDetailBox);
        ncRNAFeaturesPanel.add(naPanel);
        naSequenceLoadingLabel = new LoadingLabel("Loading DNA sequence ...", true);
        naDetailBox.add(naSequenceLoadingLabel);
    }

    private void createFeaturesPanel() {
        createDNADetailPanel();
    }

    private void createTabPanel() {
        RoundedTabPanel tabPanel = new RoundedTabPanel();
        tabPanel.setStyleName("rnaDetailTabPanel");
        metaDataPanel = new VerticalPanel();
        metaDataPanel.setStyleName("rnaDetailMetaDataPanel");
        tabPanel.add(metaDataPanel, "Metadata & Geography");
        ncRNAFeaturesPanel = new VerticalPanel();
        ncRNAFeaturesPanel.setStyleName("rnaDetailFeaturesPanel");
        tabPanel.add(ncRNAFeaturesPanel, "Sequence Features");
        tabPanel.selectTab(METADATA_TAB_INDEX);
        tabPanel.addTabListener(new TabListener() {

            public boolean onBeforeTabSelected(SourcesTabEvents tabEvents, int i) {
                return true; // don't do anything
            }

            public void onTabSelected(SourcesTabEvents tabEvents, int i) {
                // we don't have to do anything here because the data retrieve process has already been initiated
            }

        });
        add(tabPanel);
    }

}
