
package org.janelia.it.jacs.web.gwt.detail.client;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBICntgPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBIGenfPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi.NCBINtPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.rna.RNAPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 15, 2007
 * Time: 10:38:37 AM
 */
public class DetailSubPanelFactory {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanelBuilderFactory");

    private String acc;
    private int accType = -1;
    private int previousAccType = -1;
    private DetailSubPanel subPanel;

    protected DetailSubPanelFactory() {
    }

    /**
     * Return the appropriate DetailSubPanel instance given a Camera Accession
     *
     * @param acc Camera Accession
     * @return DetailPanelBuilder instance
     */
    public DetailSubPanel getDetailSubPanel(String acc) {
        try {
            setAccType(acc);
            if (subPanel == null || getAccType() != getPreviousAccType()) {
                // Create a potentially different DetailSubPanel only if the accession type has changed
                // or if subPanel instance is null
                createDetailSubPanel();
            }
            subPanel.setAcc(acc);
            setPreviousAccType(getAcc());
            if (logger.isDebugEnabled()) {
                logger.debug("DetailSubPanelFactory createDetailSubPanel returned DetailSubPanel " + (subPanel == null ? "null" : "of type " + subPanel.getClass().getName()));
            }
            return subPanel;
        }
        catch (RuntimeException e) {
            logger.error("DetailPanel getDetailSubPanel caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create the appropriate DetailSubPanel instance using the Camera Accession supplied in getDetailSubPanel
     *
     * @return DetailPanelBuilder instance
     */
    private DetailSubPanel createDetailSubPanel() {
        switch (getAccType()) {
            case AccessionIdentifierUtil.READ_ACC:
                subPanel = new ReadPanel();
                break;
            case AccessionIdentifierUtil.ORF_ACC:
                subPanel = new PeporfPanel();
                break;
            case AccessionIdentifierUtil.PROTEIN_ACC:
                subPanel = new PeporfPanel();
                break;
            case AccessionIdentifierUtil.NCRNA_ACC:
                subPanel = new RNAPanel();
                break;
            case AccessionIdentifierUtil.SCAFFOLD_ACC:
                subPanel = new ScaffoldPanel();
                break;
            case AccessionIdentifierUtil.NCBI_GENF_ACC:
                subPanel = new NCBIGenfPanel();
                break;
            case AccessionIdentifierUtil.NCBI_CNTG_ACC:
                subPanel = new NCBICntgPanel();
                break;
            case AccessionIdentifierUtil.NCBI_NT_ACC:
                subPanel = new NCBINtPanel();
                break;
            case AccessionIdentifierUtil.NCBI_AA_ACC:
                subPanel = new PeporfPanel();
                break;
            default:
                throw new InvalidAccessionException("Invalid accession: " + getAcc());
        }
        return subPanel;
    }

    private int getAccType() {
        return accType;
    }

    private void setAccType(String acc) {
        this.acc = acc;
        this.accType = AccessionIdentifierUtil.getAccType(acc);
    }

    private String getAcc() {
        return acc;
    }

    private int getPreviousAccType() {
        return previousAccType;
    }

    private void setPreviousAccType(String acc) {
        this.previousAccType = AccessionIdentifierUtil.getAccType(acc);
    }
}
