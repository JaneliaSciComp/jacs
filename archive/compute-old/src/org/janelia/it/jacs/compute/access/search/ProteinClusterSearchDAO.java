
package org.janelia.it.jacs.compute.access.search;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SearchDAO;
import org.janelia.it.jacs.compute.service.export.util.AnnotationUtil;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 11:13:35 AM
 */
public class ProteinClusterSearchDAO extends SearchDAO {
    public ProteinClusterSearchDAO(Logger logger) {
        super(logger);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_CLUSTER, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_CLUSTER, matchFlags, startIndex, numRows, sortArgs);
    }

    public List<ClusterResult> getPagedCategoryResultsByNodeId(Long nodeId,
                                                               int startIndex,
                                                               int numRows,
                                                               SortArgument[] sortArgs) throws DaoException {
        String sql = "select ts.rank, fd.final_cluster_acc, fd.num_core_cluster, fd.num_protein, fd.num_nonredundant, fd.gn_symbols, fd.protein_functions, fd.ec_List, fd.go_list " +
                "from (select hit_id, rank from final_cluster_ts_result where node_id=" + nodeId + " order by rank desc) ts " +
                "inner join final_cluster_detail fd on fd.final_cluster_id=ts.hit_id";
        sql = addOrderClauseToSql(sql, sortArgs);
        log.info("Executing cluster search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        if (startIndex >= 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        log.info("Cluster search yielded result count=" + results.size());
        List<ClusterResult> clusters = new ArrayList<ClusterResult>();
        for (Object[] res : results) {
            ClusterResult clusterResult = new ClusterResult();
            clusterResult.setRank((Float) res[0]);
            clusterResult.setFinalAccession((String) res[1]);
            clusterResult.setNumCoreClusters((Integer) res[2]);
            clusterResult.setNumProteins((Integer) res[3]);
            clusterResult.setNumNRProteins((Integer) res[4]);
            clusterResult.setGeneSymbols((String) res[5]);
            clusterResult.setProteinFunctions((String) res[6]);
            List<AnnotationDescription> annoList = AnnotationUtil.createAnnotationListFromString((String) res[7]);
            clusterResult.setEcAnnotationDescription(annoList);
            annoList = AnnotationUtil.createAnnotationListFromString((String) res[8]);
            clusterResult.setGoAnnotationDescription(annoList);
            clusters.add(clusterResult);
        }
        log.info("Returning cluster result of size=" + clusters.size());
        return clusters;
    }

    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql =
                "select cast(count(distinct hit_id) as Integer)" +
                        "from final_cluster_ts_result " +
                        "where node_id=" + nodeId;
        log.info("Executing row count cluster search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        return (Integer) sqlQuery.uniqueResult();
    }

}
