
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.janelia.it.jacs.compute.access.search.ProteinResult;
import org.janelia.it.jacs.compute.access.search.ProteinSearchDAO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 23, 2008
 * Time: 12:22:17 PM
 */
public class ExportDAO extends ComputeBaseDAO {
    public static final int BATCH_SIZE = 2000;

    public static final int FINAL_CLUSTER_TYPE = 0;
    public static final int CORE_CLUSTER_TYPE = 1;
    public static final int UNKNOWN_CLUSTER_TYPE = 2;

    public ExportDAO(Logger logger) {
        super(logger);
    }

    public int determineClusterType(String clusterAcc) {
        if (clusterAcc.indexOf("CAM_CRCL_") >= 0) {
            return CORE_CLUSTER_TYPE;
        }
        else if (clusterAcc.indexOf("CAM_CL") >= 0) {
            return FINAL_CLUSTER_TYPE;
        }
        else {
            return UNKNOWN_CLUSTER_TYPE;
        }
    }

    public List<String> getProteinIDsBySearchID(String searchID) throws DaoException {
        ProteinSearchDAO proteinSearchDAO = new ProteinSearchDAO(log);
        List<String> proteinAccList = new ArrayList<String>();
        List<ProteinResult> list = proteinSearchDAO.getPagedCategoryResultsByNodeId(new Long(searchID),
                0 /* start */, 0 /* end */, null /* sortArgs */);
        for (ProteinResult pr : list) {
            proteinAccList.add(pr.getAccession());
        }
        return proteinAccList;
    }

