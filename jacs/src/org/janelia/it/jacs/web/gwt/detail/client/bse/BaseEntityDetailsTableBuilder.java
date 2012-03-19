
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.export.SequenceExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.SpanListExpander;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.detail.client.util.TableUtil;

import java.util.ArrayList;

/**
 * Responsible for populating the entity details table
 */
public class BaseEntityDetailsTableBuilder {
    // The number of characters to display "...more" link at
    private static final int SEQUENCE_DISPLAY_MIN_SIZE = 314;

    protected BSEntityPanel parentPanel;
    protected FlexTable entityDetailsTable;
    private SecondaryTitledBox ipNoticeBox;
    private BaseSequenceEntity baseEntity;
    private SequenceUIData baseEntitySequenceData;
    private HTML ipNotice;

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public BaseEntityDetailsTableBuilder(FlexTable entityDetailsTable, BSEntityPanel parentPanel) {
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
        if (rowIndex == null) {
            rowIndex = getNextRowIndex();
        }
        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Entity Type", getBaseEntity().getEntityType().getName());
        if (getBaseEntity().getExternalSource() != null) {
            if (getBaseEntity().getExternalSource().equals("TGI") || getBaseEntity().getExternalSource().equals("ENS"))
                TableUtil.addTextRow(entityDetailsTable, rowIndex, "External Id", getBaseEntity().getExternalAcc() + " (" + getBaseEntity().getExternalSource() + ")");
            else {
                //TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "External Id", new ExternalLink(getBaseEntity().getExternalAcc()+" ("+getBaseEntity().getExternalSource()+")","http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?cmd=Retrieve&id="+getBaseEntity().getExternalAcc()));
                TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "External Id", new ExternalLink(getBaseEntity().getExternalAcc() + " (" + getBaseEntity().getExternalSource() + ")", "http://www.ncbi.nlm.nih.gov/sites/entrez?db=protein&cmd=&term=" + getBaseEntity().getExternalAcc()));
            }
        }
        if (getBaseEntity().getNcbiGINumber() != null)
            TableUtil.addWidgetRow(entityDetailsTable, rowIndex, "NCBI GI Number", new ExternalLink(getBaseEntity().getNcbiGINumber().toString(), "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?cmd=Retrieve&id=" + getBaseEntity().getNcbiGINumber().toString()));
        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Organism", getBaseEntity().getOrganism());
        if (getBaseEntity().getTaxonId() != null)
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Taxon Id", getBaseEntity().getTaxonId().toString());
//        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Locus", getBaseEntity().getLocus());
        if (getBaseEntity().getAssembly() != null)
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Assembly", getBaseEntity().getAssembly().getAssemblyAcc() + " " + getBaseEntity().getAssembly().getDescription());
        if (getBaseEntity().getSample() != null) {
            TableUtil.addTextRow(entityDetailsTable, rowIndex, "Sample", getBaseEntity().getSample().getTitle());
        }
//        TableUtil.addTextRow(entityDetailsTable, rowIndex, "Notes", getBaseEntity().getComment());
//        populateDefLine("Def Line", getBaseEntity().getDescription(), parentPanel.SEQUENCE_CHARS_PER_LINE, rowIndex);

/*
        Map deflineMap = (new DefLineFormat()).parseDefline(getBaseEntity().getDefline());
        if (deflineMap.get("core_cluster_id")!=null)
          TableUtil.addTextRow(entityDetailsTable, rowIndex, "Clore Cluster", (String) deflineMap.get("core_cluster_id"));
        if (deflineMap.get("pkl_family")!=null)
          TableUtil.addTextRow(entityDetailsTable, rowIndex, "PKL Family", (String) deflineMap.get("pkl_family"));
*/

        return rowIndex;
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
        adjustSizeOfIntellectualPropertyWidget(entityDetailsTable.getRowCount());
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

    protected RowIndex getNextRowIndex() {
        return new RowIndex(entityDetailsTable.getRowCount() + 1);
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
            entityDetailsTable.getFlexCellFormatter().setRowSpan(0, 2, nRows);
        }
    }
}
