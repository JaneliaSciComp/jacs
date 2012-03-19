
package org.janelia.it.jacs.web.gwt.detail.client.bse.read;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.xlink.CorrelatedAssembliesPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.xlink.CorrelatedFeaturesPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;


/**
 * The class contains the data and operations needed to render ReadPanel.
 *
 * @author Tareq Nabeel
 */
public class ReadPanel extends BSEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanel");

    private static final String DETAIL_TYPE = "Read";

    private SequenceDetailsTableBuilder readEntityTableBuilder;
    private VerticalPanel metaDataPanel;
    private CorrelatedFeaturesPanelBuilder correlatedFeaturesPanelBuilder;
    private CorrelatedAssembliesPanelBuilder correlatedAssembliesPanelBuilder;


    private class ReadSequenceDetailsTableBuilder extends SequenceDetailsTableBuilder {
        private ReadSequenceDetailsTableBuilder(FlexTable entityDetailsTable, BSEntityPanel parentPanel) {
            super(entityDetailsTable, parentPanel);
        }

        protected void populateSequenceLabelColumn(RowIndex rowIndex, String sequenceLabel) {
            populateSequenceLabelColumn(rowIndex, sequenceLabel, "Clear Range", "readDetailClearRangeTip");
        }
    }

    /**
     * Adds Read and Library data data to mainDataTable using the Read model instance retrieved
     * through the service call. This method is called after successful bsEntityService callback.
     *
     * @return RowIndex instance (which gets incremented if row was added)
     */
    public void displayData() {
        try {
            logger.debug("ReadPanel displayData...");
            RowIndex rowIndex = getBaseEntityTableBuilder().populateAccessionNo(getAcc(), getDetailTypeLabel(), null);
            getBaseEntityTableBuilder().populateEntityDetails(rowIndex);
        }
        catch (RuntimeException e) {
            logger.error("ReadPanel displayData caught exception:" + e.getMessage());
            throw e;
        }
    }

    public void displayEntitySequence(boolean asLinkOnlyFlag) {
        super.displayEntitySequence(asLinkOnlyFlag);
        // create the assembly table - I created it here instead of inside detail specific panels
        // only because this needs to be displayed right after the sequence
        createAssembliesTable();
        // display assembly data
        correlatedAssembliesPanelBuilder.populateData();
    }

    /**
     * Used to set error messages when read retrieval fails
     */
    public void setServiceErrorMessage() {
        try {
            logger.debug("ReadPanel setServiceErrorMessage...");
            super.setServiceErrorMessage();
            MessageUtil.addServiceErrorMessage(getMainTitledBox(), "Sequence", getEntityKeyLabel() + ": " + getAcc());
        }
        catch (RuntimeException e) {
            logger.error("ReadPanel setServiceErrorMessage caught exception:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Use to post error messages when read cannot be found in database
     */
    public void setNotFoundErrorMessage() {
        try {
            logger.debug("ReadPanel setNotFoundErrorMessage...");
            super.setNotFoundErrorMessage();
            MessageUtil.addNotFoundErrorMessage(getMainTitledBox(), "Sequence", getEntityKeyLabel() + ": " + getAcc());
        }
        catch (RuntimeException e) {
            logger.error("ReadPanel setNotFoundErrorMessage caught exception", e);
            throw e;
        }
    }

    public SequenceDetailsTableBuilder getBaseEntityTableBuilder() {
        return readEntityTableBuilder;
    }

    protected int getClearRangeBegin() {
        return readEntityTableBuilder.getClearRangeBegin();
    }

    protected int getClearRangeEnd() {
        return readEntityTableBuilder.getClearRangeEnd();
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
        addSpacer(this);
        createFeaturesPanel();
        addSpacer(this);
        createMetadataPanel();
        // start populating the features
        populateFeaturesPanel();
    }

    protected void createBaseEntityTableBuilder() {
        readEntityTableBuilder = new ReadSequenceDetailsTableBuilder(getMainDataTable(), this);
    }

    private void createAssembliesTable() {
        correlatedAssembliesPanelBuilder = new CorrelatedAssembliesPanelBuilder(this);
        int assemblyRowIndex = readEntityTableBuilder.getNextRowIndex().getCurrentRow();
        TableUtil.addWidgetRow(getMainDataTable(),
                readEntityTableBuilder.getNextRowIndex(),
                "Assemblies", correlatedAssembliesPanelBuilder.createDataPanel());
        getMainDataTable().getFlexCellFormatter().setColSpan(assemblyRowIndex, 1, 2);
    }

    private void createFeaturesPanel() {
        // create the panel
        correlatedFeaturesPanelBuilder = new CorrelatedFeaturesPanelBuilder(this);
        add(correlatedFeaturesPanelBuilder.createContent());
    }

    private void createMetadataPanel() {
        metaDataPanel = new TitledBox("Metadata", false);
        // set the style for the meta data panel
        metaDataPanel.setStyleName("readDetailMetaDataPanel");
        add(metaDataPanel);
        createSampleSiteMapPanel(metaDataPanel);
    }

    private void populateFeaturesPanel() {
        correlatedFeaturesPanelBuilder.populateData();
    }

}
