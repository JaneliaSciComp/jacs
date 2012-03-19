
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SampleDetailPanel;
import org.janelia.it.jacs.web.gwt.search.client.Search;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.project.ProjectDetailPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationDetailPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 9, 2007
 * Time: 2:57:29 PM
 */
public class SearchEntityDetailPanelBuilder {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.SearchEntityDetailPanelBuilder");

    private Search searchController;

    public SearchEntityDetailPanelBuilder(Search searchController) {
        this.searchController = searchController;
    }

    /**
     * Creates a detail panel for a given entity. The type of entity and therefore
     * the type of the detail panel is inferred from the accession number using the AccessionIdentifierUtil-ity.
     * The factory supports detail panels for entity sequences, samples, projects and publications;
     *
     * @param accession
     * @param searchData
     * @param backLink   @return the corresponding detail panel for the type of given accession;
     * @throws IllegalArgumentException if the accession type is not recognized, e.g. cluster accession
     */
    public Panel createEntityDetailPanel(String accession, final SearchResultsData searchData, ActionLink backLink) {
        AccessionIdentifierUtil.AccessionType accTypeWithDesc = AccessionIdentifierUtil.getAccTypeWithDescription(accession);
        if (accTypeWithDesc == null) {
            throw new IllegalArgumentException("Unknown accession type for " + accession);
        }
        int accType = accTypeWithDesc.getType();
        Panel entityDetailPanel = null;
        if (accType == AccessionIdentifierUtil.READ_ACC ||
                accType == AccessionIdentifierUtil.ORF_ACC ||
                accType == AccessionIdentifierUtil.PROTEIN_ACC ||
                accType == AccessionIdentifierUtil.NCRNA_ACC ||
                accType == AccessionIdentifierUtil.SCAFFOLD_ACC ||
                accType == AccessionIdentifierUtil.NCBI_NT_ACC ||
                accType == AccessionIdentifierUtil.NCBI_AA_ACC ||
                accType == AccessionIdentifierUtil.NCBI_CNTG_ACC ||
                accType == AccessionIdentifierUtil.NCBI_GENF_ACC ||
                accType == AccessionIdentifierUtil.MISC_SEQ_ACC ||
                accType == AccessionIdentifierUtil.PROTEIN_CLUSTER_ACC) {
            DetailPanel detailPanel = new DetailPanel(searchController);
            detailPanel.rebuildPanel(accession, null/*page token*/, backLink);
            entityDetailPanel = detailPanel;
        }
        else if (accType == AccessionIdentifierUtil.PROJECT_ACC) {
            _logger.info("SearchEntityDetailPanelBuilder: looking up project by acc=" + accession);
            entityDetailPanel = new ProjectDetailPanel("Project Details", true /*show actionLink*/, accession, backLink);
        }
        else if (accType == AccessionIdentifierUtil.PUBLICATION_ACC) {
            entityDetailPanel = new PublicationDetailPanel("Publication Details",
                    accession, backLink, true /*show actionLink*/);
        }
        else if (accType == AccessionIdentifierUtil.BIOSAMPLE_ACC) {
            SampleDetailPanel sampleDetailPanel = new SampleDetailPanel("Sample Details",
                    accession, backLink, true /*show actionLink*/);
            sampleDetailPanel.setEntityListener(new EntityListener() {
                public void onEntitySelected(String entityId, Object entityData) {
                    searchData.setEntityDetailAcc(entityId);
                    searchController.refresh();
                }
            });
            entityDetailPanel = sampleDetailPanel;
        }
        return entityDetailPanel;
    }

}