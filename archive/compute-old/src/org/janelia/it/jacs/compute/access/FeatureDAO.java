
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.janelia.it.jacs.compute.service.export.util.AnnotationUtil;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.ProteinClusterAnnotationMember;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.model.genomics.Read;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 2, 2008
 * Time: 1:38:49 PM
 */
public class FeatureDAO extends ComputeBaseDAO {
    private static List<EvidencePattern> externalEvidencePatternList = null;
    public static final String ANNOT_ROW_SEP = "\n";
    public static final String ANNOT_LIST_SEP = "\t";

    public FeatureDAO(Logger logger) {
        super(logger);
    }

    private static class EvidencePattern {
        Pattern p;
        String linkPrefix;

        public EvidencePattern(String patternString, String linkPrefix) {
            p = Pattern.compile(patternString);
            this.linkPrefix = linkPrefix;
        }

        public String match(String evidence) {
            if (evidence == null)
                return null;
            String link = null;
            Matcher m = p.matcher(evidence.trim());
            if (m.matches()) {
                if (m.groupCount() > 0)
                    link = linkPrefix + m.group(1);
            }
            return link; // returns null if no match
        }
    }

    /**
     * retrieve the number of actual blast hit results for the given taskId and/or set of IDs
     * the number may differ when the results are associated with "pooled" samples, i.e.,
     * samples associated with more than one site
     *
     * @param taskId
     * @param blastHitIdSet
     * @param includeAllSampleMaterials
     * @return
     * @throws DaoException
     */
    public Long getNumBlastResultsByTaskOrIDs(Long taskId, Set<Long> blastHitIdSet, boolean includeAllSampleMaterials)
            throws DaoException {
        try {
            log.debug("FeatureDAOImpl.getNumBlastResultsByTaskOrIDs");
            // Retrieve the hits from the node, plus the query and subject deflines
            String hitSelectCondition;
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hitSelectCondition = "hit.blastHitId in (:blastHitIdSet)";
            }
            else if (taskId != null) {
                hitSelectCondition = "resultNode.task.objectId = :taskId ";
            }
            else {
                throw new IllegalArgumentException("No task ID or set of hit IDs provided");
            }
            String hql = "select cast(count(*),long) " +
                    "from BlastHit hit " +
                    "inner join hit.resultNode resultNode " +
                    "inner join resultNode.deflineMap queryDef " +
                    "inner join resultNode.deflineMap subjectDef " +
                    "left join hit.queryEntity queryEntity " +
                    "inner join hit.subjectEntity subjectEntity " +
                    "left join subjectEntity.sample sample " +
                    (includeAllSampleMaterials
                            ? "left join sample.bioMaterials bioMaterial "
                            : "") +
                    "where " + hitSelectCondition + " " +
                    "  and index(subjectDef) = hit.subjectAcc " +
                    "  and index(queryDef) = hit.queryAcc ";
            log.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            else if (taskId != null) {
                query.setLong("taskId", taskId);
            }
            return (Long) query.uniqueResult();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            log.error(e);
            throw handleException(e, "FeatureDAOImpl.getNumBlastResultsByTaskOrIDs");
        }
    }

    /**
     * This method retrieves a list of blast results for a task or if the IDs are already known
     * it can retrieve only the specified results
     *
     * @param taskId            the job ID
     * @param blastHitIdSet     the set of IDs of the required results
     * @param startIndex        results offset
     * @param numRows           results length
     * @param includeHSPRanking if true the results include the number of high scoring pairs (HSP)
     *                          for a certain query, subject sequence and the the HSP rank within the query - subject match
     * @param sortArgs          specify sort options
     * @return List<Object[]> the blast results where a blast result actually consists of an array of object in which
     *         blast result[0] is a BlastHit object
     *         blast result[1] is the query defline
     *         blast result[2] is the subject defline
     *         blast result[3] the HSP rank for a (query, subject) pair if includeHSPRanking = true
     *         blast result[3] the number of HSPs for a (query, subject) pair if includeHSPRanking = true
     * @throws DaoException
     */
    public List<Object[]> getBlastResultsByTaskOrIDs(Long taskId,
                                                     Set<Long> blastHitIdSet,
                                                     int startIndex,
                                                     int numRows,
                                                     boolean includeHSPRanking,
                                                     SortArgument[] sortArgs)
            throws DaoException {
        try {
            log.debug("FeatureDAOImpl.getBlastResultsByTaskOrIDs");
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            String rankRestriction = null;
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals("rank")) {
                        dataSortField = "hit.rank";
                        if ((blastHitIdSet == null || blastHitIdSet.size() == 0) &&
                                startIndex >= 0 && numRows > 0) {
                            // only create a rank restriction if the startIndex and numRows are valid
                            // and there's no subset restriction either
                            rankRestriction = "hit.rank between " + startIndex + " and " + (startIndex + numRows - 1) + " ";
                        }
                    }
                    else if (dataSortField.equals("bitScore")) {
                        dataSortField = "hit.bitScore";
                    }
                    else if (dataSortField.equals("lengthAlignment")) {
                        dataSortField = "hit.lengthAlignment";
                    }
                    else if (dataSortField.equals("subjectAcc")) {
                        dataSortField = "hit.subjectAcc";
                    }
                    else if (dataSortField.equals("queryDef")) {
                        dataSortField = "queryDef";
                    }
                    else if (dataSortField.equals("subjectDef")) {
                        dataSortField = "subjectDef";
                    }
                    else if (dataSortField.equals("sampleName")) {
                        dataSortField = "sample.sampleName";
                    }
                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" desc");
                        }
                    }

                } // end for all sortArgs
            }
            String orderByClause;
            if (orderByFieldsBuffer.length() == 0) {
                orderByClause = "order by hit.bitScore desc ";
            }
            else {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hspFields = null;
            if (includeHSPRanking) {
                hspFields = "(select count(h2.rank)+1 " +
                        "    from BlastHit h2 " +
                        "    where h2.resultNode.objectId = resultNode.objectId " +
                        "      and h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc " +
                        "      and h2.rank < hit.rank) as hspRank, " +
                        "   (select count(*) " +
                        "    from BlastHit h2 " +
                        "    where h2.resultNode.objectId = resultNode.objectId " +
                        "      and h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc) as nhsps ";
            }
            // Retrieve the hits from the node, plus the query and subject deflines
            String hitSelectCondition;
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                hitSelectCondition = "hit.blastHitId in (:blastHitIdSet)";
            }
            else if (taskId != null) {
                hitSelectCondition = "resultNode.task.objectId = :taskId ";
            }
            else {
                throw new IllegalArgumentException("No task ID or set of hit IDs provided");
            }
            String hql = "select hit, " +
                    "       queryDef, " +
                    "       subjectDef " +
                    (hspFields != null ? "," + hspFields : "") +
                    "from BlastHit hit " +
                    "inner join hit.resultNode resultNode " +
                    "inner join resultNode.deflineMap queryDef " +
                    "inner join resultNode.deflineMap subjectDef " +
                    "left join fetch hit.queryEntity queryEntity " +
                    "inner join fetch hit.subjectEntity subjectEntity " +
                    "left join fetch subjectEntity.sample sample " +
                    "where " + hitSelectCondition + " " +
                    "  and index(subjectDef) = hit.subjectAcc " +
                    "  and index(queryDef) = hit.queryAcc " +
                    ((rankRestriction != null) ? "and " + rankRestriction : "") +
                    orderByClause;
            log.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            if (blastHitIdSet != null && blastHitIdSet.size() > 0) {
                query.setParameterList("blastHitIdSet", blastHitIdSet);
            }
            else if (taskId != null) {
                query.setLong("taskId", taskId);
            }
            if (rankRestriction == null) { // need to restrict result set if not done by rank clause
                if (startIndex > 0) {
                    query.setFirstResult(startIndex);
                }
                if (numRows > 0) {
                    query.setMaxResults(numRows);
                }
            }
            List<Object[]> results = query.list();
            log.debug("FeatureDAOImpl.getBlastResultsByTaskOrIDs found " +
                    ((results == null) ? 0 : results.size()) + " hits");
            return results;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            log.error(e);
            throw handleException(e, "FeatureDAOImpl.getBlastResultsByTaskOrIDs");
        }
    }

    public BaseSequenceEntity findBseByAcc(String accesion) throws DaoException {
        log.debug("findBseByAcc() called with accession=" + accesion);
        try {
            Criteria criteria = getSession().createCriteria(BaseSequenceEntity.class);
            Criterion cr = Restrictions.eq("accession", accesion);
            criteria.add(cr);
            criteria.setFetchMode("assembly", FetchMode.JOIN);

            List<BaseSequenceEntity> bses = (List<BaseSequenceEntity>) criteria.list();
            if (bses.size() < 1) {
                log.debug("no results - returning null");
                return null;
            }
            else {
                BaseSequenceEntity bse = bses.get(0);
                if (bse == null) {
                    log.debug("null result - returning null");
                }
                else {
                    log.debug("returning bse with accession=" + bse.getAccession());
                }
                return bses.get(0);
            }
        }
        catch (HibernateException e) {
            throw handleException(e, "FeatureDAOImpl - findByAcc");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FeatureDAOImpl - findByAcc");
        }
    }

    public List<BaseSequenceEntity> getSequenceEntitiesByAccessions(Collection<String> accCollection)
            throws DaoException {
        try {
            if (accCollection == null || accCollection.size() == 0) {
                // if no set of accessions given return an empty set
                // instead of returning everything
                return new ArrayList<BaseSequenceEntity>();
            }
            String hql = "select bse from BaseSequenceEntity bse where bse.accession in (:accessions)";
            log.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            query.setParameterList("accessions", accCollection);
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            log.error(e);
            throw handleException(e, this.getClass().getName() + " - getSequenceEntitiesByAccessions");
        }
    }

    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromCoreClusterWithAnnotation(String coreClusterAcc,
                                                                                                  String annotationId,
                                                                                                  Set<String> clusterMemberAccs,
                                                                                                  int startIndex,
                                                                                                  int numRows,
                                                                                                  SortArgument[] sortArgs)
            throws DaoException {
        String orderByClause = buildOrderByClause(sortArgs);
        String sql =
                "select " +
                        "pd.protein_acc as proteinAcc, " +
                        "cc.core_cluster_acc as coreClusterAcc, " +
                        "cp.final_cluster_acc as finalClusterAcc, " +
                        "cp.nr_parent_acc, " +
                        "cc.cluster_quality as clusterQuality, " +
                        "pa.evidence, " +
                        "pd.gene_symbol as gene_symbol, " +
                        "pd.gene_ontology as gene_ontology, " +
                        "pd.enzyme_commission as enzyme_commission, " +
                        "pd.length as length, " +
                        "pd.protein_function as protein_function " +
                        "from core_cluster cc " +
                        "inner join protein_annotation pa on pa.core_cluster_id = cc.core_cluster_id and pa.id = :annotationID " +
                        "inner join core_cluster_protein cp on pa.protein_id = cp.protein_id " +
                        "inner join protein_detail pd on pa.protein_id=pd.protein_id " +
                        "where cc.core_cluster_acc = :coreClusterAcc ";

        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve protein cluster data sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
        sqlQuery.setString("annotationID", annotationId);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterAnnotationMember> proteinClusterInfoList = new ArrayList<ProteinClusterAnnotationMember>();
        populateProteinClusterInfoList(results, proteinClusterInfoList);
        return proteinClusterInfoList;
    }

    protected String buildOrderByClause(SortArgument[] sortArgs) {
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField == null || dataSortField.length() == 0) {
                    continue;
                }
                if (sortArg.isAsc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField).append(" asc");
                }
                else if (sortArg.isDesc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField).append(" desc");
                }
            } // end for all sortArgs
        }
        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = "order by " + orderByFieldsBuffer.toString();
        }
        return orderByClause;
    }

    public void populateProteinClusterInfoList(List<Object[]> results, List<ProteinClusterAnnotationMember> proteinClusterInfoList) {
        // First, we need to construct a list of evidence strings from which we can generate a list of
        // external evidence links
        List<String> evidenceList = new ArrayList<String>();
        for (Object[] result : results) {
            evidenceList.add((String) result[5]);
        }
        List<String> evidenceExternalLinkList = createExternalEvidenceLinks(evidenceList);
        int i = 0;
        for (Object[] result : results) {
            ProteinClusterAnnotationMember proteinClusterInfo = new ProteinClusterAnnotationMember();
            proteinClusterInfo.setProteinAcc((String) result[0]);
            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
            proteinClusterInfo.setClusterQuality((String) result[4]);
            proteinClusterInfo.setEvidence((String) result[5]);
            proteinClusterInfo.setExternalEvidenceLink(evidenceExternalLinkList.get(i));
            proteinClusterInfo.setGeneSymbol((String) result[6]);
            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[8], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setLength((Integer) result[9]);
            proteinClusterInfo.setProteinFunction((String) result[10]);
            proteinClusterInfoList.add(proteinClusterInfo);
            i++;
        }
    }

    // Static method to permit other usage of this method
    public static List createExternalEvidenceLinks(List evidence) {
        if (externalEvidencePatternList == null) {
            EvidencePattern ncbiPattern = new EvidencePattern(
                    "(\\d+)\\|", "http://www.ncbi.nlm.nih.gov/sites/entrez?db=protein&cmd=search&term=");
            EvidencePattern gbPattern = new EvidencePattern(
                    "GB\\|\\S+\\|(\\d+)\\|\\S+", "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein&id=");
            EvidencePattern omniPattern = new EvidencePattern(
                    "OMNI\\|(\\S+)", "http://cmr.jcvi.org/cgi-bin/CMR/shared/GenePage.cgi?locus=");
            EvidencePattern pdbPattern = new EvidencePattern(
                    "PDB\\|(\\S+)\\_\\S+\\|\\d+\\|\\S+", "http://www.rcsb.org/pdb/cgi/explore.cgi?pdbId=");
            EvidencePattern pfPattern = new EvidencePattern(
                    "(PF\\d+)", "http://cmr.jcvi.org/cgi-bin/CMR/HmmReport.cgi?hmm_acc=");
            EvidencePattern prfPattern = new EvidencePattern(
                    "PRF\\|(\\S+)\\|\\S+\\|\\S+", "http://www.genome.jp/dbget-bin/www_bget?prf:");
            EvidencePattern rfPattern = new EvidencePattern(
                    "RF\\|\\S+\\|(\\d+)\\|\\S+", "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein&id=");
            EvidencePattern spPattern = new EvidencePattern(
                    "SP\\|(\\S+)\\|\\S+", "http://beta.uniprot.org/uniprot/");
            EvidencePattern tigrPattern = new EvidencePattern(
                    "(TIGR\\d+)", "http://cmr.jcvi.org/cgi-bin/CMR/HmmReport.cgi?hmm_acc=");
            externalEvidencePatternList = new ArrayList<EvidencePattern>();
            externalEvidencePatternList.add(ncbiPattern);
            externalEvidencePatternList.add(gbPattern);
            externalEvidencePatternList.add(omniPattern);
            externalEvidencePatternList.add(pdbPattern);
            externalEvidencePatternList.add(pfPattern);
            externalEvidencePatternList.add(prfPattern);
            externalEvidencePatternList.add(rfPattern);
            externalEvidencePatternList.add(spPattern);
            externalEvidencePatternList.add(tigrPattern);
        }
        ArrayList<String> evidenceLinkList = new ArrayList<String>();
        for (Object evidenceObject : evidence) {
            String evidenceString = (String) evidenceObject;
            String linkString = null;
            for (EvidencePattern p : externalEvidencePatternList) {
                linkString = p.match(evidenceString);
                if (linkString != null)
                    break;
            }
            evidenceLinkList.add(linkString);
        }

        // Test - temporary
//        ArrayList<String> testList=new ArrayList<String>();
//        testList.add("13423160|");
//        testList.add("24054684|");
//        testList.add("PROJECT");
//        testList.add("GB|BAC57907.1|28569866|AB090816");
//        testList.add("LipoproteinMotif");
//        testList.add("OMNI|PIN_A1174");
//        testList.add("PDB|1FCQ_A|15988107|1FCQ_A");
//        testList.add("PF00628");
//        testList.add("PRF|0508174A|223108|0508174A");
//        testList.add("PRIAM");
//        testList.add("RF|XP_001230712.1|118780358|XM_001230711");
//        testList.add("RF|NP_651629.1|21357803|NM_143372");
//        testList.add("SP|P34859|NU4LM_APILI");
//        testList.add("TIGR00006");
//        testList.add("TMHMM");
//
//        logger.debug("E check6");
//        for (String testString : testList) {
//            logger.debug("E check7");
//            String linkString=null;
//            for (EvidencePattern p : externalEvidencePatternList) {
//                linkString=p.match(testString);
//                if (linkString!=null)
//                    break;
//            }
//            logger.debug("ExternalEvidenceLink test="+testString+" link="+linkString);
//        }
        return evidenceLinkList;
    }

    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromFinalClusterWithAnnotation(
            String finalClusterAcc,
            String annotationId,
            Set<String> clusterMemberAccs,
            int startIndex,
            int numRows,
            SortArgument[] sortArgs) throws DaoException {

        String orderByClause = buildOrderByClause(sortArgs);
        String sql =
                "select " +
                        "pd.protein_acc as proteinAcc, " +
                        "cp.core_cluster_acc as coreClusterAcc, " +
                        "fc.final_cluster_acc as finalClusterAcc, " +
                        "cp.nr_parent_acc, " +
                        "fc.cluster_quality as clusterQuality, " +
                        "pa.evidence, " +
                        "pd.gene_symbol as gene_symbol, " +
                        "pd.gene_ontology as gene_ontology, " +
                        "pd.enzyme_commission as enzyme_commission, " +
                        "pd.length as length, " +
                        "pd.protein_function as protein_function " +
                        "from final_cluster fc " +
                        "inner join protein_annotation pa on pa.final_cluster_id = fc.final_cluster_id and pa.id = :annotationID " +
                        "inner join core_cluster_protein cp on pa.protein_id = cp.protein_id " +
                        "inner join protein_detail pd on pa.protein_id=pd.protein_id " +
                        "where fc.final_cluster_acc = :finalClusterAcc ";

        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve protein cluster data sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
        sqlQuery.setString("annotationID", annotationId);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterAnnotationMember> proteinClusterInfoList = new ArrayList<ProteinClusterAnnotationMember>();
        populateProteinClusterInfoList(results, proteinClusterInfoList);
        return proteinClusterInfoList;
    }

    public List<ProteinClusterMember> getPagedNRSeqMembersFromCoreCluster(String coreClusterAcc,
                                                                          Set<String> clusterMemberAccs,
                                                                          int startIndex,
                                                                          int numRows,
                                                                          SortArgument[] sortArgs)
            throws DaoException {
        String orderByClause = buildOrderByClause(sortArgs);
        String sql = "select  " +
                "pd.protein_acc as proteinAcc, " +
                "cc.core_cluster_acc as coreClusterAcc, " +
                "cp.final_cluster_acc as finalClusterAcc, " +
                "cp.nr_parent_acc, " +
                "cc.cluster_quality as clusterQuality, " +
                "pd.gene_symbol as gene_symbol, " +
                "pd.gene_ontology as gene_ontology, " +
                "pd.enzyme_commission as enzyme_commission, " +
                "pd.length as length, " +
                "pd.protein_function as protein_function " +
                "from core_cluster cc " +
                "inner join core_cluster_protein cp on cc.core_cluster_id = cp.core_cluster_id " +
                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
                "where cc.core_cluster_acc = :coreClusterAcc " +
                "and cp.nr_parent_acc is NULL ";
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve non redundant proteins sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
        for (Object[] result : results) {
            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
            proteinClusterInfo.setProteinAcc((String) result[0]);
            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
            proteinClusterInfo.setClusterQuality((String) result[4]);

            proteinClusterInfo.setGeneSymbol((String) result[5]);
            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setLength((Integer) result[8]);
            proteinClusterInfo.setProteinFunction((String) result[9]);

            proteinClusterInfoList.add(proteinClusterInfo);
        }
        return proteinClusterInfoList;
    }

    public List<ProteinClusterMember> getPagedNRSeqMembersFromFinalCluster(String finalClusterAcc,
                                                                           Set<String> clusterMemberAccs,
                                                                           int startIndex,
                                                                           int numRows,
                                                                           SortArgument[] sortArgs)
            throws DaoException {
        String orderByClause = buildOrderByClause(sortArgs);
        String sql = "select  " +
                "pd.protein_acc as proteinAcc, " +
                "cp.core_cluster_acc as coreClusterAcc, " +
                "fc.final_cluster_acc as finalClusterAcc, " +
                "cp.nr_parent_acc, " +
                "fc.cluster_quality as clusterQuality, " +
                "pd.gene_symbol as gene_symbol, " +
                "pd.gene_ontology as gene_ontology, " +
                "pd.enzyme_commission as enzyme_commission, " +
                "pd.length as length, " +
                "pd.protein_function as protein_function " +
                "from final_cluster fc " +
                "inner join core_cluster_protein cp on fc.final_cluster_id = cp.final_cluster_id " +
                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
                "where fc.final_cluster_acc = :finalClusterAcc ";
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve non redundant proteins sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
        for (Object[] result : results) {
            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
            proteinClusterInfo.setProteinAcc((String) result[0]);
            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
            proteinClusterInfo.setClusterQuality((String) result[4]);

            proteinClusterInfo.setGeneSymbol((String) result[5]);
            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setLength((Integer) result[8]);
            proteinClusterInfo.setProteinFunction((String) result[9]);

            proteinClusterInfoList.add(proteinClusterInfo);
        }
        return proteinClusterInfoList;
    }

    public List<ProteinClusterMember> getPagedSeqMembersFromCoreCluster(String coreClusterAcc,
                                                                        Set<String> clusterMemberAccs,
                                                                        int startIndex,
                                                                        int numRows,
                                                                        SortArgument[] sortArgs)
            throws DaoException {
        String orderByClause = buildOrderByClause(sortArgs);
        String sql = "select  " +
                "pd.protein_acc as proteinAcc, " +
                "cc.core_cluster_acc as coreClusterAcc, " +
                "cc.final_cluster_acc as finalClusterAcc, " +
                "cp.nr_parent_acc, " +
                "cc.cluster_quality as clusterQuality, " +
                "pd.gene_symbol as gene_symbol, " +
                "pd.gene_ontology as gene_ontology, " +
                "pd.enzyme_commission as enzyme_commission, " +
                "pd.length as length, " +
                "pd.protein_function as protein_function " +
                "from core_cluster cc " +
                "inner join core_cluster_protein cp on cc.core_cluster_id = cp.core_cluster_id " +
                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
                "where cc.core_cluster_acc = :coreClusterAcc ";
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve protein cluster data sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("coreClusterAcc", coreClusterAcc);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
        for (Object[] result : results) {
            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
            proteinClusterInfo.setProteinAcc((String) result[0]);
            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
            proteinClusterInfo.setClusterQuality((String) result[4]);

            proteinClusterInfo.setGeneSymbol((String) result[5]);
            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setLength((Integer) result[8]);
            proteinClusterInfo.setProteinFunction((String) result[9]);

            proteinClusterInfoList.add(proteinClusterInfo);
        }
        return proteinClusterInfoList;
    }

    public List<ProteinClusterMember> getPagedSeqMembersFromFinalCluster(String finalClusterAcc,
                                                                         Set<String> clusterMemberAccs, int startIndex,
                                                                         int numRows,
                                                                         SortArgument[] sortArgs)
            throws DaoException {
        String orderByClause = buildOrderByClause(sortArgs);
        String sql = "select  " +
                "pd.protein_acc as proteinAcc, " +
                "cp.core_cluster_acc as coreClusterAcc, " +
                "fc.final_cluster_acc as finalClusterAcc, " +
                "cp.nr_parent_acc, " +
                "fc.cluster_quality as clusterQuality, " +
                "pd.gene_symbol as gene_symbol, " +
                "pd.gene_ontology as gene_ontology, " +
                "pd.enzyme_commission as enzyme_commission, " +
                "pd.length as length, " +
                "pd.protein_function as protein_function " +
                "from final_cluster fc " +
                "inner join core_cluster_protein cp on fc.final_cluster_id = cp.final_cluster_id " +
                "inner join protein_detail pd on pd.protein_id=cp.protein_id " +
                "where fc.final_cluster_acc = :finalClusterAcc ";
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sql += "and pd.protein_acc in (:clusterMemberAccs) ";
        }
        sql += orderByClause;
        log.info("Retrieve protein cluster data sql: " + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setString("finalClusterAcc", finalClusterAcc);
        if (clusterMemberAccs != null && clusterMemberAccs.size() > 0) {
            sqlQuery.setParameterList("clusterMemberAccs", clusterMemberAccs);
        }
        if (startIndex > 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<ProteinClusterMember> proteinClusterInfoList = new ArrayList<ProteinClusterMember>();
        for (Object[] result : results) {
            ProteinClusterMember proteinClusterInfo = new ProteinClusterMember();
            proteinClusterInfo.setProteinAcc((String) result[0]);
            proteinClusterInfo.setCoreClusterAcc((String) result[1]);
            proteinClusterInfo.setFinalClusterAcc((String) result[2]);
            proteinClusterInfo.setNonRedundantParentAcc((String) result[3]);
            proteinClusterInfo.setClusterQuality((String) result[4]);

            proteinClusterInfo.setGeneSymbol((String) result[5]);
            proteinClusterInfo.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[6], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) result[7], ANNOT_ROW_SEP, ANNOT_LIST_SEP));
            proteinClusterInfo.setLength((Integer) result[8]);
            proteinClusterInfo.setProteinFunction((String) result[9]);

            proteinClusterInfoList.add(proteinClusterInfo);
        }
        return proteinClusterInfoList;
    }

    public List<Read> getPagedReadsFromSample(String sampleAcc,
                                              Set<String> readAccessions,
                                              int startIndex,
                                              int numRows,
                                              SortArgument[] sortArgs)
            throws DataAccessException, DaoException {
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField == null || dataSortField.length() == 0) {
                    continue;
                }
                if (sortArg.isAsc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField).append(" asc");
                }
                else if (sortArg.isDesc()) {
                    if (orderByFieldsBuffer.length() > 0) {
                        orderByFieldsBuffer.append(',');
                    }
                    orderByFieldsBuffer.append(dataSortField).append(" desc");
                }
            } // end for all sortArgs
        }
        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = "order by " + orderByFieldsBuffer.toString();
        }
        StringBuffer hqlBuffer = new StringBuffer("select r " +
                "from Read r " +
                "where r.library.sampleAcc = :sampleAcc ");
        if (readAccessions != null && readAccessions.size() > 0) {
            hqlBuffer.append("and r.accession in (:readAccessions) ");
        }
        hqlBuffer.append(orderByClause);
        Query query = getSession().createQuery(hqlBuffer.toString());
        query.setParameter("sampleAcc", sampleAcc);
        if (readAccessions != null && readAccessions.size() > 0) {
            query.setParameterList("readAccessions", readAccessions);
        }
        if (startIndex >= 0) {
            query.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            query.setMaxResults(numRows);
        }
        log.debug("Reads for sample " + sampleAcc + " hql: " + hqlBuffer.toString());
        return query.list();
    }

    public Integer getNumReadsFromSample(String sampleAcc) throws DataAccessException, DaoException {
        // we count the reads by summing the number of reads per library selected by sample
        // since this is more efficient than going through all the reads for the sample
        String hql = "select " +
                "sum(library.numberOfReads) " +
                "from Library library " +
                "where library.sampleAcc = :sampleAcc";
        Query query = getSession().createQuery(hql);
        query.setParameter("sampleAcc", sampleAcc);
        log.debug("Count reads for sample " + sampleAcc + " hql: " + hql);
        Long c = (Long) query.uniqueResult();
        return c == null ? 0 : c.intValue();
    }

}
