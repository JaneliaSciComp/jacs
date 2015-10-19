
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Mar 30, 2006
 * Time: 10:36:03 AM
 */
public interface FeatureDAO extends DAO {

//    List<Chromosome> getAllMicrobialChromosomes() throws DataAccessException, DaoException;

    List<BaseSequenceEntity> findAllBse() throws DataAccessException, DaoException;

    BaseSequenceEntity findBseByUid(Long featureUid) throws DataAccessException, DaoException;

    BaseSequenceEntity findBseByAcc(String accesion) throws DataAccessException, DaoException;

    BaseSequenceEntity findBseByAccOrId(String accOrId) throws DataAccessException, DaoException;

    List<Read> findReadsByLibraryId(String libraryId) throws DataAccessException, DaoException;

    Integer getNumReadsFromSample(String sampleAcc) throws DataAccessException, DaoException;

    List<Read> getPagedReadsFromSample(String sampleAcc,
                                       Set<String> readAccessions,
                                       int startIndex,
                                       int numRows,
                                       SortArgument[] sortArgs) throws DataAccessException, DaoException;

    /**
     * This method returns a Read given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    Read getReadByBseEntityId(Long bseEntityId) throws DaoException;

    /**
     * Returns a Read instance with it's library instance loaded
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    Read getReadWithSequenceByBseEntityId(Long bseEntityId) throws DaoException;

    /**
     * Returns a Read instance with it's library instance loaded
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    Read getReadWithLibraryByBseEntityId(Long bseEntityId) throws DaoException;

    /**
     * This method returns a Read given an accesion
     *
     * @param accesion
     * @return
     * @throws DaoException
     */
    Read getReadByAccesion(String accesion) throws DaoException;

    /**
     * This method retrieves all mates of the read identified by the <code>accession</code>.
     *
     * @param accession
     * @return the list of paired reads
     * @throws DaoException
     */
    List<Read> getPairedReadsByAccession(String accession) throws DaoException;

    /**
     * This method retrieves all mates of the read identified by the <code>entityId</code>.
     *
     * @param entityId
     * @return the list of paired reads
     * @throws DaoException
     */
    List<Read> getPairedReadsByEntityId(Long entityId) throws DaoException;

    /**
     * This method returns a Read given a BaseSequenceEntity Id
     *
     * @param accession
     * @return
     * @throws DaoException
     */
    Read getReadWithLibraryByAccesion(String accession) throws DaoException;

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */

    BioSequence getBioSequenceByBseEntityId(Long bseEntityId) throws DaoException;

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param accesion
     * @return
     * @throws DaoException
     */
    BioSequence getBioSequenceByAccession(String accesion) throws DaoException;

    /**
     * This method returns a BioSequence given a BaseSequenceEntity Id
     *
     * @param bseEntityId
     * @return
     * @throws DaoException
     */
    String getBioSequenceTextByBseEntityId(Long bseEntityId) throws DaoException;

    Map<String, String> getDeflinesBySeqUid(Set<String> SeqUid) throws DataAccessException, DaoException;

    List<BaseSequenceEntity> getSubjectBSEList_ByBlastHitIdList(List<Long> blastHitIdList) throws DaoException;

    List<BaseSequenceEntity> getQueryBSEList_ByBlastHitIdList(List<Long> blastHitIdList) throws DaoException;

    Set<BaseSequenceEntity> getSubjectBSESet_ByResultNode(Long resultNodeId) throws DaoException;

    Set<BaseSequenceEntity> getQueryBSESet_ByResultNode(Long resultNodeId) throws DaoException;

    Long getNumSubjectBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException;

    Long getNumQueryBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException;

    Set<BaseSequenceEntity> getSubjectBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException;

    Set<BaseSequenceEntity> getQueryBSESet_ByTaskId(Long taskId, Set<Long> blastHitIdSet) throws DaoException;

    Long getResultNodeIdbyBlastHitId(Long blastHitId) throws DaoException;

    // Map from BSE ID to BSE
    Map<Long, BaseSequenceEntity> getSubjectBses(Long resultNodeId) throws DaoException;

    Map<Long, BaseSequenceEntity> getSubjectBses(Set<Long> blastHitIdSet) throws DaoException;

    // Map from BSE ID's and ACC's to Read
    Map<Long, Read> getReadEntityIdToReadMap(Long resultNodeId) throws DaoException;

    Map<Long, Read> getReadEntityIdToReadMap(Set<Long> blastHitIdSet) throws DaoException;

    Map<String, Read> getReadsByIdSet(Collection<String> readIdSet) throws DaoException;

    List<Object[]> getReadsByAccessions(String[] accSet) throws DaoException;

    List<BaseSequenceEntity> getSequenceEntitiesByAccessions(Collection<String> accCollection) throws DaoException;

