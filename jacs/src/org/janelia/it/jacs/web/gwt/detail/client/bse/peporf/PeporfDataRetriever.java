
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceUIData;
import org.janelia.it.jacs.web.gwt.detail.client.service.protein.ProteinDetailService;
import org.janelia.it.jacs.web.gwt.detail.client.service.protein.ProteinDetailServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 28, 2008
 * Time: 1:20:49 PM
 */
public class PeporfDataRetriever {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfDataRetriever");

    private static ProteinDetailServiceAsync proteinDetailService =
            (ProteinDetailServiceAsync) GWT.create(ProteinDetailService.class);
    private static BSEntityServiceAsync entityService =
            (BSEntityServiceAsync) GWT.create(BSEntityService.class);

    static {
        ((ServiceDefTarget) proteinDetailService).setServiceEntryPoint("proteinDetail.srv");
        ((ServiceDefTarget) entityService).setServiceEntryPoint("bsDetail.srv");
    }

    private PeporfPanel peporfPanel;

    // Entity context for this retriever
    private BaseSequenceEntity bse;
    private Protein pro;
    private Peptide pep;
    private ORF orf;
    private Nucleotide nuc;

    PeporfDataRetriever(PeporfPanel peporfPanel) {
        this.peporfPanel = peporfPanel;
    }

    /**
     * Callback handler for protein cluster membership
     */

    private class GetProteinClusterMembershipCallback implements AsyncCallback {
        GetProteinClusterMembershipCallback() {
        }

        public void onFailure(Throwable exc) {
            logger.error("GetProteinClusterMembershipCallback failed", exc);
            peporfPanel.getClusterPanel().populateClusterInfoPanel(null,
                    "Error retrieving protein cluster data", exc);
        }

        public void onSuccess(Object result) {
            logger.debug("PeporfDataRetriever GetProteinClusterMembershipCallback onSuccess start");
            ProteinClusterMember proteinClusterInfo = (ProteinClusterMember) result;
            peporfPanel.getClusterPanel().populateClusterInfoPanel(proteinClusterInfo, null, null);
            logger.debug("PeporfDataRetriever GetProteinClusterMembershipCallback onSuccess end");
        }
    }

    /**
     * Callback handler for protein annotation
     */
    private class GetProteinAnnotationCallback implements AsyncCallback {
        GetProteinAnnotationCallback() {
        }

        public void onFailure(Throwable exc) {
            logger.error("GetProteinAnnotationCallback failed", exc);
            peporfPanel.getAnnotationPanel().populateAnnotationsPanel(null,
                    "Error retrieving protein annotations", exc);
        }

        public void onSuccess(Object result) {
            logger.debug("PeporfDataRetriever GetProteinAnnotationCallback onSuccess start");
            List geneAnnotations = (List) result;
            PeporfAnnotationPanel annotationPanel = peporfPanel.getAnnotationPanel();
            if (annotationPanel == null) {
                logger.debug("Annotation Panel is null");
            }
            else {
                logger.debug("Annotation panel is not null");
            }
            if (geneAnnotations == null) {
                logger.debug("geneAnnotations list is null");
            }
            else if (geneAnnotations.size() == 0) {
                logger.debug("geneAnnotations list size is 0");
            }
            else {
                logger.debug("geneAnnotations list is populated from service");
            }
            peporfPanel.getAnnotationPanel().populateAnnotationsPanel(geneAnnotations, null, null);
            logger.debug("PeporfDataRetriever GetProteinAnnotationCallback onSuccess end");
        }

    }

    /**
     * Callback handler for bses
     */

    private abstract class GetEntityCallback implements AsyncCallback {
        PeporfEntityPanel entityPanel;

        private GetEntityCallback(PeporfEntityPanel entityPanel) {
            this.entityPanel = entityPanel;
        }

