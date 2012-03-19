
package org.janelia.it.jacs.web.gwt.detail.client.bse.rna;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.genomics.NonCodingRNA;
import org.janelia.it.jacs.model.genomics.Nucleotide;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceUIData;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;

/**
 * Responsible for retrieving NonCodingRNA entity data
 *
 * @author Cristian Goina
 * @autor Tareq Nabeel
 */
public class RNADnaDataRetriever {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.ncRNA.RNADnaDataRetriever");

    private RNAPanel rnaPanel;
    private BSEntityServiceAsync entityService;

    public RNADnaDataRetriever(RNAPanel rnaPanel, BSEntityServiceAsync entityService) {
        this.rnaPanel = rnaPanel;
        this.entityService = entityService;
    }

    /**
     * Callback handler for entity's details
     */
    private class GetNAEntityCallback implements AsyncCallback {
        private RNAPanel rnaPanel;

        GetNAEntityCallback(RNAPanel rnaPanel) {
            this.rnaPanel = rnaPanel;
        }

        public void onFailure(Throwable throwable) {
            logger.error("GetNAEntityCallback failed: ", throwable);
            rnaPanel.getNASequenceLoadingLabel().setVisible(false);
            rnaPanel.setServiceErrorMessage();
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("GetNAEntityCallback succeeded ");
                Nucleotide naEntity = (Nucleotide) result;
                if (naEntity != null) {
                    rnaPanel.setNAEntity(naEntity);
                    rnaPanel.displayNAEntity();
                    // the NCRNA's NA sequence is available
                    if (rnaPanel.isSequenceTooBigToDisplay(naEntity)) {
                        rnaPanel.displayNASequence(true);
                    }
                    else {
                        NonCodingRNA ncRNA = (NonCodingRNA) rnaPanel.getEntity();
                        // retrieve the actual sequence
                        retrieveSequence(ncRNA);
                    }
                }
                else {
                    rnaPanel.getNASequenceLoadingLabel().setVisible(false);
                }
            }
            catch (RuntimeException e) {
                logger.error("GetNAEntityCallback onSuccess caught exception", e);
                throw e;
            }
        }
    }

    /**
     * Callback handler for entity's sequence
     */
    private class GetNASequenceCallback implements AsyncCallback {
        private RNAPanel rnaPanel;

        GetNASequenceCallback(RNAPanel rnaPanel) {
            this.rnaPanel = rnaPanel;
        }

        public void onFailure(Throwable throwable) {
            logger.error("GetNASequenceCallback failed", throwable);
            rnaPanel.getNASequenceLoadingLabel().setVisible(false);
            Nucleotide naEntity = rnaPanel.getNAEntity();
            String naAcc = naEntity.getAccession();
            MessageUtil.addServiceErrorMessage(rnaPanel.getNADetailBox(),
                    "DNA Sequence",
                    "DNA Accession: " + naAcc);
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("GetNASequenceCallback succeeded ");
                if (result != null) {
                    SequenceUIData naSequence = (SequenceUIData) result;
                    rnaPanel.setNASequenceUIData(naSequence);
                    // since the sequence was retrieved entirely it is displayable
                    rnaPanel.displayNASequence(false);
                }
                else {
                    rnaPanel.getNASequenceLoadingLabel().setVisible(false);
                    Nucleotide naEntity = rnaPanel.getNAEntity();
                    String naAcc = naEntity.getAccession();
                    MessageUtil.addServiceErrorMessage(rnaPanel.getNADetailBox(),
                            "DNA Sequence",
                            "DNA Accession: " + naAcc);
                }
            }
            catch (RuntimeException e) {
                logger.error("GetNASequenceCallback onSuccess caught exception", e);
                throw e;
            }
        }
    }

    /**
     * Retrieves the required data
     */
    protected void retrieveData() {
        logger.info("Retrieve NA");
        NonCodingRNA ncRNA = (NonCodingRNA) rnaPanel.getEntity();
        String naAcc = ncRNA.getDnaAcc();
        if (naAcc != null && naAcc.length() > 0) {
            Nucleotide naEntity = ncRNA.getDnaEntity();
            if (naEntity == null) {
                // retrieve the NA entity data
                entityService.getEntity(naAcc, new RNADnaDataRetriever.GetNAEntityCallback(rnaPanel));
            }
            else {
                if (rnaPanel.isSequenceTooBigToDisplay(naEntity)) {
                    rnaPanel.displayNASequence(true);
                }
                else {
                    retrieveSequence(ncRNA);
                }
            }
        }
    }

    private void retrieveSequence(NonCodingRNA ncRNA) {
        String dnaAcc = ncRNA.getDnaAcc();
        int dnaBegin = 0;
        if (ncRNA.getDnaBegin() != null) {
            dnaBegin = ncRNA.getDnaBegin().intValue();
        }
        int dnaEnd = 0;
        if (ncRNA.getDnaEnd() != null) {
            dnaEnd = ncRNA.getDnaEnd().intValue();
        }
        entityService.getSequenceUIData(dnaAcc,
                dnaBegin,
                dnaEnd,
                rnaPanel.SEQUENCE_CHARS_PER_LINE,
                new RNADnaDataRetriever.GetNASequenceCallback(rnaPanel));
    }

}
