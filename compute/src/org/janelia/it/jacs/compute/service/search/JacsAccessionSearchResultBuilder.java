
package org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class JacsAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    JacsAccessionSearchResultBuilder() {
    }

    List<AccessionSearchResult> retrieveAccessionSearchResult(String acc,
                                                              Long searchResultNodeId,
                                                              Session session)
            throws Exception {
        List<AccessionSearchResult> results = null;
        AccessionSearchResultBuilder accSearchBuilder =
                createCAMERAAccessionSearchResultBuilder(acc);
        if (accSearchBuilder != null) {
            results = accSearchBuilder.retrieveAccessionSearchResult(acc,
                    searchResultNodeId,
                    session);
        }
        return results;
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        throw new UnsupportedOperationException();
    }

    private AccessionSearchResultBuilder createCAMERAAccessionSearchResultBuilder(String acc) {
        // we use the upper case in order to determine the type of accession
        int accType = AccessionIdentifierUtil.getAccType(acc.toUpperCase());
        AccessionSearchResultBuilder accSearchResultBuilder = null;
        switch (accType) {
            case AccessionIdentifierUtil.CAMERA_READ_ACC:
            case AccessionIdentifierUtil.CAMERA_ORF_ACC:
            case AccessionIdentifierUtil.CAMERA_PROTEIN_ACC:
            case AccessionIdentifierUtil.CAMERA_NCRNA_ACC:
            case AccessionIdentifierUtil.CAMERA_SCAFFOLD_ACC:
            case AccessionIdentifierUtil.NCBI_GENF_ACC:
            case AccessionIdentifierUtil.NCBI_CNTG_ACC:
            case AccessionIdentifierUtil.NCBI_NT_ACC:
            case AccessionIdentifierUtil.NCBI_AA_ACC:
            case AccessionIdentifierUtil.MISC_SEQ_ACC:
                // potential sequence entity accession
                accSearchResultBuilder = new SequenceAccessionSearchResultBuilder();
                break;
            case AccessionIdentifierUtil.CAMERA_PROTEIN_CLUSTER_ACC:
                // potential protein cluster accession
                accSearchResultBuilder = new ClusterAccessionSearchResultBuilder();
                break;
            case AccessionIdentifierUtil.CAMERA_BIOSAMPLE_ACC:
                // potential sample accession
                accSearchResultBuilder = new SampleAccessionSearchResultBuilder();
                break;
            case AccessionIdentifierUtil.CAMERA_PROJECT_ACC:
                // potential project accession
                accSearchResultBuilder = new ProjectAccessionSearchResultBuilder();
                break;
            case AccessionIdentifierUtil.CAMERA_PUBLICATION_ACC:
                // potential publication accession
                accSearchResultBuilder = new PublicationAccessionSearchResultBuilder();
                break;
            default:
                break;
        }
        return accSearchResultBuilder;
    }

}