    Long getNumBlastResultsByTaskOrIDs(Long taskId,
                                       Set<Long> blastHitIdSet,
                                       boolean includeAllSampleMaterials) throws DaoException;

    List<Object[]> getBlastResultsByTaskOrIDs(Long taskId,
                                              Set<Long> blastHitIds,
                                              int startIndex,
                                              int numRows,
                                              boolean includeHSPRanking,
                                              SortArgument[] sortArgs) throws DaoException;

    // Support for Read Detail
    int getNumScaffoldsForReadByAccNo(String readAccNo) throws DaoException;

//    List<ScaffoldReadAlignment> getScaffoldsForReadByAccNo(String readAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException;

    int getNumRelatedNCRNAs(String entityAccNo) throws DaoException;

    List<BaseSequenceEntity> getRelatedNCRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException;

    int getNumRelatedORFs(String entityAccNo) throws DaoException;

    List<BaseSequenceEntity> getRelatedORFs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException;

//    List<BseEntityDetail> getRelatedORFsAndRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException;

    int getNumReadFeaturesBySubclassName(String[] name, String entityAccNo) throws DaoException;

    List<BaseSequenceEntity> getReadFeaturesBySubclassName(String[] name,
                                                           String entityAccNo,
                                                           int startIndex,
                                                           int numRecords,
                                                           SortArgument[] sortArgs)
            throws DaoException;

    // Support for Protein Detail
//    ProteinClusterMember getProteinClusterMemberInfo(String proteinAcc) throws DaoException;
//
//    List<ProteinAnnotation> getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs)
//            throws DaoException;
//
//    // Support for Scaffold Detail
//    int getNumOfReadsForScaffoldByAccNo(String scaffoldAccNo) throws DaoException;
//
//    List<ScaffoldReadAlignment> getReadsForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
//            throws DaoException;
//
//    // Support for Cluster Detail
//    ProteinCluster getProteinCoreCluster(String clusterAcc) throws DaoException;
//
//    ProteinCluster getProteinFinalCluster(String clusterAcc) throws DaoException;
//
//    List<ClusterAnnotation> getCoreClusterAnnotations(String clusterAcc, SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ClusterAnnotation> getFinalClusterAnnotations(String clusterAcc, SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ProteinCluster> getPagedCoreClustersFromFinalCluster(String finalClusterAcc,
//                                                              Set<String> clusterMemberAccs,
//                                                              int startIndex,
//                                                              int numRows,
//                                                              SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ProteinClusterMember> getPagedNRSeqMembersFromCoreCluster(String coreClusterAcc,
//                                                                   Set<String> clusterMemberAccs,
//                                                                   int startIndex,
//                                                                   int numRows,
//                                                                   SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ProteinClusterMember> getPagedSeqMembersFromCoreCluster(String coreClusterAcc,
//                                                                 Set<String> clusterMemberAccs,
//                                                                 int startIndex,
//                                                                 int numRows,
//                                                                 SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ProteinClusterMember> getPagedNRSeqMembersFromFinalCluster(String finalClusterAcc,
//                                                                    Set<String> clusterMemberAccs,
//                                                                    int startIndex,
//                                                                    int numRows,
//                                                                    SortArgument[] sortArgs)
//            throws DaoException;
//
//    List<ProteinClusterMember> getPagedSeqMembersFromFinalCluster(String finalClusterAcc,
//                                                                  Set<String> clusterMemberAccs,
//                                                                  int startIndex,
//                                                                  int numRows,
//                                                                  SortArgument[] sortArgs)
//            throws DaoException;

//    int getNumMatchingRepsFromCoreClusterWithAnnotation(String coreClusterAcc, String annotationId)
//            throws DaoException;

//    List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromCoreClusterWithAnnotation(String coreClusterAcc,
//                                                                                           String annotationId,
//                                                                                           Set<String> clusterMemberAccs,
//                                                                                           int startIndex,
//                                                                                           int numRows,
//                                                                                           SortArgument[] sortArgs)
//            throws DaoException;

//    int getNumMatchingRepsFromFinalClusterWithAnnotation(String finalClusterAcc, String annotationId)
//            throws DaoException;

//    List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromFinalClusterWithAnnotation(String finalClusterAcc,
//                                                                                            String annotationId,
//                                                                                            Set<String> clusterMemberAccs,
//                                                                                            int startIndex,
//                                                                                            int numRows,
//                                                                                            SortArgument[] sortArgs)
//            throws DaoException;


//    int getNumOfPeptidesForScaffoldByAccNo(String scaffoldAccNo) throws DaoException;

//    List<PeptideDetail> getPeptidesForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws DaoException;

    List<String> getTaxonSynonyms(Integer taxonId) throws DaoException;

}
