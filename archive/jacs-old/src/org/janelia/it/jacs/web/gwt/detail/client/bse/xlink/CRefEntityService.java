
package org.janelia.it.jacs.web.gwt.detail.client.bse.xlink;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BseEntityDetail;
import org.janelia.it.jacs.model.genomics.PeptideDetail;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;

import java.util.List;

/**
 * GWT RemoteService for retrieving correlated entities
 *
 * @author Cristian Goina
 */
public interface CRefEntityService extends RemoteService {

    /**
     * @param readAccNo the accession number of the entity of interest
     * @return the number of correlated assembly (scaffolds) entities
     * @throws GWTServiceException
     */
    int getNumAssembledScaffoldForReadByAccNo(String readAccNo)
            throws GWTServiceException;

    /**
     * @param readAccNo  the accession number of the read
     * @param startIndex search offset
     * @param numRecords maximum number of records to be retrieved; if < 0 there's no maximum limit
     * @param sortArgs   sorting options
     * @return a list of correlated assemblies, optionally sorted
     * @throws GWTServiceException
     */
    List<ScaffoldReadAlignment> getAssembledScaffoldForReadByAccNo(String readAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @return the number of correlated ncRNA entities
     * @throws GWTServiceException
     */
    int getNumRelatedNCRNAs(String entityAccNo)
            throws GWTServiceException;

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @param startIndex  search offset
     * @param numRecords  maximum number of records to be retrieved; if < 0 there's no maximum limit
     * @param sortArgs    sorting options
     * @return a list of correlated ncRNAs, optionally sorted
     * @throws GWTServiceException
     */
    List<BaseSequenceEntity> getRelatedNCRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @return the number of correlated ORF entities
     * @throws GWTServiceException
     */
    int getNumRelatedORFs(String entityAccNo)
            throws GWTServiceException;

    /**
     * @param entityAccNo the accession number of the entity of interest
     * @param startIndex  search offset
     * @param numRecords  maximum number of records to be retrieved; if < 0 there's no maximum limit
     * @param sortArgs    sorting options
     * @return a list of correlated ORFs, optionally sorted
     * @throws GWTServiceException
     */
    List<BaseSequenceEntity> getRelatedORFs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * @param entityAccNo
     * @param startIndex
     * @param numRecords
     * @param sortArgs
     * @return
     * @throws GWTServiceException
     */
    List<BseEntityDetail> getRelatedORFsAndRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws GWTServiceException;

    int getNumFeatures(String entityAccNo)
            throws GWTServiceException;

    List<BaseSequenceEntity> getFeatures(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * @param scaffoldAccNo the accession number of the entity of interest
     * @return the number of reads that are part of the given scaffold
     * @throws GWTServiceException
     */
    int getNumOfReadsForScaffoldByAccNo(String scaffoldAccNo)
            throws GWTServiceException;

    /**
     * @param scaffoldAccNo the accession number of the scaffold
     * @param startIndex    search offset
     * @param numRecords    maximum number of records to be retrieved; if < 0 there's no maximum limit
     * @param sortArgs      sorting options
     * @return a list of reads that are part of the scaffold, optionally sorted
     * @throws GWTServiceException
     */
    List<ScaffoldReadAlignment> getReadsForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * @param scaffoldAccNo the accession number of the entity of interest
     * @return the number of peptides that are part of the given scaffold
     * @throws GWTServiceException
     */
    int getNumOfPeptidesForScaffoldByAccNo(String scaffoldAccNo)
            throws GWTServiceException;

    /**
     * @param scaffoldAccNo the accession number of the scaffold
     * @param startIndex    search offset
     * @param numRecords    maximum number of records to be retrieved; if < 0 there's no maximum limit
     * @param sortArgs      sorting options
     * @return a list of peptides that are part of the scaffold, optionally sorted
     * @throws GWTServiceException
     */
    List<PeptideDetail> getPeptidesForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException;

}
