
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.CollectionSite;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.model.tasks.export.SequenceExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.SpanListExpander;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;
import org.janelia.it.jacs.web.gwt.download.client.DownloadBox;
import org.janelia.it.jacs.web.gwt.download.client.DownloadSequencePopup;

import java.util.*;

/**
 * Responsible for populating the entity details table
 */
public class SequenceDetailsTableBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.orf.ORFPeptideRetriever");

    private static final int IP_NOTICE_ROW = 3;
    private static final int IP_NOTICE_COL = 2;
    private static final int IP_NOTICE_HEIGHT = 3;
    private static final int DOWNLOAD_BOX_ROW = 0;
    private static final int DOWNLOAD_BOX_COL = 2;
    private static final int DOWNLOAD_BOX_HEIGHT = 3;

    // The number of characters to display "...more" link at
    private static final int SEQUENCE_DISPLAY_MIN_SIZE = 314;

    protected BSEntityPanel parentPanel;
    protected FlexTable entityDetailsTable;
    private SecondaryTitledBox ipNoticeBox;
    private DownloadBox sequenceDownloadBox;
    private BaseSequenceEntity baseEntity;
    private SequenceUIData baseEntitySequenceData;
    private HTML ipNotice;
    private List taxonSynonyms;

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public SequenceDetailsTableBuilder(FlexTable entityDetailsTable, BSEntityPanel parentPanel) {
        this.entityDetailsTable = entityDetailsTable;
        this.parentPanel = parentPanel;
        ipNoticeBox = null;
        ipNotice = null;
    }

    public BaseSequenceEntity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(BaseSequenceEntity baseEntity) {
        this.baseEntity = baseEntity;
    }

    public SequenceUIData getBaseEntitySequenceData() {
        return baseEntitySequenceData;
    }

    public void setBaseEntitySequenceData(SequenceUIData baseEntitySequenceData) {
        this.baseEntitySequenceData = baseEntitySequenceData;
    }

    public List getTaxonSynonyms() {
        return taxonSynonyms;
    }

    public void setTaxonSynonyms(List taxonSynonyms) {
        this.taxonSynonyms = taxonSynonyms;
    }

    public RowIndex getNextRowIndex() {
        return new RowIndex(entityDetailsTable.getRowCount() + 1);
    }

    public RowIndex populateDefLine(RowIndex rowIndex) {
        return populateDefLine("Defline", rowIndex);
    }

    public RowIndex populateDefLine(String defLineLabel, RowIndex rowIndex) {
        if (rowIndex == null) {
            rowIndex = getNextRowIndex();
        }
        String defline = getBaseEntity().getDefline();
        int cClusterPosition = defline.indexOf(" /core_cluster_id=");
        int ipNoticePosition = defline.indexOf(" /ip_notice=");
        if (ipNoticePosition > 0) {
            if (cClusterPosition > ipNoticePosition) {
                defline = defline.substring(0, ipNoticePosition - 1) + defline.substring(cClusterPosition);
            }
            else {
                defline = defline.substring(0, ipNoticePosition - 1);
            }
        }
        TableUtil.addTextRow(entityDetailsTable, rowIndex, defLineLabel, defline);
        return rowIndex;
    }

    public RowIndex populateAccessionNo(String accession, String entityType, RowIndex rowIndex) {
        if (rowIndex == null) {
            rowIndex = new RowIndex(0);
        }
        TableUtil.addTextRow(entityDetailsTable,
                rowIndex,
                "Accession",
                accession);
        return rowIndex;
    }

    public RowIndex populateAccessionNoAsTargetLink(String currentPanelLabel,
                                                    String sourceAccNo,
                                                    String label,
                                                    RowIndex rowIndex) {
        if (rowIndex == null) {
            rowIndex = getNextRowIndex();
        }
        Widget targetAccessWidget = parentPanel.getTargetAccessionWidget(sourceAccNo,
                currentPanelLabel,
                getBaseEntity().getAccession());
        TableUtil.addWidgetRow(entityDetailsTable, rowIndex, label, targetAccessWidget);
        return rowIndex;
    }

    public RowIndex populateEntityDetails(RowIndex rowIndex) {
        BaseSequenceEntity bse = getBaseEntity();
        EntityTypeGenomic seqType = bse.getEntityType();
        logger.debug("SequenceDetailsTableBuilder.pop EntityTypeGenomic=" + seqType.getName());

        if (rowIndex == null) {
            rowIndex = getNextRowIndex();
        }

        /*
         * attributes common to all types of sequences
         */
        if (bse.getReplacedBy() != null)
            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "Replaced by",
                    parentPanel.getTargetAccessionWidget(bse.getAccession(),
                            "Replaced sequence", bse.getReplacedBy()));

        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Entity Type", getBaseEntity().getEntityType().getName());
        if (getBaseEntity().getExternalSource() != null) {
            if (getBaseEntity().getExternalSource().equals("TGI") ||
                    getBaseEntity().getExternalSource().equals("ENS"))
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "External Id", getBaseEntity().getExternalAcc() + " (" + getBaseEntity().getExternalSource() + ")");
            else if (getBaseEntity().getExternalSource().equalsIgnoreCase("TI"))
                TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "External Id", new ExternalLink(getBaseEntity().getExternalAcc() + " (" + getBaseEntity().getExternalSource() + ")", "http://www.ncbi.nlm.nih.gov/Traces/trace.cgi?&cmd=retrieve&val=" + getBaseEntity().getExternalAcc() + "&dopt=info&size=1&seeas=Show"));
            else {
                //TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "External Id", new ExternalLink(getBaseEntity().getExternalAcc()+" ("+getBaseEntity().getExternalSource()+")","http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?cmd=Retrieve&id="+getBaseEntity().getExternalAcc()));
                TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "External Id", new ExternalLink(getBaseEntity().getExternalAcc() + " (" + getBaseEntity().getExternalSource() + ")", "http://www.ncbi.nlm.nih.gov/sites/entrez?db=protein&cmd=&term=" + getBaseEntity().getExternalAcc()));
            }

        }
        if (getBaseEntity().getNcbiGINumber() != null)
            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "NCBI GI Number", new ExternalLink(getBaseEntity().getNcbiGINumber().toString(), "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?cmd=Retrieve&id=" + getBaseEntity().getNcbiGINumber().toString()));
        String organismString = getBaseEntity().getOrganism();
        StringBuffer organismEntry = new StringBuffer("");
        if (getBaseEntity().getOrganism() != null)
            organismEntry.append(organismString);
        if (getTaxonSynonyms() != null) {
            int count = 0;
            Iterator iter = getTaxonSynonyms().iterator();
            while (iter.hasNext()) {
                String synonym = (String) iter.next();
                if (!synonym.equals(organismString)) { // we do not want to repeat organism name in parens
                    if (count == 0) {
                        organismEntry.append(" (" + synonym);
                    }
                    else {
                        organismEntry.append(", " + synonym);
                    }
                    count++;
                }
            }
            if (count > 0)
                organismEntry.append(")");
        }
        if (organismEntry.toString().length() > 0)
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Organism", organismEntry.toString());
        if (getBaseEntity().getTaxonId() != null)
            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "NCBI Taxon Id",
                    new ExternalLink(getBaseEntity().getTaxonId() + "",
                            "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=" + getBaseEntity().getTaxonId()));
        if (getBaseEntity().getAssembly() != null) {
            if (getBaseEntity().getAssembly().getDescription() != null)
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Assembly", getBaseEntity().getAssembly().getDescription());
            else
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Assembly", getBaseEntity().getAssembly().getAssemblyAcc());
        }
        Sample entitySample = getBaseEntity().getSample();
        if (entitySample != null) {
            StringBuffer sampleDescBuffer = new StringBuffer(entitySample.getTitle().trim());
            int countryIndex = 0;
            Set<String> countries = new HashSet<String>();
            for (Iterator sampleMaterialItr = entitySample.getBioMaterials().iterator(); sampleMaterialItr.hasNext();) {
                BioMaterial sampleMaterial = (BioMaterial) sampleMaterialItr.next();
                CollectionSite site = sampleMaterial.getCollectionSite();
                if (site instanceof GeoPoint) {
                    String country = ((GeoPoint) site).getCountry();
                    if (country != null && country.length() > 0 && !countries.contains(country)) {
                        if (countryIndex == 0) {
                            sampleDescBuffer.append(" - ");
                        }
                        else {
                            sampleDescBuffer.append(", ");
                        }
                        sampleDescBuffer.append(country);
                        countries.add(country);
                        countryIndex++;
                    }
                }
            }
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Sample", sampleDescBuffer.toString());
        }
        if (getBaseEntity().getLibrary() != null) {
            if (getBaseEntity().getLibrary().getSequencingTechnology() != null)
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Sequencing Technology", getBaseEntity().getLibrary().getSequencingTechnology());
            if (getBaseEntity().getLibrary().getMinInsertSize() != null) {
                if (getBaseEntity().getLibrary().getMaxInsertSize().intValue() == getBaseEntity().getLibrary().getMinInsertSize().intValue())
                    TableUtil.addTextRow(entityDetailsTable, rowIndex, "Insert Size", getBaseEntity().getLibrary().getMinInsertSize().toString());
                else
                    TableUtil.addTextRow(entityDetailsTable, rowIndex, "Insert Size", getBaseEntity().getLibrary().getMinInsertSize().toString() + "-" + getBaseEntity().getLibrary().getMaxInsertSize().toString());
            }
        }
        addSequenceDownloadBox(getBaseEntity());
        /****************************************************************************************
         * chromosome specific attributes
         */
        if (seqType.getCode() == EntityTypeGenomic.CHROMOSOME.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate chromosome details");
            Chromosome chromosome = (Chromosome) bse;
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Chromosome Type", chromosome.getType());
        }
        /****************************************************************************************
         * scaffold specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.SCAFFOLD.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate scaffold details");
            Scaffold scaffold = (Scaffold) bse;
        }
        /****************************************************************************************
         * read specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.READ.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate read details");
            Read read = (Read) bse;
            if (read.getMates() != null && read.getMates().size() > 0) {
                logger.debug("SequenceDetailsTableBuilder populate read mates");
                Iterator mateIter = read.getMates().iterator();
                while (mateIter.hasNext()) {
                    String mateAcc = ((Read) mateIter.next()).getAccession();
                    logger.debug("SequenceDetailsTableBuilder read mate acc=" + mateAcc);
                    TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "Mate",
                            parentPanel.getTargetAccessionWidget(read.getAccession(),
                                    "Mate details", mateAcc));
                }
            }

            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Trace", read.getTraceAcc());
            if (read.getClearRangeBegin() != null && read.getClearRangeEnd() != null)
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Clear Range", read.getClearRangeBegin().toString() + "-" + read.getClearRangeEnd().toString());
        }
        /****************************************************************************************
         * orf specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.ORF.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate ORF details");
            ORF orf = (ORF) bse;

            String orfProteinAcc = orf.getProteinAcc();
            if (orfProteinAcc != null && orfProteinAcc.length() > 0) {
                TableUtil.addWidgetRow(entityDetailsTable,
                        rowIndex,
                        "Peptide",
                        parentPanel.getTargetAccessionWidget(orf.getAccession(),
                                "ORF details",
                                orfProteinAcc));
            }

            String orfSourceDNAAcc = orf.getDnaAcc();
            if (orfSourceDNAAcc != null && orfSourceDNAAcc.length() > 0) {
                TableUtil.addWidgetRow(entityDetailsTable,
                        rowIndex,
                        "Source DNA",
                        parentPanel.getTargetAccessionWidget(orf.getAccession(),
                                "Source DNA details",
                                orfSourceDNAAcc));
            }

            String translationBegin = "";
            if (orf.getDnaBegin() != null) {
                translationBegin = String.valueOf(orf.getDnaBegin());
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Translation begin", translationBegin);
            }

            String translationEnd = "";
            if (orf.getDnaEnd() != null) {
                translationEnd = String.valueOf(orf.getDnaEnd());
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Translation end", translationEnd);
            }

            String orientation = "";
            if (orf.getDnaOrientation() != null) {
                orientation = String.valueOf(orf.getDnaOrientation());
                if (orientation.equals("1"))
                    orientation = "forward";
                else if (orientation.equals("-1"))
                    orientation = "reverse";
                else {
                    logger.debug("SequenceDetailsTableBuilder unknonw orientation value: " +
                            orientation + " for " + bse.getAccession());
                    orientation = "unknown";
                }
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Orientation", orientation);
            }
//            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "Translation table",
//                    getTranslationTableLink(orf.getTranslationTable()));

            String fivePrimeStop = orf.getStop5Prime();
            if (fivePrimeStop == null || fivePrimeStop.length() == 0 || fivePrimeStop.equals("0"))
                fivePrimeStop = "";
            else
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "5' Stop", fivePrimeStop);

            String threePrimeStop = orf.getStop3Prime();
            if (threePrimeStop == null || threePrimeStop.length() == 0 || threePrimeStop.equals("0"))
                threePrimeStop = "";
            else
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "3' Stop", threePrimeStop);
        }
        /****************************************************************************************
         * peptide specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.PEPTIDE.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate peptide details");
            // for now we don't do anything specific for peptides
        }
        /****************************************************************************************
         * protein specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.PROTEIN.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate protein details");
            Protein protein = (Protein) bse;
            String proteinOrfAcc = protein.getOrfAcc();
            if (proteinOrfAcc != null && proteinOrfAcc.length() > 0) {
                TableUtil.addWidgetRow(entityDetailsTable,
                        rowIndex,
                        "ORF",
                        parentPanel.getTargetAccessionWidget(protein.getAccession(),
                                "Peptide details",
                                proteinOrfAcc));
            }

            String peptideSourceDNAAcc = protein.getDnaAcc();
            if (peptideSourceDNAAcc != null && peptideSourceDNAAcc.length() > 0) {
                TableUtil.addWidgetRow(entityDetailsTable,
                        rowIndex,
                        "Source DNA",
                        parentPanel.getTargetAccessionWidget(protein.getAccession(),
                                "Source DNA details",
                                peptideSourceDNAAcc));
            }

//            String translationBegin = "";
//            if(protein.getDnaBegin() != null) {
//                translationBegin = String.valueOf(protein.getDnaBegin());
//                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Translation begin", translationBegin);
//            }
//
//            String translationEnd = "";
//            if(protein.getDnaEnd() != null) {
//                translationEnd = String.valueOf(protein.getDnaEnd());
//                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Translation end", translationEnd);
//            }

//            String orientation = "";
//            if(protein.getDnaOrientation() != null) {
//                orientation = String.valueOf(protein.getDnaOrientation());
//                if(orientation.equals("1"))
//                    orientation = "forward";
//                else if(orientation.equals("-1"))
//                    orientation = "reverse";
//                else {
//                    logger.error("SequenceDetailsTableBuilder unknonw orientation value: " +
//                            orientation + " for " + bse.getAccession());
//                    orientation = "unknown";
//                }
//                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Orientation", orientation);
//            }
            if (protein.getTranslationTable() == null ||
                    protein.getTranslationTable().equals("unknown")) {
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "Translation table", "unknown");
            }
            else {
                TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "Translation table",
                        getTranslationTableLink(protein.getTranslationTable()));
            }
            String fivePrimeStop = protein.getStop5Prime();
            if (fivePrimeStop == null || fivePrimeStop.length() == 0 || fivePrimeStop.equals("0"))
                fivePrimeStop = "";
            else
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "5' Stop", fivePrimeStop);

            String threePrimeStop = protein.getStop3Prime();
            if (threePrimeStop == null || threePrimeStop.length() == 0 || threePrimeStop.equals("0"))
                threePrimeStop = "";
            else
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "3' Stop", threePrimeStop);
        }
        /****************************************************************************************
         * RNA specific attributes
         */
        else if (seqType.getCode() == EntityTypeGenomic.NON_CODING_RNA.getCode()) {
            logger.debug("SequenceDetailsTableBuilder populate NC-RNA");
            NonCodingRNA ncRNA = (NonCodingRNA) bse;

            String ncRNASourceDNAAcc = ncRNA.getDnaAcc();
            if (ncRNASourceDNAAcc != null && ncRNASourceDNAAcc.length() > 0) {
                TableUtil.addWidgetRow(entityDetailsTable,
                        rowIndex,
                        "Source DNA",
                        parentPanel.getTargetAccessionWidget(ncRNA.getAccession(),
                                "RNA details",
                                ncRNASourceDNAAcc));
            }

            String translationBegin = "";
            if (ncRNA.getDnaBegin() != null) {
                translationBegin = String.valueOf(ncRNA.getDnaBegin());
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "DNA begin", translationBegin);
            }

            String translationEnd = "";
            if (ncRNA.getDnaEnd() != null) {
                translationEnd = String.valueOf(ncRNA.getDnaEnd());
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "DNA end", translationEnd);
            }

            String orientation = "";
            if (ncRNA.getDnaOrientation() != null) {
                orientation = String.valueOf(ncRNA.getDnaOrientation());
                if (orientation.equals("1"))
                    orientation = "forward";
                else if (orientation.equals("-1"))
                    orientation = "reverse";
                else {
                    logger.error("SequenceDetailsTableBuilder unknonw orientation value: " +
                            orientation + " for " + bse.getAccession());
                    orientation = "unknown";
                }
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "DNA orientation", orientation);
            }
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "RNA Type", ncRNA.getType());
        }
        return rowIndex;
    }

    public void populateIntellectualPropertyNotice(String ipString) {
        logger.debug("IP notice = '" + ipString + "'");
        ipNoticeBox = new SecondaryTitledBox("Intellectual Property Notice", false, true);
        ipNoticeBox.setVisible(false);
        ipNoticeBox.setStyleName("readDetailIPTitledBox");
        ScrollPanel ipScrollPanel = new ScrollPanel();
        ipNoticeBox.add(ipScrollPanel);
        ipNotice = new HTML();
        if (ipString == null || "None".equals(ipString)) {
            ipScrollPanel.setStyleName("readDetailIPScrollPanelFixedWidth");
            ipNotice.setText("None");
        }
        else {
            ipScrollPanel.setStyleName("readDetailIPScrollPanel");
            ipNotice.setText(ipString);
        }
        ipScrollPanel.add(ipNotice);
        entityDetailsTable.setWidget(IP_NOTICE_ROW, IP_NOTICE_COL,
                ipNoticeBox);
        // for now we only set it to span 3 rows - there's 1 for the accession,
        // 1 for the defline and 1 for the sequence at least
        // but this may change when the notice is actually displayed
        entityDetailsTable.getFlexCellFormatter().setRowSpan(IP_NOTICE_ROW, IP_NOTICE_COL,
                IP_NOTICE_HEIGHT);
        entityDetailsTable.getFlexCellFormatter().setHorizontalAlignment(IP_NOTICE_ROW, IP_NOTICE_COL,
                HorizontalPanel.ALIGN_LEFT);
        ipNoticeBox.setVisible(true);
    }

    /**
     * Adds the squence to mainDataTable using BioSequence model instance retrieved through service
     * call. This method is called after successful bsEntityService callback.
     */
    public void populateSequenceData(RowIndex rowIndex, boolean asDownloadLinkOnly) {
        if (rowIndex == null) {
            rowIndex = getNextRowIndex();
        }
        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Seq Length", String.valueOf(getBaseEntity().getSequenceLength()));
        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Seq Type", getBaseEntity().getSequenceType().getName());
        // now we have an idea how many rows will be in the main table therefore if we have an IP notice
        // we can adjust its size
        adjustSizeOfIntellectualPropertyWidget(entityDetailsTable.getRowCount() - IP_NOTICE_ROW);
        if (asDownloadLinkOnly) {
            // if the sequence is too big too display create an external link to download it as FASTA
            ArrayList<String> accessionList = new ArrayList<String>();
            accessionList.add(getBaseEntity().getAccession());
            final SequenceExportTask exportTask = new SequenceExportTask(ExportWriterConstants.EXPORT_TYPE_FASTA, accessionList, null);
            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "Sequence",
                    new ExternalLink("Export as FASTA", new ClickListener() {
                        public void onClick(Widget sender) {
                            new AsyncExportTaskController(exportTask).start();
                        }
                    }));
        }
        else {
            // otherwise output it
            populateShortSequenceData(rowIndex);
        }
        entityDetailsTable.getFlexCellFormatter().setColSpan(rowIndex.getCurrentRow(), 1, 2);
    }

    protected void populateShortSequenceData(RowIndex rowIndex) {
        if (getBaseEntitySequenceData().getSequenceSpanList() != null) {
            populateSequenceLabelColumn(rowIndex, "Sequence");
            populateSequenceDataColumn(rowIndex);
        }
    }

    protected void populateSequenceLabelColumn(RowIndex rowIndex, String sequenceLabel) {
        populateSequenceLabelColumn(rowIndex, sequenceLabel, null, null);
    }

    protected void populateSequenceLabelColumn(RowIndex rowIndex, String sequenceLabel, String tip, String tipStyle) {
        if (tip == null || tip.length() == 0) {
            entityDetailsTable.setText(rowIndex.getCurrentRow(), 0, sequenceLabel);
        }
        else {
            SpanList spanList = new SpanList();
            spanList.addSpan(sequenceLabel + ":<br/><br/>&nbsp;&nbsp;", sequenceLabel);
            spanList.addSpan(tip, tipStyle);
            HTMLPanel htmlPanel = new HTMLPanel(spanList.toString());
            entityDetailsTable.setWidget(rowIndex.getCurrentRow(), 0, htmlPanel);
        }
        TableUtil.setDetailLabelCellStyle(entityDetailsTable, rowIndex.getCurrentRow());
    }

    protected void populateSequenceDataColumn(RowIndex rowIndex) {
        Panel expandablePanel =
                new SpanListExpander(getBaseEntitySequenceData().getSequenceSpanList(), SEQUENCE_DISPLAY_MIN_SIZE)
                        .getContentPanel();
        entityDetailsTable.setWidget(rowIndex.getCurrentRow(), 1, expandablePanel);
    }

    private void adjustSizeOfIntellectualPropertyWidget(int nRows) {
        if (ipNoticeBox != null) {
            entityDetailsTable.getFlexCellFormatter().setRowSpan(IP_NOTICE_ROW, IP_NOTICE_COL, nRows);
        }
    }

    private void addSequenceDownloadBox(final BaseSequenceEntity sequenceEntity) {
        sequenceDownloadBox = new DownloadBox("Downloads",
                null, /* actionLink */
                false, /* showActionLink */
                false /* showContent */);
        // add the download AA sequence link
        sequenceDownloadBox.add(new Link("Download " + sequenceEntity.getEntityType().getName() + " sequence",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        new PopupAboveLauncher(new DownloadSequencePopup(sequenceEntity,
                                sequenceEntity.getEntityType().getDescription() + " " + "sequence",
                                false)).showPopup(sender);
                    }
                }));
        // start on the 4th row because the first three are taken by the IP notice
        entityDetailsTable.setWidget(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL,
                sequenceDownloadBox);
        entityDetailsTable.getFlexCellFormatter().setRowSpan(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL,
                DOWNLOAD_BOX_HEIGHT);
        entityDetailsTable.getFlexCellFormatter().setHorizontalAlignment(DOWNLOAD_BOX_ROW, DOWNLOAD_BOX_COL,
                HorizontalPanel.ALIGN_LEFT);
    }

