
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.model.genomics.ProteinAnnotation;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortableTableClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 12:54:13 PM
 */
public class PeporfAnnotationPanel extends VerticalPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfAnnotationPanel");

    private static BSEntityServiceAsync entityService =
            (BSEntityServiceAsync) GWT.create(BSEntityService.class);

    static {
        ((ServiceDefTarget) entityService).setServiceEntryPoint("bsDetail.srv");
    }

    private static final int CATEGORY_COLUMN = 0;
    private static final int ID_COLUMN = 1;
    private static final int DESCRIPTION_COLUMN = 2;
    private static final int ASSIGNED_BY_COLUMN = 3;

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private PeporfPanel parentPanel;
    private TitledBox annotationsBox;
    private SortableTable annoTable;
    private PagingPanel annotationsTabPanel;

    PeporfAnnotationPanel(PeporfPanel parentPanel) {
        super();
        this.parentPanel = parentPanel;
    }

    public void initialize() {
        annotationsBox = new TitledBox("Protein Annotation", false);
        annotationsBox.setStyleName("peptideAnnotationBox");
        add(annotationsBox);
    }

    public void display() {
        logger.debug("PeporfAnnotationPanel display...");
    }

    void populateAnnotationsPanel(List proteinAnnotations, String errorMessage, Throwable error) {
        logger.debug("PeporfAnnotationPanel populateAnnotationsPanel start");
        if (error != null) {
            if (annotationsTabPanel != null) {
                annotationsBox.clear();
            }
            annotationsBox.add(HtmlUtils.getHtml((errorMessage != null ? errorMessage : "Error:") +
                    " " + error.toString(),
                    "error"));
        }
        else {
            if (proteinAnnotations == null || proteinAnnotations.size() == 0) {
                if (annotationsTabPanel != null) {
                    annotationsBox.clear();
                }
                annotationsBox.add(HtmlUtils.getHtml("No annotation available", "text"));
            }
            else {
                if (annotationsTabPanel == null) {
                    createAnnotationsTable();
                    annotationsBox.add(annotationsTabPanel);
                }
                Iterator pi = proteinAnnotations.iterator();
                ArrayList<String> evidenceList = new ArrayList<String>();
                while (pi.hasNext()) {
                    ProteinAnnotation pa = (ProteinAnnotation) pi.next();
                    evidenceList.add(pa.getAssignedBy());
                }
                entityService.getExternalEvidenceLinks(evidenceList,
                        new EvidenceLinkCallback(proteinAnnotations));
            }
        }
        logger.debug("PeporfAnnotationPanel populateAnnotationsPanel end");
    }

    private void createAnnotationsTable() {
        annoTable = new SortableTable();
        annoTable.addColumn(new TextColumn("Category"));
        annoTable.addColumn(new TextColumn("ID"));
        annoTable.addColumn(new TextColumn("Description"));
        annoTable.addColumn(new TextColumn("Evidence"));
        annotationsTabPanel = new PagingPanel(annoTable,
                new String[]{"5", "10", "20"},
                10,
                true,
                true,
                null,
                false,
                PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                "PeporfAnnotation");
        annotationsTabPanel.addAdvancedSortClickListener(new AdvancedSortableTableClickListener(annoTable,
                annoTable.getAllSortableColumns()));
    }

    private TableRow formatAnnotation(ProteinAnnotation proteinAnnotation, String evidenceLink) {
        TableRow row = new TableRow();
        String annoId = proteinAnnotation.getAnnotationID();
        if (annoId.startsWith("EC:"))
            row.setValue(ID_COLUMN, new TableCell(annoId,
                    new ExternalLink(annoId,
                            "http://au.expasy.org/enzyme/" + annoId.substring(3))));
        else if (annoId.startsWith("GO:"))
            row.setValue(ID_COLUMN, new TableCell(annoId,
                    new ExternalLink(annoId,
                            "http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&search_constraint=terms&query=" +
                                    annoId)));
        else if (annoId.startsWith("TR:"))
            row.setValue(ID_COLUMN, new TableCell(annoId));
        else
            row.setValue(ID_COLUMN, new TableCell("-"));
        row.setValue(CATEGORY_COLUMN, new TableCell(proteinAnnotation.getAnnotationType()));
        if (annoId.startsWith("EC:"))
            row.setValue(DESCRIPTION_COLUMN, new TableCell(AnnotationUtil.arrowizeAnnotationDescription(proteinAnnotation.getDescription()),
                    new FulltextPopperUpperHTML(AnnotationUtil.arrowizeAnnotationDescription(proteinAnnotation.getDescription()), MAX_DESCRIPTION_LENGTH)));
        else
            row.setValue(DESCRIPTION_COLUMN, new TableCell(proteinAnnotation.getDescription(),
                    new FulltextPopperUpperHTML(proteinAnnotation.getDescription(), MAX_DESCRIPTION_LENGTH)));
        if (evidenceLink != null) {
            row.setValue(ASSIGNED_BY_COLUMN, new TableCell(proteinAnnotation.getAssignedBy(),
                    new ExternalLink(proteinAnnotation.getAssignedBy(), evidenceLink)));
        }
        else {
            row.setValue(ASSIGNED_BY_COLUMN, new TableCell(proteinAnnotation.getAssignedBy()));
        }
        return row;

    }

    private class EvidenceLinkCallback implements AsyncCallback {
        List proteinAnnotations;

        public EvidenceLinkCallback(List proteinAnnotations) {
            this.proteinAnnotations = proteinAnnotations;
        }

        public void onFailure(Throwable t) {
            // forward null list
            populateAnnotationTable(proteinAnnotations, null);
        }

        public void onSuccess(Object result) {
            List evidenceLinks = (List) result;
            populateAnnotationTable(proteinAnnotations, evidenceLinks);
        }
    }

    private void populateAnnotationTable(List proteinAnnotations, List evidenceLinks) {
        List goAnnotationRows = new ArrayList();
        Iterator evidenceIter = null;
        if (evidenceLinks != null && evidenceLinks.size() == proteinAnnotations.size()) {
            evidenceIter = evidenceLinks.iterator();
        }
        for (Iterator goItr = proteinAnnotations.iterator(); goItr.hasNext();) {
            String evidenceLink = null;
            if (evidenceIter != null) {
                evidenceLink = (String) evidenceIter.next();
            }
            ProteinAnnotation goAnnotation = (ProteinAnnotation) goItr.next();
            TableRow goAnnotationRow = formatAnnotation(goAnnotation, evidenceLink);
            goAnnotationRows.add(goAnnotationRow);
        }
        annotationsTabPanel.getPaginator().setData(goAnnotationRows);
        annotationsTabPanel.first();
    }

}
