
package org.janelia.it.jacs.web.gwt.detail.client;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.GenericPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBICntgPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBIGenfPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBINtPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.rna.RNAPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.rna.RNAPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldPanel;
import org.janelia.it.jacs.web.gwt.detail.client.cluster.ClusterPanel;
import org.janelia.it.jacs.web.gwt.detail.client.cluster.ClusterPanelBuilder;


/**
 * This class returns the appropriate Detail Sub-Panel Builder class given a Camera Accession
 *
 * @author Tareq Nabeel
 */
public class DetailSubPanelBuilderFactory {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanelBuilderFactory");

    private String acc;
    private AccessionIdentifierUtil.AccessionType accType = null;
    private AccessionIdentifierUtil.AccessionType previousAccType = null;
    private DetailPanelBuilder subPanelBuilder;

    protected DetailSubPanelBuilderFactory() {
    }

    /**
     * Return the appropriate Detail Sub-Panel Builder class given a Camera Accession and an optional ActionLink
     *
     * @param parentPanel
     * @param acc         Camera Accession
     * @param backLink    ActionLink to add to Detail Panel e.g. for taking user back to previous page
     * @return DetailPanelBuilder instance
     */
    public DetailPanelBuilder getDetailPanelBuilder(DetailPanel parentPanel, String acc, ActionLink backLink) {
        logger.debug("getDetailPanelBuilder called with acc=" + acc);
        try {
            setAccInfo(acc);
            if (subPanelBuilder == null || getAccType() != getPreviousAccType()) {
                // Create a potentially different builder only if the accession type has changed
                // or if subPanelBuilder instance is null
                DetailSubPanel subPanel = createDetailSubPanel(parentPanel);
                subPanelBuilder = createDetailPanelBuilder(subPanel);
                // copy the current accession info into the previous accession info
                // for future invocations
                previousAccType = accType;
            }
            subPanelBuilder.setAcc(acc);
            subPanelBuilder.setActionLink(backLink);
            logger.debug("DetailSubPanelBuilderFactory getDetailPanelBuilder returning subPanelBuilder " + (subPanelBuilder == null ? "null" : "of type " + subPanelBuilder.getClass().getName()));
            return subPanelBuilder;
        }
        catch (RuntimeException e) {
            logger.error("DetailSubPanelBuilderFactory getDetailPanelBuilder caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates the appropriate Detail Sub-Panel Builder class using the Camera Accession supplied to getDetailPanelBuilder
     *
     * @return DetailPanelBuilder instance
     */
    private DetailPanelBuilder createDetailPanelBuilder(DetailSubPanel subPanel) {
        DetailPanelBuilder newPanelBuilder = null;
        switch (getAccType()) {
            case AccessionIdentifierUtil.READ_ACC:
                newPanelBuilder = new ReadPanelBuilder(subPanel);
                break;
            case AccessionIdentifierUtil.ORF_ACC:
                newPanelBuilder = new PeporfPanelBuilder(subPanel);
                break;
            case AccessionIdentifierUtil.PROTEIN_ACC:
                newPanelBuilder = new PeporfPanelBuilder(subPanel);
                break;
            case AccessionIdentifierUtil.NCRNA_ACC:
                newPanelBuilder = new RNAPanelBuilder(subPanel);
                break;
            case AccessionIdentifierUtil.NCBI_AA_ACC:
                newPanelBuilder = new PeporfPanelBuilder(subPanel);
                break;
            case AccessionIdentifierUtil.PROTEIN_CLUSTER_ACC:
                newPanelBuilder = new ClusterPanelBuilder(subPanel);
                break;
            default:
                newPanelBuilder = new BSEntityPanelBuilder(subPanel);
                break;
/*
            case AccessionIdentifierUtil.SCAFFOLD_ACC:
            case AccessionIdentifierUtil.NCBI_GENF_ACC:
            case AccessionIdentifierUtil.NCBI_CNTG_ACC:
            case AccessionIdentifierUtil.NCBI_NT_ACC:
                newPanelBuilder = new BSEntityPanelBuilder(subPanel);
                break;
            default:
                throw new InvalidAccessionException("Invalid accession: " + getAcc());
*/
        }
        return newPanelBuilder;
    }

    /**
     * Create the appropriate DetailSubPanel instance using the Camera Accession supplied in getDetailSubPanel
     *
     * @return DetailPanelBuilder instance
     */
    private DetailSubPanel createDetailSubPanel(DetailPanel parentPanel) {
        DetailSubPanel newSubPanel = null;
        switch (getAccType()) {
            case AccessionIdentifierUtil.READ_ACC:
                newSubPanel = new ReadPanel();
                break;
            case AccessionIdentifierUtil.ORF_ACC:
                newSubPanel = new PeporfPanel(PeporfPanel.ORF_DETAIL_TYPE);
                break;
            case AccessionIdentifierUtil.PROTEIN_ACC:
                newSubPanel = new PeporfPanel(PeporfPanel.PEPTIDE_DETAIL_TYPE);
                break;
            case AccessionIdentifierUtil.NCRNA_ACC:
                newSubPanel = new RNAPanel();
                break;
            case AccessionIdentifierUtil.SCAFFOLD_ACC:
                newSubPanel = new ScaffoldPanel();
                break;
            case AccessionIdentifierUtil.NCBI_GENF_ACC:
                newSubPanel = new NCBIGenfPanel();
                break;
            case AccessionIdentifierUtil.NCBI_CNTG_ACC:
                newSubPanel = new NCBICntgPanel();
                break;
            case AccessionIdentifierUtil.NCBI_NT_ACC:
                newSubPanel = new NCBINtPanel();
                break;
            case AccessionIdentifierUtil.NCBI_AA_ACC:
                newSubPanel = new PeporfPanel(PeporfPanel.PEPTIDE_DETAIL_TYPE);
                break;
            case AccessionIdentifierUtil.PROTEIN_CLUSTER_ACC:
                newSubPanel = new ClusterPanel();
                break;
            default:
                newSubPanel = new GenericPanel();
        }
        newSubPanel.setParentPanel(parentPanel);
        return newSubPanel;
    }

    private String getAcc() {
        return acc;
    }

    private void setAccInfo(String acc) {
        this.acc = acc;
        this.accType = AccessionIdentifierUtil.getAccTypeWithDescription(acc);
    }

    private int getAccType() {
        return accType != null ? accType.getType() : -1;
    }

    private int getPreviousAccType() {
        return previousAccType != null ? previousAccType.getType() : -1;
    }

}