    public List<String> getProteinIDsByClusterAnnotation(String clusterID, String annotationID) throws DaoException {
        List<String> proteinAccList = new ArrayList<String>();
        FeatureDAO featureDAO = new FeatureDAO(log);
        if (determineClusterType(clusterID) == CORE_CLUSTER_TYPE) {
            List<ProteinClusterAnnotationMember> memberList =
                    featureDAO.getPagedMatchingRepsFromCoreClusterWithAnnotation(
                            clusterID, annotationID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterAnnotationMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else if (determineClusterType(clusterID) == FINAL_CLUSTER_TYPE) {
            List<ProteinClusterAnnotationMember> memberList =
                    featureDAO.getPagedMatchingRepsFromFinalClusterWithAnnotation(
                            clusterID, annotationID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterAnnotationMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else {
            throw new DaoException("Do not recognize cluster type of clusterAcc=" + clusterID);
        }
        return proteinAccList;
    }

    public List<String> getNonRedundantProteinIDsByClusterID(String clusterID) throws DaoException {
        List<String> proteinAccList = new ArrayList<String>();
        FeatureDAO featureDAO = new FeatureDAO(log);
        if (determineClusterType(clusterID) == CORE_CLUSTER_TYPE) {
            List<ProteinClusterMember> memberList =
                    featureDAO.getPagedNRSeqMembersFromCoreCluster(
                            clusterID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else if (determineClusterType(clusterID) == FINAL_CLUSTER_TYPE) {
            List<ProteinClusterMember> memberList =
                    featureDAO.getPagedNRSeqMembersFromFinalCluster(
                            clusterID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else {
            throw new DaoException("Do not recognize cluster type of clusterAcc=" + clusterID);
        }
        return proteinAccList;
    }

    public List<String> getMemberProteinIDsByClusterID(String clusterID) throws DaoException {
        List<String> proteinAccList = new ArrayList<String>();
        FeatureDAO featureDAO = new FeatureDAO(log);
        if (determineClusterType(clusterID) == CORE_CLUSTER_TYPE) {
            List<ProteinClusterMember> memberList =
                    featureDAO.getPagedSeqMembersFromCoreCluster(
                            clusterID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else if (determineClusterType(clusterID) == FINAL_CLUSTER_TYPE) {
            List<ProteinClusterMember> memberList =
                    featureDAO.getPagedSeqMembersFromFinalCluster(
                            clusterID, null /* clusterMemberAccs */, 0 /*startIndex*/, 0 /*numRows*/, null /*sortArgs*/);
            if (memberList != null) {
                for (ProteinClusterMember member : memberList) {
                    proteinAccList.add(member.getProteinAcc());
                }
            }
        }
        else {
            throw new DaoException("Do not recognize cluster type of clusterAcc=" + clusterID);
        }
        return proteinAccList;
    }

    /* this method returns the protein list ordered by rank from lowest to highest, ie, best to worst */
    public List<String> getProteinIDsByBlastTaskID(String blastTaskID) throws DaoException {
        List<String> proteinAccList = new ArrayList<String>();
        FeatureDAO featureDAO = new FeatureDAO(log);
        SortArgument[] sortArgs = new SortArgument[1];
        sortArgs[0] = new SortArgument("rank", SortArgument.SORT_ASC);
        List<Object[]> results = featureDAO.getBlastResultsByTaskOrIDs(
                new Long(blastTaskID), null /*blastHitIdSet*/, 0 /*startIndex*/, 0 /*numRows*/,
                false /* w HSPs */, sortArgs);
        if (results != null) {
            for (Object[] arr : results) {
                BlastHit hit = (BlastHit) arr[0];
                BaseSequenceEntity bse = hit.getSubjectEntity();
                if (bse instanceof Protein) {
                    proteinAccList.add(bse.getAccession());
                }
            }
        }
        return proteinAccList;
    }

    Comparator<Peptide> createProteinComparatorFromSortArgs(SortArgument[] sortArgs) {
        return null; // placeholder
    }

    /* returns proteins in accList order */
    public List<Peptide> getProteinsByIDList(List<String> accList, Comparator<Peptide> comparator) throws DaoException {
        if (comparator != null) {
            throw new DaoException("Comparator capability not implemented");
        }
        List<Peptide> proteinList = new ArrayList<Peptide>();
        List<String> tmpList = new ArrayList<String>();
        for (String anAccList : accList) {
            if (tmpList.size() == BATCH_SIZE) {
                List<Peptide> pList = getProteins(tmpList);
                proteinList.addAll(pList);
                tmpList.clear();
            }
            tmpList.add(anAccList);
        }
        if (tmpList.size() > 0) {
            List<Peptide> pList = getProteins(tmpList);
            proteinList.addAll(pList);
        }
        return proteinList;
    }

    List<Peptide> getProteins(List<String> accList) {
        if (accList == null || accList.size() == 0) {
            return new ArrayList<Peptide>(); // empty list
        }
        else {
            Set<String> uniqueCheck = new HashSet<String>();
            for (String acc : accList) {
                if (!uniqueCheck.contains(acc)) {
                    uniqueCheck.add(acc);
                }
            }
            log.debug("getProteins: accList size=" + accList.size() + " of which " + uniqueCheck.size() + " are unqiue");
//            StringBuffer hqlBuffer = new StringBuffer("select p " +
//                    "from Protein p " +
//                    "where p.accession in (:accList) ");
            StringBuffer hqlBuffer = new StringBuffer("select p " +
                    "from Peptide p " + // covers both Peptides and Proteins
                    "where p.accession in (:accList) ");
            Query query = getSession().createQuery(hqlBuffer.toString());
            query.setParameterList("accList", accList);
            List<Peptide> pList = query.list();
            log.debug("getProteins: list returned size=" + pList.size());
            Map<String, Peptide> pMap = new HashMap<String, Peptide>();
            for (Peptide p : pList) {
                pMap.put(p.getAccession().trim(), p);
            }
            List<Peptide> deliveryList = new ArrayList<Peptide>();
            List<String> nullList = new ArrayList<String>();
            for (String s : accList) {
                Peptide p = pMap.get(s.trim());
                if (p != null) {
                    deliveryList.add(p);
                }
                else {
                    nullList.add(s.trim());
                }
            }
            int limit = 10;
            if (nullList.size() < limit)
                limit = nullList.size();
            StringBuffer nullExample = new StringBuffer("");
            for (int i = 0; i < limit; i++) {
                nullExample.append(nullList.get(i)).append(" ");
            }
//            _logger.debug("getProteins: delivery size="+deliveryList.size()+" nullCount="+nullList.size()+
//                    " examples="+nullExample.toString());
            return deliveryList;
        }
    }

    /* returns bses in accList order */
    public List<BaseSequenceEntity> getBsesByIDList(List<String> accList, Comparator<BaseSequenceEntity> comparator)
            throws DaoException {
        if (comparator != null) {
            throw new DaoException("Comparator capability not implemented");
        }
        List<BaseSequenceEntity> bseList = new ArrayList<BaseSequenceEntity>();
        List<String> tmpList = new ArrayList<String>();
        for (String anAccList : accList) {
            if (tmpList.size() == BATCH_SIZE) {
                List<BaseSequenceEntity> bList = getBses(tmpList);
                bseList.addAll(bList);
                tmpList.clear();
            }
            tmpList.add(anAccList);
        }
        if (tmpList.size() > 0) {
            List<BaseSequenceEntity> bList = getBses(tmpList);
            bseList.addAll(bList);
        }
        return bseList;
    }

    List<BaseSequenceEntity> getBses(List<String> accList) {
        if (accList == null || accList.size() == 0) {
            return new ArrayList<BaseSequenceEntity>(); // empty list
        }
        else {
            Set<String> uniqueCheck = new HashSet<String>();
            for (String acc : accList) {
                if (!uniqueCheck.contains(acc)) {
                    uniqueCheck.add(acc);
                }
            }
            log.debug("getBses: accList size=" + accList.size() + " of which " + uniqueCheck.size() + " are unqiue");
            StringBuffer hqlBuffer = new StringBuffer("select b " +
                    "from BaseSequenceEntity b " +
                    "where b.accession in (:accList) ");
            Query query = getSession().createQuery(hqlBuffer.toString());
            query.setParameterList("accList", accList);
            List<BaseSequenceEntity> bList = query.list();
            log.debug("getBses: list returned size=" + bList.size());
            Map<String, BaseSequenceEntity> bMap = new HashMap<String, BaseSequenceEntity>();
            for (BaseSequenceEntity b : bList) {
                bMap.put(b.getAccession().trim(), b);
            }
            List<BaseSequenceEntity> deliveryList = new ArrayList<BaseSequenceEntity>();
            List<String> nullList = new ArrayList<String>();
            for (String s : accList) {
                BaseSequenceEntity b = bMap.get(s.trim());
                if (b != null) {
                    deliveryList.add(b);
                }
                else {
                    nullList.add(s.trim());
                }
            }
            int limit = 10;
            if (nullList.size() < limit)
                limit = nullList.size();
            StringBuffer nullExample = new StringBuffer("");
            for (int i = 0; i < limit; i++) {
                nullExample.append(nullList.get(i)).append(" ");
            }
            return deliveryList;
        }
    }

}
