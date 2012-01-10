
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.model.genomics.ORF;
import org.janelia.it.jacs.model.genomics.Protein;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceDetailsTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteManager;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:35:50 PM
 */
public class PeporfPanel extends BSEntityPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPanel");

    public static final String PEPTIDE_DETAIL_TYPE = "Peptide";
    public static final String ORF_DETAIL_TYPE = "ORF";
    public static final String GENERIC_DETAIL_TYPE = "GenericService";

    int NOT_VALID = -1;
    int tabIndex = 0;
    int metadataTab = NOT_VALID;
    int sequenceTab = NOT_VALID;
    int clusterTab = NOT_VALID;

    String initialDetailType = GENERIC_DETAIL_TYPE;

    // Paging panel to contain tabs
    RoundedTabPanel tabPanel;

    // Protein Annotation
    private PeporfAnnotationPanel annotationPanel;

    // Sample metadata
    private PeporfMetadataPanel metadataPanel;

    // Peptide
    private PeporfPeptidePanel peptidePanel;

    // Orf
    private PeporfOrfPanel orfPanel;

    // Source DNA
    private PeporfSourceDNAPanel sourceDNAPanel;

    // Cluster
    private PeporfClusterPanel clusterPanel;


    public PeporfPanel() {
        this(GENERIC_DETAIL_TYPE);
    }

    public PeporfPanel(String initialDetailType) {
        super();
        this.initialDetailType = initialDetailType;
    }

    protected PeporfAnnotationPanel getAnnotationPanel() {
        return annotationPanel;
    }

    protected PeporfMetadataPanel getMetadataPanel() {
        return metadataPanel;
    }

    protected PeporfPeptidePanel getPeptidePanel() {
        return peptidePanel;
    }

    protected PeporfOrfPanel getOrfPanel() {
        return orfPanel;
    }

    protected PeporfSourceDNAPanel getSourceDNAPanel() {
        return sourceDNAPanel;
    }

    protected PeporfClusterPanel getClusterPanel() {
        return clusterPanel;
    }

    public void displayData() {
        try {
            logger.debug("PeporfPanel displayData start...");
            super.displayData();
            if (annotationPanel != null)
                annotationPanel.display();
            if (metadataPanel != null)
                metadataPanel.display();
            if (initialDetailType.equals(ORF_DETAIL_TYPE) && peptidePanel != null)
                peptidePanel.display();
            if (initialDetailType.equals(PEPTIDE_DETAIL_TYPE) && orfPanel != null)
                orfPanel.display();
            if (sourceDNAPanel != null)
                sourceDNAPanel.display();
            displayTabPanel();
            logger.debug("PeporfPanel displayData end");
        }
        catch (RuntimeException e) {
            logger.error("PeporfPanel displayData caught exception", e);
            throw e;
        }
    }

    protected void displayTabPanel() {
        boolean selected = false;
        if (metadataPanel != null) {
            selected = true;
            tabPanel.selectTab(metadataTab);
        }
        else if (sourceDNAPanel != null) {
            selected = true;
            tabPanel.selectTab(sequenceTab);
        }
        else if (clusterPanel != null) {
            selected = true;
            tabPanel.selectTab(clusterTab);
        }
        if (selected) {
            tabPanel.setVisible(true);
        }
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        logger.debug("PeporfPanel getDetailTypeLabel...");
        SequenceDetailsTableBuilder tableBuilder = getBaseEntityTableBuilder();
        logger.debug("PeporfPanel getDetailTypeLabel getting bse...");
        if (tableBuilder == null) {
            return initialDetailType;
        }
        else {
            BaseSequenceEntity bse = tableBuilder.getBaseEntity();
            logger.debug("PeporfPabel getDetailTypeLabel getting entity type...");
            EntityTypeGenomic bseType = bse.getEntityType();
            logger.debug("PeporfPanel getDetailTypeLabel getting type code...");
            int bseCode = bseType.getCode();
            if (bseCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN ||
                    bseCode == EntityTypeGenomic.ENTITY_CODE_PEPTIDE) {
                return PEPTIDE_DETAIL_TYPE;
            }
            else if (bseCode == EntityTypeGenomic.ENTITY_CODE_ORF) {
                return ORF_DETAIL_TYPE;
            }
            else {
                return GENERIC_DETAIL_TYPE;
            }
        }
    }

    protected void createBaseEntityTableBuilder() {
        sequenceDetailsTableBuilder = new SequenceDetailsTableBuilder(getMainDataTable(), this);
    }

    protected void createDetailSpecificPanels() {
        logger.debug("PeporfPanel createDetailSpecificPanels start...");
        addSpacer(this);

        // Create and add tab panel for other subpanels
        tabIndex = 0; // must be reset in case builder is re-called
        tabPanel = new RoundedTabPanel();
        tabPanel.setVisible(false);
        annotationPanel.initialize();

        if (getDetailTypeLabel().equals(PEPTIDE_DETAIL_TYPE)) {
            peptidePanel.initialize();
            peptidePanel.getLoadingLabel().setVisible(false);
            if (peptideHasOrf()) {
                orfPanel = new PeporfOrfPanel(this);
                orfPanel.setStyleName("peptideORFDetailPanel");
                orfPanel.initialize();
                getMainTitledBox().add(orfPanel);

            }
        }
        else if (getDetailTypeLabel().equals(ORF_DETAIL_TYPE)) {
            orfPanel.initialize();
            orfPanel.getLoadingLabel().setVisible(false);
            peptidePanel = new PeporfPeptidePanel(this);
            peptidePanel.setStyleName("peptideDetailFeaturesPanel");
            peptidePanel.initialize();
            getMainTitledBox().add(peptidePanel);  // we assume all orfs have corresponding peptides
        }

        if (hasSample()) {
            metadataPanel = new PeporfMetadataPanel(this);
            metadataPanel.initialize();
            tabPanel.add(metadataPanel, "Sample Geography");
            metadataTab = tabIndex++;

        }
        if (hasSourceDNA()) {
            sourceDNAPanel = new PeporfSourceDNAPanel(this);
            sourceDNAPanel.setStyleName("peptideDNADetailPanel");
            sourceDNAPanel.initialize();
            tabPanel.add(sourceDNAPanel, "Source DNA");
            sequenceTab = tabIndex++;
        }

        clusterPanel = new PeporfClusterPanel(this);
        clusterPanel.initialize();
        tabPanel.add(clusterPanel, "Cluster Info");
        clusterTab = tabIndex++;
        add(tabPanel);

        logger.debug("PeporfPanel createDetailSpecificPanels end");
    }

    private boolean peptideHasOrf() {
        boolean hasOrf = false;
        BaseSequenceEntity bse = getBaseEntityTableBuilder().getBaseEntity();
        if (bse.getEntityType().getCode() == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
            Protein protein = (Protein) bse;
            String orfAcc = protein.getOrfAcc();
            if (orfAcc != null && orfAcc.length() > 0) {
                hasOrf = true;
            }
        }
        return hasOrf;
    }

    private boolean hasSourceDNA() {
        boolean hasSourceDNA = false;
        BaseSequenceEntity bse = getBaseEntityTableBuilder().getBaseEntity();
        String naAcc = null;
        if (bse.getEntityType().getCode() == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
            Protein protein = (Protein) bse;
            naAcc = protein.getDnaAcc();
        }
        else if (bse.getEntityType().getCode() == EntityTypeGenomic.ENTITY_CODE_ORF) {
            ORF orf = (ORF) bse;
            naAcc = orf.getDnaAcc();
        }
        if (naAcc != null && naAcc.length() > 0)
            hasSourceDNA = true;
        return hasSourceDNA;
    }

    protected void createSkeleton() {
        try {
            logger.debug("PeporfPanel createSkeleton start...");
            // Setup main titled box and its subpanels
            String typeLabel = getDetailTypeLabel();
            SecondaryTitledBox mainTitledBox = new SecondaryTitledBox(typeLabel + " Details", true);
            setMainTitledBox(mainTitledBox);
            mainTitledBox.setStyleName("detailMainPanel");
            TitledBoxActionLinkUtils.addHelpActionLink(mainTitledBox, new HelpActionLink("help"),
                    DETAIL_HELP_URL_PROP);
            FlexTable mainDataTable = new FlexTable();
            setMainDataTable(mainDataTable);

            // Add Protein Annotation panel
            annotationPanel = new PeporfAnnotationPanel(this);
            mainTitledBox.add(annotationPanel);

            // Add bse panel according to type
            if (initialDetailType.equals(PEPTIDE_DETAIL_TYPE)) {
                peptidePanel = new PeporfPeptidePanel(this);
                peptidePanel.setStyleName("peptideDetailFeaturesPanel");
                peptidePanel.setDataTable(mainDataTable);
                mainTitledBox.add(peptidePanel);
            }
            else if (initialDetailType.equals(ORF_DETAIL_TYPE)) {
                orfPanel = new PeporfOrfPanel(this);
                orfPanel.setStyleName("peptideORFDetailPanel");
                orfPanel.setDataTable(mainDataTable);
                mainTitledBox.add(orfPanel);
            }

            // After all subpanels are added, add mainTitledBox
            add(mainTitledBox);

            // Create expected contents for base bse
            createAndAddLoadingLabels();
            createBaseEntityTableBuilder();

            logger.debug("PeporfPanel createSkeleton end");
        }
        catch (RuntimeException e) {
            logger.error("PeporfPanel createSkeleton caught exception " + e.getMessage());
            throw e;
        }
    }

    protected void createAndAddLoadingLabels() {
        super.createAndAddLoadingLabels();
        LoadingLabel sequenceLoadingLabel = new LoadingLabel("Loading sequence data...", true);
        setSequenceLoadingLabel(sequenceLoadingLabel);
    }

    public void createSampleSiteMapPanel(Panel parentPanel) {
        try {
            logger.debug("BaseEntityPanel createSampleSiteMapPanel....");
            siteManager = new SiteManager();
            siteManager.setDelayAddingMapBox(true);
            sampleDataAndMapPanel = siteManager.createFullSampleDataPanel();
            parentPanel.add(sampleDataAndMapPanel);
        }
        catch (RuntimeException e) {
            logger.error("ReadPanel createSampleSiteMapPanel caught exception:" + e.getMessage(), e);
            throw e;
        }
    }

}