//    private void createIntellectualPropertySkeleton() {
//        ipNoticeBox = new SecondaryTitledBox("Intellectual Property Notice", false, true);
//        ipNoticeBox.setVisible(false);
//        ipNoticeBox.setStyleName("readDetailIPTitledBox");
//        ScrollPanel ipScrollPanel = new ScrollPanel();
//        ipNoticeBox.add(ipScrollPanel);
//        ipScrollPanel.setStyleName("readDetailIPScrollPanel");
//        ipNotice = new HTML();
//        ipScrollPanel.add(ipNotice);
//        entityDetailsTable.setWidget(IP_NOTICE_ROW, IP_NOTICE_COL,
//                ipNoticeBox);
//        // for now we only set it to span 3 rows - there's 1 for the accession,
//        // 1 for the defline and 1 for the sequence at least
//        // but this may change when the notice is actually displayed
//        entityDetailsTable.getFlexCellFormatter().setRowSpan(IP_NOTICE_ROW, IP_NOTICE_COL,
//                IP_NOTICE_HEIGHT);
//        entityDetailsTable.getFlexCellFormatter().setHorizontalAlignment(IP_NOTICE_ROW, IP_NOTICE_COL,
//                HorizontalPanel.ALIGN_LEFT);
//    }

    private Widget getTranslationTableLink(String translationTable) {
        if (translationTable != null && translationTable.length() > 0) {
            String ncbiTaxonomyURL = "http://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi";
            return new ExternalLink(translationTable, ncbiTaxonomyURL + "?" + "mode=c#SG" + translationTable);
        }
        else {
            return HtmlUtils.getHtml("Unknown", "text");
        }
    }

    public int getClearRangeBegin() {
        Read read = (Read) getBaseEntity();
        if (read.getClearRangeBegin() == null)
            return 0;
        else
            return read.getClearRangeBegin().intValue();
    }

    public int getClearRangeEnd() {
        Read read = (Read) getBaseEntity();
        if (read.getClearRangeEnd() == null)
            return 0;
        else
            return read.getClearRangeEnd().intValue();
    }

}