        public void onFailure(Throwable throwable) {
            logger.error("PeporfDataBuilder GetEntityCallback failed: ", throwable);
            entityPanel.getLoadingLabel().setVisible(false);
            peporfPanel.setServiceErrorMessage();
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("PeporfDataRetriever GetEntityCallback onSuccess start");
                BaseSequenceEntity bse = (BaseSequenceEntity) result;
                setBse(bse);
                if (result != null) {
                    entityPanel.displayDetail(bse);
                    if (peporfPanel.isSequenceTooBigToDisplay(bse)) {
                        entityPanel.displaySequence(true);
                    }
                    else {
                        retrieveSequence(entityPanel, bse);
                    }
                }
                else {
                    entityPanel.getLoadingLabel().setVisible(false);
                }
                logger.debug("PeporfDataRetriever GetEntityCallback onSuccess end");
            }
            catch (RuntimeException e) {
                logger.error("PeporfDataRetriever GetEntityCallback onSuccess caught exception", e);
                throw e;
            }
        }

        public abstract void setBse(BaseSequenceEntity bse);
    }

    private class GetPeptideCallback extends GetEntityCallback {
        private GetPeptideCallback(PeporfEntityPanel entityPanel) {
            super(entityPanel);
        }

        public void setBse(BaseSequenceEntity bse) {
            pro = (Protein) bse;
            pep = pro;
            // The annotation panel must be called also when setting the pro/pep
            populateAnnotationPanel();
        }
    }

    private class GetOrfCallback extends GetEntityCallback {
        private GetOrfCallback(PeporfEntityPanel entityPanel) {
            super(entityPanel);
        }

        public void setBse(BaseSequenceEntity bse) {
            orf = (ORF) bse;
        }
    }

    private class GetNACallback extends GetEntityCallback {
        private GetNACallback(PeporfEntityPanel entityPanel) {
            super(entityPanel);
        }

        public void setBse(BaseSequenceEntity bse) {
            nuc = (Nucleotide) bse;
        }
    }

    /**
     * Callback handler for bse sequences
     */
    private class GetSequenceCallback implements AsyncCallback {
        private PeporfEntityPanel entityPanel;

        GetSequenceCallback(PeporfEntityPanel entityPanel) {
            this.entityPanel = entityPanel;
        }

        public void onFailure(Throwable throwable) {
            logger.error("GetSequenceCallback failed", throwable);
            entityPanel.getLoadingLabel().setVisible(false);
            BaseSequenceEntity bse = entityPanel.getEntity();
            String entityType = bse.getEntityType().getName();
            String entityAcc = bse.getAccession();
            MessageUtil.addServiceErrorMessage(entityPanel.getDetailBox(),
                    entityType + " Sequence",
                    entityType + " Accession: " + entityAcc);
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("PeporfDataRetriever GetSequenceCallback onSuccess start");
                if (result != null) {
                    SequenceUIData sequence = (SequenceUIData) result;
                    entityPanel.getTableBuilder().setBaseEntitySequenceData(sequence);
                    // since the sequence was retrieved entirely it is displayable
                    entityPanel.displaySequence(false);
                }
                else {
                    entityPanel.getLoadingLabel().setVisible(false);
                    BaseSequenceEntity bse = entityPanel.getEntity();
                    String entityAcc = bse.getAccession();
                    String entityType = bse.getEntityType().getName();
                    MessageUtil.addNotFoundErrorMessage(entityPanel.getDetailBox(),
                            entityType + " Sequence",
                            entityType + " Accession: " + entityAcc);
                }
                logger.debug("PeporfDataRetriever GetSequenceCallback onSuccess end");
            }
            catch (RuntimeException e) {
                logger.error("PeporfDataRetriever GetSequenceCallback onSuccess caught exception", e);
                throw e;
            }
        }
    }

    private void retrieveSequence(PeporfEntityPanel panel, BaseSequenceEntity bse) {
        int begin = panel.getSequenceBegin();
        int end = panel.getSequenceEnd();
        entityService.getSequenceUIData(bse.getAccession(),
                begin,
                end,
                peporfPanel.SEQUENCE_CHARS_PER_LINE,
                new GetSequenceCallback(panel));
    }

    /**
     * Retrieves the required data
     */
    void retrieveData() {
        logger.debug("PeporfDataRetriever retrieveData start");
        establishEntityContext();
        int bseCode = bse.getEntityType().getCode();
        populatePeptidePanel();
        populateOrfPanel();
        populateNaPanel();
        if (bseCode == EntityTypeGenomic.ENTITY_CODE_PEPTIDE || bseCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
            populateAnnotationPanel();
        }
        else {
            // do nothing because populateAnnotationPanel() is called by a callback-chain because
            // it depends on pep/pro being populated before it is called
        }
        populateMetadataPanel();
        logger.debug("PeporfDataRetriever retrieveData end");
    }

    private void establishEntityContext() {
        bse = (BaseSequenceEntity) peporfPanel.getEntity();
        int bseCode = bse.getEntityType().getCode();
        if (bseCode == EntityTypeGenomic.ENTITY_CODE_PEPTIDE) {
            pep = (Peptide) bse;
        }
        else if (bseCode == EntityTypeGenomic.ENTITY_CODE_PROTEIN) {
            pep = (Peptide) bse;
            pro = (Protein) pep;
            orf = pro.getOrfEntity();
            nuc = pro.getDnaEntity();
        }
        else if (bseCode == EntityTypeGenomic.ENTITY_CODE_ORF) {
            orf = (ORF) bse;
            pro = orf.getProteinEntity();
            pep = pro;
            nuc = orf.getDnaEntity();
        }
    }

    private void populateAnnotationPanel() {
        logger.debug("PeporfDataRetriever populateAnnotationPanel start");
        if (pep != null) {
            logger.debug("pep is not null - calling detail services to populate annotation panel");
            proteinDetailService.getProteinClusterInfo(pep.getAccession(), new GetProteinClusterMembershipCallback());
            proteinDetailService.getProteinAnnotations(pep.getAccession(), null, new GetProteinAnnotationCallback());
        }
        else {
            logger.debug("pep is null - skipping services to populate annotation panel");
        }
    }

    private void populateMetadataPanel() {
    }

    private void populatePeptidePanel() {
        logger.debug("PeporfDataRetriever populatePeptidePanel start");
        if (orf == bse && orf != null) {
            logger.debug("orf==bse && orf!=null");
            PeporfPeptidePanel peptidePanel = peporfPanel.getPeptidePanel();
            String peptideAcc = orf.getProteinAcc();
            if (pro == null && peptideAcc != null && peptideAcc.length() > 0) {
                logger.debug("Calling entityService with GetPeptideCallback()...");
                entityService.getEntity(peptideAcc, new GetPeptideCallback(peptidePanel));
            }
            else {
                logger.debug("We already have the protein - calling display or retrieve");
                // the ORF's peptide is available so check its size to see if it is displayable
                if (peporfPanel.isSequenceTooBigToDisplay(pro)) {
                    peptidePanel.displaySequence(true);
                }
                else {
                    // retrieve the peptide's sequence
                    retrieveSequence(peptidePanel, pro);
                }
            }
        }
        else {
            logger.debug("Skipping service calls to populate peptide panel");
            if (orf != bse) {
                logger.debug("orf != bse");
            }
            if (orf == null) {
                logger.debug("orf==null");
            }
        }
        logger.debug("PeporfDataRetriever populatePeptidePanel end");
    }

    private void populateOrfPanel() {
        if (pro == bse && pro != null) {
            PeporfOrfPanel orfPanel = peporfPanel.getOrfPanel();
            String orfAcc = pro.getOrfAcc();
            if (orf == null && orfAcc != null && orfAcc.length() > 0) {
                entityService.getEntity(orfAcc, new GetOrfCallback(orfPanel));
            }
            else if (orf != null) {
                // the Protein's ORF is already available
                if (peporfPanel.isSequenceTooBigToDisplay(orf)) {
                    orfPanel.displaySequence(true);
                }
                else {
                    // retrieve the ORF sequence
                    retrieveSequence(orfPanel, orf);
                }
            }
            else {
                // orf==null, therefore no orf to retrieve or populate
                logger.debug("No orf found for protein=" + pro.getAccession());
            }
        }
    }

    private void populateNaPanel() {
        if (bse != null && (pro == bse || orf == bse)) {
            PeporfSourceDNAPanel dnaPanel = peporfPanel.getSourceDNAPanel();
            String dnaAcc = null;
            if (pro == bse && pro != null) {
                dnaAcc = pro.getDnaAcc();
            }
            else if (orf == bse && orf != null) {
                dnaAcc = orf.getDnaAcc();
            }
            if (dnaAcc != null && nuc == null) {
                entityService.getEntity(dnaAcc, new GetNACallback(dnaPanel));
            }
            else if (dnaAcc != null) {
                retrieveSequence(dnaPanel, nuc);
            }
        }
    }

}
